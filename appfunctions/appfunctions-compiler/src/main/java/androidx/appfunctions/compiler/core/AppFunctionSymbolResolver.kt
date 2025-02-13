/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appfunctions.compiler.core

import androidx.appfunctions.compiler.core.IntrospectionHelper.APP_FUNCTIONS_AGGREGATED_DEPS_PACKAGE_NAME
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionComponentRegistryAnnotation
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/** The helper class to resolve AppFunction related symbols. */
class AppFunctionSymbolResolver(private val resolver: Resolver) {

    /** Resolves valid functions annotated with @AppFunction annotation. */
    fun resolveAnnotatedAppFunctions(): List<AnnotatedAppFunctions> {
        return buildMap<KSClassDeclaration, MutableList<KSFunctionDeclaration>>() {
                val annotatedAppFunctions =
                    resolver.getSymbolsWithAnnotation(
                        AppFunctionAnnotation.CLASS_NAME.canonicalName
                    )
                for (symbol in annotatedAppFunctions) {
                    if (symbol !is KSFunctionDeclaration) {
                        throw ProcessingException(
                            "Only functions can be annotated with @AppFunction",
                            symbol
                        )
                    }
                    val functionClass = symbol.parentDeclaration as? KSClassDeclaration
                    if (functionClass == null) {
                        throw ProcessingException(
                            "Top level functions cannot be annotated with @AppFunction ",
                            symbol
                        )
                    }

                    this.getOrPut(functionClass) { mutableListOf<KSFunctionDeclaration>() }
                        .add(symbol)
                }
            }
            .map { (classDeclaration, appFunctionsDeclarations) ->
                AnnotatedAppFunctions(classDeclaration, appFunctionsDeclarations).validate()
            }
    }

    /** Gets generated AppFunctionInventory implementations. */
    fun getGeneratedAppFunctionInventories(): List<KSClassDeclaration> {
        return filterAppFunctionComponentQualifiedNames(
                AppFunctionComponentRegistryAnnotation.Category.INVENTORY
            )
            .map { componentName ->
                val ksName = resolver.getKSNameFromString(componentName)
                resolver.getClassDeclarationByName(ksName)
                    ?: throw ProcessingException(
                        "Unable to find KSClassDeclaration for ${ksName.asString()}",
                        null
                    )
            }
    }

    /** Gets generated AppFunctionInvoker implementations. */
    fun getGeneratedAppFunctionInvokers(): List<KSClassDeclaration> {
        return filterAppFunctionComponentQualifiedNames(
                AppFunctionComponentRegistryAnnotation.Category.INVOKER
            )
            .map { componentName ->
                val ksName = resolver.getKSNameFromString(componentName)
                resolver.getClassDeclarationByName(ksName)
                    ?: throw ProcessingException(
                        "Unable to find KSClassDeclaration for ${ksName.asString()}",
                        null
                    )
            }
    }

    @OptIn(KspExperimental::class)
    private fun filterAppFunctionComponentQualifiedNames(
        filterComponentCategory: String,
    ): List<String> {
        return resolver
            .getDeclarationsFromPackage(APP_FUNCTIONS_AGGREGATED_DEPS_PACKAGE_NAME)
            .flatMap { node ->
                val registryAnnotation =
                    node.annotations.findAnnotation(
                        AppFunctionComponentRegistryAnnotation.CLASS_NAME
                    ) ?: return@flatMap emptyList<String>()
                val componentCategory =
                    registryAnnotation.requirePropertyValueOfType(
                        AppFunctionComponentRegistryAnnotation.PROPERTY_COMPONENT_CATEGORY,
                        String::class
                    )
                val componentNames =
                    registryAnnotation.requirePropertyValueOfType(
                        AppFunctionComponentRegistryAnnotation.PROPERTY_COMPONENT_NAMES,
                        List::class
                    )
                return@flatMap if (componentCategory == filterComponentCategory) {
                    componentNames.filterIsInstance<String>()
                } else {
                    emptyList<String>()
                }
            }
            .toList()
    }
}
