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

import android.content.Intent
import android.graphics.Point
import android.graphics.RectF
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.pdf.PdfDocument
import androidx.pdf.content.PdfPageGotoLinkContent
import androidx.pdf.content.PdfPageLinkContent
import androidx.pdf.util.waitForIntent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewNavigationTest {
    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testGotoLinkNavigation() {
        val fakePdfDocument =
            FakePdfDocument(
                pages = List(2) { Point(1000, 1000) },
                pageLinks =
                    listOf(
                        PdfDocument.PdfPageLinks(
                            gotoLinks =
                                listOf(
                                    PdfPageGotoLinkContent(
                                        bounds = listOf(RectF(0f, 0f, 1000f, 1000f)),
                                        destination =
                                            PdfPageGotoLinkContent.Destination(
                                                pageNumber = 1,
                                                xCoordinate = 100f,
                                                yCoordinate = 1400f,
                                                zoom = 1f
                                            )
                                    )
                                ),
                            externalLinks = emptyList()
                        )
                    )
            )
        PdfViewTestActivity.onCreateCallback = { activity ->
            val container = FrameLayout(activity)
            container.addView(
                PdfView(activity).apply {
                    pdfDocument = fakePdfDocument
                    id = PDF_VIEW_ID
                },
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(withId(PDF_VIEW_ID))
                .perform(performSingleTapOnCoords(100f, 100f))
                .check { view, noViewFoundException ->
                    view ?: throw noViewFoundException
                    val pdfView = view as PdfView
                    val firstVisiblePage = pdfView.firstVisiblePage
                    val visiblePagesCount = pdfView.visiblePagesCount
                    val targetPage = 1
                    assertThat(targetPage).isAtLeast(firstVisiblePage)
                    assertThat(targetPage).isAtMost(firstVisiblePage + visiblePagesCount - 1)
                }
            close()
        }
    }

    @Test
    fun testExternalLinkNavigation() = runTest {
        val fakePdfDocument =
            FakePdfDocument(
                pages = List(5) { Point(1000, 1000) },
                pageLinks =
                    listOf(
                        PdfDocument.PdfPageLinks(
                            gotoLinks = emptyList(),
                            externalLinks =
                                listOf(
                                    PdfPageLinkContent(
                                        bounds = listOf(RectF(0f, 0f, 200f, 200f)),
                                        uri = Uri.parse("https://www.example.com")
                                    )
                                )
                        )
                    )
            )

        Intents.init()
        PdfViewTestActivity.onCreateCallback = { activity ->
            val container = FrameLayout(activity)
            container.addView(
                PdfView(activity).apply {
                    pdfDocument = fakePdfDocument
                    id = PDF_VIEW_ID
                },
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(withId(PDF_VIEW_ID)).perform(performSingleTapOnCoords(50f, 50f))
            waitForIntent {
                Intents.intended(hasAction(Intent.ACTION_VIEW))
                Intents.intended(hasData(Uri.parse("https://www.example.com")))
            }
            close()
        }
        Intents.release()
    }
}

/** Arbitrary fixed ID for PdfView */
private const val PDF_VIEW_ID = 123456789
