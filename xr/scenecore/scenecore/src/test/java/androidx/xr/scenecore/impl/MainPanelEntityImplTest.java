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

import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.PixelDimensions;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class MainPanelEntityImplTest {
    private final FakeXrExtensions fakeExtensions = new FakeXrExtensions();
    private final FakeImpressApi fakeImpressApi = new FakeImpressApi();
    private final ActivityController<Activity> activityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity hostActivity = activityController.create().start().get();
    private final FakeScheduledExecutorService fakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary perceptionLibrary = Mockito.mock(PerceptionLibrary.class);
    SplitEngineSubspaceManager splitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    ImpSplitEngineRenderer splitEngineRenderer = Mockito.mock(ImpSplitEngineRenderer.class);
    private JxrPlatformAdapterAxr testRuntime;
    private MainPanelEntityImpl mainPanelEntity;

    @Before
    public void setUp() {
        when(perceptionLibrary.initSession(eq(hostActivity), anyInt(), eq(fakeExecutor)))
                .thenReturn(immediateFuture(Mockito.mock(Session.class)));

        testRuntime =
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

        mainPanelEntity = (MainPanelEntityImpl) testRuntime.getMainPanelEntity();
    }

    @Test
    public void runtimeGetMainPanelEntity_returnsPanelEntityImpl() {
        assertThat(mainPanelEntity).isNotNull();
    }

    @Test
    public void mainPanelEntitySetPixelDimensions_callsExtensions() {
        PixelDimensions kTestPixelDimensions = new PixelDimensions(14, 14);
        mainPanelEntity.setPixelDimensions(kTestPixelDimensions);
        assertThat(fakeExtensions.getMainWindowWidth()).isEqualTo(kTestPixelDimensions.width);
        assertThat(fakeExtensions.getMainWindowHeight()).isEqualTo(kTestPixelDimensions.height);
    }

    @Test
    public void mainPanelEntitySetSize_callsExtensions() {
        // TODO(b/352630025): remove this once setSize is removed.
        // This should have the same effect as setPixelDimensions, except that it has to convert
        // from Dimensions to PixelDimensions, so it casts float to int.
        Dimensions kTestDimensions = new Dimensions(123.0f, 123.0f, 123.0f);
        mainPanelEntity.setSize(kTestDimensions);
        assertThat(fakeExtensions.getMainWindowWidth()).isEqualTo((int) kTestDimensions.width);
        assertThat(fakeExtensions.getMainWindowWidth()).isEqualTo((int) kTestDimensions.height);
    }
}
