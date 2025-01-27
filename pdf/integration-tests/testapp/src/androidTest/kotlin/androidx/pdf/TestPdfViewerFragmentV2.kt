/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf

import androidx.pdf.idlingresource.PdfIdlingResource
import androidx.pdf.viewer.fragment.PdfViewerFragmentV2

/**
 * A subclass fragment from [PdfViewerFragmentV2] to include [androidx.test.espresso.IdlingResource]
 * while loading pdf document.
 *
 * TODO(b/386721657) Remove this when PdfViewerFragment is replaced with PdfViewerFragmentV2.
 */
internal class TestPdfViewerFragmentV2 : PdfViewerFragmentV2() {

    val pdfLoadingIdlingResource = PdfIdlingResource(PDF_LOAD_RESOURCE_NAME)

    override fun onLoadDocumentSuccess() {
        pdfLoadingIdlingResource.decrement()
    }

    override fun onLoadDocumentError(error: Throwable) {
        pdfLoadingIdlingResource.decrement()
    }

    companion object {
        private const val PDF_LOAD_RESOURCE_NAME = "PdfLoad"
    }
}
