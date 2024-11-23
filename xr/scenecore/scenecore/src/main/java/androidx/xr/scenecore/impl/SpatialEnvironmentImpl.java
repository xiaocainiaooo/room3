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

import android.app.Activity;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.xr.extensions.XrExtensionResult;
import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.environment.EnvironmentVisibilityState;
import androidx.xr.extensions.environment.PassthroughVisibilityState;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.extensions.passthrough.PassthroughState;
import androidx.xr.extensions.space.SpatialState;
import androidx.xr.scenecore.JxrPlatformAdapter.ExrImageResource;
import androidx.xr.scenecore.JxrPlatformAdapter.GltfModelResource;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialCapabilities;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialEnvironment;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;
import com.google.ar.imp.apibindings.ImpressApi;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Concrete implementation of SpatialEnvironment / XR Wallpaper for Android XR. */
// TODO(b/373435470): Remove "deprecation"
@SuppressWarnings({"deprecation", "BanSynchronizedMethods"})
final class SpatialEnvironmentImpl implements SpatialEnvironment {

    public static final String TAG = "SpatialEnvironmentImpl";

    public static final String SKYBOX_NODE_NAME = "EnvironmentSkyboxNode";
    public static final String GEOMETRY_NODE_NAME = "EnvironmentGeometryNode";
    public static final String PASSTHROUGH_NODE_NAME = "EnvironmentPassthroughNode";
    @VisibleForTesting final Node passthroughNode;
    private final XrExtensions xrExtensions;
    private final Node rootEnvironmentNode;
    private final boolean useSplitEngine;
    @Nullable private Activity activity;
    // Used to represent the geometry
    private Node geometryNode;
    // the "xrExtensions.setEnvironment" call effectively makes a node into a skybox
    private Node skyboxNode;
    private SubspaceNode geometrySubspaceSplitEngine;
    private int geometrySubspaceImpressNode;
    private boolean isSpatialEnvironmentPreferenceActive = false;
    @Nullable private SpatialEnvironmentPreference spatialEnvironmentPreference = null;

    // The active passthrough opacity value is updated with every opacity change event. A null value
    // indicates it has not yet been initialized and the value should be read from the
    // spatialStateProvider.
    private Float activePassthroughOpacity = null;
    // Initialized to null to let system control opacity until preference is explicitly set.
    private Float passthroughOpacityPreference = null;
    private SplitEngineSubspaceManager splitEngineSubspaceManager;
    private ImpressApi impressApi;
    private final Supplier<SpatialState> spatialStateProvider;
    private SpatialState previousSpatialState = null;

    private final Set<Consumer<Boolean>> onSpatialEnvironmentChangedListeners =
            Collections.synchronizedSet(new HashSet<>());

    private final Set<Consumer<Float>> onPassthroughOpacityChangedListeners =
            Collections.synchronizedSet(new HashSet<>());

    public SpatialEnvironmentImpl(
            @NonNull Activity activity,
            @NonNull XrExtensions xrExtensions,
            @NonNull Node rootSceneNode,
            @NonNull Supplier<SpatialState> spatialStateProvider,
            boolean useSplitEngine) {
        this.activity = activity;
        this.xrExtensions = xrExtensions;
        this.passthroughNode = xrExtensions.createNode();
        this.rootEnvironmentNode = xrExtensions.createNode();
        this.geometryNode = xrExtensions.createNode();
        this.skyboxNode = xrExtensions.createNode();
        this.useSplitEngine = useSplitEngine;
        this.spatialStateProvider = spatialStateProvider;

        try (NodeTransaction transaction = xrExtensions.createNodeTransaction()) {
            transaction
                    .setName(geometryNode, GEOMETRY_NODE_NAME)
                    .setName(skyboxNode, SKYBOX_NODE_NAME)
                    .setName(passthroughNode, PASSTHROUGH_NODE_NAME)
                    .setParent(geometryNode, rootEnvironmentNode)
                    .setParent(skyboxNode, rootEnvironmentNode)
                    .setParent(passthroughNode, rootSceneNode)
                    .apply();
        }
    }

    // TODO: Remove these once we know the Equals() and Hashcode() methods are correct.
    boolean hasEnvironmentVisibilityChanged(@NonNull SpatialState spatialState) {
        if (previousSpatialState == null) {
            return true;
        }

        final EnvironmentVisibilityState previousEnvironmentVisibility =
                previousSpatialState.getEnvironmentVisibility();
        final EnvironmentVisibilityState currentEnvironmentVisibility =
                spatialState.getEnvironmentVisibility();

        if (previousEnvironmentVisibility.getCurrentState()
                != currentEnvironmentVisibility.getCurrentState()) {
            return true;
        }

        return false;
    }

    // TODO: Remove these once we know the Equals() and Hashcode() methods are correct.
    boolean hasPassthroughVisibilityChanged(@NonNull SpatialState spatialState) {
        if (previousSpatialState == null) {
            return true;
        }

        final PassthroughVisibilityState previousPassthroughVisibility =
                previousSpatialState.getPassthroughVisibility();
        final PassthroughVisibilityState currentPassthroughVisibility =
                spatialState.getPassthroughVisibility();

        if (previousPassthroughVisibility.getCurrentState()
                != currentPassthroughVisibility.getCurrentState()) {
            return true;
        }

        if (previousPassthroughVisibility.getOpacity()
                != currentPassthroughVisibility.getOpacity()) {
            return true;
        }

        return false;
    }

    // Package Private enum to return which spatial states have changed.
    enum ChangedSpatialStates {
        ENVIRONMENT_CHANGED,
        PASSTHROUGH_CHANGED
    }

    // Package Private method to set the current passthrough opacity and
    // isSpatialEnvironmentPreferenceActive from JxrPlatformAdapterAxr.
    // This method is synchronized because it sets several internal state variables at once, which
    // should be treated as an atomic set. We could consider replacing with AtomicReferences.
    @CanIgnoreReturnValue
    synchronized EnumSet<ChangedSpatialStates> setSpatialState(@NonNull SpatialState spatialState) {
        EnumSet<ChangedSpatialStates> changedSpatialStates =
                EnumSet.noneOf(ChangedSpatialStates.class);
        boolean passthroughVisibilityChanged = hasPassthroughVisibilityChanged(spatialState);
        if (passthroughVisibilityChanged) {
            changedSpatialStates.add(ChangedSpatialStates.PASSTHROUGH_CHANGED);
            this.activePassthroughOpacity =
                    RuntimeUtils.getPassthroughOpacity(spatialState.getPassthroughVisibility());
        }

        // TODO: b/371082454 - Check if the app is in FSM to ensure APP_VISIBLE refers to the
        // current
        // app and not another app that is visible.
        boolean environmentVisibilityChanged = hasEnvironmentVisibilityChanged(spatialState);
        if (environmentVisibilityChanged) {
            changedSpatialStates.add(ChangedSpatialStates.ENVIRONMENT_CHANGED);
            this.isSpatialEnvironmentPreferenceActive =
                    RuntimeUtils.getIsSpatialEnvironmentPreferenceActive(
                            spatialState.getEnvironmentVisibility().getCurrentState());
        }

        this.previousSpatialState = spatialState;
        return changedSpatialStates;
    }

    /** Flushes passthrough Node state to XrExtensions. */
    private void applyPassthroughChange(float opacityVal) {
        if (opacityVal > 0.0f) {
            try (NodeTransaction transaction = xrExtensions.createNodeTransaction()) {
                transaction
                        .setPassthroughState(
                                passthroughNode, opacityVal, PassthroughState.PASSTHROUGH_MODE_MAX)
                        .apply();
            }
        } else {
            try (NodeTransaction transaction = xrExtensions.createNodeTransaction()) {
                transaction
                        .setPassthroughState(
                                passthroughNode,
                                /* passthroughOpacity= */ 0.0f,
                                PassthroughState.PASSTHROUGH_MODE_OFF)
                        .apply();
            }
        }
    }

    @Override
    @NonNull
    @CanIgnoreReturnValue
    public SetPassthroughOpacityPreferenceResult setPassthroughOpacityPreference(
            @Nullable Float opacity) {
        // To work around floating-point precision issues, the opacity preference is documented to
        // clamp
        // to 0.0f if it is set below 1% opacity and it clamps to 1.0f if it is set above 99%
        // opacity.

        @Nullable
        Float newPassthroughOpacityPreference =
                opacity == null
                        ? null
                        : (opacity < 0.01f ? 0.0f : (opacity > 0.99f ? 1.0f : opacity));

        if (Objects.equals(newPassthroughOpacityPreference, passthroughOpacityPreference)) {
            return SetPassthroughOpacityPreferenceResult.CHANGE_APPLIED;
        }

        passthroughOpacityPreference = newPassthroughOpacityPreference;

        // to this method when they are removed

        // Passthrough should be enabled only if the user has explicitly set the
        // PassthroughOpacityPreference to a non-null and non-zero value, otherwise disabled.
        if (passthroughOpacityPreference != null && passthroughOpacityPreference != 0.0f) {
            applyPassthroughChange(passthroughOpacityPreference.floatValue());
        } else {
            applyPassthroughChange(0.0f);
        }

        if (RuntimeUtils.convertSpatialCapabilities(
                        spatialStateProvider.get().getSpatialCapabilities())
                .hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL)) {
            return SetPassthroughOpacityPreferenceResult.CHANGE_APPLIED;
        } else {
            return SetPassthroughOpacityPreferenceResult.CHANGE_PENDING;
        }
    }

    // Synchronized because we may need to update the entire Spatial State if the opacity has not
    // been
    // initialized previously.
    @Override
    public synchronized float getCurrentPassthroughOpacity() {
        if (activePassthroughOpacity == null) {
            setSpatialState(spatialStateProvider.get());
        }
        return activePassthroughOpacity.floatValue();
    }

    @Override
    @Nullable
    public Float getPassthroughOpacityPreference() {
        return passthroughOpacityPreference;
    }

    // This is called on the Activity's UI thread - so we should be careful to not block it.
    synchronized void firePassthroughOpacityChangedEvent(float opacity) {
        for (Consumer<Float> listener : onPassthroughOpacityChangedListeners) {
            listener.accept(opacity);
        }
    }

    @Override
    public void addOnPassthroughOpacityChangedListener(Consumer<Float> listener) {
        onPassthroughOpacityChangedListeners.add(listener);
    }

    @Override
    public void removeOnPassthroughOpacityChangedListener(Consumer<Float> listener) {
        onPassthroughOpacityChangedListeners.remove(listener);
    }

    /**
     * Stages updates to the CPM graph for the Environment to reflect a new skybox preference. If
     * skybox is null, this method unsets the client skybox preference, resulting in the system
     * skybox being used.
     */
    private void applySkybox(@Nullable ExrImageResourceImpl skybox) {
        // We need to create a new node here because we can't re-use the old CPM node when changing
        // geometry and skybox.
        try (NodeTransaction transaction = xrExtensions.createNodeTransaction()) {
            transaction.setParent(skyboxNode, null).apply();
        }

        this.skyboxNode = xrExtensions.createNode();
        try (NodeTransaction transaction = xrExtensions.createNodeTransaction()) {
            transaction
                    .setName(skyboxNode, SKYBOX_NODE_NAME)
                    .setParent(skyboxNode, rootEnvironmentNode);
            if (skybox != null) {
                transaction.setEnvironment(skyboxNode, skybox.getToken());
            }
            transaction.apply();
        }
    }

    /**
     * Stages updates to the CPM graph for the Environment to reflect a new geometry preference. If
     * geometry is null, this method unsets the client geometry preference, resulting in the system
     * geometry being used.
     */
    private void applyGeometryLegacy(@Nullable GltfModelResourceImpl geometry) {
        // We need to create a new node here because we can't re-use the old CPM node when changing
        // geometry and skybox.
        try (NodeTransaction transaction = xrExtensions.createNodeTransaction()) {
            transaction.setParent(geometryNode, null).apply();
        }
        this.geometryNode = xrExtensions.createNode();
        try (NodeTransaction transaction = xrExtensions.createNodeTransaction()) {
            transaction
                    .setName(geometryNode, GEOMETRY_NODE_NAME)
                    .setParent(geometryNode, rootEnvironmentNode);
            if (geometry != null) {
                transaction.setGltfModel(geometryNode, geometry.getExtensionModelToken());
            }
            transaction.apply();
        }
    }

    /**
     * Stages updates to the CPM graph for the Environment to reflect a new geometry preference. If
     * geometry is null, this method unsets the client geometry preference, resulting in the system
     * geometry being used.
     *
     * @throws IllegalStateException if called on a thread other than the main thread.
     */
    private void applyGeometrySplitEngine(@Nullable GltfModelResourceImplSplitEngine geometry) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        int prevGeometrySubspaceImpressNode = -1;
        SubspaceNode prevGeometrySubspaceSplitEngine = null;
        if (geometrySubspaceSplitEngine != null) {
            prevGeometrySubspaceSplitEngine = geometrySubspaceSplitEngine;
            geometrySubspaceSplitEngine = null;
            prevGeometrySubspaceImpressNode = geometrySubspaceImpressNode;
            geometrySubspaceImpressNode = -1;
        }

        geometrySubspaceImpressNode = impressApi.createImpressNode();
        String subspaceName = "geometry_subspace_" + geometrySubspaceImpressNode;

        geometrySubspaceSplitEngine =
                splitEngineSubspaceManager.createSubspace(
                        subspaceName, geometrySubspaceImpressNode);

        try (NodeTransaction transaction = xrExtensions.createNodeTransaction()) {
            transaction
                    .setName(geometrySubspaceSplitEngine.subspaceNode, GEOMETRY_NODE_NAME)
                    .setParent(geometrySubspaceSplitEngine.subspaceNode, rootEnvironmentNode)
                    .setPosition(geometrySubspaceSplitEngine.subspaceNode, 0.0f, 0.0f, 0.0f)
                    .setScale(geometrySubspaceSplitEngine.subspaceNode, 1.0f, 1.0f, 1.0f)
                    .setOrientation(
                            geometrySubspaceSplitEngine.subspaceNode, 0.0f, 0.0f, 0.0f, 1.0f)
                    .apply();
        }

        if (geometry != null) {
            int modelImpressNode =
                    impressApi.instanceGltfModel(
                            geometry.getExtensionModelToken(), /* enableCollider= */ false);
            impressApi.setImpressNodeParent(modelImpressNode, geometrySubspaceImpressNode);
        }

        if (prevGeometrySubspaceSplitEngine != null && prevGeometrySubspaceImpressNode != -1) {
            // Detach the previous geometry subspace from the root environment node.
            try (NodeTransaction transaction = xrExtensions.createNodeTransaction()) {
                transaction.setParent(prevGeometrySubspaceSplitEngine.subspaceNode, null).apply();
            }
            splitEngineSubspaceManager.deleteSubspace(prevGeometrySubspaceSplitEngine.subspaceId);
            impressApi.destroyImpressNode(prevGeometrySubspaceImpressNode);

            prevGeometrySubspaceSplitEngine = null;
            prevGeometrySubspaceImpressNode = -1;
        }
    }

    void onSplitEngineReady(SplitEngineSubspaceManager subspaceManager, ImpressApi api) {
        this.splitEngineSubspaceManager = subspaceManager;
        this.impressApi = api;
    }

    @Override
    @NonNull
    @CanIgnoreReturnValue
    public SetSpatialEnvironmentPreferenceResult setSpatialEnvironmentPreference(
            @Nullable SpatialEnvironmentPreference newPreference) {
        // TODO: b/378914007 This method is not safe for reentrant calls.

        if (Objects.equals(newPreference, spatialEnvironmentPreference)) {
            return SetSpatialEnvironmentPreferenceResult.CHANGE_APPLIED;
        }

        GltfModelResource newGeometry = newPreference == null ? null : newPreference.geometry;
        GltfModelResource prevGeometry =
                spatialEnvironmentPreference == null ? null : spatialEnvironmentPreference.geometry;
        ExrImageResource newSkybox = newPreference == null ? null : newPreference.skybox;
        ExrImageResource prevSkybox =
                spatialEnvironmentPreference == null ? null : spatialEnvironmentPreference.skybox;

        // TODO(b/329907079): Map GltfModelResourceImplSplitEngine to GltfModelResource in Impl
        // Layer
        if (newGeometry != null) {
            if (useSplitEngine && !(newGeometry instanceof GltfModelResourceImplSplitEngine)) {
                throw new IllegalArgumentException(
                        "SplitEngine is enabled but the prefererred geometry is not of type"
                                + " GltfModelResourceImplSplitEngine.");
            } else if (!useSplitEngine && !(newGeometry instanceof GltfModelResourceImpl)) {
                throw new IllegalArgumentException(
                        "SplitEngine is disabled but the prefererred geometry is not of type"
                                + " GltfModelResourceImpl.");
            }
        }

        // TODO b/329907079: Map ExrImageResourceImpl to ExrImageResource in Impl Layer
        if (newSkybox != null && !(newSkybox instanceof ExrImageResourceImpl)) {
            throw new IllegalArgumentException(
                    "The prefererred skybox is not of type ExrImageResourceImpl.");
        }

        if (!Objects.equals(newGeometry, prevGeometry)) {
            // TODO: b/354711945 - Remove this check once we migrate completely to SplitEngine
            if (useSplitEngine) {
                applyGeometrySplitEngine((GltfModelResourceImplSplitEngine) newGeometry);
            } else {
                applyGeometryLegacy((GltfModelResourceImpl) newGeometry);
            }
        }

        if (!Objects.equals(newSkybox, prevSkybox)) {
            // TODO: b/371221872 - If the preference object is non-null but contains a null skybox,
            // we
            // should set a black skybox. (This may require a change to the attach/detach logic
            // below.)
            applySkybox((ExrImageResourceImpl) newSkybox);
        }

        try (NodeTransaction transaction = xrExtensions.createNodeTransaction()) {
            if (newSkybox == null && newGeometry == null) {
                xrExtensions.detachSpatialEnvironment(
                        activity,
                        (result) -> logXrExtensionResult("detachSpatialEnvironment", result),
                        Runnable::run);
            } else {
                xrExtensions.attachSpatialEnvironment(
                        activity,
                        rootEnvironmentNode,
                        (result) -> logXrExtensionResult("attachSpatialEnvironment", result),
                        Runnable::run);
            }
        }

        this.spatialEnvironmentPreference = newPreference;

        if (RuntimeUtils.convertSpatialCapabilities(
                        spatialStateProvider.get().getSpatialCapabilities())
                .hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT)) {
            return SetSpatialEnvironmentPreferenceResult.CHANGE_APPLIED;
        } else {
            return SetSpatialEnvironmentPreferenceResult.CHANGE_PENDING;
        }
    }

    private void logXrExtensionResult(String prefix, XrExtensionResult result) {
        // TODO: b/376934871 - Better error handling?
        switch (result.getResult()) {
            case XrExtensionResult.XR_RESULT_SUCCESS:
            case XrExtensionResult.XR_RESULT_SUCCESS_NOT_VISIBLE:
                Log.d(TAG, prefix + ": success (" + result.getResult() + ")");
                break;
            case XrExtensionResult.XR_RESULT_IGNORED_ALREADY_APPLIED:
                Log.d(TAG, prefix + ": ignored, already applied (" + result.getResult() + ")");
                break;
            case XrExtensionResult.XR_RESULT_ERROR_NOT_ALLOWED:
            case XrExtensionResult.XR_RESULT_ERROR_SYSTEM:
                Log.e(TAG, prefix + ": error (" + result.getResult() + ")");
                break;
            default:
                Log.e(TAG, prefix + ": Unexpected return value (" + result.getResult() + ")");
                break;
        }
    }

    @Override
    @Nullable
    public SpatialEnvironmentPreference getSpatialEnvironmentPreference() {
        return spatialEnvironmentPreference;
    }

    @Override
    public boolean isSpatialEnvironmentPreferenceActive() {
        return isSpatialEnvironmentPreferenceActive;
    }

    // This is called on the Activity's UI thread - so we should be careful to not block it.
    synchronized void fireOnSpatialEnvironmentChangedEvent(
            boolean isSpatialEnvironmentPreferenceActive) {
        for (Consumer<Boolean> listener : onSpatialEnvironmentChangedListeners) {
            listener.accept(isSpatialEnvironmentPreferenceActive);
        }
    }

    @Override
    public void addOnSpatialEnvironmentChangedListener(Consumer<Boolean> listener) {
        onSpatialEnvironmentChangedListeners.add(listener);
    }

    @Override
    public void removeOnSpatialEnvironmentChangedListener(Consumer<Boolean> listener) {
        onSpatialEnvironmentChangedListeners.remove(listener);
    }

    /**
     * Disposes of the environment and all of its resources.
     *
     * <p>This should be called when the environment is no longer needed.
     */
    public void dispose() {
        if (useSplitEngine) {
            if (this.geometrySubspaceSplitEngine != null) {
                try (NodeTransaction transaction = xrExtensions.createNodeTransaction()) {
                    transaction.setParent(geometrySubspaceSplitEngine.subspaceNode, null).apply();
                }
                this.splitEngineSubspaceManager.deleteSubspace(
                        this.geometrySubspaceSplitEngine.subspaceId);
                this.geometrySubspaceSplitEngine = null;
                impressApi.destroyImpressNode(geometrySubspaceImpressNode);
            }
        }
        this.activePassthroughOpacity = null;
        this.passthroughOpacityPreference = null;
        try (NodeTransaction transaction = xrExtensions.createNodeTransaction()) {
            transaction
                    .setParent(skyboxNode, null)
                    .setParent(geometryNode, null)
                    .setParent(passthroughNode, null)
                    .apply();
        }
        this.geometrySubspaceSplitEngine = null;
        this.geometrySubspaceImpressNode = 0;
        this.splitEngineSubspaceManager = null;
        this.impressApi = null;
        this.spatialEnvironmentPreference = null;
        this.isSpatialEnvironmentPreferenceActive = false;
        this.onPassthroughOpacityChangedListeners.clear();
        this.onSpatialEnvironmentChangedListeners.clear();
        // TODO: b/376934871 - Check async results.
        xrExtensions.detachSpatialEnvironment(activity, (result) -> {}, Runnable::run);
        this.activity = null;
    }
}
