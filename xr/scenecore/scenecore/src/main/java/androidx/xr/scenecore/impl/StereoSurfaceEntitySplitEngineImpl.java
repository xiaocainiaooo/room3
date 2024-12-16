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

import android.view.Surface;

import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.StereoSurfaceEntity;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;
import com.google.ar.imp.apibindings.ImpressApi;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of a RealityCore StereoSurfaceEntitySplitEngine.
 *
 * <p>This is used to create an entity that contains a StereoSurfacePanel using the Split Engine
 * route.
 */
final class StereoSurfaceEntitySplitEngineImpl extends AndroidXrEntity
        implements StereoSurfaceEntity {
    private final ImpressApi impressApi;
    private final SplitEngineSubspaceManager splitEngineSubspaceManager;
    private final SubspaceNode subspace;
    // TODO: b/362520810 - Wrap impress nodes w/ Java class.
    private final int panelImpressNode;
    private final int subspaceImpressNode;
    @StereoMode private int stereoMode = StereoSurfaceEntity.StereoMode.SIDE_BY_SIDE;
    // This are the default dimensions Impress starts with for the Quad mesh used as the canvas
    private Dimensions dimensions = new Dimensions(2.0f, 2.0f, 2.0f);

    public StereoSurfaceEntitySplitEngineImpl(
            Entity parentEntity,
            ImpressApi impressApi,
            SplitEngineSubspaceManager splitEngineSubspaceManager,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor,
            @StereoMode int stereoMode) {
        super(extensions.createNode(), extensions, entityManager, executor);
        this.impressApi = impressApi;
        this.splitEngineSubspaceManager = splitEngineSubspaceManager;
        this.stereoMode = stereoMode;
        setParent(parentEntity);

        // TODO(b/377906324): - Punt this logic to the UI thread, so that applications can create
        // StereoSurface entities from any thread.

        // System will only render Impress nodes that are parented by this subspace node.
        this.subspaceImpressNode = impressApi.createImpressNode();
        String subspaceName = "stereo_surface_panel_entity_subspace_" + subspaceImpressNode;

        this.subspace =
                splitEngineSubspaceManager.createSubspace(subspaceName, subspaceImpressNode);

        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            // Make the Entity node a parent of the subspace node.
            transaction.setParent(subspace.subspaceNode, this.node).apply();
        }
        // The CPM node hierarchy is: Entity CPM node --- parent of ---> Subspace CPM node.
        this.panelImpressNode = impressApi.createStereoSurface(stereoMode);
        impressApi.setImpressNodeParent(panelImpressNode, subspaceImpressNode);
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public void dispose() {
        // TODO(b/377906324): - Punt this logic to the UI thread, so that applications can destroy
        // StereoSurface entities from any thread.
        splitEngineSubspaceManager.deleteSubspace(subspace.subspaceId);
        impressApi.destroyImpressNode(subspaceImpressNode);
        super.dispose();
    }

    @Override
    public void setStereoMode(@StereoMode int mode) {
        stereoMode = mode;
        impressApi.setStereoModeForStereoSurface(panelImpressNode, mode);
    }

    @Override
    public void setDimensions(Dimensions dimensions) {
        // TODO(b/377906324): - Punt this logic to the UI thread, so that applications can call this
        // method from any thread.
        this.dimensions = dimensions;
        impressApi.setCanvasDimensionsForStereoSurface(
                panelImpressNode, dimensions.width, dimensions.height);
    }

    @Override
    public Dimensions getDimensions() {
        return dimensions;
    }

    @Override
    @StereoMode
    public int getStereoMode() {
        return stereoMode;
    }

    @Override
    public Surface getSurface() {
        // TODO(b/377906324) - Either cache the surface in the constructor, or change this interface
        // to
        // return a Future.
        return impressApi.getSurfaceFromStereoSurface(panelImpressNode);
    }
}
