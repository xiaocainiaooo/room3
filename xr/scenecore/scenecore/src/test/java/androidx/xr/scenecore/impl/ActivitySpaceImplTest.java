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
import static androidx.xr.runtime.testing.math.MathAssertions.assertVector3;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFuture;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import androidx.xr.extensions.space.Bounds;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivitySpace;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeSpatialState;

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
public final class ActivitySpaceImplTest extends SystemSpaceEntityImplTest {
    // TODO(b/329902726): Move this boilerplate for creating a TestJxrPlatformAdapter into a test
    // util
    private final ActivityController<Activity> activityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity activity = activityController.create().start().get();
    private final FakeScheduledExecutorService fakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary perceptionLibrary = Mockito.mock(PerceptionLibrary.class);
    private final SplitEngineSubspaceManager splitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    private final ImpSplitEngineRenderer splitEngineRenderer =
            Mockito.mock(ImpSplitEngineRenderer.class);

    private FakeXrExtensions fakeExtensions;
    private FakeImpressApi fakeImpressApi;
    private JxrPlatformAdapter testRuntime;
    private ActivitySpace activitySpace;

    @Before
    public void setUp() {
        fakeExtensions = new FakeXrExtensions();
        fakeImpressApi = new FakeImpressApi();
        when(perceptionLibrary.initSession(eq(activity), anyInt(), eq(fakeExecutor)))
                .thenReturn(immediateFuture(Mockito.mock(Session.class)));

        testRuntime =
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

        activitySpace = testRuntime.getActivitySpace();

        // This is slightly hacky. We're grabbing the singleton instance of the ActivitySpaceImpl
        // that
        // was created by the RuntimeImpl. Ideally we'd have an interface to inject the
        // ActivitySpace
        // for testing.  For now this is fine since there isn't an interface difference (yet).
        assertThat(activitySpace).isInstanceOf(ActivitySpaceImpl.class);
        assertThat(activitySpace).isNotNull();
    }

    @Override
    protected SystemSpaceEntityImpl getSystemSpaceEntityImpl() {
        return (SystemSpaceEntityImpl) activitySpace;
    }

    @Override
    protected FakeScheduledExecutorService getDefaultFakeExecutor() {
        return fakeExecutor;
    }

    @Override
    protected AndroidXrEntity createChildAndroidXrEntity() {
        return (AndroidXrEntity) testRuntime.createEntity(new Pose(), "child", activitySpace);
    }

    @Override
    protected ActivitySpaceImpl getActivitySpaceEntity() {
        return (ActivitySpaceImpl) activitySpace;
    }

    @Test
    public void getBounds_returnsBounds() {
        assertThat(activitySpace.getBounds().width).isPositiveInfinity();
        assertThat(activitySpace.getBounds().height).isPositiveInfinity();
        assertThat(activitySpace.getBounds().depth).isPositiveInfinity();

        FakeSpatialState spatialState = new FakeSpatialState();
        spatialState.setBounds(new Bounds(100.0f, 200.0f, 300.0f));
        fakeExtensions.sendSpatialState(spatialState);

        assertThat(activitySpace.getBounds().width).isEqualTo(100f);
        assertThat(activitySpace.getBounds().height).isEqualTo(200f);
        assertThat(activitySpace.getBounds().depth).isEqualTo(300f);
    }

    @Test
    public void addBoundsChangedListener_happyPath() {
        JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener listener =
                Mockito.mock(JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener.class);

        FakeSpatialState spatialState = new FakeSpatialState();
        spatialState.setBounds(new Bounds(100.0f, 200.0f, 300.0f));
        activitySpace.addOnBoundsChangedListener(listener);
        fakeExtensions.sendSpatialState(spatialState);

        verify(listener).onBoundsChanged(Mockito.refEq(new Dimensions(100.0f, 200.0f, 300.0f)));
    }

    @Test
    public void removeBoundsChangedListener_happyPath() {
        JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener listener =
                Mockito.mock(JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener.class);

        activitySpace.addOnBoundsChangedListener(listener);
        activitySpace.removeOnBoundsChangedListener(listener);
        FakeSpatialState spatialState = new FakeSpatialState();
        spatialState.setBounds(new Bounds(100.0f, 200.0f, 300.0f));
        fakeExtensions.sendSpatialState(spatialState);

        verify(listener, Mockito.never()).onBoundsChanged(Mockito.any());
    }

    @Test
    public void getPoseInActivitySpace_returnsIdentity() {
        ActivitySpaceImpl activitySpaceImpl = (ActivitySpaceImpl) activitySpace;

        assertPose(activitySpaceImpl.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getActivitySpaceScale_returnsUnitScale() {
        ActivitySpaceImpl activitySpaceImpl = (ActivitySpaceImpl) activitySpace;
        activitySpaceImpl.setOpenXrReferenceSpacePose(Matrix4.fromScale(5f));
        assertVector3(activitySpaceImpl.getActivitySpaceScale(), new Vector3(1f, 1f, 1f));
    }

    @Test
    public void setScale_doesNothing() throws Exception {
        Vector3 scale = new Vector3(1, 1, 9999);
        activitySpace.setScale(scale);

        // The returned scale(s) here should be the identity scale despite the setScale call.
        assertThat(activitySpace.getScale().getX()).isWithin(1e-5f).of(1.0f);
        assertThat(activitySpace.getScale().getY()).isWithin(1e-5f).of(1.0f);
        assertThat(activitySpace.getScale().getZ()).isWithin(1e-5f).of(1.0f);

        // Note that there's no exception thrown.
    }
}
