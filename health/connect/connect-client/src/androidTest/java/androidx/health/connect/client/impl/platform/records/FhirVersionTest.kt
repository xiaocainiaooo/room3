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

package androidx.health.connect.client.impl.platform.records

import android.os.Build
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.HealthConnectFeatures.Companion.FEATURE_STATUS_AVAILABLE
import androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi
import androidx.health.connect.client.feature.HealthConnectFeaturesPlatformImpl
import androidx.health.connect.client.records.FhirVersion
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class FhirVersionTest {
    @Test
    fun compareTo_sameVersion_returnsZero() {
        val version1 = FhirVersion(1, 0, 0)
        val version2 = FhirVersion(1, 0, 0)

        assertThat(version1.compareTo(version2)).isEqualTo(0)
        assertThat(version2.compareTo(version1)).isEqualTo(0)
    }

    @Test
    fun compareTo_differentMajor_returnsCorrectValue() {
        val version1 = FhirVersion(2, 0, 0)
        val version2 = FhirVersion(1, 0, 0)

        assertThat(version1.compareTo(version2)).isGreaterThan(0)
        assertThat(version2.compareTo(version1)).isLessThan(0)
    }

    @Test
    fun compareTo_differentMinor_returnsCorrectValue() {
        val version1 = FhirVersion(1, 1, 0)
        val version2 = FhirVersion(1, 0, 0)

        assertThat(version1.compareTo(version2)).isGreaterThan(0)
        assertThat(version2.compareTo(version1)).isLessThan(0)
    }

    @Test
    fun compareTo_differentPatch_returnsCorrectValue() {
        val version1 = FhirVersion(1, 0, 1)
        val version2 = FhirVersion(1, 0, 0)

        assertThat(version1.compareTo(version2)).isGreaterThan(0)
        assertThat(version2.compareTo(version1)).isLessThan(0)
    }

    @Test
    fun validFhirVersion_equals() {
        val fhirVersion1 = FhirVersion(major = 1, minor = 2, patch = 3)
        val fhirVersion2 = FhirVersion(major = 1, minor = 2, patch = 3)

        assertThat(fhirVersion1).isEqualTo(fhirVersion2)
    }

    @Test
    fun toFhirVersionString_expectCorrectString() {
        val fhirVersion = FhirVersion(major = 1, minor = 2, patch = 3)

        val fhirVersionString = fhirVersion.toFhirVersionString()

        assertThat(fhirVersionString).isEqualTo("1.2.3")
    }

    @Test
    fun parseFhirVersionString_invalidInput_expectError() {
        val input = "1.1.a"

        assertThrows(IllegalArgumentException::class.java) { FhirVersion.parseFhirVersion(input) }
    }

    @Test
    fun parseFhirVersionString_validInput_expectCorrectFhirVersion() {
        val input = "1.1.10"

        val fhirVersion = FhirVersion.parseFhirVersion(input)

        assertThat(fhirVersion).isEqualTo(FhirVersion(major = 1, minor = 1, patch = 10))
    }

    @OptIn(ExperimentalFeatureAvailabilityApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
    fun isSupportedVersion_notSupportedVersion_expectFalse() {
        assumeTrue(
            HealthConnectFeaturesPlatformImpl.getFeatureStatus(
                HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD
            ) == FEATURE_STATUS_AVAILABLE
        )

        val fhirVersion = FhirVersion(major = 1, minor = 2, patch = 3)

        assertThat(fhirVersion.isSupportedFhirVersion()).isFalse()
    }

    @OptIn(ExperimentalFeatureAvailabilityApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
    fun isSupportedVersion_supportedVersion_expectTrue() {
        assumeTrue(
            HealthConnectFeaturesPlatformImpl.getFeatureStatus(
                HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD
            ) == FEATURE_STATUS_AVAILABLE
        )

        val fhirVersion = FhirVersion(major = 4, minor = 0, patch = 1)

        assertThat(fhirVersion.isSupportedFhirVersion()).isTrue()
    }
}
