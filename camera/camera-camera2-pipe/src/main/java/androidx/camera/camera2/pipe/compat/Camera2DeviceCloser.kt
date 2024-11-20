/*
 * Copyright 2023 The Android Open Source Project
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
import android.hardware.camera2.CameraDevice
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threading
import androidx.camera.camera2.pipe.core.Threads
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.atomicfu.atomic

@JvmDefaultWithCompatibility
internal interface Camera2DeviceCloser {
    fun closeCamera(
        cameraDeviceWrapper: CameraDeviceWrapper? = null,
        cameraDevice: CameraDevice? = null,
        closeUnderError: Boolean = false,
        androidCameraState: AndroidCameraState,
        audioRestrictionController: AudioRestrictionController
    )
}

@Singleton
internal class Camera2DeviceCloserImpl
@Inject
constructor(
    val threads: Threads,
    private val camera2Quirks: Camera2Quirks,
) : Camera2DeviceCloser {
    override fun closeCamera(
        cameraDeviceWrapper: CameraDeviceWrapper?,
        cameraDevice: CameraDevice?,
        closeUnderError: Boolean,
        androidCameraState: AndroidCameraState,
        audioRestrictionController: AudioRestrictionController
    ) {
        val unwrappedCameraDevice = cameraDeviceWrapper?.unwrapAs(CameraDevice::class)
        if (unwrappedCameraDevice != null) {
            val cameraId = CameraId.fromCamera2Id(unwrappedCameraDevice.id)
            cameraDevice?.let {
                check(cameraId.value == it.id) {
                    "Unwrapped camera device has camera ID ${cameraId.value}, " +
                        "but the wrapped camera device has camera ID ${it.id}!"
                }
            }

            if (
                camera2Quirks.shouldCreateCaptureSessionBeforeClosing(cameraId) && !closeUnderError
            ) {
                Debug.trace("Camera2DeviceCloserImpl#createCaptureSession") {
                    Log.debug {
                        "Creating an empty capture session before closing camera $cameraId"
                    }
                    createCaptureSession(cameraDeviceWrapper)
                    Log.debug { "Empty capture session quirk completed" }
                }
            }

            closeCameraDevice(unwrappedCameraDevice, androidCameraState)
            cameraDeviceWrapper.onDeviceClosed()

            /**
             * Only remove the audio restriction when CameraDeviceWrapper is present. When
             * closeCamera is called without a CameraDeviceWrapper, that means a wrapper hadn't been
             * created for the opened camera.
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                audioRestrictionController.removeListener(cameraDeviceWrapper)
            }

            // We only need to close the device once (don't want to create another capture session).
            // Return here.
            return
        }
        cameraDevice?.let { closeCameraDevice(it, androidCameraState) }
    }

    private fun closeCameraDevice(
        cameraDevice: CameraDevice,
        androidCameraState: AndroidCameraState,
    ) {
        Log.debug { "$this#closeCameraDevice($cameraDevice)" }
        Threading.runBlockingCheckedOrNull(threads.backgroundDispatcher, CAMERA_CLOSE_TIMEOUT_MS) {
            cameraDevice.closeWithTrace()
        }
            ?: run {
                Log.error {
                    "Camera device close timed out after ${CAMERA_CLOSE_TIMEOUT_MS}ms. " +
                        "The camera is likely in a bad state."
                }
            }
        val cameraId = CameraId.fromCamera2Id(cameraDevice.id)
        if (camera2Quirks.shouldWaitForCameraDeviceOnClosed(cameraId)) {
            Log.debug { "Waiting for OnClosed from $cameraId" }
            if (androidCameraState.awaitCameraDeviceClosed(timeoutMillis = 5000)) {
                Log.debug { "Received OnClosed for $cameraId" }
            } else {
                Log.warn { "Failed to close $cameraId after 500ms" }
            }
        }
    }

    private fun createCaptureSession(cameraDeviceWrapper: CameraDeviceWrapper) {
        val surfaceTexture = SurfaceTexture(0).also { it.setDefaultBufferSize(640, 480) }
        val surface = Surface(surfaceTexture)
        val surfaceReleased = atomic(false)
        val sessionConfigured = CountDownLatch(1)
        val callback =
            object : CameraCaptureSessionWrapper.StateCallback {
                override fun onConfigured(session: CameraCaptureSessionWrapper) {
                    Log.debug { "Empty capture session configured. Closing it" }
                    // We don't need to wait for the session to close, instead we can just invoke
                    // close() and end here.
                    session.close()
                    sessionConfigured.countDown()
                }

                override fun onClosed(session: CameraCaptureSessionWrapper) {
                    Log.debug { "Empty capture session closed" }
                    if (surfaceReleased.compareAndSet(expect = false, update = true)) {
                        surface.release()
                        surfaceTexture.release()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSessionWrapper) {
                    Log.debug { "Empty capture session configure failed" }
                    if (surfaceReleased.compareAndSet(expect = false, update = true)) {
                        surface.release()
                        surfaceTexture.release()
                    }
                    sessionConfigured.countDown()
                }

                override fun onReady(session: CameraCaptureSessionWrapper) {}

                override fun onCaptureQueueEmpty(session: CameraCaptureSessionWrapper) {}

                override fun onSessionFinalized() {}

                override fun onActive(session: CameraCaptureSessionWrapper) {}
            }
        if (!cameraDeviceWrapper.createCaptureSession(listOf(surface), callback)) {
            Log.error {
                "Failed to create a blank capture session! " +
                    "Surfaces may not be disconnected properly."
            }
            if (surfaceReleased.compareAndSet(expect = false, update = true)) {
                surface.release()
                surfaceTexture.release()
            }
        }
        sessionConfigured.await()
    }

    companion object {
        const val CAMERA_CLOSE_TIMEOUT_MS = 8_000L // 8s
    }
}
