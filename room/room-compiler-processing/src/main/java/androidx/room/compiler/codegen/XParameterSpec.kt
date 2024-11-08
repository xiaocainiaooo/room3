/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.compiler.codegen

import androidx.room.compiler.codegen.impl.XParameterSpecImpl
import androidx.room.compiler.codegen.java.JavaParameterSpec
import androidx.room.compiler.codegen.java.NONNULL_ANNOTATION
import androidx.room.compiler.codegen.java.NULLABLE_ANNOTATION
import androidx.room.compiler.codegen.kotlin.KotlinParameterSpec
import androidx.room.compiler.processing.XNullability
import javax.lang.model.element.Modifier

interface XParameterSpec {
    val name: String

    interface Builder {
        fun addAnnotation(annotation: XAnnotationSpec): Builder

        fun addAnnotations(annotations: List<XAnnotationSpec>) = apply {
            annotations.forEach { addAnnotation(it) }
        }

        fun build(): XParameterSpec
    }

    companion object {
        @JvmStatic
        fun of(name: String, typeName: XTypeName) = builder(XName.of(name), typeName).build()

        @JvmStatic fun of(name: XName, typeName: XTypeName) = builder(name, typeName).build()

        @JvmStatic
        fun builder(name: String, typeName: XTypeName) = builder(XName.of(name), typeName)

        @JvmStatic
        fun builder(name: XName, typeName: XTypeName): Builder {
            return XParameterSpecImpl.Builder(
                JavaParameterSpec.Builder(
                    JParameterSpec.builder(typeName.java, name.java, Modifier.FINAL).apply {
                        // Adding nullability annotation to primitive parameters is redundant as
                        // primitives can never be null.
                        if (!typeName.isPrimitive) {
                            when (typeName.nullability) {
                                XNullability.NULLABLE -> addAnnotation(NULLABLE_ANNOTATION)
                                XNullability.NONNULL -> addAnnotation(NONNULL_ANNOTATION)
                                XNullability.UNKNOWN -> {}
                            }
                        }
                    }
                ),
                KotlinParameterSpec.Builder(KParameterSpec.builder(name.kotlin, typeName.kotlin))
            )
        }
    }
}
