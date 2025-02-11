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
import androidx.appfunctions.compiler.core.AnnotatedAppFunctions
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.IntrospectionHelper
import androidx.appfunctions.compiler.core.ProcessingException
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock

/**
 * Generates implementations for the AppFunctionInventory interface.
 *
 * It resolves all functions in a class annotated with `@AppFunction`, and generates the
 * corresponding metadata for those functions.
 */
class AppFunctionInventoryProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val appFunctionClasses = appFunctionSymbolResolver.resolveAnnotatedAppFunctions()
        for (appFunctionClass in appFunctionClasses) {
            generateAppFunctionInventoryClass(appFunctionClass)
        }
        return emptyList()
    }

    private fun generateAppFunctionInventoryClass(appFunctionClass: AnnotatedAppFunctions) {
        val originalPackageName = appFunctionClass.classDeclaration.packageName.asString()
        val originalClassName = appFunctionClass.classDeclaration.simpleName.asString()

        val inventoryClassName = getAppFunctionInventoryClassName(originalClassName)
        val inventoryClassBuilder = TypeSpec.classBuilder(inventoryClassName)
        inventoryClassBuilder.addSuperinterface(IntrospectionHelper.APP_FUNCTION_INVENTORY_CLASS)
        inventoryClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
        inventoryClassBuilder.addKdoc(buildSourceFilesKdoc(appFunctionClass))
        inventoryClassBuilder.addProperty(buildFunctionIdToMetadataMapProperty())

        addFunctionMetadataProperties(inventoryClassBuilder, appFunctionClass)

        val fileSpec =
            FileSpec.builder(originalPackageName, inventoryClassName)
                .addType(inventoryClassBuilder.build())
                .build()
        codeGenerator
            .createNewFile(
                Dependencies(aggregating = true, *appFunctionClass.getSourceFiles().toTypedArray()),
                originalPackageName,
                inventoryClassName
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    /**
     * Adds properties to the `AppFunctionInventory` class for each function in the class.
     *
     * @param inventoryClassBuilder The builder for the `AppFunctionInventory` class.
     * @param appFunctionClass The class annotated with `@AppFunction`.
     */
    private fun addFunctionMetadataProperties(
        inventoryClassBuilder: TypeSpec.Builder,
        appFunctionClass: AnnotatedAppFunctions
    ) {
        val appFunctionMetadataList = appFunctionClass.createAppFunctionMetadataList()

        for (functionMetadata in appFunctionMetadataList) {
            val functionMetadataObjectClassBuilder =
                TypeSpec.objectBuilder(getFunctionMetadataObjectClassName(functionMetadata.id))
                    .addModifiers(KModifier.PRIVATE)
            addSchemaMetadataPropertyForFunction(
                functionMetadataObjectClassBuilder,
                functionMetadata.schema
            )
            addPropertiesForParameterMetadataList(
                functionMetadataObjectClassBuilder,
                functionMetadata.parameters
            )
            addPropertyForResponseMetadata(
                functionMetadataObjectClassBuilder,
                functionMetadata.response
            )
            addPropertyForAppFunctionMetadata(functionMetadataObjectClassBuilder, functionMetadata)
            inventoryClassBuilder.addType(functionMetadataObjectClassBuilder.build())
        }
    }

    private fun addPropertyForAppFunctionMetadata(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        functionMetadata: AppFunctionMetadata
    ) {
        val appFunctionMetadataPropertyName = "APP_FUNCTION_METADATA"
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    appFunctionMetadataPropertyName,
                    IntrospectionHelper.APP_FUNCTION_METADATA_CLASS
                )
                .addModifiers(KModifier.PUBLIC)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                id = %S,
                                isEnabledByDefault = %L,
                                schema =  %L,
                                parameters = %L,
                                response = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_METADATA_CLASS,
                            functionMetadata.id,
                            functionMetadata.isEnabledByDefault,
                            SCHEMA_METADATA_PROPERTY_NAME,
                            PARAMETER_METADATA_LIST_PROPERTY_NAME,
                            RESPONSE_METADATA_PROPERTY_NAME
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForResponseMetadata(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        appFunctionResponseMetadata: AppFunctionResponseMetadata
    ) {
        val responseMetadataPropertyName = RESPONSE_METADATA_PROPERTY_NAME
        val responseMetadataValueTypeName =
            when (val castDataType = appFunctionResponseMetadata.valueType) {
                is AppFunctionPrimitiveTypeMetadata -> {
                    val primitiveReturnTypeMetadataPropertyName = "PRIMITIVE_RESPONSE_VALUE_TYPE"
                    addPropertyForPrimitiveTypeMetadata(
                        primitiveReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    primitiveReturnTypeMetadataPropertyName
                }
                is AppFunctionArrayTypeMetadata -> {
                    val arrayReturnTypeMetadataPropertyName = "ARRAY_RESPONSE_VALUE_TYPE"
                    addPropertyForArrayTypeMetadata(
                        arrayReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    arrayReturnTypeMetadataPropertyName
                }
                is AppFunctionObjectTypeMetadata -> {
                    val objectReturnTypeMetadataPropertyName = "OBJECT_RESPONSE_VALUE_TYPE"
                    addPropertyForObjectTypeMetadata(
                        objectReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    objectReturnTypeMetadataPropertyName
                }
                else -> {
                    // TODO provide KSNode to improve error message
                    throw ProcessingException(
                        "Unable to build parameter metadata for unknown datatype: $castDataType",
                        null
                    )
                }
            }
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    responseMetadataPropertyName,
                    IntrospectionHelper.APP_FUNCTION_RESPONSE_METADATA_CLASS
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                valueType = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_RESPONSE_METADATA_CLASS,
                            responseMetadataValueTypeName
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertiesForParameterMetadataList(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        parameterMetadataList: List<AppFunctionParameterMetadata>
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    PARAMETER_METADATA_LIST_PROPERTY_NAME,
                    List::class.asClassName()
                        .parameterizedBy(IntrospectionHelper.APP_FUNCTION_PARAMETER_METADATA_CLASS)
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement("listOf(")
                        indent()
                        for (parameterMetadata in parameterMetadataList) {
                            addPropertiesForParameterMetadata(
                                parameterMetadata,
                                functionMetadataObjectClassBuilder
                            )
                            addStatement(
                                "%L,",
                                "${parameterMetadata.name.uppercase()}_PARAMETER_METADATA"
                            )
                        }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        )
    }

    private fun addPropertiesForParameterMetadata(
        parameterMetadata: AppFunctionParameterMetadata,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
    ) {
        val parameterMetadataPropertyName =
            "${parameterMetadata.name.uppercase()}_PARAMETER_METADATA"
        val datatypeVariableName =
            when (val castDataType = parameterMetadata.dataType) {
                is AppFunctionPrimitiveTypeMetadata -> {
                    val primitiveTypeMetadataPropertyName =
                        getPrimitiveTypeMetadataPropertyNameForParameter(parameterMetadata)
                    addPropertyForPrimitiveTypeMetadata(
                        primitiveTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    primitiveTypeMetadataPropertyName
                }
                is AppFunctionArrayTypeMetadata -> {
                    val arrayTypeMetadataPropertyName =
                        getArrayTypeMetadataPropertyNameForParameter(parameterMetadata)
                    addPropertyForArrayTypeMetadata(
                        arrayTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    arrayTypeMetadataPropertyName
                }
                is AppFunctionObjectTypeMetadata -> {
                    val objectTypeMetadataPropertyName =
                        getObjectTypeMetadataPropertyNameForParameter(parameterMetadata)
                    addPropertyForObjectTypeMetadata(
                        objectTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    objectTypeMetadataPropertyName
                }
                else -> {
                    // TODO provide KSNode to improve error message
                    throw ProcessingException(
                        "Unable to build parameter metadata for unknown datatype: $castDataType",
                        null
                    )
                }
            }
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    parameterMetadataPropertyName,
                    IntrospectionHelper.APP_FUNCTION_PARAMETER_METADATA_CLASS
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                name = %S,
                                isRequired = %L,
                                dataType = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_PARAMETER_METADATA_CLASS,
                            parameterMetadata.name,
                            parameterMetadata.isRequired,
                            datatypeVariableName
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForPrimitiveTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        primitiveTypeMetadata: AppFunctionPrimitiveTypeMetadata
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    IntrospectionHelper.APP_FUNCTION_PRIMITIVE_TYPE_METADATA_CLASS
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                type = %L,
                                isNullable = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_PRIMITIVE_TYPE_METADATA_CLASS,
                            primitiveTypeMetadata.type,
                            primitiveTypeMetadata.isNullable
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForArrayTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        arrayTypeMetadata: AppFunctionArrayTypeMetadata
    ) {
        val itemTypeVariableName =
            when (val castItemType = arrayTypeMetadata.itemType) {
                is AppFunctionPrimitiveTypeMetadata -> {
                    val primitiveItemTypeVariableName = propertyName + "_PRIMITIVE_ITEM_TYPE"
                    addPropertyForPrimitiveTypeMetadata(
                        primitiveItemTypeVariableName,
                        functionMetadataObjectClassBuilder,
                        castItemType
                    )
                    primitiveItemTypeVariableName
                }
                is AppFunctionObjectTypeMetadata -> {
                    val objectItemTypeVariableName = propertyName + "_OBJECT_ITEM_TYPE"
                    addPropertyForObjectTypeMetadata(
                        objectItemTypeVariableName,
                        functionMetadataObjectClassBuilder,
                        castItemType
                    )
                    objectItemTypeVariableName
                }
                else -> {
                    // TODO provide KSNode to improve error message
                    throw ProcessingException(
                        "Unable to build parameter item type metadata for unknown itemType: " +
                            "$castItemType",
                        null
                    )
                }
            }
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    IntrospectionHelper.APP_FUNCTION_ARRAY_TYPE_METADATA_CLASS
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                itemType = %L,
                                isNullable = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_ARRAY_TYPE_METADATA_CLASS,
                            itemTypeVariableName,
                            arrayTypeMetadata.isNullable
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForObjectTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        objectTypeMetadata: AppFunctionObjectTypeMetadata,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    IntrospectionHelper.APP_FUNCTION_OBJECT_TYPE_METADATA_CLASS
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        // TODO: properly construct Object type metadata.
                        addStatement(
                            """
                            %T(
                                properties = emptyMap(),
                                required = emptyList(),
                                qualifiedName = %L,
                                isNullable = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_OBJECT_TYPE_METADATA_CLASS,
                            objectTypeMetadata.qualifiedName,
                            objectTypeMetadata.isNullable,
                        )
                    }
                )
                .build()
        )
    }

    /** Creates the `functionIdToMetadataMap` property of the `AppFunctionInventory`. */
    private fun buildFunctionIdToMetadataMapProperty(): PropertySpec {
        return PropertySpec.builder(
                "functionIdToMetadataMap",
                Map::class.asClassName()
                    .parameterizedBy(
                        String::class.asClassName(),
                        IntrospectionHelper.APP_FUNCTION_METADATA_CLASS
                    ),
            )
            .addModifiers(KModifier.OVERRIDE)
            // TODO: Actually build map properties
            .initializer(buildCodeBlock { addStatement("mapOf()") })
            .build()
    }

    private fun addSchemaMetadataPropertyForFunction(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        schemaMetadata: AppFunctionSchemaMetadata?
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    SCHEMA_METADATA_PROPERTY_NAME,
                    IntrospectionHelper.APP_FUNCTION_SCHEMA_METADATA_CLASS.copy(nullable = true)
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        if (schemaMetadata == null) {
                            addStatement("%L", null)
                        } else {
                            addStatement(
                                "%T(category= %S, name=%S, version=%L)",
                                IntrospectionHelper.APP_FUNCTION_SCHEMA_METADATA_CLASS,
                                schemaMetadata.category,
                                schemaMetadata.name,
                                schemaMetadata.version
                            )
                        }
                    }
                )
                .build()
        )
    }

    // TODO: Remove doc once done with impl
    private fun buildSourceFilesKdoc(appFunctionClass: AnnotatedAppFunctions): CodeBlock {
        return buildCodeBlock {
            addStatement("Source Files:")
            for (file in appFunctionClass.getSourceFiles()) {
                addStatement(file.fileName)
            }
        }
    }

    private fun getAppFunctionInventoryClassName(functionClassName: String): String {
        return "$%s_AppFunctionInventory".format(functionClassName)
    }

    /**
     * Generates the name of the class for the metadata object of a function.
     *
     * @param functionId The ID of the function.
     * @return The name of the class.
     */
    private fun getFunctionMetadataObjectClassName(functionId: String): String {
        return functionId.replace("[^A-Za-z0-9]".toRegex(), "_").split("_").joinToString("") {
            it.replaceFirstChar { it.uppercase() }
        } + "MetadataObject"
    }

    /**
     * Generates the name of the property for the primitive type metadata of a parameter.
     *
     * @param parameterMetadata The metadata of the parameter.
     * @return The name of the property.
     */
    private fun getPrimitiveTypeMetadataPropertyNameForParameter(
        parameterMetadata: AppFunctionParameterMetadata
    ): String {
        return "PARAMETER_METADATA_${parameterMetadata.name.uppercase()}_PRIMITIVE_DATA_TYPE"
    }

    /**
     * Generates the name of the property for the array type metadata of a parameter.
     *
     * @param parameterMetadata The metadata of the parameter.
     * @return The name of the property.
     */
    private fun getArrayTypeMetadataPropertyNameForParameter(
        parameterMetadata: AppFunctionParameterMetadata
    ): String {
        return "PARAMETER_METADATA_${parameterMetadata.name.uppercase()}_ARRAY_DATA_TYPE"
    }

    /**
     * Generates the name of the property for the object type metadata of a parameter.
     *
     * @param parameterMetadata The metadata of the parameter.
     * @return The name of the property.
     */
    private fun getObjectTypeMetadataPropertyNameForParameter(
        parameterMetadata: AppFunctionParameterMetadata
    ): String {
        return "PARAMETER_METADATA_${parameterMetadata.name.uppercase()}_OBJECT_DATA_TYPE"
    }

    companion object {
        const val SCHEMA_METADATA_PROPERTY_NAME = "SCHEMA_METADATA"
        const val PARAMETER_METADATA_LIST_PROPERTY_NAME = "PARAMETER_METADATA_LIST"
        const val RESPONSE_METADATA_PROPERTY_NAME = "RESPONSE_METADATA"
    }
}
