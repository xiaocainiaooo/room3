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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewRenderingTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            // PdfView creates a ScaleGestureDetector internally, which can only be done from Looper
            // threads, so let's make sure we're executing on one
            if (Looper.myLooper() == null) {
                Looper.prepare()
            }
        }
    }

    @Test
    fun testPageRendering() = runTest {
        // Layout at 500x1000, and expect to render pages [0, 4] at 100x200
        val pdfDocument = FakePdfDocument(List(10) { Point(100, 200) })
        setupPdfView(500, 1000, pdfDocument)

        pdfDocument.waitForRender(untilPage = 4)

        val requestedBitmaps = pdfDocument.bitmapRequests
        for (i in 0..4) {
            assertThat((requestedBitmaps[i] as? FullBitmap)?.scaledPageSizePx?.width).isEqualTo(100)
            assertThat((requestedBitmaps[i] as? FullBitmap)?.scaledPageSizePx?.height)
                .isEqualTo(200)
        }
    }

    @Test
    fun testPageRendering_renderNewPagesOnScroll() = runTest {
        // Layout at 500x1000, and expect to render pages [0, 4] at 100x200
        val pdfDocument = FakePdfDocument(List(10) { Point(100, 200) })
        val pdfView = setupPdfView(500, 1000, pdfDocument)
        pdfDocument.waitForRender(untilPage = 4)

        // Scroll until the viewport spans [1000, 2000] vertically and expect to render pages
        // [5, 9] at 200x400
        pdfView.scrollTo(0, 1000)
        pdfDocument.waitForRender(untilPage = 9)
        val requestedBitmaps = pdfDocument.bitmapRequests
        for (i in 0..4) {
            assertThat((requestedBitmaps[i] as? FullBitmap)?.scaledPageSizePx?.width).isEqualTo(100)
            assertThat((requestedBitmaps[i] as? FullBitmap)?.scaledPageSizePx?.height)
                .isEqualTo(200)
        }
    }

    @Test
    fun testPageRendering_renderNewBitmapsOnZoom() = runTest {
        // Layout at 500x1000, and expect to render pages [0, 4] at 100x200
        val pdfDocument = FakePdfDocument(List(10) { Point(100, 200) })
        val pdfView = setupPdfView(500, 1000, pdfDocument)
        pdfDocument.waitForRender(untilPage = 4)

        // Set zoom to 2f, and expect to render pages [0, 2] at 200x400
        // Reset render reach, as we expect to start rendering new Bitmaps for already-rendered
        // pages
        pdfDocument.renderReach = 0
        withContext(Dispatchers.Main) { pdfView.zoom = 2f }
        pdfDocument.waitForRender(2)
        val requestedBitmaps = pdfDocument.bitmapRequests
        for (i in 0..2) {
            assertThat((requestedBitmaps[i] as? FullBitmap)?.scaledPageSizePx?.width).isEqualTo(200)
            assertThat((requestedBitmaps[i] as? FullBitmap)?.scaledPageSizePx?.height)
                .isEqualTo(400)
        }
    }

    /** Create, measure, and layout a [PdfView] at the specified [width] and [height] */
    private suspend fun setupPdfView(
        width: Int,
        height: Int,
        pdfDocument: FakePdfDocument?
    ): PdfView {
        return withContext(Dispatchers.Main) {
            setupPdfViewOnMainThread(width, height, pdfDocument)
        }
    }

    private fun setupPdfViewOnMainThread(
        width: Int,
        height: Int,
        pdfDocument: FakePdfDocument?
    ): PdfView {
        val pdfView = PdfView(context)
        pdfView.layoutAndMeasure(width, height)
        pdfDocument?.let { pdfView.pdfDocument = it }
        return pdfView
    }
}

private fun View.layoutAndMeasure(width: Int, height: Int) {
    measure(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
    )
    layout(0, 0, measuredWidth, measuredHeight)
}
