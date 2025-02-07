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

import androidx.annotation.RestrictTo
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.HealthConnectFeatures.Companion.FEATURE_PERSONAL_HEALTH_RECORD
import androidx.health.connect.client.records.MedicalResource

/**
 * A base class for reading [MedicalResource]s with [HealthConnectClient.readMedicalResources].
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument.
 *
 * @property pageSize The maximum number of [MedicalResource]s to be read.
 * @see [HealthConnectClient.readMedicalResources]
 */
// TODO(b/382278995): remove @RestrictTo to unhide PHR APIs
@RestrictTo(RestrictTo.Scope.LIBRARY)
abstract class ReadMedicalResourcesRequest internal constructor(val pageSize: Int) {
    companion object {
        /** Default value for [ReadMedicalResourcesRequest.pageSize]. */
        const val DEFAULT_PAGE_SIZE = 1000
    }
}
