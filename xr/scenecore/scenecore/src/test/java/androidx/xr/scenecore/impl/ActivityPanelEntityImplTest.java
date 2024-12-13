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
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;

import androidx.xr.runtime.math.Pose;
import androidx.xr.scenecore.JxrPlatformAdapter;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivityPanelEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.PixelDimensions;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeActivityPanel;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class ActivityPanelEntityImplTest {
    private final FakeXrExtensions fakeExtensions = new FakeXrExtensions();
    private final FakeImpressApi fakeImpressApi = new FakeImpressApi();
    private final ActivityController<Activity> activityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity hostActivity = activityController.create().start().get();
    private final PixelDimensions windowBoundsPx = new PixelDimensions(640, 480);
    private final FakeScheduledExecutorService fakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary perceptionLibrary = Mockito.mock(PerceptionLibrary.class);
    private final SplitEngineSubspaceManager splitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    private final ImpSplitEngineRenderer splitEngineRenderer =
            Mockito.mock(ImpSplitEngineRenderer.class);

    private ActivityPanelEntity createActivityPanelEntity() {
        when(perceptionLibrary.initSession(eq(hostActivity), anyInt(), eq(fakeExecutor)))
                .thenReturn(immediateFuture(Mockito.mock(Session.class)));

        JxrPlatformAdapter fakeRuntime =
                JxrPlatformAdapterAxr.create(
                        hostActivity,
                        fakeExecutor,
                        fakeExtensions,
                        fakeImpressApi,
                        new EntityManager(),
                        perceptionLibrary,
                        splitEngineSubspaceManager,
                        splitEngineRenderer,
                        /* useSplitEngine= */ false);
        Pose pose = new Pose();

        return fakeRuntime.createActivityPanelEntity(
                pose, windowBoundsPx, "test", hostActivity, fakeRuntime.getActivitySpaceRootImpl());
    }

    @Test
    public void createActivityPanelEntity_returnsActivityPanelEntity() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();

        assertThat(activityPanelEntity).isNotNull();
    }

    @Test
    public void activityPanelEntityLaunchActivity_callsActivityPanel() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        Intent launchIntent = activityController.getIntent();
        activityPanelEntity.launchActivity(launchIntent, null);

        FakeActivityPanel fakePanel = fakeExtensions.getActivityPanelForHost(hostActivity);

        assertThat(fakePanel.getLaunchIntent()).isEqualTo(launchIntent);
        assertThat(fakePanel.getBundle()).isNull();
        assertThat(fakePanel.getBounds())
                .isEqualTo(new Rect(0, 0, windowBoundsPx.width, windowBoundsPx.height));
    }

    @Test
    public void activityPanelEntityMoveActivity_callActivityPanel() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        activityPanelEntity.moveActivity(hostActivity);

        FakeActivityPanel fakePanel = fakeExtensions.getActivityPanelForHost(hostActivity);

        assertThat(fakePanel.getActivity()).isEqualTo(hostActivity);

        assertThat(fakePanel.getBounds())
                .isEqualTo(new Rect(0, 0, windowBoundsPx.width, windowBoundsPx.height));
    }

    @Test
    public void activityPanelEntitySetSize_callsSetPixelDimensions() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        Dimensions dimensions = new Dimensions(400f, 300f, 0f);
        activityPanelEntity.setSize(dimensions);

        FakeActivityPanel fakePanel = fakeExtensions.getActivityPanelForHost(hostActivity);

        assertThat(fakePanel.getBounds())
                .isEqualTo(new Rect(0, 0, (int) dimensions.width, (int) dimensions.height));

        // SetSize redirects to setPixelDimensions, so we check the same thing here.
        PixelDimensions viewDimensions = activityPanelEntity.getPixelDimensions();
        assertThat(viewDimensions.width).isEqualTo((int) dimensions.width);
        assertThat(viewDimensions.height).isEqualTo((int) dimensions.height);
    }

    @Test
    public void activityPanelEntitySetPixelDimensions_callActivityPanel() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        PixelDimensions dimensions = new PixelDimensions(400, 300);
        activityPanelEntity.setPixelDimensions(dimensions);

        FakeActivityPanel fakePanel = fakeExtensions.getActivityPanelForHost(hostActivity);

        assertThat(fakePanel.getBounds())
                .isEqualTo(new Rect(0, 0, dimensions.width, dimensions.height));

        PixelDimensions viewDimensions = activityPanelEntity.getPixelDimensions();
        assertThat(viewDimensions.width).isEqualTo(dimensions.width);
        assertThat(viewDimensions.height).isEqualTo(dimensions.height);
    }

    @Test
    public void activityPanelEntityDispose_callsActivityPanelDelete() {
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();
        activityPanelEntity.dispose();

        FakeActivityPanel fakePanel = fakeExtensions.getActivityPanelForHost(hostActivity);

        assertThat(fakePanel.isDeleted()).isTrue();
    }
}
