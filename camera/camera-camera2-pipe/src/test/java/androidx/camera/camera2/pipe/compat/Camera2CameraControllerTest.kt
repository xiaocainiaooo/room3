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

package androidx.camera.camera2.pipe.compat

import android.graphics.SurfaceTexture
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.internal.CameraStatusMonitor
import androidx.camera.camera2.pipe.testing.FakeCamera2DeviceManager
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class Camera2CameraControllerTest {
    private val cameraId = CameraId.fromCamera2Id("0")
    private val testScope = TestScope()
    private val fakeThreads = FakeThreads.fromTestScope(testScope)
    private val streamConfig1 =
        CameraStream.Config.create(size = Size(1280, 720), format = StreamFormat.YUV_420_888)
    private val streamId1 = StreamId(1)
    private val fakeGraphConfig = CameraGraph.Config(cameraId, listOf(streamConfig1))
    private val fakeGraphListener: GraphListener = mock()

    // TODO: b/372258646 - Implement a proper fake implementation to simulate status changes.
    private val fakeCameraStatusMonitor: CameraStatusMonitor =
        object : CameraStatusMonitor {
            private val _cameraAvailability =
                MutableStateFlow(CameraStatusMonitor.CameraStatus.CameraAvailable(cameraId))
            override val cameraAvailability: StateFlow<CameraStatusMonitor.CameraStatus>
                get() = _cameraAvailability.asStateFlow()

            override val cameraPriorities: SharedFlow<Unit>
                get() = MutableSharedFlow()

            override fun close() {}
        }

    private val fakeCaptureSessionFactory: CaptureSessionFactory = mock()
    private val fakeCaptureSequenceProcessorFactory: Camera2CaptureSequenceProcessorFactory = mock()
    private val fakeCamera2DeviceManager = FakeCamera2DeviceManager()
    private val fakeCameraSurfaceManager = CameraSurfaceManager()
    private val fakeTimeSource: TimeSource = mock()
    private val fakeGraphId = CameraGraphId.nextId()

    private val fakeSurfaceTexture = SurfaceTexture(0).apply { setDefaultBufferSize(1280, 720) }
    private val fakeSurface = Surface(fakeSurfaceTexture)

    private fun createCamera2CameraController(): Camera2CameraController {
        return Camera2CameraController(
            testScope,
            fakeThreads,
            fakeGraphConfig,
            fakeGraphListener,
            fakeCameraStatusMonitor,
            fakeCaptureSessionFactory,
            fakeCaptureSequenceProcessorFactory,
            fakeCamera2DeviceManager,
            fakeCameraSurfaceManager,
            fakeTimeSource,
            fakeGraphId,
        )
    }

    @After
    fun tearDown() {
        fakeSurface.release()
        fakeSurfaceTexture.release()
    }

    @Test
    fun testCanCreateCamera2CameraController() =
        testScope.runTest {
            val cameraController = createCamera2CameraController()
            testScope.advanceUntilIdle()
            cameraController.close()
        }

    @Test
    fun testControllerStartCreatesCaptureSession() =
        testScope.runTest {
            val cameraController = createCamera2CameraController()
            cameraController.updateSurfaceMap(mapOf(streamId1 to fakeSurface))
            cameraController.start()
            fakeCamera2DeviceManager.simulateCameraOpen(cameraId)
            testScope.advanceUntilIdle()
            verify(fakeCaptureSessionFactory, times(1)).create(any(), any(), any())
            cameraController.close()
        }
}
