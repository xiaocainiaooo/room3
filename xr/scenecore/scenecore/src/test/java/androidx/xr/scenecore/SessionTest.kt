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

@file:Suppress("UNUSED_VARIABLE")

package androidx.xr.scenecore

import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import android.widget.TextView
import androidx.xr.scenecore.JxrPlatformAdapter.GltfModelResource
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.UUID
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the JXRCore SDK Session Interface.
 *
 * TODO(b/329902726): Add a TestRuntime and verify CPM Integration.
 */
@RunWith(RobolectricTestRunner::class)
class SessionTest {
    private val activityController = Robolectric.buildActivity(Activity::class.java)
    private val activity = activityController.create().start().get()
    private val mockRuntime = mock<JxrPlatformAdapter>()
    private val mockAnchorEntity = mock<JxrPlatformAdapter.AnchorEntity>()
    lateinit var session: Session

    @Before
    fun setUp() {
        whenever(mockRuntime.spatialEnvironment).thenReturn(mock())
        val mockActivitySpace = mock<JxrPlatformAdapter.ActivitySpace>()
        whenever(mockRuntime.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockRuntime.headActivityPose).thenReturn(mock())
        whenever(mockRuntime.activitySpaceRootImpl).thenReturn(mockActivitySpace)
        whenever(mockRuntime.mainPanelEntity).thenReturn(mock())
        whenever(mockRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockAnchorEntity.state)
            .thenReturn(JxrPlatformAdapter.AnchorEntity.State.UNANCHORED)
        whenever(mockAnchorEntity.persistState)
            .thenReturn(JxrPlatformAdapter.AnchorEntity.PersistState.PERSIST_NOT_REQUESTED)
        session = Session.create(activity, mockRuntime)
    }

    @Test
    fun requestFullSpaceMode_callsThrough() {
        session.requestFullSpaceMode()
        verify(mockRuntime).requestFullSpaceMode()
    }

    @Test
    fun requestHomeSpaceMode_callsThrough() {
        session.requestHomeSpaceMode()
        verify(mockRuntime).requestHomeSpaceMode()
    }

    @Test
    fun createSession_runtimeProvided_createsSessionWithProvidedRuntime() {
        assertThat(session).isNotNull()
        assertThat(session.runtime).isNotNull()
        assertThat(session.runtime).isEqualTo(mockRuntime)
    }

    @Test
    fun createSession_twiceFromSameActivity_returnsSameInstance() {
        val newSession = Session.create(activity, mockRuntime)
        assertThat(session).isEqualTo(newSession)
    }

    @Test
    fun createSession_differentActivities_returnsUniqueInstances() {
        val newActivity = Robolectric.buildActivity(Activity::class.java).create().start().get()
        val newSession = Session.create(newActivity, mockRuntime)
        assertThat(session).isNotEqualTo(newSession)
    }

    @Test
    fun createGltfResourceAsync_callsRuntimeLoadGltf() {
        val mockGltfModelResource = mock<GltfModelResource>()
        whenever(mockRuntime.loadGltfByAssetNameSplitEngine(anyString()))
            .thenReturn(Futures.immediateFuture(mockGltfModelResource))
        val unused = session.createGltfResourceAsync("test.glb")

        verify(mockRuntime).loadGltfByAssetNameSplitEngine("test.glb")
    }

    @Test
    fun createGltfEntity_callsRuntimeCreateGltfEntity() {
        whenever(mockRuntime.loadGltfByAssetNameSplitEngine(anyString()))
            .thenReturn(Futures.immediateFuture(mock()))
        whenever(mockRuntime.createGltfEntity(any(), any(), any())).thenReturn(mock())
        val gltfModelFuture = session.createGltfResourceAsync("test.glb")
        val unused = session.createGltfEntity(gltfModelFuture.get())

        verify(mockRuntime).loadGltfByAssetNameSplitEngine(eq("test.glb"))
        verify(mockRuntime).createGltfEntity(any(), any(), any())
    }

    @Test
    fun createPanelEntity_callsRuntimeCreatePanelEntity() {
        val view = TextView(activity)
        whenever(mockRuntime.createPanelEntity(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(mock())
        val unused =
            session.createPanelEntity(
                view,
                Dimensions(720f, 480f),
                Dimensions(0.1f, 0.1f, 0.1f),
                "test"
            )

        verify(mockRuntime).createPanelEntity(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun createAnchorEntity_callsRuntimeCreateAnchorEntity() {
        whenever(mockRuntime.createAnchorEntity(any(), any(), any(), anyOrNull()))
            .thenReturn(mockAnchorEntity)
        val unused = session.createAnchorEntity(Dimensions(), PlaneType.ANY, PlaneSemantic.ANY)

        verify(mockRuntime).createAnchorEntity(any(), any(), any(), anyOrNull())
    }

    @Test
    fun getActivitySpace_returnsActivitySpace() {
        val activitySpace = session.activitySpace

        assertThat(activitySpace).isNotNull()
    }

    @Test
    fun getActivitySpaceTwice_returnsSameSpace() {
        val activitySpace1 = session.activitySpace
        val activitySpace2 = session.activitySpace

        assertThat(activitySpace1).isEqualTo(activitySpace2)
    }

    @Test
    fun getActivitySpaceRoot_returnsActivitySpaceRoot() {
        val activitySpaceRoot = session.activitySpaceRoot

        assertThat(activitySpaceRoot).isNotNull()
    }

    @Test
    fun getActivitySpaceRootTwice_returnsSameSpace() {
        val activitySpaceRoot1 = session.activitySpaceRoot
        val activitySpaceRoot2 = session.activitySpaceRoot

        assertThat(activitySpaceRoot1).isEqualTo(activitySpaceRoot2)
    }

    @Test
    fun getSpatialUser_returnsSpatialUser() {
        val spatialUser = session.spatialUser

        assertThat(spatialUser).isNotNull()
    }

    @Test
    fun getSpatialUserTwice_returnsSameUser() {
        val spatialUser1 = session.spatialUser
        val spatialUser2 = session.spatialUser

        assertThat(spatialUser1).isEqualTo(spatialUser2)
    }

    @Test
    fun getPerceptionSpace_returnPerceptionSpace() {
        val perceptionSpace = session.perceptionSpace

        assertThat(perceptionSpace).isNotNull()
    }

    @Test
    fun createActivityPanelEntity_callsRuntimeCreateActivityPanelEntity() {
        whenever(mockRuntime.createActivityPanelEntity(any(), any(), any(), any(), any()))
            .thenReturn(mock())
        val unused = session.createActivityPanelEntity(Rect(0, 0, 640, 480), "test")

        verify(mockRuntime).createActivityPanelEntity(any(), any(), any(), any(), any())
    }

    @Test
    fun getMainPanelEntity_returnsPanelEntity() {
        val unused = session.mainPanelEntity
        val unusedAgain = session.mainPanelEntity

        verify(mockRuntime, times(1)).mainPanelEntity
    }

    @Test
    fun createPersistedAnchorEntity_callsRuntimecreatePersistedAnchorEntity() {
        whenever(mockRuntime.createPersistedAnchorEntity(any(), any())).thenReturn(mockAnchorEntity)
        val unused = session.createPersistedAnchorEntity(UUID.randomUUID())

        verify(mockRuntime).createPersistedAnchorEntity(any(), any())
    }

    @Test
    fun unpersistAnchor_callsRuntimeunpersistAnchor_returnsTrue() {
        val uuid = UUID.randomUUID()
        whenever(mockRuntime.unpersistAnchor(uuid)).thenReturn(true)
        assertThat(session.unpersistAnchor(uuid)).isTrue()
        verify(mockRuntime).unpersistAnchor(uuid)
    }

    fun unpersistAnchor_callsRuntimeunpersistAnchor_returnsFalse() {
        val uuid = UUID.randomUUID()
        whenever(mockRuntime.unpersistAnchor(uuid)).thenReturn(false)
        assertThat(session.unpersistAnchor(uuid)).isFalse()
        verify(mockRuntime).unpersistAnchor(uuid)
    }

    @Test
    fun createInteractableComponent_callsRuntimeCreateInteractableComponent() {
        whenever(mockRuntime.createInteractableComponent(any(), any())).thenReturn(mock())

        val interactableComponent = session.createInteractableComponent(directExecutor(), mock())
        val view = TextView(activity)
        val mockPanelEntity = mock<JxrPlatformAdapter.PanelEntity>()
        whenever(mockRuntime.createPanelEntity(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(mockPanelEntity)
        whenever(mockPanelEntity.addComponent(any())).thenReturn(true)
        val panelEntity =
            session.createPanelEntity(
                view,
                Dimensions(720f, 480f),
                Dimensions(0.1f, 0.1f, 0.1f),
                "test"
            )
        assertThat(panelEntity.addComponent(interactableComponent)).isTrue()

        verify(mockRuntime).createInteractableComponent(any(), anyOrNull())
    }

    @Test
    fun createMovableComponent_callsRuntimeCreateMovableComponent() {
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())

        val movableComponent = session.createMovableComponent()
        val view = TextView(activity)
        val mockRtPanelEntity = mock<JxrPlatformAdapter.PanelEntity>()
        whenever(mockRuntime.createPanelEntity(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(mockRtPanelEntity)
        whenever(mockRtPanelEntity.addComponent(any())).thenReturn(true)
        val panelEntity =
            session.createPanelEntity(
                view,
                Dimensions(720f, 480f),
                Dimensions(0.1f, 0.1f, 0.1f),
                "test"
            )
        assertThat(panelEntity.addComponent(movableComponent)).isTrue()

        verify(mockRuntime).createMovableComponent(any(), any(), any(), any())
    }

    @Test
    fun createResizableComponent_callsRuntimeCreateResizableComponent() {
        whenever(mockRuntime.createResizableComponent(any(), any())).thenReturn(mock())

        val resizableComponent = session.createResizableComponent()
        val view = TextView(activity)
        val mockRtPanelEntity = mock<JxrPlatformAdapter.PanelEntity>()
        whenever(mockRtPanelEntity.getSize()).thenReturn(JxrPlatformAdapter.Dimensions(1f, 1f, 1f))
        whenever(mockRuntime.createPanelEntity(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(mockRtPanelEntity)
        whenever(mockRtPanelEntity.addComponent(any())).thenReturn(true)
        val panelEntity =
            session.createPanelEntity(
                view,
                Dimensions(720f, 480f),
                Dimensions(0.1f, 0.1f, 0.1f),
                "test"
            )
        assertThat(panelEntity.addComponent(resizableComponent)).isTrue()

        verify(mockRuntime).createResizableComponent(any(), any())
    }

    @Test
    fun setFullSpaceMode_callsThrough() {
        // Test that Session calls into the runtime.
        val bundle = Bundle().apply { putString("testkey", "testval") }
        whenever(mockRuntime.setFullSpaceMode(any())).thenReturn(bundle)
        val unused = session.setFullSpaceMode(bundle)
        verify(mockRuntime).setFullSpaceMode(bundle)
    }

    @Test
    fun setFullSpaceModeWithEnvironmentInherited_callsThrough() {
        // Test that Session calls into the runtime.
        val bundle = Bundle().apply { putString("testkey", "testval") }
        whenever(mockRuntime.setFullSpaceModeWithEnvironmentInherited(any())).thenReturn(bundle)
        val unused = session.setFullSpaceModeWithEnvironmentInherited(bundle)
        verify(mockRuntime).setFullSpaceModeWithEnvironmentInherited(bundle)
    }

    @Test
    fun setPreferredAspectRatio_callsThrough() {
        // Test that Session calls into the runtime.
        session.setPreferredAspectRatio(activity, 1.23f)
        verify(mockRuntime).setPreferredAspectRatio(activity, 1.23f)
    }

    @Test
    fun getPanelEntityType_returnsAllPanelEntities() {
        val mockPanelEntity1 = mock<JxrPlatformAdapter.PanelEntity>()
        val mockActivityPanelEntity = mock<JxrPlatformAdapter.ActivityPanelEntity>()
        whenever(mockRuntime.createPanelEntity(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(mockPanelEntity1)
        whenever(mockRuntime.createActivityPanelEntity(any(), any(), any(), any(), any()))
            .thenReturn(mockActivityPanelEntity)
        val panelEntity =
            session.createPanelEntity(
                TextView(activity),
                Dimensions(720f, 480f),
                Dimensions(0.1f, 0.1f, 0.1f),
                "test1",
            )
        val activityPanelEntity = session.createActivityPanelEntity(Rect(0, 0, 640, 480), "test2")

        assertThat(session.getEntitiesOfType(PanelEntity::class.java))
            .containsAtLeast(panelEntity, activityPanelEntity)
    }

    @Test
    fun getEntitiesBaseType_returnsAllEntities() {
        val mockPanelEntity = mock<JxrPlatformAdapter.PanelEntity>()
        whenever(mockRuntime.createPanelEntity(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(mockPanelEntity)
        whenever(mockRuntime.createAnchorEntity(any(), any(), any(), any()))
            .thenReturn(mockAnchorEntity)
        val panelEntity =
            session.createPanelEntity(
                TextView(activity),
                Dimensions(720f, 480f),
                Dimensions(0.1f, 0.1f, 0.1f),
                "test1",
            )
        val anchorEntity =
            session.createAnchorEntity(Dimensions(), PlaneType.ANY, PlaneSemantic.ANY)

        assertThat(session.getEntitiesOfType(Entity::class.java))
            .containsAtLeast(panelEntity, anchorEntity)
    }

    @Test
    fun addAndRemoveSpatialCapabilitiesChangedListener_callsRuntimeAddAndRemove() {
        val listener = Consumer<SpatialCapabilities> { _ -> }
        session.addSpatialCapabilitiesChangedListener(listener = listener)
        verify(mockRuntime).addSpatialCapabilitiesChangedListener(any(), any())
        session.removeSpatialCapabilitiesChangedListener(listener)
        verify(mockRuntime).removeSpatialCapabilitiesChangedListener(any())
    }

    @Test
    fun onDestroy_callsRuntimeDispose() {
        activityController.destroy()
        verify(mockRuntime).dispose()
    }
}
