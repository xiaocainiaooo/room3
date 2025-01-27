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

package androidx.health.connect.client.impl.platform.requests

import android.annotation.SuppressLint
import android.net.Uri
import androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi
import androidx.health.connect.client.feature.isPersonalHealthRecordFeatureAvailableInPlatform
import androidx.health.connect.client.impl.platform.request.PlatformCreateMedicalDataSourceRequestBuilder
import androidx.health.connect.client.records.FhirVersion
import androidx.health.connect.client.request.CreateMedicalDataSourceRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CreateMedicalDataSourceRequestTest {

    @Before
    fun setup() {
        Assume.assumeTrue(
            "FEATURE_PERSONAL_HEALTH_RECORD is not available on this device!",
            isPersonalHealthRecordFeatureAvailableInPlatform()
        )
    }

    @Test
    fun validCreateMedicalDataSourceRequest_equals() {
        EqualsTester()
            .addEqualityGroup(
                CreateMedicalDataSourceRequest(
                    fhirBaseUri = fhirBaseUri,
                    displayName = DISPLAY_NAME,
                    fhirVersion = fhirVersion,
                ),
                CreateMedicalDataSourceRequest(
                    fhirBaseUri = fhirBaseUri,
                    displayName = DISPLAY_NAME,
                    fhirVersion = fhirVersion,
                )
            )
            .addEqualityGroup(
                CreateMedicalDataSourceRequest(
                    fhirBaseUri = fhirBaseUri.buildUpon().appendPath("2/").build(),
                    displayName = DISPLAY_NAME,
                    fhirVersion = fhirVersion,
                )
            )
            .addEqualityGroup(
                CreateMedicalDataSourceRequest(
                    fhirBaseUri = fhirBaseUri,
                    displayName = "$DISPLAY_NAME Two",
                    fhirVersion = fhirVersion,
                )
            )
            .addEqualityGroup(
                CreateMedicalDataSourceRequest(
                    fhirBaseUri = fhirBaseUri,
                    displayName = DISPLAY_NAME,
                    fhirVersion = FhirVersion(4, 3, 0),
                )
            )
            .testEquals()
    }

    @Test
    fun toString_expectCorrectString() {
        val createMedicalDataSourceRequest =
            CreateMedicalDataSourceRequest(
                fhirBaseUri = fhirBaseUri,
                displayName = DISPLAY_NAME,
                fhirVersion = fhirVersion,
            )

        val toString = createMedicalDataSourceRequest.toString()

        assertThat(toString).contains("fhirBaseUri=https://fhir.com/oauth/api/FHIR/R4/")
        assertThat(toString).contains("displayName=Test Data Source")
        assertThat(toString).contains("fhirVersion=FhirVersion")
        assertThat(toString).contains("(4.0.1)")
    }

    @SuppressLint("NewApi") // checked with feature availability check
    @OptIn(ExperimentalFeatureAvailabilityApi::class)
    @Test
    fun toPlatformCreateMedicalDataSourceRequest_expectCorrectConversion() {
        val createMedicalDataSourceRequest =
            CreateMedicalDataSourceRequest(
                fhirBaseUri = fhirBaseUri,
                displayName = DISPLAY_NAME,
                fhirVersion = fhirVersion,
            )

        val platformCreateMedicalDataSourceRequest =
            createMedicalDataSourceRequest.platformCreateMedicalDataSourceRequest

        assertThat(platformCreateMedicalDataSourceRequest)
            .isEqualTo(
                PlatformCreateMedicalDataSourceRequestBuilder(
                        fhirBaseUri,
                        DISPLAY_NAME,
                        fhirVersion.platformFhirVersion,
                    )
                    .build()
            )
    }

    companion object {
        private const val DISPLAY_NAME = "Test Data Source"
        private val fhirBaseUri = Uri.parse("https://fhir.com/oauth/api/FHIR/R4/")
        private val fhirVersion: FhirVersion by lazy { FhirVersion(4, 0, 1) }
    }
}
