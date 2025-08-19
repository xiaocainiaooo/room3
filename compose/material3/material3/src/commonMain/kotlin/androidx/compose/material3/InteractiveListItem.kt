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

package androidx.compose.material3

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.internal.heightOrZero
import androidx.compose.material3.internal.subtractConstraintSafely
import androidx.compose.material3.internal.widthOrZero
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

/** TODO: docs */
@ExperimentalMaterial3ExpressiveApi
@Composable
internal fun ClickableListItem(
    modifier: Modifier = Modifier,

    // slots
    headlineContent: @Composable () -> Unit, // TODO: should this be `content` trailing lambda?
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,

    // interaction
    onClick: () -> Unit,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,

    // styling
    contentPadding: PaddingValues = InteractiveListPadding,
) {
    InteractiveListItem(
        modifier = modifier,
        headlineContent = headlineContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        enabled = enabled,
        applySemantics = {},
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
    )
}

/** TODO: docs */
@ExperimentalMaterial3ExpressiveApi
@Composable
internal fun SelectableListItem(
    modifier: Modifier = Modifier,

    // slots
    headlineContent: @Composable () -> Unit, // TODO: should this be `content` trailing lambda?
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,

    // interaction
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,

    // styling
    contentPadding: PaddingValues = InteractiveListPadding,
) {
    InteractiveListItem(
        modifier = modifier,
        headlineContent = headlineContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        enabled = enabled,
        applySemantics = { this.selected = selected },
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
    )
}

/** TODO: docs */
@ExperimentalMaterial3ExpressiveApi
@Composable
internal fun ToggleableListItem(
    modifier: Modifier = Modifier,

    // slots
    headlineContent: @Composable () -> Unit, // TODO: should this be `content` trailing lambda?
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,

    // interaction
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,

    // styling
    contentPadding: PaddingValues = InteractiveListPadding,
) {
    InteractiveListItem(
        modifier = modifier,
        headlineContent = headlineContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        enabled = enabled,
        applySemantics = { toggleableState = ToggleableState(checked) },
        onClick = { onCheckedChange(!checked) },
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
    )
}

@Composable
private fun LeadingDecorator(content: (@Composable () -> Unit)?) {
    if (content != null) {
        Box(Modifier.padding(end = InteractiveListInternalSpacing)) {
            // TODO: set local color/text style
            // TODO: perhaps also turn off MICS enforcement
            content()
        }
    }
}

@Composable
private fun TrailingDecorator(content: (@Composable () -> Unit)?) {
    if (content != null) {
        Box(Modifier.padding(start = InteractiveListInternalSpacing)) {
            // TODO: set local color/text style
            // TODO: perhaps also turn off MICS enforcement
            content()
        }
    }
}

@Composable
private fun OverlineDecorator(content: (@Composable () -> Unit)?) {
    if (content != null) {
        Box {
            // TODO: set local color/text style
            content()
        }
    }
}

@Composable
private fun SupportingDecorator(content: (@Composable () -> Unit)?) {
    if (content != null) {
        Box {
            // TODO: set local color/text style
            content()
        }
    }
}

@Composable
private inline fun HeadlineDecorator(content: @Composable () -> Unit) {
    Box {
        // TODO: set local color/text style
        content()
    }
}

@Composable
private fun InteractiveListItem(
    modifier: Modifier,
    headlineContent: @Composable () -> Unit,
    leadingContent: @Composable (() -> Unit)?,
    trailingContent: @Composable (() -> Unit)?,
    overlineContent: @Composable (() -> Unit)?,
    supportingContent: @Composable (() -> Unit)?,
    enabled: Boolean,
    applySemantics: SemanticsPropertyReceiver.() -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onLongClickLabel: String?,
    interactionSource: MutableInteractionSource?,
    contentPadding: PaddingValues,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    Surface(
        modifier =
            modifier
                .semantics(mergeDescendants = true, properties = applySemantics)
                .minimumInteractiveComponentSize()
                // FIXME: we need to clip the ripple without clipping away the shadow applied by
                //   Surface
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = enabled,
                    onLongClick = onLongClick,
                    onLongClickLabel = onLongClickLabel,
                    onClick = onClick,
                )
    ) {
        val alignmentBreakpoint =
            (InteractiveListVerticalAlignmentBreakpoint -
                    contentPadding.calculateTopPadding() -
                    contentPadding.calculateBottomPadding())
                .coerceAtLeast(0.dp)
        InteractiveListItemLayout(
            modifier = Modifier.padding(contentPadding),
            alignmentBreakpoint = alignmentBreakpoint,
            leading = { LeadingDecorator(leadingContent) },
            trailing = { TrailingDecorator(trailingContent) },
            overline = { OverlineDecorator(overlineContent) },
            supporting = { SupportingDecorator(supportingContent) },
            headline = { HeadlineDecorator(headlineContent) },
        )
    }
}

@Composable
private fun InteractiveListItemLayout(
    modifier: Modifier,
    alignmentBreakpoint: Dp,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    overline: @Composable () -> Unit,
    supporting: @Composable () -> Unit,
    headline: @Composable () -> Unit,
) {
    val measurePolicy =
        remember(alignmentBreakpoint) { InteractiveListItemMeasurePolicy(alignmentBreakpoint) }
    Layout(
        modifier = modifier,
        contents = listOf(leading, trailing, overline, supporting, headline),
        measurePolicy = measurePolicy,
    )
}

private class InteractiveListItemMeasurePolicy(val alignmentBreakpoint: Dp) :
    MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val (
            leadingMeasurable,
            trailingMeasurable,
            overlineMeasurable,
            supportingMeasurable,
            headlineMeasurable,
        ) = measurables

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        var constraintOffsetX = 0
        var constraintOffsetY = 0

        val leadingPlaceable = leadingMeasurable.firstOrNull()?.measure(looseConstraints)
        constraintOffsetX += leadingPlaceable.widthOrZero

        val trailingPlaceable =
            trailingMeasurable
                .firstOrNull()
                ?.measure(looseConstraints.offset(horizontal = -constraintOffsetX))
        constraintOffsetX += trailingPlaceable.widthOrZero

        val overlinePlaceable =
            overlineMeasurable
                .firstOrNull()
                ?.measure(looseConstraints.offset(horizontal = -constraintOffsetX))
        constraintOffsetY += overlinePlaceable.heightOrZero

        val headlinePlaceable =
            headlineMeasurable
                .firstOrNull()
                ?.measure(
                    looseConstraints.offset(
                        horizontal = -constraintOffsetX,
                        vertical = -constraintOffsetY,
                    )
                )
        constraintOffsetY += headlinePlaceable.heightOrZero

        val supportingPlaceable =
            supportingMeasurable
                .firstOrNull()
                ?.measure(
                    looseConstraints.offset(
                        horizontal = -constraintOffsetX,
                        vertical = -constraintOffsetY,
                    )
                )

        val width =
            calculateWidth(
                leadingWidth = leadingPlaceable.widthOrZero,
                trailingWidth = trailingPlaceable.widthOrZero,
                overlineWidth = overlinePlaceable.widthOrZero,
                supportingWidth = supportingPlaceable.widthOrZero,
                headlineWidth = headlinePlaceable.widthOrZero,
                constraints = constraints,
            )
        val height =
            calculateHeight(
                leadingHeight = leadingPlaceable.heightOrZero,
                trailingHeight = trailingPlaceable.heightOrZero,
                overlineHeight = overlinePlaceable.heightOrZero,
                supportingHeight = supportingPlaceable.heightOrZero,
                headlineHeight = headlinePlaceable.heightOrZero,
                constraints = constraints,
            )

        return place(
            width = width,
            height = height,
            leadingPlaceable = leadingPlaceable,
            trailingPlaceable = trailingPlaceable,
            headlinePlaceable = headlinePlaceable,
            overlinePlaceable = overlinePlaceable,
            supportingPlaceable = supportingPlaceable,
        )
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int,
    ): Int = calculateIntrinsicHeight(measurables, width, IntrinsicMeasurable::maxIntrinsicHeight)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int,
    ): Int = calculateIntrinsicWidth(measurables, height, IntrinsicMeasurable::maxIntrinsicWidth)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int,
    ): Int = calculateIntrinsicHeight(measurables, width, IntrinsicMeasurable::minIntrinsicHeight)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int,
    ): Int = calculateIntrinsicWidth(measurables, height, IntrinsicMeasurable::minIntrinsicWidth)

    private fun calculateIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int,
        intrinsicMeasure: IntrinsicMeasurable.(height: Int) -> Int,
    ): Int {
        val (
            leadingMeasurable,
            trailingMeasurable,
            overlineMeasurable,
            supportingMeasurable,
            headlineMeasurable,
        ) = measurables

        return calculateWidth(
            leadingWidth = leadingMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            trailingWidth = trailingMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            overlineWidth = overlineMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            supportingWidth = supportingMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            headlineWidth = headlineMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            constraints = Constraints(),
        )
    }

    private fun calculateIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int,
        intrinsicMeasure: IntrinsicMeasurable.(width: Int) -> Int,
    ): Int {
        val (
            leadingMeasurable,
            trailingMeasurable,
            overlineMeasurable,
            supportingMeasurable,
            headlineMeasurable,
        ) = measurables

        var remainingWidth = width

        val leadingHeight =
            leadingMeasurable.firstOrNull()?.let {
                val height = it.intrinsicMeasure(remainingWidth)
                remainingWidth =
                    remainingWidth.subtractConstraintSafely(
                        it.maxIntrinsicWidth(Constraints.Infinity)
                    )
                height
            } ?: 0
        val trailingHeight =
            trailingMeasurable.firstOrNull()?.let {
                val height = it.intrinsicMeasure(remainingWidth)
                remainingWidth =
                    remainingWidth.subtractConstraintSafely(
                        it.maxIntrinsicWidth(Constraints.Infinity)
                    )
                height
            } ?: 0
        val overlineHeight = overlineMeasurable.firstOrNull()?.intrinsicMeasure(remainingWidth) ?: 0
        val supportingHeight =
            supportingMeasurable.firstOrNull()?.intrinsicMeasure(remainingWidth) ?: 0
        val headlineHeight = headlineMeasurable.firstOrNull()?.intrinsicMeasure(remainingWidth) ?: 0

        return calculateHeight(
            leadingHeight = leadingHeight,
            trailingHeight = trailingHeight,
            overlineHeight = overlineHeight,
            supportingHeight = supportingHeight,
            headlineHeight = headlineHeight,
            constraints = Constraints(),
        )
    }

    private fun MeasureScope.place(
        width: Int,
        height: Int,
        leadingPlaceable: Placeable?,
        trailingPlaceable: Placeable?,
        headlinePlaceable: Placeable?,
        overlinePlaceable: Placeable?,
        supportingPlaceable: Placeable?,
    ): MeasureResult {
        return layout(width, height) {
            val verticalAlignment =
                if (height > alignmentBreakpoint.roundToPx()) {
                    Alignment.Top
                } else {
                    Alignment.CenterVertically
                }

            leadingPlaceable?.placeRelative(
                x = 0,
                y = verticalAlignment.align(leadingPlaceable.height, height),
            )

            val mainContentX = leadingPlaceable.widthOrZero
            val mainContentTotalHeight =
                headlinePlaceable.heightOrZero +
                    overlinePlaceable.heightOrZero +
                    supportingPlaceable.heightOrZero
            val mainContentY = verticalAlignment.align(mainContentTotalHeight, height)
            var currY = mainContentY

            overlinePlaceable?.placeRelative(mainContentX, currY)
            currY += overlinePlaceable.heightOrZero

            headlinePlaceable?.placeRelative(mainContentX, currY)
            currY += headlinePlaceable.heightOrZero

            supportingPlaceable?.placeRelative(mainContentX, currY)

            trailingPlaceable?.placeRelative(
                x = width - trailingPlaceable.width,
                y = verticalAlignment.align(trailingPlaceable.height, height),
            )
        }
    }

    private fun calculateWidth(
        leadingWidth: Int,
        trailingWidth: Int,
        overlineWidth: Int,
        supportingWidth: Int,
        headlineWidth: Int,
        constraints: Constraints,
    ): Int {
        if (constraints.hasBoundedWidth) {
            return constraints.maxWidth
        }
        // Fallback behavior if width constraints are infinite
        val mainContentWidth = maxOf(headlineWidth, overlineWidth, supportingWidth)
        return leadingWidth + mainContentWidth + trailingWidth
    }

    private fun calculateHeight(
        leadingHeight: Int,
        trailingHeight: Int,
        overlineHeight: Int,
        supportingHeight: Int,
        headlineHeight: Int,
        constraints: Constraints,
    ): Int {
        val mainContentHeight = headlineHeight + overlineHeight + supportingHeight

        return constraints.constrainHeight(maxOf(leadingHeight, mainContentHeight, trailingHeight))
    }
}

// TODO: replace with tokens
internal val InteractiveListStartPadding = 16.dp
internal val InteractiveListEndPadding = 16.dp
internal val InteractiveListTopPadding = 12.dp
internal val InteractiveListBottomPadding = 12.dp
internal val InteractiveListInternalSpacing = 12.dp
/**
 * How tall a list item needs to be before internal content is top-aligned instead of
 * center-aligned.
 */
internal val InteractiveListVerticalAlignmentBreakpoint = 80.dp

// TODO: move to defaults
private val InteractiveListPadding =
    PaddingValues(
        start = InteractiveListStartPadding,
        end = InteractiveListEndPadding,
        top = InteractiveListTopPadding,
        bottom = InteractiveListBottomPadding,
    )
