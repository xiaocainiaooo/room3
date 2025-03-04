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

@file:JvmName("SavedStateReaderKt")
@file:JvmMultifileClass
@file:Suppress("NOTHING_TO_INLINE")

package androidx.savedstate

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

@JvmInline
public actual value class SavedStateReader
@PublishedApi
internal actual constructor(
    @PublishedApi internal actual val source: SavedState,
) {

    public actual inline fun getBoolean(key: String): Boolean {
        return source.map[key] as? Boolean ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getBooleanOrElse(key: String, defaultValue: () -> Boolean): Boolean {
        return source.map[key] as? Boolean ?: defaultValue()
    }

    public actual inline fun getChar(key: String): Char {
        return source.map[key] as? Char ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getCharOrElse(key: String, defaultValue: () -> Char): Char {
        return source.map[key] as? Char ?: defaultValue()
    }

    public actual inline fun getCharSequence(key: String): CharSequence {
        return source.map[key] as? CharSequence ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getCharSequenceOrElse(
        key: String,
        defaultValue: () -> CharSequence
    ): CharSequence {
        return source.map[key] as? CharSequence ?: defaultValue()
    }

    public actual inline fun getDouble(key: String): Double {
        return source.map[key] as? Double ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getDoubleOrElse(key: String, defaultValue: () -> Double): Double {
        return source.map[key] as? Double ?: defaultValue()
    }

    public actual inline fun getFloat(key: String): Float {
        return source.map[key] as? Float ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getFloatOrElse(key: String, defaultValue: () -> Float): Float {
        return source.map[key] as? Float ?: defaultValue()
    }

    public actual inline fun getInt(key: String): Int {
        return source.map[key] as? Int ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getIntOrElse(key: String, defaultValue: () -> Int): Int {
        return source.map[key] as? Int ?: defaultValue()
    }

    public actual inline fun getLong(key: String): Long {
        return source.map[key] as? Long ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getLongOrElse(key: String, defaultValue: () -> Long): Long {
        return source.map[key] as? Long ?: defaultValue()
    }

    public actual inline fun getString(key: String): String {
        return source.map[key] as? String ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getStringOrElse(key: String, defaultValue: () -> String): String {
        return source.map[key] as? String ?: defaultValue()
    }

    public actual inline fun getCharSequenceList(key: String): List<CharSequence> {
        @Suppress("UNCHECKED_CAST")
        return source.map[key] as? List<CharSequence> ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getCharSequenceListOrElse(
        key: String,
        defaultValue: () -> List<CharSequence>
    ): List<CharSequence> {
        @Suppress("UNCHECKED_CAST") return source.map[key] as? List<CharSequence> ?: defaultValue()
    }

    public actual inline fun getIntList(key: String): List<Int> {
        @Suppress("UNCHECKED_CAST")
        return source.map[key] as? List<Int> ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getIntListOrElse(
        key: String,
        defaultValue: () -> List<Int>
    ): List<Int> {
        @Suppress("UNCHECKED_CAST") return source.map[key] as? List<Int> ?: defaultValue()
    }

    public actual inline fun getSavedStateList(key: String): List<SavedState> {
        @Suppress("UNCHECKED_CAST")
        return source.map[key] as? List<SavedState> ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getSavedStateListOrElse(
        key: String,
        defaultValue: () -> List<SavedState>
    ): List<SavedState> {
        @Suppress("UNCHECKED_CAST") return source.map[key] as? List<SavedState> ?: defaultValue()
    }

    public actual inline fun getStringList(key: String): List<String> {
        @Suppress("UNCHECKED_CAST")
        return source.map[key] as? List<String> ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getStringListOrElse(
        key: String,
        defaultValue: () -> List<String>
    ): List<String> {
        @Suppress("UNCHECKED_CAST") return source.map[key] as? List<String> ?: defaultValue()
    }

    public actual inline fun getCharArray(key: String): CharArray {
        return source.map[key] as? CharArray ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getCharArrayOrElse(
        key: String,
        defaultValue: () -> CharArray
    ): CharArray {
        return source.map[key] as? CharArray ?: defaultValue()
    }

    public actual inline fun getCharSequenceArray(key: String): Array<CharSequence> {
        @Suppress("UNCHECKED_CAST")
        return source.map[key] as? Array<CharSequence> ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getCharSequenceArrayOrElse(
        key: String,
        defaultValue: () -> Array<CharSequence>
    ): Array<CharSequence> {
        @Suppress("UNCHECKED_CAST") return source.map[key] as? Array<CharSequence> ?: defaultValue()
    }

    public actual inline fun getBooleanArray(key: String): BooleanArray {
        return source.map[key] as? BooleanArray ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getBooleanArrayOrElse(
        key: String,
        defaultValue: () -> BooleanArray
    ): BooleanArray {
        return source.map[key] as? BooleanArray ?: defaultValue()
    }

    public actual inline fun getDoubleArray(key: String): DoubleArray {
        return source.map[key] as? DoubleArray ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getDoubleArrayOrElse(
        key: String,
        defaultValue: () -> DoubleArray,
    ): DoubleArray {
        return source.map[key] as? DoubleArray ?: defaultValue()
    }

    public actual inline fun getFloatArray(key: String): FloatArray {
        return source.map[key] as? FloatArray ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getFloatArrayOrElse(
        key: String,
        defaultValue: () -> FloatArray
    ): FloatArray {
        return source.map[key] as? FloatArray ?: defaultValue()
    }

    public actual inline fun getIntArray(key: String): IntArray {
        return source.map[key] as? IntArray ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getIntArrayOrElse(
        key: String,
        defaultValue: () -> IntArray
    ): IntArray {
        return source.map[key] as? IntArray ?: defaultValue()
    }

    public actual inline fun getLongArray(key: String): LongArray {
        return source.map[key] as? LongArray ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getLongArrayOrElse(
        key: String,
        defaultValue: () -> LongArray
    ): LongArray {
        return source.map[key] as? LongArray ?: defaultValue()
    }

    @Suppress("UNCHECKED_CAST")
    public actual inline fun getSavedStateArray(key: String): Array<SavedState> {
        return source.map[key] as? Array<SavedState> ?: keyOrValueNotFoundError(key)
    }

    @Suppress("UNCHECKED_CAST")
    public actual inline fun getSavedStateArrayOrElse(
        key: String,
        defaultValue: () -> Array<SavedState>
    ): Array<SavedState> { return source.map[key] as? Array<SavedState>?: defaultValue()
    }

    @Suppress("UNCHECKED_CAST")
    public actual inline fun getStringArray(key: String): Array<String> {
        return source.map[key] as? Array<String> ?: keyOrValueNotFoundError(key)
    }

    @Suppress("UNCHECKED_CAST")
    public actual inline fun getStringArrayOrElse(
        key: String,
        defaultValue: () -> Array<String>
    ): Array<String> {
        return source.map[key] as? Array<String> ?: defaultValue()
    }

    public actual inline fun getSavedState(key: String): SavedState {
        return source.map[key] as? SavedState ?: keyOrValueNotFoundError(key)
    }

    public actual inline fun getSavedStateOrElse(
        key: String,
        defaultValue: () -> SavedState
    ): SavedState {
        return source.map[key] as? SavedState ?: defaultValue()
    }

    public actual inline fun size(): Int = source.map.size

    public actual inline fun isEmpty(): Boolean = source.map.isEmpty()

    public actual inline fun isNull(key: String): Boolean = contains(key) && source.map[key] == null

    public actual inline operator fun contains(key: String): Boolean = source.map.containsKey(key)

    public actual fun contentDeepEquals(other: SavedState): Boolean =
        source.contentDeepEquals(other)

    public actual fun contentDeepHashCode(): Int = source.contentDeepHashCode()

    public actual fun contentDeepToString(): String = source.contentDeepToString()

    public actual fun toMap(): Map<String, Any?> {
        return buildMap(capacity = source.map.size) {
            for (key in source.map.keys) {
                put(key, source.map[key])
            }
        }
    }
}

private fun SavedState.contentDeepEquals(other: SavedState): Boolean {
    if (this === other) return true
    if (this.map.size != other.map.size) return false

    for (k in this.map.keys) {
        val v1 = this.map[k]
        val v2 = other.map[k]

        when {
            v1 === v2 -> continue
            v1 == v2 -> continue
            v1 == null || v2 == null -> return false

            // container types
            v1 is SavedState && v2 is SavedState -> if (!v1.contentDeepEquals(v2)) return false
            v1 is Array<*> && v2 is Array<*> -> if (!v1.contentDeepEquals(v2)) return false

            // primitive arrays
            v1 is ByteArray && v2 is ByteArray -> if (!v1.contentEquals(v2)) return false
            v1 is ShortArray && v2 is ShortArray -> if (!v1.contentEquals(v2)) return false
            v1 is IntArray && v2 is IntArray -> if (!v1.contentEquals(v2)) return false
            v1 is LongArray && v2 is LongArray -> if (!v1.contentEquals(v2)) return false
            v1 is FloatArray && v2 is FloatArray -> if (!v1.contentEquals(v2)) return false
            v1 is DoubleArray && v2 is DoubleArray -> if (!v1.contentEquals(v2)) return false
            v1 is CharArray && v2 is CharArray -> if (!v1.contentEquals(v2)) return false
            v1 is BooleanArray && v2 is BooleanArray -> if (!v1.contentEquals(v2)) return false

            // if nothing else works
            else -> if (v1 != v2) return false
        }
    }
    return true
}

private fun SavedState.contentDeepHashCode(): Int {
    var result = 1

    for (k in this.map.keys) {
        val elementHash =
            when (val element = this.map[k]) {
                // container types
                is SavedState -> element.contentDeepHashCode()
                is Array<*> -> element.contentDeepHashCode()

                // primitive arrays
                is ByteArray -> element.contentHashCode()
                is ShortArray -> element.contentHashCode()
                is IntArray -> element.contentHashCode()
                is LongArray -> element.contentHashCode()
                is FloatArray -> element.contentHashCode()
                is DoubleArray -> element.contentHashCode()
                is CharArray -> element.contentHashCode()
                is BooleanArray -> element.contentHashCode()

                // if nothing else works
                else -> element.hashCode()
            }
        result = 31 * result + elementHash
    }

    return result
}

private fun SavedState.contentDeepToString(): String {
    // in order not to overflow Int.MAX_VALUE
    val length = map.size.coerceAtMost((Int.MAX_VALUE - 2) / 5) * 5 + 2
    return buildString(length) { contentDeepToStringInternal(this, mutableListOf()) }
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun SavedState.contentDeepToStringInternal(
    result: StringBuilder,
    processed: MutableList<SavedState>,
) {
    if (this in processed) {
        result.append("[...]")
        return
    }
    processed += this
    result.append('[')

    for ((i, k) in map.keys.withIndex()) {
        if (i != 0) {
            result.append(", ")
        }
        result.append("$k=")
        when (@Suppress("DEPRECATION") val element = map[k]) {
            null -> result.append("null")

            // container types
            is SavedState -> element.contentDeepToStringInternal(result, processed)
            is Array<*> -> result.append(element.contentDeepToString())

            // primitive arrays
            is ByteArray -> result.append(element.contentToString())
            is ShortArray -> result.append(element.contentToString())
            is IntArray -> result.append(element.contentToString())
            is LongArray -> result.append(element.contentToString())
            is FloatArray -> result.append(element.contentToString())
            is DoubleArray -> result.append(element.contentToString())
            is CharArray -> result.append(element.contentToString())
            is BooleanArray -> result.append(element.contentToString())

            // if nothing else works
            else -> result.append(element.toString())
        }
    }

    result.append(']')
    processed.removeAt(processed.lastIndex)
}
