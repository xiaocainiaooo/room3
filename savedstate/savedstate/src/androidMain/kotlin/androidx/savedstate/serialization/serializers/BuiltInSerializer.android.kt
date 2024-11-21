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

/**
 * A serializer for [android.util.Size]. This serializer is used as a marker to instruct
 * [androidx.savedstate.serialization.SavedStateEncoder] and
 * [androidx.savedstate.serialization.SavedStateDecoder] to use [SavedState]'s API to save/load a
 * [android.util.Size].
 *
 * Note that this serializer should be used with
 * [androidx.savedstate.serialization.SavedStateEncoder] or
 * [androidx.savedstate.serialization.SavedStateDecoder] only. Using it with other Encoders/Decoders
 * may throw [IllegalStateException].
 *
 * @sample androidx.savedstate.sizeSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
object SizeSerializer : KSerializer<android.util.Size> by BuiltInSerializer("Size")

/**
 * A serializer for [android.util.SizeF]. This serializer is used as a marker to instruct
 * [androidx.savedstate.serialization.SavedStateEncoder] and
 * [androidx.savedstate.serialization.SavedStateDecoder] to use [SavedState]'s API to save/load a
 * [android.util.SizeF].
 *
 * Note that this serializer should be used with
 * [androidx.savedstate.serialization.SavedStateEncoder] or
 * [androidx.savedstate.serialization.SavedStateDecoder] only. Using it with other Encoders/Decoders
 * may throw [IllegalStateException].
 *
 * @sample androidx.savedstate.sizeFSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
object SizeFSerializer : KSerializer<android.util.SizeF> by BuiltInSerializer("SizeF")

/**
 * A serializer for [CharSequence]. This serializer is used as a marker to instruct
 * [androidx.savedstate.serialization.SavedStateEncoder] and
 * [androidx.savedstate.serialization.SavedStateDecoder] to use [SavedState]'s API to save/load a
 * [CharSequence].
 *
 * Note that this serializer should be used with
 * [androidx.savedstate.serialization.SavedStateEncoder] or
 * [androidx.savedstate.serialization.SavedStateDecoder] only. Using it with other Encoders/Decoders
 * may throw [IllegalStateException].
 *
 * @sample androidx.savedstate.charSequenceSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
object CharSequenceSerializer : KSerializer<CharSequence> by BuiltInSerializer("CharSequence")

/**
 * A serializer for [java.io.Serializable]. This serializer is used as a marker to instruct
 * [androidx.savedstate.serialization.SavedStateEncoder] and
 * [androidx.savedstate.serialization.SavedStateDecoder] to use [SavedState]'s API to save/load a
 * [java.io.Serializable].
 *
 * Note that this serializer should be used with
 * [androidx.savedstate.serialization.SavedStateEncoder] or
 * [androidx.savedstate.serialization.SavedStateDecoder] only. Using it with other Encoders/Decoders
 * may throw [IllegalStateException].
 *
 * @sample androidx.savedstate.serializableSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
object SerializableSerializer :
    KSerializer<java.io.Serializable> by BuiltInSerializer("Serializable")

/**
 * A serializer for [java.io.Serializable]. This serializer is used as a marker to instruct
 * [androidx.savedstate.serialization.SavedStateEncoder] and
 * [androidx.savedstate.serialization.SavedStateDecoder] to use [SavedState]'s API to save/load a
 * [java.io.Serializable].
 *
 * Note that this serializer should be used with
 * [androidx.savedstate.serialization.SavedStateEncoder] or
 * [androidx.savedstate.serialization.SavedStateDecoder] only. Using it with other Encoders/Decoders
 * may throw [IllegalStateException].
 *
 * @sample androidx.savedstate.serializableSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
object ParcelableSerializer : KSerializer<android.os.Parcelable> by BuiltInSerializer("Parcelable")

/**
 * A serializer for [android.os.IBinder]. This serializer is used as a marker to instruct
 * [androidx.savedstate.serialization.SavedStateEncoder] and
 * [androidx.savedstate.serialization.SavedStateDecoder] to use [SavedState]'s API to save/load a
 * [android.os.IBinder].
 *
 * Note that this serializer should be used with
 * [androidx.savedstate.serialization.SavedStateEncoder] or
 * [androidx.savedstate.serialization.SavedStateDecoder] only. Using it with other Encoders/Decoders
 * may throw [IllegalStateException].
 *
 * @sample androidx.savedstate.iBinderSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
object IBinderSerializer : KSerializer<android.os.IBinder> by BuiltInSerializer("IBinder")

/**
 * A serializer for [Array<CharSequence>]. This serializer is used as a marker to instruct
 * [androidx.savedstate.serialization.SavedStateEncoder] and
 * [androidx.savedstate.serialization.SavedStateDecoder] to use [SavedState]'s API to save/load a
 * [Array<CharSequence>].
 *
 * Note that this serializer should be used with
 * [androidx.savedstate.serialization.SavedStateEncoder] or
 * [androidx.savedstate.serialization.SavedStateDecoder] only. Using it with other Encoders/Decoders
 * may throw [IllegalStateException].
 *
 * @sample androidx.savedstate.charSequenceArraySerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
object CharSequenceArraySerializer :
    KSerializer<Array<out CharSequence>> by BuiltInSerializer("CharSequenceArray")

/**
 * A serializer for [Array<android.os.Parcelable>]. This serializer is used as a marker to instruct
 * [androidx.savedstate.serialization.SavedStateEncoder] and
 * [androidx.savedstate.serialization.SavedStateDecoder] to use [SavedState]'s API to save/load a
 * [Array<android.os.Parcelable>].
 *
 * Note that this serializer should be used with
 * [androidx.savedstate.serialization.SavedStateEncoder] or
 * [androidx.savedstate.serialization.SavedStateDecoder] only. Using it with other Encoders/Decoders
 * may throw [IllegalStateException].
 *
 * @sample androidx.savedstate.parcelableArraySerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
object ParcelableArraySerializer :
    KSerializer<Array<out android.os.Parcelable>> by BuiltInSerializer("ParcelableArray")

/**
 * A serializer for [Array<CharSequence>]. This serializer is used as a marker to instruct
 * [androidx.savedstate.serialization.SavedStateEncoder] and
 * [androidx.savedstate.serialization.SavedStateDecoder] to use [SavedState]'s API to save/load a
 * [Array<CharSequence>].
 *
 * Note that this serializer should be used with
 * [androidx.savedstate.serialization.SavedStateEncoder] or
 * [androidx.savedstate.serialization.SavedStateDecoder] only. Using it with other Encoders/Decoders
 * may throw [IllegalStateException].
 *
 * @sample androidx.savedstate.charSequenceArrayListSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
object CharSequenceArrayListSerializer :
    KSerializer<ArrayList<out CharSequence>> by BuiltInSerializer("CharSequenceArrayList")

/**
 * A serializer for [ArrayList<android.os.Parcelable>]. This serializer is used as a marker to
 * instruct [androidx.savedstate.serialization.SavedStateEncoder] and
 * [androidx.savedstate.serialization.SavedStateDecoder] to use [SavedState]'s API to save/load a
 * [ArrayList<android.os.Parcelable>].
 *
 * Note that this serializer should be used with
 * [androidx.savedstate.serialization.SavedStateEncoder] or
 * [androidx.savedstate.serialization.SavedStateDecoder] only. Using it with other Encoders/Decoders
 * may throw [IllegalStateException].
 *
 * @sample androidx.savedstate.parcelableArrayListSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
object ParcelableArrayListSerializer :
    KSerializer<ArrayList<out android.os.Parcelable>> by BuiltInSerializer("ParcelableArrayList")

/**
 * A serializer for [android.util.SparseArray<android.os.Parcelable>]. This serializer is used as a
 * marker to instruct [androidx.savedstate.serialization.SavedStateEncoder] and
 * [androidx.savedstate.serialization.SavedStateDecoder] to use [SavedState]'s API to save/load a
 * [android.util.SparseArray<android.os.Parcelable>].
 *
 * Note that this serializer should be used with
 * [androidx.savedstate.serialization.SavedStateEncoder] or
 * [androidx.savedstate.serialization.SavedStateDecoder] only. Using it with other Encoders/Decoders
 * may throw [IllegalStateException].
 *
 * @sample androidx.savedstate.sparseParcelableArraySerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
object SparseParcelableArraySerializer :
    KSerializer<android.util.SparseArray<out android.os.Parcelable>> by BuiltInSerializer(
        "SparseParcelableArray"
    )
