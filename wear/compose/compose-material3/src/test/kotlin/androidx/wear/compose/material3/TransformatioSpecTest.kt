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

package androidx.wear.compose.material3

import androidx.compose.animation.core.CubicBezierEasing
import androidx.wear.compose.material3.lazy.TransformVariableSpec
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.responsiveTransformationSpec
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TransformatioSpecTest {

    private val SPEC1 =
        TransformationSpec(
            0.01f,
            0.02f,
            0.03f,
            0.04f,
            CubicBezierEasing(0.015f, 0.025f, 0.035f, 0.045f),
            TransformVariableSpec(0.011f, 0.021f, 0.031f, 0.041f),
            TransformVariableSpec(0.012f, 0.022f, 0.032f, 0.042f),
            TransformVariableSpec(0.013f, 0.023f, 0.033f, 0.043f),
            TransformVariableSpec(0.014f, 0.024f, 0.034f, 0.044f),
            0.95f,
            0.86f,
        )

    private val SPEC2 =
        TransformationSpec(
            0.1f,
            0.2f,
            0.3f,
            0.4f,
            CubicBezierEasing(0.15f, 0.25f, 0.35f, 0.45f),
            TransformVariableSpec(0.11f, 0.21f, 0.31f, 0.41f),
            TransformVariableSpec(0.12f, 0.22f, 0.32f, 0.42f),
            TransformVariableSpec(0.13f, 0.23f, 0.33f, 0.43f),
            TransformVariableSpec(0.14f, 0.24f, 0.34f, 0.44f),
            0.95f,
            0.86f,
        )

    private val SPECS = listOf(200 to SPEC1, 220 to SPEC2)

    @Test fun responsive_spec_coerced_to_min_screen_size() = check_responsive_spec(180, SPEC1)

    @Test fun responsive_spec_for_min_screen_size() = check_responsive_spec(200, SPEC1)

    @Test fun responsive_spec_for_max_screen_size() = check_responsive_spec(220, SPEC2)

    @Test fun responsive_spec_coerced_to_max_screen_size() = check_responsive_spec(221, SPEC2)

    @Test
    fun responsive_spec_for_mid_screen_size() {
        val spec = responsiveTransformationSpec(210, SPECS)

        assertEquals(0.055f, spec.minElementHeight, EPSILON)
        assertEquals(0.11f, spec.maxElementHeight, EPSILON)
        assertEquals(0.165f, spec.minTransitionArea, EPSILON)
        assertEquals(0.22f, spec.maxTransitionArea, EPSILON)

        assertEquals(0.066f, spec.contentAlpha.topValue, EPSILON)
        assertEquals(0.121f, spec.contentAlpha.bottomValue, EPSILON)
        assertEquals(0.176f, spec.contentAlpha.transformationZoneEnterFraction, EPSILON)
        assertEquals(0.231f, spec.contentAlpha.transformationZoneExitFraction, EPSILON)
    }

    @Test(expected = IllegalArgumentException::class)
    fun responsive_with_no_spec() {
        responsiveTransformationSpec(200, emptyList())
    }

    @Test
    fun responsive_with_one_spec() {
        val specs1 = listOf(200 to SPEC1)

        assertEquals(SPEC1, responsiveTransformationSpec(199, specs1))
        assertEquals(SPEC1, responsiveTransformationSpec(200, specs1))
        assertEquals(SPEC1, responsiveTransformationSpec(201, specs1))
    }

    @Test
    fun responsive_with_three_specs() {
        val specs3 =
            listOf(
                100 to SPEC1, // 0.01f, 0.02f, 0.03f, 0.04f
                200 to SPEC2, // 0.1f, 0.2f, 0.3f, 0.4f
                300 to
                    SPEC2.copy(
                        minElementHeight = 0.5f,
                        maxElementHeight = 0.6f,
                        minTransitionArea = 0.7f,
                        maxTransitionArea = 0.7f,
                    )
            )

        assertEquals(SPEC1, responsiveTransformationSpec(100, specs3))
        assertEquals(0.11f, responsiveTransformationSpec(150, specs3).maxElementHeight, EPSILON)
        assertEquals(0.1f, responsiveTransformationSpec(200, specs3).minElementHeight, EPSILON)
        assertEquals(0.55f, responsiveTransformationSpec(250, specs3).maxTransitionArea, EPSILON)
        assertEquals(specs3.last().second, responsiveTransformationSpec(300, specs3))
    }

    @Test
    fun copy_overrides_transformation_area_configuration() {
        val spec =
            SPEC2.copy(
                minElementHeight = 0.51f,
                maxElementHeight = 0.52f,
                minTransitionArea = 0.53f,
                maxTransitionArea = 0.54f
            )

        assertEquals(0.51f, spec.minElementHeight)
        assertEquals(0.52f, spec.maxElementHeight)
        assertEquals(0.53f, spec.minTransitionArea)
        assertEquals(0.54f, spec.maxTransitionArea)
        assertEquals(spec.easing, SPEC2.easing)
        assertEquals(spec.scale, SPEC2.scale)
        assertEquals(spec.containerAlpha, SPEC2.containerAlpha)
        assertEquals(spec.contentAlpha, SPEC2.contentAlpha)
    }

    private val EPSILON = 1e-5f

    private fun check_responsive_spec(screenSize: Int, expectedSpec: TransformationSpec) {
        val spec = responsiveTransformationSpec(screenSize, SPECS)
        assertEquals(expectedSpec, spec)
    }
}
