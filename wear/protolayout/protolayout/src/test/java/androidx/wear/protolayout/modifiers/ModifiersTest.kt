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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.ModifiersBuilders.SEMANTICS_ROLE_BUTTON
import androidx.wear.protolayout.ModifiersBuilders.SEMANTICS_ROLE_NONE
import androidx.wear.protolayout.expression.DynamicBuilders
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModifiersTest {

    @Test
    fun contentDescription_toModifier() {
        val modifiers =
            LayoutModifier.contentDescription(
                    STATIC_CONTENT_DESCRIPTION,
                    DYNAMIC_CONTENT_DESCRIPTION
                )
                .toProtoLayoutModifiers()

        assertThat(modifiers.semantics?.contentDescription?.value)
            .isEqualTo(STATIC_CONTENT_DESCRIPTION)
        assertThat(modifiers.semantics?.contentDescription?.dynamicValue?.toDynamicStringProto())
            .isEqualTo(DYNAMIC_CONTENT_DESCRIPTION.toDynamicStringProto())
        assertThat(modifiers.semantics?.role).isEqualTo(SEMANTICS_ROLE_NONE)
    }

    @Test
    fun semanticsRole_toModifier() {
        val modifiers = LayoutModifier.semanticsRole(SEMANTICS_ROLE_BUTTON).toProtoLayoutModifiers()

        assertThat(modifiers.semantics?.role).isEqualTo(SEMANTICS_ROLE_BUTTON)
        assertThat(modifiers.semantics?.contentDescription).isNull()
    }

    @Test
    fun contentDescription_semanticRole_toModifier() {
        val modifiers =
            LayoutModifier.contentDescription(
                    STATIC_CONTENT_DESCRIPTION,
                    DYNAMIC_CONTENT_DESCRIPTION
                )
                .semanticsRole(SEMANTICS_ROLE_BUTTON)
                .toProtoLayoutModifiers()

        assertThat(modifiers.semantics?.contentDescription?.value)
            .isEqualTo(STATIC_CONTENT_DESCRIPTION)
        assertThat(modifiers.semantics?.contentDescription?.dynamicValue?.toDynamicStringProto())
            .isEqualTo(DYNAMIC_CONTENT_DESCRIPTION.toDynamicStringProto())
        assertThat(modifiers.semantics?.role).isEqualTo(SEMANTICS_ROLE_BUTTON)
    }

    companion object {
        const val STATIC_CONTENT_DESCRIPTION = "content desc"
        val DYNAMIC_CONTENT_DESCRIPTION = DynamicBuilders.DynamicString.constant("dynamic content")
    }
}
