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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class InteractiveListTest {
    @get:Rule val rule = createComposeRule()
    val ListTag = "list"
    val LeadingTag = "leading"
    val TrailingTag = "trailing"
    val OverlineTag = "overline"
    val SupportingTag = "supporting"
    val HeadlineTag = "headline"

    @Test
    fun clickableListItem_intrinsicSize() {
        val headlineSize = 200.dp
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.height(IntrinsicSize.Min)) {
                ClickableListItem(
                    modifier = Modifier.fillMaxHeight().testTag(ListTag),
                    headlineContent = { Box(Modifier.size(headlineSize)) },
                    leadingContent = { Box(Modifier.fillMaxHeight().testTag(LeadingTag)) },
                    trailingContent = { Box(Modifier.fillMaxHeight().testTag(TrailingTag)) },
                )
            }
        }

        val expectedHeight = headlineSize + InteractiveListTopPadding + InteractiveListBottomPadding
        rule.onNodeWithTag(ListTag, useUnmergedTree = true).assertHeightIsEqualTo(expectedHeight)
        rule.onNodeWithTag(LeadingTag, useUnmergedTree = true).assertHeightIsEqualTo(headlineSize)
        rule.onNodeWithTag(TrailingTag, useUnmergedTree = true).assertHeightIsEqualTo(headlineSize)
    }

    @Test
    fun clickableListItem_multipleItems_intrinsicSize() {
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.width(300.dp).height(IntrinsicSize.Min)) {
                // 2 identical list items. Leading content leaves small space
                // for headline, so it has to wrap.
                ClickableListItem(
                    modifier = Modifier.testTag("ListItem1"),
                    leadingContent = { Box(Modifier.width(240.dp)) },
                    headlineContent = { Text("A B C D E F G H") },
                )
                ClickableListItem(
                    modifier = Modifier.testTag("ListItem2"),
                    leadingContent = { Box(Modifier.width(240.dp)) },
                    headlineContent = { Text("A B C D E F G H") },
                )
            }
        }

        val item1Height =
            rule
                .onNodeWithTag("ListItem1", useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
                .height
        rule.onNodeWithTag("ListItem2", useUnmergedTree = true).assertHeightIsEqualTo(item1Height)
    }

    @Test
    fun clickableListItem_verticalAlignmentCenter_positioning() {
        val height = InteractiveListVerticalAlignmentBreakpoint - 10.dp
        rule.setMaterialContent(lightColorScheme()) {
            ClickableListItem(
                modifier = Modifier.height(height),
                leadingContent = { Box(Modifier.testTag(LeadingTag).size(48.dp)) },
                trailingContent = { Box(Modifier.testTag(TrailingTag).size(48.dp)) },
                overlineContent = { Text("Overline", Modifier.testTag(OverlineTag)) },
                supportingContent = { Text("Supporting", Modifier.testTag(SupportingTag)) },
                headlineContent = { Text("Content", Modifier.testTag(HeadlineTag)) },
            )
        }

        val leadingBounds =
            rule.onNodeWithTag(LeadingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val overlineBounds =
            rule.onNodeWithTag(OverlineTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val supportingBounds =
            rule.onNodeWithTag(SupportingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val headlineBounds =
            rule.onNodeWithTag(HeadlineTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trailingNodeBounds =
            rule.onNodeWithTag(TrailingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()

        leadingBounds.left.assertIsEqualTo(InteractiveListStartPadding)
        leadingBounds.top.assertIsEqualTo((rule.rootHeight() - leadingBounds.height) / 2)

        val mainContentX = leadingBounds.right + InteractiveListInternalSpacing
        val mainContentHeight =
            overlineBounds.height + supportingBounds.height + headlineBounds.height
        overlineBounds.top.assertIsEqualTo((rule.rootHeight() - mainContentHeight) / 2)
        overlineBounds.left.assertIsEqualTo(mainContentX)
        supportingBounds.left.assertIsEqualTo(mainContentX)
        headlineBounds.left.assertIsEqualTo(mainContentX)

        trailingNodeBounds.right.assertIsEqualTo(rule.rootWidth() - InteractiveListEndPadding)
        trailingNodeBounds.top.assertIsEqualTo((rule.rootHeight() - trailingNodeBounds.height) / 2)
    }

    @Test
    fun clickableListItem_verticalAlignmentTop_positioning() {
        val height = InteractiveListVerticalAlignmentBreakpoint + 10.dp
        rule.setMaterialContent(lightColorScheme()) {
            ClickableListItem(
                modifier = Modifier.height(height),
                leadingContent = { Box(Modifier.testTag(LeadingTag).size(48.dp)) },
                trailingContent = { Box(Modifier.testTag(TrailingTag).size(48.dp)) },
                overlineContent = { Text("Overline", Modifier.testTag(OverlineTag)) },
                supportingContent = { Text("Supporting", Modifier.testTag(SupportingTag)) },
                headlineContent = { Text("Content", Modifier.testTag(HeadlineTag)) },
            )
        }

        val leadingBounds =
            rule.onNodeWithTag(LeadingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val overlineBounds =
            rule.onNodeWithTag(OverlineTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val supportingBounds =
            rule.onNodeWithTag(SupportingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val headlineBounds =
            rule.onNodeWithTag(HeadlineTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trailingNodeBounds =
            rule.onNodeWithTag(TrailingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()

        leadingBounds.left.assertIsEqualTo(InteractiveListStartPadding)
        leadingBounds.top.assertIsEqualTo(InteractiveListTopPadding)

        val mainContentX = leadingBounds.right + InteractiveListInternalSpacing
        overlineBounds.top.assertIsEqualTo(InteractiveListTopPadding)
        overlineBounds.left.assertIsEqualTo(mainContentX)
        supportingBounds.left.assertIsEqualTo(mainContentX)
        headlineBounds.left.assertIsEqualTo(mainContentX)

        trailingNodeBounds.right.assertIsEqualTo(rule.rootWidth() - InteractiveListEndPadding)
        trailingNodeBounds.top.assertIsEqualTo(InteractiveListTopPadding)
    }

    @Test
    fun clickableListItem_verticalAlignmentCenter_positioning_rtl() {
        val height = InteractiveListVerticalAlignmentBreakpoint - 10.dp
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ClickableListItem(
                    modifier = Modifier.height(height),
                    leadingContent = { Box(Modifier.testTag(LeadingTag).size(48.dp)) },
                    trailingContent = { Box(Modifier.testTag(TrailingTag).size(48.dp)) },
                    overlineContent = { Text("Overline", Modifier.testTag(OverlineTag)) },
                    supportingContent = { Text("Supporting", Modifier.testTag(SupportingTag)) },
                    headlineContent = { Text("Content", Modifier.testTag(HeadlineTag)) },
                )
            }
        }

        val leadingBounds =
            rule.onNodeWithTag(LeadingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val overlineBounds =
            rule.onNodeWithTag(OverlineTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val supportingBounds =
            rule.onNodeWithTag(SupportingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val headlineBounds =
            rule.onNodeWithTag(HeadlineTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trailingNodeBounds =
            rule.onNodeWithTag(TrailingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()

        leadingBounds.right.assertIsEqualTo(rule.rootWidth() - InteractiveListStartPadding)
        leadingBounds.top.assertIsEqualTo((rule.rootHeight() - leadingBounds.height) / 2)

        val mainContentRightX = leadingBounds.left - InteractiveListInternalSpacing
        val mainContentHeight =
            overlineBounds.height + supportingBounds.height + headlineBounds.height
        overlineBounds.top.assertIsEqualTo((rule.rootHeight() - mainContentHeight) / 2)
        overlineBounds.right.assertIsEqualTo(mainContentRightX)
        supportingBounds.right.assertIsEqualTo(mainContentRightX)
        headlineBounds.right.assertIsEqualTo(mainContentRightX)

        trailingNodeBounds.left.assertIsEqualTo(InteractiveListEndPadding)
        trailingNodeBounds.top.assertIsEqualTo((rule.rootHeight() - trailingNodeBounds.height) / 2)
    }

    @Test
    fun clickableListItem_verticalAlignmentTop_positioning_rtl() {
        val height = InteractiveListVerticalAlignmentBreakpoint + 10.dp
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ClickableListItem(
                    modifier = Modifier.height(height),
                    leadingContent = { Box(Modifier.testTag(LeadingTag).size(48.dp)) },
                    trailingContent = { Box(Modifier.testTag(TrailingTag).size(48.dp)) },
                    overlineContent = { Text("Overline", Modifier.testTag(OverlineTag)) },
                    supportingContent = { Text("Supporting", Modifier.testTag(SupportingTag)) },
                    headlineContent = { Text("Content", Modifier.testTag(HeadlineTag)) },
                )
            }
        }

        val leadingBounds =
            rule.onNodeWithTag(LeadingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val overlineBounds =
            rule.onNodeWithTag(OverlineTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val supportingBounds =
            rule.onNodeWithTag(SupportingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val headlineBounds =
            rule.onNodeWithTag(HeadlineTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trailingNodeBounds =
            rule.onNodeWithTag(TrailingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()

        leadingBounds.right.assertIsEqualTo(rule.rootWidth() - InteractiveListStartPadding)
        leadingBounds.top.assertIsEqualTo(InteractiveListTopPadding)

        val mainContentRightX = leadingBounds.left - InteractiveListInternalSpacing
        overlineBounds.top.assertIsEqualTo(InteractiveListTopPadding)
        overlineBounds.right.assertIsEqualTo(mainContentRightX)
        supportingBounds.right.assertIsEqualTo(mainContentRightX)
        headlineBounds.right.assertIsEqualTo(mainContentRightX)

        trailingNodeBounds.left.assertIsEqualTo(InteractiveListEndPadding)
        trailingNodeBounds.top.assertIsEqualTo(InteractiveListTopPadding)
    }
}
