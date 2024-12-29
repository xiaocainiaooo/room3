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

import androidx.wear.protolayout.DimensionBuilders.ContainerDimension
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.material3.ButtonDefaults.DEFAULT_CONTENT_PADDING_DP
import androidx.wear.protolayout.material3.ButtonDefaults.IMAGE_BUTTON_DEFAULT_SIZE_DP
import androidx.wear.protolayout.material3.ButtonDefaults.METADATA_TAG_BUTTON
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.types.LayoutColor

/**
 * ProtoLayout Material3 clickable component button that offers a single slot to take any content.
 *
 * The button is usually stadium or circle shaped with fully rounded corners by default. It is
 * highly recommended to set its width and height to fill the available space, by [expand] or
 * [weight] for optimal experience across different screen sizes, and use [buttonGroup] to arrange
 * them.
 *
 * It can be used for displaying any clickable container with additional data, text or images.
 *
 * This button can also be used to create image button that only has a background image and no inner
 * content, see [androidx.wear.protolayout.material3.samples.imageButtonSample]
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param shape Defines the button's shape, in other words the corner radius for this button.
 * @param backgroundColor The color to be used as a background of this button. If the background
 *   image is also specified, the image will be laid out on top of this color. In case of the fully
 *   opaque background image, then this background color will not be shown.
 * @param background The background object to be used behind the content in the button. It is
 *   recommended to use the default styling that is automatically provided by only calling
 *   [backgroundImage] with the content. It can be combined with the specified [backgroundColor]
 *   behind it.
 * @param width The width of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @param height The height of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @param contentPadding The inner padding used to prevent inner content from being too close to the
 *   button's edge. It's highly recommended to keep the default.
 * @param content The inner content to be put inside of this button.
 * @sample androidx.wear.protolayout.material3.samples.buttonSample
 * @sample androidx.wear.protolayout.material3.samples.imageButtonSample
 */
// TODO: b/346958146 - Link Button visuals in DAC
// TODO: b/373578620 - Add how corners affects margins in the layout.
public fun MaterialScope.button(
    onClick: Clickable,
    modifier: LayoutModifier = LayoutModifier,
    content: (MaterialScope.() -> LayoutElement)? = null,
    width: ContainerDimension =
        if (content == null) IMAGE_BUTTON_DEFAULT_SIZE_DP.toDp()
        else wrapWithMinTapTargetDimension(),
    height: ContainerDimension =
        if (content == null) IMAGE_BUTTON_DEFAULT_SIZE_DP.toDp()
        else wrapWithMinTapTargetDimension(),
    shape: Corner = shapes.full,
    backgroundColor: LayoutColor? = null,
    background: (MaterialScope.() -> LayoutElement)? = null,
    contentPadding: Padding = Padding.Builder().setAll(DEFAULT_CONTENT_PADDING_DP.toDp()).build()
): LayoutElement =
    componentContainer(
        onClick = onClick,
        modifier = modifier,
        width = width,
        height = height,
        shape = shape,
        backgroundColor = backgroundColor,
        background = background,
        contentPadding = contentPadding,
        metadataTag = METADATA_TAG_BUTTON,
        content = content
    )
