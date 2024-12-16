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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import androidx.xr.extensions.environment.EnvironmentVisibilityState;
import androidx.xr.extensions.environment.PassthroughVisibilityState;
import androidx.xr.extensions.space.SpatialState;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialEnvironment.SetPassthroughOpacityPreferenceResult;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialEnvironment.SetSpatialEnvironmentPreferenceResult;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialEnvironment.SpatialEnvironmentPreference;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeXrExtensions;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeEnvironmentVisibilityState;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeNode;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakePassthroughVisibilityState;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeSpatialState;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.List;
import java.util.function.Consumer;

// Technically this doesn't need to be a Robolectric test, since it doesn't directly depend on
// any Android subsystems. However, we're currently using an Android test runner for consistency
// with other Android XR impl tests in this directory.
/**
 * Unit tests for the AndroidXR implementation of JXRCore's SpatialEnvironment module.
 *
 * <p>TODO(b/326748782): Update the FakeExtensions to support better asserts.
 */
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings({"deprecation", "UnnecessarilyFullyQualified"}) // TODO(b/373435470): Remove
public final class SpatialEnvironmentImplTest {
    private static final int SUBSPACE_ID = 5;
    private final FakeImpressApi fakeImpressApi = new FakeImpressApi();
    private ActivityController<Activity> activityController;
    private Activity activity;
    private FakeXrExtensions fakeExtensions = null;
    private FakeNode subspaceNode;
    private SubspaceNode expectedSubspace;
    private SpatialEnvironmentImpl environment = null;
    private SplitEngineSubspaceManager splitEngineSubspaceManager;

    @Before
    public void setUp() {
        activityController = Robolectric.buildActivity(Activity.class);
        activity = activityController.create().start().get();
        // Reset our state.
        fakeExtensions = new FakeXrExtensions();
        FakeNode fakeSceneRootNode = (FakeNode) fakeExtensions.createNode();
        subspaceNode = (FakeNode) fakeExtensions.createNode();
        expectedSubspace = new SubspaceNode(SUBSPACE_ID, subspaceNode);

        splitEngineSubspaceManager = Mockito.mock(SplitEngineSubspaceManager.class);

        environment =
                new SpatialEnvironmentImpl(
                        activity,
                        fakeExtensions,
                        fakeSceneRootNode,
                        this::getSpatialState,
                        /* useSplitEngine= */ false);
        environment.onSplitEngineReady(splitEngineSubspaceManager, fakeImpressApi);
    }

    private void setupSplitEngineEnvironmentImpl() {
        FakeNode fakeSceneRootNode = (FakeNode) fakeExtensions.createNode();

        when(splitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
                .thenReturn(expectedSubspace);

        environment =
                new SpatialEnvironmentImpl(
                        activity,
                        fakeExtensions,
                        fakeSceneRootNode,
                        this::getSpatialState,
                        /* useSplitEngine= */ true);
        environment.onSplitEngineReady(splitEngineSubspaceManager, fakeImpressApi);
    }

    @SuppressWarnings({"FutureReturnValueIgnored", "AndroidJdkLibsChecker"})
    private androidx.xr.extensions.asset.EnvironmentToken fakeLoadEnvironment(String name) {
        try {
            return fakeExtensions.loadEnvironment(null, 0, 0, name).get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    @SuppressWarnings({"FutureReturnValueIgnored", "AndroidJdkLibsChecker"})
    private androidx.xr.extensions.asset.GltfModelToken fakeLoadGltfModel(String name) {
        try {
            return fakeExtensions.loadGltfModel(null, 0, 0, name).get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    @SuppressWarnings({"FutureReturnValueIgnored", "AndroidJdkLibsChecker"})
    private long fakeLoadGltfModelSplitEngine(String name) {
        try {
            return fakeImpressApi.loadGltfModel(name).get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return -1;
        }
    }

    private SpatialState getSpatialState() {
        return fakeExtensions.fakeSpatialState;
    }

    @Test
    public void setPassthroughOpacityPreference() {
        environment.setPassthroughOpacityPreference(null);
        assertThat(environment.getPassthroughOpacityPreference()).isNull();

        environment.setPassthroughOpacityPreference(0.1f);
        assertThat(environment.getPassthroughOpacityPreference()).isEqualTo(0.1f);
    }

    @Test
    public void setPassthroughOpacityPreferenceNearOrUnderZero_getsZeroOpacity() {
        // Opacity values below 1% should be treated as zero.
        environment.setPassthroughOpacityPreference(0.009f);
        assertThat(environment.getPassthroughOpacityPreference()).isEqualTo(0.0f);

        environment.setPassthroughOpacityPreference(-0.1f);
        assertThat(environment.getPassthroughOpacityPreference()).isEqualTo(0.0f);
    }

    @Test
    public void setPassthroughOpacityPreferenceNearOrOverOne_getsFullOpacity() {
        // Opacity values above 99% should be treated as full opacity.
        environment.setPassthroughOpacityPreference(0.991f);
        assertThat(environment.getPassthroughOpacityPreference()).isEqualTo(1.0f);

        environment.setPassthroughOpacityPreference(1.1f);
        assertThat(environment.getPassthroughOpacityPreference()).isEqualTo(1.0f);
    }

    @Test
    public void setPassthroughOpacityPreference_returnsAccordingToSpatialCapabilities() {
        // Change should be applied if the spatial capabilities allow it, otherwise should be
        // pending.
        fakeExtensions.fakeSpatialState.setAllSpatialCapabilities(true);
        assertThat(environment.setPassthroughOpacityPreference(0.5f))
                .isEqualTo(SetPassthroughOpacityPreferenceResult.CHANGE_APPLIED);

        fakeExtensions.fakeSpatialState.setAllSpatialCapabilities(false);
        assertThat(environment.setPassthroughOpacityPreference(0.6f))
                .isEqualTo(SetPassthroughOpacityPreferenceResult.CHANGE_PENDING);
    }

    @Test
    public void getCurrentPassthroughOpacity_returnsZeroInitially() {
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.0f);
    }

    @Test
    public void onPassthroughOpacityChangedListener_firesOnPassthroughOpacityChange() {
        @SuppressWarnings(value = "unchecked")
        Consumer<Float> listener1 = (Consumer<Float>) mock(Consumer.class);
        @SuppressWarnings(value = "unchecked")
        Consumer<Float> listener2 = (Consumer<Float>) mock(Consumer.class);

        environment.addOnPassthroughOpacityChangedListener(listener1);
        environment.addOnPassthroughOpacityChangedListener(listener2);

        environment.firePassthroughOpacityChangedEvent(0.5f);
        verify(listener1).accept(0.5f);
        verify(listener2).accept(0.5f);

        environment.removeOnPassthroughOpacityChangedListener(listener1);
        environment.firePassthroughOpacityChangedEvent(0.0f);
        verify(listener1)
                .accept(any()); // Verify the removed listener was called exactly once total
        verify(listener2).accept(0.0f); // Verify the active listener was called again with false
    }

    @Test
    public void getSpatialEnvironmentPreference_returnsSetSpatialEnvironmentPreference() {
        SpatialEnvironmentPreference preference = mock(SpatialEnvironmentPreference.class);
        environment.setSpatialEnvironmentPreference(preference);
        assertThat(environment.getSpatialEnvironmentPreference()).isEqualTo(preference);
    }

    @Test
    public void setSpatialEnvironmentPreference_returnsAppliedWhenCapable() {
        // Change should be applied if the spatial capabilities allow it, otherwise should be
        // pending.
        fakeExtensions.fakeSpatialState.setAllSpatialCapabilities(true);
        SpatialEnvironmentPreference preference = mock(SpatialEnvironmentPreference.class);
        assertThat(environment.setSpatialEnvironmentPreference(preference))
                .isEqualTo(SetSpatialEnvironmentPreferenceResult.CHANGE_APPLIED);

        fakeExtensions.fakeSpatialState.setAllSpatialCapabilities(false);
        preference = mock(SpatialEnvironmentPreference.class);
        assertThat(environment.setSpatialEnvironmentPreference(preference))
                .isEqualTo(SetSpatialEnvironmentPreferenceResult.CHANGE_PENDING);
    }

    @Test
    public void setSpatialEnvironmentPreferenceNull_removesEnvironment() {
        androidx.xr.extensions.asset.EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        androidx.xr.extensions.asset.GltfModelToken gltf = fakeLoadGltfModel("fakeGltfModel");

        // Ensure that an environment is set.
        environment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImpl(gltf)));

        FakeNode skyboxNode = fakeExtensions.testGetNodeWithEnvironmentToken(exr);
        FakeNode geometryNode = fakeExtensions.testGetNodeWithGltfToken(gltf);

        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNode).isNotNull();

        assertThat(skyboxNode.getParent()).isNotNull();
        assertThat(geometryNode.getParent()).isNotNull();

        // Ensure environment is removed
        environment.setSpatialEnvironmentPreference(null);

        assertThat(skyboxNode.getParent()).isNull();
        assertThat(geometryNode.getParent()).isNull();
        assertThat(fakeExtensions.getFakeEnvironmentNode()).isNull();
    }

    @Test
    public void setSpatialEnvironmentPreferenceNullWithSplitEngine_removesEnvironment() {
        setupSplitEngineEnvironmentImpl();

        androidx.xr.extensions.asset.EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");

        // Ensure that an environment is set.
        environment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImplSplitEngine(gltf)));

        FakeNode skyboxNode = fakeExtensions.testGetNodeWithEnvironmentToken(exr);
        List<Integer> geometryNodes = fakeImpressApi.getImpressNodesForToken(gltf);

        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNodes).isNotEmpty();

        assertThat(skyboxNode.getParent()).isNotNull();
        assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isTrue();

        // Ensure environment is removed
        environment.setSpatialEnvironmentPreference(null);

        assertThat(skyboxNode.getParent()).isNull();
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();
        assertThat(fakeExtensions.getFakeEnvironmentNode()).isNull();
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceWithNullSkyboxAndGeometry_doesNotDetachEnvironment() {
        androidx.xr.extensions.asset.EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        androidx.xr.extensions.asset.GltfModelToken gltf = fakeLoadGltfModel("fakeGltfModel");

        // Ensure that an environment is set.
        environment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImpl(gltf)));

        FakeNode skyboxNode = fakeExtensions.testGetNodeWithEnvironmentToken(exr);
        FakeNode geometryNode = fakeExtensions.testGetNodeWithGltfToken(gltf);

        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNode).isNotNull();

        assertThat(skyboxNode.getParent()).isNotNull();
        assertThat(geometryNode.getParent()).isNotNull();

        // Ensure environment is not removed if both skybox and geometry are updated to null.
        environment.setSpatialEnvironmentPreference(new SpatialEnvironmentPreference(null, null));

        assertThat(skyboxNode.getParent()).isNull();
        assertThat(geometryNode.getParent()).isNull();

        // TODO: b/371221872 - When the behavior is changed to set the black skybox, the fake env
        // node
        // will no longer be null and the commented out line should replace the uncommented line.
        // This change isn't relevant for end users but it confirms the environment implementation
        // is working as designed.
        // assertThat(fakeExtensions.getFakeEnvironmentNode()).isNotNull();
        assertThat(fakeExtensions.getFakeEnvironmentNode()).isNull();
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceWithNullSkyboxAndGeometrySplitEngine_doesNotDetachEnvironment() {
        setupSplitEngineEnvironmentImpl();
        androidx.xr.extensions.asset.EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");

        // Ensure that an environment is set.
        environment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImplSplitEngine(gltf)));

        FakeNode skyboxNode = fakeExtensions.testGetNodeWithEnvironmentToken(exr);
        List<Integer> geometryNodes = fakeImpressApi.getImpressNodesForToken(gltf);

        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNodes).isNotEmpty();

        assertThat(skyboxNode.getParent()).isNotNull();
        assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isTrue();

        // Ensure environment is not removed if both skybox and geometry are updated to null.
        environment.setSpatialEnvironmentPreference(new SpatialEnvironmentPreference(null, null));

        assertThat(skyboxNode.getParent()).isNull();
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();

        // TODO: b/371221872 - When the behavior is changed to set the black skybox, the fake env
        // node
        // will no longer be null and the commented out line should replace the uncommented line.
        // This change isn't relevant for end users but it confirms the environment implementation
        // is working as designed.
        // assertThat(fakeExtensions.getFakeEnvironmentNode()).isNotNull();
        assertThat(fakeExtensions.getFakeEnvironmentNode()).isNull();
    }

    @Test
    public void setNewSpatialEnvironmentPreference_replacesOldSpatialEnvironmentPreference() {
        androidx.xr.extensions.asset.EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        androidx.xr.extensions.asset.EnvironmentToken newExr =
                fakeLoadEnvironment("newFakeEnvironment");
        androidx.xr.extensions.asset.GltfModelToken gltf = fakeLoadGltfModel("fakeGltfModel");
        androidx.xr.extensions.asset.GltfModelToken newGltf = fakeLoadGltfModel("newFakeGltfModel");

        // Ensure that an environment is set a first time.
        environment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImpl(gltf)));

        FakeNode skyboxNode = fakeExtensions.testGetNodeWithEnvironmentToken(exr);
        FakeNode geometryNode = fakeExtensions.testGetNodeWithGltfToken(gltf);

        // Ensure that an environment is set a second time.
        environment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(newExr), new GltfModelResourceImpl(newGltf)));

        FakeNode newSkyboxNode = fakeExtensions.testGetNodeWithEnvironmentToken(newExr);
        FakeNode newGeometryNode = fakeExtensions.testGetNodeWithGltfToken(newGltf);

        // None of the nodes should be null.
        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNode).isNotNull();
        assertThat(newSkyboxNode).isNotNull();
        assertThat(newGeometryNode).isNotNull();

        // Only the new nodes should have a parent.
        assertThat(skyboxNode.getParent()).isNull();
        assertThat(geometryNode.getParent()).isNull();
        assertThat(newSkyboxNode.getParent()).isNotNull();
        assertThat(newGeometryNode.getParent()).isNotNull();

        // The names should be the same, but the resources should be different.
        assertThat(skyboxNode.getEnvironment()).isNotEqualTo(newSkyboxNode.getEnvironment());
        assertThat(skyboxNode.getName()).isEqualTo(SpatialEnvironmentImpl.SKYBOX_NODE_NAME);
        assertThat(newSkyboxNode.getName()).isEqualTo(SpatialEnvironmentImpl.SKYBOX_NODE_NAME);
        assertThat(geometryNode.getGltfModel()).isNotEqualTo(newGeometryNode.getGltfModel());
        assertThat(geometryNode.getName()).isEqualTo(SpatialEnvironmentImpl.GEOMETRY_NODE_NAME);
        assertThat(newGeometryNode.getName()).isEqualTo(SpatialEnvironmentImpl.GEOMETRY_NODE_NAME);

        // The environment node should still be attached.
        assertThat(fakeExtensions.getFakeEnvironmentNode()).isNotNull();
    }

    @Test
    public void
            setNewSpatialEnvironmentPreferenceSplitEngine_replacesOldSpatialEnvironmentPreference() {
        setupSplitEngineEnvironmentImpl();
        androidx.xr.extensions.asset.EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        androidx.xr.extensions.asset.EnvironmentToken newExr =
                fakeLoadEnvironment("newFakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");
        long newGltf = fakeLoadGltfModelSplitEngine("newFakeGltfModel");

        // Ensure that an environment is set a first time.
        environment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImplSplitEngine(gltf)));

        FakeNode skyboxNode = fakeExtensions.testGetNodeWithEnvironmentToken(exr);
        List<Integer> geometryNodes = fakeImpressApi.getImpressNodesForToken(gltf);

        // Ensure that an environment is set a second time.
        environment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(newExr),
                        new GltfModelResourceImplSplitEngine(newGltf)));

        FakeNode newSkyboxNode = fakeExtensions.testGetNodeWithEnvironmentToken(newExr);
        List<Integer> newGeometryNodes = fakeImpressApi.getImpressNodesForToken(newGltf);

        // None of the nodes should be null.
        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNodes).isNotEmpty();
        assertThat(newSkyboxNode).isNotNull();
        assertThat(newGeometryNodes).isNotEmpty();

        // Only the new nodes should have a parent.
        assertThat(skyboxNode.getParent()).isNull();
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();
        assertThat(newSkyboxNode.getParent()).isNotNull();
        assertThat(fakeImpressApi.impressNodeHasParent(newGeometryNodes.get(0))).isTrue();

        // The resources should be different.
        assertThat(skyboxNode.getEnvironment()).isNotEqualTo(newSkyboxNode.getEnvironment());
        assertThat(skyboxNode.getName()).isEqualTo(SpatialEnvironmentImpl.SKYBOX_NODE_NAME);
        assertThat(newSkyboxNode.getName()).isEqualTo(SpatialEnvironmentImpl.SKYBOX_NODE_NAME);
        assertThat(geometryNodes.get(0)).isNotEqualTo(newGeometryNodes.get(0));

        // The environment node should still be attached.
        assertThat(fakeExtensions.getFakeEnvironmentNode()).isNotNull();
    }

    @Test
    public void isSpatialEnvironmentPreferenceActive_defaultsToFalse() {
        assertThat(environment.isSpatialEnvironmentPreferenceActive()).isFalse();
    }

    @Test
    public void onSpatialEnvironmentChangedListener_firesOnEnvironmentChange() {
        @SuppressWarnings(value = "unchecked")
        Consumer<Boolean> listener1 = (Consumer<Boolean>) mock(Consumer.class);
        @SuppressWarnings(value = "unchecked")
        Consumer<Boolean> listener2 = (Consumer<Boolean>) mock(Consumer.class);

        environment.addOnSpatialEnvironmentChangedListener(listener1);
        environment.addOnSpatialEnvironmentChangedListener(listener2);

        environment.fireOnSpatialEnvironmentChangedEvent(true);
        verify(listener1).accept(true);
        verify(listener2).accept(true);

        environment.removeOnSpatialEnvironmentChangedListener(listener1);
        environment.fireOnSpatialEnvironmentChangedEvent(false);
        verify(listener1)
                .accept(any()); // Verify the removed listener was called exactly once total
        verify(listener2).accept(false); // Verify the active listener was called again with false
    }

    @Test
    public void dispose_clearsSpatialEnvironmentPreferenceListeners() {
        @SuppressWarnings(value = "unchecked")
        Consumer<Boolean> listener = (Consumer<Boolean>) mock(Consumer.class);
        environment.addOnSpatialEnvironmentChangedListener(listener);

        environment.fireOnSpatialEnvironmentChangedEvent(true);
        verify(listener).accept(true);

        environment.dispose();
        environment.fireOnSpatialEnvironmentChangedEvent(false);
        verify(listener, never()).accept(false);
    }

    @Test
    public void dispose_clearsPassthroughOpacityPreferenceListeners() {
        @SuppressWarnings(value = "unchecked")
        Consumer<Float> listener = (Consumer<Float>) mock(Consumer.class);
        environment.addOnPassthroughOpacityChangedListener(listener);

        environment.firePassthroughOpacityChangedEvent(1.0f);
        verify(listener).accept(1.0f);

        // Ensure the listener is called exactly once, even if the event is fired after dispose.
        environment.dispose();
        environment.firePassthroughOpacityChangedEvent(0.5f);
        verify(listener).accept(any());
    }

    @Test
    public void dispose_clearsResources() {
        androidx.xr.extensions.asset.EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        androidx.xr.extensions.asset.GltfModelToken gltf = fakeLoadGltfModel("fakeGltfModel");
        FakeSpatialState spatialState = new FakeSpatialState();

        spatialState.setEnvironmentVisibility(
                new FakeEnvironmentVisibilityState(EnvironmentVisibilityState.APP_VISIBLE));
        spatialState.setPassthroughVisibility(
                new FakePassthroughVisibilityState(PassthroughVisibilityState.APP, 0.5f));
        environment.setSpatialState(spatialState);

        environment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImpl(gltf)));
        environment.setPassthroughOpacityPreference(0.5f);

        FakeNode skyboxNode = fakeExtensions.testGetNodeWithEnvironmentToken(exr);
        FakeNode geometryNode = fakeExtensions.testGetNodeWithGltfToken(gltf);

        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNode).isNotNull();

        assertThat(skyboxNode.getParent()).isNotNull();
        assertThat(geometryNode.getParent()).isNotNull();

        assertThat(environment.getSpatialEnvironmentPreference()).isNotNull();
        assertThat(environment.isSpatialEnvironmentPreferenceActive()).isTrue();

        assertThat(environment.getPassthroughOpacityPreference()).isNotNull();
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.5f);

        environment.dispose();
        assertThat(skyboxNode.getParent()).isNull();
        assertThat(geometryNode.getParent()).isNull();
        assertThat(fakeExtensions.getFakeEnvironmentNode()).isNull();
        assertThat(environment.getSpatialEnvironmentPreference()).isNull();
        assertThat(environment.isSpatialEnvironmentPreferenceActive()).isFalse();
        assertThat(environment.getPassthroughOpacityPreference()).isNull();
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.0f);
    }
}
