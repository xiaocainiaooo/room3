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

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.os.IBinder;
import android.os.SystemClock;

import androidx.xr.extensions.node.Node;
import androidx.xr.runtime.internal.Anchor.PersistenceState;
import androidx.xr.runtime.internal.TrackingState;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.runtime.openxr.ExportableAnchor;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity.OnStateChangedListener;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity.PersistState;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity.PersistStateChangeListener;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity.State;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneSemantic;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneType;
import androidx.xr.scenecore.impl.perception.Anchor;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Plane;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeGltfModelToken;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeNode;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.time.Duration;
import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
public final class AnchorEntityImplTest extends SystemSpaceEntityImplTest {
    private static class FakeExportableAnchor implements ExportableAnchor {
        private final long nativePointer;
        private final IBinder anchorToken;
        private final Pose pose;
        private final TrackingState trackingState;
        private final PersistenceState persistenceState;
        private final UUID uuid;

        public FakeExportableAnchor(
                long nativePointer,
                IBinder anchorToken,
                Pose pose,
                TrackingState trackingState,
                PersistenceState persistenceState,
                UUID uuid) {
            this.nativePointer = nativePointer;
            this.anchorToken = anchorToken;
            this.pose = pose;
            this.trackingState = trackingState;
            this.persistenceState = persistenceState;
            this.uuid = uuid;
        }

        @Override
        public long getNativePointer() {
            return nativePointer;
        }

        @Override
        public IBinder getAnchorToken() {
            return anchorToken;
        }

        @Override
        public Pose getPose() {
            return pose;
        }

        @Override
        public TrackingState getTrackingState() {
            return trackingState;
        }

        @Override
        public PersistenceState getPersistenceState() {
            return persistenceState;
        }

        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public void detach() {}

        @Override
        public void persist() {}
    }

    private static final Dimensions ANCHOR_DIMENSIONS = new Dimensions(2f, 5f, 0f);
    private static final Plane.Type PLANE_TYPE = Plane.Type.VERTICAL;
    private static final Plane.Label PLANE_LABEL = Plane.Label.WALL;
    private static final long NATIVE_POINTER = 1234567890L;
    private final AndroidXrEntity activitySpaceRoot = Mockito.mock(AndroidXrEntity.class);
    private final FakeXrExtensions fakeExtensions = new FakeXrExtensions();
    private final PerceptionLibrary perceptionLibrary = Mockito.mock(PerceptionLibrary.class);
    private final Session session = Mockito.mock(Session.class);
    private final Plane plane = mock(Plane.class);
    private final Anchor anchor = Mockito.mock(Anchor.class);
    private final OnStateChangedListener anchorStateListener =
            Mockito.mock(OnStateChangedListener.class);
    private final IBinder sharedAnchorToken = Mockito.mock(IBinder.class);
    private final FakeScheduledExecutorService executor = new FakeScheduledExecutorService();
    private final EntityManager entityManager = new EntityManager();
    private final PersistStateChangeListener persistStateChangeListener =
            Mockito.mock(PersistStateChangeListener.class);
    private final androidx.xr.scenecore.impl.perception.Pose perceptionIdentityPose =
            androidx.xr.scenecore.impl.perception.Pose.identity();
    private long currentTimeMillis = 1000000000L;
    private ActivitySpaceImpl activitySpace;

    @Before
    public void doBeforeEachTest() {
        Node taskNode = fakeExtensions.createNode();
        this.activitySpace =
                new ActivitySpaceImpl(
                        taskNode,
                        fakeExtensions,
                        entityManager,
                        () -> fakeExtensions.fakeSpatialState,
                        executor);
        SystemClock.setCurrentTimeMillis(currentTimeMillis);

        // By default, set the activity space to the root of the underlying OpenXR reference space.
        this.activitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
    }

    /**
     * Returns the anchor entity impl. Used in the base SystemSpaceEntityImplTest to ensure that the
     * anchor entity complies with all the expected behaviors of a system space entity.
     */
    @Override
    protected SystemSpaceEntityImpl getSystemSpaceEntityImpl() {
        return createSemanticAnchorEntity();
    }

    @Override
    protected FakeScheduledExecutorService getDefaultFakeExecutor() {
        return executor;
    }

    @Override
    protected AndroidXrEntity createChildAndroidXrEntity() {
        return createGltfEntity();
    }

    @Override
    protected ActivitySpaceImpl getActivitySpaceEntity() {
        return this.activitySpace;
    }

    // Advances the clock and the executor. The fake executor is not based on the clock because we
    // are
    // using the SystemClock but they can be advanced together.
    void advanceClock(Duration duration) {
        currentTimeMillis += duration.toMillis();
        SystemClock.setCurrentTimeMillis(currentTimeMillis);
        executor.simulateSleepExecutingAllTasks(duration);
    }

    /** Creates an AnchorEntityImpl instance. */
    private AnchorEntityImpl createSemanticAnchorEntity() {
        return createAnchorEntityWithTimeout(null);
    }

    /** Creates an AnchorEntityImpl instance with a timeout. */
    private AnchorEntityImpl createAnchorEntityWithTimeout(Duration anchorSearchTimeout) {
        Node node = fakeExtensions.createNode();
        return AnchorEntityImpl.createSemanticAnchor(
                node,
                ANCHOR_DIMENSIONS,
                PlaneType.VERTICAL,
                PlaneSemantic.WALL,
                anchorSearchTimeout,
                activitySpace,
                activitySpaceRoot,
                fakeExtensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    /**
     * Creates an AnchorEntityImpl instance that will search for a persisted anchor within
     * `anchorSearchTimeout`.
     */
    private AnchorEntityImpl createPersistedAnchorEntityWithTimeout(
            UUID uuid, Duration anchorSearchTimeout) {
        Node node = fakeExtensions.createNode();
        return AnchorEntityImpl.createPersistedAnchor(
                node,
                uuid,
                anchorSearchTimeout,
                activitySpace,
                activitySpaceRoot,
                fakeExtensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    /** Creates an AnchorEntityImpl instance and initializes it with a persisted anchor. */
    private AnchorEntityImpl createInitializedPersistedAnchorEntity(Anchor anchor, UUID uuid) {
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.createAnchorFromUuid(uuid)).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        Node node = fakeExtensions.createNode();
        return AnchorEntityImpl.createPersistedAnchor(
                node,
                uuid,
                /* anchorSearchTimeout= */ null,
                activitySpace,
                activitySpaceRoot,
                fakeExtensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    /**
     * Creates an AnchorEntityImpl and initializes it with an anchor from the perception library.
     */
    private AnchorEntityImpl createAndInitAnchorEntity() {
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                perceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(plane.createAnchor(eq(perceptionIdentityPose), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        return createSemanticAnchorEntity();
    }

    private AnchorEntityImpl createInitAndPersistAnchorEntity() {
        when(anchor.persist()).thenReturn(UUID.randomUUID());
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        anchorEntity.registerPersistStateChangeListener(persistStateChangeListener);
        UUID unused = anchorEntity.persist();
        return anchorEntity;
    }

    private AnchorEntityImpl createAnchorEntityFromPlane() {
        when(anchor.persist()).thenReturn(UUID.randomUUID());

        Node node = fakeExtensions.createNode();
        return AnchorEntityImpl.createAnchorFromPlane(
                node,
                plane,
                new Pose(),
                MILLISECONDS.toNanos(currentTimeMillis),
                activitySpace,
                activitySpaceRoot,
                fakeExtensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    private AnchorEntityImpl createAnchorEntityFromPerceptionAnchor(
            androidx.xr.arcore.Anchor perceptionAnchor) {
        Node node = fakeExtensions.createNode();

        return AnchorEntityImpl.createAnchorFromPerceptionAnchor(
                node,
                perceptionAnchor,
                activitySpace,
                activitySpaceRoot,
                fakeExtensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    /** Creates a generic glTF entity. */
    private GltfEntityImpl createGltfEntity() {
        FakeGltfModelToken modelToken = new FakeGltfModelToken("model");
        GltfModelResourceImpl model = new GltfModelResourceImpl(modelToken);
        return new GltfEntityImpl(model, activitySpace, fakeExtensions, entityManager, executor);
    }

    @Test
    public void createAnchorEntityWithPersistedAnchor_returnsAnchored() throws Exception {
        AnchorEntityImpl anchorEntity =
                createInitializedPersistedAnchorEntity(anchor, UUID.randomUUID());
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(((FakeNode) anchorEntity.getNode()).getAnchorId()).isEqualTo(sharedAnchorToken);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);
    }

    @Test
    public void createAnchorEntityWithPersistedAnchor_persistAnchor_returnsUuid() throws Exception {
        UUID uuid = UUID.randomUUID();
        AnchorEntityImpl anchorEntity = createInitializedPersistedAnchorEntity(anchor, uuid);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(((FakeNode) anchorEntity.getNode()).getAnchorId()).isEqualTo(sharedAnchorToken);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);

        UUID returnedUuid = anchorEntity.persist();
        assertThat(returnedUuid).isEqualTo(uuid);
    }

    @Test
    public void createPersistedAnchorEntity_sessionNotReady_keepUnanchored() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(null);
        AnchorEntityImpl anchorEntity =
                createPersistedAnchorEntityWithTimeout(
                        UUID.randomUUID(), /* anchorSearchTimeout= */ null);

        // if the session isn't ready, we should retry later and search for the anchor.
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
    }

    @Test
    public void createPersistedAnchorEntity_persistedAnchorNotFound_keepUnanchored()
            throws Exception {
        UUID uuid = UUID.randomUUID();
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.createAnchorFromUuid(uuid)).thenReturn(null);
        AnchorEntityImpl anchorEntity =
                createPersistedAnchorEntityWithTimeout(uuid, /* anchorSearchTimeout= */ null);

        // If the session is ready and we can't find it, the perception stack might be warming up,
        // so
        // we'll retry later.
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
    }

    @Test
    public void createPersistedAnchorEntity_persistedAnchorHasNoToken_keepUnanchored()
            throws Exception {
        UUID uuid = UUID.randomUUID();
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.createAnchorFromUuid(uuid)).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(null);
        AnchorEntityImpl anchorEntity =
                createPersistedAnchorEntityWithTimeout(uuid, /* anchorSearchTimeout= */ null);

        // If the anchor is ready but its token isn't available, we'll retry later.
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
    }

    @Test
    public void createPersistedAnchorEntity_delayedSession_callsCallback() throws Exception {
        // This will return an error on the first attempt so will need to be called twice.
        when(perceptionLibrary.getSession()).thenReturn(null).thenReturn(session);
        UUID uuid = UUID.randomUUID();
        when(session.createAnchorFromUuid(uuid)).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        AnchorEntityImpl anchorEntity =
                createPersistedAnchorEntityWithTimeout(uuid, /* anchorSearchTimeout= */ null);
        anchorEntity.setOnStateChangedListener(anchorStateListener);

        verify(anchorStateListener, never()).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // The anchor starts as unanchored. Advance the executor to try again successfully and get a
        // callback for the anchor to be anchored.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(anchorStateListener).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);
    }

    @Test
    public void createPersistedAnchorEntity_delayedAnchor_callsCallback() throws Exception {
        // This will return an error on the first attempt so will need to be called twice.
        when(perceptionLibrary.getSession()).thenReturn(session);
        UUID uuid = UUID.randomUUID();
        when(session.createAnchorFromUuid(uuid)).thenReturn(anchor).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(null).thenReturn(sharedAnchorToken);

        AnchorEntityImpl anchorEntity =
                createPersistedAnchorEntityWithTimeout(uuid, /* anchorSearchTimeout= */ null);
        anchorEntity.setOnStateChangedListener(anchorStateListener);

        verify(anchorStateListener, never()).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // The anchor starts as unanchored. Advance the executor to try again successfully and get a
        // callback for the anchor to be anchored.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(anchorStateListener).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);
    }

    @Test
    public void createPersistedAnchorEntity_delayedSession_timeout_noCallback() throws Exception {
        // This will return an error on the first attempt so will need to be called twice.
        when(perceptionLibrary.getSession()).thenReturn(null).thenReturn(session);
        UUID uuid = UUID.randomUUID();
        when(session.createAnchorFromUuid(uuid)).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        AnchorEntityImpl anchorEntity =
                createPersistedAnchorEntityWithTimeout(
                        uuid, AnchorEntityImpl.ANCHOR_SEARCH_DELAY.dividedBy(2));
        anchorEntity.setOnStateChangedListener(anchorStateListener);

        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);

        verify(anchorStateListener, never()).onStateChanged(State.ANCHORED);
        verify(anchorStateListener).onStateChanged(State.TIMED_OUT);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.TIMED_OUT);
    }

    @Test
    public void createAnchorEntity_defaultUnanchored() throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
    }

    @Test
    public void createAndInitAnchor_returnsAnchored() throws Exception {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(((FakeNode) anchorEntity.getNode()).getAnchorId()).isEqualTo(sharedAnchorToken);
    }

    @Test
    public void createAndInitAnchor_noPlanes_remainsUnanchored() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of());

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();

        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
    }

    @Test
    public void createAndInitAnchor_noViablePlane_remainsUnanchored() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                perceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width - 1,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();

        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
    }

    @Test
    public void createAndInitAnchor_delayedSession_callsCallback() throws Exception {
        // This will return an error on the first attempt so will need to be called twice.
        when(perceptionLibrary.getSession()).thenReturn(null).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                perceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(plane.createAnchor(eq(perceptionIdentityPose), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        anchorEntity.setOnStateChangedListener(anchorStateListener);

        verify(anchorStateListener, never()).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // The anchor starts as unanchored. Advance the executor to try again successfully and get a
        // callback for the anchor to be anchored.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(anchorStateListener).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
    }

    @Test
    public void createAndInitAnchor_delayedAnchor_callsCallback() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(session);
        // This will return no planes on the first attempt so will need to be called twice.
        when(session.getAllPlanes())
                .thenReturn(ImmutableList.of())
                .thenReturn(ImmutableList.of(plane));
        when(plane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                perceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(plane.createAnchor(eq(perceptionIdentityPose), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        anchorEntity.setOnStateChangedListener(anchorStateListener);

        verify(anchorStateListener, never()).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // The anchor starts as unanchored. Advance the executor to try again successfully and get a
        // callback for the anchor to be anchored.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(anchorStateListener).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
    }

    @Test
    public void createAndInitAnchor_delayedSessionAndAnchor_callsCallback() throws Exception {
        // This will return an error on the first attempt then it will find no planes on the
        // second attempt. So it will need to be called three times.
        when(perceptionLibrary.getSession())
                .thenReturn(null)
                .thenReturn(session)
                .thenReturn(session);
        when(session.getAllPlanes())
                .thenReturn(ImmutableList.of())
                .thenReturn(ImmutableList.of(plane));
        when(plane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                perceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(plane.createAnchor(eq(perceptionIdentityPose), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        anchorEntity.setOnStateChangedListener(anchorStateListener);

        verify(anchorStateListener, never()).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // Advance the executor to try again with a working session but an error on the anchor.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // Advance the executor attempt the anchor again successfully and get a callback for the
        // anchor
        // to be anchored.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(anchorStateListener).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
    }

    @Test
    public void createAndInitAnchor_noToken_returnsUnanchored() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                perceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(plane.createAnchor(eq(perceptionIdentityPose), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(null);

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();

        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
    }

    @Test
    public void createAndInitAnchor_withinTimeout_success() throws Exception {
        // The anchor creation will return an error so that it is called again on the executor. The
        // timeout will happen after the second attempt so it will still succeed.
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes())
                .thenReturn(ImmutableList.of())
                .thenReturn(ImmutableList.of(plane));
        when(plane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                perceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(plane.createAnchor(eq(perceptionIdentityPose), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        // Create an anchor entity with a timeout of 1ms above the normal search delay.
        Duration timeout = AnchorEntityImpl.ANCHOR_SEARCH_DELAY.plusMillis(1);
        AnchorEntityImpl anchorEntity = createAnchorEntityWithTimeout(timeout);
        anchorEntity.setOnStateChangedListener(anchorStateListener);

        verify(anchorStateListener, never()).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // Advance the executor to try again it should now be anchored.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(anchorStateListener).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);

        // Advance the clock past the timeout and verify that nothing changed.
        advanceClock(timeout.minus(AnchorEntityImpl.ANCHOR_SEARCH_DELAY));
        verify(anchorStateListener, never()).onStateChanged(State.TIMED_OUT);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
    }

    @Test
    public void createAndInitAnchor_passedTimeout_error() throws Exception {
        // The anchor creation will return an error so that it is called again on the executor. The
        // timeout will happen before the next call.
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of());
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        // Create an anchor entity with a timeout of 1ms below the normal search delay.
        Duration timeout = AnchorEntityImpl.ANCHOR_SEARCH_DELAY.minusMillis(1);
        AnchorEntityImpl anchorEntity = createAnchorEntityWithTimeout(timeout);
        anchorEntity.setOnStateChangedListener(anchorStateListener);

        verify(anchorStateListener, never()).onStateChanged(State.ERROR);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // Advance the executor to try again it should now be timed out.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        assertThat(anchorEntity.getState()).isEqualTo(State.TIMED_OUT);

        // Advance the clock again and verify that nothing changed.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        assertThat(anchorEntity.getState()).isEqualTo(State.TIMED_OUT);
    }

    @Test
    public void createAndInitAnchor_zeroTimeout_keepsSearching() throws Exception {
        // The anchor creation will return an error so that it is called again on the executor. The
        // timeout will happen after the second attempt so it will still succeed.
        int anchorAttempts = 100;
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of());
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        // Create an anchor entity with a zero duration it should be search for indefinitely..
        AnchorEntityImpl anchorEntity = createAnchorEntityWithTimeout(Duration.ZERO);
        anchorEntity.setOnStateChangedListener(anchorStateListener);

        for (int i = 0; i < anchorAttempts - 1; i++) {
            advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
            verify(anchorStateListener, never()).onStateChanged(any());
            assertThat(anchorEntity).isNotNull();
            assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
        }
    }

    @Test
    public void anchorEntityAddChildren_addsChildren() throws Exception {
        GltfEntityImpl childEntity1 = createGltfEntity();
        GltfEntityImpl childEntity2 = createGltfEntity();
        AnchorEntityImpl parentEntity = createSemanticAnchorEntity();

        parentEntity.addChild(childEntity1);

        assertThat(parentEntity.getChildren()).containsExactly(childEntity1);

        parentEntity.addChildren(ImmutableList.of(childEntity2));

        assertThat(childEntity1.getParent()).isEqualTo(parentEntity);
        assertThat(childEntity2.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getChildren()).containsExactly(childEntity1, childEntity2);

        FakeNode parentNode = (FakeNode) parentEntity.getNode();
        FakeNode childNode1 = (FakeNode) childEntity1.getNode();
        FakeNode childNode2 = (FakeNode) childEntity2.getNode();

        assertThat(childNode1.getParent()).isEqualTo(parentNode);
        assertThat(childNode2.getParent()).isEqualTo(parentNode);
    }

    @Test
    public void anchorEntitySetPose_throwsException() throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        Pose pose = new Pose();
        assertThrows(UnsupportedOperationException.class, () -> anchorEntity.setPose(pose));
    }

    @Test
    public void anchorEntitySetScale_throwsException() throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        Vector3 scale = new Vector3(1, 1, 1);
        assertThrows(UnsupportedOperationException.class, () -> anchorEntity.setScale(scale));
    }

    @Test
    public void anchorEntityGetScale_returnsIdentityScale() throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertVector3(anchorEntity.getScale(), new Vector3(1f, 1f, 1f));
    }

    @Test
    public void anchorEntityGetWorldSpaceScale_returnsIdentityScale() throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertVector3(anchorEntity.getWorldSpaceScale(), new Vector3(1f, 1f, 1f));
    }

    @Test
    public void anchorEntityGetActivitySpaceScale_returnsInverseOfActivitySpace() throws Exception {
        float activitySpaceScale = 5f;
        this.activitySpace.setOpenXrReferenceSpacePose(Matrix4.fromScale(activitySpaceScale));
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertVector3(
                anchorEntity.getActivitySpaceScale(),
                new Vector3(1f, 1f, 1f).div(activitySpaceScale));
    }

    @Test
    public void getPoseInActivitySpace_unanchored_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        activitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void
            getPoseInActivitySpace_noActivitySpaceOpenXrReferenceSpacePose_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        activitySpace.openXrReferenceSpacePose = null;
        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));
        assertPose(anchorEntity.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_noAnchorOpenXrReferenceSpacePose_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        activitySpace.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));
        // anchorEntity.setOpenXrReferenceSpacePose(..) is not called to set the underlying pose.

        assertPose(anchorEntity.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_whenAtSamePose_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1).toNormalized());
        activitySpace.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));
        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_returnsDifferencePose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1).toNormalized());
        activitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getPoseInActivitySpace(), pose);
    }

    @Test
    public void getPoseInActivitySpace_withNoActivitySpace_throwsException() throws Exception {
        Node node = fakeExtensions.createNode();
        AnchorEntityImpl anchorEntity =
                AnchorEntityImpl.createSemanticAnchor(
                        node,
                        /* dimensions= */ null,
                        /* planeType= */ null,
                        /* planeSemantic= */ null,
                        /* anchorSearchTimeout= */ null,
                        /* activitySpace= */ null,
                        activitySpaceRoot,
                        fakeExtensions,
                        entityManager,
                        executor,
                        perceptionLibrary);

        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThrows(IllegalStateException.class, anchorEntity::getPoseInActivitySpace);
    }

    @Test
    public void getActivitySpacePose_whenAtSamePose_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1).toNormalized());
        when(activitySpaceRoot.getPoseInActivitySpace()).thenReturn(pose);

        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getActivitySpacePose(), new Pose());
    }

    @Test
    public void getActivitySpacePose_returnsDifferencePose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1).toNormalized());
        when(activitySpaceRoot.getPoseInActivitySpace()).thenReturn(new Pose());

        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getActivitySpacePose(), pose);
    }

    @Test
    public void getActivitySpacePose_withNonAndroidXrActivitySpaceRoot_throwsException()
            throws Exception {
        Node node = fakeExtensions.createNode();
        AnchorEntityImpl anchorEntity =
                AnchorEntityImpl.createSemanticAnchor(
                        node,
                        /* dimensions= */ null,
                        /* planeType= */ null,
                        /* planeSemantic= */ null,
                        /* anchorSearchTimeout= */ null,
                        activitySpace,
                        /* activitySpaceRoot= */ null,
                        fakeExtensions,
                        entityManager,
                        executor,
                        perceptionLibrary);

        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThrows(IllegalStateException.class, anchorEntity::getActivitySpacePose);
    }

    @Test
    public void transformPoseTo_withActivitySpace_returnsTransformedPose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        activitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        Pose anchorOffset =
                new Pose(
                        new Vector3(10f, 0f, 0f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f)));
        Pose transformedPose = anchorEntity.transformPoseTo(anchorOffset, activitySpace);

        assertPose(
                transformedPose,
                new Pose(
                        new Vector3(11f, 2f, 3f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f))));
    }

    @Test
    public void transformPoseTo_fromActivitySpaceChild_returnsAnchorSpacePose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        GltfEntityImpl childEntity1 = createGltfEntity();
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        Pose childPose = new Pose(new Vector3(-1f, -2f, -3f), Quaternion.Identity);

        activitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
        activitySpace.addChild(childEntity1);
        childEntity1.setPose(childPose);
        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(
                activitySpace.transformPoseTo(new Pose(), anchorEntity),
                new Pose(new Vector3(-1f, -2f, -3f), Quaternion.Identity));

        Pose transformedPose = childEntity1.transformPoseTo(new Pose(), anchorEntity);
        assertPose(transformedPose, new Pose(new Vector3(-2f, -4f, -6f), Quaternion.Identity));
    }

    @Test
    public void anchorEntity_setsParentAfterAnchoring() throws Exception {
        // This will return an error on the first attempt so will need to be called twice.
        when(perceptionLibrary.getSession()).thenReturn(null).thenReturn(session);

        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                perceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(plane.createAnchor(eq(perceptionIdentityPose), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        FakeNode anchorNode = (FakeNode) anchorEntity.getNode();
        FakeNode rootNode = (FakeNode) activitySpace.getNode();
        assertThat(anchorNode.getParent()).isNull();

        // The anchor starts as unanchored. Advance the executor to wait for it to become anchored
        // and
        // verify that the parent is the root node.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(anchorNode.getParent()).isEqualTo(rootNode);
    }

    @Test
    public void disposeAnchor_detachesAnchor() throws Exception {
        when(anchor.detach()).thenReturn(true);
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        anchorEntity.setOnStateChangedListener(anchorStateListener);
        verify(anchorStateListener, never()).onStateChanged(State.ERROR);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);

        // Verify the parent of the anchor is the root node (ActivitySpace) before disposing it.
        FakeNode anchorNode = (FakeNode) anchorEntity.getNode();
        FakeNode rootNode = (FakeNode) activitySpace.getNode();
        assertThat(anchorNode.getParent()).isEqualTo(rootNode);
        assertThat(anchorNode.getAnchorId()).isEqualTo(sharedAnchorToken);

        // Dispose the entity and verify that the state was updated.
        anchorEntity.dispose();

        verify(anchorStateListener).onStateChanged(State.ERROR);
        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThat(anchorNode.getParent()).isNull();
        assertThat(anchorNode.getAnchorId()).isNull();
    }

    @Test
    public void disposeAnchorUnanchered_stopsSearching() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(null);

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        anchorEntity.setOnStateChangedListener(anchorStateListener);

        verify(perceptionLibrary).getSession();
        verify(anchorStateListener, never()).onStateChanged(State.ERROR);
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // Dispose the anchor entity before it was anchored.
        anchorEntity.dispose();

        // verify(anchorStateListener).onStateChanged(State.ERROR);
        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);

        // Advance the executor attempt to show that it will not call into the perception library
        // again
        // once disposed.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(perceptionLibrary).getSession();
    }

    @Test
    public void disposeAnchorTwice_callsCalbackOnce() throws Exception {
        when(anchor.detach()).thenReturn(true);
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        anchorEntity.setOnStateChangedListener(anchorStateListener);
        verify(anchorStateListener, never()).onStateChanged(State.ERROR);

        // Dispose the entity and verify that the state was updated.
        anchorEntity.dispose();

        verify(anchorStateListener).onStateChanged(State.ERROR);

        // Dispose anchor again and verify onStateChanged was called only once.
        anchorEntity.dispose();

        verify(anchorStateListener).onStateChanged(State.ERROR);
    }

    @Test
    public void createAnchorEntity_defaultPersistState_returnsPersistNotRequested()
            throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_NOT_REQUESTED);
    }

    @Test
    public void persistAnchor_notAnchored_returnsNull() throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.persist()).isNull();
    }

    @Test
    public void persistAnchor_returnsUuid() throws Exception {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        when(anchor.persist()).thenReturn(UUID.randomUUID());
        anchorEntity.registerPersistStateChangeListener(persistStateChangeListener);
        assertThat(anchorEntity.persist()).isNotNull();
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_PENDING);
        verify(persistStateChangeListener).onPersistStateChanged(PersistState.PERSIST_PENDING);
    }

    @Test
    public void persistAnchor_returnsNull() throws Exception {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        when(anchor.persist()).thenReturn(null);
        anchorEntity.registerPersistStateChangeListener(persistStateChangeListener);
        assertThat(anchorEntity.persist()).isNull();
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_NOT_REQUESTED);
        verify(persistStateChangeListener, never()).onPersistStateChanged(any());
    }

    @Test
    public void persistAnchor_secondPersist_returnsSameUuid_updatesPersistStateOnce()
            throws Exception {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        when(anchor.persist()).thenReturn(UUID.randomUUID());
        anchorEntity.registerPersistStateChangeListener(persistStateChangeListener);
        UUID firstUuid = anchorEntity.persist();
        assertThat(firstUuid).isNotNull();
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_PENDING);

        UUID secondUuid = anchorEntity.persist();
        assertThat(firstUuid).isEquivalentAccordingToCompareTo(secondUuid);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_PENDING);
        verify(persistStateChangeListener).onPersistStateChanged(PersistState.PERSIST_PENDING);
    }

    @Test
    public void persistAnchor_updatesPersistStateToPersisted() throws Exception {
        AnchorEntityImpl anchorEntity = createInitAndPersistAnchorEntity();
        when(anchor.getPersistState()).thenReturn(Anchor.PersistState.PERSISTED);

        executor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);
        verify(persistStateChangeListener).onPersistStateChanged(PersistState.PERSISTED);
    }

    @Test
    public void updatePersistState_delayedPersistedState_callsCallback() throws Exception {
        AnchorEntityImpl anchorEntity = createInitAndPersistAnchorEntity();

        when(anchor.getPersistState())
                .thenReturn(Anchor.PersistState.PERSIST_PENDING)
                .thenReturn(Anchor.PersistState.PERSISTED);
        verify(persistStateChangeListener, never()).onPersistStateChanged(PersistState.PERSISTED);

        executor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        verify(persistStateChangeListener, never()).onPersistStateChanged(PersistState.PERSISTED);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_PENDING);

        executor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        verify(persistStateChangeListener).onPersistStateChanged(PersistState.PERSISTED);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);
    }

    @Test
    public void updatePersistState_noCallbackAfterStateBecomesPersisted() throws Exception {
        AnchorEntityImpl anchorEntity = createInitAndPersistAnchorEntity();

        when(anchor.getPersistState()).thenReturn(Anchor.PersistState.PERSISTED);
        executor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);

        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);
        verify(persistStateChangeListener).onPersistStateChanged(PersistState.PERSISTED);
        Mockito.clearInvocations(anchor);
        Mockito.clearInvocations(persistStateChangeListener);

        executor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        verify(anchor, never()).getPersistState();
        verify(persistStateChangeListener, never()).onPersistStateChanged(any());
    }

    @Test
    public void updatePersistState_noQueryForPersistStateAfterDispose() throws Exception {
        AnchorEntityImpl anchorEntity = createInitAndPersistAnchorEntity();
        when(anchor.getPersistState()).thenReturn(Anchor.PersistState.PERSIST_PENDING);
        executor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        verify(anchor).getPersistState();

        executor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        verify(anchor, times(2)).getPersistState();

        Mockito.clearInvocations(anchor);
        anchorEntity.dispose();
        executor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        verify(anchor, never()).getPersistState();
    }

    @Test
    public void createAnchorEntityFromPlane_returnsAnchorEntity() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(plane.createAnchor(any(), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPlane();

        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(((FakeNode) anchorEntity.getNode()).getAnchorId()).isEqualTo(sharedAnchorToken);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_NOT_REQUESTED);
        assertThat(((FakeNode) anchorEntity.getNode()).getParent())
                .isEqualTo(activitySpace.getNode());
    }

    @Test
    public void createAnchorEntityFromPlane_failureToAnchor_hasErrorState() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(session);
        when(plane.createAnchor(any(), any())).thenReturn(null);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPlane();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThat(((FakeNode) anchorEntity.getNode()).getParent()).isEqualTo(null);
    }

    @Test
    public void createAnchorEntityFromPerceptionAnchor_nativePointerMatches() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(session);
        FakeExportableAnchor fakeAnchor =
                new FakeExportableAnchor(
                        NATIVE_POINTER,
                        sharedAnchorToken,
                        new Pose(),
                        TrackingState.Tracking,
                        PersistenceState.NotPersisted,
                        null);
        androidx.xr.arcore.Anchor perceptionAnchor = new androidx.xr.arcore.Anchor(fakeAnchor);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPerceptionAnchor(perceptionAnchor);

        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.nativePointer()).isEqualTo(NATIVE_POINTER);
    }

    @Test
    public void createAnchorEntityFromPerceptionAnchor_returnsAnchorEntity() throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(session);

        FakeExportableAnchor fakeAnchor =
                new FakeExportableAnchor(
                        NATIVE_POINTER,
                        sharedAnchorToken,
                        new Pose(),
                        TrackingState.Tracking,
                        PersistenceState.NotPersisted,
                        null);
        androidx.xr.arcore.Anchor perceptionAnchor = new androidx.xr.arcore.Anchor(fakeAnchor);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPerceptionAnchor(perceptionAnchor);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(((FakeNode) anchorEntity.getNode()).getAnchorId()).isEqualTo(sharedAnchorToken);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_NOT_REQUESTED);
        assertThat(((FakeNode) anchorEntity.getNode()).getParent())
                .isEqualTo(activitySpace.getNode());
    }

    @Test
    public void createAnchorEntityFromPerceptionAnchor_failureToAnchor_hasErrorState()
            throws Exception {
        when(perceptionLibrary.getSession()).thenReturn(session);

        FakeExportableAnchor fakeAnchor =
                new FakeExportableAnchor(
                        NATIVE_POINTER,
                        null,
                        new Pose(),
                        TrackingState.Tracking,
                        PersistenceState.NotPersisted,
                        null);
        androidx.xr.arcore.Anchor perceptionAnchor = new androidx.xr.arcore.Anchor(fakeAnchor);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPerceptionAnchor(perceptionAnchor);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThat(((FakeNode) anchorEntity.getNode()).getParent()).isEqualTo(null);
    }
}
