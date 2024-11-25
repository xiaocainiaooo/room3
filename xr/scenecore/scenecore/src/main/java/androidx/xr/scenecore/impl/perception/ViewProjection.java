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

package androidx.xr.scenecore.impl.perception;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Contains the view projection state. <a
 * href="https://registry.khronos.org/OpenXR/specs/1.0/man/html/XrView.html">...</a>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ViewProjection {
    private final Pose pose;
    private final Fov fov;

    public ViewProjection(@NonNull Pose pose, @NonNull Fov fov) {
        this.pose = pose;
        this.fov = fov;
    }

    /** Returns the location and orientation of the camera/eye pose. */
    @NonNull
    public Pose getPose() {
        return pose;
    }

    /** Returns the four sides of the projection / view frustum. */
    @NonNull
    public Fov getFov() {
        return fov;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ViewProjection) {
            ViewProjection that = (ViewProjection) object;
            return this.pose.equals(that.pose) && this.fov.equals(that.fov);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return pose.hashCode() + fov.hashCode();
    }
}
