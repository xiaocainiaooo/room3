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

package androidx.savedstate.serialization

import androidx.savedstate.serialization.SavedStateConfig.Builder
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Configuration of the current [SavedStateConfig] configured with [SavedStateConfig.Builder].
 *
 * Can be used via [encodeToSavedState] and [decodeFromSavedState].
 *
 * Standalone configuration object cannot be used outside the encode and decode functions provided
 * by SavedState.
 *
 * Detailed description of each property is available in [SavedStateConfig.Builder] class.
 *
 * @see SavedStateConfig.Builder
 */
public class SavedStateConfig
private constructor(
    @PublishedApi internal val serializersModule: SerializersModule = EmptySerializersModule(),
) {
    /**
     * Builder of the [SavedStateConfig] instance provided by `SavedStateConfig { ... }` factory
     * function.
     */
    @Suppress(
        "MissingBuildMethod", // We do have an internal `build()` and the class is internal as well.
    )
    public class Builder internal constructor(config: SavedStateConfig) {

        /**
         * Module with contextual and polymorphic serializers to be used in the resulting
         * [SavedStateConfig] instance.
         *
         * @see SerializersModule
         * @see Contextual
         * @see Polymorphic
         */
        @get:Suppress("GetterOnBuilder") // Kotlin doesn't support `public set` with `private get`:
        // https://youtrack.jetbrains.com/issue/KT-3110
        @set:Suppress("SetterReturnsThis")
        public var serializersModule: SerializersModule = config.serializersModule

        internal fun build(): SavedStateConfig {
            return SavedStateConfig(serializersModule = serializersModule)
        }
    }

    public companion object {
        /**
         * The default instance of [SavedStateConfig] with default configuration.
         *
         * This configuration is used by [encodeToSavedState] and [decodeFromSavedState] unless an
         * alternative configuration is explicitly provided.
         *
         * @see encodeToSavedState
         * @see decodeFromSavedState
         */
        @JvmField public val DEFAULT: SavedStateConfig = SavedStateConfig()
    }
}

/**
 * Creates an instance of [SavedStateConfig] configured from the optionally given [from] and
 * adjusted with [builderAction].
 *
 * @sample androidx.savedstate.config
 * @param from An optional initial [SavedStateConfig] to start with. Defaults to
 *   [SavedStateConfig.DEFAULT].
 * @param builderAction A lambda function to configure the [Builder] for additional customization.
 * @return A new [SavedStateConfig] instance configured based on the provided parameters.
 */
@JvmOverloads
public fun SavedStateConfig(
    from: SavedStateConfig = SavedStateConfig.DEFAULT,
    builderAction: Builder.() -> Unit,
): SavedStateConfig {
    return Builder(from).apply(builderAction).build()
}
