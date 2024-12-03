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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.runtime.openxr.ExportableAnchor;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivitySpace;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity.OnStateChangedListener;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneSemantic;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneType;
import androidx.xr.scenecore.impl.perception.Anchor;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Plane;
import androidx.xr.scenecore.impl.perception.Plane.PlaneData;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Implementation of AnchorEntity.
 *
 * <p>This entity creates trackable anchors in space.
 */
@SuppressWarnings("BanSynchronizedMethods")
class AnchorEntityImpl extends SystemSpaceEntityImpl implements AnchorEntity {
    public static final Duration ANCHOR_SEARCH_DELAY = Duration.ofMillis(500);
    public static final Duration PERSIST_STATE_CHECK_DELAY = Duration.ofMillis(500);
    public static final String ANCHOR_NODE_NAME = "AnchorNode";
    private static final String TAG = "AnchorEntityImpl";
    private final ActivitySpaceImpl activitySpace;
    private final AndroidXrEntity activitySpaceRoot;
    private final PerceptionLibrary perceptionLibrary;
    private OnStateChangedListener onStateChangedListener;
    private State state = State.UNANCHORED;
    private PersistState persistState = PersistState.PERSIST_NOT_REQUESTED;
    private Anchor anchor;
    private UUID uuid = null;
    private PersistStateChangeListener persistStateChangeListener;

    private static class AnchorCreationData {

        static final int ANCHOR_CREATION_SEMANTIC = 1;
        static final int ANCHOR_CREATION_PERSISTED = 2;
        static final int ANCHOR_CREATION_PLANE = 3;
        static final int ANCHOR_CREATION_PERCEPTION_ANCHOR = 4;

        @AnchorCreationType int anchorCreationType;

        /** IntDef for Anchor creation types. */
        @IntDef({
            ANCHOR_CREATION_SEMANTIC,
            ANCHOR_CREATION_PERSISTED,
            ANCHOR_CREATION_PLANE,
            ANCHOR_CREATION_PERCEPTION_ANCHOR,
        })
        @Retention(RetentionPolicy.SOURCE)
        private @interface AnchorCreationType {}

        // Anchor that is already created via Perception API.
        androidx.xr.arcore.Anchor perceptionAnchor;

        // Anchor search deadline for semantic and persisted anchors.
        Long anchorSearchDeadline;

        // Fields exclusively for semantic anchors.
        Dimensions dimensions;
        PlaneType planeType;
        PlaneSemantic planeSemantic;

        // Fields exclusively for persisted anchors.
        UUID uuid = null;

        // Fields exclusively for plane anchors.
        Plane plane;
        Pose planeOffsetPose;
        Long planeDataTimeNs;
    }

    static AnchorEntityImpl createSemanticAnchor(
            Node node,
            Dimensions dimensions,
            PlaneType planeType,
            PlaneSemantic planeSemantic,
            Duration anchorSearchTimeout,
            ActivitySpace activitySpace,
            Entity activitySpaceRoot,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor,
            PerceptionLibrary perceptionLibrary) {
        AnchorCreationData anchorCreationData = new AnchorCreationData();
        anchorCreationData.anchorCreationType = AnchorCreationData.ANCHOR_CREATION_SEMANTIC;
        anchorCreationData.dimensions = dimensions;
        anchorCreationData.planeType = planeType;
        anchorCreationData.planeSemantic = planeSemantic;
        anchorCreationData.anchorSearchDeadline = getAnchorDeadline(anchorSearchTimeout);
        return new AnchorEntityImpl(
                node,
                anchorCreationData,
                activitySpace,
                activitySpaceRoot,
                extensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    static AnchorEntityImpl createPersistedAnchor(
            Node node,
            UUID uuid,
            Duration anchorSearchTimeout,
            ActivitySpace activitySpace,
            Entity activitySpaceRoot,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor,
            PerceptionLibrary perceptionLibrary) {
        AnchorCreationData anchorCreationData = new AnchorCreationData();
        anchorCreationData.anchorCreationType = AnchorCreationData.ANCHOR_CREATION_PERSISTED;
        anchorCreationData.uuid = uuid;
        anchorCreationData.anchorSearchDeadline = getAnchorDeadline(anchorSearchTimeout);
        return new AnchorEntityImpl(
                node,
                anchorCreationData,
                activitySpace,
                activitySpaceRoot,
                extensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    static AnchorEntityImpl createAnchorFromPlane(
            Node node,
            Plane plane,
            Pose planeOffsetPose,
            @Nullable Long planeDataTimeNs,
            ActivitySpace activitySpace,
            Entity activitySpaceRoot,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor,
            PerceptionLibrary perceptionLibrary) {
        AnchorCreationData anchorCreationData = new AnchorCreationData();
        anchorCreationData.anchorCreationType = AnchorCreationData.ANCHOR_CREATION_PLANE;
        anchorCreationData.plane = plane;
        anchorCreationData.planeOffsetPose = planeOffsetPose;
        anchorCreationData.planeDataTimeNs = planeDataTimeNs;
        return new AnchorEntityImpl(
                node,
                anchorCreationData,
                activitySpace,
                activitySpaceRoot,
                extensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    static AnchorEntityImpl createAnchorFromPerceptionAnchor(
            Node node,
            androidx.xr.arcore.Anchor anchor,
            ActivitySpace activitySpace,
            Entity activitySpaceRoot,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor,
            PerceptionLibrary perceptionLibrary) {
        AnchorCreationData anchorCreationData = new AnchorCreationData();
        anchorCreationData.anchorCreationType =
                AnchorCreationData.ANCHOR_CREATION_PERCEPTION_ANCHOR;
        anchorCreationData.perceptionAnchor = anchor;
        return new AnchorEntityImpl(
                node,
                anchorCreationData,
                activitySpace,
                activitySpaceRoot,
                extensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    protected AnchorEntityImpl(
            Node node,
            AnchorCreationData anchorCreationData,
            ActivitySpace activitySpace,
            Entity activitySpaceRoot,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor,
            PerceptionLibrary perceptionLibrary) {
        super(node, extensions, entityManager, executor);
        this.perceptionLibrary = perceptionLibrary;

        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction.setName(node, ANCHOR_NODE_NAME).apply();
        }

        if (activitySpace instanceof ActivitySpaceImpl) {
            this.activitySpace = (ActivitySpaceImpl) activitySpace;
        } else {
            Log.e(
                    TAG,
                    "ActivitySpace is not an instance of ActivitySpaceImpl.Anchor is in Error"
                            + " state.");
            this.state = State.ERROR;
            this.activitySpace = null;
        }

        if (activitySpaceRoot instanceof AndroidXrEntity) {
            this.activitySpaceRoot = (AndroidXrEntity) activitySpaceRoot;
        } else {
            Log.e(
                    TAG,
                    "ActivitySpaceRoot is not an instance of AndroidXrEntity. Anchor is in Error"
                            + " state.");
            this.state = State.ERROR;
            this.activitySpaceRoot = null;
        }

        // Return early if the state is already in an error state.
        if (this.state == State.ERROR) {
            return;
        }

        // If we are creating a semantic or persisted anchor then we need to search for the anchor
        // asynchronously. Otherwise we can create the anchor on the plane.
        if (anchorCreationData.anchorCreationType
                == AnchorCreationData.ANCHOR_CREATION_PERCEPTION_ANCHOR) {
            tryConvertAnchor(anchorCreationData.perceptionAnchor);
        } else if (anchorCreationData.anchorCreationType
                        == AnchorCreationData.ANCHOR_CREATION_SEMANTIC
                || anchorCreationData.anchorCreationType
                        == AnchorCreationData.ANCHOR_CREATION_PERSISTED) {
            tryFindAnchor(anchorCreationData);
        } else if (anchorCreationData.anchorCreationType
                == AnchorCreationData.ANCHOR_CREATION_PLANE) {
            tryCreateAnchorForPlane(anchorCreationData);
        }
    }

    @Nullable
    private static Long getAnchorDeadline(Duration anchorSearchTimeout) {
        // If the timeout is zero or null then we return null here and the anchor search will
        // continue
        // indefinitely.
        if (anchorSearchTimeout == null || anchorSearchTimeout.isZero()) {
            return null;
        }
        return SystemClock.uptimeMillis() + anchorSearchTimeout.toMillis();
    }

    // Converts a perception anchor to JXRCore runtime anchor.
    private void tryConvertAnchor(androidx.xr.arcore.Anchor perceptionAnchor) {
        ExportableAnchor exportableAnchor = (ExportableAnchor) perceptionAnchor.getRuntimeAnchor();
        this.anchor =
                new Anchor(exportableAnchor.getNativePointer(), exportableAnchor.getAnchorToken());
        if (anchor.getAnchorToken() == null) {
            updateState(State.ERROR);
            return;
        }
        updateState(State.ANCHORED);
    }

    // Creates an anchor on the provided plane.
    private void tryCreateAnchorForPlane(AnchorCreationData anchorCreationData) {
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                RuntimeUtils.poseToPerceptionPose(anchorCreationData.planeOffsetPose);
        anchor =
                anchorCreationData.plane.createAnchor(
                        perceptionPose, anchorCreationData.planeDataTimeNs);
        if (anchor == null || anchor.getAnchorToken() == null) {
            updateState(State.ERROR);
            return;
        }
        updateState(State.ANCHORED);
    }

    // Schedules a search for the anchor.
    private void scheduleTryFindAnchor(AnchorCreationData anchorCreationData) {
        ScheduledFuture<?> unusedAnchorFuture =
                executor.schedule(
                        () -> tryFindAnchor(anchorCreationData),
                        ANCHOR_SEARCH_DELAY.toMillis(),
                        MILLISECONDS);
    }

    // Checks if the anchor search has exceeded the deadline.
    private boolean searchDeadlineExceeded(Long anchorSearchDeadline) {
        // If the system is paused it will continue to count after it wakes up.
        return anchorSearchDeadline != null && SystemClock.uptimeMillis() > anchorSearchDeadline;
    }

    private synchronized void cancelAnchorSearch() {
        if (state == State.UNANCHORED) {
            Log.i(TAG, "Stopping search for anchor, reached timeout.");
            updateState(State.TIMED_OUT);
        }
    }

    // Searches for the anchor and updates the state based on the result. If the anchor wasn't found
    // then the search is scheduled again if the deadline has not been exceeded.
    private void tryFindAnchor(AnchorCreationData anchorCreationData) {
        if (this.activitySpace == null) {
            Log.e(TAG, "Skipping search for anchor there is no valid parent.");
            return;
        }
        synchronized (this) {
            if (state != State.UNANCHORED) {
                // This should only be searching for an anchor if the state is UNANCHORED. If the
                // state is
                // ANCHORED then the anchor was already found, if it is ERROR then the entity no
                // longer can
                // use the anchor. Return here to stop the search.
                Log.i(TAG, "Stopping search for anchor, the state is: " + state);
                return;
            }
        }
        // Check if we are passed the deadline if so, cancel the search.
        if (searchDeadlineExceeded(anchorCreationData.anchorSearchDeadline)) {
            cancelAnchorSearch();
            return;
        }

        if (perceptionLibrary.getSession() == null) {
            scheduleTryFindAnchor(anchorCreationData);
            return;
        }

        if (anchorCreationData.anchorCreationType == AnchorCreationData.ANCHOR_CREATION_SEMANTIC) {
            anchor = findPlaneAnchor(anchorCreationData);
        } else if (anchorCreationData.anchorCreationType
                == AnchorCreationData.ANCHOR_CREATION_PERSISTED) {
            anchor = perceptionLibrary.getSession().createAnchorFromUuid(anchorCreationData.uuid);
        } else {
            Log.e(
                    TAG,
                    "Searching for anchor creation type is not supported: "
                            + anchorCreationData.anchorCreationType);
        }

        if (anchor == null || anchor.getAnchorToken() == null) {
            scheduleTryFindAnchor(anchorCreationData);
            return;
        }
        Log.i(TAG, "Received anchor: " + anchor.getAnchorToken());
        // TODO: b/330933143 - Handle Additional anchor states (e.g. Error/ Becoming unanchored)
        synchronized (this) {
            // Make sure that we are still looking for the anchor before updating the state. The
            // application might have closed or disposed of the AnchorEntity while the search was
            // still
            // active on another thread.
            if (state != State.UNANCHORED
                    || searchDeadlineExceeded(anchorCreationData.anchorSearchDeadline)) {
                Log.i(TAG, "Found anchor but no longer searching.");
                if (searchDeadlineExceeded(anchorCreationData.anchorSearchDeadline)) {
                    cancelAnchorSearch();
                }
                // Detach the found anchor since it is no longer needed.
                if (!anchor.detach()) {
                    Log.e(TAG, "Error when detaching anchor.");
                }
                return;
            }
            updateState(State.ANCHORED);
            if (anchorCreationData.anchorCreationType
                    == AnchorCreationData.ANCHOR_CREATION_PERSISTED) {
                this.uuid = anchorCreationData.uuid;
                updatePersistState(PersistState.PERSISTED);
            }
        }
    }

    // Tries to find a plane that matches the semantic anchor requirements. This creates an anchor
    // on
    // the plane if found.
    @Nullable
    private Anchor findPlaneAnchor(AnchorCreationData anchorCreationData) {
        for (Plane plane : perceptionLibrary.getSession().getAllPlanes()) {
            long timeNow = SystemClock.uptimeMillis() * 1000000;
            PlaneData planeData = plane.getData(timeNow);
            if (planeData == null) {
                Log.e(TAG, "Plane data is null for plane");
                continue;
            }
            Log.i(
                    TAG,
                    "Found a matching plane with Extent Width: "
                            + planeData.extentWidth
                            + ", Extent Height: "
                            + planeData.extentHeight
                            + ", Type: "
                            + planeData.type
                            + ", Label: "
                            + planeData.label);
            Plane.Type perceptionType = RuntimeUtils.getPlaneType(anchorCreationData.planeType);
            Plane.Label perceptionLabel =
                    RuntimeUtils.getPlaneLabel(anchorCreationData.planeSemantic);
            if (anchorCreationData.dimensions.width <= planeData.extentWidth
                    && anchorCreationData.dimensions.height <= planeData.extentHeight
                    && (planeData.type == perceptionType || perceptionType == Plane.Type.ARBITRARY)
                    && (planeData.label == perceptionLabel
                            || perceptionLabel == Plane.Label.UNKNOWN)) {
                return plane.createAnchor(
                        androidx.xr.scenecore.impl.perception.Pose.identity(), timeNow);
            }
        }
        return null;
    }

    private synchronized void updateState(State newState) {
        if (state == newState) {
            return;
        }
        state = newState;
        if (state == State.ANCHORED) {
            try (NodeTransaction transaction = extensions.createNodeTransaction()) {
                // Attach to the root CPM node. This will enable the anchored content to be visible.
                // Note
                // that the parent of the Entity is null, but the CPM Node is still attached.
                transaction
                        .setParent(node, activitySpace.getNode())
                        .setAnchorId(node, anchor.getAnchorToken())
                        .apply();
            }
        }
        if (onStateChangedListener != null) {
            onStateChangedListener.onStateChanged(state);
        }
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
        this.onStateChangedListener = onStateChangedListener;
    }

    @Override
    @Nullable
    public UUID persist() {
        if (uuid != null) {
            return uuid;
        }
        if (state != State.ANCHORED) {
            Log.e(TAG, "Cannot persist an anchor that is not in the ANCHORED state.");
            return null;
        }
        uuid = anchor.persist();
        if (uuid == null) {
            Log.e(TAG, "Failed to get a UUID for the anchor.");
            return null;
        }
        updatePersistState(PersistState.PERSIST_PENDING);
        schedulePersistStateCheck();
        return uuid;
    }

    private void schedulePersistStateCheck() {
        ScheduledFuture<?> unusedPersistStateFuture =
                executor.schedule(
                        this::checkPersistState,
                        PERSIST_STATE_CHECK_DELAY.toMillis(),
                        MILLISECONDS);
    }

    private void checkPersistState() {
        synchronized (this) {
            if (anchor == null) {
                Log.i(
                        TAG,
                        "Anchor is disposed before becoming persisted, stop checking its persist"
                                + " state.");
                return;
            }
            if (anchor.getPersistState() == Anchor.PersistState.PERSISTED) {
                updatePersistState(PersistState.PERSISTED);
                Log.i(TAG, "Anchor is persisted.");
                return;
            }
        }
        schedulePersistStateCheck();
    }

    @Override
    public void registerPersistStateChangeListener(
            PersistStateChangeListener persistStateChangeListener) {
        this.persistStateChangeListener = persistStateChangeListener;
    }

    private synchronized void updatePersistState(PersistState newPersistState) {
        if (persistState == newPersistState) {
            return;
        }
        persistState = newPersistState;
        if (persistStateChangeListener != null) {
            persistStateChangeListener.onPersistStateChanged(newPersistState);
        }
    }

    @Override
    public PersistState getPersistState() {
        return persistState;
    }

    @Override
    public long nativePointer() {
        return anchor.getAnchorId();
    }

    @Override
    public Pose getPose() {
        throw new UnsupportedOperationException("Cannot get 'pose' on an AnchorEntity.");
    }

    @Override
    public void setPose(Pose pose) {
        throw new UnsupportedOperationException("Cannot set 'pose' on an AnchorEntity.");
    }

    @Override
    public void setScale(Vector3 scale) {
        // TODO(b/349391097): make this behavior consistent with ActivitySpaceImpl
        throw new UnsupportedOperationException("Cannot set 'scale' on an AnchorEntity.");
    }

    // TODO: b/360168321 Use the OpenXrPosableHelper when retrieving the pose in activity space.
    @Override
    public Pose getPoseInActivitySpace() {
        synchronized (this) {
            if (activitySpace == null) {
                throw new IllegalStateException(
                        "Cannot get pose in Activity Space with a null Activity Space.");
            }

            if (state != State.ANCHORED) {
                Log.w(
                        TAG,
                        "Cannot retrieve pose in underlying space. Ensure that the anchor is"
                                + " anchored before calling this method. Returning identity pose.");
                return new Pose();
            }

            // ActivitySpace and the anchor have unit scale and the anchor has no direct parent so
            // we can
            // just compose the two poses without scaling.
            final Pose openXrToAnchor = this.getPoseInOpenXrReferenceSpace();
            final Pose openXrToActivitySpace = activitySpace.getPoseInOpenXrReferenceSpace();
            if (openXrToActivitySpace == null || openXrToAnchor == null) {
                Log.e(
                        TAG,
                        "Cannot retrieve pose in underlying space despite anchor being anchored."
                                + " Returning identity pose.");
                return new Pose();
            }

            final Pose activitySpaceToOpenXr = openXrToActivitySpace.getInverse();
            return activitySpaceToOpenXr.compose(openXrToAnchor);
        }
    }

    // TODO: b/360168321 Use the OpenXrPosableHelper when retrieving the pose in world space.
    @Override
    public Pose getActivitySpacePose() {
        if (activitySpaceRoot == null) {
            throw new IllegalStateException(
                    "Cannot get pose in World Space Pose with a null World Space Entity.");
        }

        // ActivitySpace and the anchor have unit scale and the anchor has no direct parent so we
        // can
        // just
        // compose the two poses without scaling.
        final Pose activitySpaceToAnchor = this.getPoseInActivitySpace();
        final Pose worldSpaceToActivitySpace =
                activitySpaceRoot.getPoseInActivitySpace().getInverse();
        return worldSpaceToActivitySpace.compose(activitySpaceToAnchor);
    }

    @Override
    public Vector3 getActivitySpaceScale() {
        return getWorldSpaceScale().div(activitySpace.getWorldSpaceScale());
    }

    @Override
    public void setParent(Entity parent) {
        throw new UnsupportedOperationException("Cannot set 'parent' on an  AnchorEntity.");
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public void dispose() {
        Log.i(TAG, "Disposing " + this);

        synchronized (this) {
            // Return early if it is already in the error state.
            if (state == State.ERROR) {
                return;
            }
            updateState(State.ERROR);
            if (anchor != null && !anchor.detach()) {
                Log.e(TAG, "Error when detaching anchor.");
            }
            anchor = null;
        }

        // Set the parent of the CPM node to null; to hide the anchored content.The parent of the
        // entity
        // was always null so does not need to be reset.
        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction.setAnchorId(node, null);
            transaction.setParent(node, null).apply();
        }
        super.dispose();
    }
}
