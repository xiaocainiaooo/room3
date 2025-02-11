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

import android.app.appsearch.GenericDocument
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import com.android.extensions.appfunctions.AppFunctionException
import com.android.extensions.appfunctions.AppFunctionService
import com.android.extensions.appfunctions.ExecuteAppFunctionRequest
import com.android.extensions.appfunctions.ExecuteAppFunctionResponse

@RequiresApi(Build.VERSION_CODES.S)
class TestAppFunctionService : AppFunctionService() {
    override fun onExecuteFunction(
        request: ExecuteAppFunctionRequest,
        callingPackage: String,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<ExecuteAppFunctionResponse?, AppFunctionException?>
    ) {
        return when (request.functionIdentifier) {
            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED -> {
                callback.onResult(
                    ExecuteAppFunctionResponse(
                        GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "")
                            .setPropertyString("testResult", "result")
                            .build()
                    )
                )
            }
            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_FAIL -> {
                callback.onError(
                    AppFunctionException(
                        AppFunctionException.ERROR_INVALID_ARGUMENT,
                        "error message"
                    )
                )
            }
            else -> throw IllegalArgumentException("Unknown function id")
        }
    }
}
