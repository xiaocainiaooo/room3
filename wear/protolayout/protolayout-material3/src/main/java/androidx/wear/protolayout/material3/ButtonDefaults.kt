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
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.ColorBuilders.argb

/**
 * Represents the container and content colors used in buttons, such as [textEdgeButton] or
 * [iconEdgeButton].
 */
public class ButtonColors(
    /** The container color to be used for a button. */
    public val container: ColorProp = argb(Color.BLACK),
    /** The icon tint color to be used for a button. */
    public val icon: ColorProp = argb(Color.BLACK),
    /** The label color to be used for a button. */
    public val label: ColorProp = argb(Color.BLACK),
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
}
