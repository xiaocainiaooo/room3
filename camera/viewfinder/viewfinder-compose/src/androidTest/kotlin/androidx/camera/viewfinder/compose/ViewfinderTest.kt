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

package androidx.camera.viewfinder.compose

import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class ViewfinderTest(private val implementationMode: ImplementationMode) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "implementationMode = {0}")
        fun data(): Array<ImplementationMode> =
            arrayOf(ImplementationMode.EXTERNAL, ImplementationMode.EMBEDDED)
    }

    @get:Rule val rule = createComposeRule()

    @Test
    fun coordinatesTransformationSameSizeNoRotation(): Unit = runBlocking {
        val coordinateTransformer = MutableCoordinateTransformer()

        rule.setContent {
            with(LocalDensity.current) {
                TestViewfinder(
                    modifier = Modifier.size(540.toDp(), 960.toDp()),
                    coordinateTransformer = coordinateTransformer,
                ) {}
            }
        }

        val expectedMatrix =
            Matrix(
                values =
                    floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
            )

        assertThat(coordinateTransformer.transformMatrix.values).isEqualTo(expectedMatrix.values)
    }

    @Test
    fun coordinatesTransformationSameSizeWithHalfCrop(): Unit = runBlocking {
        // Viewfinder size: 1080x1920
        // Surface size: 1080x1920
        // Crop rect size: 540x960

        val coordinateTransformer = MutableCoordinateTransformer()

        rule.setContent {
            with(LocalDensity.current) {
                TestViewfinder(
                    modifier = Modifier.size(540.toDp(), 960.toDp()),
                    transformationInfo =
                        TransformationInfo(
                            sourceRotation = 0,
                            isSourceMirroredHorizontally = false,
                            isSourceMirroredVertically = false,
                            cropRectLeft = 0f,
                            cropRectTop = 0f,
                            cropRectRight = 270f,
                            cropRectBottom = 480f,
                        ),
                    coordinateTransformer = coordinateTransformer,
                ) {}
            }
        }

        val expectedMatrix =
            Matrix(
                values =
                    floatArrayOf(0.5f, 0f, 0f, 0f, 0f, 0.5f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
            )

        assertThat(coordinateTransformer.transformMatrix.values).isEqualTo(expectedMatrix.values)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M) // Needed for Surface.lockHardwareCanvas()
    @Test
    fun canRetrieveSurface() = runBlocking {
        testWithSession {
            val surface = awaitSurface()
            surface.lockHardwareCanvas().apply {
                try {
                    assertThat(Size(width, height))
                        .isEqualTo(ViewfinderTestParams.Default.sourceResolution)
                } finally {
                    surface.unlockCanvasAndPost(this)
                }
            }
        }
    }

    @Test
    fun verifySurfacesAreReleased_surfaceRequestReleased_thenComposableDestroyed(): Unit =
        runBlocking {
            testWithSession {
                val surface = awaitSurface()
                assertThat(surface.isValid).isTrue()

                allowSessionCompletion()
                rule.awaitIdle()
                assertThat(surface.isValid).isTrue()

                hideViewfinder()
                rule.awaitIdle()
                assertThat(surface.isValid).isFalse()
            }
        }

    @Test
    fun verifySurfacesAreReleased_composableDestroyed_thenSurfaceRequestReleased(): Unit =
        runBlocking {
            assume()
                .withMessage(
                    "EXTERNAL implamentation on API < 29 is not yet able to delay surface destruction by the Viewfinder."
                )
                .that(
                    Build.VERSION.SDK_INT >= 29 || implementationMode != ImplementationMode.EXTERNAL
                )
                .isTrue()
            testWithSession {
                val surface = awaitSurface()
                assertThat(surface.isValid).isTrue()

                hideViewfinder()
                rule.awaitIdle()
                assertThat(surface.isValid).isTrue()

                allowSessionCompletion()
                rule.awaitIdle()
                assertThat(surface.isValid).isFalse()
            }
        }

    private interface SessionTestScope {
        fun hideViewfinder()

        suspend fun awaitSurface(): Surface

        fun allowSessionCompletion()
    }

    private suspend fun testWithSession(block: suspend SessionTestScope.() -> Unit) {
        val surfaceDeferred = CompletableDeferred<Surface>()
        val sessionCompleteDeferred = CompletableDeferred<Unit>()

        val showViewfinder = mutableStateOf(true)

        rule.setContent {
            val showView by remember { showViewfinder }
            TestViewfinder(showViewfinder = showView) {
                onSurfaceSession {
                    surfaceDeferred.complete(surface)
                    withContext(NonCancellable) { sessionCompleteDeferred.await() }
                }
            }
        }

        val sessionTestScope =
            object : SessionTestScope {
                override fun hideViewfinder() {
                    showViewfinder.value = false
                }

                override suspend fun awaitSurface(): Surface {
                    return surfaceDeferred.await()
                }

                override fun allowSessionCompletion() {
                    sessionCompleteDeferred.complete(Unit)
                }
            }

        try {
            block.invoke(sessionTestScope)
        } finally {
            sessionCompleteDeferred.cancel()
        }
    }

    @Composable
    fun TestViewfinder(
        modifier: Modifier = Modifier.size(ViewfinderTestParams.Default.viewfinderSize),
        showViewfinder: Boolean = true,
        transformationInfo: TransformationInfo = ViewfinderTestParams.Default.transformationInfo,
        surfaceRequest: ViewfinderSurfaceRequest = remember {
            ViewfinderSurfaceRequest(
                width = ViewfinderTestParams.Default.sourceResolution.width,
                height = ViewfinderTestParams.Default.sourceResolution.height,
                implementationMode = implementationMode,
            )
        },
        coordinateTransformer: MutableCoordinateTransformer? = null,
        onInit: ViewfinderInitScope.() -> Unit,
    ) {
        Column {
            if (showViewfinder) {
                Viewfinder(
                    modifier = modifier,
                    surfaceRequest = surfaceRequest,
                    transformationInfo = transformationInfo,
                    coordinateTransformer = coordinateTransformer,
                    onInit = onInit,
                )
            }
        }
    }
}
