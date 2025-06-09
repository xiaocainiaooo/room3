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

package androidx.xr.glimmer

import android.graphics.Matrix
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.SurfaceDefaults.Shape
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A surface is a fundamental building block in Glimmer. A surface represents a distinct visual area
 * or 'physical' boundary for components such as buttons and cards. A surface is responsible for:
 * 1) Clipping: a surface clips its children to the shape specified by [shape]
 * 2) Border: a surface draws an inner [border] to emphasize the boundary of the component. When
 *    focused, a surface draws a wider border with a focused highlight on top to indicate the focus
 *    state.
 * 3) Background: a surface has a background color of [color].
 * 4) Content color: a surface provides a [contentColor] for text and icons inside the surface. By
 *    default this is calculated from the provided background color.
 *
 * This surface is focusable by default - set [focusable] to false for un-interactive / decorative
 * surfaces.
 *
 * @sample androidx.xr.glimmer.samples.SurfaceSample
 * @param focusable whether this surface is focusable, true by default. Most surfaces should be
 *   focusable to allow navigation between surfaces in a screen. Unfocusable surfaces may be used
 *   for decorative only elements, such as surfaces used in a compound component with a separate
 *   focusable area.
 * @param shape the [Shape] used to clip this surface, and also used to draw the background and
 *   border
 * @param color the background [Color] for this surface
 * @param contentColor the [Color] for content inside this surface
 * @param border an optional inner border for this surface
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this surface. Note that if `null` is provided, interactions will
 *   still happen internally.
 */
@Composable
public fun Modifier.surface(
    focusable: Boolean = true,
    shape: Shape = SurfaceDefaults.Shape,
    color: Color = GlimmerTheme.colors.surface,
    contentColor: Color = calculateContentColor(color),
    border: BorderStroke? = SurfaceDefaults.border(),
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    return this.clip(shape)
        .then(SurfaceNodeElement(shape, contentColor, border, interactionSource))
        .background(color = color, shape = shape)
        .focusable(enabled = focusable, interactionSource = interactionSource)
}

/** Default values used for [surface]. */
public object SurfaceDefaults {
    /** The default [Shape] used for a [surface] */
    public val Shape: Shape = RoundedCornerShape(40.dp)

    /**
     * Create the default [BorderStroke] used for a [surface]. Use the other overload in order to
     * change the width or color.
     */
    @Composable
    public fun border(): BorderStroke {
        return GlimmerTheme.LocalGlimmerTheme.current.defaultSurfaceBorder
    }

    /**
     * Create the default [BorderStroke] used for a [surface], with optional overrides for [width]
     * and [color].
     *
     * @param width width of the border in [Dp]. Use [Dp.Hairline] for one-pixel border.
     * @param color color to paint the border with
     */
    @Composable
    public fun border(
        width: Dp = DefaultSurfaceBorderWidth,
        color: Color = GlimmerTheme.colors.outline,
    ): BorderStroke {
        return BorderStroke(width, color)
    }

    /** Returns the default (cached) border for a [surface]. */
    internal val GlimmerTheme.defaultSurfaceBorder: BorderStroke
        get() {
            return defaultSurfaceBorderCached
                ?: BorderStroke(DefaultSurfaceBorderWidth, colors.outline).also {
                    defaultSurfaceBorderCached = it
                }
        }
}

/**
 * Retrieves the preferred content color for text and iconography within a [surface]. Most surfaces
 * should be [Color.Black], so content color is typically [Color.White]. In a few cases where
 * surfaces are filled with a different color, the content color may be [Color.Black] to improve
 * contrast. For cases where higher emphasis is required, content color may be a different color
 * from the theme, such as [Colors.primary].
 *
 * Content color is automatically provided by [surface], and calculated from the provided background
 * color by default. To manually calculate the default content color for a provided background
 * color, use [calculateContentColor].
 */
internal fun DelegatableNode.currentContentColor(): Color {
    var contentColor = Color.White
    traverseAncestors(SurfaceNodeTraverseKey) {
        if (it is SurfaceNode) {
            contentColor = it.contentColor
            // Stop at the nearest descendant surface
            false
        } else {
            // Theoretically someone else could define the same traverse key, so continue just to be
            // safe
            true
        }
    }
    return contentColor
}

/**
 * Surface node responsible for providing content color, drawing the border, and drawing the focused
 * border and highlight.
 */
private class SurfaceNodeElement(
    private val shape: Shape,
    private val contentColor: Color,
    private val border: BorderStroke?,
    private val interactionSource: InteractionSource?,
) : ModifierNodeElement<SurfaceNode>() {
    override fun create(): SurfaceNode = SurfaceNode(shape, contentColor, border, interactionSource)

    override fun update(node: SurfaceNode) =
        node.update(shape, contentColor, border, interactionSource)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SurfaceNodeElement) return false

        if (shape != other.shape) return false
        if (contentColor != other.contentColor) return false
        if (border != other.border) return false
        if (interactionSource != other.interactionSource) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + (border?.hashCode() ?: 0)
        result = 31 * result + (interactionSource?.hashCode() ?: 0)
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "surface"
        properties["shape"] = shape
        properties["contentColor"] = contentColor
        properties["border"] = border
        properties["interactionSource"] = interactionSource
    }
}

private class SurfaceNode(
    private var shape: Shape,
    contentColor: Color,
    private var border: BorderStroke?,
    private var interactionSource: InteractionSource?,
) : TraversableNode, DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate = false

    var contentColor by mutableStateOf(contentColor)
        private set

    // Cached border properties

    private val unfocusedBorderLogic: BorderLogic = BorderLogic()
    private var focusedBorderLogic: BorderLogic? = null
    private var focusedHighlightBorderLogic: BorderLogic? = null

    // Cache borders and highlight for unfocused and focused states. This means we
    // can avoid recreating these for a given surface, if the border and shape never
    // change. Changing between unfocused and focus states only requires a draw
    // invalidation, as the borders are already cached.

    // Unfocused border
    var drawUnfocusedBorder: (DrawScope.() -> Unit)? = null

    // Focused border - this consists of two layers. A 'base' layer (which is the
    // unfocused border with a different size) and the highlight we draw on top of
    // this base layer. We need to increase the size of the underlying border to
    // make sure that the highlight area fully matches the underlying border, to
    // avoid inconsistent areas of coverage due to the transparency of the
    // highlight.
    var drawFocusedBaseBorder: (DrawScope.() -> Unit)? = null
    var drawFocusedHighlight: (DrawScope.() -> Unit)? = null

    // Highlight shader / brush. We use the same shader and brush instance and just
    // set a rotation matrix on the shader to animate it when focused.
    var shader: Shader? = null
    var shaderBrush: Brush? = null
    var shaderMatrix: Matrix? = null

    private var interactionCollectionJob: Job? = null

    private var rotationProgress: Animatable<Float, AnimationVector1D>? = null
    private var animationJob: Job? = null

    fun update(
        shape: Shape,
        contentColor: Color,
        border: BorderStroke?,
        interactionSource: InteractionSource?,
    ) {
        if (this.shape != shape) {
            this.shape = shape
            invalidateBorderCaches()
        }
        this.contentColor = contentColor
        if (this.border != border) {
            this.border = border
            invalidateBorderCaches()
        }
        if (this.interactionSource != interactionSource) {
            this.interactionSource = interactionSource
            observeInteractions()
        }
    }

    override fun onAttach() {
        observeInteractions()
    }

    var isFocused = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    startAnimation()
                } else {
                    stopAnimation()
                }
                // No need to invalidate the border cache - we build it ahead of time to account for
                // focus changes. Just invalidate draw so we can switch to drawing the correct
                // border.
                invalidateDraw()
            }
        }

    private fun observeInteractions() {
        interactionCollectionJob?.cancel()
        interactionCollectionJob = null
        isFocused = false
        interactionSource?.let { source ->
            interactionCollectionJob =
                coroutineScope.launch {
                    source.interactions.collect {
                        if (it is FocusInteraction.Focus) {
                            isFocused = true
                        }
                        if (it is FocusInteraction.Unfocus) {
                            isFocused = false
                        }
                    }
                }
        }
    }

    private fun startAnimation() {
        stopAnimation()
        rotationProgress = Animatable(0f)
        animationJob =
            coroutineScope.launch {
                rotationProgress?.animateTo(
                    targetValue = 1f,
                    animationSpec = FocusedHighlightAnimationSpec,
                )
            }
    }

    private fun stopAnimation() {
        animationJob?.cancel()
        rotationProgress = null
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (border != null) {
            val border = border!!
            if (isFocused) {
                shader = shader ?: focusedHighlightShader(size)
                shaderBrush = shaderBrush ?: ShaderBrush(shader!!)
                shaderMatrix = shaderMatrix ?: Matrix().also { shader!!.getLocalMatrix(it) }
                shaderMatrix!!.setRotate(
                    (rotationProgress?.value ?: 1f) * 360f,
                    size.width / 2,
                    size.height / 2,
                )
                shader!!.setLocalMatrix(shaderMatrix)
                focusedBorderLogic = focusedBorderLogic ?: BorderLogic()
                focusedHighlightBorderLogic = focusedHighlightBorderLogic ?: BorderLogic()
                drawFocusedBaseBorder =
                    drawFocusedBaseBorder
                        ?: focusedBorderLogic!!.createDrawBorder(
                            this,
                            FocusedSurfaceBorderWidth,
                            border.brush,
                            shape,
                        )
                drawFocusedHighlight =
                    drawFocusedHighlight
                        ?: focusedHighlightBorderLogic!!.createDrawBorder(
                            this,
                            FocusedSurfaceBorderWidth,
                            shaderBrush!!,
                            shape,
                        )
                drawFocusedBaseBorder!!()
                drawFocusedHighlight!!()
            } else {
                drawUnfocusedBorder =
                    drawUnfocusedBorder
                        ?: unfocusedBorderLogic.createDrawBorder(
                            this,
                            border.width,
                            border.brush,
                            shape,
                        )
                drawUnfocusedBorder!!()
            }
        }
    }

    // Invalidation for border caches

    override fun onMeasureResultChanged() {
        invalidateBorderCaches()
    }

    override fun onDensityChange() {
        invalidateBorderCaches()
    }

    override fun onLayoutDirectionChange() {
        invalidateBorderCaches()
    }

    private fun invalidateBorderCaches() {
        drawUnfocusedBorder = null
        drawFocusedBaseBorder = null
        drawFocusedHighlight = null
        shader = null
        shaderBrush = null
        shaderMatrix = null
        invalidateDraw()
    }

    override val traverseKey: String = SurfaceNodeTraverseKey
}

/** @return the [Shader] used to render the highlight on top of the border when focused */
private fun focusedHighlightShader(size: Size): Shader {
    return LinearGradientShader(
        colors = FocusedHighlightColors,
        colorStops = FocusedHighlightColorStops,
        from = Offset.Zero,
        to = Offset(size.width, size.height),
    )
}

/** Default border width for a [surface]. */
private val DefaultSurfaceBorderWidth = 2.dp

/** Focused border width for a [surface]. */
private val FocusedSurfaceBorderWidth = 5.dp

@Suppress("PrimitiveInCollection")
private val FocusedHighlightColors =
    listOf(
        Color.White.copy(alpha = 0.8f),
        Color.White.copy(alpha = 0f),
        Color.White.copy(alpha = 0f),
        Color.White.copy(alpha = 0.2f),
    )

@Suppress("PrimitiveInCollection")
private val FocusedHighlightColorStops = listOf(0f, 0.3f, 0.66f, 1f)

private val FocusedHighlightAnimationSpec: AnimationSpec<Float> =
    tween(durationMillis = 7000, easing = LinearOutSlowInEasing)

private const val SurfaceNodeTraverseKey = "androidx.xr.glimmer.SurfaceNode"
