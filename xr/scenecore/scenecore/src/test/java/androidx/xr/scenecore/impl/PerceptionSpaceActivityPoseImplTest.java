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

import androidx.xr.extensions.node.Mat4f;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeGltfModelToken;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeNode;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeNodeTransform;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class PerceptionSpaceActivityPoseImplTest {

    private final AndroidXrEntity activitySpaceRoot = Mockito.mock(AndroidXrEntity.class);
    private final FakeXrExtensions fakeExtensions = new FakeXrExtensions();
    private final FakeScheduledExecutorService executor = new FakeScheduledExecutorService();
    private final EntityManager entityManager = new EntityManager();
    private final ActivitySpaceImpl activitySpace =
            new ActivitySpaceImpl(
                    fakeExtensions.createNode(),
                    fakeExtensions,
                    entityManager,
                    () -> fakeExtensions.fakeSpatialState,
                    executor);

    private PerceptionSpaceActivityPoseImpl perceptionSpaceActivityPose;

    private FakeNode getActivitySpaceNode() {
        return (FakeNode) activitySpace.getNode();
    }

    /** Creates a generic glTF entity. */
    private GltfEntityImpl createGltfEntity() {
        FakeGltfModelToken modelToken = new FakeGltfModelToken("model");
        GltfModelResourceImpl model = new GltfModelResourceImpl(modelToken);
        return new GltfEntityImpl(model, activitySpace, fakeExtensions, entityManager, executor);
    }

    @Before
    public void setUp() {
        perceptionSpaceActivityPose =
                new PerceptionSpaceActivityPoseImpl(activitySpace, activitySpaceRoot);
    }

    @Test
    public void getPoseInActivitySpace_returnsInverseOfActivitySpacePose() {
        Matrix4 activitySpaceMatrix =
                Matrix4.fromTrs(
                        new Vector3(1.0f, 2.0f, 3.0f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f)),
                        new Vector3(1.0f, 1.0f, 1.0f));
        getActivitySpaceNode()
                .sendTransformEvent(
                        new FakeNodeTransform(new Mat4f(activitySpaceMatrix.getData())));
        executor.runAll();

        Pose poseInActivitySpace = perceptionSpaceActivityPose.getPoseInActivitySpace();

        Pose expectedPose = activitySpaceMatrix.getInverse().getPose();
        assertPose(poseInActivitySpace, expectedPose);
    }

    @Test
    public void transformPoseTo_returnsCorrectPose() {
        Matrix4 activitySpaceMatrix =
                Matrix4.fromTrs(
                        new Vector3(4.0f, 5.0f, 6.0f),
                        Quaternion.fromEulerAngles(new Vector3(90f, 0f, 0f)),
                        new Vector3(1.0f, 1.0f, 1.0f));
        getActivitySpaceNode()
                .sendTransformEvent(
                        new FakeNodeTransform(new Mat4f(activitySpaceMatrix.getData())));
        executor.runAll();

        Pose transformedPose =
                perceptionSpaceActivityPose.transformPoseTo(new Pose(), activitySpace);

        Pose expectedPose = activitySpaceMatrix.getInverse().getPose();
        assertPose(transformedPose, expectedPose);
    }

    @Test
    public void transformPoseTo_toScaledEntity_returnsCorrectPose() {
        Matrix4 activitySpaceMatrix =
                Matrix4.fromTrs(
                        new Vector3(4.0f, 5.0f, 6.0f),
                        Quaternion.fromEulerAngles(new Vector3(90f, 0f, 0f)).toNormalized(),
                        new Vector3(1.0f, 1.0f, 1.0f));
        getActivitySpaceNode()
                .sendTransformEvent(
                        new FakeNodeTransform(new Mat4f(activitySpaceMatrix.getData())));
        executor.runAll();
        GltfEntityImpl gltfEntity = createGltfEntity();
        gltfEntity.setScale(new Vector3(2.0f, 2.0f, 2.0f));

        Pose transformedPose = perceptionSpaceActivityPose.transformPoseTo(new Pose(), gltfEntity);

        Pose unscaledPose = activitySpaceMatrix.getInverse().getPose();
        Pose expectedPose =
                new Pose(
                        unscaledPose.getTranslation().times(new Vector3(0.5f, 0.5f, 0.5f)),
                        unscaledPose.getRotation());
        assertPose(transformedPose, expectedPose);
    }

    @Test
    public void getActivitySpaceScale_returnsInverseOfActivitySpaceWorldScale() throws Exception {
        float activitySpaceScale = 5f;
        this.activitySpace.setOpenXrReferenceSpacePose(Matrix4.fromScale(activitySpaceScale));
        assertVector3(
                perceptionSpaceActivityPose.getActivitySpaceScale(),
                new Vector3(1f, 1f, 1f).div(activitySpaceScale));
    }
}
