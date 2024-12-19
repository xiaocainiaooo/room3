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

package androidx.wear.protolayout.modifiers

import androidx.annotation.RestrictTo
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Semantics

/** Creates a [ModifiersBuilders.Modifiers] from a [LayoutModifier]. */
fun LayoutModifier.toProtoLayoutModifiers(): ModifiersBuilders.Modifiers =
    toProtoLayoutModifiersBuilder().build()

// TODO: b/384921198 - Remove when M3 elements can use LayoutModifier chain for everything.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
/** Creates a [ModifiersBuilders.Modifiers.Builder] from a [LayoutModifier]. */
fun LayoutModifier.toProtoLayoutModifiersBuilder(): ModifiersBuilders.Modifiers.Builder {
    data class AccumulatingModifier(val semantics: Semantics.Builder? = null)

    val accumulatingModifier =
        this.foldIn(AccumulatingModifier()) { acc, e ->
            when (e) {
                is BaseSemanticElement -> AccumulatingModifier(semantics = e.foldIn(acc.semantics))
                else -> acc
            }
        }

    return ModifiersBuilders.Modifiers.Builder().apply {
        accumulatingModifier.semantics?.let { setSemantics(it.build()) }
    }
}
