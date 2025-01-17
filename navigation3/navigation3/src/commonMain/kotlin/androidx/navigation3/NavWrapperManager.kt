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

package androidx.navigation3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Creates a [NavLocalProvider]. */
@Composable
public fun rememberNavWrapperManager(navLocalProviders: List<NavLocalProvider>): NavWrapperManager {
    return remember { NavWrapperManager(navLocalProviders) }
}

/**
 * Class that manages all of the provided [NavLocalProvider]. It is responsible for executing the
 * functions provided by each [NavLocalProvider] appropriately.
 *
 * Note: the order in which the [NavLocalProvider]s are added to the list determines their scope,
 * i.e. a [NavLocalProvider] added earlier in a list has its data available to those added later.
 *
 * @param navLocalProviders the [NavLocalProvider]s that are providing data to the content
 */
public class NavWrapperManager(navLocalProviders: List<NavLocalProvider> = emptyList()) {
    /**
     * Final list of wrappers. This always adds a [SaveableStateNavLocalProvider] by default, as it
     * is required. It then filters out any duplicates to ensure there is always one instance of any
     * wrapper at a given time.
     */
    private val finalWrappers =
        (navLocalProviders + listOf(SaveableStateNavLocalProvider())).distinct()

    /**
     * Calls the [NavLocalProvider.ProvideToBackStack] functions on each wrapper
     *
     * This function is called by the [NavDisplay](reference/androidx/navigation/NavDisplay) and
     * should not be called directly.
     */
    @Composable
    public fun PrepareBackStack(backStack: List<Any>) {
        finalWrappers.distinct().forEach { it.ProvideToBackStack(backStack = backStack) }
    }

    /**
     * Calls the [NavLocalProvider.ProvideToEntry] functions on each wrapper.
     *
     * This function is called by the [NavDisplay](reference/androidx/navigation/NavDisplay) and
     * should not be called directly.
     */
    @Composable
    public fun <T : Any> ContentForEntry(entry: NavEntry<T>) {
        val key = entry.key
        finalWrappers
            .distinct()
            .foldRight(entry.content) { wrapper, contentLambda ->
                { wrapper.ProvideToEntry(NavEntry(key, entry.featureMap, content = contentLambda)) }
            }
            .invoke(key)
    }
}
