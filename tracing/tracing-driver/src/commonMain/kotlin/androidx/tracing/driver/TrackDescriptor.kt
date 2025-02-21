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

package androidx.tracing.driver

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TRACK_DESCRIPTOR_TYPE_COUNTER: Int = 1
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TRACK_DESCRIPTOR_TYPE_THREAD: Int = 2
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TRACK_DESCRIPTOR_TYPE_PROCESS: Int = 3

public class TrackDescriptor(
    public var name: String,
    public var uuid: Long,
    public var parentUuid: Long,
    /**
     * One of [TRACK_DESCRIPTOR_TYPE_THREAD], [TRACK_DESCRIPTOR_TYPE_COUNTER],
     * [TRACK_DESCRIPTOR_TYPE_PROCESS]
     */
    public var type: Int,
    public var pid: Int,
    public var tid: Int,
)
