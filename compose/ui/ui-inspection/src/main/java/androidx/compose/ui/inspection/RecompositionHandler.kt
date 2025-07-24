/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.inspection

import android.util.Log
import androidx.annotation.GuardedBy
import androidx.compose.runtime.Composer
import androidx.compose.ui.inspection.util.AnchorMap
import androidx.inspection.ArtTooling

private const val START_RESTART_GROUP = "startRestartGroup(I)Landroidx/compose/runtime/Composer;"
private const val SKIP_TO_GROUP_END = "skipToGroupEnd()V"

/** Detection of recompose counts and skips from installing runtime hooks. */
class RecompositionHandler(private val artTooling: ArtTooling, private val anchorMap: AnchorMap) {
    /** For each composable store the recomposition [count] and [skips]. */
    class Data(var count: Int, var skips: Int)

    private val lock = Any()
    @GuardedBy("lock") private var currentlyCollecting = false
    @GuardedBy("lock") private var hooksInstalled = false
    @GuardedBy("lock") private val counts = mutableMapOf<Any, Data>()

    fun changeCollectionMode(startCollecting: Boolean, keepCounts: Boolean) {
        synchronized(lock) {
            if (startCollecting != currentlyCollecting) {
                if (!hooksInstalled) {
                    installHooks()
                }
                currentlyCollecting = startCollecting
            }
            if (!keepCounts) {
                counts.clear()
            }
        }
    }

    fun getCounts(anchorId: Int): Data? {
        synchronized(lock) {
            return anchorMap[anchorId]?.let { counts[it] }
        }
    }

    private fun composerImplementationClasses(): List<Class<*>> {
        val baseName = Composer::class.java.name.let { it.substring(0..it.lastIndexOf('.')) }
        val classes = mutableListOf<Class<*>>()
        try {
            classes.add(Class.forName(baseName + "ComposerImpl"))
        } catch (_: Throwable) {}
        try {
            classes.add(Class.forName(baseName + "GapComposer"))
        } catch (_: Throwable) {}
        try {
            classes.add(Class.forName(baseName + "LinkComposer"))
        } catch (_: Throwable) {}
        if (classes.isEmpty()) {
            Log.w(LOG_TAG, "Could not install recomposition hooks")
        }
        return classes
    }

    /**
     * We install 2 hooks:
     * - exit hook for ComposerImpl.startRestartGroup gives us the anchor of the composable
     * - entry hook for ComposerImpl.skipToGroupEnd converts a recompose count to a skip count.
     */
    private fun installHooks() {
        composerImplementationClasses().forEach { composerImpl ->
            artTooling.registerExitHook(composerImpl, START_RESTART_GROUP) { composer: Composer ->
                synchronized(lock) {
                    if (currentlyCollecting) {
                        composer.recomposeScopeIdentity?.let { anchor ->
                            val data = counts.getOrPut(anchor) { Data(0, 0) }
                            data.count++
                        }
                    }
                    composer
                }
            }

            artTooling.registerEntryHook(composerImpl, SKIP_TO_GROUP_END) { obj, _ ->
                synchronized(lock) {
                    if (currentlyCollecting) {
                        val composer = obj as? Composer
                        composer?.recomposeScopeIdentity?.let { anchor ->
                            counts[anchor]?.let {
                                it.count--
                                it.skips++
                            }
                        }
                    }
                }
            }
        }
        hooksInstalled = true
    }
}
