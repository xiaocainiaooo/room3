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

package androidx.appfunctions

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.junit.AssumptionViolatedException
import org.junit.Test

class AppFunctionExceptionTest {
    @Test
    fun testConstructor_withoutMessageAndExtras() {
        val exception = AppFunctionException(AppFunctionException.ERROR_DENIED)

        assertThat(exception.errorCode).isEqualTo(AppFunctionException.ERROR_DENIED)
        assertThat(exception.errorMessage).isNull()
        assertThat(exception.extras).isEqualTo(Bundle.EMPTY)
    }

    @Test
    fun testConstructor_withoutExtras() {
        val exception = AppFunctionException(AppFunctionException.ERROR_DENIED, "testMessage")

        assertThat(exception.errorCode).isEqualTo(AppFunctionException.ERROR_DENIED)
        assertThat(exception.errorMessage).isEqualTo("testMessage")
        assertThat(exception.extras).isEqualTo(Bundle.EMPTY)
    }

    @Test
    fun testConstructor() {
        val extras = Bundle().apply { putString("testKey", "testValue") }
        val exception =
            AppFunctionException(AppFunctionException.ERROR_DENIED, "testMessage", extras)

        assertThat(exception.errorCode).isEqualTo(AppFunctionException.ERROR_DENIED)
        assertThat(exception.errorMessage).isEqualTo("testMessage")
        assertThat(exception.extras.getString("testKey")).isEqualTo("testValue")
    }

    @Test
    fun testErrorCategory_RequestError() {
        assertThat(AppFunctionException(AppFunctionException.ERROR_DENIED).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)
        assertThat(AppFunctionException(AppFunctionException.ERROR_INVALID_ARGUMENT).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)
        assertThat(AppFunctionException(AppFunctionException.ERROR_DISABLED).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)
        assertThat(
                AppFunctionException(AppFunctionException.ERROR_FUNCTION_NOT_FOUND).errorCategory
            )
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)
        assertThat(
                AppFunctionException(AppFunctionException.ERROR_RESOURCE_NOT_FOUND).errorCategory
            )
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)
        assertThat(AppFunctionException(AppFunctionException.ERROR_LIMIT_EXCEEDED).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)
        assertThat(
                AppFunctionException(AppFunctionException.ERROR_RESOURCE_ALREADY_EXISTS)
                    .errorCategory
            )
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)
    }

    @Test
    fun testErrorCategory_SystemError() {
        assertThat(AppFunctionException(AppFunctionException.ERROR_SYSTEM_ERROR).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_SYSTEM)
        assertThat(AppFunctionException(AppFunctionException.ERROR_CANCELLED).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_SYSTEM)
    }

    @Test
    fun testErrorCategory_AppError() {
        assertThat(AppFunctionException(AppFunctionException.ERROR_APP_UNKNOWN_ERROR).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_APP)
        assertThat(
                AppFunctionException(AppFunctionException.ERROR_PERMISSION_REQUIRED).errorCategory
            )
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_APP)
        assertThat(AppFunctionException(AppFunctionException.ERROR_NOT_SUPPORTED).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_APP)
    }

    @Test
    fun testTransformToPlatformExtensionsClass() {
        assumeAppFunctionExtensionLibraryAvailable()
        val extras = Bundle().apply { putString("testKey", "testValue") }
        val exception =
            AppFunctionException(AppFunctionException.ERROR_DENIED, "testMessage", extras)

        val platformException = exception.toPlatformExtensionsClass()

        assertThat(platformException.errorCode).isEqualTo(AppFunctionException.ERROR_DENIED)
        assertThat(platformException.errorMessage).isEqualTo("testMessage")
        assertThat(platformException.extras.getString("testKey")).isEqualTo("testValue")
    }

    @Test
    fun testCreateFromPlatformExtensionsClass() {
        assumeAppFunctionExtensionLibraryAvailable()
        val extras = Bundle().apply { putString("testKey", "testValue") }
        val platformException =
            com.android.extensions.appfunctions.AppFunctionException(
                AppFunctionException.ERROR_DENIED,
                "testMessage",
                extras
            )

        val exception = AppFunctionException.fromPlatformExtensionsClass(platformException)

        assertThat(exception).isInstanceOf(AppFunctionDeniedException::class.java)
        assertThat(exception.errorCode).isEqualTo(AppFunctionException.ERROR_DENIED)
        assertThat(exception.errorMessage).isEqualTo("testMessage")
        assertThat(exception.extras.getString("testKey")).isEqualTo("testValue")
    }

    private fun assumeAppFunctionExtensionLibraryAvailable(): Boolean {
        try {
            Class.forName("com.android.extensions.appfunctions.AppFunctionManager")
            return true
        } catch (e: ClassNotFoundException) {
            throw AssumptionViolatedException("Unable to find AppFunction extension library", e)
        }
    }
}
