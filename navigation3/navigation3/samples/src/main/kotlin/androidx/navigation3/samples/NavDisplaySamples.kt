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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavLocalProvider
import androidx.navigation3.NavDisplay
import androidx.navigation3.NavEntry
import androidx.navigation3.SavedStateNavLocalProvider
import androidx.navigation3.entry
import androidx.navigation3.entryProvider

class ProfileViewModel : ViewModel() {
    val name = "no user"
}

@Sampled
@Composable
fun BaseNav() {
    val backStack = rememberMutableStateListOf(Profile)
    val showDialog = remember { mutableStateOf(false) }
    NavDisplay(
        backstack = backStack,
        localProviders = listOf(SavedStateNavLocalProvider, ViewModelStoreNavLocalProvider),
        onBack = { backStack.removeLast() },
        entryProvider =
            entryProvider({ NavEntry(Unit) { Text(text = "Invalid Key") } }) {
                entry<Profile>(
                    NavDisplay.transition(slideInHorizontally { it }, slideOutHorizontally { it })
                ) {
                    val viewModel = viewModel<ProfileViewModel>()
                    Profile(viewModel, { backStack.add(it) }) { backStack.removeLast() }
                }
                entry<Scrollable>(
                    NavDisplay.transition(slideInHorizontally { it }, slideOutHorizontally { it })
                ) {
                    Scrollable({ backStack.add(it) }) { backStack.removeLast() }
                }
                entry<DialogBase> {
                    DialogBase(onClick = { showDialog.value = true }) { backStack.removeLast() }
                }
                entry<Dashboard>(
                    NavDisplay.transition(slideInHorizontally { it }, slideOutHorizontally { it })
                ) { dashboardArgs ->
                    val userId = dashboardArgs.userId
                    Dashboard(userId, onBack = { backStack.removeLast() })
                }
            }
    )
    if (showDialog.value) {
        DialogContent(onDismissRequest = { showDialog.value = false })
    }
}
