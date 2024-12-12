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

@file:Suppress("BanConcurrentHashMap", "Deprecation")

package androidx.xr.scenecore

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.xr.arcore.Anchor
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.JxrPlatformAdapter.Entity as RtEntity
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.scenecore.SpatialCapabilities.SpatialCapability
import androidx.xr.scenecore.impl.JxrPlatformAdapterAxr
import com.google.common.util.concurrent.ListenableFuture
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.function.Consumer

/**
 * The Session provides the primary interface to SceneCore functionality for the application. Each
 * spatialized Activity must create and hold an instance of Session.
 *
 * Once created, the application can use the Session interfaces to create spatialized entities, such
 * as Widget panels and geometric models, set the background environment, and anchor content to the
 * real world.
 */
// TODO: Make this class thread safe.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Session(
    public val activity: Activity,
    public val runtime: JxrPlatformAdapter,
    public val spatialEnvironment: SpatialEnvironment,
) {
    internal val entityManager by lazy { EntityManager() }

    private val spatialCapabilitiesListeners:
        ConcurrentMap<Consumer<SpatialCapabilities>, Consumer<RtSpatialCapabilities>> =
        ConcurrentHashMap()

    /**
     * The ActivitySpace is a special entity that represents the space in which the application is
     * launched. It is the default parent of all entities in the scene.
     *
     * The ActivitySpace is created automatically when the Session is created.
     */
    public val activitySpace: ActivitySpace = ActivitySpace.create(runtime, entityManager)

    /** The SpatialUser contains information about the user. */
    public val spatialUser: SpatialUser = SpatialUser.create(runtime)

    /**
     * A spatialized PanelEntity associated with the "main window" for the Activity. When in
     * HomeSpace mode, this is the application's "main window".
     *
     * If called multiple times, this will return the same PanelEntity.
     */
    public val mainPanelEntity: PanelEntity =
        PanelEntity.createMainPanelEntity(runtime, entityManager)

    /**
     * The PerceptionSpace represents the origin of the space in which the ARCore for XR API
     * provides tracking info. The transformations provided by the PerceptionSpace are only valid
     * for the call frame, as the transformation can be changed by the system at any time.
     */
    public val perceptionSpace: PerceptionSpace = PerceptionSpace.create(runtime)

    // TODO: 378706624 - Remove this method once we have a better way to handle the root entity.
    public val activitySpaceRoot: Entity by lazy {
        entityManager.getEntityForRtEntity(runtime.activitySpaceRootImpl)!!
    }

    public companion object {
        private const val TAG = "Session"
        private val activitySessionMap = ConcurrentHashMap<Activity, Session>()

        // TODO: b/323060217 - Move the platformAdapter behind a loader class that loads it in.
        /**
         * Creates a session and pairs it with an Activity and its lifecycle. If a session is
         * already paired with an Activity, return that Session instead of creating a new one.
         *
         * For our Alpha release, we just directly instantiate the Android XR PlatformAdapter.
         */
        // TODO(b/326748782): Change the returned Session here to be nullable or asynchronous.
        // TODO: b/372299691 - Rename the runtime parameter to platformAdapter.
        @JvmStatic
        @JvmOverloads
        public fun create(activity: Activity, runtime: JxrPlatformAdapter? = null): Session {
            // TODO(bhavsar): Rethink moving this check when integration with Spatial Activity
            // happens.
            if (
                !PermissionHelper.hasPermission(
                    activity,
                    PermissionHelper.SCENE_UNDERSTANDING_PERMISSION
                )
            ) {
                PermissionHelper.requestPermission(
                    activity,
                    PermissionHelper.SCENE_UNDERSTANDING_PERMISSION,
                    PermissionHelper.SCENE_UNDERSTANDING_PERMISSION_CODE,
                )
            }
            return activitySessionMap.computeIfAbsent(activity) {
                Log.i(TAG, "Creating session for activity $activity")
                val session =
                    when (runtime) {
                        null -> {
                            val runtimeImpl =
                                JxrPlatformAdapterAxr.create(
                                    activity,
                                    Executors.newSingleThreadScheduledExecutor(
                                        object : ThreadFactory {
                                            override fun newThread(r: Runnable): Thread {
                                                return Thread(r, "JXRCoreSession")
                                            }
                                        }
                                    ),
                                )
                            Session(activity, runtimeImpl, SpatialEnvironment(runtimeImpl))
                        }
                        else -> Session(activity, runtime, SpatialEnvironment(runtime))
                    }
                activity.registerActivityLifecycleCallbacks(
                    object : ActivityLifecycleCallbacks {
                        override fun onActivityCreated(
                            activity: Activity,
                            savedInstanceState: Bundle?
                        ) {}

                        override fun onActivityStarted(activity: Activity) {}

                        override fun onActivityResumed(activity: Activity) {
                            session.runtime.startRenderer()
                        }

                        override fun onActivityPaused(activity: Activity) {
                            session.runtime.stopRenderer()
                        }

                        override fun onActivityStopped(activity: Activity) {}

                        override fun onActivitySaveInstanceState(
                            activity: Activity,
                            outState: Bundle
                        ) {}

                        override fun onActivityDestroyed(activity: Activity) {
                            activitySessionMap.remove(activity)
                            session.entityManager.clear()
                            session.runtime.dispose()
                        }
                    }
                )
                session
            }
        }
    }

    /**
     * Returns true if the Session is currently capable of the given [SpatialCapability], false
     * otherwise. The available set of capabilities can change within a session.
     */
    @Deprecated(message = "Removing in favor of getSpatialCapabilities().hasCapability()")
    // TODO: b/366214441 - Remove this method
    public fun hasSpatialCapability(@SpatialCapability capability: Int): Boolean =
        getSpatialCapabilities().hasCapability(capability)

    /**
     * Returns the current [SpatialCapabilities] of the Session. The set of capabilities can change
     * within a session. The returned object will not update if the capabilities change; this method
     * should be called again to get the latest set of capabilities.
     */
    public fun getSpatialCapabilities(): SpatialCapabilities =
        runtime.spatialCapabilities.toSpatialCapabilities()

    /**
     * Adds the given [Consumer] as a listener to be invoked when this Session's current
     * [SpatialCapabilities] change. [Consumer#accept(SpatialCapabilities)] will be invoked on the
     * main thread.
     */
    public fun addSpatialCapabilitiesChangedListener(
        listener: Consumer<SpatialCapabilities>
    ): Unit = addSpatialCapabilitiesChangedListener(HandlerExecutor.mainThreadExecutor, listener)

    /**
     * Adds the given [Consumer] as a listener to be invoked when this Session's current
     * [SpatialCapabilities] change. [Consumer#accept(SpatialCapabilities)] will be invoked on the
     * given callbackExecutor, or the main thread if the callbackExecutor is null (default).
     */
    public fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    ): Unit {
        // wrap the client's listener in a callback that receives & converts the runtime
        // SpatialCapabilities type.
        val rtListener: Consumer<RtSpatialCapabilities> =
            Consumer<RtSpatialCapabilities> { rtCaps: RtSpatialCapabilities ->
                listener.accept(rtCaps.toSpatialCapabilities())
            }
        spatialCapabilitiesListeners.compute(
            listener,
            { _, _ ->
                runtime.addSpatialCapabilitiesChangedListener(callbackExecutor, rtListener)
                rtListener
            },
        )
    }

    /**
     * Releases the given [Consumer] from receiving updates when the Session's [SpatialCapabilities]
     * change.
     */
    @Suppress("PairedRegistration") // The corresponding remove method does not accept an Executor
    public fun removeSpatialCapabilitiesChangedListener(
        listener: Consumer<SpatialCapabilities>
    ): Unit {
        spatialCapabilitiesListeners.computeIfPresent(
            listener,
            { _, rtListener ->
                runtime.removeSpatialCapabilitiesChangedListener(rtListener)
                null
            },
        )
    }

    /**
     * If the primary Activity for this Session has focus, causes it to be placed in FullSpace Mode.
     * Otherwise, this call does nothing.
     */
    public fun requestFullSpaceMode(): Unit = runtime.requestFullSpaceMode()

    /**
     * If the primary Activity for this Session has focus, causes it to be placed in HomeSpace Mode.
     * Otherwise, this call does nothing.
     */
    public fun requestHomeSpaceMode(): Unit = runtime.requestHomeSpaceMode()

    /**
     * Public factory function for a [GltfModel], where the glTF is asynchronously loaded.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * Currently, only URLs and relative paths from the android_assets/ directory are supported.
     * Currently, only binary glTF (.glb) files are supported.
     *
     * @param name The URL or asset-relative path of a binary glTF (.glb) model to be loaded
     * @return a ListenableFuture<GltfModel>. Listeners will be called on the main thread if
     *   Runnable::run is supplied.
     */
    @MainThread
    public fun createGltfResourceAsync(name: String): ListenableFuture<GltfModel> {
        return GltfModel.createAsync(runtime, name)
    }

    /**
     * Public factory function for an EXRImage, where the EXR is loaded from a local file.
     *
     * @param name The path for an EXR image to be loaded
     * @return an EXRImage instance.
     */
    public fun createExrImageResource(name: String): ExrImage = ExrImage.create(runtime, name)

    /**
     * Public factory function for a [GltfModelEntity].
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param model The [GltfModel] this Entity is referencing.
     * @param pose The initial pose of the entity.
     * @return a GltfModelEntity instance
     */
    // TODO: b/341372472 - Rename createGltfEntity to createGltfModelEntity
    @JvmOverloads
    @MainThread
    public fun createGltfEntity(model: GltfModel, pose: Pose = Pose.Identity): GltfModelEntity =
        GltfModelEntity.create(runtime, entityManager, model, pose)

    /**
     * Public factory function for a StereoSurfaceEntity.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param stereoMode Stereo mode for the surface.
     * @param dimensions Dimensions for the surface.
     * @param pose Pose of this entity relative to its parent, default value is Identity.
     * @return a StereoSurfaceEntity instance
     */
    @MainThread
    @JvmOverloads
    public fun createStereoSurfaceEntity(
        @StereoSurfaceEntity.StereoModeValue
        stereoMode: Int = StereoSurfaceEntity.StereoMode.SIDE_BY_SIDE,
        dimensions: Dimensions = Dimensions(1.0f, 1.0f, 1.0f),
        pose: Pose = Pose.Identity,
    ): StereoSurfaceEntity {
        return StereoSurfaceEntity.create(runtime, entityManager, stereoMode, dimensions, pose)
    }

    // TODO(b/352629832): Update surfaceDimensionsPx to be a PixelDimensions
    /**
     * Public factory function for a spatialized PanelEntity.
     *
     * @param view View to embed in this panel entity.
     * @param surfaceDimensionsPx Dimensions for the underlying surface for the given view.
     * @param dimensions Dimensions for the panel in meters.
     * @param name Name of the panel.
     * @param pose Pose of this entity relative to its parent, default value is Identity.
     * @return a PanelEntity instance.
     */
    @JvmOverloads
    public fun createPanelEntity(
        view: View,
        surfaceDimensionsPx: Dimensions,
        dimensions: Dimensions,
        name: String,
        pose: Pose = Pose.Identity,
    ): PanelEntity =
        PanelEntity.create(
            runtime,
            entityManager,
            view,
            surfaceDimensionsPx,
            dimensions,
            name,
            activity,
            pose,
        )

    /** Helper function to query if given activity can be a host to ActivityPanel. */
    @Deprecated(
        message = "Use getSpatialCapabilities.hasCapabilility(SPATIAL_CAPABILITY_EMBED_ACTIVITY)"
    )
    public fun canEmbedActivityPanel(@Suppress("UNUSED_PARAMETER") activity: Activity): Boolean =
        getSpatialCapabilities()
            .hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)

    // TODO(b/352629832): Update windowBoundsPx to be a PixelDimensions
    /**
     * Public factory function for a spatial ActivityPanelEntity.
     *
     * @param windowBoundsPx Bounds for the panel window in pixels.
     * @param name Name of the panel.
     * @param pose Pose of this entity relative to its parent, default value is Identity.
     * @return an ActivityPanelEntity instance.
     */
    @JvmOverloads
    public fun createActivityPanelEntity(
        windowBoundsPx: Rect,
        name: String,
        pose: Pose = Pose.Identity,
    ): ActivityPanelEntity =
        ActivityPanelEntity.create(
            runtime,
            entityManager,
            PixelDimensions(windowBoundsPx.width(), windowBoundsPx.height()),
            name,
            activity,
            pose,
        )

    /**
     * Public factory function for an AnchorEntity which searches for a location to create an Anchor
     * among the tracked planes available to the perception system.
     *
     * Note that this function will fail if the application has not been granted the
     * "android.permission.SCENE_UNDERSTANDING" permission. Consider using PermissionHelper to help
     * request permission from the User.
     *
     * @param bounds Bounds for this AnchorEntity.
     * @param planeType Orientation of plane to which this Anchor should attach.
     * @param planeSemantic Semantics of the plane to which this Anchor should attach.
     * @param timeout The amount of time as a [Duration] to search for the a suitable plane to
     *   attach to. If a plane is not found within the timeout, the returned AnchorEntity state will
     *   be set to AnchorEntity.State.TIMEDOUT. It may take longer than the timeout period before
     *   the anchor state is updated. If the timeout duration is zero it will search for the anchor
     *   indefinitely.
     */
    @JvmOverloads
    public fun createAnchorEntity(
        bounds: Dimensions,
        planeType: @PlaneTypeValue Int,
        planeSemantic: @PlaneSemanticValue Int,
        timeout: Duration = Duration.ZERO,
    ): AnchorEntity {
        return AnchorEntity.create(
            runtime,
            entityManager,
            bounds,
            planeType,
            planeSemantic,
            timeout
        )
    }

    /**
     * Public factory function for an AnchorEntity which uses an Anchor from ARCore for XR.
     *
     * @param anchor The PerceptionAnchor to use for this AnchorEntity.
     */
    public fun createAnchorEntity(anchor: Anchor): AnchorEntity {
        return AnchorEntity.create(runtime, entityManager, anchor)
    }

    /**
     * Unpersists an anchor. It will clean up the data in the storage that is required to retrieve
     * the anchor.
     *
     * @param uuid UUID of the anchor to unpersist.
     */
    public fun unpersistAnchor(uuid: UUID): Boolean {
        return runtime.unpersistAnchor(uuid)
    }

    /**
     * Public factory function for creating a content-less entity. This entity is used as a
     * connection point for attaching children entities and managing them (i.e. setPose()) as a
     * group.
     *
     * @param name Name of the entity.
     * @param pose Initial pose of the entity.
     */
    @JvmOverloads
    public fun createEntity(name: String, pose: Pose = Pose.Identity): Entity =
        ContentlessEntity.create(runtime, entityManager, name, pose)

    /**
     * Public factory function for a persisted AnchorEntity using UUID. Note that the system keeps a
     * limited number of scenes and anchors. If the anchor is pruned due to the system limitation,
     * the creation will fail. It will return null if there is a failure.
     *
     * @param uuid The UUID of the persisted anchor to recreate.
     * @return a persisted AnchorEntity instance.
     */
    public fun createPersistedAnchorEntity(uuid: UUID): AnchorEntity {
        return AnchorEntity.create(runtime, entityManager, uuid)
    }

    /**
     * Public factory for creating an [InteractableComponent]. It enables access to raw input
     * events.
     *
     * @param executor Executor for invoking [InputEventListener].
     * @param inputEventListener [InputEventListener] that accepts [InputEvent]s.
     * @return [InteractableComponent] instance.
     */
    @Suppress("ExecutorRegistration")
    public fun createInteractableComponent(
        executor: Executor,
        inputEventListener: InputEventListener,
    ): InteractableComponent =
        InteractableComponent.create(runtime, entityManager, executor, inputEventListener)

    /**
     * Public factory function for creating a MovableComponent. This component can be attached to a
     * single instance of any non-Anchor Entity.
     *
     * When attached, this Component will enable the user to translate the Entity by pointing and
     * dragging on it.
     *
     * @param systemMovable A [Boolean] which causes the system to automatically apply transform
     *   updates to the entity in response to user interaction.
     * @param scaleInZ A [Boolean] which tells the system to update the scale of the Entity as the
     *   user moves it closer and further away. This is mostly useful for Panel auto-rescaling with
     *   Distance
     * @param anchorPlacement A Set containing different [AnchorPlacement] for how to anchor the
     *   [Entity] movable component. If this is not empty the movement semantics will be slightly
     *   different from the system as it will add the ability to anchor to nearby planes.
     * @param shouldDisposeParentAnchor A [Boolean], which if set to true, when an entity is moved
     *   off of an [AnchorEntity] that was created by the underlying [MovableComponent], and the
     *   [AnchorEntity] has no other children, the AnchorEntity will be disposed, and the underlying
     *   Anchor will be detached.
     * @return [MovableComponent] instance.
     */
    @JvmOverloads
    public fun createMovableComponent(
        systemMovable: Boolean = true,
        scaleInZ: Boolean = true,
        anchorPlacement: Set<AnchorPlacement> = emptySet(),
        shouldDisposeParentAnchor: Boolean = true,
    ): MovableComponent =
        MovableComponent.create(
            runtime = runtime,
            entityManager = entityManager,
            systemMovable = systemMovable,
            scaleInZ = scaleInZ,
            anchorPlacement = anchorPlacement,
            shouldDisposeParentAnchor = shouldDisposeParentAnchor,
        )

    /**
     * Public factory function for creating a ResizableComponent. This component can be attached to
     * a single instance of any non-Anchor Entity.
     *
     * When attached, this Component will enable the user to resize the Entity by dragging along the
     * boundaries of the interaction highlight.
     *
     * @param minimumSize A lower bound for the User's resize actions, in meters. This value is used
     *   to set constraints on how small the user can resize the bounding box of the entity down to.
     *   The size of the content inside that bounding box is fully controlled by the application.
     *   The default value for this param is 0 meters.
     * @param maximumSize An upper bound for the User's resize actions, in meters. This value is
     *   used to set constraints on how large the user can resize the bounding box of the entity up
     *   to. The size of the content inside that bounding box is fully controlled by the
     *   application. The default value for this param is 10 meters.
     * @return [ResizableComponent] instance.
     */
    @JvmOverloads
    public fun createResizableComponent(
        minimumSize: Dimensions = ResizableComponent.kMinimumSize,
        maximumSize: Dimensions = ResizableComponent.kMaximumSize,
    ): ResizableComponent = ResizableComponent.create(runtime, minimumSize, maximumSize)

    /**
     * Sets the full space mode flag to the given [android.os.Bundle].
     *
     * The [android.os.Bundle] then could be used to launch an [android.app.Activity] with
     * requesting to enter full space mode through [android.app.Activity.startActivity]. If there's
     * a bundle used for customizing how the [android.app.Activity] should be started by
     * [android.app.ActivityOptions.toBundle] or [androidx.core.app.ActivityOptionsCompat.toBundle],
     * it's suggested to use the bundle to call this method.
     *
     * The flag will be ignored when no [android.content.Intent.FLAG_ACTIVITY_NEW_TASK] is set in
     * the bundle, or it is not started from a focused Activity context.
     *
     * This flag is also ignored when the [android.window.PROPERTY_XR_ACTIVITY_START_MODE] property
     * is set to a value other than [XR_ACTIVITY_START_MODE_UNDEFINED] in the AndroidManifest.xml
     * file for the activity being launched.
     *
     * @param bundle the input bundle to set with the full space mode flag.
     * @return the input bundle with the full space mode flag set.
     */
    public fun setFullSpaceMode(bundle: Bundle): Bundle = runtime.setFullSpaceMode(bundle)

    /**
     * Sets the inherit full space mode environvment flag to the given [android.os.Bundle].
     *
     * The [android.os.Bundle] then could be used to launch an [android.app.Activity] with
     * requesting to enter full space mode while inherit the existing environment through
     * [android.app.Activity.startActivity]. If there's a bundle used for customizing how the
     * [android.app.Activity] should be started by [android.app.ActivityOptions.toBundle] or
     * [androidx.core.app.ActivityOptionsCompat.toBundle], it's suggested to use the bundle to call
     * this method.
     *
     * When launched, the activity will be in full space mode and also inherits the environment from
     * the launching activity. If the inherited environment needs to be animated, the launching
     * activity has to continue updating the environment even after the activity is put into the
     * stopped state.
     *
     * The flag will be ignored when no [android.content.Intent.FLAG_ACTIVITY_NEW_TASK] is set in
     * the intent, or it is not started from a focused Activity context.
     *
     * The flag will also be ignored when there is no environment to inherit or the activity has its
     * own environment set already.
     *
     * This flag is ignored too when the [android.window.PROPERTY_XR_ACTIVITY_START_MODE] property
     * is set to a value other than [XR_ACTIVITY_START_MODE_UNDEFINED] in the AndroidManifest.xml
     * file for the activity being launched.
     *
     * For security reasons, Z testing for the new activity is disabled, and the activity is always
     * drawn on top of the inherited environment. Because Z testing is disabled, the activity should
     * not spatialize itself, and should not curve its panel too much either.
     *
     * @param bundle the input bundle to set with the inherit full space mode environment flag.
     * @return the input bundle with the inherit full space mode flag set.
     */
    public fun setFullSpaceModeWithEnvironmentInherited(bundle: Bundle): Bundle =
        runtime.setFullSpaceModeWithEnvironmentInherited(bundle)

    /**
     * Sets a preferred main panel aspect ratio for home space mode.
     *
     * The ratio is only applied to the activity. If the activity launches another activity in the
     * same task, the ratio is not applied to the new activity. Also, while the activity is in full
     * space mode, the preference is temporarily removed.
     *
     * @param activity the activity to set the preference.
     * @param preferredRatio the aspect ratio determined by taking the panel's width over its
     *   height. A value <= 0.0f means there are no preferences.
     */
    public fun setPreferredAspectRatio(activity: Activity, preferredRatio: Float): Unit =
        runtime.setPreferredAspectRatio(activity, preferredRatio)

    /**
     * Returns all [Entity]s of the given type or its subtypes.
     *
     * @param type the type of [Entity] to return.
     * @return a list of all [Entity]s of the given type.
     */
    public fun <T : Entity> getEntitiesOfType(type: Class<out T>): List<T> =
        entityManager.getEntitiesOfType(type)

    internal fun getEntityForRtEntity(entity: RtEntity): Entity? {
        return entityManager.getEntityForRtEntity(entity)
    }
}
