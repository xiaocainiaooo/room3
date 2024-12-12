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
package androidx.health.connect.client.records

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.health.connect.client.feature.requireFeaturePersonalHealthRecordAvailable
import androidx.health.connect.client.impl.platform.records.toPlatformFhirVersion

/**
 * Represents the FHIR version. This is designed according to
 * [the official FHIR versions](https://build.fhir.org/versions.html#versions) of the Fast
 * Healthcare Interoperability Resources (FHIR) standard. "label", which represents a 'working'
 * version, is not supported for now.
 *
 * The versions R4 (4.0.1) and R4B (4.3.0) are supported in Health Connect.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class FhirVersion(val major: Int, val minor: Int, val patch: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FhirVersion) return false

        if (major != other.major) return false
        if (minor != other.minor) return false
        if (patch != other.patch) return false

        return true
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        return result
    }

    /** Returns the string representation of the FHIR version. */
    fun toFhirVersionString(): String {
        return "$major.$minor.$patch"
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName}: ${toFhirVersionString()}"
    }

    /** Returns `true` if this [FhirVersion] is supported, `false` otherwise. */
    @SuppressLint("NewApi") // checked by `getFeatureStatus()`
    fun isSupportedFhirVersion(): Boolean {
        requireFeaturePersonalHealthRecordAvailable()
        return toPlatformFhirVersion().isSupportedFhirVersion
    }

    companion object {
        private val VERSION_REGEX = Regex("(\\d+)\\.(\\d+)\\.(\\d+)")

        /**
         * Creates a [FhirVersion] object with the version of string format.
         *
         * The format should look like "4.0.1" which contains 3 numbers - major, minor and patch,
         * separated by ".". This aligns with
         * [the official FHIR versions](https://build.fhir.org/versions.html#versions). Note that
         * the "label" is not supported for now, which represents a 'working' version.
         */
        fun parseFhirVersion(fhirVersionString: String): FhirVersion {
            val result = VERSION_REGEX.find(fhirVersionString)
            require(result != null) { "Invalid FHIR version string: $fhirVersionString" }
            return FhirVersion(
                result.groupValues[1].toInt(),
                result.groupValues[2].toInt(),
                result.groupValues[3].toInt()
            )
        }
    }
}
