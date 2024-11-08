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

package androidx.navigation3.samples

import androidx.annotation.Sampled
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.NavDisplay
import androidx.navigation3.Record
import androidx.navigation3.rememberNavWrapperManager

@Sampled
@Composable
fun BasicNav() {
    val backStack = rememberMutableStateListOf(Profile)
    val manager = rememberNavWrapperManager(emptyList())
    NavDisplay(
        backstack = backStack,
        wrapperManager = manager,
        onBack = { backStack.removeLast() },
    ) { key ->
        when (key) {
            Profile -> {
                Record(Profile) { Profile({ backStack.add(it) }) { backStack.removeLast() } }
            }
            Scrollable -> {
                Record(Scrollable) { Scrollable({ backStack.add(it) }) { backStack.removeLast() } }
            }
            Dialog -> {
                Record(Dialog, featureMap = NavDisplay.isDialog(true)) { third ->
                    DialogContent { backStack.removeLast() }
                }
            }
            Dashboard -> {
                Record(Dashboard) { dashboardArgs ->
                    val userId = (dashboardArgs as Dashboard).userId
                    Dashboard(userId, onBack = { backStack.removeLast() })
                }
            }
            else -> {
                Record(Unit) { Text(text = "Invalid Key") }
            }
        }
    }
}
