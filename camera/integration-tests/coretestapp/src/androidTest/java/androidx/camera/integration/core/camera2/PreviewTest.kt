/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.camera.integration.core.camera2

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.DisplayInfoManager
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.ImageOutputConfig.OPTION_RESOLUTION_SELECTOR
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.Threads.runOnMainSync
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionFilter
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.integration.core.util.CameraInfoUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.concurrent.futures.await
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class PreviewTest(private val implName: String, private val cameraConfig: CameraXConfig) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName == CameraPipeConfig::class.simpleName,
        )

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    companion object {
        private const val ANY_THREAD_NAME = "any-thread-name"
        private val DEFAULT_RESOLUTION: Size by lazy { Size(640, 480) }
        private const val FRAMES_TO_VERIFY = 10
        private const val RESULT_TIMEOUT = 5000L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
            )
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var cameraProvider: ProcessCameraProvider
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var defaultBuilder: Preview.Builder? = null
    private var previewResolution: Size? = null
    private var frameSemaphore: Semaphore? = null
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val lifecycleOwner = FakeLifecycleOwner()

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setUp() = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context).await()

        // init CameraX before creating Preview to get preview size with CameraX's context
        defaultBuilder =
            Preview.Builder.fromConfig(Preview.DEFAULT_CONFIG.config).also {
                it.mutableConfig.removeOption(ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)
            }
        frameSemaphore = Semaphore(/* permits= */ 0)
        lifecycleOwner.startAndResume()
    }

    @After
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun tearDown() {
        cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
    }

    // ======================================================
    //   Section 1: SurfaceProvider behavior testing
    // ======================================================
    @Test
    fun surfaceProvider_isUsedAfterSetting() = runBlocking {
        val preview = defaultBuilder!!.build()
        val completableDeferred = CompletableDeferred<Unit>()

        instrumentation.runOnMainSync {
            preview.setSurfaceProvider { request ->
                val surfaceTexture = SurfaceTexture(0)
                surfaceTexture.setDefaultBufferSize(
                    request.resolution.width,
                    request.resolution.height
                )
                surfaceTexture.detachFromGLContext()
                val surface = Surface(surfaceTexture)
                request.provideSurface(surface, CameraXExecutors.directExecutor()) {
                    surface.release()
                    surfaceTexture.release()
                }
                completableDeferred.complete(Unit)
            }

            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }
        withTimeout(3_000) { completableDeferred.await() }
    }

    @Test
    @Throws(InterruptedException::class)
    fun previewUnbound_RESULT_SURFACE_USED_SUCCESSFULLY_isCalled() = runBlocking {
        // Arrange.
        val preview = Preview.Builder().build()
        val resultDeferred = CompletableDeferred<Int>()
        // Act.
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                getSurfaceProvider(
                    frameAvailableListener = { frameSemaphore!!.release() },
                    resultListener = { result -> resultDeferred.complete(result.resultCode) }
                )
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Wait until preview gets frame.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 5)

        // Ensure resultListener is not invoked before unbind.
        assertThat(withTimeoutOrNull(500) { resultDeferred.await() }).isNull()

        // Remove the UseCase from the camera
        instrumentation.runOnMainSync { cameraProvider.unbind(preview) }

        // Assert.
        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setSurfaceProviderBeforeBind_getsFrame() {
        // Arrange.
        val preview = defaultBuilder!!.build()
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            )
            // Act.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Assert.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setSurfaceProviderBeforeBind_providesSurfaceOnWorkerExecutorThread() {
        val threadName = AtomicReference<String>()

        // Arrange.
        val preview = defaultBuilder!!.build()
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                workExecutorWithNamedThread,
                getSurfaceProvider(
                    threadNameConsumer = { newValue: String -> threadName.set(newValue) },
                    frameAvailableListener = { frameSemaphore!!.release() }
                )
            )

            // Act.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Assert.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
        assertThat(threadName.get()).isEqualTo(ANY_THREAD_NAME)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setSurfaceProviderAfterBind_getsFrame() {
        // Arrange.
        val preview = defaultBuilder!!.build()

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)

            // Act.
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            )
        }

        // Assert.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setSurfaceProviderAfterBind_providesSurfaceOnWorkerExecutorThread() {
        val threadName = AtomicReference<String>()

        // Arrange.
        val preview = defaultBuilder!!.build()

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            // Act.
            preview.setSurfaceProvider(
                workExecutorWithNamedThread,
                getSurfaceProvider(
                    threadNameConsumer = { newValue: String -> threadName.set(newValue) },
                    frameAvailableListener = { frameSemaphore!!.release() }
                )
            )
        }

        // Assert.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
        assertThat(threadName.get()).isEqualTo(ANY_THREAD_NAME)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setMultipleNonNullSurfaceProviders_getsFrame() {
        val preview = defaultBuilder!!.build()

        instrumentation.runOnMainSync {
            // Set a different SurfaceProvider which will provide a different surface to be used
            // for preview.
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)

        // Use another sSemaphore to monitor the frames from the 2nd surfaceProvider.
        val frameSemaphore2 = Semaphore(/* permits= */ 0)

        instrumentation.runOnMainSync {
            // Set a different SurfaceProvider which will provide a different surface to be used
            // for preview.
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore2.release() })
            )
        }
        frameSemaphore2.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setMultipleNullableSurfaceProviders_getsFrame() {
        val preview = defaultBuilder!!.build()

        instrumentation.runOnMainSync {
            // Set a different SurfaceProvider which will provide a different surface to be used
            // for preview.
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)

        // Recreate the semaphore to monitor the frame available callback.
        val frameSemaphore2 = Semaphore(/* permits= */ 0)

        instrumentation.runOnMainSync {
            // Set the SurfaceProvider to null in order to force the Preview into an inactive
            // state before setting a different SurfaceProvider for preview.
            preview.setSurfaceProvider(null)
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore2.release() })
            )
        }
        frameSemaphore2.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    fun willNotProvideSurface_resultCode_WILL_NOT_PROVIDE_SURFACE(): Unit = runBlocking {
        val preview = defaultBuilder!!.build()

        val resultDeferred = CompletableDeferred<Int>()
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider { surfaceRequest ->
                surfaceRequest.willNotProvideSurface()
                val surface = Surface(SurfaceTexture(0))
                // can't provideSurface successfully after willNotProvideSurface.
                // RESULT_WILL_NOT_PROVIDE_SURFACE will be notified.
                surfaceRequest.provideSurface(surface, CameraXExecutors.directExecutor()) { result
                    ->
                    resultDeferred.complete(result.resultCode)
                }
            }
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_WILL_NOT_PROVIDE_SURFACE)
    }

    @Test
    fun provideSurfaceTwice_resultCode_SURFACE_ALREADY_PROVIDED(): Unit = runBlocking {
        val preview = defaultBuilder!!.build()

        val resultDeferred1 = CompletableDeferred<Int>()
        val resultDeferred2 = CompletableDeferred<Int>()
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider { surfaceRequest ->
                runBlocking {
                    val surfaceTextureHolder =
                        SurfaceTextureProvider.createAutoDrainingSurfaceTextureAsync(
                                surfaceRequest.resolution.width,
                                surfaceRequest.resolution.height,
                                { frameSemaphore!!.release() }
                            )
                            .await()
                    val surface = Surface(surfaceTextureHolder!!.surfaceTexture)
                    surfaceRequest.provideSurface(
                        surface,
                        CameraXExecutors.directExecutor(),
                        { result ->
                            surface.release()
                            resultDeferred1.complete(result.resultCode)
                        }
                    )

                    // Invoking provideSurface twice is a no-op and the result will be
                    // RESULT_SURFACE_ALREADY_PROVIDED
                    surfaceRequest.provideSurface(
                        Surface(SurfaceTexture(1)),
                        CameraXExecutors.directExecutor()
                    ) { result ->
                        resultDeferred2.complete(result.resultCode)
                    }
                }
            }
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Wait until preview gets frame.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 5)

        instrumentation.runOnMainSync { cameraProvider.unbind(preview) }

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred2.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_ALREADY_PROVIDED)

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred1.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)
    }

    @Test
    fun surfaceRequestCancelled_resultCode_REQUEST_CANCELLED() = runBlocking {
        val preview = defaultBuilder!!.build()

        val resultDeferred = CompletableDeferred<Int>()
        val surfaceRequestDeferred = CompletableDeferred<SurfaceRequest>()
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider { surfaceRequest ->
                surfaceRequestDeferred.complete(surfaceRequest)
            }
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            // unbind the use case to cancel the surface request.
            cameraProvider.unbind(preview)
        }

        surfaceRequestDeferred.await().provideSurface(
            Surface(SurfaceTexture(0)),
            CameraXExecutors.directExecutor()
        ) { result ->
            resultDeferred.complete(result.resultCode)
        }

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_REQUEST_CANCELLED)
    }

    @Test
    fun newSurfaceProviderAfterSurfaceProvided_resultCode_SURFACE_USED_SUCCESSFULLY() =
        runBlocking {
            val preview = defaultBuilder!!.build()

            val resultDeferred = CompletableDeferred<Int>()
            instrumentation.runOnMainSync {
                preview.setSurfaceProvider(CameraXExecutors.mainThreadExecutor()) { surfaceRequest
                    ->
                    val surface = Surface(SurfaceTexture(0))
                    surfaceRequest.provideSurface(surface, CameraXExecutors.directExecutor()) {
                        result ->
                        resultDeferred.complete(result.resultCode)
                    }

                    // After the surface is provided, if there is a new request (here we trigger by
                    // setting another surfaceProvider), the previous surfaceRequest will receive
                    // RESULT_SURFACE_USED_SUCCESSFULLY.
                    preview.setSurfaceProvider(
                        getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
                    )
                }
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }

            assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
                .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)

            // Wait until preview gets frame.
            frameSemaphore!!.verifyFramesReceived(
                frameCount = FRAMES_TO_VERIFY,
                timeoutInSeconds = 5
            )
        }

    @Test
    fun newSurfaceRequestAfterSurfaceProvided_resultCode_SURFACE_USED_SUCCESSFULLY() = runBlocking {
        val preview = defaultBuilder!!.build()

        val resultDeferred = CompletableDeferred<Int>()
        var surfaceRequestCount = 0
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(CameraXExecutors.mainThreadExecutor()) { surfaceRequest ->
                // the surface will be requested twice on the same CameraProvider instance.
                if (surfaceRequestCount == 0) {
                    val surface = Surface(SurfaceTexture(0))
                    surfaceRequest.provideSurface(surface, CameraXExecutors.directExecutor()) {
                        result ->
                        resultDeferred.complete(result.resultCode)
                    }

                    // After the surface is provided, if there is a new request (here we trigger by
                    // unbind and rebind), the previous surfaceRequest will receive
                    // RESULT_SURFACE_USED_SUCCESSFULLY.
                    cameraProvider.unbind(preview)
                    surfaceRequestCount++
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } else {
                    runBlocking {
                        val surfaceTextureHolder =
                            SurfaceTextureProvider.createAutoDrainingSurfaceTextureAsync(
                                    surfaceRequest.resolution.width,
                                    surfaceRequest.resolution.height,
                                    { frameSemaphore!!.release() }
                                )
                                .await()
                        val surface = Surface(surfaceTextureHolder.surfaceTexture)
                        surfaceRequest.provideSurface(surface, CameraXExecutors.directExecutor()) {
                            surfaceTextureHolder.close()
                            surface.release()
                        }
                    }
                }
            }
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)

        // Wait until preview gets frame.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 5)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setNullSurfaceProvider_shouldStopPreview() {
        // Arrange.
        val preview = Preview.Builder().build()

        // Act.
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                    frameSemaphore!!.release()
                }
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Assert.
        // Wait until preview gets frame.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)

        // Act.
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(CameraXExecutors.mainThreadExecutor(), null)
        }

        // Assert.
        // No frame coming for 3 seconds in 10 seconds timeout.
        assertThat(noFrameCome(3000L, 10000L)).isTrue()
    }

    // ======================================================
    //   Resolutions / Aspect Ratio
    // ======================================================

    @Test
    fun defaultAspectRatioWillBeSet_whenTargetResolutionIsNotSet() =
        runBlocking(Dispatchers.Main) {
            val useCase = Preview.Builder().build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)
            val config = useCase.currentConfig as ImageOutputConfig
            assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
        }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun defaultAspectRatioWillBeSet_whenRatioDefaultIsSet() =
        runBlocking(Dispatchers.Main) {
            val useCase = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_DEFAULT).build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)
            val config = useCase.currentConfig as ImageOutputConfig
            assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
        }

    @Suppress("DEPRECATION") // legacy resolution API
    @Test
    fun defaultAspectRatioWontBeSet_whenTargetResolutionIsSet() =
        runBlocking(Dispatchers.Main) {
            assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
            val useCase = Preview.Builder().setTargetResolution(DEFAULT_RESOLUTION).build()
            assertThat(
                    useCase.currentConfig.containsOption(
                        ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO
                    )
                )
                .isFalse()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCase
            )

            assertThat(
                    useCase.currentConfig.containsOption(
                        ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO
                    )
                )
                .isFalse()
        }

    @Test
    fun useCaseConfigCanBeReset_afterUnbind() =
        runBlocking(Dispatchers.Main) {
            val preview = defaultBuilder!!.build()
            val initialConfig = preview.currentConfig
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            cameraProvider.unbind(preview)
            val configAfterUnbinding = preview.currentConfig
            assertThat(initialConfig == configAfterUnbinding).isTrue()
        }

    @Test
    fun targetRotationIsRetained_whenUseCaseIsReused() =
        runBlocking(Dispatchers.Main) {
            val useCase = defaultBuilder!!.build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)

            // Generally, the device can't be rotated to Surface.ROTATION_180. Therefore,
            // use it to do the test.
            useCase.targetRotation = Surface.ROTATION_180
            cameraProvider.unbind(useCase)

            // Check the target rotation is kept when the use case is unbound.
            assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)

            // Check the target rotation is kept when the use case is rebound to the
            // lifecycle.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)
            assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)
        }

    @Test
    fun targetRotationReturnsDisplayRotationIfNotSet() =
        runBlocking(Dispatchers.Main) {
            val displayRotation =
                DisplayInfoManager.getInstance(context).getMaxSizeDisplay(true).rotation
            val useCase = defaultBuilder!!.build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)

            assertThat(useCase.targetRotation).isEqualTo(displayRotation)
        }

    @Test
    @Throws(InterruptedException::class)
    fun useCaseCanBeReusedInSameCamera() = runBlocking {
        val preview = defaultBuilder!!.build()
        var resultDeferred = CompletableDeferred<Int>()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                getSurfaceProvider(
                    frameAvailableListener = { frameSemaphore!!.release() },
                    resultListener = { result -> resultDeferred.complete(result.resultCode) }
                )
            )
            // This is the first time the use case bound to the lifecycle.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Check the frame available callback is called.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
        withContext(Dispatchers.Main) { cameraProvider.unbind(preview) }

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)

        // Recreate the semaphore / deferred to monitor the frame available callback.
        frameSemaphore = Semaphore(/* permits= */ 0)
        // Recreate the resultDeferred to monitor the result listener again.
        resultDeferred = CompletableDeferred()

        withContext(Dispatchers.Main) {
            // Rebind the use case to the same camera.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Check the frame available callback can be called after reusing the use case.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
        withContext(Dispatchers.Main) { cameraProvider.unbind(preview) }
        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)
    }

    @Test
    @Throws(InterruptedException::class)
    fun useCaseCanBeReusedInDifferentCamera() = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))

        val preview = defaultBuilder!!.build()
        var resultDeferred = CompletableDeferred<Int>()
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                getSurfaceProvider(
                    frameAvailableListener = { frameSemaphore!!.release() },
                    resultListener = { result -> resultDeferred.complete(result.resultCode) }
                )
            )
            // This is the first time the use case bound to the lifecycle.
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
        }

        // Check the frame available callback is called.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
        // Unbind and rebind the use case to the same lifecycle.
        instrumentation.runOnMainSync { cameraProvider.unbind(preview) }

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)

        // Recreate the semaphore to monitor the frame available callback.
        frameSemaphore = Semaphore(/* permits= */ 0)
        // Recreate the resultDeferred to monitor the result listener again.
        resultDeferred = CompletableDeferred()

        instrumentation.runOnMainSync {
            // Rebind the use case to different camera.
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview
            )
        }

        // Check the frame available callback can be called after reusing the use case.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
        withContext(Dispatchers.Main) { cameraProvider.unbind(preview) }
        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)
    }

    @Test
    fun returnValidTargetRotation_afterUseCaseIsCreated() {
        val imageCapture = ImageCapture.Builder().build()
        assertThat(imageCapture.targetRotation).isNotEqualTo(ImageOutputConfig.INVALID_ROTATION)
    }

    @Test
    fun returnCorrectTargetRotation_afterUseCaseIsBound() =
        runBlocking(Dispatchers.Main) {
            val preview = Preview.Builder().setTargetRotation(Surface.ROTATION_180).build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            assertThat(preview.targetRotation).isEqualTo(Surface.ROTATION_180)
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Test
    fun getsFrame_withHighResolutionEnabled() = runBlocking {
        val camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA)
            }
        val cameraInfo = camera.cameraInfo
        val maxHighResolutionOutputSize =
            CameraInfoUtil.getMaxHighResolutionOutputSize(cameraInfo, ImageFormat.PRIVATE)
        // Only runs the test when the device has high resolution output sizes
        assumeTrue(maxHighResolutionOutputSize != null)

        // Arrange.
        val resolutionSelector =
            ResolutionSelector.Builder()
                .setAllowedResolutionMode(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .setResolutionFilter { _, _ -> listOf(maxHighResolutionOutputSize) }
                .build()
        val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            )
            // Act.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        assertThat(preview.resolutionInfo!!.resolution).isEqualTo(maxHighResolutionOutputSize)

        // Assert.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    fun defaultMaxResolutionCanBeKept_whenResolutionStrategyIsNotSet() =
        runBlocking(Dispatchers.Main) {
            assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
            val useCase = Preview.Builder().build()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCase
            )

            assertThat(
                    useCase.currentConfig.containsOption(ImageOutputConfig.OPTION_MAX_RESOLUTION)
                )
                .isTrue()
        }

    @Test
    fun defaultMaxResolutionCanBeRemoved_whenResolutionStrategyIsSet() =
        runBlocking(Dispatchers.Main) {
            assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
            val useCase =
                Preview.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                            .build()
                    )
                    .build()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCase
            )
            assertThat(
                    useCase.currentConfig.containsOption(ImageOutputConfig.OPTION_MAX_RESOLUTION)
                )
                .isFalse()
        }

    @Test
    fun resolutionSelectorConfigCorrectlyMerged_afterBindToLifecycle() =
        runBlocking(Dispatchers.Main) {
            val resolutionFilter = ResolutionFilter { supportedSizes, _ -> supportedSizes }
            val useCase =
                Preview.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionFilter(resolutionFilter)
                            .setAllowedResolutionMode(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                            .build()
                    )
                    .build()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCase
            )

            val resolutionSelector =
                useCase.currentConfig.retrieveOption(OPTION_RESOLUTION_SELECTOR)
            // The default 4:3 AspectRatioStrategy is kept
            assertThat(resolutionSelector!!.aspectRatioStrategy)
                .isEqualTo(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            // The default highest available ResolutionStrategy is kept
            assertThat(resolutionSelector.resolutionStrategy)
                .isEqualTo(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            // The set resolutionFilter is kept
            assertThat(resolutionSelector.resolutionFilter).isEqualTo(resolutionFilter)
            // The set allowedResolutionMode is kept
            assertThat(resolutionSelector.allowedResolutionMode)
                .isEqualTo(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
        }

    @Test
    fun sessionErrorListenerReceivesError_getsFrame(): Unit = runBlocking {
        // Arrange.
        val preview = defaultBuilder!!.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            // Act.
            preview.surfaceProvider =
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
        }

        // Retrieves the initial session config
        val initialSessionConfig = preview.sessionConfig

        // Checks that image can be received successfully when onError is received.
        triggerOnErrorAndVerifyNewImageReceived(initialSessionConfig)

        // Rebinds to different camera
        if (CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
            withContext(Dispatchers.Main) {
                cameraProvider.unbind(preview)
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview
                )
            }

            // Checks that image can be received successfully when onError is received by the old
            // error listener.
            triggerOnErrorAndVerifyNewImageReceived(initialSessionConfig)
        }

        val sessionConfigBeforeValidErrorNotification = preview.sessionConfig
        // Checks that image can be received successfully when onError is received by the new
        // error listener.
        triggerOnErrorAndVerifyNewImageReceived(preview.sessionConfig)
        // Checks that triggering onError to valid listener has recreated the pipeline
        assertThat(preview.sessionConfig).isNotEqualTo(sessionConfigBeforeValidErrorNotification)
    }

    private fun triggerOnErrorAndVerifyNewImageReceived(sessionConfig: SessionConfig) {
        frameSemaphore = Semaphore(0)
        // Forces invoke the onError callback
        runOnMainSync {
            sessionConfig.errorListener!!.onError(
                sessionConfig,
                SessionConfig.SessionError.SESSION_ERROR_UNKNOWN
            )
        }
        // Assert.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    private val workExecutorWithNamedThread: Executor
        get() {
            val threadFactory = ThreadFactory { runnable: Runnable? ->
                Thread(runnable, ANY_THREAD_NAME)
            }
            return Executors.newSingleThreadExecutor(threadFactory)
        }

    private fun getSurfaceProvider(
        threadNameConsumer: Consumer<String>? = null,
        resultListener: Consumer<SurfaceRequest.Result>? = null,
        frameAvailableListener: SurfaceTexture.OnFrameAvailableListener? = null
    ): Preview.SurfaceProvider {
        return SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider(
            frameAvailableListener,
            { surfaceRequest ->
                previewResolution = surfaceRequest.resolution
                threadNameConsumer?.accept(Thread.currentThread().name)
            },
            resultListener
        )
    }

    /*
     * Check if there is no frame callback for `noFrameIntervalMs` milliseconds, then it will
     * return true; If the total check time is over `timeoutMs` milliseconds, then it will return
     * false.
     */
    @Throws(InterruptedException::class)
    private fun noFrameCome(noFrameIntervalMs: Long, timeoutMs: Long): Boolean {
        require(!(noFrameIntervalMs <= 0 || timeoutMs <= 0)) { "Time can't be negative value." }
        require(timeoutMs >= noFrameIntervalMs) {
            "timeoutMs should be larger than noFrameIntervalMs."
        }
        val checkFrequency = 200L
        var totalCheckTime = 0L
        var zeroFrameTimer = 0L
        do {
            Thread.sleep(checkFrequency)
            if (frameSemaphore!!.availablePermits() > 0) {
                // Has frame, reset timer and frame count.
                zeroFrameTimer = 0
                frameSemaphore!!.drainPermits()
            } else {
                zeroFrameTimer += checkFrequency
            }
            if (zeroFrameTimer > noFrameIntervalMs) {
                return true
            }
            totalCheckTime += checkFrequency
        } while (totalCheckTime < timeoutMs)
        return false
    }

    private fun Semaphore.verifyFramesReceived(frameCount: Int, timeoutInSeconds: Long = 10) {
        assertThat(this.tryAcquire(frameCount, timeoutInSeconds, TimeUnit.SECONDS)).isTrue()
    }
}
