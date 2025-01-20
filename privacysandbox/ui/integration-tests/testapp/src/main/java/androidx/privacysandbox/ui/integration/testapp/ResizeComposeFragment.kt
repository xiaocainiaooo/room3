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

package androidx.privacysandbox.ui.integration.testapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkUi
import androidx.privacysandbox.ui.client.view.SandboxedSdkViewEventListener
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ResizeComposeFragment : BaseFragment() {

    private var adapter: SandboxedUiAdapter? by mutableStateOf(null)
    private var adEventText by mutableStateOf("")
    private var bannerDimension by mutableStateOf(BannerDimension())
    private val onBannerDimensionChanged: (BannerDimension) -> Unit = { currentDimension ->
        val displayMetrics = resources.displayMetrics
        val maxSizePixels = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels)
        val newSize = { currentSize: Int, maxSize: Int ->
            (currentSize + (100..200).random()) % maxSize
        }
        val newWidth = newSize(currentDimension.width.value.toInt(), maxSizePixels).dp
        val newHeight = newSize(currentDimension.height.value.toInt(), maxSizePixels).dp
        bannerDimension = BannerDimension(newWidth, newHeight)
    }

    override fun handleLoadAdFromDrawer(
        adType: Int,
        mediationOption: Int,
        drawViewabilityLayer: Boolean
    ) {
        currentAdType = adType
        currentMediationOption = mediationOption
        shouldDrawViewabilityLayer = drawViewabilityLayer
        setAdAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setAdAdapter()
        return ComposeView(requireContext()).apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { ResizeableBannerAd(adapter, bannerDimension, onBannerDimensionChanged) }
        }
    }

    @Composable
    fun ResizeableBannerAd(
        adapter: SandboxedUiAdapter?,
        bannerDimension: BannerDimension,
        onResizeClicked: (BannerDimension) -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            val sandboxedSdkUiModifier =
                if (bannerDimension.height != 0.dp && bannerDimension.width != 0.dp) {
                    Modifier.width(bannerDimension.width)
                        .weight(
                            1f,
                        )
                } else {
                    Modifier.fillMaxWidth().weight(1f)
                }

            Text("Ad state: $adEventText")
            if (adapter != null) {
                SandboxedSdkUi(
                    adapter,
                    sandboxedSdkUiModifier,
                    providerUiOnTop = providerUiOnTop,
                    sandboxedSdkViewEventListener =
                        object : SandboxedSdkViewEventListener {
                            override fun onUiDisplayed() {
                                adEventText = "Ad is visible"
                            }

                            override fun onUiError(error: Throwable) {
                                adEventText = "Error loading ad : ${error.message}"
                            }

                            override fun onUiClosed() {
                                adEventText = "Ad session is closed"
                            }
                        },
                )
            }
            Button(onClick = { onResizeClicked(bannerDimension) }) { Text("Resize") }
        }
    }

    private fun setAdAdapter() {
        val coroutineScope = MainScope()
        coroutineScope.launch {
            adapter =
                SandboxedUiAdapterFactory.createFromCoreLibInfo(
                    getSdkApi()
                        .loadBannerAd(
                            currentAdType,
                            currentMediationOption,
                            false,
                            shouldDrawViewabilityLayer,
                        )
                )
        }
    }

    data class BannerDimension(val width: Dp = 0.dp, val height: Dp = 0.dp)
}
