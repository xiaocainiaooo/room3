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

package androidx.lifecycle.viewmodel.testing

import androidx.kruth.assertThat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.savedState
import kotlin.test.Test

internal class ViewModelScenarioTest : RobolectricTest() {

    @Test
    fun viewModel_createsInstance() {
        val scenario = viewModelScenario { TestViewModel() }

        val actualModel = scenario.viewModel

        assertThat(actualModel).isNotNull()
        assertThat(actualModel).isInstanceOf<TestViewModel>()
    }

    @Test
    fun viewModel_whenNotCleared_returnsSameInstance() {
        val scenario = viewModelScenario { TestViewModel() }

        val actualModel1 = scenario.viewModel
        val actualModel2 = scenario.viewModel

        assertThat(actualModel1).isEqualTo(actualModel2)
    }

    @Test
    fun viewModel_whenCleared_returnsNewInstance() {
        val scenario = viewModelScenario { TestViewModel() }

        val actualModel1 = scenario.viewModel
        scenario.close()
        val actualModel2 = scenario.viewModel

        assertThat(actualModel1).isNotEqualTo(actualModel2)
    }

    @Test
    fun viewModel_whenNotCleared_usesCustomCreationExtras() {
        val expectedExtras = MutableCreationExtras().apply { this[CREATION_EXTRAS_KEY] = "value" }
        val scenario = viewModelScenario(expectedExtras) { TestViewModel(extras = this) }

        val actualExtras = scenario.viewModel.extras

        assertThat(actualExtras[CREATION_EXTRAS_KEY]).isEqualTo(expectedExtras[CREATION_EXTRAS_KEY])
    }

    @Test
    fun viewModel_whenCleared_reusesCustomCreationExtras() {
        val expectedExtras = MutableCreationExtras().apply { this[CREATION_EXTRAS_KEY] = "value" }
        val scenario = viewModelScenario(expectedExtras) { TestViewModel(extras = this) }

        val actualExtras1 = scenario.viewModel.extras
        scenario.close()
        val actualExtras2 = scenario.viewModel.extras

        assertThat(actualExtras1[CREATION_EXTRAS_KEY]).isEqualTo(actualExtras2[CREATION_EXTRAS_KEY])
    }

    @Test
    fun viewModel_whenCleared_clearsViewModel() {
        val scenario = viewModelScenario { TestViewModel() }

        val actualModel1 = scenario.viewModel
        scenario.close()
        val actualModel2 = scenario.viewModel

        assertThat(actualModel1.onClearedCount).isEqualTo(expected = 1)
        assertThat(actualModel2.onClearedCount).isEqualTo(expected = 0)
    }

    @Test
    fun createSavedStateHandle_withDefaultExtras() {
        val scenario = viewModelScenario { TestViewModel(handle = createSavedStateHandle()) }

        // assert `.viewModel` does not throw.
        assertThat(scenario.viewModel.handle).isNotNull()
    }

    @Test
    fun createSavedStateHandle_withInitialExtras() {
        val defaultArgs = savedState { putString("key", "value") }
        val creationExtras = DefaultCreationExtras(defaultArgs)
        val scenario =
            viewModelScenario(creationExtras) { TestViewModel(handle = createSavedStateHandle()) }

        assertThat(scenario.viewModel.handle.get<String>("key")).isEqualTo("value")
    }

    private val CREATION_EXTRAS_KEY = CreationExtras.Key<String>()

    private class TestViewModel(
        val handle: SavedStateHandle = SavedStateHandle(),
        val extras: CreationExtras = CreationExtras.Empty,
    ) : ViewModel() {

        var onClearedCount = 0
            private set

        override fun onCleared() {
            onClearedCount++
        }
    }
}
