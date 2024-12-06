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

package androidx.core.backported.fixes

/** List of all known issue reportable by [BackportedFixManager] */
internal enum class KnownIssue(public val id: Long, public val alias: Int) {
    // TODO b/381267367 - Add link to public list issues

    // keep-sorted start

    KI_350037023(350037023L, 1),
    KI_350037348(350037348L, 3),
    KI_372917199(372917199L, 2),

    // keep-sorted end

}
