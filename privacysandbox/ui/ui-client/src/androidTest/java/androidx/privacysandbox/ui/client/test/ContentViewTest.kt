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

package androidx.privacysandbox.ui.client.test

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import androidx.privacysandbox.ui.client.ContentView
import androidx.privacysandbox.ui.core.IRemoteSessionController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ContentViewTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun transferEventsWithTargetTimeOnDispatchTouchEventTest() {
        var stubCalledAt = 0L
        var stubMotionEvent: MotionEvent? = null

        val remoteController =
            object : IRemoteSessionController.Stub() {
                override fun close() {}

                override fun notifyConfigurationChanged(configuration: Configuration?) {}

                override fun notifyResized(width: Int, height: Int) {}

                override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {}

                override fun notifyFetchUiForSession() {}

                override fun notifyUiChanged(uiContainerInfo: Bundle?) {}

                override fun notifyMotionEvent(
                    motionEvent: MotionEvent,
                    eventTargetFrameTime: Long
                ) {
                    stubMotionEvent = motionEvent
                    stubCalledAt = eventTargetFrameTime
                }
            }
        val contentView = ContentView(context, remoteController)

        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN)
        val timeNow = AnimationUtils.currentAnimationTimeMillis()
        contentView.onTouchEvent(downEvent)

        assertThat(stubCalledAt).isAtLeast(timeNow)
        assertThat(stubMotionEvent).isEqualTo(downEvent)
    }

    private fun createMotionEvent(motionEventAction: Int): MotionEvent {
        return MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            motionEventAction,
            0f,
            0f,
            /* metaState = */ 0
        )
    }
}
