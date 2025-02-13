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

@file:Suppress("UNUSED_VARIABLE")

package androidx.health.connect.client.samples

import android.net.Uri
import androidx.annotation.Sampled
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.FhirVersion
import androidx.health.connect.client.request.CreateMedicalDataSourceRequest

@Sampled
suspend fun CreateMedicalDataSourceSample(healthConnectClient: HealthConnectClient) {
    val medicalDataSource =
        healthConnectClient.createMedicalDataSource(
            CreateMedicalDataSourceRequest(
                fhirBaseUri = Uri.parse("https://fhir.com/oauth/api/FHIR/R4/"),
                displayName = "Test Data Source",
                fhirVersion = FhirVersion(4, 0, 1)
            )
        )
}
