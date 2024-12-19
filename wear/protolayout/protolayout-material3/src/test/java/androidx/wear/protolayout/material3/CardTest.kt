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

package androidx.wear.protolayout.material3

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.testing.LayoutElementAssertionsProvider
import androidx.wear.protolayout.testing.hasClickable
import androidx.wear.protolayout.testing.hasColor
import androidx.wear.protolayout.testing.hasContentDescription
import androidx.wear.protolayout.testing.hasHeight
import androidx.wear.protolayout.testing.hasImage
import androidx.wear.protolayout.testing.hasTag
import androidx.wear.protolayout.testing.hasText
import androidx.wear.protolayout.testing.hasWidth
import androidx.wear.protolayout.types.argb
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class CardTest {
    @Test
    fun containerCard_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_CONTAINER_CARD_WITH_TEXT)
            .onRoot()
            .assert(hasWidth(wrapWithMinTapTargetDimension()))
            .assert(hasHeight(wrapWithMinTapTargetDimension()))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun titleCard_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_TITLE_CARD_WITH_TEXT)
            .onRoot()
            .assert(hasWidth(expand()))
            .assert(hasHeight(wrapWithMinTapTargetDimension()))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun appCard_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_APP_CARD_WITH_TEXT)
            .onRoot()
            .assert(hasWidth(expand()))
            .assert(hasHeight(wrapWithMinTapTargetDimension()))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun containerCard_hasContentDescription() {
        LayoutElementAssertionsProvider(DEFAULT_CONTAINER_CARD_WITH_TEXT)
            .onRoot()
            .assert(hasContentDescription(CONTENT_DESCRIPTION))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun containerCard_hasClickable() {
        LayoutElementAssertionsProvider(DEFAULT_CONTAINER_CARD_WITH_TEXT)
            .onRoot()
            .assert(hasClickable(CLICKABLE))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun containerCard_hasContent_asText() {
        LayoutElementAssertionsProvider(DEFAULT_CONTAINER_CARD_WITH_TEXT)
            .onElement(hasText(TEXT))
            .assertExists()
    }

    @Test
    fun titleCard_hasTitle_asText() {
        LayoutElementAssertionsProvider(DEFAULT_TITLE_CARD_WITH_TEXT)
            .onElement(hasText(TEXT))
            .assertExists()
    }

    @Test
    fun appCard_hasTitle_asText() {
        LayoutElementAssertionsProvider(DEFAULT_APP_CARD_WITH_TEXT)
            .onElement(hasText(TEXT))
            .assertExists()
    }

    @Test
    fun titleCard_hasContent_asText() {
        LayoutElementAssertionsProvider(DEFAULT_TITLE_CARD_WITH_TEXT)
            .onElement(hasText(TEXT2))
            .assertExists()
    }

    @Test
    fun appCard_hasContent_asText() {
        LayoutElementAssertionsProvider(DEFAULT_APP_CARD_WITH_TEXT)
            .onElement(hasText(TEXT2))
            .assertExists()
    }

    @Test
    fun titleCard_hasTime_asText() {
        LayoutElementAssertionsProvider(DEFAULT_TITLE_CARD_WITH_TEXT)
            .onElement(hasText(TEXT3))
            .assertExists()
    }

    @Test
    fun appCard_hasTime_asText() {
        LayoutElementAssertionsProvider(DEFAULT_APP_CARD_WITH_TEXT)
            .onElement(hasText(TEXT3))
            .assertExists()
    }

    @Test
    fun appCard_hasLabel_asText() {
        LayoutElementAssertionsProvider(DEFAULT_APP_CARD_WITH_TEXT)
            .onElement(hasText(TEXT4))
            .assertExists()
    }

    @Test
    fun appCard_hasAvatar() {
        LayoutElementAssertionsProvider(DEFAULT_APP_CARD_WITH_TEXT)
            .onElement(hasImage(AVATAR_ID))
            .assertExists()
    }

    @Test
    fun containerCard_hasBackgroundImage() {
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                card(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    background = { backgroundImage(IMAGE_ID) }
                ) {
                    text(TEXT.prop())
                }
            }

        LayoutElementAssertionsProvider(card).onElement(hasImage(IMAGE_ID)).assertExists()
        LayoutElementAssertionsProvider(card).onRoot().assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun containerCard_hasBackgroundColor() {
        val color = Color.YELLOW
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                card(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    backgroundColor = color.argb
                ) {
                    text(TEXT.prop())
                }
            }

        LayoutElementAssertionsProvider(card).onRoot().assert(hasColor(color))
        LayoutElementAssertionsProvider(card).onRoot().assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun titleCard_hasColors() {
        val titleColor = Color.YELLOW
        val contentColor = Color.MAGENTA
        val timeColor = Color.CYAN
        val backgroundColor = Color.BLUE
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                titleCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    colors =
                        CardColors(
                            background = backgroundColor.argb,
                            title = titleColor.argb,
                            content = contentColor.argb,
                            time = timeColor.argb
                        ),
                    title = { text(TEXT.prop()) },
                    content = { text(TEXT2.prop()) },
                    time = { text(TEXT3.prop()) },
                )
            }

        LayoutElementAssertionsProvider(card).onRoot().assert(hasColor(backgroundColor))
        LayoutElementAssertionsProvider(card).onElement(hasText(TEXT)).assert(hasColor(titleColor))
        LayoutElementAssertionsProvider(card)
            .onElement(hasText(TEXT2))
            .assert(hasColor(contentColor))
        LayoutElementAssertionsProvider(card).onElement(hasText(TEXT3)).assert(hasColor(timeColor))
        LayoutElementAssertionsProvider(card).onRoot().assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun appCard_hasColors() {
        val titleColor = Color.YELLOW
        val contentColor = Color.MAGENTA
        val timeColor = Color.CYAN
        val labelColor = Color.GREEN
        val backgroundColor = Color.BLUE
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                appCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    colors =
                        CardColors(
                            background = backgroundColor.argb,
                            title = titleColor.argb,
                            content = contentColor.argb,
                            time = timeColor.argb,
                            label = labelColor.argb
                        ),
                    title = { text(TEXT.prop()) },
                    content = { text(TEXT2.prop()) },
                    time = { text(TEXT3.prop()) },
                    label = { text(TEXT4.prop()) },
                )
            }

        LayoutElementAssertionsProvider(card).onRoot().assert(hasColor(backgroundColor))
        LayoutElementAssertionsProvider(card).onElement(hasText(TEXT)).assert(hasColor(titleColor))
        LayoutElementAssertionsProvider(card)
            .onElement(hasText(TEXT2))
            .assert(hasColor(contentColor))
        LayoutElementAssertionsProvider(card).onElement(hasText(TEXT3)).assert(hasColor(timeColor))
        LayoutElementAssertionsProvider(card).onElement(hasText(TEXT4)).assert(hasColor(labelColor))
        LayoutElementAssertionsProvider(card).onRoot().assert(hasTag(CardDefaults.METADATA_TAG))
    }

    // TODO: b/381518061 - Add test for corner shape.

    @Test
    fun containerCard_hasGivenSize() {
        val height = 12
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                card(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    width = expand(),
                    height = height.toDp()
                ) {
                    text(TEXT.prop())
                }
            }

        LayoutElementAssertionsProvider(card)
            .onRoot()
            .assert(hasWidth(expand()))
            .assert(hasHeight((height.toDp())))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    companion object {
        private val CONTEXT = getApplicationContext() as Context

        private val DEVICE_CONFIGURATION =
            DeviceParametersBuilders.DeviceParameters.Builder()
                .setScreenWidthDp(192)
                .setScreenHeightDp(192)
                .build()

        private val CLICKABLE = clickable("id")

        private const val CONTENT_DESCRIPTION = "This is a card"

        private const val IMAGE_ID = "image"

        private const val TEXT = "Container card"
        private const val TEXT2 = "Description"
        private const val TEXT3 = "Now"
        private const val TEXT4 = "Label"
        private const val AVATAR_ID = "id"

        private val DEFAULT_CONTAINER_CARD_WITH_TEXT =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                card(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION)
                ) {
                    text(TEXT.prop())
                }
            }

        private val DEFAULT_TITLE_CARD_WITH_TEXT =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                titleCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    title = { text(TEXT.prop()) },
                    content = { text(TEXT2.prop()) },
                    time = { text(TEXT3.prop()) },
                )
            }

        private val DEFAULT_APP_CARD_WITH_TEXT =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                appCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    title = { text(TEXT.prop()) },
                    content = { text(TEXT2.prop()) },
                    time = { text(TEXT3.prop()) },
                    avatar = { avatarImage(AVATAR_ID) },
                    label = { text(TEXT4.prop()) }
                )
            }
    }
}
