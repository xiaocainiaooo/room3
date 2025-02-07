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
import androidx.health.connect.client.records.MedicalResource.Companion.MedicalResourceType
import androidx.health.connect.client.request.ReadMedicalResourcesRequest.Companion.DEFAULT_PAGE_SIZE

/**
 * A class to make **initial** requests when reading [MedicalResource]s with
 * [HealthConnectClient.readMedicalResources].
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [FEATURE_PERSONAL_HEALTH_RECORD] as an argument.
 *
 * @param pageSize The maximum number of [MedicalResource]s to be read. Default value is
 *   [DEFAULT_PAGE_SIZE]. An [IllegalArgumentException] might be thrown if [pageSize] is deemed as
 *   invalid, such as too large.
 * @property medicalResourceType Only [MedicalResource]s with this [MedicalResourceType] will be
 *   returned. An [IllegalArgumentException] might be thrown if an invalid type is used, see
 *   [MedicalResourceType] for valid values.
 * @property medicalDataSourceIds Only [MedicalResource]s belong to one of the [MedicalDataSource]s
 *   represented by these IDs will be returned. An [IllegalArgumentException] might be thrown if any
 *   ID is deemed as invalid.
 * @see [HealthConnectClient.readMedicalResources]
 */
// TODO(b/382278995): remove @RestrictTo to unhide PHR APIs
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ReadMedicalResourcesInitialRequest(
    @MedicalResourceType val medicalResourceType: Int,
    val medicalDataSourceIds: Set<String>,
    pageSize: Int = DEFAULT_PAGE_SIZE
) : ReadMedicalResourcesRequest(pageSize) {
    // TODO: b/382682197 - The rest of this class & tests will be filled in a subsequent CL
}
