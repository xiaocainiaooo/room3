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

import androidx.appfunctions.compiler.core.AnnotatedAppFunctions.Companion.SUPPORTED_TYPES
import androidx.appfunctions.compiler.core.AnnotatedAppFunctions.Companion.getTypeNameAsString
import androidx.appfunctions.compiler.core.AnnotatedAppFunctions.Companion.isAppFunctionSerializableType
import androidx.appfunctions.compiler.core.AnnotatedAppFunctions.Companion.isSupportedType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName

/** Represents a class annotated with [androidx.appfunctions.AppFunctionSerializable]. */
data class AnnotatedAppFunctionSerializable(val appFunctionSerializableClass: KSClassDeclaration) {
    fun validate(): AnnotatedAppFunctionSerializable {
        val parameters = appFunctionSerializableClass.primaryConstructor?.parameters
        if (parameters == null) {
            // No parameters to validate.
            return this
        }

        for (ksValueParameter in parameters) {
            if (!ksValueParameter.isVal) {
                throw ProcessingException(
                    "All parameters in @AppFunctionSerializable primary constructor must have getters",
                    ksValueParameter
                )
            }

            if (!isSupportedType(ksValueParameter.type)) {
                throw ProcessingException(
                    "AppFunctionSerializable properties must be one of the following types:\n" +
                        SUPPORTED_TYPES.joinToString(",") +
                        ", an @AppFunctionSerializable or a list of @AppFunctionSerializable\nbut found " +
                        ksValueParameter.type.getTypeNameAsString(),
                    ksValueParameter
                )
            }
        }
        return this
    }

    /**
     * Returns the set of source files that contain the definition of the
     * [appFunctionSerializableClass] and all the @AppFunctionSerializable classes that it contains.
     */
    fun getSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()
        val visitedSerializableSet: MutableSet<ClassName> = mutableSetOf()

        // Add the file containing the AppFunctionSerializable class definition immediately it's
        // seen
        appFunctionSerializableClass.containingFile?.let { sourceFileSet.add(it) }
        visitedSerializableSet.add(appFunctionSerializableClass.getClassName())
        traverseSerializableClassSourceFiles(
            appFunctionSerializableClass,
            sourceFileSet,
            visitedSerializableSet
        )
        return sourceFileSet
    }

    private fun traverseSerializableClassSourceFiles(
        serializableClassDefinition: KSClassDeclaration,
        sourceFileSet: MutableSet<KSFile>,
        visitedSerializableSet: MutableSet<ClassName>
    ) {
        val parameters: List<KSValueParameter> =
            serializableClassDefinition.primaryConstructor?.parameters ?: emptyList()
        for (ksValueParameter in parameters) {
            if (isAppFunctionSerializableType(ksValueParameter.type)) {
                val appFunctionSerializableDefinition =
                    ksValueParameter.type.resolve().declaration as KSClassDeclaration
                // Skip serializable that have been seen before
                if (
                    visitedSerializableSet.contains(
                        appFunctionSerializableDefinition.getClassName()
                    )
                ) {
                    continue
                }
                // Process newly found serializable
                sourceFileSet.addAll(
                    AnnotatedAppFunctionSerializable(appFunctionSerializableDefinition)
                        .getSourceFiles()
                )
            }
        }
    }

    private fun KSClassDeclaration.getClassName(): ClassName {
        return ClassName(packageName.asString(), simpleName.asString())
    }
}
