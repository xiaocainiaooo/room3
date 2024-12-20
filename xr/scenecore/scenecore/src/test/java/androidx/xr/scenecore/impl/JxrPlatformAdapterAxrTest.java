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

import static androidx.xr.runtime.testing.math.MathAssertions.assertPose;
import static androidx.xr.runtime.testing.math.MathAssertions.assertRotation;
import static androidx.xr.runtime.testing.math.MathAssertions.assertVector3;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.xr.extensions.environment.EnvironmentVisibilityState;
import androidx.xr.extensions.environment.PassthroughVisibilityState;
import androidx.xr.extensions.node.Mat4f;
import androidx.xr.extensions.node.ReformOptions;
import androidx.xr.extensions.node.Vec3;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivitySpace;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity.State;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorPlacement;
import androidx.xr.scenecore.JxrPlatformAdapter.CameraViewActivityPose;
import androidx.xr.scenecore.JxrPlatformAdapter.Component;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.ExrImageResource;
import androidx.xr.scenecore.JxrPlatformAdapter.GltfEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.GltfModelResource;
import androidx.xr.scenecore.JxrPlatformAdapter.HeadActivityPose;
import androidx.xr.scenecore.JxrPlatformAdapter.InputEvent;
import androidx.xr.scenecore.JxrPlatformAdapter.InputEventListener;
import androidx.xr.scenecore.JxrPlatformAdapter.InteractableComponent;
import androidx.xr.scenecore.JxrPlatformAdapter.LoggingEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.MovableComponent;
import androidx.xr.scenecore.JxrPlatformAdapter.PanelEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.PixelDimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneSemantic;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneType;
import androidx.xr.scenecore.JxrPlatformAdapter.PointerCaptureComponent;
import androidx.xr.scenecore.JxrPlatformAdapter.ResizableComponent;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialCapabilities;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialEnvironment;
import androidx.xr.scenecore.JxrPlatformAdapter.StereoSurfaceEntity;
import androidx.xr.scenecore.impl.perception.Anchor;
import androidx.xr.scenecore.impl.perception.Fov;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Plane;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.impl.perception.ViewProjection;
import androidx.xr.scenecore.impl.perception.ViewProjections;
import androidx.xr.scenecore.impl.perception.exceptions.FailedToInitializeException;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeEnvironmentToken;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeEnvironmentVisibilityState;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeGltfModelToken;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeInputEvent;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeInputEvent.FakeHitInfo;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeNode;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakePassthroughVisibilityState;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeSpatialState;
import androidx.xr.scenecore.testing.FakeXrExtensions.SpaceMode;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.time.Duration;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
public final class JxrPlatformAdapterAxrTest {
    private static final int OPEN_XR_REFERENCE_SPACE_TYPE = 1;

    private static final int SUBSPACE_ID = 5;
    private final FakeXrExtensions fakeExtensions = new FakeXrExtensions();
    private final FakeImpressApi fakeImpressApi = new FakeImpressApi();
    private final FakeNode subspaceNode = (FakeNode) fakeExtensions.createNode();
    private final SubspaceNode expectedSubspace = new SubspaceNode(SUBSPACE_ID, subspaceNode);
    private final FakeScheduledExecutorService fakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary perceptionLibrary = mock(PerceptionLibrary.class);
    private final Session session = mock(Session.class);
    private final Plane plane = mock(Plane.class);
    private final Anchor anchor = mock(Anchor.class);
    private final IBinder sharedAnchorToken = mock(IBinder.class);
    SplitEngineSubspaceManager splitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    ImpSplitEngineRenderer splitEngineRenderer = Mockito.mock(ImpSplitEngineRenderer.class);
    private ActivityController<Activity> activityController;
    private Activity activity;
    private JxrPlatformAdapter realityCoreRuntime;

    @Before
    public void setUp() {
        activityController = Robolectric.buildActivity(Activity.class);
        activity = activityController.create().start().get();
        fakeExtensions.setOpenXrWorldSpaceType(OPEN_XR_REFERENCE_SPACE_TYPE);
        when(perceptionLibrary.initSession(activity, OPEN_XR_REFERENCE_SPACE_TYPE, fakeExecutor))
                .thenReturn(immediateFuture(session));

        realityCoreRuntime =
                JxrPlatformAdapterAxr.create(
                        activity,
                        fakeExecutor,
                        fakeExtensions,
                        fakeImpressApi,
                        new EntityManager(),
                        perceptionLibrary,
                        splitEngineSubspaceManager,
                        splitEngineRenderer,
                        /* useSplitEngine= */ false);
    }

    GltfEntity createGltfEntity() throws Exception {
        return createGltfEntity(new Pose());
    }

    GltfEntity createGltfEntity(Pose pose) throws Exception {
        ListenableFuture<GltfModelResource> modelFuture =
                realityCoreRuntime.loadGltfByAssetName("FakeAsset.glb");
        assertThat(modelFuture).isNotNull();
        GltfModelResource model = modelFuture.get();
        return realityCoreRuntime.createGltfEntity(
                pose, model, realityCoreRuntime.getActivitySpaceRootImpl());
    }

    GltfEntity createGltfEntitySplitEngine() throws Exception {
        return createGltfEntitySplitEngine(new Pose());
    }

    GltfEntity createGltfEntitySplitEngine(Pose pose) throws Exception {
        FakeNode rootNode = (FakeNode) fakeExtensions.createNode();
        FakeNode taskWindowLeashNode = (FakeNode) fakeExtensions.createNode();

        when(splitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
                .thenReturn(expectedSubspace);

        JxrPlatformAdapterAxr realityCoreRuntimeWithSplitEngine =
                JxrPlatformAdapterAxr.create(
                        activity,
                        fakeExecutor,
                        fakeExtensions,
                        fakeImpressApi,
                        new EntityManager(),
                        perceptionLibrary,
                        splitEngineSubspaceManager,
                        splitEngineRenderer,
                        rootNode,
                        taskWindowLeashNode,
                        /* useSplitEngine= */ true);

        realityCoreRuntimeWithSplitEngine.setSplitEngineSubspaceManager(splitEngineSubspaceManager);

        ListenableFuture<GltfModelResource> modelFuture =
                realityCoreRuntimeWithSplitEngine.loadGltfByAssetNameSplitEngine("FakeAsset.glb");
        assertThat(modelFuture).isNotNull();
        // This resolves the transformation of the Future from a SplitEngine token to the JXR
        // GltfModelResource.  This is a hidden detail from the API surface's perspective.
        fakeExecutor.runAll();
        GltfModelResource model = modelFuture.get();
        return realityCoreRuntimeWithSplitEngine.createGltfEntity(
                pose, model, realityCoreRuntime.getActivitySpaceRootImpl());
    }

    private PanelEntity createPanelEntity() {
        return createPanelEntity(new Pose());
    }

    /**
     * Creates a generic panel entity instance for testing by creating a dummy view to insert into
     * the panel, and setting the activity space as parent.
     */
    private PanelEntity createPanelEntity(Pose pose) {
        Display display = activity.getSystemService(DisplayManager.class).getDisplays()[0];
        Context displayContext = activity.createDisplayContext(display);
        View view = new View(displayContext);
        view.setLayoutParams(new LayoutParams(640, 480));
        return realityCoreRuntime.createPanelEntity(
                pose,
                view,
                new PixelDimensions(640, 480),
                new Dimensions(0.5f, 0.5f, 0.5f),
                "testPanel",
                displayContext,
                realityCoreRuntime.getActivitySpaceRootImpl());
    }

    private Entity createContentlessEntity() {
        return createContentlessEntity(new Pose());
    }

    private Entity createContentlessEntity(Pose pose) {
        return realityCoreRuntime.createEntity(
                pose, "test", realityCoreRuntime.getActivitySpaceRootImpl());
    }

    @Test
    @Ignore
    public void initRuntimePerceptionFailure() {
        ListenableFuture<Session> sessionFuture =
                immediateFailedFuture(
                        new FailedToInitializeException("Failed to initialize a session."));
        when(perceptionLibrary.initSession(activity, OPEN_XR_REFERENCE_SPACE_TYPE, fakeExecutor))
                .thenReturn(sessionFuture);

        realityCoreRuntime =
                JxrPlatformAdapterAxr.create(
                        activity,
                        fakeExecutor,
                        fakeExtensions,
                        fakeImpressApi,
                        new EntityManager(),
                        perceptionLibrary,
                        splitEngineSubspaceManager,
                        splitEngineRenderer,
                        /* useSplitEngine= */ false);

        // The perception library failed to initialize a session, but the runtime should still be
        // created.
        assertThat(realityCoreRuntime).isNotNull();
    }

    @Test
    @Ignore
    public void requestHomeSpaceMode_callsExtensions() {
        realityCoreRuntime.requestHomeSpaceMode();
        assertThat(fakeExtensions.getSpaceMode()).isEqualTo(SpaceMode.HOME_SPACE);
    }

    @Test
    @Ignore
    public void requestFullSpaceMode_callsExtensions() {
        realityCoreRuntime.requestFullSpaceMode();
        assertThat(fakeExtensions.getSpaceMode()).isEqualTo(SpaceMode.FULL_SPACE);
    }

    @Test
    public void createLoggingEntity_returnsEntity() {
        Pose pose = new Pose();
        LoggingEntity loggingeEntity = realityCoreRuntime.createLoggingEntity(pose);
        Pose updatedPose =
                new Pose(
                        new Vector3(1f, pose.getTranslation().getY(), pose.getTranslation().getZ()),
                        pose.getRotation());
        loggingeEntity.setPose(updatedPose);
    }

    @Test
    @Ignore
    public void loggingEntitySetParent() {
        Pose pose = new Pose();
        LoggingEntity childEntity = realityCoreRuntime.createLoggingEntity(pose);
        LoggingEntity parentEntity = realityCoreRuntime.createLoggingEntity(pose);

        childEntity.setParent(parentEntity);
        parentEntity.addChild(childEntity);

        assertThat(childEntity.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getParent()).isEqualTo(null);
        assertThat(childEntity.getChildren()).isEmpty();
        assertThat(parentEntity.getChildren()).containsExactly(childEntity);
    }

    @Test
    @Ignore
    public void loggingEntityUpdateParent() {
        Pose pose = new Pose();
        LoggingEntity childEntity = realityCoreRuntime.createLoggingEntity(pose);
        LoggingEntity parentEntity1 = realityCoreRuntime.createLoggingEntity(pose);
        LoggingEntity parentEntity2 = realityCoreRuntime.createLoggingEntity(pose);

        childEntity.setParent(parentEntity1);
        assertThat(childEntity.getParent()).isEqualTo(parentEntity1);
        assertThat(parentEntity1.getChildren()).containsExactly(childEntity);
        assertThat(parentEntity2.getChildren()).isEmpty();

        childEntity.setParent(parentEntity2);
        assertThat(childEntity.getParent()).isEqualTo(parentEntity2);
        assertThat(parentEntity2.getChildren()).containsExactly(childEntity);
        assertThat(parentEntity1.getChildren()).isEmpty();
    }

    @Test
    @Ignore
    public void onSpatialStateChanged_setsSpatialCapabilities() {
        realityCoreRuntime =
                JxrPlatformAdapterAxr.create(
                        activity,
                        fakeExecutor,
                        fakeExtensions,
                        fakeImpressApi,
                        new EntityManager(),
                        perceptionLibrary,
                        splitEngineSubspaceManager,
                        splitEngineRenderer,
                        /* useSplitEngine= */ false);

        FakeSpatialState spatialState = new FakeSpatialState();
        spatialState.setSpatialCapabilities(
                new androidx.xr.extensions.space.SpatialCapabilities() {
                    @Override
                    public boolean get(int capability) {
                        return capability
                                == androidx.xr.extensions.space.SpatialCapabilities
                                        .SPATIAL_UI_CAPABLE;
                    }
                });
        ((JxrPlatformAdapterAxr) realityCoreRuntime).onSpatialStateChanged(spatialState);

        SpatialCapabilities caps = realityCoreRuntime.getSpatialCapabilities();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isFalse();
    }

    @Test
    @Ignore
    public void onSpatialStateChanged_setsEnvironmentVisibility() {
        SpatialEnvironment environment = realityCoreRuntime.getSpatialEnvironment();
        assertThat(environment.isSpatialEnvironmentPreferenceActive()).isFalse();

        FakeSpatialState state = new FakeSpatialState();
        state.setEnvironmentVisibility(
                new FakeEnvironmentVisibilityState(EnvironmentVisibilityState.APP_VISIBLE));
        fakeExtensions.sendSpatialState(state);
        assertThat(environment.isSpatialEnvironmentPreferenceActive()).isTrue();

        state = new FakeSpatialState();
        state.setEnvironmentVisibility(
                new FakeEnvironmentVisibilityState(EnvironmentVisibilityState.INVISIBLE));
        fakeExtensions.sendSpatialState(state);
        assertThat(environment.isSpatialEnvironmentPreferenceActive()).isFalse();

        state = new FakeSpatialState();
        state.setEnvironmentVisibility(
                new FakeEnvironmentVisibilityState(EnvironmentVisibilityState.HOME_VISIBLE));
        fakeExtensions.sendSpatialState(state);
        assertThat(environment.isSpatialEnvironmentPreferenceActive()).isFalse();
    }

    @Test
    @Ignore
    public void onSpatialStateChanged_callsEnvironmentListenerOnlyForChanges() {
        SpatialEnvironment environment = realityCoreRuntime.getSpatialEnvironment();
        @SuppressWarnings(value = "unchecked")
        Consumer<Boolean> listener = (Consumer<Boolean>) mock(Consumer.class);

        environment.addOnSpatialEnvironmentChangedListener(listener);

        assertThat(environment.isSpatialEnvironmentPreferenceActive()).isFalse();

        // The first spatial state should always fire the listener
        FakeSpatialState state = new FakeSpatialState();
        state.setEnvironmentVisibility(
                new FakeEnvironmentVisibilityState(EnvironmentVisibilityState.APP_VISIBLE));
        fakeExtensions.sendSpatialState(state);
        verify(listener).accept(true);

        // The second spatial state should also fire the listener since it's a different state
        state = new FakeSpatialState();
        state.setEnvironmentVisibility(
                new FakeEnvironmentVisibilityState(EnvironmentVisibilityState.INVISIBLE));
        fakeExtensions.sendSpatialState(state);
        assertThat(environment.isSpatialEnvironmentPreferenceActive()).isFalse();
        verify(listener).accept(false);

        // The third spatial state should not fire the listener since it is the same as the last
        // state.
        state = new FakeSpatialState();
        state.setEnvironmentVisibility(
                new FakeEnvironmentVisibilityState(EnvironmentVisibilityState.INVISIBLE));
        fakeExtensions.sendSpatialState(state);
        assertThat(environment.isSpatialEnvironmentPreferenceActive()).isFalse();
        verify(listener, times(2))
                .accept(any()); // Verify the listener was not called a third time.
    }

    @Test
    @Ignore
    public void onSpatialStateChanged_setsPassthroughOpacity() {
        SpatialEnvironment environment = realityCoreRuntime.getSpatialEnvironment();
        assertThat(environment.getCurrentPassthroughOpacity()).isZero();

        FakeSpatialState state = new FakeSpatialState();
        state.setPassthroughVisibility(
                new FakePassthroughVisibilityState(PassthroughVisibilityState.APP, 0.4f));
        fakeExtensions.sendSpatialState(state);
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.4f);

        state = new FakeSpatialState();
        state.setPassthroughVisibility(
                new FakePassthroughVisibilityState(PassthroughVisibilityState.HOME, 0.5f));
        fakeExtensions.sendSpatialState(state);
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.5f);

        state = new FakeSpatialState();
        state.setPassthroughVisibility(
                new FakePassthroughVisibilityState(PassthroughVisibilityState.SYSTEM, 0.9f));
        fakeExtensions.sendSpatialState(state);
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.9f);

        state = new FakeSpatialState();
        state.setPassthroughVisibility(
                new FakePassthroughVisibilityState(PassthroughVisibilityState.DISABLED, 0.0f));
        fakeExtensions.sendSpatialState(state);
        assertThat(environment.getCurrentPassthroughOpacity()).isZero();
    }

    @Test
    @Ignore
    public void onSpatialStateChanged_callsPassthroughListenerOnlyForChanges() {
        SpatialEnvironment environment = realityCoreRuntime.getSpatialEnvironment();
        @SuppressWarnings(value = "unchecked")
        Consumer<Float> listener = (Consumer<Float>) mock(Consumer.class);

        environment.addOnPassthroughOpacityChangedListener(listener);

        assertThat(environment.getCurrentPassthroughOpacity()).isZero();

        // The first spatial state should always fire the listener
        FakeSpatialState state = new FakeSpatialState();
        state.setPassthroughVisibility(
                new FakePassthroughVisibilityState(PassthroughVisibilityState.APP, 1.0f));
        fakeExtensions.sendSpatialState(state);
        verify(listener).accept(1.0f);

        // The second spatial state should also fire the listener even if only the opacity changes
        state = new FakeSpatialState();
        state.setPassthroughVisibility(
                new FakePassthroughVisibilityState(PassthroughVisibilityState.APP, 0.5f));
        fakeExtensions.sendSpatialState(state);
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.5f);

        // The third spatial state should also fire the listener even if only the visibility state
        // changes, but getCurrentPassthroughOpacity() returns the same value as the last state.
        state = new FakeSpatialState();
        state.setPassthroughVisibility(
                new FakePassthroughVisibilityState(PassthroughVisibilityState.HOME, 0.5f));
        fakeExtensions.sendSpatialState(state);
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.5f);
        verify(listener, times(2))
                .accept(0.5f); // Verify it was called a second time with this value.

        // The fourth spatial state should not fire the listener since it is the same as the last
        // state.
        state = new FakeSpatialState();
        state.setPassthroughVisibility(
                new FakePassthroughVisibilityState(PassthroughVisibilityState.HOME, 0.5f));
        fakeExtensions.sendSpatialState(state);
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.5f);
        verify(listener, times(3))
                .accept(any()); // Verify the listener was not called a fourth time.
    }

    @Test
    @Ignore
    public void currentPassthroughOpacity_isSetDuringRuntimeCreation() {
        fakeExtensions.fakeSpatialState.setPassthroughVisibility(
                new FakePassthroughVisibilityState(PassthroughVisibilityState.APP, 0.5f));

        JxrPlatformAdapter newRealityCoreRuntime =
                JxrPlatformAdapterAxr.create(
                        activity,
                        fakeExecutor,
                        fakeExtensions,
                        fakeImpressApi,
                        new EntityManager(),
                        perceptionLibrary,
                        splitEngineSubspaceManager,
                        splitEngineRenderer,
                        /* useSplitEngine= */ false);

        SpatialEnvironment newEnvironment = newRealityCoreRuntime.getSpatialEnvironment();
        assertThat(newEnvironment.getCurrentPassthroughOpacity()).isEqualTo(0.5f);
    }

    @Test
    @Ignore
    public void onSpatialStateChanged_firesSpatialCapabilitiesChangedListener() {
        realityCoreRuntime =
                JxrPlatformAdapterAxr.create(
                        activity,
                        fakeExecutor,
                        fakeExtensions,
                        fakeImpressApi,
                        new EntityManager(),
                        perceptionLibrary,
                        splitEngineSubspaceManager,
                        splitEngineRenderer,
                        /* useSplitEngine= */ false);

        @SuppressWarnings(value = "unchecked")
        Consumer<SpatialCapabilities> listener1 =
                (Consumer<SpatialCapabilities>) mock(Consumer.class);
        @SuppressWarnings(value = "unchecked")
        Consumer<SpatialCapabilities> listener2 =
                (Consumer<SpatialCapabilities>) mock(Consumer.class);

        realityCoreRuntime.addSpatialCapabilitiesChangedListener(directExecutor(), listener1);
        realityCoreRuntime.addSpatialCapabilitiesChangedListener(directExecutor(), listener2);

        FakeSpatialState state = new FakeSpatialState();
        state.setSpatialCapabilities(
                new androidx.xr.extensions.space.SpatialCapabilities() {
                    @Override
                    public boolean get(int capability) {
                        return true;
                    }
                });
        fakeExtensions.sendSpatialState(state);
        verify(listener1).accept(any());
        verify(listener2).accept(any());

        state = new FakeSpatialState();
        state.setSpatialCapabilities(
                new androidx.xr.extensions.space.SpatialCapabilities() {
                    @Override
                    public boolean get(int capability) {
                        return false;
                    }
                });
        realityCoreRuntime.removeSpatialCapabilitiesChangedListener(listener1);
        fakeExtensions.sendSpatialState(state);
        verify(listener1).accept(any()); // Verify the removed listener was called exactly once
        verify(listener2, times(2)).accept(any()); // Verify the active listener was called twice
    }

    @Test
    @Ignore
    public void getHeadPoseInOpenXrUnboundedSpace_returnsNullWhenPerceptionSessionUninitialized() {
        when(perceptionLibrary.getSession()).thenReturn(null);
        assertThat(((JxrPlatformAdapterAxr) realityCoreRuntime).getHeadPoseInOpenXrUnboundedSpace())
                .isNull();
    }

    @Test
    @Ignore
    public void getHeadPoseInOpenXrUnboundedSpace_returnsPose() {
        when(session.getHeadPose())
                .thenReturn(
                        new androidx.xr.scenecore.impl.perception.Pose(1f, 1f, 1f, 0f, 0f, 0f, 1f));
        when(perceptionLibrary.getSession()).thenReturn(session);
        assertPose(
                ((JxrPlatformAdapterAxr) realityCoreRuntime).getHeadPoseInOpenXrUnboundedSpace(),
                new Pose(new Vector3(1f, 1f, 1f), new Quaternion(0f, 0f, 0f, 1f)));
    }

    @Test
    @Ignore
    public void
            getStereoViewsInOpenXrUnboundedSpace_returnsNullWhenPerceptionSessionUninitialized() {
        when(perceptionLibrary.getSession()).thenReturn(null);
        assertThat(
                        ((JxrPlatformAdapterAxr) realityCoreRuntime)
                                .getStereoViewsInOpenXrUnboundedSpace())
                .isNull();
    }

    @Test
    @Ignore
    public void getStereoViewsInOpenXrUnboundedSpace_returnsViewProjections() {
        ViewProjection leftViewProjection =
                new ViewProjection(
                        new androidx.xr.scenecore.impl.perception.Pose(-1f, 1f, 1f, 0f, 0f, 0f, 1f),
                        new Fov(-1f, -1f, -1f, -1f));

        ViewProjection rightViewProjection =
                new ViewProjection(
                        new androidx.xr.scenecore.impl.perception.Pose(1f, 1f, 1f, 0f, 0f, 0f, 1f),
                        new Fov(1f, 1f, 1f, 1f));

        when(session.getStereoViews())
                .thenReturn(new ViewProjections(leftViewProjection, rightViewProjection));
        when(perceptionLibrary.getSession()).thenReturn(session);
        assertThat(
                        ((JxrPlatformAdapterAxr) realityCoreRuntime)
                                .getStereoViewsInOpenXrUnboundedSpace())
                .isEqualTo(new ViewProjections(leftViewProjection, rightViewProjection));
    }

    @Test
    @Ignore
    public void loggingEntity_getActivitySpacePose_returnsIdentityPose() {
        Pose identityPose = new Pose();
        LoggingEntity loggingEntity = realityCoreRuntime.createLoggingEntity(identityPose);
        assertPose(loggingEntity.getActivitySpacePose(), identityPose);
    }

    @Test
    @Ignore
    public void loggingEntity_transformPoseTo_returnsIdentityPose() {
        Pose identityPose = new Pose();
        LoggingEntity loggingEntity = realityCoreRuntime.createLoggingEntity(identityPose);
        assertPose(loggingEntity.transformPoseTo(identityPose, loggingEntity), identityPose);
    }

    @Test
    @Ignore
    public void getPose_returnsSetPose() throws Exception {
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), new Quaternion(1f, 2f, 3f, 4f));
        Pose identityPose = new Pose();
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = realityCoreRuntime.createLoggingEntity(identityPose);
        Entity contentlessEntity = createContentlessEntity();

        assertPose(panelEntity.getPose(), identityPose);
        assertPose(gltfEntity.getPose(), identityPose);
        assertPose(loggingEntity.getPose(), identityPose);
        assertPose(contentlessEntity.getPose(), identityPose);

        panelEntity.setPose(pose);
        gltfEntity.setPose(pose);
        loggingEntity.setPose(pose);
        contentlessEntity.setPose(pose);

        assertPose(panelEntity.getPose(), pose);
        assertPose(gltfEntity.getPose(), pose);
        assertPose(loggingEntity.getPose(), pose);
        assertPose(contentlessEntity.getPose(), pose);
    }

    @Test
    @Ignore
    public void getPose_returnsFactoryMethodPose() throws Exception {
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), new Quaternion(1f, 2f, 3f, 4f));
        PanelEntity panelEntity = createPanelEntity(pose);
        GltfEntity gltfEntity = createGltfEntity(pose);
        LoggingEntity loggingEntity = realityCoreRuntime.createLoggingEntity(pose);
        Entity contentlessEntity = createContentlessEntity(pose);

        assertPose(panelEntity.getPose(), pose);
        assertPose(gltfEntity.getPose(), pose);
        assertPose(loggingEntity.getPose(), pose);
        assertPose(contentlessEntity.getPose(), pose);
    }

    @Test
    @Ignore
    public void getPoseInActivitySpace_withParentChainTranslation_returnsOffsetPositionFromRoot()
            throws Exception {
        // Create a simple pose with only a small translation on all axes
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);

        // Set the activity space as the root of this entity hierarchy..
        AndroidXrEntity parentEntity =
                (AndroidXrEntity)
                        realityCoreRuntime.createEntity(
                                pose, "parent", realityCoreRuntime.getActivitySpace());
        AndroidXrEntity childEntity1 =
                (AndroidXrEntity) realityCoreRuntime.createEntity(pose, "child1", parentEntity);
        AndroidXrEntity childEntity2 =
                (AndroidXrEntity) realityCoreRuntime.createEntity(pose, "child2", childEntity1);

        assertVector3(
                parentEntity.getPoseInActivitySpace().getTranslation(), new Vector3(1f, 2f, 3f));
        assertVector3(
                childEntity1.getPoseInActivitySpace().getTranslation(), new Vector3(2f, 4f, 6f));
        assertVector3(
                childEntity2.getPoseInActivitySpace().getTranslation(), new Vector3(3f, 6f, 9f));
    }

    @Test
    @Ignore
    public void getPoseInActivitySpace_withParentChainRotation_returnsOffsetRotationFromRoot()
            throws Exception {
        // Create a pose with a translation and one with 90 degree rotation around the y axis.
        Vector3 parentTranslation = new Vector3(1f, 2f, 3f);
        Pose translatedPose = new Pose(parentTranslation, Quaternion.Identity);
        Quaternion quaternion = Quaternion.fromAxisAngle(new Vector3(0f, 1f, 0f), 90f);
        Pose rotatedPose = new Pose(new Vector3(0f, 0f, 0f), quaternion);

        // The parent has a translation and no rotation.
        AndroidXrEntity parentEntity =
                (AndroidXrEntity)
                        realityCoreRuntime.createEntity(
                                translatedPose, "parent", realityCoreRuntime.getActivitySpace());

        // Each child adds a rotation, but no translation.
        AndroidXrEntity childEntity1 =
                (AndroidXrEntity)
                        realityCoreRuntime.createEntity(rotatedPose, "child1", parentEntity);
        AndroidXrEntity childEntity2 =
                (AndroidXrEntity)
                        realityCoreRuntime.createEntity(rotatedPose, "child2", childEntity1);

        // There should be no translation offset from the root, only changes in rotation.
        assertPose(parentEntity.getPoseInActivitySpace(), translatedPose);
        assertPose(childEntity1.getPoseInActivitySpace(), new Pose(parentTranslation, quaternion));
        assertPose(
                childEntity2.getPoseInActivitySpace(),
                new Pose(
                        parentTranslation,
                        Quaternion.fromAxisAngle(new Vector3(0f, 1f, 0f), 180f)));
    }

    @Test
    @Ignore
    public void getPoseInActivitySpace_withParentChainPoseOffsets_returnsOffsetPoseFromRoot()
            throws Exception {
        // Create a pose with a 1D translation and a 90 degree rotation around the z axis.
        Vector3 parentTranslation = new Vector3(1f, 0f, 0f);
        Quaternion quaternion = Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 90f);
        Pose pose = new Pose(parentTranslation, quaternion);

        // Each entity adds a translation and a rotation.
        AndroidXrEntity parentEntity =
                (AndroidXrEntity)
                        realityCoreRuntime.createEntity(
                                pose, "parent", realityCoreRuntime.getActivitySpace());
        AndroidXrEntity childEntity1 =
                (AndroidXrEntity) realityCoreRuntime.createEntity(pose, "child1", parentEntity);
        AndroidXrEntity childEntity2 =
                (AndroidXrEntity) realityCoreRuntime.createEntity(pose, "child2", childEntity1);

        // Local pose of ActivitySpace's direct child must be the same as child's ActivitySpace
        // pose.
        assertPose(parentEntity.getPoseInActivitySpace(), parentEntity.getPose());

        // Each child should be positioned one unit away at 90 degrees from its parent's position.
        // Since our coordinate system is right-handed, a +ve rotation around the z axis is a
        // counter-clockwise rotation of the XY plane.
        // First child should be 1 unit in the ActivitySpace's positive y direction from its parent
        assertPose(
                childEntity1.getPoseInActivitySpace(),
                new Pose(
                        new Vector3(1f, 1f, 0f),
                        Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 180f)));
        // Second child should be 1 unit in the ActivitySpace's negative x direction from its parent
        assertPose(
                childEntity2.getPoseInActivitySpace(),
                new Pose(
                        new Vector3(0f, 1f, 0f),
                        Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 270f)));
    }

    @Test
    @Ignore
    public void getPoseInActivitySpace_withActivitySpaceParent_returnsScaledPose()
            throws Exception {
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), new Quaternion(1f, 2f, 3f, 4f));

        // Set the parent as the activity space so these entities' activitySpacePose should match
        // their
        // local pose relative to their parent.
        PanelEntityImpl panelEntity = (PanelEntityImpl) createPanelEntity(pose);
        GltfEntityImpl gltfEntity = (GltfEntityImpl) createGltfEntity(pose);
        AndroidXrEntity contentlessEntity = (AndroidXrEntity) createContentlessEntity(pose);
        ActivitySpace activitySpace = realityCoreRuntime.getActivitySpace();
        panelEntity.setParent(activitySpace);
        gltfEntity.setParent(activitySpace);
        contentlessEntity.setParent(activitySpace);

        assertPose(panelEntity.getPoseInActivitySpace(), pose);
        assertPose(gltfEntity.getPoseInActivitySpace(), pose);
        assertPose(contentlessEntity.getPoseInActivitySpace(), pose);
    }

    @Test
    @Ignore
    public void getPoseInActivitySpace_withScale_returnsPose() throws Exception {
        Pose localPose = new Pose(new Vector3(1f, 2f, 1f), Quaternion.Identity);

        // Create a hierarchy of entities each translated from their parent by (1,2,1) in parent
        // space.
        GltfEntityImpl child1 = (GltfEntityImpl) createGltfEntity(localPose);
        GltfEntityImpl child2 = (GltfEntityImpl) createGltfEntity(localPose);
        GltfEntityImpl child3 = (GltfEntityImpl) createGltfEntity(localPose);
        ActivitySpace activitySpace = realityCoreRuntime.getActivitySpace();
        assertVector3(activitySpace.getScale(), new Vector3(1f, 1f, 1f));

        // Set a non-unit local scale to each child.
        child1.setParent(activitySpace);
        child1.setScale(new Vector3(2f, 2f, 2f));

        child2.setParent(child1);
        child2.setScale(new Vector3(3f, 2f, 3f));

        child3.setParent(child2);
        child3.setScale(new Vector3(1f, 1f, 2f));

        // The position (in ActivitySpace) should be:
        // child's local position * parent's scale in AS + parent's position since there's no
        // rotation.

        // Assuming c1 = child1, c2 = child2, c3 = child3, AS = activitySpace.
        // c1.posInAS = c1.localPos * AS.scaleInAS + AS.posInAS = (1,2,1) * (1,1,1) + (0,0,0) =
        // (1,2,1)
        assertPose(
                child1.getPoseInActivitySpace(),
                new Pose(new Vector3(1f, 2f, 1f), Quaternion.Identity));

        // c2.posInAS = c2.localPos * c1.scaleInAS + c1.posInAS = (1,2,1) * (2,2,2) + (1,2,1) =
        // (3,6,3)
        assertPose(
                child2.getPoseInActivitySpace(),
                new Pose(new Vector3(3f, 6f, 3f), Quaternion.Identity));

        // c2.scaleInA = c2.localScale * c1.scaleInAS * AS.scale = (3,2,3) * (2,2,2) * (1,1,1) =
        // (6,4,6)
        // c3.posInAS = c3.localPos * c2.scaleInAS + c2.posInAS = (1,2,1) * (6,4,6) + (3,6,3) =
        // (9,14,9)
        assertPose(
                child3.getPoseInActivitySpace(),
                new Pose(new Vector3(9f, 14f, 9f), Quaternion.Identity));
    }

    @Test
    @Ignore
    public void getActivitySpacePose_withParentChainTranslation_returnsOffsetPositionFromRoot()
            throws Exception {
        // Create a simple pose with only a small translation on all axes
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);

        // Set the ActivitySpace as the root of this entity hierarchy.
        Entity parentEntity =
                realityCoreRuntime.createEntity(
                        pose, "parent", realityCoreRuntime.getActivitySpaceRootImpl());
        Entity childEntity1 = realityCoreRuntime.createEntity(pose, "child1", parentEntity);
        Entity childEntity2 = realityCoreRuntime.createEntity(pose, "child2", childEntity1);

        // The translations should accumulate with each child, but there should be no rotation.
        assertVector3(
                parentEntity.getActivitySpacePose().getTranslation(), new Vector3(1f, 2f, 3f));
        assertVector3(
                childEntity1.getActivitySpacePose().getTranslation(), new Vector3(2f, 4f, 6f));
        assertVector3(
                childEntity2.getActivitySpacePose().getTranslation(), new Vector3(3f, 6f, 9f));
        assertRotation(childEntity2.getActivitySpacePose().getRotation(), Quaternion.Identity);
    }

    @Test
    @Ignore
    public void getActivitySpacePose_withParentChainRotation_returnsOffsetRotationFromRoot()
            throws Exception {
        // Create a pose with a translation and one with 90 degree rotation around the y axis.
        Vector3 parentTranslation = new Vector3(1f, 0f, 0f);
        Pose translatedPose = new Pose(parentTranslation, Quaternion.Identity);
        Quaternion quaternion = Quaternion.fromAxisAngle(new Vector3(0f, 1f, 0f), 90f);
        Pose rotatedPose = new Pose(new Vector3(0f, 0f, 0f), quaternion);

        // The parent has a translation and no rotation and each child adds a rotation.
        Entity parentEntity =
                realityCoreRuntime.createEntity(
                        translatedPose, "parent", realityCoreRuntime.getActivitySpaceRootImpl());
        Entity childEntity1 = realityCoreRuntime.createEntity(rotatedPose, "child1", parentEntity);
        Entity childEntity2 = realityCoreRuntime.createEntity(rotatedPose, "child2", childEntity1);

        // There should be no translation offset from the parent, but rotations should accumulate.
        assertPose(parentEntity.getActivitySpacePose(), translatedPose);
        assertPose(childEntity1.getActivitySpacePose(), new Pose(parentTranslation, quaternion));
        assertPose(
                childEntity2.getActivitySpacePose(),
                new Pose(
                        parentTranslation,
                        Quaternion.fromAxisAngle(new Vector3(0f, 1f, 0f), 180f)));
    }

    @Test
    @Ignore
    public void getActivitySpacePose_withParentChainPoseOffsets_returnsOffsetPoseFromRoot()
            throws Exception {
        // Create a pose with a 1D translation and a 90 degree rotation around the z axis.
        Vector3 parentTranslation = new Vector3(1f, 0f, 0f);
        Quaternion quaternion = Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 90f);
        Pose pose = new Pose(parentTranslation, quaternion);

        // Each entity adds a translation and a rotation.
        Entity parentEntity =
                realityCoreRuntime.createEntity(
                        pose, "parent", realityCoreRuntime.getActivitySpaceRootImpl());
        Entity childEntity1 = realityCoreRuntime.createEntity(pose, "child1", parentEntity);
        Entity childEntity2 = realityCoreRuntime.createEntity(pose, "child2", childEntity1);

        // Local pose of ActivitySpace's direct child must be the same as child's ActivitySpace
        // pose.
        assertPose(parentEntity.getActivitySpacePose(), parentEntity.getPose());

        // Each child should be positioned one unit away at 90 degrees from its parent's position.
        // Since our coordinate system is right-handed, a +ve rotation around the z axis is a
        // counter-clockwise rotation of the XY plane.
        // First child should be 1 unit in the ActivitySpace's positive y direction from its parent
        assertPose(
                childEntity1.getActivitySpacePose(),
                new Pose(
                        new Vector3(1f, 1f, 0f),
                        Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 180f)));
        // Second child should be 1 unit in the ActivitySpace's negative x direction from its parent
        assertPose(
                childEntity2.getActivitySpacePose(),
                new Pose(
                        new Vector3(0f, 1f, 0f),
                        Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 270f)));
    }

    @Test
    @Ignore
    public void getActivitySpacePose_withDefaultParent_returnsPose() throws Exception {
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), new Quaternion(1f, 2f, 3f, 4f));

        // All these entities should have the ActivitySpaceRootImpl as their parent by default.
        PanelEntity panelEntity = createPanelEntity(pose);
        GltfEntity gltfEntity = createGltfEntity(pose);
        Entity contentlessEntity = createContentlessEntity(pose);

        assertPose(panelEntity.getActivitySpacePose(), pose);
        assertPose(gltfEntity.getActivitySpacePose(), pose);
        assertPose(contentlessEntity.getActivitySpacePose(), pose);
    }

    @Test
    @Ignore
    public void getPoseInActivitySpace_withScale_returnsScaledPose() throws Exception {
        Pose localPose = new Pose(new Vector3(1f, 2f, 1f), Quaternion.Identity);

        // Create a hierarchy of entities each translated from their parent by (1,1,1) in parent
        // space.
        GltfEntityImpl child1 = (GltfEntityImpl) createGltfEntity(localPose);
        GltfEntityImpl child2 = (GltfEntityImpl) createGltfEntity(localPose);
        GltfEntityImpl child3 = (GltfEntityImpl) createGltfEntity(localPose);
        assertVector3(
                realityCoreRuntime.getActivitySpaceRootImpl().getScale(), new Vector3(1f, 1f, 1f));

        // Set a non-unit local scale to each child.
        child1.setParent(realityCoreRuntime.getActivitySpaceRootImpl());
        child1.setScale(new Vector3(2f, 2f, 2f));

        child2.setParent(child1);
        child2.setScale(new Vector3(3f, 2f, 3f));

        child3.setParent(child2);
        child3.setScale(new Vector3(1f, 1f, 2f));

        // See getPoseInActivitySpace_withScale_returnsScaledPose for more detailed comments.
        // The position should be (parent's scale * child's position) + parent's position
        // since there's no rotation.
        assertPose(
                child1.getActivitySpacePose(),
                new Pose(new Vector3(1f, 2f, 1f), Quaternion.Identity));
        assertPose(
                child2.getActivitySpacePose(),
                new Pose(new Vector3(3f, 6f, 3f), Quaternion.Identity));
        assertPose(
                child3.getActivitySpacePose(),
                new Pose(new Vector3(9f, 14f, 9f), Quaternion.Identity));
    }

    @Test
    @Ignore
    public void transformPoseTo_sameDestAndSourceEntity_returnsUnchangedPose() throws Exception {
        Pose pose =
                new Pose(new Vector3(1f, 2f, 3f), new Quaternion(1f, 2f, 3f, 4f).toNormalized());
        Pose identity = new Pose();

        PanelEntity panelEntity = createPanelEntity(pose);
        GltfEntity gltfEntity = createGltfEntity(pose);
        Entity contentlessEntity = createContentlessEntity(pose);
        assertPose(panelEntity.transformPoseTo(pose, panelEntity), pose);
        assertPose(gltfEntity.transformPoseTo(pose, gltfEntity), pose);
        assertPose(contentlessEntity.transformPoseTo(pose, contentlessEntity), pose);

        assertPose(panelEntity.transformPoseTo(identity, panelEntity), identity);
        assertPose(gltfEntity.transformPoseTo(identity, gltfEntity), identity);
        assertPose(contentlessEntity.transformPoseTo(identity, contentlessEntity), identity);
    }

    @Test
    @Ignore
    public void transformPoseTo_withOnlyTranslationOffset_returnsTranslationDifference()
            throws Exception {
        PanelEntityImpl sourceEntity = (PanelEntityImpl) createPanelEntity();
        GltfEntityImpl destinationEntity = (GltfEntityImpl) createGltfEntity();
        sourceEntity.setPose(
                new Pose(new Vector3(1f, 2f, 3f), sourceEntity.getPose().getRotation()));
        destinationEntity.setPose(
                new Pose(new Vector3(4f, 5f, 6f), destinationEntity.getPose().getRotation()));
        Pose offsetFromSource = new Pose(new Vector3(7f, 8f, 9f), Quaternion.Identity);

        // The expected translation is destOffset = (sourceOrigin + sourceOffset) - destOrigin
        // since there's no rotation and the entities are in the same coordinate space.
        // So ((1,2,3) + (7,8,9)) - (4,5,6) = (4, 5, 6)
        Pose offsetInDestinationSpace =
                sourceEntity.transformPoseTo(offsetFromSource, destinationEntity);
        Pose expectedPose = new Pose(new Vector3(4f, 5f, 6f), Quaternion.Identity);
        assertPose(offsetInDestinationSpace, expectedPose);
    }

    @Test
    @Ignore
    public void transformPoseTo_withOnlyRotationOffset_returnsRotationDifference()
            throws Exception {
        PanelEntityImpl sourceEntity = (PanelEntityImpl) createPanelEntity();
        GltfEntityImpl destinationEntity = (GltfEntityImpl) createGltfEntity();

        sourceEntity.setPose(
                new Pose(
                        sourceEntity.getPose().getTranslation(),
                        Quaternion.fromEulerAngles(new Vector3(1f, 2f, 3f))));
        destinationEntity.setPose(
                new Pose(
                        destinationEntity.getPose().getTranslation(),
                        Quaternion.fromEulerAngles(new Vector3(4f, 5f, 6f))));

        Pose offsetFromSource =
                new Pose(new Vector3(), Quaternion.fromEulerAngles(new Vector3(7f, 8f, 9f)));

        // The expected rotation is (source + sourceOffset) - destination since the source and
        // destination are in the same coordinate space: ((1,2,3) + (7,8,9)) - (4,5,6) = (4, 5, 6)
        Pose offsetInDestinationSpace =
                sourceEntity.transformPoseTo(offsetFromSource, destinationEntity);
        Pose expectedPose =
                new Pose(new Vector3(), Quaternion.fromEulerAngles(new Vector3(4f, 5f, 6f)));
        assertPose(offsetInDestinationSpace, expectedPose);
    }

    @Test
    @Ignore
    public void transformPoseTo_withDifferentTranslationAndRotation_returnsTransformedPose() {
        // Assume the source and destination entities are in the same coordinate space.
        Vector3 sourceVector = new Vector3(1f, 2f, 3f);
        Quaternion sourceQuaternion = Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), -90f);
        Vector3 destinationVector = new Vector3(10f, 20f, 30f);
        Quaternion destinationQuaternion = Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 90f);
        Pose identity = new Pose();

        AndroidXrEntity sourceEntity =
                (AndroidXrEntity) createContentlessEntity(new Pose(sourceVector, sourceQuaternion));
        AndroidXrEntity destinationEntity =
                (AndroidXrEntity)
                        createContentlessEntity(new Pose(destinationVector, destinationQuaternion));

        //// Transform an identity pose from the source to the destination space. ////
        Pose sourceToDestinationPose = sourceEntity.transformPoseTo(identity, destinationEntity);

        // The expected rotation is the difference between the quaternions -90 - 90 = -180.
        Quaternion expectedQuaternion = Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), -180f);
        assertRotation(sourceToDestinationPose.getRotation(), expectedQuaternion);

        // The expected translation is the difference between the source and destination vectors
        // rotated
        // by the inverse of the destination quaternion.
        Vector3 expectedVector =
                destinationQuaternion.getInverse().times(sourceVector.minus(destinationVector));

        // So difference is (1,2,3) - (10,20,30) = (-9,-18,-27) then rotate CCW by -90 degrees
        // around
        // the z axis (ie. swap x and y, set x positive and y negative since we're rotating from 3rd
        // quadrant to the 2nd quadrant of XY plane) => (-18, 9, -27)
        assertVector3(expectedVector, new Vector3(-18f, 9f, -27f));
        assertVector3(sourceToDestinationPose.getTranslation(), expectedVector);

        //// Transform an offset pose from the source to the destination. ////
        Pose offsetPose =
                new Pose(
                        new Vector3(1f, 0f, 0f),
                        Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 20f));
        Pose newSourceToDestinationPose =
                sourceEntity.transformPoseTo(offsetPose, destinationEntity);

        // The expected rotation is the difference between the quaternions (20-90) - 90 = -160.
        Quaternion newExpectedQuaternion = Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), -160f);
        assertRotation(newSourceToDestinationPose.getRotation(), newExpectedQuaternion);

        // The expected translation is expected to be the same as the previous one but with the
        // offset
        // vector added to it in the destination space.
        Vector3 offsetInActivitySpace = sourceQuaternion.times(offsetPose.getTranslation());
        Vector3 offsetInDestinationSpace =
                destinationQuaternion.getInverse().times(offsetInActivitySpace);
        Vector3 newExpectedVector = expectedVector.plus(offsetInDestinationSpace);

        // So (1, 0, 0) rotated by -90 degrees around the z axis is (0, 1, 0) in activity space then
        // add to the difference from source to destination vector (-9,-18,-27) to get (-9, -19,
        // -27)
        // and finally rotate by the inverse of the destination quaternion to get (-19, 9, -27).
        assertVector3(newExpectedVector, new Vector3(-19f, 9f, -27f));
        assertVector3(newSourceToDestinationPose.getTranslation(), newExpectedVector);
    }

    @Test
    @Ignore
    public void getAlpha_returnsSetAlpha() throws Exception {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        Entity contentlessEntity = createContentlessEntity();

        assertThat(panelEntity.getAlpha()).isEqualTo(1.0f);
        assertThat(gltfEntity.getAlpha()).isEqualTo(1.0f);
        assertThat(contentlessEntity.getAlpha()).isEqualTo(1.0f);

        panelEntity.setAlpha(0.5f);
        gltfEntity.setAlpha(0.5f);
        contentlessEntity.setAlpha(0.5f);

        assertThat(panelEntity.getAlpha()).isEqualTo(0.5f);
        assertThat(gltfEntity.getAlpha()).isEqualTo(0.5f);
        assertThat(contentlessEntity.getAlpha()).isEqualTo(0.5f);
        assertThat(
                        fakeExtensions.createdNodes.stream()
                                .map(FakeNode::getAlpha)
                                .collect(Collectors.toList()))
                .containsAtLeast(0.5f, 0.5f, 0.5f);
    }

    @Test
    @Ignore
    public void getActivitySpaceAlpha_returnsTotalAncestorAlpha() throws Exception {
        PanelEntity grandparent = createPanelEntity();
        GltfEntity parent = createGltfEntity();
        Entity entity = createContentlessEntity();

        assertThat(grandparent.getActivitySpaceAlpha()).isEqualTo(1.0f);
        assertThat(parent.getActivitySpaceAlpha()).isEqualTo(1.0f);
        assertThat(entity.getActivitySpaceAlpha()).isEqualTo(1.0f);

        grandparent.setAlpha(0.5f);
        parent.setParent(grandparent);
        parent.setAlpha(0.5f);
        entity.setParent(parent);
        entity.setAlpha(0.5f);

        assertThat(grandparent.getActivitySpaceAlpha()).isEqualTo(0.5f);
        assertThat(parent.getActivitySpaceAlpha()).isEqualTo(0.25f);
        assertThat(entity.getActivitySpaceAlpha()).isEqualTo(0.125f);
        assertThat(
                        fakeExtensions.createdNodes.stream()
                                .map(FakeNode::getAlpha)
                                .collect(Collectors.toList()))
                .containsAtLeast(0.5f, 0.5f, 0.5f);
    }

    @Test
    @Ignore
    public void transformPoseTo_withScale_returnsPose() throws Exception {
        PanelEntityImpl sourceEntity = (PanelEntityImpl) createPanelEntity();
        GltfEntityImpl destinationEntity = (GltfEntityImpl) createGltfEntity();
        sourceEntity.setPose(new Pose(new Vector3(0f, 0f, 1f), Quaternion.Identity));
        sourceEntity.setScale(new Vector3(2f, 2f, 2f));
        destinationEntity.setPose(new Pose(new Vector3(1f, 0f, 0f), Quaternion.Identity));
        destinationEntity.setScale(new Vector3(3f, 3f, 3f));

        Pose offsetFromSource = new Pose(new Vector3(0f, 0f, 1f), Quaternion.Identity);
        assertPose(
                sourceEntity.transformPoseTo(offsetFromSource, destinationEntity),
                new Pose(new Vector3(-1 / 3f, 0f, 1f), Quaternion.Identity));
    }

    @Test
    @Ignore("Flaky test, see b/380269912")
    public void transformPoseTo_withNonUniformScalesAndTranslations_returnsPose() throws Exception {
        PanelEntityImpl sourceEntity = (PanelEntityImpl) createPanelEntity();
        GltfEntityImpl destinationEntity = (GltfEntityImpl) createGltfEntity();
        sourceEntity.setPose(new Pose(new Vector3(0f, 0f, 1f), Quaternion.Identity));
        sourceEntity.setScale(new Vector3(0.5f, 2f, -3f));
        destinationEntity.setPose(new Pose(new Vector3(1f, 1f, 0f), Quaternion.Identity));
        destinationEntity.setScale(new Vector3(4f, 5f, 6f));

        Pose offsetFromSource = new Pose(new Vector3(1f, 3f, 1f), Quaternion.Identity);
        // translation is:
        //  ((localOffsetFromSource * scale of source) + sourceTranslation - destinationTranslation)
        //    * (1/scale of  destination)
        //
        //  ((1, 3, 1) * (1/2, 2, -3) + (0, 0, 1) - (1, 1, 0)) * (1/4, 1/5, 1/6) =
        //              ((1/2, 6, -3) + (0, 0, 1) - (1, 1, 0)) * (1/4, 1/5, 1/6) =
        //                                       (-1/2, 5, -2) * (1/4, 1/5, 1/6) =
        //                                                     (-1/8, 1, -2/6) = (-1/8, 1, -1/3)
        assertPose(
                sourceEntity.transformPoseTo(offsetFromSource, destinationEntity),
                new Pose(new Vector3(-1 / 8f, 1f, -1 / 3f), Quaternion.Identity));
    }

    @Test
    @Ignore
    public void isHidden_returnsSetHidden() throws Exception {
        PanelEntity parentEntity = createPanelEntity();
        assertThat(parentEntity.isHidden(true)).isFalse();
        assertThat(parentEntity.isHidden(false)).isFalse();

        PanelEntity childEntity1 = createPanelEntity();
        PanelEntity childEntity2 = createPanelEntity();
        childEntity1.setParent(parentEntity);
        childEntity2.setParent(childEntity1);

        assertThat(childEntity1.isHidden(true)).isFalse();
        assertThat(childEntity1.isHidden(false)).isFalse();

        parentEntity.setHidden(true);
        assertThat(parentEntity.isHidden(true)).isTrue();
        assertThat(parentEntity.isHidden(false)).isTrue();
        assertThat(childEntity1.isHidden(true)).isTrue();
        assertThat(childEntity1.isHidden(false)).isFalse();
        assertThat(childEntity2.isHidden(true)).isTrue();
        assertThat(childEntity2.isHidden(false)).isFalse();

        parentEntity.setHidden(false);
        assertThat(parentEntity.isHidden(true)).isFalse();
        assertThat(parentEntity.isHidden(false)).isFalse();
        assertThat(childEntity1.isHidden(true)).isFalse();
        assertThat(childEntity1.isHidden(false)).isFalse();
        assertThat(childEntity2.isHidden(true)).isFalse();
        assertThat(childEntity2.isHidden(false)).isFalse();

        childEntity1.setHidden(true);
        assertThat(parentEntity.isHidden(true)).isFalse();
        assertThat(parentEntity.isHidden(false)).isFalse();
        assertThat(childEntity1.isHidden(true)).isTrue();
        assertThat(childEntity1.isHidden(false)).isTrue();
        assertThat(childEntity2.isHidden(true)).isTrue();
        assertThat(childEntity2.isHidden(false)).isFalse();
    }

    @Test
    @Ignore
    public void setHidden_modifiesReforms() throws Exception {
        PanelEntity testEntity = createPanelEntity();
        FakeNode testNode = (FakeNode) ((AndroidXrEntity) testEntity).getNode();

        assertThat(
                        testEntity.addComponent(
                                realityCoreRuntime.createMovableComponent(
                                        /* systemMovable= */ true,
                                        /* scaleInZ= */ true,
                                        /* anchorPlacement= */ ImmutableSet.of(),
                                        /* shouldDisposeParentAnchor= */ true)))
                .isTrue();
        testEntity.setHidden(true);
        assertThat(testNode.getReformOptions().getEnabledReform()).isEqualTo(0);
        testEntity.setHidden(false);
        assertThat(testNode.getReformOptions().getEnabledReform())
                .isEqualTo(ReformOptions.ALLOW_MOVE);
    }

    @Test
    @Ignore
    public void loggingEntityAddChildren() {
        Pose pose = new Pose();
        LoggingEntity childEntity1 = realityCoreRuntime.createLoggingEntity(pose);
        LoggingEntity childEntity2 = realityCoreRuntime.createLoggingEntity(pose);
        LoggingEntity parentEntity = realityCoreRuntime.createLoggingEntity(pose);

        parentEntity.addChild(childEntity1);

        assertThat(parentEntity.getChildren()).containsExactly(childEntity1);

        parentEntity.addChildren(ImmutableList.of(childEntity2));

        assertThat(childEntity1.getParent()).isEqualTo(parentEntity);
        assertThat(childEntity2.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getChildren()).containsExactly(childEntity1, childEntity2);
    }

    @Test
    @Ignore
    public void getActivitySpace_returnsEntity() {
        ActivitySpace activitySpace = realityCoreRuntime.getActivitySpace();

        assertThat(activitySpace).isNotNull();
        // Verify that there is an underlying extension node.
        ActivitySpaceImpl activitySpaceImpl = (ActivitySpaceImpl) activitySpace;
        assertThat(activitySpaceImpl.getNode()).isNotNull();
    }

    @Test
    @Ignore
    public void getActivitySpaceRootImpl_returnsEntity() {
        Entity activitySpaceRoot = realityCoreRuntime.getActivitySpaceRootImpl();
        assertThat(activitySpaceRoot).isNotNull();

        // Verify that there is an underlying extension node.
        AndroidXrEntity activitySpaceRootImpl = (AndroidXrEntity) activitySpaceRoot;
        assertThat(activitySpaceRootImpl.getNode()).isNotNull();
    }

    @Test
    @Ignore
    public void getEnvironment_returnsEnvironment() {
        SpatialEnvironment environment = realityCoreRuntime.getSpatialEnvironment();
        assertThat(environment).isNotNull();
    }

    @Test
    @Ignore
    public void getHeadActivityPose_returnsNullIfNotReady() {
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.getHeadPose()).thenReturn(null);
        HeadActivityPose headActivityPose = realityCoreRuntime.getHeadActivityPose();

        assertThat(headActivityPose).isNull();
    }

    @Test
    @Ignore
    public void getHeadActivityPose_returnsActivityPose() {
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.getHeadPose())
                .thenReturn(androidx.xr.scenecore.impl.perception.Pose.identity());
        HeadActivityPose headActivityPose = realityCoreRuntime.getHeadActivityPose();

        assertThat(headActivityPose).isNotNull();
    }

    @Test
    @Ignore
    public void getCameraViewActivityPose_returnsNullIfNotReady() {
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.getStereoViews()).thenReturn(new ViewProjections(null, null));

        CameraViewActivityPose leftCameraViewActivityPose =
                realityCoreRuntime.getCameraViewActivityPose(
                        CameraViewActivityPose.CAMERA_TYPE_LEFT_EYE);
        CameraViewActivityPose rightCameraViewActivityPose =
                realityCoreRuntime.getCameraViewActivityPose(
                        CameraViewActivityPose.CAMERA_TYPE_RIGHT_EYE);

        assertThat(leftCameraViewActivityPose).isNull();
        assertThat(rightCameraViewActivityPose).isNull();
    }

    @Test
    @Ignore
    public void getLeftCameraViewActivityPose_returnsActivityPose() {
        when(perceptionLibrary.getSession()).thenReturn(session);
        ViewProjection viewProjection =
                new ViewProjection(
                        androidx.xr.scenecore.impl.perception.Pose.identity(), new Fov(0, 0, 0, 0));
        when(session.getStereoViews())
                .thenReturn(new ViewProjections(viewProjection, viewProjection));
        CameraViewActivityPose cameraViewActivityPose =
                realityCoreRuntime.getCameraViewActivityPose(
                        CameraViewActivityPose.CAMERA_TYPE_LEFT_EYE);

        assertThat(cameraViewActivityPose).isNotNull();
    }

    @Test
    @Ignore
    public void getRightCameraViewActivityPose_returnsActivityPose() {
        when(perceptionLibrary.getSession()).thenReturn(session);
        ViewProjection viewProjection =
                new ViewProjection(
                        androidx.xr.scenecore.impl.perception.Pose.identity(), new Fov(0, 0, 0, 0));
        when(session.getStereoViews())
                .thenReturn(new ViewProjections(viewProjection, viewProjection));
        CameraViewActivityPose cameraViewActivityPose =
                realityCoreRuntime.getCameraViewActivityPose(
                        CameraViewActivityPose.CAMERA_TYPE_RIGHT_EYE);

        assertThat(cameraViewActivityPose).isNotNull();
    }

    @Test
    @Ignore
    public void getUnknownCameraViewActivityPose_returnsEmptyOptional() {
        CameraViewActivityPose cameraViewActivityPose =
                realityCoreRuntime.getCameraViewActivityPose(555);

        assertThat(cameraViewActivityPose).isNull();
    }

    @Test
    @Ignore
    public void loadExrImageByAssetName_returnsImage() throws Exception {
        ListenableFuture<ExrImageResource> imageFuture =
                realityCoreRuntime.loadExrImageByAssetName("FakeAsset.exr");

        assertThat(imageFuture).isNotNull();

        ExrImageResource image = imageFuture.get();
        assertThat(image).isNotNull();
        ExrImageResourceImpl imageImpl = (ExrImageResourceImpl) image;
        assertThat(imageImpl).isNotNull();
        FakeEnvironmentToken token = (FakeEnvironmentToken) imageImpl.getToken();
        assertThat(token).isNotNull();
        assertThat(token.getUrl()).isEqualTo("FakeAsset.exr");
    }

    @Test
    @Ignore
    public void loadGltfByAssetName_returnsModel() throws Exception {
        ListenableFuture<GltfModelResource> modelFuture =
                realityCoreRuntime.loadGltfByAssetName("FakeAsset.glb");

        assertThat(modelFuture).isNotNull();

        GltfModelResource model = modelFuture.get();
        assertThat(model).isNotNull();
        GltfModelResourceImpl modelImpl = (GltfModelResourceImpl) model;
        assertThat(modelImpl).isNotNull();
        FakeGltfModelToken token = (FakeGltfModelToken) modelImpl.getExtensionModelToken();
        assertThat(token).isNotNull();
        assertThat(token.getUrl()).isEqualTo("FakeAsset.glb");
    }

    @Test
    @Ignore
    public void createGltfEntity_returnsEntity() throws Exception {
        assertThat(createGltfEntity()).isNotNull();
    }

    @Test
    @Ignore
    public void createGltfEntitySplitEngine_returnsEntity() throws Exception {
        assertThat(createGltfEntitySplitEngine()).isNotNull();
    }

    @Test
    @Ignore
    public void animateGltfEntitySplitEngine_gltfEntityIsAnimating() throws Exception {
        GltfEntity gltfEntitySplitEngine = createGltfEntitySplitEngine();
        gltfEntitySplitEngine.startAnimation(false, "animation_name");
        int animatingNodes = fakeImpressApi.impressNodeAnimatingSize();
        int loopingAnimatingNodes = fakeImpressApi.impressNodeLoopAnimatingSize();

        // The fakeJniApi returns a future which immediately fires, which makes it seem like the
        // animation is done immediately. This makes it look like the animation stopped right away.
        assertThat(gltfEntitySplitEngine.getAnimationState())
                .isEqualTo(GltfEntity.AnimationState.PLAYING);
        assertThat(animatingNodes).isEqualTo(1);
        assertThat(loopingAnimatingNodes).isEqualTo(0);
    }

    @Test
    @Ignore
    public void animateLoopGltfEntitySplitEngine_gltfEntityIsAnimatingInLoop() throws Exception {
        GltfEntity gltfEntitySplitEngine = createGltfEntitySplitEngine();
        gltfEntitySplitEngine.startAnimation(true, "animation_name");
        int animatingNodes = fakeImpressApi.impressNodeAnimatingSize();
        int loopingAnimatingNodes = fakeImpressApi.impressNodeLoopAnimatingSize();

        assertThat(gltfEntitySplitEngine.getAnimationState())
                .isEqualTo(GltfEntity.AnimationState.PLAYING);
        assertThat(animatingNodes).isEqualTo(0);
        assertThat(loopingAnimatingNodes).isEqualTo(1);
    }

    @Test
    @Ignore
    public void stopAnimateGltfEntitySplitEngine_gltfEntityStopsAnimating() throws Exception {
        GltfEntity gltfEntitySplitEngine = createGltfEntitySplitEngine();
        gltfEntitySplitEngine.startAnimation(true, "animation_name");
        gltfEntitySplitEngine.stopAnimation();
        int animatingNodes = fakeImpressApi.impressNodeAnimatingSize();
        int loopingAnimatingNodes = fakeImpressApi.impressNodeLoopAnimatingSize();

        assertThat(gltfEntitySplitEngine.getAnimationState())
                .isEqualTo(GltfEntity.AnimationState.STOPPED);
        assertThat(animatingNodes).isEqualTo(0);
        assertThat(loopingAnimatingNodes).isEqualTo(0);
    }

    @Test
    @Ignore
    public void gltfEntitySetParent() throws Exception {
        GltfEntity childEntity = createGltfEntity();
        GltfEntity parentEntity = createGltfEntity();

        childEntity.setParent(parentEntity);

        assertThat(childEntity.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getParent())
                .isEqualTo(realityCoreRuntime.getActivitySpaceRootImpl());
        assertThat(childEntity.getChildren()).isEmpty();
        assertThat(parentEntity.getChildren()).containsExactly(childEntity);

        // Verify that there is an underlying extension node relationship.
        FakeNode childNode = (FakeNode) ((GltfEntityImpl) childEntity).getNode();
        assertThat(childNode.getParent()).isEqualTo(((GltfEntityImpl) parentEntity).getNode());
    }

    @Test
    @Ignore
    public void gltfEntityUpdateParent() throws Exception {
        GltfEntity childEntity = createGltfEntity();
        GltfEntity parentEntity1 = createGltfEntity();
        GltfEntity parentEntity2 = createGltfEntity();

        childEntity.setParent(parentEntity1);
        assertThat(childEntity.getParent()).isEqualTo(parentEntity1);
        assertThat(parentEntity1.getChildren()).containsExactly(childEntity);
        assertThat(parentEntity2.getChildren()).isEmpty();

        FakeNode childNode = (FakeNode) ((GltfEntityImpl) childEntity).getNode();
        assertThat(childNode.getParent()).isEqualTo(((GltfEntityImpl) parentEntity1).getNode());

        childEntity.setParent(parentEntity2);
        assertThat(childEntity.getParent()).isEqualTo(parentEntity2);
        assertThat(parentEntity2.getChildren()).containsExactly(childEntity);
        assertThat(parentEntity1.getChildren()).isEmpty();
        assertThat(childNode.getParent()).isEqualTo(((GltfEntityImpl) parentEntity2).getNode());
    }

    @Test
    @Ignore
    public void gltfEntityAddChildren() throws Exception {
        GltfEntity childEntity1 = createGltfEntity();
        GltfEntity childEntity2 = createGltfEntity();
        GltfEntity parentEntity = createGltfEntity();

        parentEntity.addChild(childEntity1);

        assertThat(parentEntity.getChildren()).containsExactly(childEntity1);

        parentEntity.addChildren(ImmutableList.of(childEntity2));

        assertThat(childEntity1.getParent()).isEqualTo(parentEntity);
        assertThat(childEntity2.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getChildren()).containsExactly(childEntity1, childEntity2);

        FakeNode childNode1 = (FakeNode) ((GltfEntityImpl) childEntity1).getNode();
        assertThat(childNode1.getParent()).isEqualTo(((GltfEntityImpl) parentEntity).getNode());
        FakeNode childNode2 = (FakeNode) ((GltfEntityImpl) childEntity2).getNode();
        assertThat(childNode2.getParent()).isEqualTo(((GltfEntityImpl) parentEntity).getNode());
    }

    @Test
    @Ignore
    public void createPanelEntity_returnsEntity() throws Exception {
        assertThat(createPanelEntity()).isNotNull();
    }

    @Test
    @Ignore
    public void allPanelEnities_haveActivitySpaceRootImplAsParentByDefault() throws Exception {
        PanelEntity panelEntity = createPanelEntity();

        assertThat(panelEntity.getParent())
                .isEqualTo(realityCoreRuntime.getActivitySpaceRootImpl());
    }

    @Test
    @Ignore
    public void panelEntitySetParent_setsParent() throws Exception {
        PanelEntity childEntity = createPanelEntity();
        PanelEntity parentEntity = createPanelEntity();

        childEntity.setParent(parentEntity);

        assertThat(childEntity.getParent()).isEqualTo(parentEntity);
        assertThat(childEntity.getChildren()).isEmpty();
        assertThat(parentEntity.getChildren()).containsExactly(childEntity);

        // Verify that there is an underlying extension node relationship.
        FakeNode childNode = (FakeNode) ((PanelEntityImpl) childEntity).getNode();
        assertThat(childNode.getParent()).isEqualTo(((PanelEntityImpl) parentEntity).getNode());
    }

    @Test
    @Ignore
    public void panelEntityUpdateParent_updatesParent() throws Exception {
        PanelEntity childEntity = createPanelEntity();
        PanelEntity parentEntity1 = createPanelEntity();
        PanelEntity parentEntity2 = createPanelEntity();

        childEntity.setParent(parentEntity1);
        assertThat(childEntity.getParent()).isEqualTo(parentEntity1);
        assertThat(parentEntity1.getChildren()).containsExactly(childEntity);
        assertThat(parentEntity2.getChildren()).isEmpty();

        FakeNode childNode = (FakeNode) ((PanelEntityImpl) childEntity).getNode();
        assertThat(childNode.getParent()).isEqualTo(((PanelEntityImpl) parentEntity1).getNode());

        childEntity.setParent(parentEntity2);
        assertThat(childEntity.getParent()).isEqualTo(parentEntity2);
        assertThat(parentEntity2.getChildren()).containsExactly(childEntity);
        assertThat(parentEntity1.getChildren()).isEmpty();
        assertThat(childNode.getParent()).isEqualTo(((PanelEntityImpl) parentEntity2).getNode());
    }

    @Test
    @Ignore
    public void panelEntityAddChildren_addsChildren() throws Exception {
        PanelEntity childEntity1 = createPanelEntity();
        PanelEntity childEntity2 = createPanelEntity();
        PanelEntity parentEntity = createPanelEntity();

        parentEntity.addChild(childEntity1);

        assertThat(parentEntity.getChildren()).containsExactly(childEntity1);

        parentEntity.addChildren(ImmutableList.of(childEntity2));

        assertThat(childEntity1.getParent()).isEqualTo(parentEntity);
        assertThat(childEntity2.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getChildren()).containsExactly(childEntity1, childEntity2);

        FakeNode childNode1 = (FakeNode) ((PanelEntityImpl) childEntity1).getNode();
        assertThat(childNode1.getParent()).isEqualTo(((PanelEntityImpl) parentEntity).getNode());
        FakeNode childNode2 = (FakeNode) ((PanelEntityImpl) childEntity2).getNode();
        assertThat(childNode2.getParent()).isEqualTo(((PanelEntityImpl) parentEntity).getNode());
    }

    @Test
    @Ignore
    public void createAnchorEntity_returnsAndInitsAnchor() throws Exception {
        Dimensions anchorDimensions = new Dimensions(2f, 5f, 0f);
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                androidx.xr.scenecore.impl.perception.Pose.identity();
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                perceptionPose,
                                3.0f,
                                5.0f,
                                Plane.Type.VERTICAL.intValue,
                                Plane.Label.WALL.intValue));
        when(plane.createAnchor(eq(perceptionPose), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        AnchorEntity anchorEntity =
                realityCoreRuntime.createAnchorEntity(
                        anchorDimensions, PlaneType.VERTICAL, PlaneSemantic.WALL, Duration.ZERO);

        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
    }

    @Test
    @Ignore
    public void getMainPanelEntity_returnsPanelEntity() throws Exception {
        assertThat(realityCoreRuntime.getMainPanelEntity()).isNotNull();
    }

    @Test
    @Ignore
    public void getMainPanelEntity_usesWindowLeashNode() throws Exception {
        PanelEntity mainPanel = realityCoreRuntime.getMainPanelEntity();

        assertThat(((MainPanelEntityImpl) mainPanel).getNode())
                .isEqualTo(fakeExtensions.getFakeNodeForMainWindow());
    }

    @Test
    @Ignore
    public void addInputEventConsumerToEntity_setsUpNodeListener() {
        InputEventListener mockConsumer = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        Executor executor = directExecutor();
        panelEntity.addInputEventListener(executor, mockConsumer);
        FakeNode node = (FakeNode) ((PanelEntityImpl) panelEntity).getNode();

        assertThat(node.getListener()).isNotNull();
        assertThat(node.getExecutor()).isEqualTo(fakeExecutor);

        FakeInputEvent inputEvent = new FakeInputEvent();
        inputEvent.setOrigin(new Vec3(0, 0, 0));
        inputEvent.setDirection(new Vec3(1, 1, 1));

        node.sendInputEvent(inputEvent);
        fakeExecutor.runAll();

        verify(mockConsumer).onInputEvent(any());
    }

    @Test
    @Ignore
    public void inputEvent_hasHitInfo() {
        InputEventListener mockConsumer = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        Executor executor = directExecutor();
        panelEntity.addInputEventListener(executor, mockConsumer);
        FakeNode node = (FakeNode) ((PanelEntityImpl) panelEntity).getNode();
        FakeInputEvent xrInputEvent = new FakeInputEvent();
        xrInputEvent.setOrigin(new Vec3(0, 0, 0));
        xrInputEvent.setDirection(new Vec3(1, 1, 1));
        FakeHitInfo hitInfo = new FakeHitInfo();
        hitInfo.setInputNode(node);
        hitInfo.setHitPosition(new Vec3(1, 2, 3));
        hitInfo.setTransform(new Mat4f(new float[16]));
        xrInputEvent.setFakeHitInfo(hitInfo);

        node.sendInputEvent(xrInputEvent);
        fakeExecutor.runAll();

        ArgumentCaptor<InputEvent> inputEventCaptor = ArgumentCaptor.forClass(InputEvent.class);
        verify(mockConsumer).onInputEvent(inputEventCaptor.capture());
        InputEvent capturedEvent = inputEventCaptor.getValue();
        assertThat(capturedEvent.hitInfo).isNotNull();
        assertThat(capturedEvent.hitInfo.inputEntity).isEqualTo(panelEntity);
        assertThat(capturedEvent.hitInfo.hitPosition).isEqualTo(new Vector3(1, 2, 3));
    }

    @Test
    @Ignore
    public void passingNullExecutorWhenAddingConsumer_usesInternalExecutor() {
        InputEventListener mockConsumer = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        panelEntity.addInputEventListener(/* executor= */ null, mockConsumer);
        FakeNode node = (FakeNode) ((PanelEntityImpl) panelEntity).getNode();

        assertThat(node.getListener()).isNotNull();
        assertThat(node.getExecutor()).isNotNull();
    }

    @Test
    @Ignore
    public void addMultipleInputEventConsumerToEntity_setsUpInputCallbacksForAll() {
        InputEventListener mockConsumer1 = mock(InputEventListener.class);
        InputEventListener mockConsumer2 = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        Executor executor = directExecutor();
        panelEntity.addInputEventListener(executor, mockConsumer1);
        panelEntity.addInputEventListener(executor, mockConsumer2);
        FakeNode node = (FakeNode) ((PanelEntityImpl) panelEntity).getNode();
        FakeInputEvent inputEvent = new FakeInputEvent();
        inputEvent.setOrigin(new Vec3(0, 0, 0));
        inputEvent.setDirection(new Vec3(1, 1, 1));

        node.sendInputEvent(inputEvent);
        fakeExecutor.runAll();

        verify(mockConsumer1).onInputEvent(any());
        verify(mockConsumer2).onInputEvent(any());
    }

    @Test
    @Ignore
    public void addMultipleInputEventConsumersToEntity_setsUpInputCallbacksOnGivenExecutors() {
        InputEventListener mockConsumer1 = mock(InputEventListener.class);
        InputEventListener mockConsumer2 = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        FakeScheduledExecutorService executor1 = new FakeScheduledExecutorService();
        FakeScheduledExecutorService executor2 = new FakeScheduledExecutorService();
        panelEntity.addInputEventListener(executor1, mockConsumer1);
        panelEntity.addInputEventListener(executor2, mockConsumer2);
        FakeNode node = (FakeNode) ((PanelEntityImpl) panelEntity).getNode();
        FakeInputEvent inputEvent = new FakeInputEvent();
        inputEvent.setOrigin(new Vec3(0, 0, 0));
        inputEvent.setDirection(new Vec3(1, 1, 1));

        node.sendInputEvent(inputEvent);
        fakeExecutor.runAll();

        assertThat(executor1.hasNext()).isTrue();
        assertThat(executor2.hasNext()).isTrue();

        executor1.runAll();
        executor2.runAll();

        verify(mockConsumer1).onInputEvent(any());
        verify(mockConsumer2).onInputEvent(any());
    }

    @Test
    @Ignore
    public void removeInputEventConsumerToEntity_removesFromCallbacks() {
        InputEventListener mockConsumer1 = mock(InputEventListener.class);
        InputEventListener mockConsumer2 = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        Executor executor = directExecutor();
        panelEntity.addInputEventListener(executor, mockConsumer1);
        panelEntity.addInputEventListener(executor, mockConsumer2);
        FakeNode node = (FakeNode) ((PanelEntityImpl) panelEntity).getNode();
        FakeInputEvent inputEvent = new FakeInputEvent();
        inputEvent.setOrigin(new Vec3(0, 0, 0));
        inputEvent.setDirection(new Vec3(1, 1, 1));

        node.sendInputEvent(inputEvent);
        fakeExecutor.runAll();

        panelEntity.removeInputEventListener(mockConsumer1);

        node.sendInputEvent(inputEvent);
        fakeExecutor.runAll();

        verify(mockConsumer2, times(2)).onInputEvent(any());
        verify(mockConsumer1).onInputEvent(any());
    }

    @Test
    @Ignore
    public void removeAllInputEventConsumers_stopsInputListening() {
        InputEventListener mockConsumer1 = mock(InputEventListener.class);
        InputEventListener mockConsumer2 = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        Executor executor = directExecutor();
        panelEntity.addInputEventListener(executor, mockConsumer1);
        panelEntity.addInputEventListener(executor, mockConsumer2);
        FakeNode node = (FakeNode) ((PanelEntityImpl) panelEntity).getNode();
        FakeInputEvent inputEvent = new FakeInputEvent();
        inputEvent.setOrigin(new Vec3(0, 0, 0));
        inputEvent.setDirection(new Vec3(1, 1, 1));

        node.sendInputEvent(inputEvent);
        fakeExecutor.runAll();

        verify(mockConsumer1).onInputEvent(any());
        verify(mockConsumer2).onInputEvent(any());

        panelEntity.removeInputEventListener(mockConsumer1);
        panelEntity.removeInputEventListener(mockConsumer2);

        assertThat(((PanelEntityImpl) panelEntity).inputEventListenerMap).isEmpty();
        assertThat(node.getListener()).isNull();
        assertThat(node.getExecutor()).isNull();
    }

    @Test
    @Ignore
    public void dispose_stopsInputListening() {
        InputEventListener mockConsumer1 = mock(InputEventListener.class);
        InputEventListener mockConsumer2 = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        Executor executor = directExecutor();
        panelEntity.addInputEventListener(executor, mockConsumer1);
        panelEntity.addInputEventListener(executor, mockConsumer2);
        FakeNode node = (FakeNode) ((PanelEntityImpl) panelEntity).getNode();
        FakeInputEvent inputEvent = new FakeInputEvent();
        inputEvent.setOrigin(new Vec3(0, 0, 0));
        inputEvent.setDirection(new Vec3(1, 1, 1));

        node.sendInputEvent(inputEvent);
        fakeExecutor.runAll();

        verify(mockConsumer1).onInputEvent(any());
        verify(mockConsumer2).onInputEvent(any());

        panelEntity.dispose();

        assertThat(((PanelEntityImpl) panelEntity).inputEventListenerMap).isEmpty();
        assertThat(node.getListener()).isNull();
        assertThat(node.getExecutor()).isNull();
    }

    @Test
    @Ignore
    public void createContentlessEntity_returnsEntity() throws Exception {
        assertThat(createContentlessEntity()).isNotNull();
    }

    @Test
    @Ignore
    public void contentlessEntity_hasActivitySpaceRootImplAsParentByDefault() throws Exception {
        Entity entity = createContentlessEntity();
        assertThat(entity.getParent()).isEqualTo(realityCoreRuntime.getActivitySpaceRootImpl());
    }

    @Test
    @Ignore
    public void contentlessEntityAddChildren_addsChildren() throws Exception {
        Entity childEntity1 = createContentlessEntity();
        Entity childEntity2 = createContentlessEntity();
        Entity parentEntity = createContentlessEntity();

        parentEntity.addChild(childEntity1);

        assertThat(parentEntity.getChildren()).containsExactly(childEntity1);

        parentEntity.addChildren(ImmutableList.of(childEntity2));

        assertThat(childEntity1.getParent()).isEqualTo(parentEntity);
        assertThat(childEntity2.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getChildren()).containsExactly(childEntity1, childEntity2);

        FakeNode childNode1 = (FakeNode) ((AndroidXrEntity) childEntity1).getNode();
        assertThat(childNode1.getParent()).isEqualTo(((AndroidXrEntity) parentEntity).getNode());
        FakeNode childNode2 = (FakeNode) ((AndroidXrEntity) childEntity2).getNode();
        assertThat(childNode2.getParent()).isEqualTo(((AndroidXrEntity) parentEntity).getNode());
    }

    @Test
    @Ignore
    public void addComponent_callsOnAttach() throws Exception {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = realityCoreRuntime.createLoggingEntity(new Pose());
        Component component = mock(Component.class);
        when(component.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component)).isTrue();
        verify(component).onAttach(panelEntity);

        assertThat(gltfEntity.addComponent(component)).isTrue();
        verify(component).onAttach(gltfEntity);

        assertThat(loggingEntity.addComponent(component)).isTrue();
        verify(component).onAttach(loggingEntity);
    }

    @Test
    @Ignore
    public void addComponent_failsIfOnAttachFails() throws Exception {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = realityCoreRuntime.createLoggingEntity(new Pose());
        Component component = mock(Component.class);
        when(component.onAttach(any())).thenReturn(false);

        assertThat(panelEntity.addComponent(component)).isFalse();
        verify(component).onAttach(panelEntity);

        assertThat(gltfEntity.addComponent(component)).isFalse();
        verify(component).onAttach(gltfEntity);

        assertThat(loggingEntity.addComponent(component)).isFalse();
        verify(component).onAttach(loggingEntity);
    }

    @Test
    @Ignore
    public void removeComponent_callsOnDetach() throws Exception {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = realityCoreRuntime.createLoggingEntity(new Pose());
        Component component = mock(Component.class);
        when(component.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component)).isTrue();
        verify(component).onAttach(panelEntity);

        panelEntity.removeComponent(component);
        verify(component).onDetach(panelEntity);

        assertThat(gltfEntity.addComponent(component)).isTrue();
        verify(component).onAttach(gltfEntity);

        gltfEntity.removeComponent(component);
        verify(component).onDetach(gltfEntity);

        assertThat(loggingEntity.addComponent(component)).isTrue();
        verify(component).onAttach(loggingEntity);

        loggingEntity.removeComponent(component);
        verify(component).onDetach(loggingEntity);
    }

    @Test
    @Ignore
    public void addingSameComponentTypeAgain_addsComponent() throws Exception {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = realityCoreRuntime.createLoggingEntity(new Pose());
        Component component1 = mock(Component.class);
        Component component2 = mock(Component.class);
        when(component1.onAttach(any())).thenReturn(true);
        when(component2.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component1)).isTrue();
        assertThat(panelEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(panelEntity);
        verify(component2).onAttach(panelEntity);

        assertThat(gltfEntity.addComponent(component1)).isTrue();
        assertThat(gltfEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(gltfEntity);
        verify(component2).onAttach(panelEntity);

        assertThat(loggingEntity.addComponent(component1)).isTrue();
        assertThat(loggingEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(loggingEntity);
        verify(component2).onAttach(panelEntity);
    }

    @Test
    @Ignore
    public void addingDifferentComponentType_addComponentSucceeds() throws Exception {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = realityCoreRuntime.createLoggingEntity(new Pose());
        Component component1 = mock(Component.class);
        Component component2 = mock(FakeComponent.class);
        when(component1.onAttach(any())).thenReturn(true);
        when(component2.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component1)).isTrue();
        assertThat(panelEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(panelEntity);
        verify(component2).onAttach(panelEntity);

        assertThat(gltfEntity.addComponent(component1)).isTrue();
        assertThat(gltfEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(gltfEntity);
        verify(component2).onAttach(gltfEntity);

        assertThat(loggingEntity.addComponent(component1)).isTrue();
        assertThat(loggingEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(loggingEntity);
        verify(component2).onAttach(loggingEntity);
    }

    @Test
    @Ignore
    public void removeAll_callsOnDetachOnAll() throws Exception {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = realityCoreRuntime.createLoggingEntity(new Pose());
        Component component1 = mock(Component.class);
        Component component2 = mock(FakeComponent.class);
        when(component1.onAttach(any())).thenReturn(true);
        when(component2.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component1)).isTrue();
        assertThat(panelEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(panelEntity);
        verify(component2).onAttach(panelEntity);

        panelEntity.removeAllComponents();
        verify(component1).onDetach(panelEntity);
        verify(component2).onDetach(panelEntity);

        assertThat(gltfEntity.addComponent(component1)).isTrue();
        assertThat(gltfEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(gltfEntity);
        verify(component2).onAttach(gltfEntity);

        gltfEntity.removeAllComponents();
        verify(component1).onDetach(gltfEntity);
        verify(component2).onDetach(gltfEntity);

        assertThat(loggingEntity.addComponent(component1)).isTrue();
        assertThat(loggingEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(loggingEntity);
        verify(component2).onAttach(loggingEntity);

        loggingEntity.removeAllComponents();
        verify(component1).onDetach(loggingEntity);
        verify(component2).onDetach(loggingEntity);
    }

    @Test
    @Ignore
    public void addSameComponentTwice_callsOnAttachTwice() throws Exception {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = realityCoreRuntime.createLoggingEntity(new Pose());
        Component component = mock(Component.class);
        when(component.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component)).isTrue();
        assertThat(panelEntity.addComponent(component)).isTrue();
        verify(component, times(2)).onAttach(panelEntity);

        assertThat(gltfEntity.addComponent(component)).isTrue();
        assertThat(gltfEntity.addComponent(component)).isTrue();
        verify(component, times(2)).onAttach(gltfEntity);

        assertThat(loggingEntity.addComponent(component)).isTrue();
        assertThat(loggingEntity.addComponent(component)).isTrue();
        verify(component, times(2)).onAttach(loggingEntity);
    }

    @Test
    @Ignore
    public void removeSameComponentTwice_callsOnDetachOnce() throws Exception {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = realityCoreRuntime.createLoggingEntity(new Pose());
        Component component = mock(Component.class);
        when(component.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component)).isTrue();
        verify(component).onAttach(panelEntity);

        panelEntity.removeComponent(component);
        panelEntity.removeComponent(component);
        verify(component).onDetach(panelEntity);

        assertThat(gltfEntity.addComponent(component)).isTrue();
        verify(component).onAttach(gltfEntity);

        gltfEntity.removeComponent(component);
        gltfEntity.removeComponent(component);
        verify(component).onDetach(gltfEntity);

        assertThat(loggingEntity.addComponent(component)).isTrue();
        verify(component).onAttach(loggingEntity);

        loggingEntity.removeComponent(component);
        loggingEntity.removeComponent(component);
        verify(component).onDetach(loggingEntity);
    }

    @Test
    @Ignore
    public void createInteractableComponent_returnsComponent() {
        InputEventListener mockConsumer = mock(InputEventListener.class);
        InteractableComponent interactableComponent =
                realityCoreRuntime.createInteractableComponent(directExecutor(), mockConsumer);
        assertThat(interactableComponent).isNotNull();
    }

    @Test
    @Ignore
    public void createPersistedAnchorEntity_returnsEntityInNominalCase() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.createAnchorFromUuid(any())).thenReturn(anchor);
        assertThat(
                        realityCoreRuntime.createPersistedAnchorEntity(
                                UUID.randomUUID(), /* searchTimeout= */ Duration.ofSeconds(1)))
                .isNotNull();
    }

    @Test
    @Ignore
    public void createPersistedAnchorEntity_returnsEntityForNullSession() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(null);
        assertThat(
                        realityCoreRuntime.createPersistedAnchorEntity(
                                UUID.randomUUID(), /* searchTimeout= */ Duration.ofSeconds(1)))
                .isNotNull();
    }

    @Test
    @Ignore
    public void createPersistedAnchorEntity_returnsEntityForNullAnchor() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.createAnchorFromUuid(any())).thenReturn(null);
        assertThat(
                        realityCoreRuntime.createPersistedAnchorEntity(
                                UUID.randomUUID(), /* searchTimeout= */ Duration.ofSeconds(1)))
                .isNotNull();
    }

    @Test
    @Ignore
    public void createPersistedAnchorEntity_returnsEntityForNullAnchorToken() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.createAnchorFromUuid(any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(null);
        UUID uuid = UUID.randomUUID();
        assertThat(
                        realityCoreRuntime.createPersistedAnchorEntity(
                                uuid, /* searchTimeout= */ Duration.ofSeconds(1)))
                .isNotNull();
        verify(perceptionLibrary, times(3)).getSession();
        verify(session).createAnchorFromUuid(uuid);
        verify(anchor).getAnchorToken();
    }

    @Test
    @Ignore
    public void unpersistAnchor_failsWhenSessionIsNotInitialized() {
        when(perceptionLibrary.getSession()).thenReturn(null);
        assertThat(realityCoreRuntime.unpersistAnchor(UUID.randomUUID())).isFalse();
    }

    @Test
    @Ignore
    public void unpersistAnchor_sessionIsInitialized_operationSucceeds() {
        when(perceptionLibrary.getSession()).thenReturn(session);
        UUID uuid = UUID.randomUUID();
        when(session.unpersistAnchor(uuid)).thenReturn(true);
        assertThat(realityCoreRuntime.unpersistAnchor(uuid)).isTrue();
    }

    @Test
    @Ignore
    public void unpersistAnchor_sessionIsInitialized_operationFails() {
        when(perceptionLibrary.getSession()).thenReturn(session);
        UUID uuid = UUID.randomUUID();
        when(session.unpersistAnchor(uuid)).thenReturn(false);
        assertThat(realityCoreRuntime.unpersistAnchor(uuid)).isFalse();
    }

    @Test
    @Ignore
    public void createMovableComponent_returnsComponent() {
        MovableComponent movableComponent =
                realityCoreRuntime.createMovableComponent(
                        true, true, new HashSet<AnchorPlacement>(), true);
        assertThat(movableComponent).isNotNull();
    }

    @Test
    @Ignore
    public void createAnchorPlacement_returnsAnchorPlacement() {
        AnchorPlacement anchorPlacement =
                realityCoreRuntime.createAnchorPlacementForPlanes(
                        ImmutableSet.of(PlaneType.ANY), ImmutableSet.of(PlaneSemantic.ANY));
        assertThat(anchorPlacement).isNotNull();
    }

    @Test
    @Ignore
    public void createResizableComponent_returnsComponent() {
        ResizableComponent resizableComponent =
                realityCoreRuntime.createResizableComponent(
                        new Dimensions(0f, 0f, 0f), new Dimensions(5f, 5f, 5f));
        assertThat(resizableComponent).isNotNull();
    }

    @Test
    @Ignore
    public void createPointerCaptureComponent_returnsComponent() {
        PointerCaptureComponent pointerCaptureComponent =
                realityCoreRuntime.createPointerCaptureComponent(
                        null, (inputEvent) -> {}, (state) -> {});
        assertThat(pointerCaptureComponent).isNotNull();
    }

    @Test
    @Ignore
    public void dispose_clearsReformOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createContentlessEntity();
        FakeNode node = (FakeNode) entity.getNode();
        ReformOptions reformOptions = entity.getReformOptions();
        assertThat(reformOptions).isNotNull();
        reformOptions.setEnabledReform(ReformOptions.ALLOW_MOVE | ReformOptions.ALLOW_RESIZE);
        entity.dispose();
        assertThat(node.getReformOptions().getEnabledReform()).isEqualTo(0);
        assertThat(node.getReformOptions().getEventCallback()).isNull();
        assertThat(node.getReformOptions().getEventExecutor()).isNull();
    }

    @Test
    @Ignore
    public void dispose_clearsParents() {
        AndroidXrEntity entity = (AndroidXrEntity) createContentlessEntity();
        entity.setParent(realityCoreRuntime.getActivitySpaceRootImpl());
        assertThat(entity.getParent()).isNotNull();

        entity.dispose();
        assertThat(entity.getParent()).isNull();
    }

    @Test
    @Ignore
    public void setFullSpaceMode_callsExtensions() {
        Bundle bundle = Bundle.EMPTY;
        bundle = realityCoreRuntime.setFullSpaceMode(bundle);
        assertThat(bundle).isNotNull();
    }

    @Test
    @Ignore
    public void setFullSpaceModeWithEnvironmentInherited_callsExtensions() {
        Bundle bundle = Bundle.EMPTY;
        bundle = realityCoreRuntime.setFullSpaceModeWithEnvironmentInherited(bundle);
        assertThat(bundle).isNotNull();
    }

    @Test
    @Ignore
    public void setPreferredAspectRatio_callsExtensions() {
        realityCoreRuntime.setPreferredAspectRatio(activity, 1.23f);
        assertThat(fakeExtensions.getPreferredAspectRatio()).isEqualTo(1.23f);
    }

    @Test
    @Ignore
    public void createStereoSurface_returnsStereoSurface() {
        // Not a great test, since it returns the (non-SplitEngine) StereoSurfaceEntityImpl
        // and that throws this from its Ctor.
        // TODO: b/366588688 - Properly test this path once SplitEngine is fully enabled.
        assertThrows(
                UnsupportedOperationException.class,
                () ->
                        realityCoreRuntime.createStereoSurfaceEntity(
                                StereoSurfaceEntity.StereoMode.SIDE_BY_SIDE,
                                new Dimensions(1.0f, 1.0f, 1.0f),
                                new Pose(),
                                realityCoreRuntime.getActivitySpaceRootImpl()));
    }

    @Test
    @Ignore
    public void getSurfaceFromStereoSurface_returnsSurface() {
        assertThrows(
                IllegalArgumentException.class,
                () -> fakeImpressApi.getSurfaceFromStereoSurface(1));
    }

    @Test
    @Ignore
    public void setStereoModeForStereoSurface_callsExtensions() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        fakeImpressApi.setStereoModeForStereoSurface(
                                1, StereoSurfaceEntity.StereoMode.SIDE_BY_SIDE));
    }

    @Test
    @Ignore
    public void injectRootNodeAndTaskWindowLeashNode_runtimeImplUsesThoseNodes() {
        FakeNode rootNode = (FakeNode) fakeExtensions.createNode();
        FakeNode taskWindowLeashNode = (FakeNode) fakeExtensions.createNode();
        JxrPlatformAdapterAxr runtime =
                JxrPlatformAdapterAxr.create(
                        activity,
                        fakeExecutor,
                        fakeExtensions,
                        fakeImpressApi,
                        new EntityManager(),
                        perceptionLibrary,
                        splitEngineSubspaceManager,
                        splitEngineRenderer,
                        rootNode,
                        taskWindowLeashNode,
                        /* useSplitEngine= */ false);

        assertThat(((AndroidXrEntity) runtime.getActivitySpace()).getNode()).isEqualTo(rootNode);
        assertThat(((AndroidXrEntity) runtime.getMainPanelEntity()).getNode())
                .isEqualTo(taskWindowLeashNode);
    }

    @Test
    @Ignore
    public void dispose_clearsResources() {
        AndroidXrEntity entity = (AndroidXrEntity) createContentlessEntity();
        FakeNode node = (FakeNode) entity.getNode();
        assertThat(node).isNotNull();
        assertThat(node.getParent()).isNotNull();

        realityCoreRuntime.dispose();
        assertThat(node.getParent()).isNull();
        assertThat(fakeExtensions.getSpatialStateCallback()).isNull();
        assertThat(fakeExtensions.getFakeNodeForMainWindow()).isNull();
        assertThat(fakeExtensions.getFakeTaskNode()).isNull();
    }

    interface FakeComponent extends Component {}
}
