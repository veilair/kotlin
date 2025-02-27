/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.FirIdeResolveStateService
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

/**
 * Returns [FirModuleResolveState] which corresponds to containing module
 */
fun KtElement.getResolveState(): FirModuleResolveState {
    val project = project
    return getKtModule(project).getResolveState(project)
}

/**
 * Returns [FirModuleResolveState] which corresponds to containing module
 */
fun KtModule.getResolveState(project: Project): FirModuleResolveState =
    FirIdeResolveStateService.getInstance(project).getResolveState(this)


/**
 * Creates [FirBasedSymbol] by [KtDeclaration] .
 * returned [FirDeclaration]  will be resolved at least to [phase]
 *
 */
fun KtDeclaration.resolveToFirSymbol(
    resolveState: FirModuleResolveState,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
): FirBasedSymbol<*> {
    return resolveState.resolveToFirSymbol(this, resolveState, phase)
}

/**
 * Creates [FirBasedSymbol] by [KtDeclaration] .
 * returned [FirDeclaration] will be resolved at least to [phase]
 *
 * If resulted [FirBasedSymbol] is not subtype of [S], throws [InvalidFirElementTypeException]
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
inline fun <reified S : FirBasedSymbol<*>> KtDeclaration.resolveToFirSymbolOfType(
    resolveState: FirModuleResolveState,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
): @kotlin.internal.NoInfer S {
    val symbol = resolveToFirSymbol(resolveState, phase)
    if (symbol !is S) {
        throwUnexpectedFirElementError(symbol, this, S::class)
    }
    return symbol
}

/**
 * Creates [FirBasedSymbol] by [KtDeclaration] .
 * returned [FirDeclaration] will be resolved at least to [phase]
 *
 * If resulted [FirBasedSymbol] is not subtype of [S], returns `null`
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
inline fun <reified S : FirBasedSymbol<*>> KtDeclaration.resolveToFirSymbolOfTypeSafe(
    resolveState: FirModuleResolveState,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
): @kotlin.internal.NoInfer S? {
    return resolveToFirSymbol(resolveState, phase) as? S
}


/**
 * Returns a list of Diagnostics compiler finds for given [KtElement]
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
fun KtElement.getDiagnostics(resolveState: FirModuleResolveState, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> =
    resolveState.getDiagnostics(this, filter)

/**
 * Returns a list of Diagnostics compiler finds for given [KtFile]
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
fun KtFile.collectDiagnosticsForFile(
    resolveState: FirModuleResolveState,
    filter: DiagnosticCheckerFilter
): Collection<KtPsiDiagnostic> =
    resolveState.collectDiagnosticsForFile(this, filter)

/**
 * Get a [FirElement] which was created by [KtElement]
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase.
 *
 * The `null` value is returned iff FIR tree does not have corresponding element
 */
fun KtElement.getOrBuildFir(
    resolveState: FirModuleResolveState,
): FirElement? = resolveState.getOrBuildFirFor(this)

/**
 * Get a [FirElement] which was created by [KtElement], but only if it is subtype of [E], `null` otherwise
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
inline fun <reified E : FirElement> KtElement.getOrBuildFirSafe(
    resolveState: FirModuleResolveState,
) = getOrBuildFir(resolveState) as? E

/**
 * Get a [FirElement] which was created by [KtElement], but only if it is subtype of [E], throws [InvalidFirElementTypeException] otherwise
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
inline fun <reified E : FirElement> KtElement.getOrBuildFirOfType(
    resolveState: FirModuleResolveState,
): E {
    val fir = this.getOrBuildFir(resolveState)
    if (fir is E) return fir
    throwUnexpectedFirElementError(fir, this, E::class)
}

/**
 * Get a [FirFile] which was created by [KtElement]
 * Returned [FirFile] can be resolved to any phase from [FirResolvePhase.RAW_FIR] to [FirResolvePhase.BODY_RESOLVE]
 */
fun KtFile.getOrBuildFirFile(resolveState: FirModuleResolveState): FirFile =
    resolveState.getOrBuildFirFile(this)
