/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.ui.client

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.IRemoteSharedUiSessionClient
import androidx.privacysandbox.ui.core.IRemoteSharedUiSessionController
import androidx.privacysandbox.ui.core.ISharedUiAdapter
import androidx.privacysandbox.ui.core.RemoteCallManager.addBinderDeathListener
import androidx.privacysandbox.ui.core.RemoteCallManager.closeRemoteSession
import androidx.privacysandbox.ui.core.RemoteCallManager.tryToCallRemoteObject
import androidx.privacysandbox.ui.core.SharedUiAdapter
import java.util.concurrent.Executor

/**
 * Provides an implementation of [SharedUiAdapter] created from a supplied Bundle which acts as a
 * proxy between the host app and the Binder provided by the UI provider.
 */
@SuppressLint("NullAnnotationGroup")
@ExperimentalFeatures.SharedUiPresentationApi
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
object SharedUiAdapterFactory {

    // Bundle key is a binary compatibility requirement
    private const val SHARED_UI_ADAPTER_BINDER = "sharedUiAdapterBinder"

    /**
     * Creates a [SharedUiAdapter] from a supplied [coreLibInfo] that acts as a proxy between the
     * host app and the Binder provided by the UI provider.
     *
     * @throws IllegalArgumentException if `coreLibInfo` does not contain a Binder corresponding to
     *   [SharedUiAdapter]
     */
    // TODO(b/365553832): add shim support to generate client proxy.
    @SuppressLint("NullAnnotationGroup")
    @ExperimentalFeatures.SharedUiPresentationApi
    fun createFromCoreLibInfo(coreLibInfo: Bundle): SharedUiAdapter {
        val uiAdapterBinder =
            requireNotNull(coreLibInfo.getBinder(SHARED_UI_ADAPTER_BINDER)) {
                "Invalid bundle, missing $SHARED_UI_ADAPTER_BINDER."
            }
        val adapterInterface = ISharedUiAdapter.Stub.asInterface(uiAdapterBinder)

        return RemoteAdapter(adapterInterface)
    }

    /**
     * [RemoteAdapter] maintains a shared session with a UI provider living in a different process.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private class RemoteAdapter(private val adapterInterface: ISharedUiAdapter) : SharedUiAdapter {
        override fun openSession(clientExecutor: Executor, client: SharedUiAdapter.SessionClient) {
            tryToCallRemoteObject(adapterInterface) {
                this.openRemoteSession(RemoteSharedUiSessionClient(client, clientExecutor))
            }
        }

        class RemoteSharedUiSessionClient(
            val client: SharedUiAdapter.SessionClient,
            val clientExecutor: Executor
        ) : IRemoteSharedUiSessionClient.Stub() {
            override fun onRemoteSessionOpened(
                remoteSessionController: IRemoteSharedUiSessionController
            ) {
                clientExecutor.execute {
                    client.onSessionOpened(SessionImpl(remoteSessionController))
                }
                addBinderDeathListener(remoteSessionController) {
                    onRemoteSessionError("Remote process died")
                }
            }

            override fun onRemoteSessionError(errorString: String) {
                clientExecutor.execute { client.onSessionError(Throwable(errorString)) }
            }

            private class SessionImpl(
                val remoteSessionController: IRemoteSharedUiSessionController
            ) : SharedUiAdapter.Session {
                override fun close() {
                    closeRemoteSession(remoteSessionController)
                }
            }
        }
    }
}
