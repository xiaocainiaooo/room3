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

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter.CameraViewActivityPose;
import androidx.xr.scenecore.common.BaseActivityPose;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.impl.perception.ViewProjection;
import androidx.xr.scenecore.impl.perception.ViewProjections;

/**
 * A ActivityPose representing a user's camera. This can be used to determine the location and field
 * of view of the camera.
 */
final class CameraViewActivityPoseImpl extends BaseActivityPose implements CameraViewActivityPose {
    private static final String TAG = "CameraViewActivityPose";
    private final PerceptionLibrary perceptionLibrary;
    @CameraType private final int cameraType;
    private final OpenXrActivityPoseHelper openXrActivityPoseHelper;
    // Default the pose to null. A null pose indicates that the camera is not ready yet.
    private Pose lastOpenXrPose = null;

    public CameraViewActivityPoseImpl(
            @CameraType int cameraType,
            ActivitySpaceImpl activitySpace,
            AndroidXrEntity activitySpaceRoot,
            PerceptionLibrary perceptionLibrary) {
        this.cameraType = cameraType;
        this.perceptionLibrary = perceptionLibrary;
        this.openXrActivityPoseHelper =
                new OpenXrActivityPoseHelper(activitySpace, activitySpaceRoot);
    }

    @Override
    public Pose getPoseInActivitySpace() {
        return openXrActivityPoseHelper.getPoseInActivitySpace(getPoseInOpenXrReferenceSpace());
    }

    @Override
    public Pose getActivitySpacePose() {
        return openXrActivityPoseHelper.getActivitySpacePose(getPoseInOpenXrReferenceSpace());
    }

    @Override
    public Vector3 getActivitySpaceScale() {
        // This WorldPose is assumed to always have a scale of 1.0f in the OpenXR reference space.
        return openXrActivityPoseHelper.getActivitySpaceScale(new Vector3(1f, 1f, 1f));
    }

    @Nullable
    private ViewProjection getViewProjection() {
        final Session session = perceptionLibrary.getSession();
        if (session == null) {
            Log.w(TAG, "Cannot retrieve the camera pose with a null perception session.");
            return null;
        }
        ViewProjections perceptionViews = session.getStereoViews();
        if (perceptionViews == null) {
            Log.e(TAG, "Error retrieving the camera.");
            return null;
        }
        if (cameraType == CameraViewActivityPose.CAMERA_TYPE_LEFT_EYE) {
            return perceptionViews.getLeftEye();
        } else if (cameraType == CameraViewActivityPose.CAMERA_TYPE_RIGHT_EYE) {
            return perceptionViews.getRightEye();
        } else {
            Log.w(TAG, "Unsupported camera type: " + cameraType);
            return null;
        }
    }

    /** Gets the pose in the OpenXR reference space. Can be null if it is not yet ready. */
    @Nullable
    public Pose getPoseInOpenXrReferenceSpace() {
        ViewProjection viewProjection = getViewProjection();
        if (viewProjection != null) {
            lastOpenXrPose = RuntimeUtils.fromPerceptionPose(viewProjection.getPose());
        }
        return lastOpenXrPose;
    }

    @Override
    @CameraType
    public int getCameraType() {
        return cameraType;
    }

    @Override
    public Fov getFov() {
        ViewProjection viewProjection = getViewProjection();
        if (viewProjection == null) {
            return new Fov(0, 0, 0, 0);
        }
        return RuntimeUtils.fovFromPerceptionFov(viewProjection.getFov());
    }
}
