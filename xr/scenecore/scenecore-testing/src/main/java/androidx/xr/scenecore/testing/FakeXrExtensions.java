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

package androidx.xr.scenecore.testing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.view.AttachedSurfaceControl;
import android.view.SurfaceControlViewHost;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.xr.extensions.Config;
import androidx.xr.extensions.Consumer;
import androidx.xr.extensions.XrExtensionResult;
import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.environment.EnvironmentVisibilityState;
import androidx.xr.extensions.environment.PassthroughVisibilityState;
import androidx.xr.extensions.media.AudioTrackExtensions;
import androidx.xr.extensions.media.MediaPlayerExtensions;
import androidx.xr.extensions.media.PointSourceAttributes;
import androidx.xr.extensions.media.SoundFieldAttributes;
import androidx.xr.extensions.media.SoundPoolExtensions;
import androidx.xr.extensions.media.SpatializerExtensions;
import androidx.xr.extensions.media.XrSpatialAudioExtensions;
import androidx.xr.extensions.node.InputEvent;
import androidx.xr.extensions.node.Mat4f;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.extensions.node.NodeTransform;
import androidx.xr.extensions.node.Quatf;
import androidx.xr.extensions.node.ReformEvent;
import androidx.xr.extensions.node.ReformOptions;
import androidx.xr.extensions.node.Vec3;
import androidx.xr.extensions.passthrough.PassthroughState;
import androidx.xr.extensions.space.ActivityPanel;
import androidx.xr.extensions.space.ActivityPanelLaunchParameters;
import androidx.xr.extensions.space.Bounds;
import androidx.xr.extensions.space.HitTestResult;
import androidx.xr.extensions.space.SpatialCapabilities;
import androidx.xr.extensions.space.SpatialState;
import androidx.xr.extensions.splitengine.SplitEngineBridge;
import androidx.xr.extensions.subspace.Subspace;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.Closeable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A fake for the XrExtensions.
 *
 * <p>This has fake implementations for a subset of the XrExtension capability that is used by the
 * JXRCore runtime for AndroidXR.
 */
@SuppressWarnings("deprecation")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeXrExtensions implements XrExtensions {
    private static final String NOT_IMPLEMENTED_IN_FAKE =
            "This function is not implemented yet in FakeXrExtensions.  Please add an"
                    + " implementation if support is desired for testing.";

    @NonNull public final List<FakeNode> createdNodes = new ArrayList<>();

    @NonNull public final List<FakeGltfModelToken> createdGltfModelTokens = new ArrayList<>();

    @NonNull public final List<FakeEnvironmentToken> createdEnvironmentTokens = new ArrayList<>();

    @NonNull public final Map<Activity, FakeActivityPanel> activityPanelMap = new HashMap<>();

    FakeNode fakeTaskNode = null;
    FakeNode fakeEnvironmentNode = null;
    FakeNode fakeNodeForMainWindow = null;

    // TODO: b/370033054 - fakeSpatialState should be updated according to some fake extensions
    // calls
    // like requestFullSpaceMode after migration to SpatialState API
    @NonNull public final FakeSpatialState fakeSpatialState = new FakeSpatialState();

    // Technically this could be set per-activity, but we're assuming that there's a single activity
    // associated with each JXRCore session, so we're only tracking it once for now.
    SpaceMode spaceMode = SpaceMode.NONE;

    int mainWindowWidth = 0;
    int mainWindowHeight = 0;

    Consumer<SpatialState> spatialStateCallback = null;

    float preferredAspectRatioHsm = 0.0f;
    int openXrWorldSpaceType = 0;
    FakeNodeTransaction lastFakeNodeTransaction = null;

    @NonNull
    public final FakeSpatialAudioExtensions fakeSpatialAudioExtensions =
            new FakeSpatialAudioExtensions();

    @Nullable
    public FakeNode getFakeEnvironmentNode() {
        return fakeEnvironmentNode;
    }

    @Nullable
    public FakeNode getFakeNodeForMainWindow() {
        return fakeNodeForMainWindow;
    }

    @Override
    public int getApiVersion() {
        // The API surface is aligned with the initial XRU release.
        return 1;
    }

    @Override
    @NonNull
    public Node createNode() {
        FakeNode node = new FakeNode();
        createdNodes.add(node);
        return node;
    }

    @Override
    @NonNull
    public NodeTransaction createNodeTransaction() {
        lastFakeNodeTransaction = new FakeNodeTransaction();
        return lastFakeNodeTransaction;
    }

    @Override
    @NonNull
    public Subspace createSubspace(@NonNull SplitEngineBridge splitEngineBridge, int subspaceId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @NonNull
    @Deprecated
    public Bundle setMainPanelCurvatureRadius(@NonNull Bundle bundle, float panelCurvatureRadius) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    @NonNull
    public Config getConfig() {
        return new FakeConfig();
    }

    @NonNull
    public SpaceMode getSpaceMode() {
        return spaceMode;
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void setMainWindowSize(@NonNull Activity activity, int width, int height) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void setMainWindowSize(
            @NonNull Activity activity,
            int width,
            int height,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mainWindowWidth = width;
        mainWindowHeight = height;
        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void setMainWindowCurvatureRadius(@NonNull Activity activity, float curvatureRadius) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    public int getMainWindowWidth() {
        return mainWindowWidth;
    }

    public int getMainWindowHeight() {
        return mainWindowHeight;
    }

    @Override
    public void getBounds(
            @NonNull Activity activity,
            @NonNull Consumer<Bounds> callback,
            @NonNull Executor executor) {
        callback.accept(
                (spaceMode == SpaceMode.FULL_SPACE
                        ? new Bounds(
                                Float.POSITIVE_INFINITY,
                                Float.POSITIVE_INFINITY,
                                Float.POSITIVE_INFINITY)
                        : new Bounds(1f, 1f, 1f)));
    }

    private SpatialCapabilities getCapabilities(boolean allowAll) {
        return new SpatialCapabilities() {
            @Override
            public boolean get(int capQuery) {
                return allowAll;
            }
        };
    }

    @Override
    public void getSpatialCapabilities(
            @NonNull Activity activity,
            @NonNull Consumer<SpatialCapabilities> callback,
            @NonNull Executor executor) {
        callback.accept(fakeSpatialState.getSpatialCapabilities());
    }

    @Override
    @NonNull
    public SpatialState getSpatialState(@NonNull Activity activity) {
        return fakeSpatialState;
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public boolean canEmbedActivityPanel(@NonNull Activity activity) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public boolean requestFullSpaceMode(@NonNull Activity activity) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void requestFullSpaceMode(
            @NonNull Activity activity,
            boolean requestEnter,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        FakeSpatialState spatialState = new FakeSpatialState();
        spatialState.bounds =
                requestEnter
                        ? new Bounds(
                                Float.POSITIVE_INFINITY,
                                Float.POSITIVE_INFINITY,
                                Float.POSITIVE_INFINITY)
                        : new Bounds(10f, 10f, 10f);
        spatialState.capabilities = getCapabilities(requestEnter);
        sendSpatialState(spatialState);
        spaceMode = requestEnter ? SpaceMode.FULL_SPACE : SpaceMode.HOME_SPACE;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public boolean requestHomeSpaceMode(@NonNull Activity activity) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void setSpatialStateCallback(
            @NonNull Activity activity,
            @NonNull Consumer<androidx.xr.extensions.space.SpatialStateEvent> callback,
            @NonNull Executor executor) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    @SuppressLint("PairedRegistration")
    public void registerSpatialStateCallback(
            @NonNull Activity activity,
            @NonNull Consumer<SpatialState> callback,
            @NonNull Executor executor) {
        // note that we assume this is only called for the (single) primary activity associated with
        // the JXRCore session and we also don't honor the executor here
        spatialStateCallback = callback;
    }

    @Override
    public void clearSpatialStateCallback(@NonNull Activity activity) {
        spatialStateCallback = null;
    }

    // Method for tests to call to trigger the spatial state callback.
    // This should probably be called on the provided executor.
    public void sendSpatialState(@NonNull SpatialState spatialState) {
        if (spatialStateCallback != null) {
            spatialStateCallback.accept(spatialState);
        }
    }

    @Nullable
    public Consumer<SpatialState> getSpatialStateCallback() {
        return spatialStateCallback;
    }

    private XrExtensionResult createAsyncResult() {
        return new XrExtensionResult() {
            @Override
            public int getResult() {
                return XrExtensionResult.XR_RESULT_SUCCESS;
            }
        };
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void attachSpatialScene(
            @NonNull Activity activity, @NonNull Node sceneNode, @NonNull Node windowNode) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void attachSpatialScene(
            @NonNull Activity activity,
            @NonNull Node sceneNode,
            @NonNull Node windowNode,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        fakeTaskNode = (FakeNode) sceneNode;
        fakeTaskNode.name = "taskNode";

        fakeNodeForMainWindow = (FakeNode) windowNode;
        fakeNodeForMainWindow.name = "nodeForMainWindow";

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void detachSpatialScene(@NonNull Activity activity) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void detachSpatialScene(
            @NonNull Activity activity,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        fakeTaskNode = null;
        fakeNodeForMainWindow = null;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    @Nullable
    public FakeNode getFakeTaskNode() {
        return fakeTaskNode;
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void attachSpatialEnvironment(
            @NonNull Activity activity, @NonNull Node environmentNode) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void attachSpatialEnvironment(
            @NonNull Activity activity,
            @NonNull Node environmentNode,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        fakeEnvironmentNode = (FakeNode) environmentNode;
        fakeEnvironmentNode.name = "environmentNode";

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void detachSpatialEnvironment(@NonNull Activity activity) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void detachSpatialEnvironment(
            @NonNull Activity activity,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        fakeEnvironmentNode = null;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings({"AndroidJdkLibsChecker", "BadFuture"})
    @Override
    @NonNull
    @Deprecated
    public CompletableFuture</* @Nullable */ androidx.xr.extensions.asset.GltfModelToken>
            loadGltfModel(
                    @Nullable InputStream asset,
                    int regionSizeBytes,
                    int regionOffsetBytes,
                    @Nullable String url) {
        FakeGltfModelToken modelToken = new FakeGltfModelToken(url);
        createdGltfModelTokens.add(modelToken);
        return CompletableFuture.completedFuture(modelToken);
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings("AndroidJdkLibsChecker")
    @Override
    @NonNull
    @Deprecated
    public CompletableFuture</* @Nullable */ SceneViewerResult> displayGltfModel(
            Activity activity, androidx.xr.extensions.asset.GltfModelToken gltfModel) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings({"AndroidJdkLibsChecker", "BadFuture"})
    @Override
    @NonNull
    @Deprecated
    //  public ListenableFuture</* @Nullable */ androidx.xr.extensions.asset.EnvironmentToken>
    // loadEnvironment(
    public CompletableFuture</* @Nullable */ androidx.xr.extensions.asset.EnvironmentToken>
            loadEnvironment(
                    @Nullable InputStream asset,
                    int regionSizeBytes,
                    int regionOffsetBytes,
                    @Nullable String url) {
        FakeEnvironmentToken imageToken = new FakeEnvironmentToken(url);
        createdEnvironmentTokens.add(imageToken);
        //    return immediateFuture(imageToken);
        return CompletableFuture.completedFuture(imageToken);
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings("AndroidJdkLibsChecker")
    @Override
    @NonNull
    @Deprecated
    public CompletableFuture</* @Nullable */ androidx.xr.extensions.asset.EnvironmentToken>
            loadEnvironment(
                    InputStream asset,
                    int regionSizeBytes,
                    int regionOffsetBytes,
                    String url,
                    int textureWidth,
                    int textureHeight) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings("AndroidJdkLibsChecker")
    @Override
    @Deprecated
    @NonNull
    public CompletableFuture</* @Nullable */ androidx.xr.extensions.asset.SceneToken>
            loadImpressScene(InputStream asset, int regionSizeBytes, int regionOffsetBytes) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    @NonNull
    public SplitEngineBridge createSplitEngineBridge() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * Returns a FakeNode with corresponding gltfModelToken if it was created and found
     *
     * @deprecated This method is no longer supported.
     */
    @NonNull
    @Deprecated
    public FakeNode testGetNodeWithGltfToken(
            @NonNull androidx.xr.extensions.asset.GltfModelToken token) {
        for (FakeNode node : createdNodes) {
            if (node.gltfModel != null && node.gltfModel.equals(token)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Returns a FakeNode with corresponding environmentToken if it was created and found
     *
     * @deprecated This method is no longer supported.
     */
    @NonNull
    @Deprecated
    public FakeNode testGetNodeWithEnvironmentToken(
            @NonNull androidx.xr.extensions.asset.EnvironmentToken token) {
        for (FakeNode node : createdNodes) {
            if (node.environment != null && node.environment.equals(token)) {
                return node;
            }
        }
        return null;
    }

    @Override
    @NonNull
    public ActivityPanel createActivityPanel(
            @NonNull Activity host, @NonNull ActivityPanelLaunchParameters launchParameters) {
        FakeActivityPanel fakeActivityPanel = new FakeActivityPanel(createNode());
        activityPanelMap.put(host, fakeActivityPanel);
        return fakeActivityPanel;
    }

    @NonNull
    public FakeActivityPanel getActivityPanelForHost(@NonNull Activity host) {
        return activityPanelMap.get(host);
    }

    @Override
    @NonNull
    public ReformOptions createReformOptions(
            @NonNull Consumer<ReformEvent> callback, @NonNull Executor executor) {
        return new FakeReformOptions(callback, executor);
    }

    @Override
    public void addFindableView(@NonNull View view, @NonNull ViewGroup group) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void removeFindableView(@NonNull View view, @NonNull ViewGroup group) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    @Nullable
    public Node getSurfaceTrackingNode(@NonNull View view) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void hitTest(
            @NonNull Activity activity,
            @NonNull Vec3 origin,
            @NonNull Vec3 direction,
            @NonNull Consumer<HitTestResult> callback,
            @NonNull Executor executor) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public int getOpenXrWorldSpaceType() {
        return openXrWorldSpaceType;
    }

    public void setOpenXrWorldSpaceType(int openXrWorldSpaceType) {
        this.openXrWorldSpaceType = openXrWorldSpaceType;
    }

    @Override
    @NonNull
    public Bundle setFullSpaceMode(@NonNull Bundle bundle) {
        return bundle;
    }

    @Override
    @NonNull
    public Bundle setFullSpaceModeWithEnvironmentInherited(@NonNull Bundle bundle) {
        return bundle;
    }

    @Override
    public void setPreferredAspectRatio(@NonNull Activity activity, float preferredRatio) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void setPreferredAspectRatio(
            @NonNull Activity activity,
            float preferredRatio,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        preferredAspectRatioHsm = preferredRatio;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    public float getPreferredAspectRatio() {
        return preferredAspectRatioHsm;
    }

    @NonNull
    @Override
    public XrSpatialAudioExtensions getXrSpatialAudioExtensions() {
        return fakeSpatialAudioExtensions;
    }

    /** Tracks whether an Activity has requested a mode for when it's focused. */
    public enum SpaceMode {
        NONE,
        HOME_SPACE,
        FULL_SPACE
    }

    /** Fake implementation of Extensions Config. */
    public static class FakeConfig implements Config {
        public static final float DEFAULT_PIXELS_PER_METER = 1f;

        @Override
        public float defaultPixelsPerMeter(float density) {
            return DEFAULT_PIXELS_PER_METER;
        }
    }

    /** A fake implementation of Closeable. */
    @SuppressWarnings("NotCloseable")
    public static class FakeCloseable implements Closeable {
        boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }
    }

    /** A fake implementation of the XR extensions Node. */
    @SuppressWarnings("ParcelCreator")
    public static final class FakeNode implements Node {
        FakeNode parent = null;
        float xPosition = 0.0f;
        float yPosition = 0.0f;
        float zPosition = 0.0f;
        float xOrientation = 0.0f;
        float yOrientation = 0.0f;
        float zOrientation = 0.0f;
        float wOrientation = 1.0f;
        float xScale = 1.0f;
        float yScale = 1.0f;
        float zScale = 1.0f;
        float cornerRadius = 0.0f;
        boolean isVisible = false;
        float alpha = 1.0f;
        androidx.xr.extensions.asset.GltfModelToken gltfModel = null;
        IBinder anchorId = null;
        String name = null;
        float passthroughOpacity = 1.0f;
        @PassthroughState.Mode int passthroughMode = 0;
        SurfaceControlViewHost.SurfacePackage surfacePackage = null;
        androidx.xr.extensions.asset.EnvironmentToken environment = null;
        Consumer<InputEvent> listener = null;
        Consumer<NodeTransform> transformListener = null;
        Consumer<Integer> pointerCaptureStateCallback = null;

        Executor executor = null;
        ReformOptions reformOptions;
        Executor transformExecutor = null;

        private FakeNode() {}

        @Override
        public void listenForInput(
                @NonNull Consumer<InputEvent> listener, @NonNull Executor executor) {
            this.listener = listener;
            this.executor = executor;
        }

        @Override
        public void stopListeningForInput() {
            listener = null;
            executor = null;
        }

        @Override
        public void setNonPointerFocusTarget(@NonNull AttachedSurfaceControl focusTarget) {}

        @Override
        public void requestPointerCapture(
                @NonNull Consumer<Integer> stateCallback, @NonNull Executor executor) {
            pointerCaptureStateCallback = stateCallback;
        }

        @Override
        public void stopPointerCapture() {
            pointerCaptureStateCallback = null;
        }

        public void sendInputEvent(@NonNull InputEvent event) {
            executor.execute(() -> listener.accept(event));
        }

        public void sendTransformEvent(@NonNull FakeNodeTransform nodeTransform) {
            transformExecutor.execute(() -> transformListener.accept(nodeTransform));
        }

        @Override
        @NonNull
        public Closeable subscribeToTransform(
                @NonNull Consumer<NodeTransform> transformCallback, @NonNull Executor executor) {
            this.transformListener = transformCallback;
            this.transformExecutor = executor;
            return new FakeCloseable();
        }

        @Nullable
        public Consumer<NodeTransform> getTransformListener() {
            return transformListener;
        }

        @Nullable
        public Executor getTransformExecutor() {
            return transformExecutor;
        }

        @Override
        public void writeToParcel(@NonNull Parcel in, int flags) {}

        @Override
        public int describeContents() {
            return 0;
        }

        @Nullable
        public FakeNode getParent() {
            return parent;
        }

        public float getXPosition() {
            return xPosition;
        }

        public float getYPosition() {
            return yPosition;
        }

        public float getZPosition() {
            return zPosition;
        }

        public float getXOrientation() {
            return xOrientation;
        }

        public float getYOrientation() {
            return yOrientation;
        }

        public float getZOrientation() {
            return zOrientation;
        }

        public float getWOrientation() {
            return wOrientation;
        }

        public boolean isVisible() {
            return isVisible;
        }

        public float getAlpha() {
            return alpha;
        }

        /**
         * @deprecated This method is no longer supported.
         */
        @Nullable
        @Deprecated
        public androidx.xr.extensions.asset.GltfModelToken getGltfModel() {
            return gltfModel;
        }

        @Nullable
        public IBinder getAnchorId() {
            return anchorId;
        }

        @Nullable
        public String getName() {
            return name;
        }

        @Nullable
        public SurfaceControlViewHost.SurfacePackage getSurfacePackage() {
            return surfacePackage;
        }

        /**
         * @deprecated This method is no longer supported.
         */
        @Nullable
        @Deprecated
        public androidx.xr.extensions.asset.EnvironmentToken getEnvironment() {
            return environment;
        }

        @Nullable
        public Consumer<InputEvent> getListener() {
            return listener;
        }

        @Nullable
        public Consumer<Integer> getPointerCaptureStateCallback() {
            return pointerCaptureStateCallback;
        }

        @Nullable
        public Executor getExecutor() {
            return executor;
        }

        @Nullable
        public ReformOptions getReformOptions() {
            return reformOptions;
        }
    }

    /**
     * A fake implementation of the XR extensions Node transaction.
     *
     * <p>All modifications happen immediately and not when the transaction is applied.
     */
    @SuppressWarnings("NotCloseable")
    public static class FakeNodeTransaction implements NodeTransaction {
        FakeNode lastFakeNode = null;
        boolean applied = false;

        private FakeNodeTransaction() {}

        @Override
        @NonNull
        public NodeTransaction setParent(@NonNull Node node, @Nullable Node parent) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).parent = (FakeNode) parent;
            return this;
        }

        /**
         * @deprecated This method is no longer supported.
         */
        @Override
        @NonNull
        @Deprecated
        public NodeTransaction setEnvironment(
                @NonNull Node node, @Nullable androidx.xr.extensions.asset.EnvironmentToken token) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).environment = token;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setPosition(@NonNull Node node, float x, float y, float z) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).xPosition = x;
            ((FakeNode) node).yPosition = y;
            ((FakeNode) node).zPosition = z;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setOrientation(
                @NonNull Node node, float x, float y, float z, float w) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).xOrientation = x;
            ((FakeNode) node).yOrientation = y;
            ((FakeNode) node).zOrientation = z;
            ((FakeNode) node).wOrientation = w;
            return this;
        }

        @Override
        @NonNull
        // TODO(b/354731545): Cover this with an AndroidXREntity test
        public NodeTransaction setScale(@NonNull Node node, float sx, float sy, float sz) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).xScale = sx;
            ((FakeNode) node).yScale = sy;
            ((FakeNode) node).zScale = sz;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setVisibility(@NonNull Node node, boolean isVisible) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).isVisible = isVisible;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setAlpha(@NonNull Node node, float value) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).alpha = value;
            return this;
        }

        /**
         * @deprecated This method is no longer supported.
         */
        @Override
        @NonNull
        @Deprecated
        public NodeTransaction setGltfModel(
                @NonNull Node node,
                @NonNull androidx.xr.extensions.asset.GltfModelToken gltfModelToken) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).gltfModel = gltfModelToken;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setName(@NonNull Node node, @NonNull String name) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).name = name;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setPassthroughState(
                @NonNull Node node,
                float passthroughOpacity,
                @PassthroughState.Mode int passthroughMode) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).passthroughOpacity = passthroughOpacity;
            ((FakeNode) node).passthroughMode = passthroughMode;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setSurfacePackage(
                @Nullable Node node,
                @NonNull SurfaceControlViewHost.SurfacePackage surfacePackage) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).surfacePackage = surfacePackage;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setWindowBounds(
                @NonNull SurfaceControlViewHost.SurfacePackage surfacePackage,
                int widthPx,
                int heightPx) {
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setAnchorId(@NonNull Node node, @Nullable IBinder anchorId) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).anchorId = anchorId;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction enableReform(@NonNull Node node, @NonNull ReformOptions options) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).reformOptions = options;
            ((FakeReformOptions) options).optionsApplied = true;
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setReformSize(@NonNull Node node, @NonNull Vec3 reformSize) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).reformOptions.setCurrentSize(reformSize);
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction disableReform(@NonNull Node node) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).reformOptions = new FakeReformOptions(null, null);
            return this;
        }

        @Override
        @NonNull
        public NodeTransaction setCornerRadius(@NonNull Node node, float cornerRadius) {
            lastFakeNode = (FakeNode) node;
            ((FakeNode) node).cornerRadius = cornerRadius;
            return this;
        }

        @Override
        public void apply() {
            applied = true;
        }

        @Override
        public void close() {}
    }

    /** A fake implementation of the XR extensions NodeTransform. */
    public static class FakeNodeTransform implements NodeTransform {
        Mat4f transform;

        public FakeNodeTransform(@NonNull Mat4f transform) {
            this.transform = transform;
        }

        @Override
        @NonNull
        public Mat4f getTransform() {
            return transform;
        }

        @Override
        public long getTimestamp() {
            return 0;
        }
    }

    /** A fake implementation of the XR extensions GltfModelToken. */
    public static class FakeGltfModelToken implements androidx.xr.extensions.asset.GltfModelToken {
        String url;

        public FakeGltfModelToken(@NonNull String url) {
            this.url = url;
        }

        @NonNull
        public String getUrl() {
            return url;
        }
    }

    /** A fake implementation of the XR extensions EnvironmentToken. */
    public static class FakeEnvironmentToken
            implements androidx.xr.extensions.asset.EnvironmentToken {
        String url;

        private FakeEnvironmentToken(@NonNull String url) {
            this.url = url;
        }

        @NonNull
        public String getUrl() {
            return url;
        }
    }

    /** A fake implementation of the XR extensions EnvironmentVisibilityState. */
    public static class FakeEnvironmentVisibilityState implements EnvironmentVisibilityState {
        @EnvironmentVisibilityState.State int state;

        public FakeEnvironmentVisibilityState(@EnvironmentVisibilityState.State int state) {
            this.state = state;
        }

        @Override
        @EnvironmentVisibilityState.State
        public int getCurrentState() {
            return state;
        }
    }

    /** A fake implementation of the XR extensions EnvironmentVisibilityState. */
    public static class FakePassthroughVisibilityState implements PassthroughVisibilityState {
        @PassthroughVisibilityState.State int state;
        float opacity;

        public FakePassthroughVisibilityState(
                @PassthroughVisibilityState.State int state, float opacity) {
            this.state = state;
            this.opacity = opacity;
        }

        @Override
        @PassthroughVisibilityState.State
        public int getCurrentState() {
            return state;
        }

        @Override
        public float getOpacity() {
            return opacity;
        }
    }

    /** Creates fake activity panel. */
    public static class FakeActivityPanel implements ActivityPanel {
        Intent launchIntent;
        Bundle bundle;
        Activity activity;
        Rect bounds;
        boolean isDeleted = false;
        Node node;

        FakeActivityPanel(@NonNull Node node) {
            this.node = node;
        }

        @Override
        public void launchActivity(@NonNull Intent intent, @Nullable Bundle options) {
            launchIntent = intent;
            this.bundle = options;
        }

        @Nullable
        public Intent getLaunchIntent() {
            return launchIntent;
        }

        @NonNull
        public Bundle getBundle() {
            return bundle;
        }

        @Override
        public void moveActivity(@NonNull Activity activity) {
            this.activity = activity;
        }

        @Nullable
        public Activity getActivity() {
            return activity;
        }

        @NonNull
        @Override
        public Node getNode() {
            return node;
        }

        @Override
        public void setWindowBounds(@NonNull Rect windowBounds) {
            bounds = windowBounds;
        }

        @Nullable
        public Rect getBounds() {
            return bounds;
        }

        @Override
        public void delete() {
            isDeleted = true;
        }

        public boolean isDeleted() {
            return isDeleted;
        }
    }

    /** Fake input event. */
    public static class FakeInputEvent implements InputEvent {
        int source;
        int pointerType;
        long timestamp;
        Vec3 origin;
        Vec3 direction;
        FakeHitInfo hitInfo;
        FakeHitInfo secondaryHitInfo;
        int dispatchFlags;
        int action;

        @Override
        public int getSource() {
            return source;
        }

        @Override
        public int getPointerType() {
            return pointerType;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        @NonNull
        public Vec3 getOrigin() {
            return origin;
        }

        @Override
        @NonNull
        public Vec3 getDirection() {
            return direction;
        }

        @Override
        @Nullable
        public HitInfo getHitInfo() {
            return hitInfo;
        }

        @Override
        @Nullable
        public HitInfo getSecondaryHitInfo() {
            return secondaryHitInfo;
        }

        @Override
        public int getDispatchFlags() {
            return dispatchFlags;
        }

        @Override
        public int getAction() {
            return action;
        }

        public void setDispatchFlags(int dispatchFlags) {
            this.dispatchFlags = dispatchFlags;
        }

        public void setOrigin(@NonNull Vec3 origin) {
            this.origin = origin;
        }

        public void setDirection(@NonNull Vec3 direction) {
            this.direction = direction;
        }

        public void setFakeHitInfo(@NonNull FakeHitInfo hitInfo) {
            this.hitInfo = hitInfo;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        /** Fake hit info. */
        public static class FakeHitInfo implements InputEvent.HitInfo {
            int subspaceImpressNodeId;
            Node inputNode;
            Vec3 hitPosition;
            Mat4f transform;

            @Override
            public int getSubspaceImpressNodeId() {
                return subspaceImpressNodeId;
            }

            @Override
            @NonNull
            public Node getInputNode() {
                return inputNode;
            }

            @Override
            @Nullable
            public Vec3 getHitPosition() {
                return hitPosition;
            }

            @Override
            @NonNull
            public Mat4f getTransform() {
                return transform;
            }

            public void setSubspaceImpressNodeId(int subspaceImpressNodeId) {
                this.subspaceImpressNodeId = subspaceImpressNodeId;
            }

            public void setInputNode(@NonNull Node inputNode) {
                this.inputNode = inputNode;
            }

            public void setHitPosition(@Nullable Vec3 hitPosition) {
                this.hitPosition = hitPosition;
            }

            public void setTransform(@NonNull Mat4f transform) {
                this.transform = transform;
            }
        }
    }

    /** Fake ReformOptions. */
    public static class FakeReformOptions implements ReformOptions {

        int enabledReforms;
        int reformFlags;
        int scaleWithDistanceMode = SCALE_WITH_DISTANCE_MODE_DEFAULT;
        Vec3 currentSize;
        Vec3 minimumSize;
        Vec3 maximumSize;
        float fixedAspectRatio;
        Consumer<ReformEvent> consumer;
        Executor executor;

        boolean optionsApplied = true;

        FakeReformOptions(Consumer<ReformEvent> consumer, Executor executor) {
            this.consumer = consumer;
            this.executor = executor;
        }

        @Override
        public int getEnabledReform() {
            return enabledReforms;
        }

        @Override
        @NonNull
        public ReformOptions setEnabledReform(int i) {
            enabledReforms = i;
            return this;
        }

        @Override
        public int getFlags() {
            return reformFlags;
        }

        @Override
        @NonNull
        public ReformOptions setFlags(int i) {
            optionsApplied = false;
            reformFlags = i;
            return this;
        }

        @NonNull
        @Override
        public Vec3 getCurrentSize() {
            return currentSize;
        }

        @Override
        @NonNull
        public ReformOptions setCurrentSize(@NonNull Vec3 vec3) {
            optionsApplied = false;
            currentSize = vec3;
            return this;
        }

        @NonNull
        @Override
        public Vec3 getMinimumSize() {
            return minimumSize;
        }

        @Override
        @NonNull
        public ReformOptions setMinimumSize(@NonNull Vec3 vec3) {
            optionsApplied = false;
            minimumSize = vec3;
            return this;
        }

        @NonNull
        @Override
        public Vec3 getMaximumSize() {
            return maximumSize;
        }

        @Override
        @NonNull
        public ReformOptions setMaximumSize(@NonNull Vec3 vec3) {
            optionsApplied = false;
            maximumSize = vec3;
            return this;
        }

        @Override
        public float getFixedAspectRatio() {
            return fixedAspectRatio;
        }

        @Override
        @NonNull
        public ReformOptions setFixedAspectRatio(float fixedAspectRatio) {
            this.fixedAspectRatio = fixedAspectRatio;
            return this;
        }

        @NonNull
        @Override
        public Consumer<ReformEvent> getEventCallback() {
            return consumer;
        }

        @Override
        @NonNull
        @SuppressLint("InvalidNullabilityOverride")
        public ReformOptions setEventCallback(@NonNull Consumer<ReformEvent> consumer) {
            this.consumer = consumer;
            return this;
        }

        @NonNull
        @Override
        public Executor getEventExecutor() {
            return executor;
        }

        @Override
        @NonNull
        public ReformOptions setEventExecutor(@NonNull Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public int getScaleWithDistanceMode() {
            return scaleWithDistanceMode;
        }

        @Override
        @NonNull
        public ReformOptions setScaleWithDistanceMode(int scaleWithDistanceMode) {
            this.scaleWithDistanceMode = scaleWithDistanceMode;
            return this;
        }
    }

    /** Fake ReformEvent. */
    public static class FakeReformEvent implements ReformEvent {

        int type;
        int state;
        int id;
        Vec3 origin = new Vec3(0f, 0f, 0f);
        Vec3 initialRayOrigin = origin;
        Vec3 proposedPosition = origin;
        Vec3 ones = new Vec3(1f, 1f, 1f);
        Vec3 initialRayDirection = ones;
        Vec3 proposedScale = ones;
        Vec3 proposedSize = ones;
        Vec3 twos = new Vec3(2f, 2f, 2f);
        Vec3 currentRayOrigin = twos;
        Vec3 threes = new Vec3(3f, 3f, 3f);
        Vec3 currentRayDirection = threes;
        Quatf identity = new Quatf(0f, 0f, 0f, 1f);
        Quatf proposedOrientation = identity;

        public void setType(int type) {
            this.type = type;
        }

        public void setState(int state) {
            this.state = state;
        }

        public void setProposedPosition(@NonNull Vec3 proposedPosition) {
            this.proposedPosition = proposedPosition;
        }

        public void setProposedScale(@NonNull Vec3 proposedScale) {
            this.proposedScale = proposedScale;
        }

        public void setProposedOrientation(@NonNull Quatf proposedOrientation) {
            this.proposedOrientation = proposedOrientation;
        }

        @Override
        public int getType() {
            return type;
        }

        @Override
        public int getState() {
            return state;
        }

        @Override
        public int getId() {
            return id;
        }

        @NonNull
        @Override
        public Vec3 getInitialRayOrigin() {
            return initialRayOrigin;
        }

        @NonNull
        @Override
        public Vec3 getInitialRayDirection() {
            return initialRayDirection;
        }

        @NonNull
        @Override
        public Vec3 getCurrentRayOrigin() {
            return currentRayOrigin;
        }

        @NonNull
        @Override
        public Vec3 getCurrentRayDirection() {
            return currentRayDirection;
        }

        @NonNull
        @Override
        public Vec3 getProposedPosition() {
            return proposedPosition;
        }

        @NonNull
        @Override
        public Quatf getProposedOrientation() {
            return proposedOrientation;
        }

        @NonNull
        @Override
        public Vec3 getProposedScale() {
            return proposedScale;
        }

        @NonNull
        @Override
        public Vec3 getProposedSize() {
            return proposedSize;
        }
    }

    /** A fake implementation of the XR extensions SpatialState. */
    public static class FakeSpatialState implements SpatialState {
        Bounds bounds;
        SpatialCapabilities capabilities;
        EnvironmentVisibilityState environmentVisibilityState;
        PassthroughVisibilityState passthroughVisibilityState;

        public FakeSpatialState() {
            // Initialize params to any non-null values
            // TODO: b/370033054 - Revisit the default values for the bounds and capabilities.
            this.bounds =
                    new Bounds(
                            Float.POSITIVE_INFINITY,
                            Float.POSITIVE_INFINITY,
                            Float.POSITIVE_INFINITY);
            this.setAllSpatialCapabilities(true);
            this.environmentVisibilityState =
                    new FakeEnvironmentVisibilityState(EnvironmentVisibilityState.INVISIBLE);
            this.passthroughVisibilityState =
                    new FakePassthroughVisibilityState(PassthroughVisibilityState.DISABLED, 0.0f);
        }

        @Override
        @NonNull
        public Bounds getBounds() {
            return bounds;
        }

        public void setBounds(@NonNull Bounds bounds) {
            this.bounds = bounds;
        }

        @Override
        @NonNull
        public SpatialCapabilities getSpatialCapabilities() {
            return capabilities;
        }

        @Override
        @NonNull
        public EnvironmentVisibilityState getEnvironmentVisibility() {
            return environmentVisibilityState;
        }

        public void setEnvironmentVisibility(
                @NonNull EnvironmentVisibilityState environmentVisibilityState) {
            this.environmentVisibilityState = environmentVisibilityState;
        }

        @Override
        @NonNull
        public PassthroughVisibilityState getPassthroughVisibility() {
            return passthroughVisibilityState;
        }

        public void setPassthroughVisibility(
                @NonNull PassthroughVisibilityState passthroughVisibilityState) {
            this.passthroughVisibilityState = passthroughVisibilityState;
        }

        // Methods for tests to set the capabilities.
        public void setSpatialCapabilities(@NonNull SpatialCapabilities capabilities) {
            this.capabilities = capabilities;
        }

        public void setAllSpatialCapabilities(boolean allowAll) {
            this.capabilities =
                    new SpatialCapabilities() {
                        @Override
                        public boolean get(int capQuery) {
                            return allowAll;
                        }
                    };
        }
    }

    /** Fake XrSpatialAudioExtensions. */
    public static class FakeSpatialAudioExtensions implements XrSpatialAudioExtensions {

        @NonNull
        public final FakeSoundPoolExtensions soundPoolExtensions = new FakeSoundPoolExtensions();

        FakeAudioTrackExtensions audioTrackExtensions = new FakeAudioTrackExtensions();

        @NonNull
        public final FakeMediaPlayerExtensions mediaPlayerExtensions =
                new FakeMediaPlayerExtensions();

        @NonNull
        @Override
        public SoundPoolExtensions getSoundPoolExtensions() {
            return soundPoolExtensions;
        }

        @NonNull
        @Override
        public AudioTrackExtensions getAudioTrackExtensions() {
            return audioTrackExtensions;
        }

        public void setFakeAudioTrackExtensions(
                @NonNull FakeAudioTrackExtensions audioTrackExtensions) {
            this.audioTrackExtensions = audioTrackExtensions;
        }

        @NonNull
        @Override
        public MediaPlayerExtensions getMediaPlayerExtensions() {
            return mediaPlayerExtensions;
        }
    }

    /** Fake SoundPoolExtensions. */
    public static class FakeSoundPoolExtensions implements SoundPoolExtensions {

        int playAsPointSourceResult = 0;
        int playAsSoundFieldResult = 0;
        int sourceType = SpatializerExtensions.SOURCE_TYPE_BYPASS;

        public void setPlayAsPointSourceResult(int result) {
            playAsPointSourceResult = result;
        }

        public void setPlayAsSoundFieldResult(int result) {
            playAsSoundFieldResult = result;
        }

        public void setSourceType(@SpatializerExtensions.SourceType int sourceType) {
            this.sourceType = sourceType;
        }

        @Override
        public int playAsPointSource(
                @NonNull SoundPool soundPool,
                int soundID,
                @NonNull PointSourceAttributes attributes,
                float volume,
                int priority,
                int loop,
                float rate) {
            return playAsPointSourceResult;
        }

        @Override
        public int playAsSoundField(
                @NonNull SoundPool soundPool,
                int soundID,
                @NonNull SoundFieldAttributes attributes,
                float volume,
                int priority,
                int loop,
                float rate) {
            return playAsSoundFieldResult;
        }

        @Override
        @SpatializerExtensions.SourceType
        public int getSpatialSourceType(@NonNull SoundPool soundPool, int streamID) {
            return sourceType;
        }
    }

    /** Fake AudioTrackExtensions. */
    public static class FakeAudioTrackExtensions implements AudioTrackExtensions {

        PointSourceAttributes pointSourceAttributes;

        SoundFieldAttributes soundFieldAttributes;

        @SpatializerExtensions.SourceType int sourceType;

        @CanIgnoreReturnValue
        @Override
        @NonNull
        public AudioTrack.Builder setPointSourceAttributes(
                @NonNull AudioTrack.Builder builder, @Nullable PointSourceAttributes attributes) {
            this.pointSourceAttributes = attributes;
            return builder;
        }

        public void setPointSourceAttributes(
                @Nullable PointSourceAttributes pointSourceAttributes) {
            this.pointSourceAttributes = pointSourceAttributes;
        }

        @CanIgnoreReturnValue
        @Override
        @NonNull
        public AudioTrack.Builder setSoundFieldAttributes(
                @NonNull AudioTrack.Builder builder, @Nullable SoundFieldAttributes attributes) {
            this.soundFieldAttributes = attributes;
            return builder;
        }

        public void setSoundFieldAttributes(@Nullable SoundFieldAttributes soundFieldAttributes) {
            this.soundFieldAttributes = soundFieldAttributes;
        }

        @Override
        @Nullable
        public PointSourceAttributes getPointSourceAttributes(@NonNull AudioTrack track) {
            return pointSourceAttributes;
        }

        @Nullable
        public PointSourceAttributes getPointSourceAttributes() {
            return pointSourceAttributes;
        }

        @Override
        @Nullable
        public SoundFieldAttributes getSoundFieldAttributes(@NonNull AudioTrack track) {
            return soundFieldAttributes;
        }

        @Nullable
        public SoundFieldAttributes getSoundFieldAttributes() {
            return soundFieldAttributes;
        }

        @Override
        public int getSpatialSourceType(@NonNull AudioTrack track) {
            return sourceType;
        }

        public void setSourceType(@SpatializerExtensions.SourceType int sourceType) {
            this.sourceType = sourceType;
        }
    }

    /** Fake MediaPlayerExtensions. */
    public static class FakeMediaPlayerExtensions implements MediaPlayerExtensions {

        PointSourceAttributes pointSourceAttributes;

        SoundFieldAttributes soundFieldAttributes;

        @CanIgnoreReturnValue
        @Override
        @NonNull
        public MediaPlayer setPointSourceAttributes(
                @NonNull MediaPlayer mediaPlayer, @Nullable PointSourceAttributes attributes) {
            this.pointSourceAttributes = attributes;
            return mediaPlayer;
        }

        @CanIgnoreReturnValue
        @Override
        @NonNull
        public MediaPlayer setSoundFieldAttributes(
                @NonNull MediaPlayer mediaPlayer, @Nullable SoundFieldAttributes attributes) {
            this.soundFieldAttributes = attributes;
            return mediaPlayer;
        }

        @Nullable
        public PointSourceAttributes getPointSourceAttributes() {
            return pointSourceAttributes;
        }

        @Nullable
        public SoundFieldAttributes getSoundFieldAttributes() {
            return soundFieldAttributes;
        }
    }
}
