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

package androidx.savedstate.serialization.serializers

import androidx.savedstate.SavedState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A built-in serializer. This serializer is used as a marker to instruct
 * [androidx.savedstate.serialization.SavedStateEncoder] and
 * [androidx.savedstate.serialization.SavedStateDecoder] to use [SavedState]'s API to save/load a
 * serializable data of type [T].
 */
internal class BuiltInSerializer<T>(private val serialName: String) : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName)

    override fun deserialize(decoder: Decoder): T {
        error(
            "Cannot deserialize $serialName with '${decoder::class.simpleName}'." +
                " This serializer can only be used with SavedStateDecoder." +
                " Use 'decodeFromSavedState' instead."
        )
    }

    override fun serialize(encoder: Encoder, value: T) {
        error(
            "Cannot serialize $serialName with '${encoder::class.simpleName}'." +
                " This serializer can only be used with SavedStateEncoder." +
                " Use 'encodeToSavedState' instead."
        )
    }
}

/**
 * A serializer for [SavedState]. This serializer is used as a marker to instruct
 * [androidx.savedstate.serialization.SavedStateEncoder] and
 * [androidx.savedstate.serialization.SavedStateDecoder] to use [SavedState]'s API to save/load a
 * [SavedState].
 *
 * Note that this serializer should be used with
 * [androidx.savedstate.serialization.SavedStateEncoder] or
 * [androidx.savedstate.serialization.SavedStateDecoder] only. Using it with other Encoders/Decoders
 * may throw [IllegalStateException].
 *
 * @sample androidx.savedstate.savedStateSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
object SavedStateSerializer : KSerializer<SavedState> by BuiltInSerializer("SavedState")
