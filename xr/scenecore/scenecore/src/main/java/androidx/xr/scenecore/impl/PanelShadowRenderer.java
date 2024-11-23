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

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceControlViewHost.SurfacePackage;
import android.view.View;

import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;

import java.util.Objects;

/** Class for rendering the border of a panel onto a perception plane. */
class PanelShadowRenderer {
    private static final float STROKE_WIDTH = 20f;
    private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;
    private static final float CORNER_RADIUS = 20f;
    private static final float PANEL_BORDER_ADDED_MARGIN = 50f;
    private final ActivitySpaceImpl activitySpaceImpl;
    private final PerceptionSpaceActivityPoseImpl perceptionSpaceActivityPose;
    private final XrExtensions extensions;
    private SurfaceControlViewHost surfaceControlViewHost;
    private final Handler handler;
    private boolean isVisible;
    private final Activity activity;
    Node panelShadowNode;

    /** PanelShadowView is a view with a blue border to enable the shadow effect. */
    private static class PanelShadowView extends View {

        public PanelShadowView(Context context) {
            super(context);
        }

        @Override
        public void onDrawForeground(Canvas canvas) {
            super.onDrawForeground(canvas);
            Path border = new Path();
            border.addRoundRect(
                    HALF_STROKE_WIDTH,
                    HALF_STROKE_WIDTH,
                    canvas.getWidth() - HALF_STROKE_WIDTH,
                    canvas.getHeight() - HALF_STROKE_WIDTH,
                    CORNER_RADIUS,
                    CORNER_RADIUS,
                    Path.Direction.CW);
            Paint paint = new Paint();
            paint.setColor(0xFF54639F);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(STROKE_WIDTH);
            canvas.drawPath(border, paint);
        }
    }

    public PanelShadowRenderer(
            ActivitySpaceImpl activitySpaceImpl,
            PerceptionSpaceActivityPoseImpl perceptionSpaceActivityPose,
            Activity activity,
            XrExtensions extensions) {
        this.activitySpaceImpl = activitySpaceImpl;
        this.perceptionSpaceActivityPose = perceptionSpaceActivityPose;
        this.extensions = extensions;
        this.activity = activity;
        this.handler = new Handler(Looper.getMainLooper());
    }

    void updatePanelPose(
            Pose openXrToProposedPanel, Pose openXrtoPlane, PanelEntityImpl panelEntity) {
        // If there is no panel shadow node, create it.
        if (panelShadowNode == null) {
            createPanelShadow(openXrToProposedPanel, openXrtoPlane, panelEntity);
            return;
        }

        Pose panelPoseInActivitySpace =
                getUpdatedPanelPoseInActivitySpace(openXrToProposedPanel, openXrtoPlane);
        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            if (!isVisible) {
                transaction.setVisibility(panelShadowNode, true);
                isVisible = true;
            }
            transaction
                    .setPosition(
                            panelShadowNode,
                            panelPoseInActivitySpace.getTranslation().getX(),
                            panelPoseInActivitySpace.getTranslation().getY(),
                            panelPoseInActivitySpace.getTranslation().getZ())
                    .setOrientation(
                            panelShadowNode,
                            panelPoseInActivitySpace.getRotation().getX(),
                            panelPoseInActivitySpace.getRotation().getY(),
                            panelPoseInActivitySpace.getRotation().getZ(),
                            panelPoseInActivitySpace.getRotation().getW())
                    .apply();
        }
    }

    void hidePlane() {
        if (!isVisible) {
            return;
        }
        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction.setVisibility(panelShadowNode, false).apply();
        }
        isVisible = false;
    }

    void destroy() {
        if (surfaceControlViewHost != null) {
            handler.post(() -> surfaceControlViewHost.release());
        }
        if (panelShadowNode != null) {
            try (NodeTransaction transaction = extensions.createNodeTransaction()) {
                transaction.setParent(panelShadowNode, null).apply();
            }
        }
        panelShadowNode = null;
    }

    private void createPanelShadow(
            Pose openXrToProposedPanel, Pose openXrtoPlane, PanelEntityImpl panelEntity) {
        View view = new PanelShadowView(activity);

        // Scale the panel shadow to the size of the PanelEntity in the activity space.
        Vector3 entityScale = panelEntity.getWorldSpaceScale();
        float sizeX =
                panelEntity.getPixelDimensions().width
                                * entityScale.getX()
                                / activitySpaceImpl.getWorldSpaceScale().getX()
                        + PANEL_BORDER_ADDED_MARGIN;
        float sizeZ =
                panelEntity.getPixelDimensions().height
                                * entityScale.getZ()
                                / activitySpaceImpl.getWorldSpaceScale().getX()
                        + PANEL_BORDER_ADDED_MARGIN;

        Pose panelPoseInActivitySpace =
                getUpdatedPanelPoseInActivitySpace(openXrToProposedPanel, openXrtoPlane);

        panelShadowNode = extensions.createNode();

        // The surfaceControlViewHost needs to be created on the main thread.
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        handler.post(
                () -> {
                    surfaceControlViewHost =
                            new SurfaceControlViewHost(
                                    activity,
                                    Objects.requireNonNull(activity.getDisplay()),
                                    new Binder());
                    surfaceControlViewHost.setView(view, (int) sizeX, (int) sizeZ);
                    SurfacePackage surfacePackage =
                            Objects.requireNonNull(surfaceControlViewHost.getSurfacePackage());
                    try (NodeTransaction transaction = extensions.createNodeTransaction()) {
                        transaction
                                .setName(panelShadowNode, "PanelRenderer")
                                .setSurfacePackage(panelShadowNode, surfacePackage)
                                .setWindowBounds(surfacePackage, (int) sizeX, (int) sizeZ)
                                .setVisibility(panelShadowNode, true)
                                .setPosition(
                                        panelShadowNode,
                                        panelPoseInActivitySpace.getTranslation().getX(),
                                        panelPoseInActivitySpace.getTranslation().getY(),
                                        panelPoseInActivitySpace.getTranslation().getZ())
                                .setOrientation(
                                        panelShadowNode,
                                        panelPoseInActivitySpace.getRotation().getX(),
                                        panelPoseInActivitySpace.getRotation().getY(),
                                        panelPoseInActivitySpace.getRotation().getZ(),
                                        panelPoseInActivitySpace.getRotation().getW())
                                .setParent(panelShadowNode, activitySpaceImpl.getNode())
                                .apply();
                    }
                    surfacePackage.release();
                });
        isVisible = true;
    }

    private Pose getUpdatedPanelPoseInActivitySpace(
            Pose openXrToProposedPanel, Pose openXrtoPlane) {
        Pose planeToOpenXr = openXrtoPlane.getInverse();
        Pose planeToPanel = planeToOpenXr.compose(openXrToProposedPanel);
        Pose planeToProjectedPanel =
                new Pose(
                        new Vector3(
                                planeToPanel.getTranslation().getX(),
                                0f,
                                planeToPanel.getTranslation().getZ()),
                        planeToOpenXr
                                .getRotation()
                                .times(
                                        PlaneUtils.rotateEntityToPlane(
                                                openXrToProposedPanel.getRotation(),
                                                openXrtoPlane.getRotation())));
        Pose panelInOxr = openXrtoPlane.compose(planeToProjectedPanel);
        return perceptionSpaceActivityPose.transformPoseTo(panelInOxr, activitySpaceImpl);
    }
}
