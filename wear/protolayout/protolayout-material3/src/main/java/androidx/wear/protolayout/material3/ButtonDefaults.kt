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

import android.graphics.Color
import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.material3.ButtonDefaults.DEFAULT_CONTENT_PADDING_DP
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.argb

/**
 * Represents the container and content colors used in buttons, such as [textEdgeButton] or
 * [iconEdgeButton].
 */
public class ButtonColors(
    /** The container color to be used for a button. */
    public val container: LayoutColor = Color.BLACK.argb,
    /** The icon tint color to be used for a button. */
    public val icon: LayoutColor = Color.BLACK.argb,
    /** The label color to be used for a button. */
    public val label: LayoutColor = Color.BLACK.argb,
)

public object ButtonDefaults {
    /**
     * [ButtonColors] for the high-emphasis button representing the primary, most important or most
     * common action on a screen.
     *
     * These colors are using [ColorScheme.primary] for background color and [ColorScheme.onPrimary]
     * for content color.
     */
    public fun MaterialScope.filledButtonColors(): ButtonColors =
        ButtonColors(
            container = theme.colorScheme.primary,
            icon = theme.colorScheme.onPrimary,
            label = theme.colorScheme.onPrimary
        )

    /**
     * [ButtonColors] for the medium-emphasis button.
     *
     * These colors are using [ColorScheme.surfaceContainer] for background color,
     * [ColorScheme.onSurface] for content color and [ColorScheme.primary] for icon.
     */
    public fun MaterialScope.filledTonalButtonColors(): ButtonColors =
        ButtonColors(
            container = theme.colorScheme.surfaceContainer,
            icon = theme.colorScheme.primary,
            label = theme.colorScheme.onSurface
        )

    /**
     * Alternative [ButtonColors] for the high-emphasis button.
     *
     * These colors are using [ColorScheme.primaryContainer] for background color and
     * [ColorScheme.onPrimaryContainer] for content color.
     */
    public fun MaterialScope.filledVariantButtonColors(): ButtonColors =
        ButtonColors(
            container = theme.colorScheme.primaryContainer,
            icon = theme.colorScheme.onPrimaryContainer,
            label = theme.colorScheme.onPrimaryContainer
        )

    internal const val METADATA_TAG_BUTTON: String = "BTN"
    @Dimension(DP) internal const val DEFAULT_CONTENT_PADDING_DP: Int = 8
    @Dimension(DP) internal const val IMAGE_BUTTON_DEFAULT_SIZE_DP = 52
}

/** Provides style values for the icon button component. */
public class IconButtonStyle
internal constructor(
    @Dimension(unit = DP) internal val iconSize: Int,
    internal val innerPadding: Padding = DEFAULT_CONTENT_PADDING_DP.toPadding()
) {
    public companion object {
        /**
         * Default style variation for the [iconButton] where all opinionated inner content is
         * displayed in a medium size.
         */
        public fun defaultIconButtonStyle(): IconButtonStyle = IconButtonStyle(26)

        /**
         * Default style variation for the [iconButton] where all opinionated inner content is
         * displayed in a large size.
         */
        public fun largeIconButtonStyle(): IconButtonStyle = IconButtonStyle(32)
    }
}
