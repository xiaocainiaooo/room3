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

import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName

/**
 * Checks if the type reference is of the given type.
 *
 * @param type the type to check against
 * @return true if the type reference is of the given type
 * @throws ProcessingException If unable to resolve the type.
 */
fun KSTypeReference.isOfType(type: ClassName): Boolean {
    val ksType = this.resolve()
    val typeName =
        ksType.declaration.qualifiedName
            ?: throw ProcessingException(
                "Unable to resolve the type to check if it is of type [${type}]",
                this
            )
    return typeName.asString() == type.canonicalName
}
