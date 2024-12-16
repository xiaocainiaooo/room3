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

import static java.lang.Math.max;

import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.xr.extensions.Consumer;
import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.node.ReformEvent;
import androidx.xr.extensions.node.ReformOptions;
import androidx.xr.extensions.node.Vec3;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorPlacement;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.MovableComponent;
import androidx.xr.scenecore.JxrPlatformAdapter.MoveEvent;
import androidx.xr.scenecore.JxrPlatformAdapter.MoveEventListener;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneSemantic;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneType;
import androidx.xr.scenecore.JxrPlatformAdapter.Ray;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Plane;
import androidx.xr.scenecore.impl.perception.Plane.PlaneData;
import androidx.xr.scenecore.impl.perception.Session;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/** Implementation of MovableComponent. */
@SuppressWarnings("BanConcurrentHashMap")
class MovableComponentImpl implements MovableComponent {
    private static final String TAG = "MovableComponentImpl";
    static final float MIN_PLANE_ANCHOR_DISTANCE = .2f;
    private final boolean systemMovable;
    private final boolean scaleInZ;
    private final boolean shouldDisposeParentAnchor;
    private final PerceptionLibrary perceptionLibrary;
    private final XrExtensions extensions;
    private final ActivitySpaceImpl activitySpaceImpl;
    private final AndroidXrEntity activitySpaceEntity;
    private final PerceptionSpaceActivityPoseImpl perceptionSpaceActivityPose;
    private final EntityManager entityManager;
    private final PanelShadowRenderer panelShadowRenderer;
    private final ScheduledExecutorService runtimeExecutor;
    private final ConcurrentHashMap<MoveEventListener, Executor> moveEventListenersMap =
            new ConcurrentHashMap<>();
    private final Map<PlaneType, Map<PlaneSemantic, AnchorPlacementImpl>> anchorableFilters =
            new EnumMap<>(PlaneType.class);
    // Visible for testing.
    Consumer<ReformEvent> reformEventConsumer;
    private Entity entity;
    private Entity initialParent;
    private Pose lastPose = new Pose();
    private Vector3 lastScale = new Vector3(1f, 1f, 1f);
    private Dimensions currentSize;
    private boolean userAnchorable = false;
    private AnchorEntity createdAnchorEntity;
    private AnchorPlacementImpl createdAnchorPlacement;
    @ScaleWithDistanceMode private int scaleWithDistanceMode = ScaleWithDistanceMode.DEFAULT;

    public MovableComponentImpl(
            boolean systemMovable,
            boolean scaleInZ,
            Set<AnchorPlacement> anchorPlacement,
            boolean shouldDisposeParentAnchor,
            PerceptionLibrary perceptionLibrary,
            XrExtensions extensions,
            ActivitySpaceImpl activitySpaceImpl,
            AndroidXrEntity activitySpaceEntity,
            PerceptionSpaceActivityPoseImpl perceptionSpaceActivityPose,
            EntityManager entityManager,
            PanelShadowRenderer panelShadowRenderer,
            ScheduledExecutorService runtimeExecutor) {
        this.systemMovable = systemMovable;
        this.scaleInZ = scaleInZ;
        this.shouldDisposeParentAnchor = shouldDisposeParentAnchor;
        this.perceptionLibrary = perceptionLibrary;
        this.extensions = extensions;
        this.activitySpaceImpl = activitySpaceImpl;
        this.activitySpaceEntity = activitySpaceEntity;
        this.perceptionSpaceActivityPose = perceptionSpaceActivityPose;
        this.entityManager = entityManager;
        this.panelShadowRenderer = panelShadowRenderer;
        this.runtimeExecutor = runtimeExecutor;
        setUpAnchorPlacement(anchorPlacement);
    }

    @Override
    public boolean onAttach(Entity entity) {
        if (this.entity != null) {
            Log.e(TAG, "Already attached to entity " + this.entity);
            return false;
        }
        this.entity = entity;
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        // The math for anchoring uses the pose relative to the activity space so we should not set
        // the reform options to be relative to the parent if the entity is anchorable.
        int reformFlags = userAnchorable ? 0 : ReformOptions.FLAG_POSE_RELATIVE_TO_PARENT;
        reformFlags =
                (systemMovable && !userAnchorable)
                        ? reformFlags | ReformOptions.FLAG_ALLOW_SYSTEM_MOVEMENT
                        : reformFlags;
        reformFlags = scaleInZ ? reformFlags | ReformOptions.FLAG_SCALE_WITH_DISTANCE : reformFlags;
        reformOptions.setFlags(reformFlags);
        reformOptions.setEnabledReform(reformOptions.getEnabledReform() | ReformOptions.ALLOW_MOVE);
        reformOptions.setScaleWithDistanceMode(
                translateScaleWithDistanceMode(scaleWithDistanceMode));

        // TODO: b/348037292 - Remove this special case for PanelEntityImpl.
        if (entity instanceof PanelEntityImpl && currentSize == null) {
            currentSize = ((PanelEntityImpl) entity).getSize();
        }
        if (currentSize != null) {
            reformOptions.setCurrentSize(
                    new Vec3(currentSize.width, currentSize.height, currentSize.depth));
        }
        if (userAnchorable && systemMovable && reformEventConsumer == null) {
            reformEventConsumer =
                    reformEvent -> {
                        Pair<Pose, Entity> unused = getUpdatedReformEventPoseAndParent(reformEvent);
                    };
        }
        lastPose = entity.getPose();
        lastScale = entity.getScale();
        ((AndroidXrEntity) entity).updateReformOptions();
        if (reformEventConsumer != null) {
            ((AndroidXrEntity) entity).addReformEventConsumer(reformEventConsumer, runtimeExecutor);
        }
        return true;
    }

    @Override
    public void onDetach(Entity entity) {
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        reformOptions.setEnabledReform(
                reformOptions.getEnabledReform() & ~ReformOptions.ALLOW_MOVE);
        // Clear any flags that were set by this component.
        int reformFlags = reformOptions.getFlags();
        reformFlags =
                systemMovable
                        ? reformFlags & ~ReformOptions.FLAG_ALLOW_SYSTEM_MOVEMENT
                        : reformFlags;
        reformFlags =
                scaleInZ ? reformFlags & ~ReformOptions.FLAG_SCALE_WITH_DISTANCE : reformFlags;
        reformOptions.setFlags(reformFlags);
        ((AndroidXrEntity) entity).updateReformOptions();
        if (reformEventConsumer != null) {
            ((AndroidXrEntity) entity).removeReformEventConsumer(reformEventConsumer);
            reformEventConsumer = null;
        }
        this.entity = null;
    }

    @Override
    public void setSize(Dimensions dimensions) {
        currentSize = dimensions;
        if (entity == null) {
            Log.i(TAG, "setSize called before component is attached to an Entity.");
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        reformOptions.setCurrentSize(
                new Vec3(dimensions.width, dimensions.height, dimensions.depth));
        ((AndroidXrEntity) entity).updateReformOptions();
    }

    @Override
    @ScaleWithDistanceMode
    public int getScaleWithDistanceMode() {
        return scaleWithDistanceMode;
    }

    @Override
    public void setScaleWithDistanceMode(@ScaleWithDistanceMode int scaleWithDistanceMode) {
        this.scaleWithDistanceMode = scaleWithDistanceMode;
        if (entity == null) {
            Log.w(
                    TAG,
                    "setScaleWithDistanceMode called before component is attached to an Entity.");
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        reformOptions.setScaleWithDistanceMode(
                translateScaleWithDistanceMode(scaleWithDistanceMode));
        ((AndroidXrEntity) entity).updateReformOptions();
    }

    @Override
    public void addMoveEventListener(Executor executor, MoveEventListener moveEventListener) {
        if (reformEventConsumer != null) {
            ((AndroidXrEntity) entity).removeReformEventConsumer(reformEventConsumer);
        }
        reformEventConsumer =
                reformEvent -> {
                    if (reformEvent.getType() != ReformEvent.REFORM_TYPE_MOVE) {
                        return;
                    }
                    if (reformEvent.getState() == ReformEvent.REFORM_STATE_START) {
                        initialParent = entity.getParent();
                    }
                    Pose newPose;
                    Entity updatedParent = null;
                    if (userAnchorable) {
                        Pair<Pose, Entity> updatedPoseParentPair =
                                getUpdatedReformEventPoseAndParent(reformEvent);
                        newPose = updatedPoseParentPair.first;
                        updatedParent = updatedPoseParentPair.second;
                    } else {
                        newPose =
                                RuntimeUtils.getPose(
                                        reformEvent.getProposedPosition(),
                                        reformEvent.getProposedOrientation());
                    }
                    Vector3 newScale = RuntimeUtils.getVector3(reformEvent.getProposedScale());
                    Entity disposeEntity = null;

                    Entity parent = updatedParent;
                    moveEventListenersMap.forEach(
                            (listener, listenerExecutor) ->
                                    executor.execute(
                                            () ->
                                                    listener.onMoveEvent(
                                                            new MoveEvent(
                                                                    reformEvent.getState(),
                                                                    new Ray(
                                                                            RuntimeUtils.getVector3(
                                                                                    reformEvent
                                                                                            .getInitialRayOrigin()),
                                                                            RuntimeUtils.getVector3(
                                                                                    reformEvent
                                                                                            .getInitialRayDirection())),
                                                                    new Ray(
                                                                            RuntimeUtils.getVector3(
                                                                                    reformEvent
                                                                                            .getCurrentRayOrigin()),
                                                                            RuntimeUtils.getVector3(
                                                                                    reformEvent
                                                                                            .getCurrentRayDirection())),
                                                                    lastPose,
                                                                    newPose,
                                                                    lastScale,
                                                                    newScale,
                                                                    initialParent,
                                                                    parent,
                                                                    disposeEntity))));
                    lastPose = newPose;
                    lastScale = newScale;
                };
        moveEventListenersMap.put(moveEventListener, executor);
        if (entity == null) {
            Log.i(TAG, "setMoveEventListener called before component is attached to an Entity.");
            return;
        }
        ((AndroidXrEntity) entity).addReformEventConsumer(reformEventConsumer, executor);
    }

    private void setUpAnchorPlacement(Set<AnchorPlacement> anchorPlacement) {

        for (AnchorPlacement placement : anchorPlacement) {
            if (!(placement instanceof AnchorPlacementImpl)) {
                continue;
            }
            AnchorPlacementImpl placementImpl = (AnchorPlacementImpl) placement;
            Map<PlaneSemantic, AnchorPlacementImpl> anchorablePlaneSemantic =
                    new EnumMap<>(PlaneSemantic.class);
            for (PlaneSemantic planeSemantic : placementImpl.planeSemanticFilter) {
                anchorablePlaneSemantic.put(planeSemantic, placementImpl);
            }
            for (PlaneType planeType : placementImpl.planeTypeFilter) {
                this.anchorableFilters.put(planeType, anchorablePlaneSemantic);
            }
        }
        if (!anchorableFilters.isEmpty()) {
            this.userAnchorable = true;
        }
    }

    @Override
    public void removeMoveEventListener(MoveEventListener moveEventListener) {
        moveEventListenersMap.remove(moveEventListener);
    }

    private Pair<Pose, Entity> getUpdatedReformEventPoseAndParent(ReformEvent reformEvent) {
        if (reformEvent.getState() == ReformEvent.REFORM_STATE_END && shouldRenderPlaneShadow()) {
            panelShadowRenderer.destroy();
        }
        Pose proposedPose =
                RuntimeUtils.getPose(
                        reformEvent.getProposedPosition(), reformEvent.getProposedOrientation());
        Pair<Pose, Entity> updatedEntity = updatePoseWithPlanes(proposedPose, reformEvent);
        if (systemMovable) {
            entity.setPose(updatedEntity.first);
        }
        return updatedEntity;
    }

    private Pair<Pose, Entity> updatePoseWithPlanes(Pose proposedPose, ReformEvent reformEvent) {
        Session session = perceptionLibrary.getSession();
        if (session == null) {
            Log.w(TAG, "Unable to load perception session, cannot anchor object to a plane.");
            return Pair.create(proposedPose, null);
        }
        List<Plane> planes = session.getAllPlanes();
        if (planes.isEmpty()) {
            return Pair.create(proposedPose, null);
        }

        // The proposed pose is relative to the activity space, it needs to be updated to be in the
        // perception reference space to be compared against the planes..
        Pose updatedPoseInOpenXr =
                activitySpaceImpl.transformPoseTo(proposedPose, perceptionSpaceActivityPose);

        // Create variables to store the plane in case we need to anchor to it later.
        Plane anchorablePlane = null;
        PlaneData anchorablePlaneData = null;
        AnchorPlacementImpl anchorPlacement = null;
        Long dataTimeNs = SystemClock.uptimeMillis() * 1000000;

        // Update the pose based on all the known planes.
        for (Plane plane : planes) {

            PlaneData planeData = plane.getData(dataTimeNs);
            if (planeData == null) {
                continue;
            }
            Pose planePoseUpdate = updatePoseForPlane(planeData, updatedPoseInOpenXr);
            if (planePoseUpdate == null) {
                continue;
            }
            AnchorPlacementImpl newAnchorPlacement = getAnchorPlacementIfAnchorable(planeData);
            if (newAnchorPlacement != null) {
                anchorablePlane = plane;
                anchorablePlaneData = planeData;
                anchorPlacement = newAnchorPlacement;
            }
            updatedPoseInOpenXr = planePoseUpdate;
        }

        if (anchorablePlane != null) {
            // If the reform state is end, we should try to anchor the entity to the plane.
            if (reformEvent.getState() == ReformEvent.REFORM_STATE_END) {
                Pair<Pose, AnchorEntity> resultPair =
                        anchorEntityToPlane(
                                updatedPoseInOpenXr,
                                anchorablePlane,
                                anchorablePlaneData,
                                anchorPlacement,
                                dataTimeNs);
                if (resultPair.second != null) {
                    return Pair.create(resultPair.first, (Entity) resultPair.second);
                }
            } else {
                // Otherwise, we should try to render the plane shadow onto the plane.
                tryRenderPlaneShadow(
                        updatedPoseInOpenXr,
                        RuntimeUtils.fromPerceptionPose(anchorablePlaneData.centerPose));
            }
        } else if (shouldRenderPlaneShadow()) {
            // If there is nothing to anchor to, hide the plane shadow.
            panelShadowRenderer.hidePlane();
        }

        // If the entity was anchored and the reform is complete, update the entity to be in the
        // activity space and remove the previously created anchor data. If
        // shouldDisposeParentAnchor is
        // true dispose the previously created anchor entity.
        if (createdAnchorEntity != null
                && entity.getParent() == createdAnchorEntity
                && reformEvent.getState() == ReformEvent.REFORM_STATE_END) {

            entity.setScale(
                    entity.getWorldSpaceScale().div(activitySpaceImpl.getWorldSpaceScale()));
            entity.setParent(activitySpaceImpl);
            checkAndDisposeAnchorEntity();
            createdAnchorEntity = null;
            createdAnchorPlacement = null;

            // Move the updated pose back to the activity space.
            Pose updatedPoseInActivitySpace =
                    perceptionSpaceActivityPose.transformPoseTo(
                            updatedPoseInOpenXr, activitySpaceImpl);
            return Pair.create(updatedPoseInActivitySpace, activitySpaceImpl);
        }

        // If the entity has a parent, transform the pose to the parent's space.
        Entity parent = entity.getParent();
        if (parent == null || parent == activitySpaceImpl) {
            return Pair.create(
                    perceptionSpaceActivityPose.transformPoseTo(
                            updatedPoseInOpenXr, activitySpaceImpl),
                    null);
        }

        return Pair.create(
                perceptionSpaceActivityPose.transformPoseTo(updatedPoseInOpenXr, parent), null);
    }

    // Gets the anchor placement settings for the given plane data, if it is null the entity should
    // not be anchored to this plane.
    @Nullable
    private AnchorPlacementImpl getAnchorPlacementIfAnchorable(PlaneData planeData) {
        if (!userAnchorable || !systemMovable) {
            return null;
        }
        Map<PlaneSemantic, AnchorPlacementImpl> anchorablePlaneSemantic =
                anchorableFilters.get(RuntimeUtils.getPlaneType(planeData.type));
        if (anchorablePlaneSemantic != null) {
            if (anchorablePlaneSemantic.containsKey(
                    RuntimeUtils.getPlaneSemantic(planeData.label))) {
                return anchorablePlaneSemantic.get(RuntimeUtils.getPlaneSemantic(planeData.label));
            } else if (anchorablePlaneSemantic.containsKey(PlaneSemantic.ANY)) {
                return anchorablePlaneSemantic.get(PlaneSemantic.ANY);
            }
        }
        anchorablePlaneSemantic = anchorableFilters.get(PlaneType.ANY);
        if (anchorablePlaneSemantic != null) {
            if (anchorablePlaneSemantic.containsKey(
                    RuntimeUtils.getPlaneSemantic(planeData.label))) {
                return anchorablePlaneSemantic.get(RuntimeUtils.getPlaneSemantic(planeData.label));
            } else if (anchorablePlaneSemantic.containsKey(PlaneSemantic.ANY)) {
                return anchorablePlaneSemantic.get(PlaneSemantic.ANY);
            }
        }
        return null;
    }

    private Pair<Pose, AnchorEntity> anchorEntityToPlane(
            Pose updatedPose,
            Plane plane,
            PlaneData anchorablePlaneData,
            AnchorPlacementImpl anchorPlacement,
            Long dataTimeNs) {
        AnchorEntityImpl anchorEntity =
                AnchorEntityImpl.createAnchorFromPlane(
                        extensions.createNode(),
                        plane,
                        new Pose(),
                        dataTimeNs,
                        activitySpaceImpl,
                        activitySpaceEntity,
                        extensions,
                        entityManager,
                        runtimeExecutor,
                        perceptionLibrary);
        if (anchorEntity.getState() != AnchorEntityImpl.State.ANCHORED) {
            return Pair.create(updatedPose, null);
        }

        // TODO: b/367754233: Fix the flashing when parented to a new anchor.
        // Check the scale of the entity before the move so we can rescale when we move it to the
        // AnchorEntity. Note the AnchorEntity has a scale of 1 so we don't need to also scale by
        // the
        // anchor entity's scale.
        Vector3 entityScale = entity.getWorldSpaceScale();
        entity.setScale(entityScale);
        Quaternion planeRotation =
                RuntimeUtils.fromPerceptionPose(anchorablePlaneData.centerPose).getRotation();
        Pose rotatedPose =
                new Pose(
                        updatedPose.getTranslation(),
                        PlaneUtils.rotateEntityToPlane(updatedPose.getRotation(), planeRotation));

        Pose planeCenterPose = RuntimeUtils.fromPerceptionPose(anchorablePlaneData.centerPose);
        Pose poseToAnchor = planeCenterPose.getInverse().compose(rotatedPose);
        poseToAnchor =
                new Pose(
                        new Vector3(
                                poseToAnchor.getTranslation().getX(),
                                0f,
                                poseToAnchor.getTranslation().getZ()),
                        poseToAnchor.getRotation());
        entity.setParent(anchorEntity);
        // If the anchor placement settings specify that the anchor should be disposed, dispose of
        // the
        // previously created anchor entity.
        checkAndDisposeAnchorEntity();
        createdAnchorEntity = anchorEntity;
        createdAnchorPlacement = anchorPlacement;
        return Pair.create(poseToAnchor, anchorEntity);
    }

    @Nullable
    private Pose updatePoseForPlane(PlaneData planeData, Pose proposedPoseInOpenXr) {
        // Get the pose as related to the center of the plane.

        Pose centerPose = RuntimeUtils.fromPerceptionPose(planeData.centerPose);
        Pose centerPoseToProposedPose = centerPose.getInverse().compose(proposedPoseInOpenXr);

        // The extents of the plane are in the X and Z directions so we can use those to determine
        // if
        // the point is outside the plane.
        if (centerPoseToProposedPose.getTranslation().getX() < -planeData.extentWidth
                || centerPoseToProposedPose.getTranslation().getX() > planeData.extentWidth
                || centerPoseToProposedPose.getTranslation().getZ() < -planeData.extentHeight
                || centerPoseToProposedPose.getTranslation().getZ() > planeData.extentHeight) {
            return null;
        }

        // The distance between the point and the plane. If it is less than the minimum allowed
        // distance, move it to the plane. We only need to take the y-value because the y-value is
        // normal to the plane. We established above that the point is within the extents of the
        // plane.
        float distance = centerPoseToProposedPose.getTranslation().getY();
        if (distance >= MIN_PLANE_ANCHOR_DISTANCE) {
            return null;
        }
        centerPoseToProposedPose =
                new Pose(
                        new Vector3(
                                centerPoseToProposedPose.getTranslation().getX(),
                                max(0f, distance),
                                centerPoseToProposedPose.getTranslation().getZ()),
                        centerPoseToProposedPose.getRotation());
        return centerPose.compose(centerPoseToProposedPose);
    }

    private void tryRenderPlaneShadow(Pose proposedPose, Pose planePose) {
        if (!shouldRenderPlaneShadow()) {
            return;
        }
        panelShadowRenderer.updatePanelPose(proposedPose, planePose, (PanelEntityImpl) entity);
    }

    private boolean shouldRenderPlaneShadow() {
        return entity instanceof PanelEntityImpl && systemMovable;
    }

    // Checks if there is a created anchor entity and if it should be disposed. If so, disposes of
    // the
    // anchor entity. Resets the createdAnchorEntity and createdAnchorPlacement to null.
    private void checkAndDisposeAnchorEntity() {
        if (createdAnchorEntity != null
                && createdAnchorEntity.getChildren().isEmpty()
                && createdAnchorPlacement != null
                && shouldDisposeParentAnchor) {
            createdAnchorEntity.dispose();
        }
    }

    private static @ReformOptions.ScaleWithDistanceMode int translateScaleWithDistanceMode(
            @ScaleWithDistanceMode int scale) {
        switch (scale) {
            case ScaleWithDistanceMode.DMM:
                return ReformOptions.SCALE_WITH_DISTANCE_MODE_DMM;
            default:
                return ReformOptions.SCALE_WITH_DISTANCE_MODE_DEFAULT;
        }
    }
}
