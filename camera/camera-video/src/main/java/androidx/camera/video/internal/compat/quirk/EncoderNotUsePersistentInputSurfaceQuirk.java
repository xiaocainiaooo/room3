/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video.internal.compat.quirk;

import android.media.MediaCodec;
import android.os.Build;

import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 *     Bug Id: N/A
 *     Description: Quirk denotes that the encoder should create new input surface for every
 *                  encoding instead of using {@link MediaCodec#createPersistentInputSurface()}.
 *                  , which creates a reusable surface for multiple encodings and is the
 *                  suggested approach after API 23. So for devices with API 21 and 22, a new
 *                  surface has to be created for every encoding instead.
 *     Device(s): All API 21 and 22 devices
 */
public class EncoderNotUsePersistentInputSurfaceQuirk implements Quirk {

    static boolean load() {
        return Build.VERSION.SDK_INT <= 22;
    }
}
