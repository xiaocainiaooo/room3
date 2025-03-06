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

package androidx.camera.extensions.internal.sessionprocessor

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Pair
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.OutputSurfaceConfiguration
import androidx.camera.core.impl.RequestProcessor
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.TagBundle
import androidx.camera.extensions.CameraExtensionsControl
import androidx.camera.extensions.CameraExtensionsInfo
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.internal.Camera2ExtensionsUtil.convertCameraXModeToCamera2Mode
import androidx.camera.extensions.internal.ExtensionsUtils
import androidx.camera.extensions.internal.VendorExtender

@RequiresApi(31)
public class Camera2ExtensionsSessionProcessor(
    private val availableCaptureRequestKeys: List<CaptureRequest.Key<*>>,
    @ExtensionMode.Mode private val mode: Int,
    private val vendorExtender: VendorExtender
) : SessionProcessor, CameraExtensionsInfo, CameraExtensionsControl {

    private val camera2ExtensionMode = convertCameraXModeToCamera2Mode(mode)

    @AdapterCameraInfo.CameraOperation
    private val supportedCameraOperations: Set<Int> =
        ExtensionsUtils.getSupportedCameraOperations(availableCaptureRequestKeys)

    override fun initSession(
        cameraInfo: CameraInfo,
        outputSurfaceConfig: OutputSurfaceConfiguration
    ): SessionConfig {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#initSession should not be invoked!"
        )
    }

    override fun deInitSession() {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#deInitSession should not be invoked!"
        )
    }

    override fun getSupportedPostviewSize(captureSize: Size): Map<Int, List<Size>> {
        return vendorExtender.getSupportedPostviewResolutions(captureSize)
    }

    override fun getSupportedCameraOperations(): Set<Int> {
        return supportedCameraOperations
    }

    override fun getAvailableCharacteristicsKeyValues():
        List<Pair<CameraCharacteristics.Key<*>, in Any>> {
        return vendorExtender.availableCharacteristicsKeyValues
    }

    override fun getExtensionAvailableStabilizationModes(): IntArray? {
        return super.getExtensionAvailableStabilizationModes()
    }

    override fun getImplementationType(): Pair<Int, Int> {
        return Pair.create(SessionProcessor.TYPE_CAMERA2_EXTENSION, camera2ExtensionMode)
    }

    override fun setParameters(config: Config) {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#setParameters should not be invoked!"
        )
    }

    override fun onCaptureSessionStart(requestProcessor: RequestProcessor) {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#onCaptureSessionStart should not be invoked!"
        )
    }

    override fun onCaptureSessionEnd() {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#onCaptureSessionEnd should not be invoked!"
        )
    }

    override fun startRepeating(
        tagBundle: TagBundle,
        callback: SessionProcessor.CaptureCallback
    ): Int {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#startRepeating should not be invoked!"
        )
    }

    override fun stopRepeating() {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#stopRepeating should not be invoked!"
        )
    }

    override fun startCapture(
        postviewEnabled: Boolean,
        tagBundle: TagBundle,
        callback: SessionProcessor.CaptureCallback
    ): Int {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#startCapture should not be invoked!"
        )
    }

    override fun abortCapture(captureSequenceId: Int) {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#abortCapture should not be invoked!"
        )
    }
}
