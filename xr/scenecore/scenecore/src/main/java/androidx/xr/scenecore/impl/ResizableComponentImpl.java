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

import android.util.Log;

import androidx.xr.extensions.Consumer;
import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.extensions.node.ReformEvent;
import androidx.xr.extensions.node.ReformOptions;
import androidx.xr.extensions.node.Vec3;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.ResizableComponent;
import androidx.xr.scenecore.JxrPlatformAdapter.ResizeEvent;
import androidx.xr.scenecore.JxrPlatformAdapter.ResizeEventListener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/** Implementation of ResizableComponent. */
@SuppressWarnings({"BanConcurrentHashMap"})
class ResizableComponentImpl implements ResizableComponent {

    private static final String TAG = "ResizableComponentImpl";

    private final XrExtensions extensions;
    private final ExecutorService executor;
    private final ConcurrentHashMap<ResizeEventListener, Executor> resizeEventListenerMap =
            new ConcurrentHashMap<>();
    // Visible for testing.
    Consumer<ReformEvent> reformEventConsumer;
    private Entity entity;
    private Dimensions currentSize;
    private Dimensions minSize;
    private Dimensions maxSize;
    private float fixedAspectRatio = 0.0f;

    public ResizableComponentImpl(
            ExecutorService executor,
            XrExtensions extensions,
            Dimensions minSize,
            Dimensions maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.extensions = extensions;
        this.executor = executor;
    }

    @Override
    public boolean onAttach(Entity entity) {
        if (this.entity != null) {
            Log.e(TAG, "Already attached to entity " + this.entity);
            return false;
        }
        this.entity = entity;
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        reformOptions.setEnabledReform(
                reformOptions.getEnabledReform() | ReformOptions.ALLOW_RESIZE);

        // Update the current size if it's not set.
        // TODO: b/348037292 - Remove this special case for PanelEntityImpl.
        if (entity instanceof PanelEntityImpl && currentSize == null) {
            currentSize = ((PanelEntityImpl) entity).getSize();
            // TODO: b/350563642 - Add checks that size is within minsize and maxsize.
        }
        if (currentSize != null) {
            reformOptions.setCurrentSize(
                    new Vec3(currentSize.width, currentSize.height, currentSize.depth));
        }
        reformOptions.setMinimumSize(new Vec3(minSize.width, minSize.height, minSize.depth));
        reformOptions.setMaximumSize(new Vec3(maxSize.width, maxSize.height, maxSize.depth));
        reformOptions.setFixedAspectRatio(fixedAspectRatio);
        ((AndroidXrEntity) entity).updateReformOptions();
        if (reformEventConsumer != null) {
            ((AndroidXrEntity) entity).addReformEventConsumer(reformEventConsumer, executor);
        }
        return true;
    }

    @Override
    public void onDetach(Entity entity) {
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        reformOptions.setEnabledReform(
                reformOptions.getEnabledReform() & ~ReformOptions.ALLOW_RESIZE);
        ((AndroidXrEntity) entity).updateReformOptions();
        if (reformEventConsumer != null) {
            ((AndroidXrEntity) entity).removeReformEventConsumer(reformEventConsumer);
        }
        this.entity = null;
    }

    @Override
    public void setSize(Dimensions size) {
        // TODO: b/350821054 - Implement synchronization policy around Entity/Component updates.
        currentSize = size;
        if (entity == null) {
            Log.e(TAG, "This component isn't attached to an entity.");
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        reformOptions.setCurrentSize(new Vec3(size.width, size.height, size.depth));
        ((AndroidXrEntity) entity).updateReformOptions();
    }

    @Override
    public void setMinimumSize(Dimensions minSize) {
        this.minSize = minSize;
        if (entity == null) {
            Log.e(TAG, "This component isn't attached to an entity.");
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        reformOptions.setMinimumSize(new Vec3(minSize.width, minSize.height, minSize.depth));
        ((AndroidXrEntity) entity).updateReformOptions();
    }

    @Override
    public void setMaximumSize(Dimensions maxSize) {
        this.maxSize = maxSize;
        if (entity == null) {
            Log.e(TAG, "This component isn't attached to an entity.");
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        reformOptions.setMaximumSize(new Vec3(maxSize.width, maxSize.height, maxSize.depth));
        ((AndroidXrEntity) entity).updateReformOptions();
    }

    @Override
    public void setFixedAspectRatio(float fixedAspectRatio) {
        this.fixedAspectRatio = fixedAspectRatio;
        if (entity == null) {
            Log.e(TAG, "This component isn't attached to an entity.");
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        reformOptions.setFixedAspectRatio(fixedAspectRatio);
        ((AndroidXrEntity) entity).updateReformOptions();
    }

    @Override
    public void addResizeEventListener(
            Executor resizeExecutor, ResizeEventListener resizeEventListener) {
        resizeEventListenerMap.put(resizeEventListener, resizeExecutor);
        if (reformEventConsumer != null) {
            return;
        }
        reformEventConsumer =
                reformEvent -> {
                    if (reformEvent.getType() != ReformEvent.REFORM_TYPE_RESIZE) {
                        return;
                    }
                    // Set the alpha to 0 when the resize starts and restore when resize ends, to
                    // hide the
                    // entity content while it's being resized.
                    switch (reformEvent.getState()) {
                        case ReformEvent.REFORM_STATE_START:
                            try (NodeTransaction transaction = extensions.createNodeTransaction()) {
                                transaction
                                        .setAlpha(((AndroidXrEntity) entity).getNode(), 0f)
                                        .apply();
                            }
                            break;
                        case ReformEvent.REFORM_STATE_END:
                            entity.setAlpha(entity.getAlpha());
                            break;
                        default:
                            break;
                    }
                    Dimensions newSize =
                            new Dimensions(
                                    reformEvent.getProposedSize().x,
                                    reformEvent.getProposedSize().y,
                                    reformEvent.getProposedSize().z);
                    // Update the resize affordance size.
                    setSize(newSize);
                    resizeEventListenerMap.forEach(
                            (listener, listenerExecutor) ->
                                    listenerExecutor.execute(
                                            () ->
                                                    listener.onResizeEvent(
                                                            new ResizeEvent(
                                                                    RuntimeUtils
                                                                            .getResizeEventState(
                                                                                    reformEvent
                                                                                            .getState()),
                                                                    newSize))));
                };
        if (entity == null) {
            Log.e(TAG, "This component isn't attached to an entity.");
            return;
        }
        ((AndroidXrEntity) entity).addReformEventConsumer(reformEventConsumer, executor);
    }

    @Override
    public void removeResizeEventListener(ResizeEventListener resizeEventListener) {
        resizeEventListenerMap.remove(resizeEventListener);
        if (resizeEventListenerMap.isEmpty()) {
            ((AndroidXrEntity) entity).removeReformEventConsumer(reformEventConsumer);
        }
    }
}
