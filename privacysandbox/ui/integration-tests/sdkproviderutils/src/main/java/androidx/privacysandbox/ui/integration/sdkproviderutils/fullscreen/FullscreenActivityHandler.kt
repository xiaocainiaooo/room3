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

package androidx.privacysandbox.ui.integration.sdkproviderutils.fullscreen

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder

class FullscreenActivityHandler(
    private val activityHolder: ActivityHolder,
    private val adView: View,
) {
    private val activity = activityHolder.getActivity()
    private lateinit var destroyActivityButton: Button
    private lateinit var openLandingPage: Button

    fun buildLayout() {
        buildLayoutProgrammatically()
        registerDestroyActivityButton()
        registerOpenLandingPageButton()
        registerLifecycleListener()
    }

    /** Builds the activity layout programmatically. */
    private fun buildLayoutProgrammatically(): ViewGroup {
        val mainLayout = LinearLayout(activity)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.layoutParams =
            ViewGroup.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

        destroyActivityButton = Button(activity).apply { text = DESTROY_ACTIVITY }
        mainLayout.addView(destroyActivityButton)

        openLandingPage = Button(activity).apply { text = OPEN_LANDING_PAGE }
        mainLayout.addView(openLandingPage)

        if (adView.parent != null) {
            (adView.parent as ViewGroup).removeView(adView)
        }
        adView.layoutParams =
            ViewGroup.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        mainLayout.addView(adView)

        activity.setContentView(mainLayout)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        return mainLayout
    }

    private fun registerDestroyActivityButton() {
        destroyActivityButton.setOnClickListener { activity.finish() }
    }

    private fun registerOpenLandingPageButton() {
        openLandingPage.setOnClickListener {
            val visitUrl = Intent(Intent.ACTION_VIEW)
            visitUrl.setData(Uri.parse(LANDING_PAGE_URL))
            visitUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(visitUrl)
        }
    }

    private fun registerLifecycleListener() {
        activityHolder.lifecycle.addObserver(LocalLifecycleObserver())
    }

    private fun makeToast(message: String) {
        activity.runOnUiThread { Toast.makeText(activity, message, Toast.LENGTH_SHORT).show() }
    }

    private inner class LocalLifecycleObserver : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            makeToast("Current activity state is: $event")
        }
    }

    private companion object {
        private const val DESTROY_ACTIVITY = "Destroy Activity"
        private const val OPEN_LANDING_PAGE = "Open Landing Page"
        private const val LANDING_PAGE_URL = "https://www.google.com"
    }
}
