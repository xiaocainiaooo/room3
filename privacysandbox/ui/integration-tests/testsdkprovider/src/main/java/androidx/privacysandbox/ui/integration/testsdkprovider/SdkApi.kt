/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.ui.integration.testsdkprovider

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import androidx.privacysandbox.ui.core.DelegatingSandboxedUiAdapter
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.integration.sdkproviderutils.PlayerViewProvider
import androidx.privacysandbox.ui.integration.sdkproviderutils.PlayerViewabilityHandler
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.integration.sdkproviderutils.TestAdapters
import androidx.privacysandbox.ui.integration.sdkproviderutils.ViewabilityHandler
import androidx.privacysandbox.ui.integration.testaidl.IMediateeSdkApi
import androidx.privacysandbox.ui.integration.testaidl.ISdkApi
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SdkApi(private val sdkContext: Context) : ISdkApi.Stub() {
    private val testAdapters = TestAdapters(sdkContext)
    private val handler = Handler(Looper.getMainLooper())

    override fun loadBannerAd(
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        waitInsideOnDraw: Boolean,
        drawViewability: Boolean
    ): Bundle {
        val isMediation = mediationOption != MediationOption.NON_MEDIATED
        val isAppOwnedMediation = (mediationOption == MediationOption.IN_APP_MEDIATEE)
        if (isMediation) {
            return loadMediatedTestAd(
                    isAppOwnedMediation,
                    adType,
                    waitInsideOnDraw,
                    drawViewability
                )
                .toCoreLibInfo(sdkContext)
        }
        val adapter: SandboxedUiAdapter =
            when (adType) {
                AdType.BASIC_NON_WEBVIEW -> {
                    loadNonWebViewBannerAd("Simple Ad", waitInsideOnDraw)
                }
                AdType.BASIC_WEBVIEW -> {
                    loadWebViewBannerAd()
                }
                AdType.WEBVIEW_FROM_LOCAL_ASSETS -> {
                    loadWebViewBannerAdFromLocalAssets()
                }
                AdType.NON_WEBVIEW_VIDEO -> loadVideoAd()
                else -> {
                    loadNonWebViewBannerAd("Ad type not present", waitInsideOnDraw)
                }
            }.also { ViewabilityHandler.addObserverFactoryToAdapter(it, drawViewability) }
        return adapter.toCoreLibInfo(sdkContext)
    }

    @OptIn(ExperimentalFeatures.DelegatingAdapterApi::class)
    private fun startDelegatingAdUpdateHandler(
        adapter: DelegatingSandboxedUiAdapter,
        drawViewability: Boolean
    ) {
        val updateInterval = UPDATE_DELEGATE_INTERVAL

        val displayAdFromRuntimeMediatee = Runnable {
            val coroutineScope = MainScope()
            coroutineScope.launch {
                val runtimeAdapterBundle =
                    maybeGetMediateeBannerAdBundle(
                        false,
                        AdType.BASIC_NON_WEBVIEW,
                        false,
                        drawViewability
                    )
                adapter.updateDelegate(runtimeAdapterBundle)
            }
        }
        val displayAdFromAppOwnedMediatee = Runnable {
            val coroutineScope = MainScope()
            coroutineScope.launch {
                val inAppAdapterBundle =
                    maybeGetMediateeBannerAdBundle(
                        true,
                        AdType.BASIC_NON_WEBVIEW,
                        false,
                        drawViewability
                    )
                adapter.updateDelegate(inAppAdapterBundle)
            }
        }
        // Post events to update the delegate after certain intervals
        handler.postDelayed(displayAdFromRuntimeMediatee, updateInterval)
        // race condition
        handler.postDelayed(displayAdFromRuntimeMediatee, 2 * updateInterval)
        handler.postDelayed(displayAdFromAppOwnedMediatee, 2 * updateInterval)

        handler.postDelayed(displayAdFromRuntimeMediatee, 4 * updateInterval)
    }

    /** Kill sandbox process */
    override fun triggerProcessDeath() {
        Process.killProcess(Process.myPid())
    }

    private fun loadWebViewBannerAd(): SandboxedUiAdapter {
        return testAdapters.WebViewBannerAd()
    }

    private fun loadWebViewBannerAdFromLocalAssets(): SandboxedUiAdapter {
        return testAdapters.WebViewAdFromLocalAssets()
    }

    private fun loadNonWebViewBannerAd(
        text: String,
        waitInsideOnDraw: Boolean
    ): SandboxedUiAdapter {
        return testAdapters.TestBannerAd(text, waitInsideOnDraw)
    }

    private fun loadVideoAd(): SandboxedUiAdapter {
        val playerViewProvider = PlayerViewProvider()
        val adapter = testAdapters.VideoBannerAd(playerViewProvider)
        PlayerViewabilityHandler.addObserverFactoryToAdapter(adapter, playerViewProvider)
        return adapter
    }

    @OptIn(ExperimentalFeatures.DelegatingAdapterApi::class)
    private fun loadMediatedTestAd(
        isAppMediatee: Boolean,
        @AdType adType: Int,
        waitInsideOnDraw: Boolean,
        drawViewability: Boolean
    ): SandboxedUiAdapter {
        // TODO(b/350473804): Clean up mediatee flag - redundant after introducing Delegating
        // adapters
        val mediateeBannerAdBundle =
            maybeGetMediateeBannerAdBundle(isAppMediatee, adType, waitInsideOnDraw, drawViewability)
        val bannerAd = DelegatingSandboxedUiAdapter(mediateeBannerAdBundle)
        // The ad will keep refreshing between different mediatees
        startDelegatingAdUpdateHandler(bannerAd, drawViewability)
        return bannerAd
    }

    override fun requestResize(width: Int, height: Int) {}

    private fun maybeGetMediateeBannerAdBundle(
        isAppMediatee: Boolean,
        adType: Int,
        withSlowDraw: Boolean,
        drawViewability: Boolean
    ): Bundle {
        val sdkSandboxControllerCompat = SdkSandboxControllerCompat.from(sdkContext)
        if (isAppMediatee) {
            val appOwnedSdkSandboxInterfaces =
                sdkSandboxControllerCompat.getAppOwnedSdkSandboxInterfaces()
            appOwnedSdkSandboxInterfaces.forEach { appOwnedSdkSandboxInterfaceCompat ->
                if (appOwnedSdkSandboxInterfaceCompat.getName() == MEDIATEE_SDK) {
                    val appOwnedMediateeSdkApi =
                        IMediateeSdkApi.Stub.asInterface(
                            appOwnedSdkSandboxInterfaceCompat.getInterface()
                        )
                    return appOwnedMediateeSdkApi.loadBannerAd(
                        adType,
                        withSlowDraw,
                        drawViewability
                    )
                }
            }
        } else {
            val sandboxedSdks = sdkSandboxControllerCompat.getSandboxedSdks()
            sandboxedSdks.forEach { sandboxedSdkCompat ->
                if (sandboxedSdkCompat.getSdkInfo()?.name == MEDIATEE_SDK) {
                    val mediateeSdkApi =
                        IMediateeSdkApi.Stub.asInterface(sandboxedSdkCompat.getInterface())
                    return mediateeSdkApi.loadBannerAd(adType, withSlowDraw, drawViewability)
                }
            }
        }
        // Show a non-mediated ad if no mediatee can be found.
        return testAdapters
            .TestBannerAd("Mediated SDK is not loaded, this is a mediator Ad!", true)
            .toCoreLibInfo(sdkContext)
    }

    companion object {
        private const val MEDIATEE_SDK =
            "androidx.privacysandbox.ui.integration.mediateesdkprovider"
        private const val UPDATE_DELEGATE_INTERVAL: Long = 5000L
    }
}
