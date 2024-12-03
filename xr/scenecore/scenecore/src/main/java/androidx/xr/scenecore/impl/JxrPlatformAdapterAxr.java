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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceControlViewHost.SurfacePackage;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.xr.arcore.Anchor;
import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.XrExtensionsProvider;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.extensions.space.ActivityPanel;
import androidx.xr.extensions.space.ActivityPanelLaunchParameters;
import androidx.xr.extensions.space.SpatialState;
import androidx.xr.runtime.math.Pose;
import androidx.xr.scenecore.JxrPlatformAdapter;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.impl.perception.ViewProjections;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.apibindings.ImpressApi;
import com.google.ar.imp.apibindings.ImpressApiImpl;
import com.google.ar.imp.view.splitengine.ImpSplitEngine;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Implementation of JxrPlatformAdapter for AndroidXR. */
// TODO: b/322550407 - Use the Android Fluent Logger
// TODO(b/373435470): Remove "deprecation" and "UnnecessarilyFullyQualified"
@SuppressWarnings({
    "deprecation",
    "UnnecessarilyFullyQualified",
    "BanSynchronizedMethods",
    "BanConcurrentHashMap",
})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class JxrPlatformAdapterAxr implements JxrPlatformAdapter {
    private static final String TAG = "JxrPlatformAdapterAxr";
    private static final String SPLIT_ENGINE_LIBRARY_NAME = "impress_api_jni";

    private final ActivitySpaceImpl activitySpace;
    private final HeadActivityPoseImpl headActivityPose;
    private final PerceptionSpaceActivityPoseImpl perceptionSpaceActivityPose;
    private final List<CameraViewActivityPoseImpl> cameraActivityPoses = new ArrayList<>();
    private final ScheduledExecutorService executor;
    private final XrExtensions extensions;

    private final SoundPoolExtensionsWrapper soundPoolExtensionsWrapper;
    private final AudioTrackExtensionsWrapper audioTrackExtensionsWrapper;
    private final MediaPlayerExtensionsWrapper mediaPlayerExtensionsWrapper;

    private final PerceptionLibrary perceptionLibrary;
    private final SpatialEnvironmentImpl environment;
    private final boolean useSplitEngine;
    private final Node taskWindowLeashNode;
    private final int openXrReferenceSpaceType;
    private final EntityManager entityManager;
    private final PanelEntity mainPanelEntity;
    private final ImpressApi impressApi;
    private final Map<Consumer<SpatialCapabilities>, Executor> spatialCapabilitiesChangedListeners =
            new ConcurrentHashMap<>();

    @Nullable private Activity activity;
    private SplitEngineSubspaceManager splitEngineSubspaceManager;
    private ImpSplitEngineRenderer splitEngineRenderer;
    private boolean frameLoopStarted;

    // TODO b/373481538: remove lazy initialization once XR Extensions bug is fixed. This will allow
    // us to remove the lazySpatialStateProvider instance and pass the spatialState directly.
    private final AtomicReference<SpatialState> spatialState = new AtomicReference<>(null);

    // Returns the currently-known spatial state, or fetches it from the extensions if it has never
    // been set. The spatial state is kept updated in the SpatialStateCallback.
    private final Supplier<SpatialState> lazySpatialStateProvider;

    private JxrPlatformAdapterAxr(
            Activity activity,
            ScheduledExecutorService executor,
            XrExtensions extensions,
            @Nullable ImpressApi impressApi,
            EntityManager entityManager,
            PerceptionLibrary perceptionLibrary,
            @Nullable SplitEngineSubspaceManager subspaceManager,
            @Nullable ImpSplitEngineRenderer renderer,
            Node rootSceneNode,
            Node taskWindowLeashNode,
            boolean useSplitEngine) {
        this.activity = activity;
        this.executor = executor;
        this.extensions = extensions;

        this.lazySpatialStateProvider =
                () ->
                        spatialState.updateAndGet(
                                oldSpatialState -> {
                                    if (oldSpatialState == null) {
                                        oldSpatialState = this.extensions.getSpatialState(activity);
                                    }
                                    return oldSpatialState;
                                });
        setSpatialStateCallback();

        soundPoolExtensionsWrapper =
                new SoundPoolExtensionsWrapperImpl(
                        extensions.getXrSpatialAudioExtensions().getSoundPoolExtensions());
        audioTrackExtensionsWrapper =
                new AudioTrackExtensionsWrapperImpl(
                        extensions.getXrSpatialAudioExtensions().getAudioTrackExtensions(),
                        entityManager);
        mediaPlayerExtensionsWrapper =
                new MediaPlayerExtensionsWrapperImpl(
                        extensions.getXrSpatialAudioExtensions().getMediaPlayerExtensions());
        this.entityManager = entityManager;
        this.perceptionLibrary = perceptionLibrary;
        this.taskWindowLeashNode = taskWindowLeashNode;
        this.environment =
                new SpatialEnvironmentImpl(
                        activity,
                        extensions,
                        rootSceneNode,
                        lazySpatialStateProvider,
                        useSplitEngine);
        this.activitySpace =
                new ActivitySpaceImpl(
                        rootSceneNode,
                        extensions,
                        entityManager,
                        lazySpatialStateProvider,
                        executor);
        this.headActivityPose =
                new HeadActivityPoseImpl(
                        activitySpace,
                        (AndroidXrEntity) getActivitySpaceRootImpl(),
                        perceptionLibrary);
        this.perceptionSpaceActivityPose =
                new PerceptionSpaceActivityPoseImpl(
                        activitySpace, (AndroidXrEntity) getActivitySpaceRootImpl());
        this.cameraActivityPoses.add(
                new CameraViewActivityPoseImpl(
                        CameraViewActivityPose.CAMERA_TYPE_LEFT_EYE,
                        activitySpace,
                        (AndroidXrEntity) getActivitySpaceRootImpl(),
                        perceptionLibrary));
        this.cameraActivityPoses.add(
                new CameraViewActivityPoseImpl(
                        CameraViewActivityPose.CAMERA_TYPE_RIGHT_EYE,
                        activitySpace,
                        (AndroidXrEntity) getActivitySpaceRootImpl(),
                        perceptionLibrary));
        this.useSplitEngine = useSplitEngine;
        this.openXrReferenceSpaceType = extensions.getOpenXrWorldSpaceType();

        this.mainPanelEntity =
                new MainPanelEntityImpl(
                        activity, taskWindowLeashNode, extensions, entityManager, executor);
        this.mainPanelEntity.setParent(activitySpace);

        // TODO:b/377918731 - Move this logic into factories and inject SE into the constructor
        if (impressApi == null) {
            // TODO: b/370116937) - Check against useSplitEngine as well and don't load this if
            //                      SplitEngine is disabled.
            this.impressApi = new ImpressApiImpl();
        } else {
            this.impressApi = impressApi;
        }

        if (useSplitEngine && subspaceManager == null && renderer == null) {
            ImpSplitEngine.SplitEngineSetupParams impApiSetupParams =
                    new ImpSplitEngine.SplitEngineSetupParams();
            impApiSetupParams.jniLibraryName = SPLIT_ENGINE_LIBRARY_NAME;
            this.splitEngineRenderer = ImpSplitEngineRenderer.create(activity, impApiSetupParams);
            startRenderer();
            this.splitEngineSubspaceManager =
                    new SplitEngineSubspaceManager(
                            this.splitEngineRenderer,
                            rootSceneNode,
                            taskWindowLeashNode,
                            SPLIT_ENGINE_LIBRARY_NAME);
            this.impressApi.setup(splitEngineRenderer.getView());
            environment.onSplitEngineReady(this.splitEngineSubspaceManager, this.impressApi);
        }
    }

    @NonNull
    public static JxrPlatformAdapterAxr create(
            @NonNull Activity activity, @NonNull ScheduledExecutorService executor) {
        return create(
                activity,
                executor,
                XrExtensionsProvider.getXrExtensions(),
                null,
                new EntityManager(),
                new PerceptionLibrary(),
                null,
                null,
                /* useSplitEngine= */ true);
    }

    @NonNull
    public static JxrPlatformAdapterAxr create(
            @NonNull Activity activity,
            @NonNull ScheduledExecutorService executor,
            boolean useSplitEngine) {
        return create(
                activity,
                executor,
                XrExtensionsProvider.getXrExtensions(),
                null,
                new EntityManager(),
                new PerceptionLibrary(),
                null,
                null,
                useSplitEngine);
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static JxrPlatformAdapterAxr create(
            @NonNull Activity activity,
            @NonNull ScheduledExecutorService executor,
            @NonNull Node rootSceneNode,
            @NonNull Node taskWindowLeashNode) {
        return create(
                activity,
                executor,
                XrExtensionsProvider.getXrExtensions(),
                null,
                new EntityManager(),
                new PerceptionLibrary(),
                null,
                null,
                rootSceneNode,
                taskWindowLeashNode,
                /* useSplitEngine= */ false);
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static JxrPlatformAdapterAxr create(
            @NonNull Activity activity,
            @NonNull ScheduledExecutorService executor,
            @NonNull XrExtensions extensions,
            @Nullable ImpressApi impressApi,
            @NonNull PerceptionLibrary perceptionLibrary,
            @Nullable SplitEngineSubspaceManager splitEngineSubspaceManager,
            @Nullable ImpSplitEngineRenderer splitEngineRenderer) {
        return create(
                activity,
                executor,
                extensions,
                impressApi,
                new EntityManager(),
                perceptionLibrary,
                splitEngineSubspaceManager,
                splitEngineRenderer,
                /* useSplitEngine= */ false);
    }

    static JxrPlatformAdapterAxr create(
            Activity activity,
            ScheduledExecutorService executor,
            XrExtensions extensions,
            ImpressApi impressApi,
            EntityManager entityManager,
            PerceptionLibrary perceptionLibrary,
            SplitEngineSubspaceManager splitEngineSubspaceManager,
            ImpSplitEngineRenderer splitEngineRenderer,
            boolean useSplitEngine) {
        Node rootSceneNode = extensions.createNode();
        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction.setName(rootSceneNode, "RootSceneNode").apply();
        }
        Log.i(TAG, "Impl Node for task $activity.taskId is root scene node: " + rootSceneNode);
        Node taskWindowLeashNode = extensions.createNode();
        // TODO: b/376934871 - Check async results.
        extensions.attachSpatialScene(
                activity, rootSceneNode, taskWindowLeashNode, (result) -> {}, Runnable::run);
        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction
                    .setParent(taskWindowLeashNode, rootSceneNode)
                    .setName(taskWindowLeashNode, "TaskWindowLeashNode")
                    .apply();
        }
        return create(
                activity,
                executor,
                extensions,
                impressApi,
                entityManager,
                perceptionLibrary,
                splitEngineSubspaceManager,
                splitEngineRenderer,
                rootSceneNode,
                taskWindowLeashNode,
                useSplitEngine);
    }

    static JxrPlatformAdapterAxr create(
            @NonNull Activity activity,
            @NonNull ScheduledExecutorService executor,
            @NonNull XrExtensions extensions,
            @Nullable ImpressApi impressApi,
            @NonNull EntityManager entityManager,
            @NonNull PerceptionLibrary perceptionLibrary,
            @Nullable SplitEngineSubspaceManager splitEngineSubspaceManager,
            @Nullable ImpSplitEngineRenderer splitEngineRenderer,
            @NonNull Node rootSceneNode,
            @NonNull Node taskWindowLeashNode,
            boolean useSplitEngine) {
        JxrPlatformAdapterAxr runtime =
                new JxrPlatformAdapterAxr(
                        activity,
                        executor,
                        extensions,
                        impressApi,
                        entityManager,
                        perceptionLibrary,
                        splitEngineSubspaceManager,
                        splitEngineRenderer,
                        rootSceneNode,
                        taskWindowLeashNode,
                        useSplitEngine);

        Log.i(TAG, "Initing perception library soon");
        runtime.initPerceptionLibrary();
        return runtime;
    }

    private static GltfModelResourceImpl getModelResourceFromToken(
            androidx.xr.extensions.asset.GltfModelToken token) {
        return new GltfModelResourceImpl(token);
    }

    private static GltfModelResourceImplSplitEngine getModelResourceFromTokenSplitEngine(
            long token) {
        return new GltfModelResourceImplSplitEngine(token);
    }

    private static ExrImageResourceImpl getExrImageResourceFromToken(
            androidx.xr.extensions.asset.EnvironmentToken token) {
        return new ExrImageResourceImpl(token);
    }

    // Note that this is called on the Activity's UI thread so we should be careful to not  block
    // it.
    // It is synchronized because we assume this.spatialState cannot be updated elsewhere during the
    // execution of this method.
    @VisibleForTesting
    synchronized void onSpatialStateChanged(
            @NonNull androidx.xr.extensions.space.SpatialState newSpatialState) {
        SpatialState previousSpatialState = this.spatialState.getAndSet(newSpatialState);

        boolean spatialCapabilitiesChanged =
                previousSpatialState == null
                        || !newSpatialState
                                .getSpatialCapabilities()
                                .equals(previousSpatialState.getSpatialCapabilities());

        boolean hasBoundsChanged =
                previousSpatialState == null
                        || !newSpatialState.getBounds().equals(previousSpatialState.getBounds());

        EnumSet<SpatialEnvironmentImpl.ChangedSpatialStates> changedSpatialStates =
                environment.setSpatialState(newSpatialState);
        boolean environmentVisibilityChanged =
                changedSpatialStates.contains(
                        SpatialEnvironmentImpl.ChangedSpatialStates.ENVIRONMENT_CHANGED);
        boolean passthroughVisibilityChanged =
                changedSpatialStates.contains(
                        SpatialEnvironmentImpl.ChangedSpatialStates.PASSTHROUGH_CHANGED);

        // Fire the state change events only after all the states have been updated.
        if (environmentVisibilityChanged) {
            environment.fireOnSpatialEnvironmentChangedEvent(
                    environment.isSpatialEnvironmentPreferenceActive());
        }
        if (passthroughVisibilityChanged) {
            environment.firePassthroughOpacityChangedEvent(
                    environment.getCurrentPassthroughOpacity());
        }

        if (spatialCapabilitiesChanged) {
            SpatialCapabilities spatialCapabilities =
                    RuntimeUtils.convertSpatialCapabilities(
                            newSpatialState.getSpatialCapabilities());

            spatialCapabilitiesChangedListeners.forEach(
                    (listener, executor) ->
                            executor.execute(() -> listener.accept(spatialCapabilities)));
        }

        if (hasBoundsChanged) {
            activitySpace.onBoundsChanged(newSpatialState.getBounds());
        }
    }

    private void setSpatialStateCallback() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        extensions.registerSpatialStateCallback(
                activity, this::onSpatialStateChanged, mainHandler::post);
    }

    private synchronized void initPerceptionLibrary() {
        if (perceptionLibrary.getSession() != null) {
            Log.w(TAG, "Cannot init perception session, already initialized.");
            return;
        }
        ListenableFuture<Session> sessionFuture =
                perceptionLibrary.initSession(activity, openXrReferenceSpaceType, executor);
        Objects.requireNonNull(sessionFuture)
                .addListener(
                        () -> {
                            try {
                                sessionFuture.get();
                            } catch (Exception e) {
                                if (e instanceof InterruptedException) {
                                    Thread.currentThread().interrupt();
                                }
                                Log.e(
                                        TAG,
                                        "Failed to init perception session with error: "
                                                + e.getMessage());
                            }
                        },
                        executor);
    }

    @Override
    @NonNull
    public SpatialCapabilities getSpatialCapabilities() {
        return RuntimeUtils.convertSpatialCapabilities(
                lazySpatialStateProvider.get().getSpatialCapabilities());
    }

    @Override
    public void addSpatialCapabilitiesChangedListener(
            @NonNull Executor executor, @NonNull Consumer<SpatialCapabilities> listener) {
        spatialCapabilitiesChangedListeners.put(listener, executor);
    }

    @Override
    public void removeSpatialCapabilitiesChangedListener(
            @NonNull Consumer<SpatialCapabilities> listener) {
        spatialCapabilitiesChangedListeners.remove(listener);
    }

    @Override
    @NonNull
    public LoggingEntity createLoggingEntity(@NonNull Pose pose) {
        LoggingEntityImpl entity = new LoggingEntityImpl();
        entity.setPose(pose);
        return entity;
    }

    @Override
    @NonNull
    public SpatialEnvironment getSpatialEnvironment() {
        return environment;
    }

    @Override
    @NonNull
    public ActivitySpace getActivitySpace() {
        return activitySpace;
    }

    @Override
    @Nullable
    public HeadActivityPose getHeadActivityPose() {
        // If it is unable to retrieve a pose the head in not yet loaded in openXR so return null.
        if (headActivityPose.getPoseInOpenXrReferenceSpace() == null) {
            return null;
        }
        return headActivityPose;
    }

    @Override
    @Nullable
    public CameraViewActivityPose getCameraViewActivityPose(
            @CameraViewActivityPose.CameraType int cameraType) {
        CameraViewActivityPoseImpl cameraViewActivityPose = null;
        if (cameraType == CameraViewActivityPose.CAMERA_TYPE_LEFT_EYE) {
            cameraViewActivityPose = cameraActivityPoses.get(0);
        } else if (cameraType == CameraViewActivityPose.CAMERA_TYPE_RIGHT_EYE) {
            cameraViewActivityPose = cameraActivityPoses.get(1);
        }
        // If it is unable to retrieve a pose the camera in not yet loaded in openXR so return null.
        if (cameraViewActivityPose == null
                || cameraViewActivityPose.getPoseInOpenXrReferenceSpace() == null) {
            return null;
        }
        return cameraViewActivityPose;
    }

    @Override
    @NonNull
    public PerceptionSpaceActivityPose getPerceptionSpaceActivityPose() {
        return perceptionSpaceActivityPose;
    }

    // TODO(b/349180723): Refactor to a streaming based approach.
    @Nullable
    public Pose getHeadPoseInOpenXrUnboundedSpace() {
        Session session = perceptionLibrary.getSession();
        if (session == null) {
            Log.w(TAG, "Perception session is uninitialized, returning null head pose.");
            return null;
        }
        return RuntimeUtils.fromPerceptionPose(Objects.requireNonNull(session.getHeadPose()));
    }

    @Nullable
    public ViewProjections getStereoViewsInOpenXrUnboundedSpace() {
        Session session = perceptionLibrary.getSession();
        if (session == null) {
            Log.w(TAG, "Perception session is uninitialized, returning null head pose.");
            return null;
        }
        return session.getStereoViews();
    }

    @Override
    @NonNull
    public Entity getActivitySpaceRootImpl() {
        // Trivially returns the activity space for now, but it could be updated to return any other
        // singleton space entity. That space entity will define the world space origin of the SDK
        // and
        // will be the default parent for new content entities.

        return activitySpace;
    }

    @Override
    public void requestFullSpaceMode() {
        // TODO: b/376934871 - Check async results.
        extensions.requestFullSpaceMode(
                activity, /* requestEnter= */ true, (result) -> {}, Runnable::run);
    }

    @Override
    public void requestHomeSpaceMode() {
        // TODO: b/376934871 - Check async results.
        extensions.requestFullSpaceMode(
                activity, /* requestEnter= */ false, (result) -> {}, Runnable::run);
    }

    // TODO: b/374345896 - Delete this method once we've finalized the SplitEngine migration.
    @SuppressWarnings({
        "AndroidJdkLibsChecker",
        "RestrictTo",
        "FutureReturnValueIgnored",
        "AsyncSuffixFuture"
    })
    @Override
    @Nullable
    public ListenableFuture<GltfModelResource> loadGltfByAssetName(@NonNull String assetName) {
        ResolvableFuture<GltfModelResource> gltfModelResourceFuture = ResolvableFuture.create();
        InputStream asset;
        try {
            asset = activity.getAssets().open(assetName);
        } catch (Exception e) {
            Log.w(TAG, "Could not open asset with error: " + e.getMessage());
            return null;
        }

        CompletableFuture<androidx.xr.extensions.asset.GltfModelToken> tokenFuture;
        try {
            tokenFuture = extensions.loadGltfModel(asset, asset.available(), 0, assetName);
            // Unfortunately, there is no way to avoid "leaking" this future, since we want to
            // return a
            // ListenableFuture. This should be a short lived problem since clients should be using
            // loadGltfByAssetNameSplitEngine() if they have SplitEngine enabled.
            tokenFuture.thenApply(
                    token -> gltfModelResourceFuture.set(getModelResourceFromToken(token)));
        } catch (Exception e) {
            Log.w(TAG, "Could not load glTF model with error: " + e.getMessage());
            return null;
        }

        return gltfModelResourceFuture;
    }

    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    @Nullable
    public ListenableFuture<GltfModelResource> loadGltfByAssetNameSplitEngine(
            @NonNull String name) {
        ResolvableFuture<GltfModelResource> gltfModelResourceFuture = ResolvableFuture.create();
        // TODO:b/374216912 - Consider calling setFuture() here to catch if the application calls
        // cancel() on the return value from this function, so we can propagate the cancelation
        // message
        // to the Impress API.

        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        ListenableFuture<Long> gltfTokenFuture;
        try {
            gltfTokenFuture = impressApi.loadGltfModel(name);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to load glTF model with error: " + e.getMessage());
            // TODO:b/375070346 - make this method NonNull and set the gltfModelResourceFuture to an
            // exception and return that.
            return null;
        }

        gltfTokenFuture.addListener(
                () -> {
                    try {
                        long gltfToken = gltfTokenFuture.get();
                        gltfModelResourceFuture.set(
                                getModelResourceFromTokenSplitEngine(gltfToken));
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        Log.e(TAG, "Failed to load glTF model with error: " + e.getMessage());
                        gltfModelResourceFuture.setException(e);
                    }
                },
                // It's convenient for the main application for us to dispatch their listeners on
                // the main
                // thread, because they are required to call back to Impress from there, and it's
                // likely
                // that they will want to call back into the SDK to create entities from within a
                // listener.
                // We defensively post to the main thread here, but in practice this should not
                // cause a
                // thread hop because the Impress API already dispatches its callbacks to the main
                // thread.
                activity::runOnUiThread);
        return gltfModelResourceFuture;
    }

    // TODO: b/376504646 - Delete this method once we've migrated to a SplitEngine backed skybox.
    @SuppressWarnings({
        "AndroidJdkLibsChecker",
        "RestrictTo",
        "FutureReturnValueIgnored",
        "AsyncSuffixFuture"
    })
    @Override
    @Nullable
    public ListenableFuture<ExrImageResource> loadExrImageByAssetName(@NonNull String assetName) {
        ResolvableFuture<ExrImageResource> exrImageResourceFuture = ResolvableFuture.create();
        InputStream asset;
        try {
            // NOTE: extensions.loadEnvironment expects a .EXR file.
            asset = activity.getAssets().open(assetName);
        } catch (Exception e) {
            Log.w(TAG, "Could not open asset with error: " + e.getMessage());
            return null;
        }

        CompletableFuture<androidx.xr.extensions.asset.EnvironmentToken> tokenFuture;
        try {
            // NOTE: At the moment, extensions.loadEnvironment expects a .EXR file explicitly. This
            //       will need to be updated as support for GLTF environment geometry is added by
            //       the system.
            tokenFuture = extensions.loadEnvironment(asset, asset.available(), 0, assetName);
            // Unfortunately, there is no way to avoid "leaking" this future, since we want to
            // return a
            // ListenableFuture. This method should be deleted soon, once the SplitEngine backed
            // skybox
            // is ready.
            tokenFuture.thenApply(
                    token -> exrImageResourceFuture.set(getExrImageResourceFromToken(token)));
        } catch (Exception e) {
            Log.i(TAG, "Could not load ExrImage with error: " + e.getMessage());
            return null;
        }
        Log.w(TAG, "Loaded asset: " + assetName);

        return exrImageResourceFuture;
    }

    @Override
    @NonNull
    public GltfEntity createGltfEntity(
            @NonNull Pose pose, @NonNull GltfModelResource model, @Nullable Entity parentEntity) {
        if (useSplitEngine && model instanceof GltfModelResourceImplSplitEngine) {
            return createGltfEntitySplitEngine(pose, model, parentEntity);
        }
        if (parentEntity == null) {
            throw new IllegalArgumentException("parentEntity cannot be null");
        }
        if (!(model instanceof GltfModelResourceImpl)) {
            throw new IllegalArgumentException("GltfModelResource is not a GltfModelResourceImpl");
        }
        GltfEntity entity =
                new GltfEntityImpl(
                        (GltfModelResourceImpl) model,
                        parentEntity,
                        extensions,
                        entityManager,
                        executor);
        entity.setPose(pose);
        return entity;
    }

    @Override
    @NonNull
    public StereoSurfaceEntity createStereoSurfaceEntity(
            @StereoSurfaceEntity.StereoMode int stereoMode,
            @NonNull Dimensions dimensions,
            @NonNull Pose pose,
            @NonNull Entity parentEntity) {
        if (useSplitEngine) {
            return createStereoSurfaceEntitySplitEngine(stereoMode, dimensions, pose, parentEntity);
        } else {
            // TODO: b/377983872 - Throw UnsupportedOperationException here.
            Log.w(TAG, "stereo surface only works with split engine.");
            return new StereoSurfaceEntityImpl(parentEntity, extensions, entityManager, executor);
        }
    }

    @Override
    @NonNull
    public PanelEntity createPanelEntity(
            @NonNull Pose pose,
            @NonNull View view,
            @NonNull PixelDimensions surfaceDimensionsPx,
            @NonNull Dimensions dimensions,
            @NonNull String name,
            @SuppressWarnings("ContextFirst") @NonNull Context context,
            @NonNull Entity parent) {

        // TODO(b/352630140):  Move this into a static factory method of PanelEntityImpl.

        SurfaceControlViewHost surfaceControlViewHost =
                new SurfaceControlViewHost(
                        context, Objects.requireNonNull(context.getDisplay()), new Binder());
        surfaceControlViewHost.setView(view, surfaceDimensionsPx.width, surfaceDimensionsPx.height);
        Node node = extensions.createNode();
        SurfacePackage surfacePackage =
                Objects.requireNonNull(surfaceControlViewHost.getSurfacePackage());

        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction
                    .setName(node, name)
                    .setSurfacePackage(node, surfacePackage)
                    .setWindowBounds(
                            surfacePackage, surfaceDimensionsPx.width, surfaceDimensionsPx.height)
                    .setVisibility(node, true)
                    // Corner radius must be zeroed as the value is inherited from the parent node
                    // otherwise.
                    .setCornerRadius(node, 0f)
                    .apply();
        }
        surfacePackage.release();

        PanelEntity panelEntity =
                new PanelEntityImpl(
                        node,
                        extensions,
                        entityManager,
                        surfaceControlViewHost,
                        surfaceDimensionsPx,
                        executor);
        panelEntity.setParent(parent);
        panelEntity.setPose(pose);
        return panelEntity;
    }

    @Override
    @NonNull
    public PanelEntity getMainPanelEntity() {
        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction.setVisibility(taskWindowLeashNode, true).apply();
        }
        return mainPanelEntity;
    }

    @Override
    @SuppressLint("ExecutorRegistration")
    @NonNull
    public InteractableComponent createInteractableComponent(
            @NonNull Executor executor, @NonNull InputEventListener listener) {
        return new InteractableComponentImpl(executor, listener);
    }

    @Override
    @NonNull
    public MovableComponent createMovableComponent(
            boolean systemMovable,
            boolean scaleInZ,
            @NonNull Set<AnchorPlacement> anchorPlacement,
            boolean shouldDisposeParentAnchor) {
        return new MovableComponentImpl(
                systemMovable,
                scaleInZ,
                anchorPlacement,
                shouldDisposeParentAnchor,
                perceptionLibrary,
                extensions,
                activitySpace,
                (AndroidXrEntity) getActivitySpaceRootImpl(),
                perceptionSpaceActivityPose,
                entityManager,
                new PanelShadowRenderer(
                        activitySpace, perceptionSpaceActivityPose, activity, extensions),
                executor);
    }

    @Override
    @NonNull
    public AnchorPlacement createAnchorPlacementForPlanes(
            @NonNull Set<PlaneType> planeTypeFilter,
            @NonNull Set<PlaneSemantic> planeSemanticFilter) {
        AnchorPlacementImpl anchorPlacement = new AnchorPlacementImpl();
        anchorPlacement.planeTypeFilter.addAll(planeTypeFilter);
        anchorPlacement.planeSemanticFilter.addAll(planeSemanticFilter);
        return anchorPlacement;
    }

    @Override
    @NonNull
    public ResizableComponent createResizableComponent(
            @NonNull Dimensions minimumSize, @NonNull Dimensions maximumSize) {
        return new ResizableComponentImpl(executor, extensions, minimumSize, maximumSize);
    }

    @Override
    @SuppressLint("ExecutorRegistration")
    @SuppressWarnings("ExecutorRegistration")
    @NonNull
    public PointerCaptureComponent createPointerCaptureComponent(
            @NonNull Executor executor,
            @NonNull PointerCaptureComponent.StateListener stateListener,
            @NonNull InputEventListener inputListener) {
        return new PointerCaptureComponentImpl(executor, stateListener, inputListener);
    }

    @Override
    @NonNull
    public ActivityPanelEntity createActivityPanelEntity(
            @NonNull Pose pose,
            @NonNull PixelDimensions windowBoundsPx,
            @NonNull String name,
            @NonNull Activity hostActivity,
            @NonNull Entity parent) {

        // TODO(b/352630140): Move this into a static factory method of ActivityPanelEntityImpl.
        Rect windowBoundsRect = new Rect(0, 0, windowBoundsPx.width, windowBoundsPx.height);
        ActivityPanel activityPanel =
                extensions.createActivityPanel(
                        hostActivity, new ActivityPanelLaunchParameters(windowBoundsRect));
        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction
                    .setVisibility(activityPanel.getNode(), true)
                    .setName(activityPanel.getNode(), name)
                    // Corner radius must be zeroed as the value is inherited from the parent node
                    // otherwise.
                    .setCornerRadius(activityPanel.getNode(), 0f)
                    .apply();
        }
        activityPanel.setWindowBounds(windowBoundsRect);
        ActivityPanelEntityImpl activityPanelEntity =
                new ActivityPanelEntityImpl(
                        activityPanel.getNode(),
                        extensions,
                        entityManager,
                        activityPanel,
                        windowBoundsPx,
                        executor);
        activityPanelEntity.setParent(parent);
        activityPanelEntity.setPose(pose);
        return activityPanelEntity;
    }

    @Override
    @NonNull
    public AnchorEntity createAnchorEntity(
            @NonNull Dimensions bounds,
            @NonNull PlaneType planeType,
            @NonNull PlaneSemantic planeSemantic,
            @NonNull Duration searchTimeout) {
        Node node = extensions.createNode();
        return AnchorEntityImpl.createSemanticAnchor(
                node,
                bounds,
                planeType,
                planeSemantic,
                searchTimeout,
                getActivitySpace(),
                getActivitySpaceRootImpl(),
                extensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    @Override
    @NonNull
    public AnchorEntity createAnchorEntity(@NonNull Anchor anchor) {
        Node node = extensions.createNode();
        return AnchorEntityImpl.createAnchorFromPerceptionAnchor(
                node,
                anchor,
                getActivitySpace(),
                getActivitySpaceRootImpl(),
                extensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    @Override
    @NonNull
    public Entity createEntity(@NonNull Pose pose, @NonNull String name, @NonNull Entity parent) {
        Node node = extensions.createNode();
        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction.setName(node, name).apply();
        }

        // This entity is used to back JXR Core's ContentlessEntity.
        Entity entity = new AndroidXrEntity(node, extensions, entityManager, executor) {};
        entity.setParent(parent);
        entity.setPose(pose);
        return entity;
    }

    @Override
    @NonNull
    public AnchorEntity createPersistedAnchorEntity(
            @NonNull UUID uuid, @NonNull Duration searchTimeout) {
        Node node = extensions.createNode();
        return AnchorEntityImpl.createPersistedAnchor(
                node,
                uuid,
                searchTimeout,
                getActivitySpace(),
                getActivitySpaceRootImpl(),
                extensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    @Override
    public boolean unpersistAnchor(@NonNull UUID uuid) {
        Session session = perceptionLibrary.getSession();
        if (session == null) {
            Log.w(TAG, "Cannot unpersist anchor, perception session is not initialized.");
            return false;
        }
        return session.unpersistAnchor(uuid);
    }

    @Override
    @NonNull
    public Bundle setFullSpaceMode(@NonNull Bundle bundle) {
        return extensions.setFullSpaceMode(bundle);
    }

    @Override
    @NonNull
    public Bundle setFullSpaceModeWithEnvironmentInherited(@NonNull Bundle bundle) {
        return extensions.setFullSpaceModeWithEnvironmentInherited(bundle);
    }

    @Override
    public void setPreferredAspectRatio(@NonNull Activity activity, float preferredRatio) {
        // TODO: b/376934871 - Check async results.
        extensions.setPreferredAspectRatio(activity, preferredRatio, (result) -> {}, Runnable::run);
    }

    @Override
    public void startRenderer() {
        if (splitEngineRenderer == null || frameLoopStarted) {
            return;
        }
        frameLoopStarted = true;
        splitEngineRenderer.startFrameLoop();
    }

    @Override
    public void stopRenderer() {
        if (splitEngineRenderer == null || !frameLoopStarted) {
            return;
        }
        frameLoopStarted = false;
        splitEngineRenderer.stopFrameLoop();
    }

    @Override
    public void dispose() {
        Log.i(TAG, "Disposing resources");
        environment.dispose();
        extensions.clearSpatialStateCallback(activity);
        // TODO: b/376934871 - Check async results.
        extensions.detachSpatialScene(activity, (result) -> {}, Runnable::run);
        activity = null;
        entityManager.getAllEntities().forEach(Entity::dispose);
        entityManager.clear();
        if (splitEngineRenderer != null && splitEngineSubspaceManager != null) {
            splitEngineRenderer.destroy();
            splitEngineSubspaceManager.destroy();
        }
    }

    public void setSplitEngineSubspaceManager(
            @Nullable SplitEngineSubspaceManager splitEngineSubspaceManager) {
        this.splitEngineSubspaceManager = splitEngineSubspaceManager;
    }

    @Override
    @NonNull
    public SoundPoolExtensionsWrapper getSoundPoolExtensionsWrapper() {
        return soundPoolExtensionsWrapper;
    }

    @Override
    @NonNull
    public AudioTrackExtensionsWrapper getAudioTrackExtensionsWrapper() {
        return audioTrackExtensionsWrapper;
    }

    @Override
    @NonNull
    public MediaPlayerExtensionsWrapper getMediaPlayerExtensionsWrapper() {
        return mediaPlayerExtensionsWrapper;
    }

    /**
     * Get the underlying OpenXR session that backs perception.
     *
     * <p>The OpenXR session is created on JXR's primary thread so this may return {@code
     * XR_NULL_HANDLE} for a few frames at startup.
     *
     * @return the OpenXR XrSession, encoded in a jlong
     */
    public long getNativeSession() {
        Session session = perceptionLibrary.getSession();
        if (session == null) {
            Log.w(TAG, "Perception session is uninitilazied, returning XR_NULL_HANDLE");
            return Session.XR_NULL_HANDLE;
        }

        long nativeSession = session.getNativeSession();
        if (nativeSession == Session.XR_NULL_HANDLE) {
            Log.w(TAG, "Perception session initialized, but native session not yet created");
            return Session.XR_NULL_HANDLE;
        }

        return nativeSession;
    }

    /**
     * Get the underlying OpenXR instance that backs perception.
     *
     * <p>The OpenXR instance is created on JXR's primary thread so this may return {@code
     * XR_NULL_HANDLE} for a few frames at startup.
     *
     * @return the OpenXR XrInstance, encoded in a jlong
     */
    public long getNativeInstance() {
        Session session = perceptionLibrary.getSession();
        if (session == null) {
            Log.w(TAG, "Perception session is uninitilazied, returning XR_NULL_HANDLE");
            return Session.XR_NULL_HANDLE;
        }

        long nativeInstance = session.getNativeInstance();
        if (nativeInstance == Session.XR_NULL_HANDLE) {
            Log.w(TAG, "Perception session initialized, but native instance not yet created");
            return Session.XR_NULL_HANDLE;
        }

        return nativeInstance;
    }

    private StereoSurfaceEntity createStereoSurfaceEntitySplitEngine(
            @StereoSurfaceEntity.StereoMode int stereoMode,
            Dimensions dimensions,
            Pose pose,
            @NonNull Entity parentEntity) {

        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        StereoSurfaceEntity entity =
                new StereoSurfaceEntitySplitEngineImpl(
                        parentEntity,
                        impressApi,
                        splitEngineSubspaceManager,
                        extensions,
                        entityManager,
                        executor,
                        stereoMode);
        entity.setPose(pose);
        entity.setDimensions(dimensions);
        return entity;
    }

    private GltfEntity createGltfEntitySplitEngine(
            Pose pose, GltfModelResource model, Entity parentEntity) {

        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }
        if (parentEntity == null) {
            throw new IllegalArgumentException("parentEntity cannot be null");
        }
        if (!(model instanceof GltfModelResourceImplSplitEngine)) {
            throw new IllegalArgumentException(
                    "GltfModelResource is not a GltfModelResourceImplSplitEngine");
        }
        GltfEntity entity =
                new GltfEntityImplSplitEngine(
                        (GltfModelResourceImplSplitEngine) model,
                        parentEntity,
                        impressApi,
                        splitEngineSubspaceManager,
                        extensions,
                        entityManager,
                        executor);
        entity.setPose(pose);
        return entity;
    }
}
