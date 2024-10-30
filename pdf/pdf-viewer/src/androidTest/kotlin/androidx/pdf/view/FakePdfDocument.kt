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

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.util.Size
import android.util.SparseArray
import androidx.annotation.OpenForTesting
import androidx.annotation.RequiresExtension
import androidx.pdf.PdfDocument
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.content.PageSelection
import androidx.pdf.content.SelectionBoundary
import kotlin.random.Random

/** Fake implementation of [PdfDocument], for testing */
@OpenForTesting
internal open class FakePdfDocument(
    override val pageCount: Int,
    /** A list of (x, y) page dimensions in content coordinates */
    private val pages: List<Point>,
    override val formType: Int = PDF_FORM_TYPE_NONE,
    override val isLinearized: Boolean = false,
) : PdfDocument {
    override val uri: Uri
        get() = Uri.parse("content://test.app/document.pdf")

    @get:Synchronized @set:Synchronized internal var layoutReach: Int = 0

    init {
        require(pages.size == pageCount) { "Invalid page count" }
    }

    override fun getPageBitmapSource(pageNumber: Int): PdfDocument.BitmapSource {
        return FakeBitmapSource(pageNumber)
    }

    override suspend fun getPageLinks(pageNumber: Int): PdfDocument.PdfPageLinks {
        // TODO(b/376136907) provide a useful implementation when it's needed for testing
        return PdfDocument.PdfPageLinks(listOf(), listOf())
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    override suspend fun getPageContent(pageNumber: Int): PdfDocument.PdfPageContent {
        // TODO(b/376136746) provide a useful implementation when it's needed for testing
        return PdfDocument.PdfPageContent(listOf(), listOf())
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    override suspend fun getSelectionBounds(
        pageNumber: Int,
        start: PointF,
        stop: PointF
    ): PageSelection {
        // TODO(b/376136631) provide a useful implementation when it's needed for testing
        return PageSelection(0, SelectionBoundary(0), SelectionBoundary(0), listOf())
    }

    override suspend fun searchDocument(
        query: String,
        pageRange: IntRange
    ): SparseArray<List<PageMatchBounds>> {
        // TODO - provide a useful implementation when it's needed for testing
        return SparseArray()
    }

    override suspend fun getPageInfos(pageRange: IntRange): List<PdfDocument.PageInfo> {
        return pageRange.map { getPageInfo(it) }
    }

    override suspend fun getPageInfo(pageNumber: Int): PdfDocument.PageInfo {
        layoutReach = maxOf(pageNumber, layoutReach)
        val size = pages[pageNumber]
        return PdfDocument.PageInfo(pageNumber, size.y, size.x)
    }

    override fun close() {
        // No-op, fake
    }

    /**
     * A fake [PdfDocument.BitmapSource] that produces random RGB [Bitmap]s of the requested size
     */
    private class FakeBitmapSource(override val pageNumber: Int) : PdfDocument.BitmapSource {

        override suspend fun getBitmap(scaledPageSizePx: Size, tileRegion: Rect?): Bitmap {
            // Fake: generate a solid random RGB bitmap at the requested size
            val size =
                if (tileRegion != null) Size(tileRegion.width(), tileRegion.height())
                else scaledPageSizePx
            val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            bitmap.apply {
                val colorRng = Random(System.currentTimeMillis())
                eraseColor(
                    Color.argb(
                        255,
                        colorRng.nextInt(256),
                        colorRng.nextInt(256),
                        colorRng.nextInt(256)
                    )
                )
            }
            return bitmap
        }

        override fun close() {
            /* No-op, fake */
        }
    }
}

// Duplicated from PdfRenderer to avoid a hard dependency on SDK 35
internal const val PDF_FORM_TYPE_NONE = 0
/** Represents a PDF with form fields specified using the AcroForm spec */
internal const val PDF_FORM_TYPE_ACRO_FORM = 1
/** Represents a PDF with form fields specified using the entire XFA spec */
internal const val PDF_FORM_TYPE_XFA_FULL = 2
/** Represents a PDF with form fields specified using the XFAF subset of the XFA spec */
internal const val PDF_FORM_TYPE_XFA_FOREGROUND = 3
