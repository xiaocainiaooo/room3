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

package androidx.appfunctions.compiler.processors

import androidx.appfunctions.compiler.AppFunctionCompiler
import androidx.appfunctions.compiler.AppFunctionCompilerOptions
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.IntrospectionHelper.APP_FUNCTIONS_INTERNAL_PACKAGE_NAME
import androidx.appfunctions.compiler.core.IntrospectionHelper.APP_FUNCTION_INVENTORY_CLASS
import androidx.appfunctions.compiler.core.IntrospectionHelper.AggregateAppFunctionInventoryClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AggregateAppFunctionInvokerClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionInvokerClass
import androidx.appfunctions.compiler.core.toClassName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock

/**
 * The processor generates the implementation of AggregateAppFunctionInventory and
 * AggregateAppFunctionInvoker.
 *
 * The processor would only start aggregation process when
 * * [AppFunctionCompilerOptions.aggregateAppFunctions] is true.
 * * AND there is no remaining @AppFunction nodes to processed.
 */
class AppFunctionAggregateProcessor(
    private val options: AppFunctionCompilerOptions,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    private var hasProcessed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!options.aggregateAppFunctions) {
            return emptyList()
        }

        if (hasProcessed) {
            return emptyList()
        }

        if (!shouldProcess(resolver)) {
            return emptyList()
        }

        generateAggregateAppFunctionInventory(resolver)
        generateAggregateAppFunctionInvoker(resolver)

        hasProcessed = true
        return emptyList()
    }

    private fun generateAggregateAppFunctionInventory(resolver: Resolver) {
        val generatedInventories =
            AppFunctionSymbolResolver(resolver).getGeneratedAppFunctionInventories()
        val aggregateInventoryClassName =
            "${'$'}${AggregateAppFunctionInventoryClass.CLASS_NAME.simpleName}_Impl"

        val aggregateInventoryClassBuilder = TypeSpec.classBuilder(aggregateInventoryClassName)
        aggregateInventoryClassBuilder.superclass(AggregateAppFunctionInventoryClass.CLASS_NAME)
        aggregateInventoryClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
        aggregateInventoryClassBuilder.addProperty(buildInventoriesProperty(generatedInventories))

        val fileSpec =
            FileSpec.builder(APP_FUNCTIONS_INTERNAL_PACKAGE_NAME, aggregateInventoryClassName)
                .addType(aggregateInventoryClassBuilder.build())
                .build()

        codeGenerator
            .createNewFile(
                // TODO: Collect all AppFunction files as source files set
                Dependencies.ALL_FILES,
                APP_FUNCTIONS_INTERNAL_PACKAGE_NAME,
                aggregateInventoryClassName
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    private fun buildInventoriesProperty(
        generatedInventories: List<KSClassDeclaration>
    ): PropertySpec {
        return PropertySpec.builder(
                AggregateAppFunctionInventoryClass.PROPERTY_INVENTORIES_NAME,
                List::class.asClassName().parameterizedBy(APP_FUNCTION_INVENTORY_CLASS)
            )
            .addModifiers(KModifier.OVERRIDE)
            .initializer(
                buildCodeBlock {
                    addStatement("listOf(")
                    indent()
                    for (generatedInventory in generatedInventories) {
                        addStatement("%T(),", generatedInventory.toClassName())
                    }
                    unindent()
                    addStatement(")")
                }
            )
            .build()
    }

    private fun generateAggregateAppFunctionInvoker(resolver: Resolver) {
        val generatedInvokers =
            AppFunctionSymbolResolver(resolver).getGeneratedAppFunctionInvokers()
        val aggregateInvokerClassName =
            "${'$'}${AggregateAppFunctionInvokerClass.CLASS_NAME.simpleName}_Impl"

        val aggregateInvokerClassBuilder = TypeSpec.classBuilder(aggregateInvokerClassName)
        aggregateInvokerClassBuilder.superclass(AggregateAppFunctionInvokerClass.CLASS_NAME)
        aggregateInvokerClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
        aggregateInvokerClassBuilder.addProperty(buildInvokersProperty(generatedInvokers))

        val fileSpec =
            FileSpec.builder(APP_FUNCTIONS_INTERNAL_PACKAGE_NAME, aggregateInvokerClassName)
                .addType(aggregateInvokerClassBuilder.build())
                .build()

        codeGenerator
            .createNewFile(
                // TODO: Collect all AppFunction files as source files set
                Dependencies.ALL_FILES,
                APP_FUNCTIONS_INTERNAL_PACKAGE_NAME,
                aggregateInvokerClassName
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    private fun buildInvokersProperty(generatedInvokers: List<KSClassDeclaration>): PropertySpec {
        return PropertySpec.builder(
                AggregateAppFunctionInvokerClass.PROPERTY_INVOKERS_NAME,
                List::class.asClassName().parameterizedBy(AppFunctionInvokerClass.CLASS_NAME)
            )
            .addModifiers(KModifier.OVERRIDE)
            .initializer(
                buildCodeBlock {
                    addStatement("listOf(")
                    indent()
                    for (generatedInvoker in generatedInvokers) {
                        addStatement("%T(),", generatedInvoker.toClassName())
                    }
                    unindent()
                    addStatement(")")
                }
            )
            .build()
    }

    private fun shouldProcess(resolver: Resolver): Boolean {
        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val appFunctionClasses = appFunctionSymbolResolver.resolveAnnotatedAppFunctions()
        return appFunctionClasses.isEmpty()
    }
}
