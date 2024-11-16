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

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.util.Range
import android.util.SparseArray
import androidx.annotation.VisibleForTesting
import androidx.core.util.keyIterator
import androidx.core.util.valueIterator
import androidx.pdf.PdfDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages a collection of [Page]s, each representing a single PDF page. Receives events to update
 * pages' internal state, and produces events via a [StateFlow] of type [Unit] to signal the host
 * [PdfView] to invalidate itself when any page needs to be redrawn. Operations like drawing pages
 * and handling touch events on pages may be delegated to this manager.
 *
 * Not thread safe
 */
internal class PageManager(
    private val pdfDocument: PdfDocument,
    private val backgroundScope: CoroutineScope,
    private val pagePrefetchRadius: Int,
) {
    /** Replay at least 1 value in case of pages rendered while we're not collecting */
    private val _updatedPagesFlow = MutableSharedFlow<Int>(replay = 1)
    val updatedPagesFlow: SharedFlow<Int>
        get() = _updatedPagesFlow

    @VisibleForTesting val pages = SparseArray<Page>()

    /**
     * Updates the internal state of [Page]s owned by this manager in response to a viewport change
     */
    fun maybeUpdateBitmaps(visiblePages: Range<Int>, currentZoomLevel: Float) {
        // Start preparing UI for visible pages
        for (i in visiblePages.lower..visiblePages.upper) {
            pages[i]?.setVisible(currentZoomLevel)
        }

        // Hide pages that are well outside the viewport. We deliberately don't set pages that
        // are within nearPages, but outside visible pages to invisible to avoid rendering churn
        // for pages likely to return to the viewport.
        val nearPages =
            Range(
                maxOf(0, visiblePages.lower - pagePrefetchRadius),
                minOf(visiblePages.upper + pagePrefetchRadius, pdfDocument.pageCount - 1),
            )
        for (pageNum in pages.keyIterator()) {
            if (pageNum < nearPages.lower || pageNum > nearPages.upper) {
                pages[pageNum]?.setInvisible()
            }
        }
    }

    /**
     * Updates the set of [Page]s owned by this manager when a new Page's dimensions are loaded.
     * Dimensions are the minimum data required to instantiate a page.
     */
    fun onPageSizeReceived(pageNum: Int, size: Point, isVisible: Boolean, currentZoomLevel: Float) {
        if (pages.contains(pageNum)) return
        val page =
            Page(pageNum, size, pdfDocument, backgroundScope) { _updatedPagesFlow.tryEmit(pageNum) }
                .apply { if (isVisible) setVisible(currentZoomLevel) }
        pages.put(pageNum, page)
    }

    /** Draws the [Page] at [pageNum] to the canvas at [locationInView] */
    fun drawPage(pageNum: Int, canvas: Canvas, locationInView: Rect) {
        pages.get(pageNum)?.draw(canvas, locationInView)
    }

    /**
     * Sets all [Page]s owned by this manager to invisible, i.e. to reduce memory when the host
     * [PdfView] is not in an interactive state.
     */
    fun onDetached() {
        for (page in pages.valueIterator()) {
            page.setInvisible()
        }
    }
}
