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

package androidx.camera.core;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines the valid states for Low Light Boost, as returned by
 * {@link CameraInfo#getLowLightBoostState()}.
 *
 * <p>These states indicate whether the camera device supports low light boost and if it is
 * currently active.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LowLightBoostState {
    /** Low-light boost is off. */
    public static final int OFF = -1;
    /** Low-light boost is on but inactive. */
    public static final int INACTIVE = 0;
    /** Low-light boost is on and active. */
    public static final int ACTIVE = 1;

    private LowLightBoostState() {
    }

    /**
     */
    @IntDef({OFF, INACTIVE, ACTIVE})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface State {
    }
}
