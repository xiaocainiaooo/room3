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

package androidx.savedstate.serialization

import android.os.Parcelable
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.serialization.serializers.CharSequenceArrayListSerializer
import androidx.savedstate.serialization.serializers.CharSequenceArraySerializer
import androidx.savedstate.serialization.serializers.CharSequenceSerializer
import androidx.savedstate.serialization.serializers.IBinderSerializer
import androidx.savedstate.serialization.serializers.ParcelableArrayListSerializer
import androidx.savedstate.serialization.serializers.ParcelableArraySerializer
import androidx.savedstate.serialization.serializers.ParcelableSerializer
import androidx.savedstate.serialization.serializers.SerializableSerializer
import androidx.savedstate.serialization.serializers.SizeFSerializer
import androidx.savedstate.serialization.serializers.SizeSerializer
import androidx.savedstate.serialization.serializers.SparseParcelableArraySerializer
import kotlinx.serialization.DeserializationStrategy

@Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
internal actual fun <T> platformSpecificDecodeSerializableValue(
    savedState: SavedState,
    deserializer: DeserializationStrategy<T>,
    key: String
): T? {
    return savedState.read {
        when (deserializer.descriptor) {
            CharSequenceSerializer.descriptor -> getCharSequence(key)
            SizeSerializer.descriptor -> getSize(key)
            SizeFSerializer.descriptor -> getSizeF(key)
            SerializableSerializer.descriptor -> getJavaSerializable(key)
            ParcelableSerializer.descriptor -> getParcelable(key)
            IBinderSerializer.descriptor -> getBinder(key)
            CharSequenceArraySerializer.descriptor -> getCharSequenceArray(key)
            ParcelableArraySerializer.descriptor -> getParcelableArray<Parcelable>(key)
            CharSequenceArrayListSerializer.descriptor -> getCharSequenceList(key)
            ParcelableArrayListSerializer.descriptor -> getParcelableList<Parcelable>(key)
            SparseParcelableArraySerializer.descriptor -> getSparseParcelableArray<Parcelable>(key)
            else -> null
        }
            as T?
    }
}
