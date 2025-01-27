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

@file:JvmName("SessionExt")
@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialCapabilities as RtSpatialCapabilities
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executor
import java.util.function.Consumer

private val spatialCapabilitiesListeners:
    ConcurrentMap<Consumer<SpatialCapabilities>, Consumer<RtSpatialCapabilities>> =
    ConcurrentHashMap()

/**
 * Returns the current [SpatialCapabilities] of the Session. The set of capabilities can change
 * within a session. The returned object will not update if the capabilities change; this method
 * should be called again to get the latest set of capabilities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun Session.getSpatialCapabilities(): SpatialCapabilities =
    this.platformAdapter.spatialCapabilities.toSpatialCapabilities()

/**
 * Adds the given [Consumer] as a listener to be invoked when this Session's current
 * [SpatialCapabilities] change. [Consumer#accept(SpatialCapabilities)] will be invoked on the main
 * thread.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun Session.addSpatialCapabilitiesChangedListener(
    listener: Consumer<SpatialCapabilities>
): Unit = addSpatialCapabilitiesChangedListener(HandlerExecutor.mainThreadExecutor, listener)

/**
 * Adds the given [Consumer] as a listener to be invoked when this Session's current
 * [SpatialCapabilities] change. [Consumer#accept(SpatialCapabilities)] will be invoked on the given
 * callbackExecutor, or the main thread if the callbackExecutor is null (default).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun Session.addSpatialCapabilitiesChangedListener(
    callbackExecutor: Executor,
    listener: Consumer<SpatialCapabilities>,
): Unit {
    // wrap the client's listener in a callback that receives & converts the platformAdapter
    // SpatialCapabilities type.
    val rtListener: Consumer<RtSpatialCapabilities> =
        Consumer<RtSpatialCapabilities> { rtCaps: RtSpatialCapabilities ->
            listener.accept(rtCaps.toSpatialCapabilities())
        }
    spatialCapabilitiesListeners.compute(
        listener,
        { _, _ ->
            platformAdapter.addSpatialCapabilitiesChangedListener(callbackExecutor, rtListener)
            rtListener
        },
    )
}

/**
 * Releases the given [Consumer] from receiving updates when the Session's [SpatialCapabilities]
 * change.
 */
@Suppress("PairedRegistration") // The corresponding remove method does not accept an Executor
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun Session.removeSpatialCapabilitiesChangedListener(
    listener: Consumer<SpatialCapabilities>
): Unit {
    spatialCapabilitiesListeners.computeIfPresent(
        listener,
        { _, rtListener ->
            platformAdapter.removeSpatialCapabilitiesChangedListener(rtListener)
            null
        },
    )
}
