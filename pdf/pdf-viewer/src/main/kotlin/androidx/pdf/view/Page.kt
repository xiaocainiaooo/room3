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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Size
import androidx.annotation.RestrictTo
import androidx.core.view.doOnDetach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A single PDF page that knows how to render and draw itself */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class Page(
    val pageNum: Int,
    val size: Size,
    private val pdfView: PdfView,
) {

    init {
        require(pageNum >= 0) { "Invalid negative page" }
        pdfView.doOnDetach { setInvisible() }
    }

    var isVisible: Boolean = false
        set(value) {
            field = value
            if (field) maybeRender() else setInvisible()
        }

    private var renderedZoom: Float? = null
    private var renderBitmapJob: Job? = null
    private var bitmap: Bitmap? = null

    fun maybeRender() {
        // If we're actively rendering or have rendered a bitmap for the current zoom level, there's
        // no need to refresh bitmaps
        if (renderedZoom?.equals(pdfView.zoom) == true && bitmap != null) return
        renderBitmapJob?.cancel()
        fetchNewBitmap()
    }

    fun draw(canvas: Canvas, locationInView: Rect) {
        bitmap?.let {
            if (DEBUG_DRAW) {
                canvas.drawRect(locationInView, DEBUG_PAINT)
                canvas.drawText(
                    "Page $pageNum",
                    locationInView.centerX().toFloat(),
                    locationInView.centerY().toFloat(),
                    DEBUG_PAINT_TEXT,
                )
            } else {
                canvas.drawBitmap(it, /* src= */ null, locationInView, BMP_PAINT)
            }
            return
        }
        canvas.drawRect(locationInView, BLANK_PAINT)
    }

    private fun fetchNewBitmap() {
        val bitmapSource =
            pdfView.pdfDocument?.getPageBitmapSource(pageNum)
                ?: throw IllegalStateException("No PDF document to render")
        renderBitmapJob =
            pdfView.coroutineScope.launch {
                if (!isActive) return@launch
                val zoom = pdfView.zoom
                val width = (size.width * zoom).toInt()
                val height = (size.height * zoom).toInt()
                renderedZoom = zoom
                bitmap = bitmapSource.getBitmap(Size(width, height))
                withContext(Dispatchers.Main) { pdfView.invalidate() }
            }
    }

    private fun setInvisible() {
        renderBitmapJob?.cancel()
        renderBitmapJob = null
        bitmap = null
        renderedZoom = null
    }
}

private const val DEBUG_DRAW = false
private val BMP_PAINT = Paint(Paint.FILTER_BITMAP_FLAG)
private val BLANK_PAINT =
    Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
private val DEBUG_PAINT =
    Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
private val DEBUG_PAINT_TEXT =
    Paint().apply {
        color = Color.RED
        textSize = 24f
    }
