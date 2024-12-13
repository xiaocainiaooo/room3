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
import androidx.xr.extensions.node.InputEvent;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.extensions.node.ReformEvent;
import androidx.xr.extensions.node.ReformOptions;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.InputEventListener;
import androidx.xr.scenecore.JxrPlatformAdapter.PointerCaptureComponent;
import androidx.xr.scenecore.common.BaseEntity;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of a RealityCore Entity that wraps an android XR extension Node.
 *
 * <p>This should not be created on its own but should be inherited by objects that need to wrap an
 * Android extension node.
 */
@SuppressWarnings({"BanSynchronizedMethods", "BanConcurrentHashMap"})
abstract class AndroidXrEntity extends BaseEntity implements Entity {

    protected final Node node;
    protected final XrExtensions extensions;
    protected final ScheduledExecutorService executor;
    // Visible for testing
    final ConcurrentHashMap<InputEventListener, Executor> inputEventListenerMap =
            new ConcurrentHashMap<>();
    Optional<InputEventListener> pointerCaptureInputEventListener = Optional.empty();
    Optional<Executor> pointerCaptureExecutor = Optional.empty();
    final ConcurrentHashMap<Consumer<ReformEvent>, Executor> reformEventConsumerMap =
            new ConcurrentHashMap<>();
    private final EntityManager entityManager;
    private ReformOptions reformOptions;

    AndroidXrEntity(
            Node node,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        this.node = node;
        this.extensions = extensions;
        this.entityManager = entityManager;
        this.executor = executor;
        entityManager.setEntityForNode(node, this);
    }

    @Override
    public void setPose(Pose pose) {
        // TODO: b/321268237 - Minimize the number of node transactions
        super.setPose(pose);

        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction
                    .setPosition(
                            node,
                            pose.getTranslation().getX(),
                            pose.getTranslation().getY(),
                            pose.getTranslation().getZ())
                    .setOrientation(
                            node,
                            pose.getRotation().getX(),
                            pose.getRotation().getY(),
                            pose.getRotation().getZ(),
                            pose.getRotation().getW())
                    .apply();
        }
    }

    @Override
    public void setScale(Vector3 scale) {
        super.setScale(scale);
        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction.setScale(node, scale.getX(), scale.getY(), scale.getZ()).apply();
        }
    }

    /** Returns the pose for this entity, relative to the activity space root. */
    @Override
    public Pose getPoseInActivitySpace() {
        // TODO: b/355680575 - Revisit if we need to account for parent rotation when calculating
        // the
        // scale. This code might produce unexpected results when non-uniform scale is involved in
        // the
        // parent-child entity hierarchy.

        // Any parentless "space" entities (such as the root and anchor entities) are expected to
        // override this method non-recursively so that this error is never thrown.
        if (!(getParent() instanceof AndroidXrEntity)) {
            throw new IllegalStateException(
                    "Cannot get pose in Activity Space with a non-AndroidXrEntity parent");
        }
        AndroidXrEntity xrParent = (AndroidXrEntity) getParent();
        return xrParent.getPoseInActivitySpace()
                .compose(
                        new Pose(
                                getPose().getTranslation().times(xrParent.getWorldSpaceScale()),
                                getPose().getRotation()));
    }

    /**
     * This method should be called when the ActivitySpace's underlying base space has been updated.
     * For example, this method should be called when the task node moves in XrExtensions.
     */
    public void onActivitySpaceUpdated() {
        // Defaults to a no-op.
    }

    // Returns the underlying extension Node for the Entity.
    public Node getNode() {
        return node;
    }

    @Override
    public void setParent(Entity parent) {
        if ((parent != null) && !(parent instanceof AndroidXrEntity)) {
            Log.e(
                    "RealityCoreRuntime",
                    "Cannot set non-AndroidXrEntity as a parent of a AndroidXrEntity");
            return;
        }
        super.setParent(parent);

        AndroidXrEntity xrParent = (AndroidXrEntity) parent;

        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            if (xrParent == null) {
                transaction.setVisibility(node, false).setParent(node, null);
            } else {
                transaction.setParent(node, xrParent.getNode());
            }
            transaction.apply();
        }
    }

    @Override
    public void setSize(Dimensions dimensions) {
        // TODO: b/326479171: Uncomment when extensions implement setSize.
        // try (NodeTransaction transaction = extensions.createNodeTransaction()) {
        //   transaction.setSize(node, dimensions.width, dimensions.height,
        // dimensions.depth).apply();
        // }
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);

        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction.setAlpha(node, alpha).apply();
        }
    }

    @Override
    public void setHidden(boolean hidden) {
        super.setHidden(hidden);

        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            if (reformOptions != null) {
                if (hidden) {
                    // Since this entity is being hidden, disable reform and the highlights around
                    // the node.
                    transaction.disableReform(node);
                } else {
                    // Enables reform and the highlights around the node.
                    transaction.enableReform(node, reformOptions);
                }
            }
            transaction.setVisibility(node, !hidden).apply();
        }
    }

    @Override
    public void addInputEventListener(Executor executor, InputEventListener eventListener) {
        maybeSetupInputListeners();
        inputEventListenerMap.put(eventListener, executor == null ? this.executor : executor);
    }

    /**
     * Request pointer capture for this Entity, using the given interfaces to propagate state and
     * captured input.
     *
     * <p>Returns true if a new pointer capture session was requested. Returns false if there is
     * already a previously existing pointer capture session as only one can be supported at a given
     * time.
     */
    public boolean requestPointerCapture(
            Executor executor,
            InputEventListener eventListener,
            PointerCaptureComponent.StateListener stateListener) {
        if (pointerCaptureInputEventListener.isPresent()) {
            return false;
        }
        getNode()
                .requestPointerCapture(
                        (pcState) -> {
                            if (pcState == Node.POINTER_CAPTURE_STATE_PAUSED) {
                                stateListener.onStateChanged(
                                        PointerCaptureComponent.POINTER_CAPTURE_STATE_PAUSED);
                            } else if (pcState == Node.POINTER_CAPTURE_STATE_ACTIVE) {
                                stateListener.onStateChanged(
                                        PointerCaptureComponent.POINTER_CAPTURE_STATE_ACTIVE);
                            } else if (pcState == Node.POINTER_CAPTURE_STATE_STOPPED) {
                                stateListener.onStateChanged(
                                        PointerCaptureComponent.POINTER_CAPTURE_STATE_STOPPED);
                            } else {
                                Log.e("Runtime", "Invalid state received for pointer capture");
                            }
                        },
                        executor);

        addPointerCaptureInputListener(executor, eventListener);
        return true;
    }

    private void addPointerCaptureInputListener(
            Executor executor, InputEventListener eventListener) {
        maybeSetupInputListeners();
        pointerCaptureInputEventListener = Optional.of(eventListener);
        pointerCaptureExecutor = Optional.ofNullable(executor);
    }

    private void maybeSetupInputListeners() {
        if (inputEventListenerMap.isEmpty() && pointerCaptureInputEventListener.isEmpty()) {
            node.listenForInput(
                    (xrInputEvent) -> {
                        if (xrInputEvent.getDispatchFlags()
                                == InputEvent.DISPATCH_FLAG_CAPTURED_POINTER) {
                            pointerCaptureInputEventListener.ifPresent(
                                    (listener) ->
                                            pointerCaptureExecutor
                                                    .orElse(this.executor)
                                                    .execute(
                                                            () ->
                                                                    listener.onInputEvent(
                                                                            RuntimeUtils
                                                                                    .getInputEvent(
                                                                                            xrInputEvent,
                                                                                            entityManager))));
                        } else {
                            inputEventListenerMap.forEach(
                                    (inputEventListener, listenerExecutor) ->
                                            listenerExecutor.execute(
                                                    () ->
                                                            inputEventListener.onInputEvent(
                                                                    RuntimeUtils.getInputEvent(
                                                                            xrInputEvent,
                                                                            entityManager))));
                        }
                    },
                    this.executor);
        }
    }

    @Override
    public void removeInputEventListener(InputEventListener consumer) {
        inputEventListenerMap.remove(consumer);
        maybeStopListeningForInput();
    }

    /** Stop any pointer capture requests on this Entity. */
    public void stopPointerCapture() {
        getNode().stopPointerCapture();
        pointerCaptureInputEventListener = Optional.empty();
        pointerCaptureExecutor = Optional.empty();
        maybeStopListeningForInput();
    }

    private void maybeStopListeningForInput() {
        if (inputEventListenerMap.isEmpty() && pointerCaptureInputEventListener.isEmpty()) {
            node.stopListeningForInput();
        }
    }

    @Override
    public void dispose() {
        inputEventListenerMap.clear();
        node.stopListeningForInput();
        reformEventConsumerMap.clear();
        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction.disableReform(node);
        }

        // SystemSpaceEntityImpls (Anchors, ActivitySpace, etc) should have null parents.
        if (getParent() != null) {
            setParent(null);
        }
        entityManager.removeEntityForNode(node);
        super.dispose();
    }

    /**
     * Gets the reform options for this entity.
     *
     * @return The reform options for this entity.
     */
    public ReformOptions getReformOptions() {
        if (reformOptions == null) {
            Consumer<ReformEvent> reformEventConsumer =
                    reformEvent -> {
                        if ((reformOptions.getEnabledReform() & ReformOptions.ALLOW_MOVE) != 0
                                && (reformOptions.getFlags()
                                                & ReformOptions.FLAG_ALLOW_SYSTEM_MOVEMENT)
                                        != 0) {
                            // Update the cached pose of the entity.
                            super.setPose(
                                    new Pose(
                                            new Vector3(
                                                    reformEvent.getProposedPosition().x,
                                                    reformEvent.getProposedPosition().y,
                                                    reformEvent.getProposedPosition().z),
                                            new Quaternion(
                                                    reformEvent.getProposedOrientation().x,
                                                    reformEvent.getProposedOrientation().y,
                                                    reformEvent.getProposedOrientation().z,
                                                    reformEvent.getProposedOrientation().w)));
                            // Update the cached scale of the entity.
                            super.setScaleInternal(
                                    new Vector3(
                                            reformEvent.getProposedScale().x,
                                            reformEvent.getProposedScale().y,
                                            reformEvent.getProposedScale().z));
                        }
                        reformEventConsumerMap.forEach(
                                (eventConsumer, consumerExecutor) ->
                                        consumerExecutor.execute(
                                                () -> eventConsumer.accept(reformEvent)));
                    };
            reformOptions = extensions.createReformOptions(reformEventConsumer, executor);
        }
        return reformOptions;
    }

    /**
     * Updates the reform options for this entity. Uses the same instance of [ReformOptions]
     * provided by {@link #getReformOptions()}.
     */
    public synchronized void updateReformOptions() {
        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            if (reformOptions.getEnabledReform() == 0) {
                // Disables reform and the highlights around the node.
                transaction.disableReform(node);
            } else {
                // Enables reform and the highlights around the node.
                transaction.enableReform(node, reformOptions);
            }
            transaction.apply();
        }
    }

    public void addReformEventConsumer(
            Consumer<ReformEvent> reformEventConsumer, Executor executor) {
        executor = (executor == null) ? this.executor : executor;
        reformEventConsumerMap.put(reformEventConsumer, executor);
    }

    public void removeReformEventConsumer(Consumer<ReformEvent> reformEventConsumer) {
        reformEventConsumerMap.remove(reformEventConsumer);
    }
}
