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

package androidx.xr.scenecore.impl;

import android.view.Surface;

import androidx.xr.extensions.XrExtensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.StereoSurfaceEntity;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of a RealityCore StereoSurfaceEntityImpl.
 *
 * <p>Unimplemented, this requires split engine.
 */
final class StereoSurfaceEntityImpl extends AndroidXrEntity implements StereoSurfaceEntity {
    @StereoMode private int stereoMode;

    public StereoSurfaceEntityImpl(
            Entity parentEntity,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        super(extensions.createNode(), extensions, entityManager, executor);
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void setStereoMode(@StereoMode int mode) {
        stereoMode = mode;
    }

    @Override
    public int getStereoMode() {
        return stereoMode;
    }

    @Override
    public void setDimensions(Dimensions dimensions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dimensions getDimensions() {
        return new Dimensions(0.0f, 0.0f, 0.0f);
    }

    @Override
    public Surface getSurface() {
        throw new UnsupportedOperationException();
    }
}
