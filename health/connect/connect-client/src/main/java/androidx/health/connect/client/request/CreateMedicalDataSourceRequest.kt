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

package androidx.health.connect.client.request

import android.annotation.SuppressLint
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.health.connect.client.feature.withPhrFeatureCheck
import androidx.health.connect.client.impl.platform.request.PlatformCreateMedicalDataSourceRequest
import androidx.health.connect.client.impl.platform.request.PlatformCreateMedicalDataSourceRequestBuilder
import androidx.health.connect.client.records.FhirVersion
import androidx.health.connect.client.records.toString
import kotlin.String

/**
 * A create request for
 * [androidx.health.connect.client.HealthConnectClient.createMedicalDataSource].
 *
 * The medical data is represented using the Fast Healthcare Interoperability Resources
 * ([FHIR](https://hl7.org/fhir/)) standard.
 */
// TODO(b/382278995): remove @RestrictTo and internal to unhide PHR APIs
@RestrictTo(RestrictTo.Scope.LIBRARY)
class CreateMedicalDataSourceRequest(
    /**
     * The FHIR base URI of the data source. For data coming from a FHIR server this should be the
     * base URL.
     */
    val fhirBaseUri: Uri,
    /** The display name that describes the data source. This must be unique per app. */
    val displayName: String,
    /**
     * The FHIR version of the medical data that will be linked to this data source. This has to be
     * a version supported by Health Connect, as documented on the [FhirVersion].
     */
    val fhirVersion: FhirVersion
) {
    @SuppressLint("NewApi") // already checked with a feature availability check
    internal val platformCreateMedicalDataSourceRequest: PlatformCreateMedicalDataSourceRequest =
        withPhrFeatureCheck(this::class) {
            PlatformCreateMedicalDataSourceRequestBuilder(
                    fhirBaseUri,
                    displayName,
                    fhirVersion.platformFhirVersion
                )
                .build()
        }

    override fun toString() =
        toString(
            this,
            mapOf(
                "fhirBaseUri" to fhirBaseUri,
                "displayName" to displayName,
                "fhirVersion" to fhirVersion
            )
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CreateMedicalDataSourceRequest

        if (fhirBaseUri != other.fhirBaseUri) return false
        if (displayName != other.displayName) return false
        if (fhirVersion != other.fhirVersion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fhirBaseUri.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + fhirVersion.hashCode()
        return result
    }
}
