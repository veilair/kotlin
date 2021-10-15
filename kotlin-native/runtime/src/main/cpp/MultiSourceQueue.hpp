/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MULTI_SOURCE_QUEUE_H
#define RUNTIME_MULTI_SOURCE_QUEUE_H

#include <atomic>
#include <list>
#include <mutex>

#include "Mutex.hpp"
#include "Types.h"

namespace kotlin {

// A queue that is constructed by collecting subqueues from several `Producer`s.
template <typename T, typename Mutex>
class MultiSourceQueue {
public:
    class Producer;

    // TODO: Consider switching from `KStdList` to `SingleLockList` to hide the constructor
    // and to not store the iterator.
    class Node : private Pinned, public KonanAllocatorAware {
    public:
        Node(Producer* owner, const T& value) noexcept : value_(value), owner_(owner) {}

        template <typename... Args, typename = std::enable_if_t<std::is_constructible_v<T, Args...>>>
        explicit Node(Producer* owner, Args&& ...args) noexcept : value_(std::forward<Args>(args)...), owner_(owner) {}

        T& operator*() noexcept { return value_; }

        static Node& fromValue(T& t) noexcept {
            static_assert(std::is_base_of_v<Pinned, T>, "fromValue function only makes sense for non-movable object");
            return *reinterpret_cast<Node*>(reinterpret_cast<uintptr_t>(&t) - offsetof(Node, value_));
        }

    private:
        friend class MultiSourceQueue;

        T value_;
        std::atomic<Producer*> owner_; // `nullptr` signifies that `MultiSourceQueue` owns it.
        typename KStdList<Node>::iterator position_;
    };

    class Producer {
    public:
        explicit Producer(MultiSourceQueue& owner) noexcept : owner_(owner) {}

        ~Producer() { Publish(); }

        Node* Insert(const T& value) noexcept {
            queue_.emplace_back(this, value);
            auto& node = queue_.back();
            node.position_ = std::prev(queue_.end());
            return &node;
        }

        template <typename... Args, typename = std::enable_if_t<std::is_constructible_v<T, Args...>>>
        Node* Emplace(Args&& ...value) noexcept {
            queue_.emplace_back(this, std::forward<Args>(value)...);
            auto& node = queue_.back();
            node.position_ = std::prev(queue_.end());
            return &node;
        }

        void Erase(Node* node) noexcept {
            if (node->owner_ == this) {
                // If we own it, delete it immediately.
                queue_.erase(node->position_);
                return;
            }
            // If it's owned by the global queue or some other `Producer`, queue it.
            deletionQueue_.push_back(node);
        }

        // Merge `this` queue with owning `MultiSourceQueue`. `this` will have empty queue after the call.
        // This call is performed without heap allocations. TODO: Test that no allocations are happening.
        void Publish() noexcept {
            for (auto& node : queue_) {
                node.owner_ = nullptr;
            }
            std::lock_guard<Mutex> guard(owner_.mutex_);
            owner_.queue_.splice(owner_.queue_.end(), queue_);
            owner_.deletionQueue_.splice(owner_.deletionQueue_.end(), deletionQueue_);
        }

        void ClearForTests() noexcept {
            queue_.clear();
            deletionQueue_.clear();
        }

    private:
        MultiSourceQueue& owner_; // weak
        KStdList<Node> queue_;
        KStdList<Node*> deletionQueue_;
    };

    class Iterator {
    public:
        T& operator*() noexcept { return **position_; }

        Iterator& operator++() noexcept {
            ++position_;
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return position_ == rhs.position_; }

        bool operator!=(const Iterator& rhs) const noexcept { return position_ != rhs.position_; }

    private:
        friend class MultiSourceQueue;

        explicit Iterator(const typename KStdList<Node>::iterator& position) noexcept : position_(position) {}

        typename KStdList<Node>::iterator position_;
    };

    class Iterable : MoveOnly {
    public:
        Iterator begin() noexcept { return Iterator(owner_.queue_.begin()); }
        Iterator end() noexcept { return Iterator(owner_.queue_.end()); }

    private:
        friend class MultiSourceQueue;

        explicit Iterable(MultiSourceQueue& owner) noexcept : owner_(owner), guard_(owner_.mutex_) {}

        MultiSourceQueue& owner_; // weak
        std::unique_lock<Mutex> guard_;
    };

    // Lock `MultiSourceQueue` for safe iteration. If element was scheduled for deletion,
    // it'll still be iterated. Use `ApplyDeletions` to remove those elements.
    Iterable LockForIter() noexcept { return Iterable(*this); }

    // Lock `MultiSourceQueue` and apply deletions. Only deletes elements that were published.
    void ApplyDeletions() noexcept {
        std::lock_guard<Mutex> guard(mutex_);
        KStdList<Node*> remainingDeletions;

        auto it = deletionQueue_.begin();
        while (it != deletionQueue_.end()) {
            auto next = std::next(it);
            Node* node = *it;

            if (node->owner_ != nullptr) {
                // If the `Node` is still owned by some `Producer`, skip it.
                remainingDeletions.splice(remainingDeletions.end(), deletionQueue_, it);
            } else {
                queue_.erase(node->position_);
                // `node` is invalid after this
            }

            it = next;
        }
        deletionQueue_ = std::move(remainingDeletions);
    }

    void ClearForTests() noexcept {
        queue_.clear();
        deletionQueue_.clear();
    }

private:
    // Using `KStdList` as it allows to implement `Collect` without memory allocations,
    // which is important for GC mark phase.
    KStdList<Node> queue_;
    KStdList<Node*> deletionQueue_;
    Mutex mutex_;
};

} // namespace kotlin

#endif // RUNTIME_MULTI_SOURCE_QUEUE_H
