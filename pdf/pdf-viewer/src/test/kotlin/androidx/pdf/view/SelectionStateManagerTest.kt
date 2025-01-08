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

package androidx.pdf.view

import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import androidx.pdf.PdfDocument
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.content.SelectionBoundary
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@SmallTest
@RunWith(RobolectricTestRunner::class)
class SelectionStateManagerTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // TODO(b/385407478) replace with FakePdfDocument when we're able to share it more broadly
    private val pdfDocument =
        mock<PdfDocument> {
            onBlocking { getSelectionBounds(any(), any(), any()) } doAnswer
                { invocation ->
                    val startPoint = invocation.getArgument<PointF>(1)
                    val endPoint = invocation.getArgument<PointF>(2)
                    pageSelectionFor(invocation.getArgument(0), startPoint, endPoint)
                }
        }

    private lateinit var selectionStateManager: SelectionStateManager

    @Before
    fun setup() {
        selectionStateManager = SelectionStateManager(pdfDocument, testScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun maybeSelectWordAtPoint() = runTest {
        val selectionPoint = PdfPoint(pageNum = 10, PointF(150F, 265F))
        val invalidations = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            selectionStateManager.invalidationSignalFlow.toList(invalidations)
        }

        selectionStateManager.maybeSelectWordAtPoint(selectionPoint)
        testDispatcher.scheduler.runCurrent()

        val selectionModel = selectionStateManager.selectionModel
        assertThat(selectionModel).isNotNull()
        assertThat(selectionModel?.selection).isInstanceOf(TextSelection::class.java)
        val selection = requireNotNull(selectionModel?.selection as TextSelection)
        assertThat(selection.bounds)
            .isEqualTo(
                listOf(
                    PdfRect(
                        selectionPoint.pageNum,
                        RectF(
                            selectionPoint.pagePoint.x,
                            selectionPoint.pagePoint.y,
                            selectionPoint.pagePoint.x,
                            selectionPoint.pagePoint.y
                        )
                    )
                )
            )
        assertThat(selection.text)
            .isEqualTo(
                "This is all the text between ${selectionPoint.pagePoint} and ${selectionPoint.pagePoint}"
            )

        assertThat(invalidations.size).isEqualTo(1)
    }

    @Test
    fun maybeSelectWordAtPoint_twice_lastSelectionWins() {
        val selectionPoint = PdfPoint(pageNum = 10, PointF(150F, 265F))
        val selectionPoint2 = PdfPoint(pageNum = 10, PointF(250F, 193F))

        selectionStateManager.maybeSelectWordAtPoint(selectionPoint)
        selectionStateManager.maybeSelectWordAtPoint(selectionPoint2)
        testDispatcher.scheduler.runCurrent()

        val selectionModel = selectionStateManager.selectionModel
        assertThat(selectionModel).isNotNull()
        assertThat(selectionModel?.selection).isInstanceOf(TextSelection::class.java)
        val selection = requireNotNull(selectionModel?.selection as TextSelection)
        assertThat(selection.bounds)
            .isEqualTo(
                listOf(
                    PdfRect(
                        selectionPoint2.pageNum,
                        RectF(
                            selectionPoint2.pagePoint.x,
                            selectionPoint2.pagePoint.y,
                            selectionPoint2.pagePoint.x,
                            selectionPoint2.pagePoint.y
                        )
                    )
                )
            )
        assertThat(selection.text)
            .isEqualTo(
                "This is all the text between ${selectionPoint2.pagePoint} and ${selectionPoint2.pagePoint}"
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun clearSelection() = runTest {
        val selectionPoint = PdfPoint(pageNum = 10, PointF(150F, 265F))
        val invalidations = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            selectionStateManager.invalidationSignalFlow.toList(invalidations)
        }

        selectionStateManager.maybeSelectWordAtPoint(selectionPoint)
        testDispatcher.scheduler.runCurrent()
        assertThat(selectionStateManager.selectionModel).isNotNull()
        selectionStateManager.clearSelection()

        assertThat(selectionStateManager.selectionModel).isNull()
        // One for the selection, one for clearing it
        assertThat(invalidations.size).isEqualTo(2)
    }

    @Test
    fun clearSelection_cancelsWork() {
        val selectionPoint = PdfPoint(pageNum = 10, PointF(150F, 265F))

        // Start a selection and don't finish it (i.e. no runCurrent)
        selectionStateManager.maybeSelectWordAtPoint(selectionPoint)
        assertThat(selectionStateManager.selectionModel).isNull()

        // Clear selection, flush the scheduler, and make sure selection remains null (i.e. the work
        // enqueued by our initial selection doesn't finish and supersede the cleared state)
        selectionStateManager.clearSelection()
        testDispatcher.scheduler.runCurrent()
        assertThat(selectionStateManager.selectionModel).isNull()
    }

    private fun pageSelectionFor(page: Int, start: PointF, end: PointF): PageSelection {
        return PageSelection(
            page,
            SelectionBoundary(point = Point(start.x.toInt(), start.y.toInt())),
            SelectionBoundary(point = Point(end.x.toInt(), end.y.toInt())),
            listOf(
                PdfPageTextContent(
                    listOf(
                        RectF(
                            minOf(start.x, end.x),
                            minOf(start.y, end.y),
                            maxOf(start.x, end.x),
                            maxOf(start.y, end.y)
                        )
                    ),
                    text = "This is all the text between $start and $end"
                )
            )
        )
    }
}
