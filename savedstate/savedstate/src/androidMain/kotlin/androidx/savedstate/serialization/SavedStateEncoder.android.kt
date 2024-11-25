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

import android.os.IBinder
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import androidx.savedstate.SavedState
import androidx.savedstate.serialization.serializers.CharSequenceArrayListSerializer
import androidx.savedstate.serialization.serializers.CharSequenceArraySerializer
import androidx.savedstate.serialization.serializers.CharSequenceSerializer
import androidx.savedstate.serialization.serializers.IBinderSerializer
import androidx.savedstate.serialization.serializers.JavaSerializableSerializer
import androidx.savedstate.serialization.serializers.ParcelableArrayListSerializer
import androidx.savedstate.serialization.serializers.ParcelableArraySerializer
import androidx.savedstate.serialization.serializers.ParcelableSerializer
import androidx.savedstate.serialization.serializers.SizeFSerializer
import androidx.savedstate.serialization.serializers.SizeSerializer
import androidx.savedstate.serialization.serializers.SparseParcelableArraySerializer
import androidx.savedstate.write
import java.io.Serializable
import kotlinx.serialization.SerializationStrategy

@Suppress("UNCHECKED_CAST")
internal actual fun <T> platformSpecificEncodeSerializableValue(
    savedState: SavedState,
    serializer: SerializationStrategy<T>,
    key: String,
    value: T
): Boolean {
    savedState.write {
        when (serializer.descriptor) {
            SizeSerializer.descriptor -> {
                putSize(key, value as Size)
            }
            SizeFSerializer.descriptor -> {
                putSizeF(key, value as SizeF)
            }
            CharSequenceSerializer.descriptor -> {
                putCharSequence(key, value as CharSequence)
            }
            JavaSerializableSerializer.descriptor -> {
                putJavaSerializable(key, value as Serializable)
            }
            ParcelableSerializer.descriptor -> {
                putParcelable(key, value as Parcelable)
            }
            IBinderSerializer.descriptor -> {
                putBinder(key, value as IBinder)
            }
            CharSequenceArraySerializer.descriptor -> {
                putCharSequenceArray(key, value as Array<CharSequence>)
            }
            ParcelableArraySerializer.descriptor -> {
                putParcelableArray(key, value as Array<Parcelable>)
            }
            CharSequenceArrayListSerializer.descriptor -> {
                putCharSequenceList(key, value as ArrayList<CharSequence>)
            }
            ParcelableArrayListSerializer.descriptor -> {
                putParcelableList(key, value as ArrayList<Parcelable>)
            }
            SparseParcelableArraySerializer.descriptor -> {
                putSparseParcelableArray(key, value as android.util.SparseArray<Parcelable>)
            }
            else -> return false
        }
    }
    return true
}
