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
import android.os.Bundle
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.xr.scenecore.JxrPlatformAdapter.Entity as RtEntity
import androidx.xr.scenecore.impl.JxrPlatformAdapterAxr
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

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
    public val platformAdapter: JxrPlatformAdapter,
    public val spatialEnvironment: SpatialEnvironment,
) {
    internal val entityManager by lazy { EntityManager() }

    /**
     * The ActivitySpace is a special entity that represents the space in which the application is
     * launched. It is the default parent of all entities in the scene.
     *
     * The ActivitySpace is created automatically when the Session is created.
     */
    public val activitySpace: ActivitySpace = ActivitySpace.create(platformAdapter, entityManager)

    /** The SpatialUser contains information about the user. */
    public val spatialUser: SpatialUser = SpatialUser.create(platformAdapter)

    /**
     * A spatialized PanelEntity associated with the "main window" for the Activity. When in
     * HomeSpace mode, this is the application's "main window".
     *
     * If called multiple times, this will return the same PanelEntity.
     */
    public val mainPanelEntity: PanelEntity =
        PanelEntity.createMainPanelEntity(platformAdapter, entityManager)

    /**
     * The PerceptionSpace represents the origin of the space in which the ARCore for XR API
     * provides tracking info. The transformations provided by the PerceptionSpace are only valid
     * for the call frame, as the transformation can be changed by the system at any time.
     */
    public val perceptionSpace: PerceptionSpace = PerceptionSpace.create(platformAdapter)

    // TODO: 378706624 - Remove this method once we have a better way to handle the root entity.
    public val activitySpaceRoot: Entity by lazy {
        entityManager.getEntityForRtEntity(platformAdapter.activitySpaceRootImpl)!!
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
        @JvmStatic
        @JvmOverloads
        public fun create(
            activity: Activity,
            platformAdapter: JxrPlatformAdapter? = null
        ): Session {
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
                    when (platformAdapter) {
                        null -> {
                            val platformAdapterImpl =
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
                            Session(
                                activity,
                                platformAdapterImpl,
                                SpatialEnvironment(platformAdapterImpl)
                            )
                        }
                        else ->
                            Session(activity, platformAdapter, SpatialEnvironment(platformAdapter))
                    }
                activity.registerActivityLifecycleCallbacks(
                    object : ActivityLifecycleCallbacks {
                        override fun onActivityCreated(
                            activity: Activity,
                            savedInstanceState: Bundle?
                        ) {}

                        override fun onActivityStarted(activity: Activity) {}

                        override fun onActivityResumed(activity: Activity) {
                            session.platformAdapter.startRenderer()
                        }

                        override fun onActivityPaused(activity: Activity) {
                            session.platformAdapter.stopRenderer()
                        }

                        override fun onActivityStopped(activity: Activity) {}

                        override fun onActivitySaveInstanceState(
                            activity: Activity,
                            outState: Bundle
                        ) {}

                        override fun onActivityDestroyed(activity: Activity) {
                            activitySessionMap.remove(activity)
                            session.entityManager.clear()
                            session.platformAdapter.dispose()
                        }
                    }
                )
                session
            }
        }
    }

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
    public fun setFullSpaceMode(bundle: Bundle): Bundle = platformAdapter.setFullSpaceMode(bundle)

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
     * not spatialize itself.
     *
     * @param bundle the input bundle to set with the inherit full space mode environment flag.
     * @return the input bundle with the inherit full space mode flag set.
     */
    public fun setFullSpaceModeWithEnvironmentInherited(bundle: Bundle): Bundle =
        platformAdapter.setFullSpaceModeWithEnvironmentInherited(bundle)

    /**
     * Sets a preferred main panel aspect ratio for home space mode.
     *
     * The ratio is only applied to the activity. If the activity launches another activity in the
     * same task, the ratio is not applied to the new activity. Also, while the activity is in full
     * space mode, the preference is temporarily removed.
     *
     * If the activity's current aspect ratio differs from the preferredRatio, the panel is
     * automatically resized. This resizing preserves the panel's area. To avoid runtime resizing,
     * consider specifying the desired aspect ratio in your AndroidManifest.xml. This ensures your
     * activity launches with the preferred aspect ratio from the start.
     *
     * @param activity the activity to set the preference.
     * @param preferredRatio the aspect ratio determined by taking the panel's width over its
     *   height. A value <= 0.0f means there are no preferences.
     */
    public fun setPreferredAspectRatio(activity: Activity, preferredRatio: Float): Unit =
        platformAdapter.setPreferredAspectRatio(activity, preferredRatio)

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
