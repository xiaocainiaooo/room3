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

/** A translation and rotation of an object (e.g. Trackable, Anchor). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class Pose {
    private float tx;
    private float ty;
    private float tz;
    private float qx;
    private float qy;
    private float qz;
    private float qw;

    public Pose(float tx, float ty, float tz, float qx, float qy, float qz, float qw) {
        this.tx = tx;
        this.ty = ty;
        this.tz = tz;
        this.qx = qx;
        this.qy = qy;
        this.qz = qz;
        this.qw = qw;
    }

    // A pose at zero position and orientation.
    @NonNull
    public static Pose identity() {
        return new Pose(0, 0, 0, 0, 0, 0, 1);
    }

    public void updateTranslation(float tx, float ty, float tz) {
        this.tx = tx;
        this.ty = ty;
        this.tz = tz;
    }

    public void updateRotation(float qx, float qy, float qz, float qw) {
        this.qx = qx;
        this.qy = qy;
        this.qz = qz;
        this.qw = qw;
    }

    /** Returns the X components of this pose's translation. */
    public float tx() {
        return tx;
    }

    /** Returns the Y components of this pose's translation. */
    public float ty() {
        return ty;
    }

    /** Returns the Z components of this pose's translation. */
    public float tz() {
        return tz;
    }

    /** Returns the X component of this pose's rotation quaternion. */
    public float qx() {
        return qx;
    }

    /** Returns the Y component of this pose's rotation quaternion. */
    public float qy() {
        return qy;
    }

    /** Returns the Z component of this pose's rotation quaternion. */
    public float qz() {
        return qz;
    }

    /** Returns the W component of this pose's rotation quaternion. */
    public float qw() {
        return qw;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Pose) {
            Pose that = (Pose) object;
            return this.tx == that.tx
                    && this.ty == that.ty
                    && this.tz == that.tz
                    && this.qx == that.qx
                    && this.qy == that.qy
                    && this.qz == that.qz
                    && this.qw == that.qw;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Float.floatToIntBits(tx);
        result = 31 * result + Float.floatToIntBits(ty);
        result = 31 * result + Float.floatToIntBits(tz);
        result = 31 * result + Float.floatToIntBits(qx);
        result = 31 * result + Float.floatToIntBits(qy);
        result = 31 * result + Float.floatToIntBits(qz);
        result = 31 * result + Float.floatToIntBits(qw);
        return result;
    }
}
