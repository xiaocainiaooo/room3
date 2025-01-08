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

import android.graphics.PointF
import androidx.annotation.VisibleForTesting
import androidx.pdf.PdfDocument
import androidx.pdf.content.PageSelection

/** Value class containing all data necessary to display UI related to content selection */
internal class SelectionModel
@VisibleForTesting
internal constructor(
    val selection: Selection,
    val startBoundary: UiSelectionBoundary,
    val endBoundary: UiSelectionBoundary
) {
    companion object {
        /** Produces a [SelectionModel] from a single [PageSelection] on a single page */
        // TODO(b/386398335) Add support for creating a SelectionModel from selections on 2 pages
        fun fromSinglePageSelection(pageSelection: PageSelection): SelectionModel {
            val startPoint =
                requireNotNull(pageSelection.start.point) { "PageSelection is missing start point" }
            val stopPoint =
                requireNotNull(pageSelection.stop.point) { "PageSelection is missing end point" }
            return SelectionModel(
                pageSelection.toViewSelection(),
                UiSelectionBoundary(
                    PdfPoint(
                        pageSelection.page,
                        PointF(startPoint.x.toFloat(), startPoint.y.toFloat())
                    ),
                    pageSelection.start.isRtl
                ),
                UiSelectionBoundary(
                    PdfPoint(
                        pageSelection.page,
                        PointF(stopPoint.x.toFloat(), stopPoint.y.toFloat())
                    ),
                    pageSelection.stop.isRtl
                ),
            )
        }

        /**
         * Returns a [Selection] as exposed in the [PdfView] API from a [PageSelection] as produced
         * by the [PdfDocument] API
         */
        private fun PageSelection.toViewSelection(): Selection {
            val flattenedBounds =
                this.selectedTextContents.map { it.bounds }.flatten().map { PdfRect(this.page, it) }
            val concatenatedText = this.selectedTextContents.joinToString(" ") { it.text }
            return TextSelection(concatenatedText, flattenedBounds)
        }
    }
}

/**
 * Represents a selection boundary that includes the page on which it exists and the point at which
 * it exists (as [PdfPoint]), as well as the direction of the selection ([isRtl] is true if the
 * selection was made in a right-to-left direction).
 */
internal class UiSelectionBoundary(val location: PdfPoint, val isRtl: Boolean)
