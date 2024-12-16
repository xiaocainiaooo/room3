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
import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.GltfEntity;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;
import com.google.ar.imp.apibindings.ImpressApi;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of a RealityCore GltfEntitySplitEngine.
 *
 * <p>This is used to create an entity that contains a glTF object using the Split Engine route.
 */
// TODO: b/375520647 - Add unit tests for this class.
class GltfEntityImplSplitEngine extends AndroidXrEntity implements GltfEntity {
    private final ImpressApi impressApi;
    private final SplitEngineSubspaceManager splitEngineSubspaceManager;
    private final SubspaceNode subspace;
    private final int modelImpressNode;
    private final int subspaceImpressNode;
    @AnimationState private int animationState = AnimationState.STOPPED;

    public GltfEntityImplSplitEngine(
            GltfModelResourceImplSplitEngine gltfModelResource,
            Entity parentEntity,
            ImpressApi impressApi,
            SplitEngineSubspaceManager splitEngineSubspaceManager,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        super(extensions.createNode(), extensions, entityManager, executor);
        this.impressApi = impressApi;
        this.splitEngineSubspaceManager = splitEngineSubspaceManager;
        setParent(parentEntity);

        // TODO(b/377907379): - Punt this logic to the UI thread, so that applications can create
        // Gltf entities from any thread.

        // System will only render Impress nodes that are parented by this subspace node.
        this.subspaceImpressNode = impressApi.createImpressNode();
        String subspaceName = "gltf_entity_subspace_" + subspaceImpressNode;

        this.subspace =
                splitEngineSubspaceManager.createSubspace(subspaceName, subspaceImpressNode);

        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            // Make the Entity node a parent of the subspace node.
            transaction.setParent(subspace.subspaceNode, this.node).apply();
        }
        this.modelImpressNode =
                impressApi.instanceGltfModel(gltfModelResource.getExtensionModelToken());
        impressApi.setImpressNodeParent(modelImpressNode, subspaceImpressNode);
        // The Impress node hierarchy is: Subspace Impress node --- parent of ---> model Impress
        // node.
        // The CPM node hierarchy is: Entity CPM node --- parent of ---> Subspace CPM node.
    }

    @Override
    public void startAnimation(boolean looping, @Nullable String animationName) {
        // TODO: b/362826747 - Add a listener interface so that the application can be
        // notified that the animation has stopped, been cancelled (by starting another animation)
        // and / or shown an error state if something went wrong.

        // TODO(b/377907379): - Punt this logic to the UI thread.

        // Note that at the moment this future will be garbage collected, since we don't return it
        // from
        // this method.
        ListenableFuture<Void> future =
                impressApi.animateGltfModel(modelImpressNode, animationName, looping);
        animationState = AnimationState.PLAYING;

        // At the moment, we don't do anything interesting on failure except for logging. If we
        // didn't
        // care about logging the failure, we could just not register a listener at all if the
        // animation
        // is looping, since it will never terminate normally.
        future.addListener(
                () -> {
                    try {
                        future.get();
                        // The animation played to completion and has stopped
                        animationState = AnimationState.STOPPED;
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            // If this happened, then it's likely Impress is shutting down and we
                            // need to
                            // shut down as well.
                            Thread.currentThread().interrupt();
                        } else {
                            // Some other error happened.  Log it and stop the animation.
                            Log.e("GltfEntityImpl", "Could not start animation: " + e);
                            animationState = AnimationState.STOPPED;
                        }
                    }
                },
                this.executor);
    }

    @Override
    public void stopAnimation() {
        // TODO(b/377907379): - Punt this logic to the UI thread.
        impressApi.stopGltfModelAnimation(modelImpressNode);
        animationState = AnimationState.STOPPED;
    }

    @Override
    @AnimationState
    public int getAnimationState() {
        return animationState;
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public void dispose() {
        // TODO(b/377907379): - Punt this logic to the UI thread.
        splitEngineSubspaceManager.deleteSubspace(subspace.subspaceId);
        impressApi.destroyImpressNode(modelImpressNode);
        impressApi.destroyImpressNode(subspaceImpressNode);
        super.dispose();
    }

    public void setColliderEnabled(boolean enableCollider) {
        // TODO(b/377907379): - Punt this logic to the UI thread
        impressApi.setGltfModelColliderEnabled(modelImpressNode, enableCollider);
    }
}
