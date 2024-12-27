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
import android.graphics.Rect
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.VisibleForTesting
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import androidx.pdf.R

/**
 * Accessibility delegate for PdfView that provides a virtual view hierarchy for pages.
 *
 * This helper class allows accessibility services to interact with individual pages as virtual
 * views, enabling navigation and content exploration.
 */
internal class AccessibilityPageHelper(
    private val pdfView: PdfView,
    private val pageLayoutManager: PageLayoutManager,
    private val pageManager: PageManager
) : ExploreByTouchHelper(pdfView) {

    public override fun getVirtualViewAt(x: Float, y: Float): Int {
        val visiblePages = pageLayoutManager.visiblePages.value

        val contentX = pdfView.toContentX(x).toInt()
        val contentY = pdfView.toContentY(y).toInt()

        return (visiblePages.lower..visiblePages.upper).firstOrNull { page ->
            pageLayoutManager
                .getPageLocation(page, pdfView.getVisibleAreaInContentCoords())
                .contains(contentX, contentY)
        } ?: HOST_ID
    }

    public override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
        val visiblePages = pageLayoutManager.visiblePages.value
        virtualViewIds.addAll(visiblePages.lower..visiblePages.upper)
    }

    public override fun onPopulateNodeForVirtualView(
        virtualViewId: Int,
        @NonNull node: AccessibilityNodeInfoCompat
    ) {
        node.apply {
            // Set content description (use extracted text if available, otherwise a placeholder)
            val pageText = pageManager.pages.get(virtualViewId)?.pageText
            contentDescription =
                pageText?.let { getContentDescriptionForPage(pdfView.context, virtualViewId, it) }
                    ?: getDefaultDesc(pdfView.context, virtualViewId)

            val pageBounds =
                pageLayoutManager.getPageLocation(
                    virtualViewId,
                    pdfView.getVisibleAreaInContentCoords()
                )
            setBoundsInScreenFromBoundsInParent(node, scalePageBounds(pageBounds, pdfView.zoom))

            isFocusable = true
        }
    }

    override fun onPerformActionForVirtualView(
        virtualViewId: Int,
        action: Int,
        arguments: Bundle?
    ): Boolean {
        // This view does not handle any actions.
        return false
    }

    @VisibleForTesting
    fun scalePageBounds(bounds: Rect, zoom: Float): Rect {
        return Rect(
            (bounds.left * zoom).toInt(),
            (bounds.top * zoom).toInt(),
            (bounds.right * zoom).toInt(),
            (bounds.bottom * zoom).toInt()
        )
    }

    /**
     * Updates accessibility node with extracted page text when ready.
     *
     * @param pageNum 0-indexed page number.
     */
    fun onPageTextReady(pageNum: Int) {
        val pageText = pageManager.pages.get(pageNum)?.pageText

        if (pageText != null) {
            // Update accessibility node with new text.
            invalidateVirtualView(pageNum)
        }
    }

    companion object {

        /**
         * Builds the content description for a page.
         *
         * @param context The context for accessing resources.
         * @param pageText The extracted text content of the page, or null if not loaded.
         * @param pageNum The 0-indexed page number.
         * @return The content description string.
         */
        private fun getContentDescriptionForPage(
            context: Context,
            pageNum: Int,
            pageText: String?
        ): String {
            return when {
                pageText == null -> getDefaultDesc(context, pageNum)
                pageText.trim().isEmpty() -> context.getString(R.string.desc_empty_page)
                else -> context.getString(R.string.desc_page_with_text, pageNum + 1, pageText)
            }
        }

        /**
         * Gets the default content description for a page. This is used as a placeholder while the
         * actual content description is loading.
         *
         * @param context The context for accessing resources.
         * @param pageNum The 0-indexed page number.
         * @return The default content description string.
         */
        private fun getDefaultDesc(context: Context, pageNum: Int): String {
            return context.getString(R.string.desc_page, pageNum + 1)
        }
    }
}
