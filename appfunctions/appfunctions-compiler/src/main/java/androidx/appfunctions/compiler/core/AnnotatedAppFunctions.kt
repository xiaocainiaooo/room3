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

import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionContextClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSchemaDefinitionAnnotation
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LIST

/**
 * Represents a collection of functions within a specific class that are annotated as app functions.
 */
data class AnnotatedAppFunctions(
    /**
     * The [com.google.devtools.ksp.symbol.KSClassDeclaration] of the class that contains the
     * annotated app functions.
     */
    val classDeclaration: KSClassDeclaration,
    /**
     * The list of [com.google.devtools.ksp.symbol.KSFunctionDeclaration] that annotated as app
     * function.
     */
    val appFunctionDeclarations: List<KSFunctionDeclaration>
) {
    fun validate(): AnnotatedAppFunctions {
        validateFirstParameter()
        validateParameterTypes()
        return this
    }

    private fun validateFirstParameter() {
        for (appFunctionDeclaration in appFunctionDeclarations) {
            val firstParam = appFunctionDeclaration.parameters.firstOrNull()
            if (firstParam == null) {
                throw ProcessingException(
                    "The first parameter of an app function must be " +
                        "${AppFunctionContextClass.CLASS_NAME}",
                    appFunctionDeclaration
                )
            }
            if (!firstParam.type.isOfType(AppFunctionContextClass.CLASS_NAME)) {
                throw ProcessingException(
                    "The first parameter of an app function must be " +
                        "${AppFunctionContextClass.CLASS_NAME}",
                    firstParam
                )
            }
        }
    }

    private fun validateParameterTypes() {
        for (appFunctionDeclaration in appFunctionDeclarations) {
            for ((paramIndex, ksValueParameter) in appFunctionDeclaration.parameters.withIndex()) {
                if (paramIndex == 0) {
                    // Skip the first parameter which is always the `AppFunctionContext`.
                    continue
                }

                if (!ksValueParameter.validateAppFunctionParameterType()) {
                    throw ProcessingException(
                        "App function parameters must be one of the following " +
                            "primitive types or a list of these types:\n${
                                SUPPORTED_RAW_PRIMITIVE_TYPES.joinToString(
                                    ",\n"
                                )
                            }, but found ${
                                ksValueParameter.resolveTypeReference().ensureQualifiedTypeName()
                                    .asString()
                            }",
                        ksValueParameter
                    )
                }
            }
        }
    }

    /**
     * Gets the identifier of an app functions.
     *
     * The format of the identifier is `packageName.className#methodName`.
     */
    fun getAppFunctionIdentifier(functionDeclaration: KSFunctionDeclaration): String {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val methodName = functionDeclaration.simpleName.asString()
        return "${packageName}.${className}#${methodName}"
    }

    /** Returns the file containing the class declaration and app functions. */
    fun getSourceFile(): KSFile? = classDeclaration.containingFile

    /** Gets the [classDeclaration]'s [ClassName]. */
    fun getEnclosingClassName(): ClassName {
        return ClassName(
            classDeclaration.packageName.asString(),
            classDeclaration.simpleName.asString()
        )
    }

    /** Creates [AppFunctionMetadata] instances for each of [appFunctionDeclarations]. */
    fun createAppFunctionMetadataInstances(): List<AppFunctionMetadata> =
        this.appFunctionDeclarations.map { fnDeclaration ->
            val appFunctionAnnotationProperties =
                computeAppFunctionAnnotationProperties(fnDeclaration)
            val schemaMetadata =
                if (appFunctionAnnotationProperties.schemaName != null) {
                    AppFunctionSchemaMetadata(
                        checkNotNull(appFunctionAnnotationProperties.schemaCategory),
                        checkNotNull(appFunctionAnnotationProperties.schemaName),
                        checkNotNull(appFunctionAnnotationProperties.schemaVersion)
                    )
                } else {
                    null
                }
            AppFunctionMetadata(
                id = this.getAppFunctionIdentifier(fnDeclaration),
                isEnabledByDefault = appFunctionAnnotationProperties.isEnabledByDefault,
                // TODO: Remove these when AppFunctionMetadata is finalized.
                isRestrictToTrustedCaller = false,
                displayNameRes = 0,
                schema = schemaMetadata,
                // TODO: Handle non-primitive and collections.
                parameters = fnDeclaration.buildMetadataForParameters(),
                response =
                    AppFunctionResponseMetadata(
                        isNullable = fnDeclaration.returnType?.resolve()?.isMarkedNullable == true,
                        dataType =
                            AppFunctionDataTypeMetadata(
                                type =
                                    checkNotNull(fnDeclaration.returnType?.toAppFunctionDataType())
                            )
                    ),
                components = AppFunctionComponentsMetadata(dataTypes = emptyList())
            )
        }

    private fun KSFunctionDeclaration.buildMetadataForParameters():
        List<AppFunctionParameterMetadata> =
        parameters
            .filter { !it.type.isOfType(AppFunctionContextClass.CLASS_NAME) }
            .map {
                AppFunctionParameterMetadata(
                    name = checkNotNull(it.name?.asString()),
                    // TODO: Correctly populate isRequired.
                    isRequired = !it.hasDefault,
                    dataType = AppFunctionDataTypeMetadata(type = it.type.toAppFunctionDataType())
                )
            }

    private fun KSTypeReference.toAppFunctionDataType(): Int =
        when (this.resolve().declaration.qualifiedName?.asString()) {
            "kotlin.String" -> AppFunctionDataTypeMetadata.STRING
            "kotlin.Int" -> AppFunctionDataTypeMetadata.INT
            "kotlin.Long" -> AppFunctionDataTypeMetadata.LONG
            "kotlin.Float" -> AppFunctionDataTypeMetadata.FLOAT
            "kotlin.Double" -> AppFunctionDataTypeMetadata.DOUBLE
            "kotlin.Boolean" -> AppFunctionDataTypeMetadata.BOOLEAN
            "kotlin.Byte" -> AppFunctionDataTypeMetadata.BYTES
            "kotlin.Unit" -> AppFunctionDataTypeMetadata.UNIT
            // TODO: Support converting other types.
            else -> AppFunctionDataTypeMetadata.OBJECT
        }

    private fun computeAppFunctionAnnotationProperties(
        functionDeclaration: KSFunctionDeclaration
    ): AppFunctionAnnotationProperties {
        val appFunctionAnnotation =
            functionDeclaration.annotations.findAnnotation(AppFunctionAnnotation.CLASS_NAME)
                ?: throw ProcessingException(
                    "Function not annotated with @AppFunction.",
                    functionDeclaration
                )
        val enabled =
            appFunctionAnnotation.requirePropertyValueOfType(
                AppFunctionAnnotation.PROPERTY_IS_ENABLED,
                Boolean::class,
            )

        val rootInterfaceWithAppFunctionSchemaDefinition =
            findRootAppFunctionSchemaInterface(functionDeclaration)

        val schemaFunctionAnnotation =
            rootInterfaceWithAppFunctionSchemaDefinition
                ?.annotations
                ?.findAnnotation(AppFunctionSchemaDefinitionAnnotation.CLASS_NAME)
        val schemaCategory =
            schemaFunctionAnnotation?.requirePropertyValueOfType(
                AppFunctionSchemaDefinitionAnnotation.PROPERTY_CATEGORY,
                String::class,
            )
        val schemaName =
            schemaFunctionAnnotation?.requirePropertyValueOfType(
                AppFunctionSchemaDefinitionAnnotation.PROPERTY_NAME,
                String::class,
            )
        val schemaVersion =
            schemaFunctionAnnotation
                ?.requirePropertyValueOfType(
                    AppFunctionSchemaDefinitionAnnotation.PROPERTY_VERSION,
                    Int::class,
                )
                ?.toLong()

        return AppFunctionAnnotationProperties(enabled, schemaName, schemaVersion, schemaCategory)
    }

    private fun findRootAppFunctionSchemaInterface(
        function: KSFunctionDeclaration,
    ): KSClassDeclaration? {
        val parentDeclaration = function.parentDeclaration as? KSClassDeclaration ?: return null

        // Check if the enclosing class has the @AppFunctionSchemaDefinition
        val annotation =
            parentDeclaration.annotations.findAnnotation(
                AppFunctionSchemaDefinitionAnnotation.CLASS_NAME
            )
        if (annotation != null) {
            return parentDeclaration
        }

        val superClassFunction = (function.findOverridee() as? KSFunctionDeclaration) ?: return null
        return findRootAppFunctionSchemaInterface(superClassFunction)
    }

    private fun KSValueParameter.validateAppFunctionParameterType(): Boolean {
        // Todo(b/391342300): Allow AppFunctionSerializable type too.
        if (type.isOfType(LIST)) {
            val typeReferenceArgument = type.resolveListParameterizedType()
            // List types only support raw primitive types
            return SUPPORTED_RAW_PRIMITIVE_TYPES.contains(
                typeReferenceArgument.ensureQualifiedTypeName().asString()
            )
        }
        return SUPPORTED_RAW_PRIMITIVE_TYPES.contains(type.ensureQualifiedTypeName().asString()) ||
            SUPPORTED_ARRAY_PRIMITIVE_TYPES.contains(type.ensureQualifiedTypeName().asString())
    }

    /**
     * Resolves the type reference of a parameter.
     *
     * If the parameter type is a list, it will resolve the type reference of the list element.
     */
    private fun KSValueParameter.resolveTypeReference(): KSTypeReference {
        return if (type.isOfType(LIST)) {
            type.resolveListParameterizedType()
        } else {
            type
        }
    }

    private data class AppFunctionAnnotationProperties(
        val isEnabledByDefault: Boolean,
        val schemaName: String?,
        val schemaVersion: Long?,
        val schemaCategory: String?
    )

    private companion object {
        val SUPPORTED_RAW_PRIMITIVE_TYPES: Set<String> =
            setOf(
                Int::class.qualifiedName!!,
                Long::class.qualifiedName!!,
                Float::class.qualifiedName!!,
                Double::class.qualifiedName!!,
                Boolean::class.qualifiedName!!,
                String::class.qualifiedName!!,
            )

        val SUPPORTED_ARRAY_PRIMITIVE_TYPES: Set<String> =
            setOf(
                IntArray::class.qualifiedName!!,
                LongArray::class.qualifiedName!!,
                FloatArray::class.qualifiedName!!,
                DoubleArray::class.qualifiedName!!,
                BooleanArray::class.qualifiedName!!,
            )
    }
}
