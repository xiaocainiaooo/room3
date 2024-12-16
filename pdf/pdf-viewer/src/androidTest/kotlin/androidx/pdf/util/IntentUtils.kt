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

package androidx.pdf.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Waits for the intent to be fired by polling repeatedly. This is useful when checking for an
 * intent that may take some time to be triggered. The function retries every 100 ms until the
 * intent is captured or the [timeoutMillis] duration has passed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun waitForIntent(timeoutMillis: Long = 1000, checkIntent: () -> Unit) {
    withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(timeoutMillis) {
            var intentCaptured = false
            while (!intentCaptured) {
                try {
                    checkIntent()
                    intentCaptured = true
                } catch (e: AssertionError) {
                    delay(100)
                }
            }
        }
    }
}
