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

// TODO(b/289518597): Remove this SuppressLint
@file:SuppressLint("NullAnnotationGroup")
@file:OptIn(
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3XrApi::class
)

package androidx.xr.compose.material3.integration.testapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.xr.compose.material3.EnableXrComponentOverrides
import androidx.xr.compose.material3.ExperimentalMaterial3XrApi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EnableXrComponentOverrides { Content() } }
    }
}

private val navSuiteType = mutableStateOf(NavigationSuiteType.NavigationRail)

@Composable
private fun Content() {
    var navSuiteSelectedItem by remember { mutableStateOf(NavSuiteItem.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            NavSuiteItem.values().forEach { item ->
                item(
                    selected = navSuiteSelectedItem == item,
                    onClick = { navSuiteSelectedItem = item },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                )
            }
        },
        layoutType = navSuiteType.value
    ) {
        when (navSuiteSelectedItem) {
            NavSuiteItem.HOME -> {
                Home()
            }
            NavSuiteItem.SETTINGS -> {
                XrSettingsPane(navSuiteType)
            }
        }
    }
}

@Composable
private fun Home() {
    val navigator: ThreePaneScaffoldNavigator<Destination> =
        rememberListDetailPaneScaffoldNavigator(
            initialDestinationHistory =
                listOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List))
        )
    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = { AnimatedPane { ListPane(navigator) } },
        detailPane = { AnimatedPane { DetailPane(navigator) } },
    )
}

private const val TAG = "MainActivity"
