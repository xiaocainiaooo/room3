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

package androidx.xr.arcore

import android.app.Activity
import android.content.ContentResolver
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.testing.FakeRuntimeFace
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class FaceTest {
    private lateinit var xrResourcesManager: XrResourcesManager
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session

    @get:Rule
    val grantPermissionRule = GrantPermissionRule.grant("android.permission.FACE_TRACKING")

    private lateinit var mockContentResolver: ContentResolver

    @Before
    fun setUp() {
        xrResourcesManager = XrResourcesManager()
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        mockContentResolver = mock<ContentResolver>()
    }

    @After
    fun tearDown() {
        xrResourcesManager.clear()
    }

    @Test
    fun update_stateMachesRuntimeFace() = runBlocking {
        val runtimeFace = FakeRuntimeFace()
        val underTest = Face(runtimeFace)
        check(underTest.state.value.trackingState != TrackingState.TRACKING)
        check(underTest.state.value.blendShapeValues.isEmpty())
        check(underTest.state.value.confidenceValues.isEmpty())
        runtimeFace.trackingState = TrackingState.TRACKING
        val expectedBlendShapeValues = floatArrayOf(0.1f, 0.2f, 0.3f)
        val expectedConfidenceValues = floatArrayOf(0.4f, 0.5f, 0.6f)
        runtimeFace.blendShapeValues = expectedBlendShapeValues
        runtimeFace.confidenceValues = expectedConfidenceValues

        underTest.update()

        assertThat(underTest.state.value.trackingState).isEqualTo(TrackingState.TRACKING)
        assertThat(underTest.state.value.blendShapeValues).isEqualTo(expectedBlendShapeValues)
        assertThat(underTest.state.value.confidenceValues).isEqualTo(expectedConfidenceValues)
        assertThat(underTest.state.value.blendShapes.keys.size)
            .isEqualTo(expectedBlendShapeValues.size)
        assertThat(underTest.state.value.blendShapes.values.size)
            .isEqualTo(expectedBlendShapeValues.size)
    }

    private fun createTestSessionAndRunTest(
        coroutineDispatcher: CoroutineDispatcher = StandardTestDispatcher(),
        testBody: () -> Unit,
    ) {
        ActivityScenario.launch(Activity::class.java).use {
            it.onActivity { activity ->
                session =
                    (Session.create(activity, coroutineDispatcher) as SessionCreateSuccess).session
                xrResourcesManager.lifecycleManager = session.runtime.lifecycleManager

                testBody()
            }
        }
    }
}
