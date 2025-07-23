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

package androidx.camera.integration.macrobenchmark.target

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ExperimentalLensFacing
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

class SessionConfigBindActivity : ComponentActivity() {
    private val preview = Preview.Builder().build()
    private val imageCapture = ImageCapture.Builder().build()
    private val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

    @androidx.annotation.OptIn(ExperimentalLensFacing::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cameraXConfig =
            when (intent.extras?.getString("camerax_config")) {
                Camera2Config::class.simpleName -> Camera2Config.defaultConfig()
                CameraPipeConfig::class.simpleName -> CameraPipeConfig.defaultConfig()
                else -> Camera2Config.defaultConfig()
            }

        val cameraSelector =
            when (intent.extras?.getInt("lens")) {
                CameraSelector.LENS_FACING_BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                CameraSelector.LENS_FACING_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                CameraSelector.LENS_FACING_EXTERNAL ->
                    CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_EXTERNAL)
                        .build()
                else -> CameraSelector.DEFAULT_BACK_CAMERA
            }

        setContent { CameraScreen(cameraXConfig, cameraSelector) }
    }

    @Composable
    fun CameraScreen(cameraXConfig: CameraXConfig, cameraSelector: CameraSelector) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val lifecycleOwner = LocalLifecycleOwner.current

        LifecycleStartEffect(Unit) {
            coroutineScope.launch {
                initCameraX(
                    cameraXConfig,
                    cameraSelector,
                    context.applicationContext,
                    lifecycleOwner,
                )
            }
            onStopOrDispose {}
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PreviewView(context).apply {
                    preview.surfaceProvider = this.surfaceProvider
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                }
            },
        )
    }

    @androidx.annotation.OptIn(ExperimentalCameraProviderConfiguration::class)
    @OptIn(ExperimentalSessionConfig::class)
    suspend fun initCameraX(
        cameraXConfig: CameraXConfig,
        cameraSelector: CameraSelector,
        context: Context,
        lifecycleOwner: LifecycleOwner,
    ) {
        ProcessCameraProvider.configureInstance(cameraXConfig)
        ProcessCameraProvider.awaitInstance(context)
            .bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                SessionConfig(listOf(preview, imageCapture, videoCapture)),
            )
    }
}
