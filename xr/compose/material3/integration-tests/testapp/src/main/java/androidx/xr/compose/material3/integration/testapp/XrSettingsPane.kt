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

package androidx.xr.compose.material3.integration.testapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities

@Composable
internal fun XrSettingsPane(navSuiteType: MutableState<NavigationSuiteType>) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ListItem(headlineContent = { XrModeButton() })
            ListItem(headlineContent = { NavigationSuiteTypeButton(navSuiteType) })
        }
    }
}

@Composable
private fun XrModeButton() {
    val session = LocalSession.current
    val isDeviceXr = session != null
    val isFullSpaceMode = LocalSpatialCapabilities.current.isSpatialUiEnabled

    Button(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
        enabled = isDeviceXr,
        onClick = {
            if (isFullSpaceMode) {
                session?.requestHomeSpaceMode()
            } else {
                session?.requestFullSpaceMode()
            }
        }
    ) {
        Text(
            text = if (isDeviceXr) "Toggle FullSpace/HomeSpace Mode" else "XR unsupported",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun NavigationSuiteTypeButton(navSuiteType: MutableState<NavigationSuiteType>) {
    Button(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
        onClick = {
            if (navSuiteType.value == NavigationSuiteType.NavigationRail) {
                navSuiteType.value = NavigationSuiteType.NavigationBar
            } else {
                navSuiteType.value = NavigationSuiteType.NavigationRail
            }
        }
    ) {
        Text(text = "Toggle NavRail/NavBar", style = MaterialTheme.typography.bodyLarge)
    }
}
