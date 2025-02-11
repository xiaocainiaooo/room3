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

import androidx.appfunctions.compiler.AppFunctionCompiler
import androidx.appfunctions.compiler.core.IntrospectionHelper.APP_FUNCTIONS_AGGREGATED_DEPS_PACKAGE_NAME
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionComponentRegistryAnnotation
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock

/** A helper class to generate AppFunction component registry. */
class AppFunctionComponentRegistryGenerator(private val codeGenerator: CodeGenerator) {
    /**
     * Generates AppFunction component registry..
     *
     * For example, if a list of components under module "myLibrary" were provided to generate
     * INVENTORY registry,
     * * "com.android.Test1"
     * * "com.android.Test2"
     * * "com.android.diff.Test1"
     *
     * It would generate
     *
     * ```
     * package appfunctions_aggregated_deps
     *
     * @AppFunctionComponentRegistry(
     *   componentCategory = "INVENTORY",
     *   componentNames = [
     *     "com.android.Test1",
     *     "com.android.Test2",
     *   ]
     * )
     * @Generated
     * public class `$ComAndroid_Inventory`
     *
     * @AppFunctionComponentRegistry(
     *   componentCategory = "INVENTORY",
     *   componentNames = [
     *     "com.android.diff.Test1",
     *   ]
     * )
     * @Generated
     * public class `$ComAndroidDiff_Inventory`
     * ```
     *
     * The components are grouped by the package name when generating a registry. This is to ensure
     * that each registry class has an unique name across all compilation units.
     */
    fun generateRegistriesByPackageName(
        category: String,
        components: List<AppFunctionComponent>,
    ) {
        val componentsByPackageName = components.groupBy(AppFunctionComponent::packageName)
        for ((packageName, groupedComponents) in componentsByPackageName) {
            val registryName = getRegistryClassName(packageName, category)
            generateRegistry(registryName, category, groupedComponents)
        }
    }

    private fun generateRegistry(
        className: String,
        componentCategory: String,
        components: List<AppFunctionComponent>,
    ) {
        val annotationBuilder =
            AnnotationSpec.builder(AppFunctionComponentRegistryAnnotation.CLASS_NAME)
                .addMember(
                    "${AppFunctionComponentRegistryAnnotation.PROPERTY_COMPONENT_CATEGORY} = %S",
                    componentCategory,
                )
                .addMember(
                    buildCodeBlock {
                        addStatement(
                            "${AppFunctionComponentRegistryAnnotation.PROPERTY_COMPONENT_NAMES} = ["
                        )
                        indent()
                        for (componentName in components.map { it.qualifiedName }) {
                            addStatement("%S,", componentName)
                        }
                        unindent()
                        add("]")
                    }
                )

        val registryClassBuilder = TypeSpec.classBuilder(className)
        registryClassBuilder.addAnnotation(annotationBuilder.build())
        registryClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)

        val fileSpec =
            FileSpec.builder(APP_FUNCTIONS_AGGREGATED_DEPS_PACKAGE_NAME, className)
                .addType(registryClassBuilder.build())
                .build()

        val sourceFiles = components.flatMap { it.sourceFiles }.toSet()
        codeGenerator
            .createNewFile(
                Dependencies(aggregating = true, sources = sourceFiles.toTypedArray()),
                APP_FUNCTIONS_AGGREGATED_DEPS_PACKAGE_NAME,
                className,
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    private fun getRegistryClassName(packageName: String, componentCategory: String): String {
        val prefix = packageName.toPascalCase()
        val componentCategoryPascalCase = componentCategory.toPascalCase()
        return "${'$'}${prefix}_${componentCategoryPascalCase}ComponentRegistry"
    }

    /** Wrapper to hold AppFunction component data. */
    class AppFunctionComponent(
        /** The component class package name. */
        val packageName: String,
        /** The component class qualified name. */
        val qualifiedName: String,
        /** The source files used to generate the component. */
        val sourceFiles: Set<KSFile> = emptySet(),
    )
}
