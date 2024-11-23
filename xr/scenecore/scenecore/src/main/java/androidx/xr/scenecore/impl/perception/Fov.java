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

import androidx.annotation.RestrictTo;

/**
 * Represents the field of view. <a
 * href="https://registry.khronos.org/OpenXR/specs/1.0/man/html/XrFovf.html">...</a>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class Fov {
    private final float angleLeft;
    private final float angleRight;
    private final float angleUp;
    private final float angleDown;

    public Fov(float angleLeft, float angleRight, float angleUp, float angleDown) {
        this.angleLeft = angleLeft;
        this.angleRight = angleRight;
        this.angleUp = angleUp;
        this.angleDown = angleDown;
    }

    /**
     * Returns the angle of the left side of the field of view. For a symmetric field of view, this
     * value is negative. *
     */
    public float getAngleLeft() {
        return angleLeft;
    }

    /** Returns the angle of the right part of the field of view. */
    public float getAngleRight() {
        return angleRight;
    }

    /** Returns the angle of the top part of the field of view. */
    public float getAngleUp() {
        return angleUp;
    }

    /**
     * Returns the angle of the bottom side of the field of view. For a symmetric field of view,
     * this value is negative. *
     */
    public float getAngleDown() {
        return angleDown;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Fov) {
            Fov that = (Fov) object;
            return this.angleLeft == that.angleLeft
                    && this.angleRight == that.angleRight
                    && this.angleUp == that.angleUp
                    && this.angleDown == that.angleDown;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Float.floatToIntBits(angleLeft);
        result = 31 * result + Float.floatToIntBits(angleRight);
        result = 31 * result + Float.floatToIntBits(angleUp);
        result = 31 * result + Float.floatToIntBits(angleDown);
        return result;
    }
}
