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

package androidx.privacysandbox.ui.core

import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.RestrictTo

/**
 * A class containing values that will be constant for the lifetime of a
 * [SandboxedUiAdapter.Session].
 */
class SessionConstants
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    /**
     * The input token of the window hosting this session.
     *
     * This value will be used when [Build.VERSION.SDK_INT] is equal to
     * [Build.VERSION_CODES.UPSIDE_DOWN_CAKE].
     */
    val windowInputToken: IBinder?
) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor() : this(null)

    companion object {
        private const val KEY_WINDOW_INPUT_TOKEN = "windowInputToken"

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun toBundle(sessionConstants: SessionConstants): Bundle {
            val bundle = Bundle()
            sessionConstants.windowInputToken?.let { bundle.putBinder(KEY_WINDOW_INPUT_TOKEN, it) }
            return bundle
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun fromBundle(bundle: Bundle): SessionConstants {
            val windowInputToken = bundle.getBinder(KEY_WINDOW_INPUT_TOKEN)
            return SessionConstants(windowInputToken)
        }
    }

    override fun toString() = "SessionConstants windowInputToken=$windowInputToken"

    override fun hashCode(): Int {
        return windowInputToken.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionConstants) return false

        return windowInputToken == other.windowInputToken
    }
}
