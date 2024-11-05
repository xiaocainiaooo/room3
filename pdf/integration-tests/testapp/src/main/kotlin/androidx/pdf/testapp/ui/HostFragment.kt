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

package androidx.pdf.testapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.OperationCanceledException
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.pdf.testapp.R
import androidx.pdf.viewer.fragment.PdfViewerFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * This fragment extends PdfViewerFragment to provide a custom layout and handle immersive mode. It
 * adds a FloatingActionButton for search functionality and manages its visibility based on the
 * immersive mode state.
 */
class HostFragment : PdfViewerFragment() {
    private var hostView: FrameLayout? = null
    private var search: FloatingActionButton? = null
    private var fullScreen: FloatingActionButton? = null
    private var isImmersiveModeEnabled = false

    private var openPdfButton: MaterialButton? = null
    private var searchButton: MaterialButton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState) as FrameLayout

        // Inflate the custom layout for this fragment.
        hostView = inflater.inflate(R.layout.fragment_host, container, false) as FrameLayout
        search = hostView?.findViewById(R.id.host_Search)
        fullScreen = hostView?.findViewById(R.id.toggle_full_screen)

        // Add the default PDF viewer to the custom layout
        hostView?.addView(view)

        // Show/hide the search button based on initial toolbox visibility
        if (isToolboxVisible) search?.show() else search?.hide()

        // Set up search button click listener
        search?.setOnClickListener { isTextSearchActive = true }
        fullScreen?.setOnClickListener { toggleImmersiveMode() }
        return hostView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        openPdfButton = activity?.findViewById(R.id.open_pdf)
        searchButton = activity?.findViewById(R.id.search_button)
    }

    private fun toggleImmersiveMode() {
        isImmersiveModeEnabled = !isImmersiveModeEnabled
        updateSystemUi(!isImmersiveModeEnabled)
        updateBottomButtonsVisibility(!isImmersiveModeEnabled)
        onRequestImmersiveMode(isImmersiveModeEnabled)
    }

    private fun updateSystemUi(showSystemUi: Boolean) {
        val insetsController =
            WindowCompat.getInsetsController(requireActivity().window, hostView!!)
        if (showSystemUi) {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun updateBottomButtonsVisibility(visibility: Boolean) {
        if (visibility) {
            openPdfButton?.visibility = View.VISIBLE
            searchButton?.visibility = View.VISIBLE
        } else {
            openPdfButton?.visibility = View.GONE
            searchButton?.visibility = View.GONE
        }
    }

    override fun onLoadDocumentError(error: Throwable) {
        super.onLoadDocumentError(error)
        when (error) {
            is OperationCanceledException ->
                (activity as? OpCancellationHandler)?.handleCancelOperation()
        }
    }
}
