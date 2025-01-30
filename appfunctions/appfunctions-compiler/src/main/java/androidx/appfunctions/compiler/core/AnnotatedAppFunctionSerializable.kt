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

import com.google.devtools.ksp.symbol.KSClassDeclaration

/** Represents a class annotated with [androidx.appfunctions.AppFunctionSerializable]. */
data class AnnotatedAppFunctionSerializable(val classDeclaration: KSClassDeclaration) {
    fun validate(): AnnotatedAppFunctionSerializable {
        val parameters = classDeclaration.primaryConstructor?.parameters
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

            if (!ksValueParameter.type.isSupportedType()) {
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
}
