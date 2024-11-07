/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.codegen.kotlin

import androidx.room.compiler.codegen.KFunSpec
import androidx.room.compiler.codegen.KFunSpecBuilder
import androidx.room.compiler.codegen.KParameterSpec
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XName
import androidx.room.compiler.codegen.XSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.impl.XAnnotationSpecImpl
import androidx.room.compiler.codegen.impl.XCodeBlockImpl
import com.squareup.kotlinpoet.KModifier

internal class KotlinFunSpec(internal val actual: KFunSpec) : XSpec(), XFunSpec {
    override val name: XName = XName.of(actual.name)

    internal class Builder(internal val actual: KFunSpecBuilder) :
        XSpec.Builder(), XFunSpec.Builder {

        override fun addAnnotation(annotation: XAnnotationSpec) = apply {
            require(annotation is XAnnotationSpecImpl)
            actual.addAnnotation(annotation.kotlin.actual)
        }

        override fun addAbstractModifier() = apply { actual.addModifiers(KModifier.ABSTRACT) }

        override fun addCode(code: XCodeBlock) = apply {
            require(code is XCodeBlockImpl)
            actual.addCode(code.kotlin.actual)
        }

        override fun addParameter(
            typeName: XTypeName,
            name: XName,
            annotations: List<XAnnotationSpec>
        ) = apply {
            actual.addParameter(
                KParameterSpec.builder(name.kotlin, typeName.kotlin)
                    .apply {
                        // TODO(b/247247439): Add other annotations
                    }
                    .build()
            )
        }

        override fun callSuperConstructor(vararg args: XCodeBlock) = apply {
            actual.callSuperConstructor(
                args.map {
                    require(it is XCodeBlockImpl)
                    it.kotlin.actual
                }
            )
        }

        override fun returns(typeName: XTypeName) = apply { actual.returns(typeName.kotlin) }

        override fun build() = KotlinFunSpec(actual.build())
    }
}

internal fun VisibilityModifier.toKotlinVisibilityModifier() =
    when (this) {
        VisibilityModifier.PUBLIC -> KModifier.PUBLIC
        VisibilityModifier.PROTECTED -> KModifier.PROTECTED
        VisibilityModifier.INTERNAL -> KModifier.INTERNAL
        VisibilityModifier.PRIVATE -> KModifier.PRIVATE
    }
