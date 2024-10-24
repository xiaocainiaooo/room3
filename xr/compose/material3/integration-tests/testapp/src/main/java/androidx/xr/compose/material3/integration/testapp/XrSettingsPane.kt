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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.XrMode
import androidx.xr.compose.platform.currentOrNull

@Composable
internal fun XrSettingsPane() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ListItem(headlineContent = { XrModeCheckbox() })
        }
    }
}

@Composable
private fun XrModeCheckbox() {
    val session = LocalSession.currentOrNull
    val isDeviceXr = session != null

    val contentColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDeviceXr) 1f else 0.38f)

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        val isFullSpaceMode = XrMode.isSpatialUiEnabled
        Row(
            Modifier.fillMaxWidth()
                .height(56.dp)
                .toggleable(
                    value = isFullSpaceMode,
                    onValueChange = { newState ->
                        if (newState) {
                            session?.requestFullSpaceMode()
                        } else {
                            session?.requestHomeSpaceMode()
                        }
                    },
                    role = Role.Checkbox,
                    enabled = isDeviceXr,
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isFullSpaceMode,
                onCheckedChange = null, // null recommended for accessibility with screenreaders
                enabled = isDeviceXr,
            )
            Text(
                text = if (isDeviceXr) "Enable Fullspace Mode" else "XR unsupported",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}
