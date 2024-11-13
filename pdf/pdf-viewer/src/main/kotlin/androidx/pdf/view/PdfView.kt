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
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.os.Looper
import android.util.AttributeSet
import android.util.Range
import android.util.Size
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RestrictTo
import androidx.core.util.keyIterator
import androidx.pdf.PdfDocument
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A [View] for presenting PDF content, represented by [PdfDocument].
 *
 * This View supports zooming, scrolling, and flinging. Zooming is supported via pinch gesture,
 * quick scale gesture, and double tap to zoom in or snap back to fitting the page width inside its
 * bounds. Zoom can be changed using the [zoom] property, which is notably distinct from
 * [View.getScaleX] / [View.getScaleY]. Scroll position is based on the [View.getScrollX] /
 * [View.getScrollY] properties.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public open class PdfView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    View(context, attrs, defStyle) {
    /** Supply a [PdfDocument] to process the PDF content for rendering */
    public var pdfDocument: PdfDocument? = null
        set(value) {
            checkMainThread()
            value?.let {
                val reset = field != null && field?.uri != value.uri
                field = it
                if (reset) reset()
                onDocumentSet()
            }
        }

    /**
     * The [CoroutineScope] used to make suspending calls to [PdfDocument]. The size of the fixed
     * thread pool is arbitrary and subject to tuning.
     */
    internal val coroutineScope: CoroutineScope =
        CoroutineScope(Executors.newFixedThreadPool(5).asCoroutineDispatcher())

    /** The maximum scaling factor that can be applied to this View using the [zoom] property */
    // TODO(b/376299551) - Make maxZoom configurable via XML attribute
    public var maxZoom: Float = DEFAULT_MAX_ZOOM

    /** The minimum scaling factor that can be applied to this View using the [zoom] property */
    // TODO(b/376299551) - Make minZoom configurable via XML attribute
    public var minZoom: Float = DEFAULT_MIN_ZOOM

    /**
     * The zoom level of this view, as a factor of the content's natural size with when 1 pixel is
     * equal to 1 PDF point. Will always be clamped within ([minZoom], [maxZoom])
     */
    public var zoom: Float = DEFAULT_INIT_ZOOM
        set(value) {
            checkMainThread()
            field = value
            updateVisibleContent()
            invalidate()
        }

    /**
     * The radius of pages around the current viewport for which dimensions and other metadata will
     * be loaded
     */
    // TODO(b/376299551) - Make page prefetch radius configurable via XML attribute
    public var pagePrefetchRadius: Int = 1

    private var visiblePages: Range<Int> = Range(0, 1)
        set(value) {
            // Debounce setting the range to the same value
            if (field == value) return
            field = value
            onVisiblePagesChanged()
        }

    /** The first page in the viewport, including partially-visible pages. 0-indexed. */
    public val firstVisiblePage: Int
        get() = visiblePages.lower

    /** The number of pages visible in the viewport, including partially visible pages */
    public val visiblePagesCount: Int
        get() = visiblePages.upper - visiblePages.lower + 1

    /**
     * [PdfDocument] is backed by a single-threaded PDF parser, so only allow one thread to access
     * at a time
     */
    private var pdfDocumentMutex = Mutex()
    private var paginationModel: PaginationModel? = null

    private val pages = SparseArray<Page>()

    private val gestureHandler = ZoomScrollGestureHandler(this@PdfView)
    private val gestureTracker = GestureTracker(context).apply { delegate = gestureHandler }

    // To avoid allocations during drawing
    private val visibleAreaRect = Rect()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val localPaginationModel = paginationModel ?: return
        canvas.scale(zoom, zoom)
        for (i in visiblePages.lower..visiblePages.upper) {
            pages[i]?.draw(
                canvas,
                localPaginationModel.getPageLocation(i, getVisibleAreaInContentCoords())
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val handled = event?.let { gestureTracker.feed(it) } ?: false
        return handled || super.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateVisibleContent()
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        updateVisibleContent()
    }

    /** Start using the [PdfDocument] to present PDF content */
    private fun onDocumentSet() {
        val localPdfDocument = pdfDocument ?: return
        // TODO(b/376299551) - Make page margin configurable via XML attribute
        paginationModel = PaginationModel(DEFAULT_PAGE_SPACING_PX, localPdfDocument.pageCount)
        updateVisibleContent()
    }

    /**
     * Compute what content is visible from the current position of this View. Generally invoked on
     * position or size changes.
     */
    internal fun updateVisibleContent() {
        val localPaginationModel = paginationModel ?: return

        val contentTop = round(scrollY / zoom).toInt()
        val contentBottom = round((height + scrollY) / zoom).toInt()
        visiblePages = localPaginationModel.getPagesInViewport(contentTop, contentBottom)

        // If scale changed, update already-visible pages so they can re-render and redraw
        // themselves accordingly
        if (!gestureHandler.scaleInProgress && !gestureHandler.scrollInProgress) {
            for (i in visiblePages.lower..visiblePages.upper) {
                pages[i]?.maybeRender()
            }
        }
    }

    private fun reset() {
        scrollTo(0, 0)
        zoom = DEFAULT_INIT_ZOOM
        pages.clear()
        coroutineScope.coroutineContext.cancelChildren()
    }

    /** React to a change in visible pages (load new pages and clean up old ones) */
    private fun onVisiblePagesChanged() {
        val localPaginationModel = paginationModel ?: return
        val nearPages =
            Range(
                maxOf(0, visiblePages.lower - pagePrefetchRadius),
                minOf(visiblePages.upper + pagePrefetchRadius, localPaginationModel.numPages - 1),
            )

        // Fetch dimensions for near pages
        for (i in max(localPaginationModel.reach - 1, nearPages.lower)..nearPages.upper) {
            loadPageDimensions(i)
        }

        for (i in visiblePages.lower..visiblePages.upper) {
            pages[i]?.isVisible = true
        }

        // Clean up pages that are no longer visible
        for (pageIndex in pages.keyIterator()) {
            if (pageIndex < nearPages.lower || pageIndex > nearPages.upper) {
                pages[pageIndex]?.isVisible = false
            }
        }
    }

    /** Loads dimensions for a single page */
    private fun loadPageDimensions(pageNum: Int) {
        coroutineScope.launch {
            val pageMetadata = withPdfDocument { it.getPageInfo(pageNum) }
            // Update mutable state on the main thread
            withContext(Dispatchers.Main) {
                val localPaginationModel =
                    paginationModel ?: throw IllegalStateException("No PdfDocument")
                localPaginationModel.addPage(
                    pageNum,
                    Point(pageMetadata.width, pageMetadata.height)
                )
                val page =
                    Page(pageNum, Size(pageMetadata.width, pageMetadata.height), this@PdfView)
                pages[pageNum] = page
                if (pageNum >= visiblePages.lower && pageNum <= visiblePages.upper) {
                    // Make the page visible if it is, so it starts to render itself
                    page.isVisible = true
                }
                // Learning the dimensions of a page might affect our understanding of which pages
                // are visible
                updateVisibleContent()
            }
        }
    }

    /** Set the zoom, using the given point as a pivot point to zoom in or out of */
    internal fun zoomTo(zoom: Float, pivotX: Float, pivotY: Float) {
        // TODO(b/376299551) - Restore to developer-configured initial zoom value once that API is
        // implemented
        val newZoom = if (Float.NaN.equals(zoom)) DEFAULT_INIT_ZOOM else zoom
        val deltaX = scrollDeltaNeededForZoomChange(this.zoom, newZoom, pivotX, scrollX)
        val deltaY = scrollDeltaNeededForZoomChange(this.zoom, newZoom, pivotY, scrollY)

        this.zoom = newZoom
        scrollBy(deltaX, deltaY)
    }

    private fun scrollDeltaNeededForZoomChange(
        oldZoom: Float,
        newZoom: Float,
        pivot: Float,
        scroll: Int,
    ): Int {
        // Find where the given pivot point would move to when we change the zoom, and return the
        // delta.
        val contentPivot = toContentCoord(pivot, oldZoom, scroll)
        val movedZoomViewPivot: Float = toViewCoord(contentPivot, newZoom, scroll)
        return (movedZoomViewPivot - pivot).toInt()
    }

    /**
     * Computes the part of the content visible within the outer part of this view (including this
     * view's padding) in co-ordinates of the content.
     */
    private fun getVisibleAreaInContentCoords(): Rect {
        visibleAreaRect.set(
            toContentX(-paddingLeft.toFloat()).toInt(),
            toContentY(-paddingTop.toFloat()).toInt(),
            toContentX(viewportWidth.toFloat() + paddingRight).toInt(),
            toContentY(viewportHeight.toFloat() + paddingBottom).toInt(),
        )
        return visibleAreaRect
    }

    /** The height of the viewport, minus padding */
    private val viewportHeight: Int
        get() = bottom - top - paddingBottom - paddingTop

    /** The width of the viewport, minus padding */
    private val viewportWidth: Int
        get() = right - left - paddingRight - paddingLeft

    /** Converts an X coordinate in View space to an X coordinate in content space */
    private fun toContentX(viewX: Float): Float {
        return toContentCoord(viewX, zoom, scrollX)
    }

    /** Converts a Y coordinate in View space to a Y coordinate in content space */
    private fun toContentY(viewY: Float): Float {
        return toContentCoord(viewY, zoom, scrollY)
    }

    private fun toViewCoord(contentCoord: Float, zoom: Float, scroll: Int): Float {
        return (contentCoord * zoom) - scroll
    }

    /**
     * Converts a one-dimensional coordinate in View space to a one-dimensional coordinate in
     * content space
     */
    private fun toContentCoord(viewCoord: Float, zoom: Float, scroll: Int): Float {
        return (viewCoord + scroll) / zoom
    }

    /** Helper to use [PdfDocument] behind a mutex to ensure FIFO semantics for requests */
    private suspend fun <T> withPdfDocument(block: suspend (PdfDocument) -> T): T {
        pdfDocumentMutex.withLock {
            val localPdfDocument = pdfDocument ?: throw IllegalStateException("No PdfDocument")
            return block(localPdfDocument)
        }
    }

    public companion object {
        public const val DEFAULT_PAGE_SPACING_PX: Int = 20
        public const val DEFAULT_INIT_ZOOM: Float = 1.0f
        public const val DEFAULT_MAX_ZOOM: Float = 25.0f
        public const val DEFAULT_MIN_ZOOM: Float = 0.1f

        private fun checkMainThread() {
            check(Looper.myLooper() == Looper.getMainLooper()) {
                "Property must be set on the main thread"
            }
        }
    }
}
