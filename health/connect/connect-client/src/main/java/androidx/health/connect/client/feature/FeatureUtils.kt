/*
 * Copyright (C) 2024 The Android Open Source Project
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
// To prevent an empty UtilsKt class from being exposed in APi files
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.health.connect.client.feature

import android.os.Build
import androidx.annotation.RestrictTo
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.HealthConnectFeatures.Companion.FEATURE_STATUS_AVAILABLE

@OptIn(ExperimentalFeatureAvailabilityApi::class)
internal fun requireFeaturePersonalHealthRecordAvailable() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        throw UnsupportedOperationException(
            "FEATURE_PERSONAL_HEALTH_RECORD is not available on this device."
        )
    }
    if (
        HealthConnectFeaturesPlatformImpl.getFeatureStatus(
            HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD
        ) != FEATURE_STATUS_AVAILABLE
    ) {
        throw UnsupportedOperationException(
            "FEATURE_PERSONAL_HEALTH_RECORD is not available on this device."
        )
    }
}
