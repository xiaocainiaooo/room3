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

import androidx.annotation.NonNull;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.InputEventListener;
import androidx.xr.scenecore.JxrPlatformAdapter.PointerCaptureComponent;
import androidx.xr.scenecore.JxrPlatformAdapter.PointerCaptureComponent.StateListener;

import java.util.concurrent.Executor;

/** Implementation of PointerCaptureComponent. */
final class PointerCaptureComponentImpl implements PointerCaptureComponent {

    private final Executor executor;
    private final StateListener stateListener;
    private final InputEventListener inputListener;

    private AndroidXrEntity attachedEntity;

    public PointerCaptureComponentImpl(
            @NonNull Executor executor,
            @NonNull StateListener stateListener,
            @NonNull InputEventListener inputListener) {
        this.executor = executor;
        this.stateListener = stateListener;
        this.inputListener = inputListener;
    }

    @Override
    public boolean onAttach(Entity entity) {
        if (!(entity instanceof AndroidXrEntity) || attachedEntity != null) {
            return false;
        }

        attachedEntity = (AndroidXrEntity) entity;
        return attachedEntity.requestPointerCapture(executor, inputListener, stateListener);
    }

    @Override
    public void onDetach(Entity entity) {
        attachedEntity.stopPointerCapture();
        attachedEntity = null;
    }
}
