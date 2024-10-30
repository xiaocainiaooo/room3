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

import android.content.Context
import android.graphics.Point
import android.os.Looper
import android.view.View
import android.view.View.MeasureSpec
import androidx.pdf.PdfDocument
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewPaginationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testScope = TestScope()

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            // PdfView creates a ScaleGestureDetector internally, which can only be done from Looper
            // threads, so let's make sure we're executing on one
            Looper.prepare()
        }
    }

    @Test
    fun testPageVisibility() = runTest {
        // Layout at 500x1000, and expect to see pages [0, 4] at 100x200
        val pdfDocument = fakePdfDocument(10, Point(100, 200))
        val pdfView = setupPdfView(500, 1000, pdfDocument)

        pdfDocument.waitForLayout(untilPage = 4)

        assertThat(pdfView.firstVisiblePage).isEqualTo(0)
        assertThat(pdfView.visiblePagesCount).isEqualTo(5)
    }

    @Test
    fun testPageVisibility_withoutPdfDocument() {
        val pdfView = setupPdfViewOnMain(500, 1000, null)

        assertThat(pdfView.firstVisiblePage).isEqualTo(0)
        // Default visible pages is [0, 1]
        assertThat(pdfView.visiblePagesCount).isEqualTo(2)
    }

    @Test
    fun testPageVisibility_onSizeDecreased() = runTest {
        // Layout at 500x1000 initially, and expect to see pages [0, 3] at 100x300
        val pdfDocument = fakePdfDocument(10, Point(100, 300))
        val pdfView = setupPdfView(500, 1000, pdfDocument)
        pdfDocument.waitForLayout(untilPage = 3)
        assertThat(pdfView.firstVisiblePage).isEqualTo(0)
        assertThat(pdfView.visiblePagesCount).isEqualTo(4)

        // Reduce size to 100x200, and expect to see only page 0
        pdfView.layoutAndMeasure(100, 200)
        assertThat(pdfView.firstVisiblePage).isEqualTo(0)
        assertThat(pdfView.visiblePagesCount).isEqualTo(1)
    }

    @Test
    fun testPageVisibility_onScrollChanged() = runTest {
        // Layout at 1000x2000 initially, and expect to see pages [0, 3] at 200x500
        val pdfDocument = fakePdfDocument(10, Point(200, 500))
        val pdfView = setupPdfView(1000, 2000, pdfDocument)
        pdfDocument.waitForLayout(untilPage = 3)
        assertThat(pdfView.firstVisiblePage).isEqualTo(0)
        assertThat(pdfView.visiblePagesCount).isEqualTo(4)

        // Scroll until the viewport spans [500, 2500] vertically and expect to see pages [1, 4]
        pdfView.scrollBy(0, 500)
        pdfDocument.waitForLayout(untilPage = 4)
        assertThat(pdfView.firstVisiblePage).isEqualTo(1)
        assertThat(pdfView.visiblePagesCount).isEqualTo(4)
    }

    @Test
    fun testPageVisibility_onZoomChanged() = runTest {
        // Layout at 100x500 initially, and expect to see pages [0, 5] at 30x80
        val pdfDocument = fakePdfDocument(10, Point(30, 80))
        val pdfView = setupPdfView(100, 500, pdfDocument)
        pdfDocument.waitForLayout(untilPage = 5)
        assertThat(pdfView.firstVisiblePage).isEqualTo(0)
        assertThat(pdfView.visiblePagesCount).isEqualTo(6)

        // Set zoom to 2f and expect to see pages [0, 2]
        withContext(Dispatchers.Main) { pdfView.zoom = 2f }
        assertThat(pdfView.firstVisiblePage).isEqualTo(0)
        assertThat(pdfView.visiblePagesCount).isEqualTo(3)
    }

    /** Create, measure, and layout a [PdfView] at the specified [width] and [height] */
    private suspend fun setupPdfView(width: Int, height: Int, pdfDocument: FakePdfDocument?) =
        withContext(Dispatchers.Main) { setupPdfViewOnMain(width, height, pdfDocument) }

    private fun setupPdfViewOnMain(width: Int, height: Int, pdfDocument: PdfDocument?): PdfView {
        val pdfView = PdfView(context)
        pdfView.layoutAndMeasure(width, height)
        pdfDocument?.let { pdfView.pdfDocument = it }
        return pdfView
    }
}

/**
 * Laying out pages involves waiting for multiple coroutines that are started sequentially. It is
 * not possible to use TestScheduler alone to wait for a certain amount of layout to happen. This
 * uses a polling loop to wait for a certain number of pages to be laid out, up to [timeoutMillis]
 */
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun FakePdfDocument.waitForLayout(untilPage: Int, timeoutMillis: Long = 1000) {
    // Jump to Dispatchers.Default, as TestDispatcher will skip delays and timeouts
    withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(timeoutMillis) {
            while (layoutReach < untilPage) {
                delay(100)
            }
        }
    }
}

/** Returns a [FakePdfDocument] containing [pages] pages with uniform [pageSize] dimensions */
private fun fakePdfDocument(pages: Int, pageSize: Point): FakePdfDocument {
    return FakePdfDocument(pages, List(pages) { pageSize })
}

private fun View.layoutAndMeasure(width: Int, height: Int) {
    measure(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
    )
    layout(0, 0, measuredWidth, measuredHeight)
}
