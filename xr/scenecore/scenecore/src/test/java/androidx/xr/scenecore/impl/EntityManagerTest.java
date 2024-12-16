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
import static com.google.common.util.concurrent.Futures.immediateFuture;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.SystemClock;
import android.view.Display;
import android.view.SurfaceControlViewHost;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.xr.extensions.node.Node;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivityPanelEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.GltfEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.GltfModelResource;
import androidx.xr.scenecore.JxrPlatformAdapter.PanelEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.PixelDimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneSemantic;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneType;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.Objects;

@RunWith(RobolectricTestRunner.class)
public class EntityManagerTest {

    private static final int VGA_WIDTH = 640;
    private static final int VGA_HEIGHT = 480;
    private final FakeXrExtensions fakeExtensions = new FakeXrExtensions();
    private final FakeImpressApi fakeImpressApi = new FakeImpressApi();
    private final FakeScheduledExecutorService fakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary perceptionLibrary = mock(PerceptionLibrary.class);
    private final Session session = mock(Session.class);
    private final AndroidXrEntity activitySpaceRoot = mock(AndroidXrEntity.class);
    private final FakeScheduledExecutorService executor = new FakeScheduledExecutorService();
    private final Node panelEntityNode = fakeExtensions.createNode();
    private final Node anchorEntityNode = fakeExtensions.createNode();
    private final EntityManager entityManager = new EntityManager();
    private final SplitEngineSubspaceManager splitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    private final ImpSplitEngineRenderer splitEngineRenderer =
            Mockito.mock(ImpSplitEngineRenderer.class);
    private Node contentLessEntityNode;
    private Node gltfEntityNode;
    private Activity activity;
    private JxrPlatformAdapterAxr runtime;
    private ActivitySpaceImpl activitySpace;

    @Before
    public void setUp() {
        try (ActivityController<Activity> activityController =
                Robolectric.buildActivity(Activity.class)) {
            activity = activityController.create().start().get();
        }
        when(perceptionLibrary.initSession(eq(activity), anyInt(), eq(fakeExecutor)))
                .thenReturn(immediateFuture(session));
        runtime =
                JxrPlatformAdapterAxr.create(
                        activity,
                        fakeExecutor,
                        fakeExtensions,
                        fakeImpressApi,
                        entityManager,
                        perceptionLibrary,
                        splitEngineSubspaceManager,
                        splitEngineRenderer,
                        /* useSplitEngine= */ false);
        Node taskNode = fakeExtensions.createNode();
        this.activitySpace =
                new ActivitySpaceImpl(
                        taskNode,
                        fakeExtensions,
                        entityManager,
                        () -> fakeExtensions.fakeSpatialState,
                        executor);
        long currentTimeMillis = 1000000000L;
        SystemClock.setCurrentTimeMillis(currentTimeMillis);

        // By default, set the activity space to the root of the underlying OpenXR reference space.
        this.activitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
    }

    @Test
    public void creatingEntity_addsEntityToEntityManager() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        PanelEntity panelEntity = createPanelEntity();
        Entity contentlessEntity = createContentlessEntity();
        AnchorEntity anchorEntity = createAnchorEntity();
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();

        // Entity manager also contains the main panel entity and activity space, which are created
        // when
        // the runtime is created.
        assertThat(entityManager.getAllEntities().size()).isAtLeast(5);
        assertThat(entityManager.getAllEntities())
                .containsAtLeast(
                        gltfEntity,
                        panelEntity,
                        contentlessEntity,
                        anchorEntity,
                        activityPanelEntity);
    }

    @Test
    public void getEntityForNode_returnsEntity() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        PanelEntity panelEntity = createPanelEntity();
        Entity contentlessEntity = createContentlessEntity();
        AnchorEntity anchorEntity = createAnchorEntity();
        Node testNode = fakeExtensions.createNode();

        assertThat(entityManager.getEntityForNode(gltfEntityNode)).isEqualTo(gltfEntity);
        assertThat(entityManager.getEntityForNode(panelEntityNode)).isEqualTo(panelEntity);
        assertThat(entityManager.getEntityForNode(contentLessEntityNode))
                .isEqualTo(contentlessEntity);
        assertThat(entityManager.getEntityForNode(anchorEntityNode)).isEqualTo(anchorEntity);
        assertThat(entityManager.getEntityForNode(testNode)).isNull();
    }

    @Test
    public void getEntityByType_returnsEntityOfType() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        PanelEntity panelEntity = createPanelEntity();
        Entity contentlessEntity = createContentlessEntity();
        AnchorEntity anchorEntity = createAnchorEntity();
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();

        assertThat(entityManager.getEntitiesOfType(GltfEntity.class)).containsExactly(gltfEntity);
        // MainPanel is also a PanelEntity.
        assertThat(entityManager.getEntitiesOfType(PanelEntity.class)).contains(panelEntity);
        // Base class of all entities.
        assertThat(entityManager.getEntitiesOfType(Entity.class)).contains(contentlessEntity);
        assertThat(entityManager.getEntitiesOfType(AnchorEntity.class))
                .containsExactly(anchorEntity);
        assertThat(entityManager.getEntitiesOfType(ActivityPanelEntity.class))
                .containsExactly(activityPanelEntity);
    }

    @Test
    public void removeEntity_removesFromEntityManager() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        PanelEntity panelEntity = createPanelEntity();
        Entity contentlessEntity = createContentlessEntity();
        AnchorEntity anchorEntity = createAnchorEntity();
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();

        assertThat(entityManager.getAllEntities().size()).isAtLeast(5);
        assertThat(entityManager.getAllEntities())
                .containsAtLeast(
                        gltfEntity,
                        panelEntity,
                        contentlessEntity,
                        anchorEntity,
                        activityPanelEntity);

        entityManager.removeEntityForNode(contentLessEntityNode);

        assertThat(entityManager.getAllEntities().size()).isAtLeast(4);
        assertThat(entityManager.getAllEntities()).doesNotContain(contentlessEntity);
    }

    @Test
    public void disposeEntity_removesFromEntityManager() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        PanelEntity panelEntity = createPanelEntity();
        Entity contentlessEntity = createContentlessEntity();
        AnchorEntity anchorEntity = createAnchorEntity();
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();

        assertThat(entityManager.getAllEntities().size()).isAtLeast(5);
        assertThat(entityManager.getAllEntities())
                .containsAtLeast(
                        gltfEntity,
                        panelEntity,
                        contentlessEntity,
                        anchorEntity,
                        activityPanelEntity);

        contentlessEntity.dispose();

        assertThat(entityManager.getAllEntities().size()).isAtLeast(4);
        assertThat(entityManager.getAllEntities()).doesNotContain(contentlessEntity);
    }

    @Test
    public void clearEntityManager_removesAllEntityFromEntityManager() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        PanelEntity panelEntity = createPanelEntity();
        Entity contentlessEntity = createContentlessEntity();
        AnchorEntity anchorEntity = createAnchorEntity();
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();

        assertThat(entityManager.getAllEntities().size()).isAtLeast(5);
        assertThat(entityManager.getAllEntities())
                .containsAtLeast(
                        gltfEntity,
                        panelEntity,
                        contentlessEntity,
                        anchorEntity,
                        activityPanelEntity);

        entityManager.clear();

        assertThat(entityManager.getAllEntities()).isEmpty();
    }

    private GltfEntity createGltfEntity() throws Exception {
        ListenableFuture<GltfModelResource> modelFuture =
                runtime.loadGltfByAssetName("FakeAsset.glb");
        assertThat(modelFuture).isNotNull();
        GltfModelResource model = modelFuture.get();
        GltfEntityImpl gltfEntity =
                new GltfEntityImpl(
                        (GltfModelResourceImpl) model,
                        activitySpaceRoot,
                        fakeExtensions,
                        entityManager,
                        executor);
        gltfEntityNode = gltfEntity.getNode();
        entityManager.setEntityForNode(gltfEntityNode, gltfEntity);
        return gltfEntity;
    }

    private PanelEntity createPanelEntity() {
        Display display = activity.getSystemService(DisplayManager.class).getDisplays()[0];
        Context displayContext = activity.createDisplayContext(display);
        View view = new View(displayContext);
        view.setLayoutParams(new LayoutParams(VGA_WIDTH, VGA_HEIGHT));
        SurfaceControlViewHost surfaceControlViewHost =
                new SurfaceControlViewHost(
                        displayContext,
                        Objects.requireNonNull(displayContext.getDisplay()),
                        new Binder());
        surfaceControlViewHost.setView(view, VGA_WIDTH, VGA_HEIGHT);
        PanelEntityImpl panelEntity =
                new PanelEntityImpl(
                        panelEntityNode,
                        fakeExtensions,
                        entityManager,
                        surfaceControlViewHost,
                        new PixelDimensions(VGA_WIDTH, VGA_HEIGHT),
                        executor);
        entityManager.setEntityForNode(panelEntityNode, panelEntity);
        return panelEntity;
    }

    private Entity createContentlessEntity() {
        Entity contentlessEntity =
                runtime.createEntity(new Pose(), "testContentLess", runtime.getActivitySpace());
        contentLessEntityNode = ((AndroidXrEntity) contentlessEntity).getNode();
        entityManager.setEntityForNode(contentLessEntityNode, contentlessEntity);
        return contentlessEntity;
    }

    private AnchorEntity createAnchorEntity() {
        AnchorEntityImpl anchorEntity =
                AnchorEntityImpl.createSemanticAnchor(
                        anchorEntityNode,
                        new Dimensions(1f, 1f, 1f),
                        PlaneType.VERTICAL,
                        PlaneSemantic.WALL,
                        null,
                        activitySpace,
                        activitySpaceRoot,
                        fakeExtensions,
                        entityManager,
                        executor,
                        perceptionLibrary);
        entityManager.setEntityForNode(anchorEntityNode, anchorEntity);
        return anchorEntity;
    }

    private ActivityPanelEntity createActivityPanelEntity() {
        return runtime.createActivityPanelEntity(
                new Pose(),
                new PixelDimensions(VGA_WIDTH, VGA_HEIGHT),
                "test",
                activity,
                activitySpace);
    }
}
