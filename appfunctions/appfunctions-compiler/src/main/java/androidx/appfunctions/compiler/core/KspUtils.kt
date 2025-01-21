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

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Checks if the type reference is of the given type.
 *
 * @param type the type to check against
 * @return true if the type reference is of the given type
 * @throws ProcessingException If unable to resolve the type.
 */
fun KSTypeReference.isOfType(type: ClassName): Boolean {
    val typeName =
        resolveTypeName()
            ?: throw ProcessingException(
                "Unable to resolve the type to check if it is of type [${type}]",
                this
            )
    return typeName.asString() == type.canonicalName
}

/**
 * Finds and returns an annotation of [annotationClass] type.
 *
 * @param annotationClass the annotation class to find
 */
fun Sequence<KSAnnotation>.findAnnotation(annotationClass: ClassName): KSAnnotation? =
    this.singleOrNull() {
        val shortName = it.shortName.getShortName()
        if (shortName != annotationClass.simpleName) {
            false
        } else {
            val typeName =
                it.annotationType.resolveTypeName()
                    ?: throw ProcessingException(
                        "Unable to resolve type for [$shortName]",
                        it.annotationType
                    )
            typeName.asString() == annotationClass.canonicalName
        }
    }

private fun KSTypeReference.resolveTypeName(): KSName? = resolve().declaration.qualifiedName

/** Returns the value of the annotation property if found. */
fun <T : Any> KSAnnotation.requirePropertyValueOfType(
    propertyName: String,
    expectedType: KClass<T>,
): T {
    val propertyValue =
        this.arguments.singleOrNull { it.name?.asString() == propertyName }?.value
            ?: throw ProcessingException("Unable to find property with name: $propertyName", this)
    return expectedType.cast(propertyValue)
}
