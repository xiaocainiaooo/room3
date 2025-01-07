/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.camera.lifecycle

import androidx.camera.core.CompositionSettings
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraConfigs
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

@SmallTest
@SdkSuppress(minSdkVersion = 21)
class LifecycleCameraRepositoryTest {
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var repository: LifecycleCameraRepository
    private lateinit var cameraCoordinator: FakeCameraCoordinator
    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private var cameraId = 0
    private val camera: CameraInternal = FakeCamera(cameraId.toString())

    @Before
    fun setUp() {
        cameraCoordinator = FakeCameraCoordinator()
        lifecycleOwner = FakeLifecycleOwner()
        repository = LifecycleCameraRepository()
        cameraUseCaseAdapter =
            CameraUseCaseAdapter(
                camera,
                cameraCoordinator,
                FakeCameraDeviceSurfaceManager(),
                FakeUseCaseConfigFactory()
            )
    }

    @Test
    fun throwException_ifTryingToCreateWithExistingIdentifier() {
        repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)

        assertThrows(IllegalArgumentException::class.java) {
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        }
    }

    @Test
    fun differentLifecycleCamerasAreCreated_forDifferentLifecycles() {
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        val secondLifecycle = FakeLifecycleOwner()
        val secondLifecycleCamera =
            repository.createLifecycleCamera(secondLifecycle, cameraUseCaseAdapter)

        assertThat(firstLifecycleCamera).isNotEqualTo(secondLifecycleCamera)
    }

    @Test
    fun differentLifecycleCamerasAreCreated_forDifferentCameraSets() {
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)

        // Creates LifecycleCamera with different camera set
        val secondLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, createNewCameraUseCaseAdapter())

        assertThat(firstLifecycleCamera).isNotEqualTo(secondLifecycleCamera)
    }

    @Test
    fun differentLifecycleCamerasAreCreated_forDifferentCameraConfig() {
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)

        // Creates LifecycleCamera with different camera set
        val secondLifecycleCamera =
            repository.createLifecycleCamera(
                lifecycleOwner,
                createCameraUseCaseAdapterWithNewCameraConfig()
            )

        assertThat(firstLifecycleCamera).isNotEqualTo(secondLifecycleCamera)
    }

    @Test
    fun lifecycleCameraIsNotActive_createWithNoUseCasesAfterLifecycleStarted() {
        lifecycleOwner.start()
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        assertThat(lifecycleCamera.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraIsNotActive_createWithNoUseCasesBeforeLifecycleStarted() {
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        assertThat(lifecycleCamera.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraIsNotActive_bindUseCase_whenLifecycleIsNotStarted() {
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCamera(
            lifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )
        // LifecycleCamera is inactive before the lifecycle state becomes ON_START.
        assertThat(lifecycleCamera.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraIsActive_lifecycleStartedAfterBindUseCase() {
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCamera(
            lifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )
        lifecycleOwner.start()
        // LifecycleCamera is active after the lifecycle state becomes ON_START.
        assertThat(lifecycleCamera.isActive).isTrue()
    }

    @Test
    fun lifecycleCameraIsActive_bindToLifecycleCameraAfterLifecycleStarted() {
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        repository.bindToLifecycleCamera(
            lifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // LifecycleCamera is active after binding a use case when lifecycle state is ON_START.
        assertThat(lifecycleCamera.isActive).isTrue()
    }

    @Test
    fun throwException_withUseCase_twoLifecycleCamerasControlledByOneLifecycle() {
        // Creates first LifecycleCamera with use case bound.
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCamera(
            lifecycleCamera0,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // Creates second LifecycleCamera with use case bound to the same Lifecycle.
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycleOwner, createNewCameraUseCaseAdapter())
        assertThrows(IllegalArgumentException::class.java) {
            repository.bindToLifecycleCamera(
                lifecycleCamera1,
                null,
                listOf(),
                listOf(FakeUseCase()),
                cameraCoordinator
            )
        }
    }

    @Test
    fun lifecycleCameraIsNotActive_withNoUseCase_unbindAfterLifecycleStarted() {
        // Creates LifecycleCamera with use case bound.
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        val useCase = FakeUseCase()
        repository.bindToLifecycleCamera(
            lifecycleCamera,
            null,
            listOf(),
            listOf(useCase),
            cameraCoordinator
        )

        // Unbinds the use case that was bound previously.
        repository.unbind(listOf(useCase))

        // LifecycleCamera is not active if LifecycleCamera has no use case bound after unbinding
        // the use case.
        assertThat(lifecycleCamera.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraIsActive_withUseCase_unbindAfterLifecycleStarted() {
        // Creates LifecycleCamera with two use cases bound.
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        val useCase0 = FakeUseCase()
        val useCase1 = FakeUseCase()
        repository.bindToLifecycleCamera(
            lifecycleCamera,
            null,
            listOf(),
            listOf(useCase0, useCase1),
            cameraCoordinator
        )

        // Only unbinds one use case but another one is kept in the LifecycleCamera.
        repository.unbind(listOf(useCase0))

        // LifecycleCamera is active if LifecycleCamera still has use case bound after unbinding
        // the use case.
        assertThat(lifecycleCamera.isActive).isTrue()
    }

    @Test
    fun lifecycleCameraIsNotActive_unbindAllAfterLifecycleStarted() {
        // Creates LifecycleCamera with use case bound.
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        repository.bindToLifecycleCamera(
            lifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // Unbinds all use cases from all LifecycleCamera by the unbindAll() API.
        repository.unbindAll()

        // LifecycleCamera is not active after unbinding all use cases.
        assertThat(lifecycleCamera.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraOf1stActiveLifecycleIsInactive_bindToNewActiveLifecycleCamera() {
        // Starts first lifecycle with use case bound.
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        repository.bindToLifecycleCamera(
            lifecycleCamera0,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // Starts second lifecycle with use case bound.
        val lifecycle1 = FakeLifecycleOwner()
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycle1, createNewCameraUseCaseAdapter())
        lifecycle1.start()
        repository.bindToLifecycleCamera(
            lifecycleCamera1,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // The previous LifecycleCamera becomes inactive after new LifecycleCamera becomes active.
        assertThat(lifecycleCamera0.isActive).isFalse()
        // New LifecycleCamera becomes active after binding use case to it.
        assertThat(lifecycleCamera1.isActive).isTrue()
    }

    @Test
    fun lifecycleCameraOf1stActiveLifecycleIsActive_bindNewUseCase() {
        // Starts first lifecycle with use case bound.
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        repository.bindToLifecycleCamera(
            lifecycleCamera0,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // Starts second lifecycle with use case bound.
        val lifecycle1 = FakeLifecycleOwner()
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycle1, createNewCameraUseCaseAdapter())
        lifecycle1.start()
        repository.bindToLifecycleCamera(
            lifecycleCamera1,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // Binds new use case to the next most recent active LifecycleCamera.
        repository.bindToLifecycleCamera(
            lifecycleCamera0,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // The next most recent active LifecycleCamera becomes active after binding new use case.
        assertThat(lifecycleCamera0.isActive).isTrue()
        // The original active LifecycleCamera becomes inactive after the next most recent active
        // LifecycleCamera becomes active.
        assertThat(lifecycleCamera1.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraOf2ndActiveLifecycleIsActive_unbindFromActiveLifecycleCamera() {
        // Starts first lifecycle with use case bound.
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        repository.bindToLifecycleCamera(
            lifecycleCamera0,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // Starts second lifecycle with use case bound.
        val lifecycle1 = FakeLifecycleOwner()
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycle1, createNewCameraUseCaseAdapter())
        lifecycle1.start()
        val useCase = FakeUseCase()
        repository.bindToLifecycleCamera(
            lifecycleCamera1,
            null,
            listOf(),
            listOf(useCase),
            cameraCoordinator
        )

        // Unbinds use case from the most recent active LifecycleCamera.
        repository.unbind(listOf(useCase))

        // The most recent active LifecycleCamera becomes inactive after all use case unbound
        // from it.
        assertThat(lifecycleCamera1.isActive).isFalse()
        // The next most recent active LifecycleCamera becomes active after previous active
        // LifecycleCamera becomes inactive.
        assertThat(lifecycleCamera0.isActive).isTrue()
    }

    @Test
    fun useCaseIsCleared_whenLifecycleIsDestroyed() {
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        val useCase = FakeUseCase()
        repository.bindToLifecycleCamera(
            lifecycleCamera,
            null,
            listOf(),
            listOf(useCase),
            cameraCoordinator
        )

        assertThat(useCase.isDetached).isFalse()

        lifecycleOwner.destroy()

        assertThat(useCase.isDetached).isTrue()
    }

    @Test
    fun lifecycleCameraIsStopped_whenNewLifecycleIsStarted() {
        // Starts first lifecycle and check LifecycleCamera active state is true.
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCamera(
            firstLifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )
        lifecycleOwner.start()
        assertThat(firstLifecycleCamera.isActive).isTrue()

        // Starts second lifecycle and check previous LifecycleCamera is stopped.
        val secondLifecycle = FakeLifecycleOwner()
        val secondLifecycleCamera =
            repository.createLifecycleCamera(secondLifecycle, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCamera(
            secondLifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )
        secondLifecycle.start()
        assertThat(secondLifecycleCamera.isActive).isTrue()
        assertThat(firstLifecycleCamera.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraOf2ndActiveLifecycleIsStarted_when1stActiveLifecycleIsStopped() {
        // Starts first lifecycle and check LifecycleCamera active state is true.
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCamera(
            firstLifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )
        lifecycleOwner.start()
        assertThat(firstLifecycleCamera.isActive).isTrue()

        // Starts second lifecycle and check previous LifecycleCamera is stopped.
        val secondLifecycle = FakeLifecycleOwner()
        val secondLifecycleCamera =
            repository.createLifecycleCamera(secondLifecycle, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCamera(
            secondLifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )
        secondLifecycle.start()
        assertThat(secondLifecycleCamera.isActive).isTrue()
        assertThat(firstLifecycleCamera.isActive).isFalse()

        // Stops second lifecycle and check previous LifecycleCamera is started again.
        secondLifecycle.stop()
        assertThat(secondLifecycleCamera.isActive).isFalse()
        assertThat(firstLifecycleCamera.isActive).isTrue()
    }

    @Test
    fun lifecycleCameraWithUseCaseIsActive_whenNewLifecycleCameraWithoutUseCaseIsStarted() {
        // Starts first LifecycleCamera with use case bound.
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCamera(
            firstLifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )
        lifecycleOwner.start()
        assertThat(firstLifecycleCamera.isActive).isTrue()

        // Starts second LifecycleCamera without use case bound.
        val secondLifecycle = FakeLifecycleOwner()
        val secondLifecycleCamera =
            repository.createLifecycleCamera(secondLifecycle, createNewCameraUseCaseAdapter())
        secondLifecycle.start()

        // The first LifecycleCamera is still active because the second LifecycleCamera won't
        // become active when there is no use case bound.
        assertThat(firstLifecycleCamera.isActive).isTrue()
        assertThat(secondLifecycleCamera.isActive).isFalse()
    }

    @Test
    fun onlyLifecycleCameraWithUseCaseIsActive_afterLifecycleIsStarted() {
        // Starts first LifecycleCamera with no use case bound.
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()

        // Starts second LifecycleCamera with use case bound to the same Lifecycle.
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycleOwner, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCamera(
            lifecycleCamera1,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // Starts third LifecycleCamera with no use case bound to the same Lifecycle.
        val lifecycleCamera2 =
            repository.createLifecycleCamera(lifecycleOwner, createNewCameraUseCaseAdapter())

        // Checks only the LifecycleCamera with use case bound can become active.
        assertThat(lifecycleCamera0.isActive).isFalse()
        assertThat(lifecycleCamera1.isActive).isTrue()
        assertThat(lifecycleCamera2.isActive).isFalse()

        // Stops and resumes the lifecycle
        lifecycleOwner.stop()
        lifecycleOwner.start()

        // Checks still only the LifecycleCamera with use case bound is active.
        assertThat(lifecycleCamera0.isActive).isFalse()
        assertThat(lifecycleCamera1.isActive).isTrue()
        assertThat(lifecycleCamera2.isActive).isFalse()
    }

    @Test
    fun retrievesExistingCamera() {
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        val retrieved =
            repository.getLifecycleCamera(
                lifecycleOwner,
                CameraUseCaseAdapter.CameraId.create(
                    camera.getCameraInfoInternal().getCameraId(),
                    CameraConfigs.defaultConfig().getCompatibilityId()
                )
            )

        assertThat(lifecycleCamera).isSameInstanceAs(retrieved)
    }

    @Test
    fun lifecycleCameraWithDifferentCameraConfig_returnDifferentInstance() {
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)

        val newCameraUseCaseAdapter = createCameraUseCaseAdapterWithNewCameraConfig()
        val lifecycleCamera2 =
            repository.createLifecycleCamera(lifecycleOwner, newCameraUseCaseAdapter)

        val retrieved1 =
            repository.getLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter.cameraId)

        val retrieved2 =
            repository.getLifecycleCamera(lifecycleOwner, newCameraUseCaseAdapter.cameraId)

        assertThat(lifecycleCamera1).isSameInstanceAs(retrieved1)
        assertThat(lifecycleCamera2).isSameInstanceAs(retrieved2)
        assertThat(retrieved1).isNotSameInstanceAs(retrieved2)
    }

    @Test
    fun keys() {
        val key0 =
            LifecycleCameraRepository.Key.create(lifecycleOwner, cameraUseCaseAdapter.cameraId)
        val key1 =
            LifecycleCameraRepository.Key.create(
                lifecycleOwner,
                CameraUseCaseAdapter.CameraId.create(
                    camera.getCameraInfoInternal().getCameraId(),
                    CameraConfigs.defaultConfig().getCompatibilityId()
                )
            )

        assertThat(key0).isEqualTo(key1)
    }

    @Test
    fun noException_setInactiveAfterUnregisterLifecycle() {
        // This test simulate an ON_STOP event comes after an ON_DESTROY event. It should be an
        // abnormal case and the FakeLifecycleOwner will throw IllegalStateException. See
        // b/222105787 for why this test is added.

        // Starts LifecycleCamera with use case bound.

        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCamera(
            lifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )
        lifecycleOwner.start()
        assertThat(lifecycleCamera.isActive).isTrue()

        // This will be called when an ON_DESTROY event is received.
        repository.unregisterLifecycle(lifecycleOwner)

        // This will be called when an ON_STOP event is received.
        repository.setInactive(lifecycleOwner)
    }

    @Test
    fun concurrentModeOn_twoLifecycleCamerasControlledByOneLifecycle_start() {
        cameraCoordinator.setCameraOperatingMode(CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT)

        // Starts first lifecycle camera
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCamera(
            lifecycleCamera0,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // Starts second lifecycle camera
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycleOwner, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCamera(
            lifecycleCamera1,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // Starts lifecycle
        lifecycleOwner.start()

        // Both cameras are active in concurrent mode
        assertThat(lifecycleCamera0.isActive).isTrue()
        assertThat(lifecycleCamera1.isActive).isTrue()
    }

    @Test
    fun concurrentModeOn_twoLifecycleCamerasControlledByTwoLifecycles_start() {
        cameraCoordinator.setCameraOperatingMode(CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT)

        // Starts first lifecycle camera
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCamera(
            lifecycleCamera0,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // Starts lifecycle
        lifecycleOwner.start()

        // Starts second lifecycle camera
        val lifecycle1 = FakeLifecycleOwner()
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycle1, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCamera(
            lifecycleCamera1,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // Starts lifecycle1
        lifecycle1.start()

        // Both cameras are active in concurrent mode
        assertThat(lifecycleCamera0.isActive).isTrue()
        assertThat(lifecycleCamera1.isActive).isTrue()
    }

    @Test
    fun concurrentModeOn_twoLifecycleCamerasControlledByOneLifecycle_stop() {
        cameraCoordinator.setCameraOperatingMode(CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT)

        // Starts first lifecycle camera
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCamera(
            firstLifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // Starts second lifecycle camera
        val secondLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCamera(
            secondLifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        // Starts lifecycle
        lifecycleOwner.start()
        assertThat(secondLifecycleCamera.isActive).isTrue()
        assertThat(firstLifecycleCamera.isActive).isTrue()

        // Stops lifecycle
        lifecycleOwner.stop()
        assertThat(secondLifecycleCamera.isActive).isFalse()
        assertThat(firstLifecycleCamera.isActive).isFalse()
    }

    @Test
    fun concurrentModeOn_twoLifecycleCamerasControlledByTwoLifecycles_stop() {
        cameraCoordinator.setCameraOperatingMode(CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT)

        // Starts first lifecycle camera
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCamera(
            firstLifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )
        lifecycleOwner.start()
        assertThat(firstLifecycleCamera.isActive).isTrue()

        // Starts second lifecycle camera
        val secondLifecycle = FakeLifecycleOwner()
        val secondLifecycleCamera =
            repository.createLifecycleCamera(secondLifecycle, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCamera(
            secondLifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )
        secondLifecycle.start()
        assertThat(secondLifecycleCamera.isActive).isTrue()
        assertThat(firstLifecycleCamera.isActive).isTrue()

        // Stops lifecycle
        secondLifecycle.stop()
        assertThat(secondLifecycleCamera.isActive).isFalse()
        assertThat(firstLifecycleCamera.isActive).isTrue()
    }

    @Test
    fun lifecycleCameraIsInactive_createAndBindToLifecycleCamera_AfterLifecycleDestroyed() {
        lifecycleOwner.destroy()
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCamera(
            lifecycleCamera,
            null,
            listOf(),
            listOf(FakeUseCase()),
            cameraCoordinator
        )

        assertThat(lifecycleCamera.isActive).isFalse()
    }

    private fun createNewCameraUseCaseAdapter(): CameraUseCaseAdapter {
        val cameraId = (++cameraId).toString()
        val fakeCamera: CameraInternal = FakeCamera(cameraId)
        return CameraUseCaseAdapter(
            fakeCamera,
            cameraCoordinator,
            FakeCameraDeviceSurfaceManager(),
            FakeUseCaseConfigFactory()
        )
    }

    private fun createCameraUseCaseAdapterWithNewCameraConfig(): CameraUseCaseAdapter {
        val cameraConfig: CameraConfig = FakeCameraConfig()
        return CameraUseCaseAdapter(
            camera,
            null,
            AdapterCameraInfo(camera.cameraInfo as CameraInfoInternal, cameraConfig),
            null,
            CompositionSettings.DEFAULT,
            CompositionSettings.DEFAULT,
            cameraCoordinator,
            FakeCameraDeviceSurfaceManager(),
            FakeUseCaseConfigFactory()
        )
    }
}
