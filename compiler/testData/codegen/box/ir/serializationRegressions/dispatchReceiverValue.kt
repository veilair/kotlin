// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: expect/actual in the same module (ACTUAL_WITHOUT_EXPECT)
// MODULE: lib
// FILE: common.kt

expect class C {
    val value: String

    fun test(result: String = value): String
}

// FILE: platform.kt
actual class C(actual val value: String) {
    actual fun test(result: String): String = result
}

// MODULE: main(lib)
// FILE: main.kt
fun box() = C("Fail").test("OK")
