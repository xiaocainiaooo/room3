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

package androidx.wear.compose.material3

import android.annotation.SuppressLint
import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.GestureInclusion
import androidx.wear.compose.material3.ButtonDefaults.buttonColors
import androidx.wear.compose.material3.RevealDirection.Companion.Bidirectional
import androidx.wear.compose.material3.RevealDirection.Companion.RightToLeft
import androidx.wear.compose.material3.RevealValue.Companion.Covered
import androidx.wear.compose.material3.RevealValue.Companion.LeftRevealed
import androidx.wear.compose.material3.RevealValue.Companion.LeftRevealing
import androidx.wear.compose.material3.RevealValue.Companion.RightRevealed
import androidx.wear.compose.material3.RevealValue.Companion.RightRevealing
import androidx.wear.compose.material3.SwipeToRevealDefaults.bidirectionalGestureInclusion
import androidx.wear.compose.material3.SwipeToRevealDefaults.createRevealAnchors
import androidx.wear.compose.material3.SwipeToRevealDefaults.gestureInclusion
import androidx.wear.compose.material3.tokens.SwipeToRevealTokens
import androidx.wear.compose.materialcore.CustomTouchSlopProvider
import androidx.wear.compose.materialcore.SwipeableV2State
import androidx.wear.compose.materialcore.screenWidthDp
import androidx.wear.compose.materialcore.swipeAnchors
import androidx.wear.compose.materialcore.swipeableV2
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * [SwipeToReveal] Material composable. This adds the option to configure up to two additional
 * actions on a Composable: a mandatory [SwipeToRevealScope.primaryAction] and an optional
 * [SwipeToRevealScope.secondaryAction]. These actions are initially hidden and revealed only when
 * the [content] is swiped. These additional actions can be triggered by clicking on them after they
 * are revealed. [SwipeToRevealScope.primaryAction] will be triggered on full swipe of the
 * [content].
 *
 * For actions like "Delete", consider adding [SwipeToRevealScope.undoPrimaryAction] (displayed when
 * the [SwipeToRevealScope.primaryAction] is activated). Adding undo composables allow users to undo
 * the action that they just performed.
 *
 * [SwipeToReveal] composable adds the [CustomAccessibilityAction]s using the labels from primary
 * and secondary actions.
 *
 * Example of [SwipeToReveal] with primary and secondary actions
 *
 * @sample androidx.wear.compose.material3.samples.SwipeToRevealSample
 *
 * Example of [SwipeToReveal] with a Card composable, it reveals a taller button.
 *
 * @sample androidx.wear.compose.material3.samples.SwipeToRevealSingleActionCardSample
 *
 * Example of [SwipeToReveal] that doesn't reveal the actions, instead it only executes them when
 * fully swiped or bounces back to its initial state.
 *
 * @sample androidx.wear.compose.material3.samples.SwipeToRevealNonAnchoredSample
 *
 * Example of [SwipeToReveal] with a [TransformingLazyColumn]
 *
 * @sample androidx.wear.compose.material3.samples.SwipeToRevealWithTransformingLazyColumnSample
 * @param actions Actions of the [SwipeToReveal] composable, such as
 *   [SwipeToRevealScope.primaryAction]. [actions] should always include exactly one
 *   [SwipeToRevealScope.primaryAction]. [SwipeToRevealScope.secondaryAction],
 *   [SwipeToRevealScope.undoPrimaryAction] and [SwipeToRevealScope.undoSecondaryAction] are
 *   optional.
 * @param modifier [Modifier] to be applied on the composable
 * @param revealState [RevealState] of the [SwipeToReveal]
 * @param actionButtonHeight Desired height of the revealed action buttons. In case the content is a
 *   Button composable, it's suggested to use [SwipeToRevealDefaults.SmallActionButtonHeight], and
 *   for a Card composable, it's suggested to use [SwipeToRevealDefaults.LargeActionButtonHeight].
 * @param gestureInclusion Provides fine-grained control so that touch gestures can be excluded when
 *   they start in a certain region. An instance of [GestureInclusion] can be passed in here which
 *   will determine via [GestureInclusion.ignoreGestureStart] whether the gesture should proceed or
 *   not. By default, [gestureInclusion] allows gestures everywhere for when [revealState] contains
 *   anchors for both directions (see [bidirectionalGestureInclusion]). If it doesn't, then it
 *   allows gestures everywhere, except a zone on the left edge, which is used for swipe-to-dismiss
 *   (see [gestureInclusion]).
 * @param content The content that will be initially displayed over the other actions provided.
 * @see [androidx.wear.compose.foundation.SwipeToReveal]
 */
@Composable
public fun SwipeToReveal(
    actions: SwipeToRevealScope.() -> Unit,
    modifier: Modifier = Modifier,
    revealState: RevealState = rememberRevealState(anchors = SwipeToRevealDefaults.anchors()),
    actionButtonHeight: Dp = SwipeToRevealDefaults.SmallActionButtonHeight,
    gestureInclusion: GestureInclusion =
        if (revealState.hasBidirectionalAnchors) {
            bidirectionalGestureInclusion
        } else {
            gestureInclusion(revealState)
        },
    content: @Composable () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val children = SwipeToRevealScope()
    with(children, actions)
    val primaryAction = children.primaryAction
    require(primaryAction != null) {
        "PrimaryAction should be provided in actions by calling the PrimaryAction method"
    }

    val hasSecondaryAction = children.secondaryAction != null
    val iconStartFadeInFraction =
        if (hasSecondaryAction) {
            DOUBLE_ICON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
        } else {
            SINGLE_ICON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
        }
    val iconEndFadeInFraction =
        if (hasSecondaryAction) {
            DOUBLE_ICON_FADE_IN_END_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
        } else {
            SINGLE_ICON_FADE_IN_END_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
        }

    SwipeToReveal(
        modifier = modifier.fillMaxWidth(),
        primaryAction = {
            ActionButton(
                revealState,
                primaryAction,
                RevealActionType.PrimaryAction,
                actionButtonHeight,
                iconStartFadeInFraction,
                iconEndFadeInFraction,
                children.undoPrimaryAction != null,
            )
        },
        secondaryAction =
            children.secondaryAction?.let {
                {
                    ActionButton(
                        revealState,
                        it,
                        RevealActionType.SecondaryAction,
                        actionButtonHeight,
                        iconStartFadeInFraction,
                        iconEndFadeInFraction,
                        children.undoSecondaryAction != null,
                    )
                }
            },
        undoAction =
            when (revealState.lastActionType) {
                RevealActionType.SecondaryAction ->
                    children.undoSecondaryAction?.let {
                        {
                            ActionButton(
                                revealState,
                                it,
                                RevealActionType.UndoAction,
                                actionButtonHeight,
                                iconStartFadeInFraction,
                                iconEndFadeInFraction,
                            )
                        }
                    }
                else ->
                    children.undoPrimaryAction?.let {
                        {
                            ActionButton(
                                revealState,
                                it,
                                RevealActionType.UndoAction,
                                actionButtonHeight,
                                iconStartFadeInFraction,
                                iconEndFadeInFraction,
                            )
                        }
                    }
            },
        onFullSwipe = {
            // Full swipe triggers the main action, but does not set the click type.
            // Explicitly set the click type as main action when full swipe occurs.
            revealState.lastActionType = RevealActionType.PrimaryAction
            primaryAction.onClick()
        },
        state = revealState,
        gestureInclusion = gestureInclusion,
        content = content,
    )

    LaunchedEffect(revealState.targetValue) {
        if ((revealState.targetValue == LeftRevealed || revealState.targetValue == RightRevealed)) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }
}

/**
 * Scope for the actions of a [SwipeToReveal] composable. Used to define the primary, secondary,
 * undo primary and undo secondary actions.
 */
public class SwipeToRevealScope {
    /**
     * Adds the primary action to a [SwipeToReveal]. This is required and exactly one primary action
     * should be specified. In case there are multiple, only the latest one will be displayed.
     *
     * When first revealed the primary action displays an icon and then, if fully swiped, it
     * additionally shows text.
     *
     * @param onClick Callback to be executed when the action is performed via a full swipe, or a
     *   button click.
     * @param icon Icon composable to be displayed for this action.
     * @param text Text composable to be displayed when the user fully swipes to execute the primary
     *   action.
     * @param containerColor Container color for this action.
     * @param contentColor Content color for this action.
     */
    public fun primaryAction(
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        text: @Composable () -> Unit,
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified
    ) {
        primaryAction = SwipeToRevealAction(onClick, icon, text, containerColor, contentColor)
    }

    /**
     * Adds the secondary action to a [SwipeToReveal]. This is optional and at most one secondary
     * action should be specified. In case there are multiple, only the latest one will be
     * displayed.
     *
     * Secondary action only displays an icon, because, unlike the primary action, it is never
     * extended to full width so does not have room to display text.
     *
     * @param onClick Callback to be executed when the action is performed via a button click.
     * @param icon Icon composable to be displayed for this action.
     * @param containerColor Container color for this action.
     * @param contentColor Content color for this action.
     */
    public fun secondaryAction(
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified
    ) {
        secondaryAction = SwipeToRevealAction(onClick, icon, null, containerColor, contentColor)
    }

    /**
     * Adds the undo action for the primary action to a [SwipeToReveal]. Displayed after the user
     * performs the primary action. This is optional and at most one undo primary action should be
     * specified. In case there are multiple, only the latest one will be displayed.
     *
     * @param onClick Callback to be executed when the action is performed via a button click.
     * @param text Text composable to indicate what the undo action is, to be displayed when the
     *   user executes the primary action. This should include appropriated semantics for
     *   accessibility.
     * @param icon Optional Icon composable to be displayed for this action.
     * @param containerColor Container color for this action.
     * @param contentColor Content color for this action.
     */
    public fun undoPrimaryAction(
        onClick: () -> Unit,
        text: @Composable () -> Unit,
        icon: @Composable (() -> Unit)? = null,
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified
    ) {
        undoPrimaryAction = SwipeToRevealAction(onClick, icon, text, containerColor, contentColor)
    }

    /**
     * Adds the undo action for the secondary action to a [SwipeToReveal]. Displayed after the user
     * performs the secondary action.This is optional and at most one undo secondary action should
     * be specified. In case there are multiple, only the latest one will be displayed.
     *
     * @param onClick Callback to be executed when the action is performed via a button click.
     * @param text Text composable to indicate what the undo action is, to be displayed when the
     *   user executes the primary action. This should include appropriated semantics for
     *   accessibility.
     * @param icon Optional Icon composable to be displayed for this action.
     * @param containerColor Container color for this action.
     * @param contentColor Content color for this action.
     */
    public fun undoSecondaryAction(
        onClick: () -> Unit,
        text: @Composable () -> Unit,
        icon: @Composable (() -> Unit)? = null,
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified
    ) {
        undoSecondaryAction = SwipeToRevealAction(onClick, icon, text, containerColor, contentColor)
    }

    internal var primaryAction: SwipeToRevealAction? = null
    internal var undoPrimaryAction: SwipeToRevealAction? = null
    internal var secondaryAction: SwipeToRevealAction? = null
    internal var undoSecondaryAction: SwipeToRevealAction? = null
}

public object SwipeToRevealDefaults {

    /** Width that's required to display both actions in a [SwipeToReveal] composable. */
    public val DoubleActionAnchorWidth: Dp = 130.dp

    /** Width that's required to display a single action in a [SwipeToReveal] composable. */
    public val SingleActionAnchorWidth: Dp = 64.dp

    /** Standard height for a small revealed action, such as when the swiped item is a Button. */
    public val SmallActionButtonHeight: Dp = 52.dp

    /** Standard height for a large revealed action, such as when the swiped item is a Card. */
    public val LargeActionButtonHeight: Dp = 84.dp

    internal val IconSize = 26.dp

    /**
     * Creates the required anchors to which the top content can be swiped, to reveal the actions.
     * Each value should be in the range [0..1], where 0 represents right most end and 1 represents
     * the full width of the top content starting from right and ending on left.
     *
     * @param coveredAnchor Anchor for the [Covered] value
     * @param revealingAnchor Anchor for the [LeftRevealing] or [RightRevealing] value
     * @param revealedAnchor Anchor for the [LeftRevealed] or [RightRevealed] value
     * @param revealDirection The direction in which the content can be swiped. It's strongly
     *   advised to keep the default [RevealDirection.RightToLeft] in order to preserve
     *   compatibility with the system wide swipe to dismiss gesture.
     */
    @SuppressWarnings("PrimitiveInCollection")
    public fun createRevealAnchors(
        coveredAnchor: Float = 0f,
        revealingAnchor: Float = RevealingRatio,
        revealedAnchor: Float = 1f,
        revealDirection: RevealDirection = RightToLeft
    ): Map<RevealValue, Float> {
        if (revealDirection == Bidirectional) {
            return mapOf(
                LeftRevealed to -revealedAnchor,
                LeftRevealing to -revealingAnchor,
                Covered to coveredAnchor,
                RightRevealing to revealingAnchor,
                RightRevealed to revealedAnchor
            )
        }
        return mapOf(
            Covered to coveredAnchor,
            RightRevealing to revealingAnchor,
            RightRevealed to revealedAnchor
        )
    }

    /**
     * Creates the recommended anchors to support right-to-left swiping to reveal additional action
     * buttons.
     *
     * @param anchorWidth Absolute width, in dp, of the screen revealed items should be displayed
     *   in. Ignored if [useAnchoredActions] is set to false, as the items won't be anchored to the
     *   screen. For a single action SwipeToReveal component, this should be
     *   [SwipeToRevealDefaults.SingleActionAnchorWidth], and for a double action SwipeToReveal,
     *   [SwipeToRevealDefaults.DoubleActionAnchorWidth] to be able to display both action buttons.
     * @param useAnchoredActions Whether the actions should stay revealed, or bounce back to hidden
     *   when the user stops swiping. This is relevant for SwipeToReveal components with a single
     *   action. If the developer wants a swipe to clear behaviour, this should be set to false.
     */
    @SuppressLint("PrimitiveInCollection")
    @Composable
    public fun anchors(
        anchorWidth: Dp = SingleActionAnchorWidth,
        useAnchoredActions: Boolean = true,
    ): Map<RevealValue, Float> =
        createAnchors(anchorWidth = anchorWidth, useAnchoredActions = useAnchoredActions)

    /**
     * Creates anchors that allow the user to swipe either left-to-right or right-to-left to reveal
     * or execute the actions. This should not be used if the component is part of an activity, as
     * the gesture might conflict with the swipe-to-dismiss gesture. This is only supported for rare
     * cases where the current screen does not support swipe to dismiss.
     *
     * @param anchorWidth Absolute width, in dp, of the screen revealed items should be displayed
     *   in. Ignored if [useAnchoredActions] is set to false, as the items won't be anchored to the
     *   screen. For a single action SwipeToReveal component, this should be
     *   [SwipeToRevealDefaults.SingleActionAnchorWidth], and for a double action SwipeToReveal,
     *   [SwipeToRevealDefaults.DoubleActionAnchorWidth] to be able to display both action buttons.
     * @param useAnchoredActions Whether the actions should stay revealed, or bounce back to hidden
     *   when the user stops swiping. This is relevant for SwipeToReveal components with a single
     *   action. If the developer wants a swipe to clear behaviour, this should be set to false.
     */
    @SuppressLint("PrimitiveInCollection")
    @Composable
    public fun bidirectionalAnchors(
        anchorWidth: Dp = SingleActionAnchorWidth,
        useAnchoredActions: Boolean = true,
    ): Map<RevealValue, Float> =
        createAnchors(
            anchorWidth = anchorWidth,
            useAnchoredActions = useAnchoredActions,
            revealDirection = Bidirectional
        )

    @SuppressLint("PrimitiveInCollection")
    @Composable
    internal fun createAnchors(
        anchorWidth: Dp = SingleActionAnchorWidth,
        useAnchoredActions: Boolean = true,
        revealDirection: RevealDirection = RightToLeft,
    ): Map<RevealValue, Float> {
        val screenWidthDp = LocalConfiguration.current.screenWidthDp
        val anchorFraction = anchorWidth.value / screenWidthDp
        return createRevealAnchors(
            revealingAnchor = if (useAnchoredActions) anchorFraction else 0f,
            revealDirection = revealDirection,
        )
    }

    /** Default animation spec used when moving between states. */
    internal val AnimationSpec: AnimationSpec<Float> =
        tween(durationMillis = RAPID_ANIMATION, easing = FastOutSlowInEasing)

    /** Default padding space between action slots. */
    internal val Padding = 4.dp

    /**
     * Default ratio of the content displayed when in [RightRevealing] state, i.e. all the actions
     * are revealed and the top content is not being swiped. For example, a value of 0.7 means that
     * 70% of the width is used to place the actions.
     */
    public val RevealingRatio: Float = 0.7f

    /**
     * Default position threshold that needs to be swiped in order to transition to the next state.
     * Used in conjunction with [RevealingRatio]; for example, a threshold of 0.5 with a revealing
     * ratio of 0.7 means that the user needs to swipe at least 35% (0.5 * 0.7) of the component
     * width to go from [Covered] to [RightRevealing] and at least 85% (0.7 + 0.5 * (1 - 0.7)) of
     * the component width to go from [RightRevealing] to [RightRevealed].
     */
    public val PositionalThreshold: (totalDistance: Float) -> Float = { totalDistance: Float ->
        totalDistance * 0.5f
    }

    /**
     * The default value used to configure the size of the left edge zone in a [SwipeToReveal]. The
     * left edge zone in this case refers to the leftmost edge of the screen, in this region it is
     * common to disable scrolling in order for swipe-to-dismiss handlers to take over.
     */
    public val LeftEdgeZoneFraction: Float = 0.15f

    /**
     * The default behaviour for when [SwipeToReveal] should handle gestures. In this implementation
     * of [GestureInclusion], swipe events that originate in the left edge of the screen (as
     * determined by [LeftEdgeZoneFraction]) will be ignored, if the [RevealState] is [Covered].
     * This allows swipe-to-dismiss handlers (if present) to handle the gesture in this region.
     *
     * @param state [RevealState] of the [SwipeToReveal].
     * @param edgeZoneFraction The fraction of the screen width from the left edge where gestures
     *   should be ignored. Defaults to [LeftEdgeZoneFraction].
     */
    public fun gestureInclusion(
        state: RevealState,
        @FloatRange(from = 0.0, to = 1.0) edgeZoneFraction: Float = LeftEdgeZoneFraction
    ): GestureInclusion = DefaultGestureInclusion(state, edgeZoneFraction)

    /**
     * A behaviour for [SwipeToReveal] to handle all gestures, intended for rare cases where
     * bidirectional anchors are used and no swipe events are ignored
     */
    public val bidirectionalGestureInclusion: GestureInclusion
        get() = BidirectionalGestureInclusion
}

@Composable
internal fun ActionButton(
    revealState: RevealState,
    action: SwipeToRevealAction,
    revealActionType: RevealActionType,
    buttonHeight: Dp,
    iconStartFadeInFraction: Float,
    iconEndFadeInFraction: Float,
    hasUndo: Boolean = false,
) {
    val containerColor =
        action.containerColor.takeOrElse {
            when (revealActionType) {
                RevealActionType.PrimaryAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.PrimaryActionContainerColor
                    )
                RevealActionType.SecondaryAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.SecondaryActionContainerColor
                    )
                RevealActionType.UndoAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.UndoActionContainerColor
                    )
                else -> Color.Unspecified
            }
        }
    val contentColor =
        action.contentColor.takeOrElse {
            when (revealActionType) {
                RevealActionType.PrimaryAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.PrimaryActionContentColor
                    )
                RevealActionType.SecondaryAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.SecondaryActionContentColor
                    )
                RevealActionType.UndoAction ->
                    MaterialTheme.colorScheme.fromToken(SwipeToRevealTokens.UndoActionContentColor)
                else -> Color.Unspecified
            }
        }
    val fullScreenPaddingDp = (screenWidthDp() * FULL_SCREEN_PADDING_FRACTION).dp
    val startPadding =
        when (revealActionType) {
            RevealActionType.UndoAction -> fullScreenPaddingDp
            else -> 0.dp
        }
    val endPadding =
        when (revealActionType) {
            RevealActionType.UndoAction -> fullScreenPaddingDp
            else -> 0.dp
        }
    val screenWidthPx = with(LocalDensity.current) { screenWidthDp().dp.toPx() }
    val fadeInStart = screenWidthPx * BUTTON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
    val fadeInEnd = screenWidthPx * BUTTON_FADE_IN_END_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
    val coroutineScope = rememberCoroutineScope()
    Button(
        modifier =
            Modifier.height(buttonHeight)
                .padding(startPadding, 0.dp, endPadding, 0.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val offset = abs(revealState.offset)
                    val shouldDisplayButton =
                        offset > screenWidthPx * BUTTON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
                    alpha =
                        if (shouldDisplayButton) {
                            val coercedOffset =
                                offset.coerceIn(
                                    minimumValue = fadeInStart,
                                    maximumValue = fadeInEnd
                                )
                            (coercedOffset - fadeInStart) / (fadeInEnd - fadeInStart)
                        } else {
                            0f
                        }
                },
        onClick = {
            coroutineScope.launch {
                try {
                    if (revealActionType == RevealActionType.UndoAction) {
                        revealState.animateTo(Covered)
                    } else {
                        if (hasUndo || revealActionType == RevealActionType.PrimaryAction) {
                            revealState.lastActionType = revealActionType
                            revealState.animateTo(
                                if (revealState.offset > 0) {
                                    LeftRevealed
                                } else {
                                    RightRevealed
                                }
                            )
                        }
                    }
                } finally {
                    // Execute onClick even if the animation gets interrupted
                    action.onClick()
                }
            }
        },
        colors = buttonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(ACTION_BUTTON_CONTENT_PADDING),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val primaryActionTextRevealed = remember { mutableStateOf(false) }
            action.icon?.let {
                ActionIconWrapper(revealState, iconStartFadeInFraction, iconEndFadeInFraction, it)
            }
            when (revealActionType) {
                RevealActionType.PrimaryAction -> {
                    AnimatedVisibility(
                        visible = primaryActionTextRevealed.value,
                        enter =
                            fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                                expandHorizontally(spring(stiffness = Spring.StiffnessMedium)),
                        exit =
                            fadeOut(spring(stiffness = Spring.StiffnessHigh)) +
                                shrinkHorizontally(spring(stiffness = Spring.StiffnessMedium)),
                    ) {
                        ActionText(action, contentColor)
                    }

                    LaunchedEffect(revealState.offset) {
                        primaryActionTextRevealed.value =
                            abs(revealState.offset) > revealState.revealThreshold &&
                                (revealState.targetValue == RightRevealed ||
                                    revealState.targetValue == LeftRevealed)
                    }
                }
                RevealActionType.UndoAction -> ActionText(action, contentColor)
            }
        }
    }
}

@Composable
private fun ActionText(action: SwipeToRevealAction, contentColor: Color) {
    require(action.text != null) { "A text composable should be provided to ActionText." }
    Row(modifier = Modifier.padding(start = action.icon?.let { ICON_AND_TEXT_PADDING } ?: 0.dp)) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides LocalTextStyle.current,
            LocalTextConfiguration provides
                TextConfiguration(
                    textAlign = LocalTextConfiguration.current.textAlign,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
        ) {
            action.text.invoke()
        }
    }
}

@Composable
private fun ActionIconWrapper(
    revealState: RevealState,
    iconStartFadeInFraction: Float,
    iconEndFadeInFraction: Float,
    content: @Composable () -> Unit
) {
    val screenWidthPx = with(LocalDensity.current) { screenWidthDp().dp.toPx() }
    val fadeInStart = screenWidthPx * iconStartFadeInFraction
    val fadeInEnd = screenWidthPx * iconEndFadeInFraction
    Box(
        modifier =
            Modifier.size(SwipeToRevealDefaults.IconSize, Dp.Unspecified).graphicsLayer {
                val offset = abs(revealState.offset)
                val shouldDisplayIcon = offset > fadeInStart
                alpha =
                    if (shouldDisplayIcon) {
                        val coercedOffset =
                            offset.coerceIn(minimumValue = fadeInStart, maximumValue = fadeInEnd)
                        (coercedOffset - fadeInStart) / (fadeInEnd - fadeInStart)
                    } else {
                        0f
                    }
            }
    ) {
        content()
    }
}

/** Data class to define an action to be displayed in a [SwipeToReveal] composable. */
internal data class SwipeToRevealAction(
    /** Callback to be executed when the action is performed via a full swipe, or a button click. */
    val onClick: () -> Unit,

    /**
     * Icon composable to be displayed for this action. This accepts a scale parameter that should
     * be used to increase icon icon when an action is fully revealed.
     */
    val icon: @Composable (() -> Unit)?,

    /**
     * Text composable to be displayed when the user fully swipes to execute the primary action, or
     * when the undo action is shown.
     */
    val text: @Composable (() -> Unit)?,

    /**
     * Color of the container, used for the background of the action button. This can be
     * [Color.Unspecified], and in case it is, needs to be replaced with a default.
     */
    val containerColor: Color,

    /**
     * Color of the content, used for the icon and text. This can be [Color.Unspecified], and in
     * case it is, needs to be replaced with a default.
     */
    val contentColor: Color,
)

/**
 * Different values that determine the state of the [SwipeToReveal] composable, reflected in
 * [RevealState.currentValue]. [Covered] is considered the default state where none of the actions
 * are revealed yet.
 *
 * [SwipeToReveal] direction is not localised, with the default being [RevealDirection.RightToLeft],
 * and [RightRevealing] and [RightRevealed] correspond to the actions getting revealed from the
 * right side of the screen. In case swipe direction is set to [RevealDirection.Bidirectional],
 * actions can also get revealed from the left side of the screen, and in that case [LeftRevealing]
 * and [LeftRevealed] are used.
 *
 * @see [RevealDirection]
 */
@JvmInline
public value class RevealValue private constructor(public val value: Int) {
    public companion object {
        /**
         * The value which represents the state in which the whole revealable content is fully
         * revealed, and they are displayed on the left side of the screen. This also represents the
         * state in which one of the actions has been triggered/performed.
         *
         * This is only used when the swipe direction is set to [RevealDirection.Bidirectional], and
         * the user swipes from the left side of the screen.
         */
        public val LeftRevealed: RevealValue = RevealValue(-2)

        /**
         * The value which represents the state in which all the actions are revealed and the top
         * content is not being swiped. In this state, none of the actions have been triggered or
         * performed yet, and they are displayed on the left side of the screen.
         *
         * This is only used when the swipe direction is set to [RevealDirection.Bidirectional], and
         * the user swipes from the left side of the screen.
         */
        public val LeftRevealing: RevealValue = RevealValue(-1)

        /**
         * The default first value which generally represents the state where the revealable actions
         * have not been revealed yet. In this state, none of the actions have been triggered or
         * performed yet.
         */
        public val Covered: RevealValue = RevealValue(0)

        /**
         * The value which represents the state in which all the actions are revealed and the top
         * content is not being swiped. In this state, none of the actions have been triggered or
         * performed yet, and they are displayed on the right side of the screen.
         */
        public val RightRevealing: RevealValue = RevealValue(1)

        /**
         * The value which represents the state in which the whole revealable content is fully
         * revealed, and the actions are revealed on the right side of the screen. This also
         * represents the state in which one of the actions has been triggered/performed.
         */
        public val RightRevealed: RevealValue = RevealValue(2)
    }
}

/**
 * Different values [SwipeToReveal] composable can reveal the actions from.
 *
 * [RevealDirection] is not localised, with the default being [RevealDirection.RightToLeft] to
 * prevent conflict with the system-wide swipe to dismiss gesture in an activity, so it's strongly
 * advised to respect the default value to avoid conflicting gestures.
 */
@JvmInline
public value class RevealDirection private constructor(public val value: Int) {
    public companion object {
        /**
         * The default value which allows the user to swipe right to left to reveal or execute the
         * actions. It's strongly advised to respect the default behavior to avoid conflict with the
         * swipe-to-dismiss gesture.
         */
        public val RightToLeft: RevealDirection = RevealDirection(0)

        /**
         * The value which allows the user to swipe in either direction to reveal or execute the
         * actions. This should not be used if the component is used in an activity as the gesture
         * might conflict with the swipe-to-dismiss gesture and could be confusing for the users.
         * This is only supported for rare cases where the current screen does not support swipe to
         * dismiss.
         */
        public val Bidirectional: RevealDirection = RevealDirection(1)
    }
}

/**
 * Different values which can trigger the state change from one [RevealValue] to another. These are
 * not set by themselves and need to be set appropriately with [RevealState.snapTo] and
 * [RevealState.animateTo].
 */
@JvmInline
public value class RevealActionType private constructor(public val value: Int) {
    public companion object {
        /**
         * Represents the primary action composable of [SwipeToReveal]. This corresponds to the
         * mandatory `primaryAction` parameter of [SwipeToReveal].
         */
        public val PrimaryAction: RevealActionType = RevealActionType(0)

        /**
         * Represents the secondary action composable of [SwipeToReveal]. This corresponds to the
         * optional `secondaryAction` composable of [SwipeToReveal].
         */
        public val SecondaryAction: RevealActionType = RevealActionType(1)

        /**
         * Represents the undo action composable of [SwipeToReveal]. This corresponds to the
         * `undoAction` composable of [SwipeToReveal] which is shown once an action is performed.
         */
        public val UndoAction: RevealActionType = RevealActionType(2)

        /** Default value when none of the above are applicable. */
        public val None: RevealActionType = RevealActionType(-1)
    }
}

/**
 * A class to keep track of the state of the composable. It can be used to customise the behavior
 * and state of the composable.
 *
 * @param initialValue The initial value of this state.
 * @param anchors Defines the anchors for revealable content. These anchors are used to determine
 *   the width at which the revealable content can be revealed to and stopped without requiring any
 *   input from the user. Each anchor should be a fraction in the range [0..1].
 * @constructor Create a [RevealState].
 */
@SuppressLint("PrimitiveInCollection")
public class RevealState(
    initialValue: RevealValue,
    internal val anchors: Map<RevealValue, Float>,
) {
    internal val nestedScrollDispatcher: NestedScrollDispatcher = NestedScrollDispatcher()

    /** [androidx.wear.compose.materialcore.SwipeableV2State] internal instance for the state. */
    internal val swipeableState =
        SwipeableV2State(
            initialValue = initialValue,
            animationSpec = SwipeToRevealDefaults.AnimationSpec,
            confirmValueChange = { revealValue -> confirmValueChangeAndReset(revealValue) },
            positionalThreshold = { totalDistance ->
                SwipeToRevealDefaults.PositionalThreshold(totalDistance)
            },
            nestedScrollDispatcher = nestedScrollDispatcher,
        )

    public var lastActionType: RevealActionType by mutableStateOf(RevealActionType.None)

    /**
     * The current [RevealValue] based on the status of the component.
     *
     * @see swipeableV2
     */
    public val currentValue: RevealValue
        get() = swipeableState.currentValue

    /**
     * The target [RevealValue] based on the status of the component. This will be equal to the
     * [currentValue] if there is no animation running or swiping has stopped. Otherwise, this
     * returns the next [RevealValue] based on the animation/swipe direction.
     *
     * @see swipeableV2
     */
    public val targetValue: RevealValue
        get() = swipeableState.targetValue

    /**
     * Returns whether the animation is running or not.
     *
     * @see swipeableV2
     */
    public val isAnimationRunning: Boolean
        get() = swipeableState.isAnimationRunning

    /**
     * The current amount by which the revealable content has been revealed by.
     *
     * @see swipeableV2
     */
    public val offset: Float
        get() = swipeableState.offset ?: 0f

    /**
     * The threshold, in pixels, where the revealed actions are fully visible but the existing
     * content would be left in place if the reveal action was stopped. This threshold is used to
     * create the anchor for [RightRevealing]. If there is no such anchor defined for
     * [RightRevealing], it returns 0.0f.
     */
    /* @FloatRange(from = 0.0) */
    public val revealThreshold: Float
        get() = width.floatValue * (anchors[RightRevealing] ?: 0.0f)

    /**
     * The total width of the component in pixels. Initialise to zero, updated when the width
     * changes.
     */
    public val width: MutableFloatState = mutableFloatStateOf(0.0f)

    /**
     * Snaps to the [targetValue] without any animation.
     *
     * @param targetValue The target [RevealValue] where the [currentValue] will be changed to.
     * @see swipeableV2
     */
    public suspend fun snapTo(targetValue: RevealValue) {
        // Cover the previously open component if revealing a different one
        if (targetValue != Covered) {
            resetLastState(this)
        }
        swipeableState.snapTo(targetValue)
    }

    /**
     * Animates to the [targetValue] with the animation spec provided.
     *
     * @param targetValue The target [RevealValue] where the [currentValue] will animate to.
     */
    public suspend fun animateTo(targetValue: RevealValue) {
        // Cover the previously open component if revealing a different one
        if (targetValue != Covered) {
            resetLastState(this)
        }
        try {
            swipeableState.animateTo(targetValue)
        } finally {
            if (targetValue == Covered) {
                lastActionType = RevealActionType.None
            }
        }
    }

    /** Indicate if this state was created with bidirectional anchors. */
    @get:JvmName("hasBidirectionalAnchors")
    public val hasBidirectionalAnchors: Boolean
        get() = this.anchors.keys.any { it == LeftRevealing || it == LeftRevealed }

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    internal fun requireOffset(): Float = swipeableState.requireOffset()

    private suspend fun confirmValueChangeAndReset(revealValue: RevealValue): Boolean {
        val currentState = this
        // Update the state if the reveal value is changing to a different value than Covered.
        if (revealValue != Covered) {
            resetLastState(currentState)
        }
        return true
    }

    /**
     * Resets last state if a different SwipeToReveal is being moved to new anchor and the last
     * state is in [RightRevealing] mode which represents no action has been performed yet. In
     * [RightRevealed], the action has been performed and it will not be reset.
     */
    private suspend fun resetLastState(currentState: RevealState) {
        val oldState = SingleSwipeCoordinator.lastUpdatedState.getAndSet(currentState)
        if (currentState != oldState && oldState?.currentValue == RightRevealing) {
            oldState.animateTo(Covered)
        }
    }

    /** A singleton instance to keep track of the [RevealState] which was modified the last time. */
    private object SingleSwipeCoordinator {
        var lastUpdatedState: AtomicReference<RevealState?> = AtomicReference(null)
    }
}

/**
 * Create and [remember] a [RevealState].
 *
 * @param initialValue The initial value of the [RevealValue].
 * @param anchors A map of [RevealValue] to the fraction where the content can be revealed to reach
 *   that value. Each anchor should be between [0..1] which will be adjusted based on total width.
 */
@Composable
@SuppressLint("PrimitiveInCollection")
public fun rememberRevealState(
    initialValue: RevealValue = Covered,
    anchors: Map<RevealValue, Float> = createRevealAnchors(),
): RevealState =
    remember(initialValue) {
        RevealState(
            initialValue = initialValue,
            anchors = anchors,
        )
    }

/**
 * A composable that can be used to add extra actions to a composable (up to two) which will be
 * revealed when the original composable is swiped to the left. This composable requires a primary
 * swipe/click action, a secondary optional click action can also be provided.
 *
 * When the composable reaches the state where all the actions are revealed and the swipe continues
 * beyond the positional threshold defined in [RevealState], the primary action is automatically
 * triggered.
 *
 * An optional undo action can also be added. This undo action will be visible to users once the
 * [RevealValue] becomes [RightRevealed].
 *
 * It is strongly recommended to have icons represent the actions and maybe a text and icon for the
 * undo action.
 *
 * @param primaryAction The primary action that will be triggered in the event of a completed swipe.
 *   We also strongly recommend to trigger the action when it is clicked.
 * @param modifier Optional [Modifier] for this component.
 * @param onFullSwipe An optional lambda which will be triggered when a full swipe from either of
 *   the anchors is performed.
 * @param state The [RevealState] of this component. It can be used to customise the anchors and
 *   threshold config of the swipeable modifier which is applied.
 * @param secondaryAction An optional action that can be added to the component. We strongly
 *   recommend triggering the action when it is clicked.
 * @param undoAction The optional undo action that will be applied to the component once the
 *   [RevealState.currentValue] becomes [RightRevealed].
 * @param gestureInclusion Provides fine-grained control so that touch gestures can be excluded when
 *   they start in a certain region. An instance of [GestureInclusion] can be passed in here which
 *   will determine via [GestureInclusion.ignoreGestureStart] whether the gesture should proceed or
 *   not. By default, [gestureInclusion] allows gestures everywhere except a zone on the left edge,
 *   which is used for swipe-to-dismiss (see [SwipeToRevealDefaults.gestureInclusion]).
 * @param content The content that will be initially displayed over the other actions provided.
 */
@Composable
internal fun SwipeToReveal(
    primaryAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onFullSwipe: () -> Unit = {},
    state: RevealState = rememberRevealState(),
    secondaryAction: (@Composable () -> Unit)? = null,
    undoAction: (@Composable () -> Unit)? = null,
    gestureInclusion: GestureInclusion = SwipeToRevealDefaults.gestureInclusion(state = state),
    content: @Composable () -> Unit
) {
    // A no-op NestedScrollConnection which does not consume scroll/fling events
    val noOpNestedScrollConnection = remember { object : NestedScrollConnection {} }

    var globalPosition by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var allowSwipe by remember { mutableStateOf(true) }

    CustomTouchSlopProvider(
        newTouchSlop = LocalViewConfiguration.current.touchSlop * CustomTouchSlopMultiplier
    ) {
        Box(
            modifier =
                modifier
                    .onGloballyPositioned { layoutCoordinates ->
                        globalPosition = layoutCoordinates
                    }
                    .pointerInput(globalPosition) {
                        awaitEachGesture {
                            allowSwipe = true
                            val firstDown = awaitFirstDown(false, PointerEventPass.Initial)
                            globalPosition?.let {
                                allowSwipe =
                                    !gestureInclusion.ignoreGestureStart(firstDown.position, it)
                            }
                        }
                    }
                    .swipeableV2(
                        state = state.swipeableState,
                        orientation = Orientation.Horizontal,
                        enabled =
                            allowSwipe &&
                                state.currentValue != LeftRevealed &&
                                state.currentValue != RightRevealed,
                    )
                    .swipeAnchors(
                        state = state.swipeableState,
                        possibleValues = state.anchors.keys
                    ) { value, layoutSize ->
                        val swipeableWidth = layoutSize.width.toFloat()
                        // Update the total width which will be used to calculate the anchors
                        state.width.floatValue = swipeableWidth
                        // Multiply the anchor with -1f to get the actual swipeable anchor
                        state.anchors[value]?.let { anchor -> -anchor * swipeableWidth }
                    }
                    // NestedScrollDispatcher sends the scroll/fling events from the node to its
                    // parent
                    // and onwards including the modifier chain. Apply it in the end to let nested
                    // scroll
                    // connection applied before this modifier consume the scroll/fling events.
                    .nestedScroll(noOpNestedScrollConnection, state.nestedScrollDispatcher)
        ) {
            val swipeCompleted =
                state.currentValue == RightRevealed || state.currentValue == LeftRevealed
            val lastActionIsSecondary = state.lastActionType == RevealActionType.SecondaryAction
            val isWithinRevealOffset by remember {
                derivedStateOf { abs(state.offset) <= state.revealThreshold }
            }
            val canSwipeRight = (state.anchors.minOfOrNull { (_, offset) -> offset } ?: 0f) < 0f

            // Determines whether the secondary action will be visible based on the current
            // reveal offset
            val showSecondaryAction = isWithinRevealOffset || lastActionIsSecondary

            // Determines whether both primary and secondary action should be hidden, usually the
            // case
            // when secondary action is clicked
            val hideActions = !isWithinRevealOffset && lastActionIsSecondary

            val swipingRight by remember { derivedStateOf { state.offset > 0 } }

            // Don't draw actions on the left side if the user cannot swipe right, and they are
            // currently swiping right
            val shouldDrawActions by remember {
                derivedStateOf { abs(state.offset) > 0 && (canSwipeRight || !swipingRight) }
            }

            // Draw the buttons only when offset is greater than zero.
            if (shouldDrawActions) {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment =
                        if (swipingRight) AbsoluteAlignment.CenterLeft
                        else AbsoluteAlignment.CenterRight
                ) {
                    AnimatedContent(
                        targetState = swipeCompleted && undoAction != null,
                        transitionSpec = {
                            if (targetState) { // Fade in the Undo composable and fade out actions
                                fadeInUndo()
                            } else { // Fade in the actions and fade out the undo composable
                                fadeOutUndo()
                            }
                        },
                        label = "AnimatedContentS2R"
                    ) { displayUndo ->
                        if (displayUndo && undoAction != null) {
                            val undoActionAlpha =
                                animateFloatAsState(
                                    targetValue = if (swipeCompleted) 1f else 0f,
                                    animationSpec =
                                        tween(
                                            durationMillis = RAPID_ANIMATION,
                                            delayMillis = FLASH_ANIMATION,
                                            easing = STANDARD_IN_OUT,
                                        ),
                                    label = "UndoActionAlpha"
                                )
                            Row(
                                modifier =
                                    Modifier.graphicsLayer { alpha = undoActionAlpha.value }
                                        .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                ActionSlot(content = undoAction)
                            }
                        } else {
                            // Animate weight for secondary action slot.
                            val secondaryActionWeight =
                                animateFloatAsState(
                                    targetValue = if (showSecondaryAction) 1f else 0f,
                                    animationSpec = tween(durationMillis = QUICK_ANIMATION),
                                    label = "SecondaryActionAnimationSpec"
                                )
                            val secondaryActionAlpha =
                                animateFloatAsState(
                                    targetValue =
                                        if (!showSecondaryAction || hideActions) 0f else 1f,
                                    animationSpec =
                                        tween(
                                            durationMillis = QUICK_ANIMATION,
                                            easing = LinearEasing
                                        ),
                                    label = "SecondaryActionAlpha"
                                )
                            val primaryActionAlpha =
                                animateFloatAsState(
                                    targetValue = if (hideActions) 0f else 1f,
                                    animationSpec =
                                        tween(durationMillis = 100, easing = LinearEasing),
                                    label = "PrimaryActionAlpha"
                                )
                            val revealedContentAlpha =
                                animateFloatAsState(
                                    targetValue = if (swipeCompleted) 0f else 1f,
                                    animationSpec =
                                        tween(
                                            durationMillis = FLASH_ANIMATION,
                                            easing = LinearEasing
                                        ),
                                    label = "RevealedContentAlpha"
                                )
                            var revealedContentHeight by remember { mutableIntStateOf(0) }
                            Row(
                                modifier =
                                    Modifier.graphicsLayer { alpha = revealedContentAlpha.value }
                                        .onSizeChanged { revealedContentHeight = it.height }
                                        .layout { measurable, constraints ->
                                            val placeable =
                                                measurable.measure(
                                                    constraints.copy(
                                                        maxWidth =
                                                            if (hideActions) {
                                                                    state.revealThreshold
                                                                } else {
                                                                    abs(state.offset)
                                                                }
                                                                .roundToInt()
                                                    )
                                                )
                                            layout(placeable.width, placeable.height) {
                                                placeable.placeRelative(
                                                    0,
                                                    calculateVerticalOffsetBasedOnScreenPosition(
                                                        revealedContentHeight,
                                                        globalPosition
                                                    )
                                                )
                                            }
                                        },
                                horizontalArrangement = Arrangement.Absolute.Right
                            ) {
                                if (!swipingRight) {
                                    // weight cannot be 0 so remove the composable when weight
                                    // becomes 0
                                    if (
                                        secondaryAction != null && secondaryActionWeight.value > 0
                                    ) {
                                        Spacer(Modifier.size(SwipeToRevealDefaults.Padding))
                                        ActionSlot(
                                            weight = secondaryActionWeight.value,
                                            opacity = secondaryActionAlpha,
                                            content = secondaryAction,
                                        )
                                    }
                                    Spacer(Modifier.size(SwipeToRevealDefaults.Padding))
                                    ActionSlot(
                                        content = primaryAction,
                                        opacity = primaryActionAlpha
                                    )
                                } else {
                                    ActionSlot(
                                        content = primaryAction,
                                        opacity = primaryActionAlpha
                                    )
                                    Spacer(Modifier.size(SwipeToRevealDefaults.Padding))
                                    // weight cannot be 0 so remove the composable when weight
                                    // becomes 0
                                    if (
                                        secondaryAction != null && secondaryActionWeight.value > 0
                                    ) {
                                        ActionSlot(
                                            weight = secondaryActionWeight.value,
                                            opacity = secondaryActionAlpha,
                                            content = secondaryAction,
                                        )
                                        Spacer(Modifier.size(SwipeToRevealDefaults.Padding))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier =
                    Modifier.absoluteOffset {
                        val xOffset = state.requireOffset().roundToInt()
                        IntOffset(
                            x = if (canSwipeRight) xOffset else xOffset.coerceAtMost(0),
                            y = 0
                        )
                    }
            ) {
                content()
            }
            LaunchedEffect(state.currentValue) {
                if (
                    (state.currentValue == LeftRevealed || state.currentValue == RightRevealed) &&
                        state.lastActionType == RevealActionType.None
                ) {
                    onFullSwipe()
                }
            }
        }
    }
}

@Stable
private class DefaultGestureInclusion(
    private val revealState: RevealState,
    private val edgeZoneFraction: Float
) : GestureInclusion {
    override fun ignoreGestureStart(offset: Offset, layoutCoordinates: LayoutCoordinates): Boolean {
        val screenOffset = layoutCoordinates.localToScreen(offset)
        val screenWidth = layoutCoordinates.findRootCoordinates().size.width
        return revealState.currentValue == Covered &&
            screenOffset.x <= screenWidth * edgeZoneFraction
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DefaultGestureInclusion

        if (edgeZoneFraction != other.edgeZoneFraction) return false
        if (revealState != other.revealState) return false

        return true
    }

    override fun hashCode(): Int {
        var result = edgeZoneFraction.hashCode()
        result = 31 * result + revealState.hashCode()
        return result
    }
}

@Stable
private object BidirectionalGestureInclusion : GestureInclusion {
    override fun ignoreGestureStart(offset: Offset, layoutCoordinates: LayoutCoordinates): Boolean =
        false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

@Composable
private fun RowScope.ActionSlot(
    modifier: Modifier = Modifier,
    weight: Float = 1f,
    opacity: State<Float> = mutableFloatStateOf(1f),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.weight(weight).graphicsLayer { alpha = opacity.value },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun fadeInUndo(): ContentTransform =
    ContentTransform(
        // animation spec for the fading in undo action (fadeIn + scaleIn)
        targetContentEnter =
            fadeIn(
                animationSpec =
                    tween(
                        durationMillis = RAPID_ANIMATION,
                        delayMillis = FLASH_ANIMATION,
                        easing = LinearEasing,
                    )
            ) +
                scaleIn(
                    initialScale = 1.2f,
                    animationSpec =
                        tween(
                            durationMillis = RAPID_ANIMATION,
                            delayMillis = FLASH_ANIMATION,
                            easing = STANDARD_IN_OUT
                        )
                ),
        // animation spec for the fading out content and actions (fadeOut)
        initialContentExit =
            fadeOut(animationSpec = tween(durationMillis = FLASH_ANIMATION, easing = LinearEasing))
    )

private fun fadeOutUndo(): ContentTransform =
    ContentTransform(
        // No animation, fade-in in 0 milliseconds since enter transition is mandatory
        targetContentEnter =
            fadeIn(animationSpec = tween(durationMillis = 0, delayMillis = SHORT_ANIMATION)),

        // animation spec for the fading out undo action (fadeOut + scaleOut)
        initialContentExit =
            fadeOut(animationSpec = tween(durationMillis = SHORT_ANIMATION, easing = LinearEasing))
    )

private fun calculateVerticalOffsetBasedOnScreenPosition(
    childHeight: Int,
    globalPosition: LayoutCoordinates?
): Int {
    if (globalPosition == null || !globalPosition.positionOnScreen().isSpecified) {
        return 0
    }
    val positionOnScreen = globalPosition.positionOnScreen()
    val boundsInWindow = globalPosition.boundsInWindow()
    val parentTop = positionOnScreen.y.toInt()
    val parentHeight = globalPosition.size.height
    val parentBottom = parentTop + parentHeight
    if (parentTop >= boundsInWindow.top && parentBottom <= boundsInWindow.bottom) {
        // Don't offset if the item is fully on screen
        return 0
    }

    // Avoid going outside parent bounds
    val minCenter = parentTop + childHeight / 2
    val maxCenter = parentTop + parentHeight - childHeight / 2
    val desiredCenter = boundsInWindow.center.y.toInt().coerceIn(minCenter, maxCenter)
    val actualCenter = parentTop + parentHeight / 2
    return desiredCenter - actualCenter
}

internal const val CustomTouchSlopMultiplier = 1.20f

/** Short animation in milliseconds. */
private const val SHORT_ANIMATION = 50

/** Flash animation length in milliseconds. */
private const val FLASH_ANIMATION = 100

/** Rapid animation length in milliseconds. */
private const val RAPID_ANIMATION = 200

/** Quick animation length in milliseconds. */
private const val QUICK_ANIMATION = 250

/** Standard easing for Swipe To Reveal. */
private val STANDARD_IN_OUT = CubicBezierEasing(0.20f, 0.0f, 0.0f, 1.00f)

private val ICON_AND_TEXT_PADDING = 4.dp

private val ACTION_BUTTON_CONTENT_PADDING = 4.dp

// Swipe required to start displaying the action buttons.
private const val BUTTON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE = 0.06f

// End threshold for the fade in progression of the action buttons.
private const val BUTTON_FADE_IN_END_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE = 0.12f

// Swipe required to start displaying the icon for a single action button.
private const val SINGLE_ICON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE = 0.15f

// Swipe required to start displaying the icon for two action buttons.
private const val DOUBLE_ICON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE = 0.30f

// End threshold for the fade in progression of the icon for a single action button.
private const val SINGLE_ICON_FADE_IN_END_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE = 0.21f

// End threshold for the fade in progression of the icon for two action buttons.
private const val DOUBLE_ICON_FADE_IN_END_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE = 0.36f

private val FULL_SCREEN_PADDING_FRACTION = 0.0625f
