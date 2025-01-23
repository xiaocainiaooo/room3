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

import androidx.collection.MutableObjectIntMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Wraps the content of a [NavEntry] with a [SaveableStateHolder.SaveableStateProvider] to ensure
 * that calls to [rememberSaveable] within the content work properly and that state can be saved.
 *
 * This [NavLocalProvider] is the only one that is **required** as saving state is considered a
 * non-optional feature.
 */
public class SaveableStateNavLocalProvider : NavLocalProvider {

    @Composable
    override fun ProvideToBackStack(backStack: List<Any>, content: @Composable () -> Unit) {
        val localInfo = remember { SaveableStateNavLocalInfo() }
        DisposableEffect(key1 = backStack) {
            localInfo.refCount.clear()
            onDispose {}
        }

        localInfo.savedStateHolder = rememberSaveableStateHolder()
        localInfo.backstackSize = backStack.size
        backStack.forEach { key ->
            DisposableEffect(key1 = key) {
                localInfo.refCount[key] = localInfo.refCount.getOrDefault(key, 0).plus(1)
                onDispose {
                    if (localInfo.refCount[key] == 0) {
                        localInfo.savedStateHolder!!.removeState(key)
                    } else {
                        localInfo.refCount[key] =
                            localInfo.refCount
                                .getOrElse(key) {
                                    error(
                                        "Attempting to incorrectly dispose of backstack state in " +
                                            "SaveableStateNavLocalProvider"
                                    )
                                }
                                .minus(1)
                    }
                }
            }
        }

        CompositionLocalProvider(LocalSaveableStateNavLocalInfo provides localInfo) {
            content.invoke()
        }
    }

    @Composable
    public override fun <T : Any> ProvideToEntry(entry: NavEntry<T>) {
        val localInfo = LocalSaveableStateNavLocalInfo.current
        val key = entry.key
        DisposableEffect(key1 = key) {
            localInfo.refCount[key] = localInfo.refCount.getOrDefault(key, 0).plus(1)
            onDispose {
                // We need to check to make sure that the refcount has been cleared here because
                // when we are using animations, if the entire back stack is changed, we will
                // execute the onDispose above that clears all of the counts before we finish the
                // transition and run this onDispose so our count will already be gone and we
                // should just remove the state.
                if (!localInfo.refCount.contains(key) || localInfo.refCount[key] == 0) {
                    localInfo.savedStateHolder?.removeState(key)
                } else {
                    localInfo.refCount[key] =
                        localInfo.refCount
                            .getOrElse(key) {
                                error(
                                    "Attempting to incorrectly dispose of state associated with " +
                                        "key $key in SaveableStateNavLocalProvider"
                                )
                            }
                            .minus(1)
                }
            }
        }

        val id: Int = rememberSaveable(key) { key.hashCode() + localInfo.backstackSize }
        localInfo.savedStateHolder?.SaveableStateProvider(id) { entry.content.invoke(key) }
    }
}

internal val LocalSaveableStateNavLocalInfo =
    staticCompositionLocalOf<SaveableStateNavLocalInfo> {
        error(
            "CompositionLocal LocalSaveableStateNavLocalInfo not present. You must call " +
                "ProvideToBackStack before calling ProvideToEntry."
        )
    }

internal class SaveableStateNavLocalInfo {
    internal var savedStateHolder: SaveableStateHolder? = null
    internal val refCount: MutableObjectIntMap<Any> = MutableObjectIntMap()
    internal var backstackSize = 0
}
