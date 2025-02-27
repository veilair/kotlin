/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import com.intellij.openapi.extensions.AreaInstance
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class CliFe10AnalysisFacade(project: Project) : Fe10AnalysisFacade {
    private val handler by lazy { KtFe10AnalysisHandlerExtension.getInstance(project) }

    override fun getResolveSession(element: KtElement): ResolveSession {
        return handler.resolveSession ?: error("Resolution is not performed")
    }

    override fun getDeprecationResolver(element: KtElement): DeprecationResolver {
        return handler.deprecationResolver ?: error("Resolution is not performed")
    }

    override fun analyze(element: KtElement, mode: Fe10AnalysisFacade.AnalysisMode): BindingContext {
        return getResolveSession(element).bindingContext
    }

    override fun getOrigin(file: VirtualFile): KtSymbolOrigin {
        return KtSymbolOrigin.LIBRARY
    }
}

class KtFe10AnalysisHandlerExtension : AnalysisHandlerExtension {
    internal companion object {
        fun getInstance(area: AreaInstance): KtFe10AnalysisHandlerExtension {
            return AnalysisHandlerExtension.extensionPointName.getExtensions(area).firstIsInstanceOrNull()
                ?: error(KtFe10AnalysisHandlerExtension::class.java.name + " should be registered")
        }
    }

    var resolveSession: ResolveSession? = null
        private set

    var deprecationResolver: DeprecationResolver? = null
        private set

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        resolveSession = componentProvider.get()
        deprecationResolver = componentProvider.get()
        return super.doAnalysis(project, module, projectContext, files, bindingTrace, componentProvider)
    }
}