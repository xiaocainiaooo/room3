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

import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_LIST
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_SINGULAR
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.Companion.SUPPORTED_TYPES_STRING
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.Companion.isSupportedType
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.ClassName

/** Represents a class annotated with [androidx.appfunctions.AppFunctionSerializable]. */
data class AnnotatedAppFunctionSerializable(val appFunctionSerializableClass: KSClassDeclaration) {
    // TODO(b/392587953): throw an error if a property has the same name as one of the factory
    //  method parameters
    /**
     * Validates that the class annotated with AppFunctionSerializable follows app function's spec.
     *
     * The annotated class must adhere to the following requirements:
     * 1. **Primary Constructor:** The class must have a public primary constructor.
     * 2. **Property Parameters:** Only properties (declared with `val`) can be passed as parameters
     *    to the primary constructor.
     * 3. **Supported Types:** All properties must be of one of the supported types.
     * 4. **Super Type Parameters:** All parameters in the primary constructor of a super type must
     *    be present in the primary constructor of the subtype.
     *
     * @throws ProcessingException if the class does not adhere to the requirements
     */
    fun validate(): AnnotatedAppFunctionSerializable {
        val validatedPrimaryConstructor =
            appFunctionSerializableClass.validateSerializablePrimaryConstructor()
        val superTypesWithSerializableAnnotation =
            appFunctionSerializableClass.superTypes
                .map { it.resolve().declaration as KSClassDeclaration }
                .filter {
                    it.annotations.findAnnotation(
                        IntrospectionHelper.AppFunctionSerializableAnnotation.CLASS_NAME
                    ) != null
                }
                .toSet()
        val superTypesWithCapabilityAnnotation = findSuperTypesWithCapabilityAnnotation()
        val parametersToValidate =
            validatedPrimaryConstructor.parameters
                .associateBy { checkNotNull(it.name).toString() }
                .toMutableMap()

        validateParameters(
            parametersToValidate,
            superTypesWithSerializableAnnotation,
            superTypesWithCapabilityAnnotation
        )

        return this
    }

    /**
     * Finds all super types of an [appFunctionSerializableClass] that are annotated with the
     * [androidx.appfunctions.AppFunctionSchemaCapability] annotation.
     *
     * For example, consider the following classes:
     * ```
     * @AppFunctionSchemaCapability
     * public interface AppFunctionOpenable {
     *     public val intentToOpen: PendingIntent
     * }
     *
     * public interface OpenableResponse : AppFunctionOpenable {
     *     override val intentToOpen: PendingIntent
     * }
     *
     * @AppFunctionSerializable
     * class MySerializableClass(
     *   override val intentToOpen: PendingIntent
     * ) : OpenableResponse
     * ```
     *
     * This method will return the [KSClassDeclaration] of `AppFunctionOpenable` since it is a super
     * type of `MySerializableClass` and is annotated with the
     * [androidx.appfunctions.AppFunctionSchemaCapability] annotation.
     *
     * @param appFunctionSerializableClass the [KSClassDeclaration] of the
     *   [appFunctionSerializableClass] being processed.
     * @return a set of [KSClassDeclaration] for all super types of the
     *   [appFunctionSerializableClass] that are annotated with
     *   [androidx.appfunctions.AppFunctionSchemaCapability].
     */
    fun findSuperTypesWithCapabilityAnnotation(): Set<KSClassDeclaration> {
        return buildSet {
            val unvisitedSuperTypes: MutableList<KSTypeReference> =
                appFunctionSerializableClass.superTypes.toMutableList()

            while (!unvisitedSuperTypes.isEmpty()) {
                val superTypeClassDeclaration =
                    unvisitedSuperTypes.removeLast().resolve().declaration as KSClassDeclaration
                if (
                    superTypeClassDeclaration.annotations.findAnnotation(
                        IntrospectionHelper.AppFunctionSchemaCapability.CLASS_NAME
                    ) != null
                ) {
                    add(superTypeClassDeclaration)
                }
                if (
                    superTypeClassDeclaration.annotations.findAnnotation(
                        IntrospectionHelper.AppFunctionSerializableAnnotation.CLASS_NAME
                    ) == null
                ) {
                    // Only consider non serializable super types since serializable super types
                    // are already handled separately
                    unvisitedSuperTypes.addAll(superTypeClassDeclaration.superTypes)
                }
            }
        }
    }

    private fun validateParameters(
        parametersToValidate: MutableMap<String, KSValueParameter>,
        superTypesWithSerializableAnnotation: Set<KSClassDeclaration>,
        superTypesWithCapabilityAnnotation: Set<KSClassDeclaration>,
    ) {
        for (superType in superTypesWithSerializableAnnotation) {
            if (
                superType.annotations.findAnnotation(
                    IntrospectionHelper.AppFunctionSerializableAnnotation.CLASS_NAME
                ) == null
            ) {
                throw ProcessingException(
                    "Expected supertype with @AppFunctionSerializable annotation.",
                    superType
                )
            }
            val superTypePrimaryConstructor = superType.validateSerializablePrimaryConstructor()

            for (superTypeParameter in superTypePrimaryConstructor.parameters) {
                // Parameter has now been visited
                val parameterInSuperType =
                    parametersToValidate.remove(superTypeParameter.name.toString())
                if (parameterInSuperType == null) {
                    throw ProcessingException(
                        "All parameters in @AppFunctionSerializable " +
                            "supertypes must be present in subtype",
                        superTypeParameter
                    )
                }
                validateSerializableParameter(parameterInSuperType)
            }
        }

        for (superType in superTypesWithCapabilityAnnotation) {
            if (
                superType.annotations.findAnnotation(
                    IntrospectionHelper.AppFunctionSchemaCapability.CLASS_NAME
                ) == null
            ) {
                throw ProcessingException(
                    "Expected supertype with @AppFunctionSchemaCapability annotation.",
                    superType
                )
            }
            val capabilityProperties = superType.getDeclaredProperties()

            for (superTypeProperty in capabilityProperties) {
                // Parameter has now been visited
                val parameterInSuperType =
                    parametersToValidate.remove(superTypeProperty.simpleName.toString())
                if (parameterInSuperType == null) {
                    throw ProcessingException(
                        "All Properties in @AppFunctionSchemaCapability " +
                            "supertypes must be present in subtype",
                        superTypeProperty
                    )
                }
                validateSerializableParameter(parameterInSuperType)
            }
        }

        // Validate the remaining parameters
        if (parametersToValidate.isNotEmpty()) {
            for ((_, parameterToValidate) in parametersToValidate) {
                validateSerializableParameter(parameterToValidate)
            }
        }
    }

    private fun KSClassDeclaration.validateSerializablePrimaryConstructor(): KSFunctionDeclaration {
        if (primaryConstructor == null) {
            throw ProcessingException(
                "Classes annotated with AppFunctionSerializable must have a primary constructor.",
                this
            )
        }
        val primaryConstructorDeclaration = checkNotNull(primaryConstructor)
        if (primaryConstructorDeclaration.parameters.isEmpty()) {
            throw ProcessingException(
                "Classes annotated with AppFunctionSerializable must not have an empty " +
                    "primary constructor.",
                this
            )
        }

        if (primaryConstructorDeclaration.getVisibility() != Visibility.PUBLIC) {
            throw ProcessingException(
                "The primary constructor of @AppFunctionSerializable must be public.",
                appFunctionSerializableClass
            )
        }
        return primaryConstructorDeclaration
    }

    private fun validateSerializableParameter(ksValueParameter: KSValueParameter) {
        if (!ksValueParameter.isVal) {
            throw ProcessingException(
                "All parameters in @AppFunctionSerializable primary constructor must have getters",
                ksValueParameter
            )
        }

        if (!isSupportedType(ksValueParameter.type)) {
            throw ProcessingException(
                "AppFunctionSerializable properties must be one of the following types:\n" +
                    SUPPORTED_TYPES_STRING +
                    ", an @AppFunctionSerializable or a list of @AppFunctionSerializable\nbut found " +
                    ksValueParameter.type.toTypeName(),
                ksValueParameter
            )
        }
    }

    /** Returns the annotated class's properties as defined in its primary constructor. */
    fun getProperties(): List<KSValueParameter> {
        return checkNotNull(appFunctionSerializableClass.primaryConstructor).parameters
    }

    /** Returns the properties that have @AppFunctionSerializable class types. */
    fun getSerializablePropertyTypeReferences(): Set<AppFunctionTypeReference> {
        return getProperties()
            .map { param -> AppFunctionTypeReference(param.type) }
            .filter { afType ->
                afType.isOfTypeCategory(SERIALIZABLE_SINGULAR) ||
                    afType.isOfTypeCategory(SERIALIZABLE_LIST)
            }
            .toSet()
    }

    /**
     * Returns the set of source files that contain the definition of [appFunctionSerializableClass]
     * and all @AppFunctionSerializable classes directly reachable through its fields. This method
     * differs from [getTransitiveSerializableSourceFiles] by excluding transitively
     * nested @AppFunctionSerializable classes.
     */
    fun getSerializableSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()
        appFunctionSerializableClass.containingFile?.let { sourceFileSet.add(it) }
        for (serializableAfType in getSerializablePropertyTypeReferences()) {
            val appFunctionSerializableDefinition =
                serializableAfType.selfOrItemTypeReference.resolve().declaration
                    as KSClassDeclaration
            appFunctionSerializableDefinition.containingFile?.let { sourceFileSet.add(it) }
        }
        return sourceFileSet
    }

    /**
     * Returns the set of source files that contain the definition of [appFunctionSerializableClass]
     * and all @AppFunctionSerializable classes transitively reachable through its fields or nested
     * classes.
     */
    fun getTransitiveSerializableSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()
        val visitedSerializableSet: MutableSet<ClassName> = mutableSetOf()

        // Add the file containing the AppFunctionSerializable class definition immediately it's
        // seen
        appFunctionSerializableClass.containingFile?.let { sourceFileSet.add(it) }
        visitedSerializableSet.add(originalClassName)
        traverseSerializableClassSourceFiles(sourceFileSet, visitedSerializableSet)
        return sourceFileSet
    }

    private fun traverseSerializableClassSourceFiles(
        sourceFileSet: MutableSet<KSFile>,
        visitedSerializableSet: MutableSet<ClassName>
    ) {
        for (serializableAfType in getSerializablePropertyTypeReferences()) {
            val appFunctionSerializableDefinition =
                serializableAfType.selfOrItemTypeReference.resolve().declaration
                    as KSClassDeclaration
            // Skip serializable that have been seen before
            if (visitedSerializableSet.contains(originalClassName)) {
                continue
            }
            // Process newly found serializable
            sourceFileSet.addAll(
                AnnotatedAppFunctionSerializable(appFunctionSerializableDefinition)
                    .getTransitiveSerializableSourceFiles()
            )
        }
    }

    val originalClassName: ClassName by lazy {
        ClassName(
            appFunctionSerializableClass.packageName.asString(),
            appFunctionSerializableClass.simpleName.asString()
        )
    }
}
