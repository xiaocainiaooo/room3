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

package androidx.privacysandbox.ui.client

import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.animation.AnimationUtils
import androidx.privacysandbox.ui.core.IRemoteSessionController

/**
 * Custom implementation of the [android.view.SurfaceView] that holds the
 * [android.view.SurfaceControlViewHost.SurfacePackage] passed from the provider. It transfers the
 * [android.view.MotionEvent] objects received in [android.view.View.onTouchEvent] to the provider.
 */
internal class ContentView(
    context: Context,
    val remoteSessionController: IRemoteSessionController
) : SurfaceView(context) {

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        val eventTargetFrameTime = AnimationUtils.currentAnimationTimeMillis()
        remoteSessionController.notifyMotionEvent(motionEvent, eventTargetFrameTime)
        return super.onTouchEvent(motionEvent)
    }
}
