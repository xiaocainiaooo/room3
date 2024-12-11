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

package androidx.wear.tiles.samples.tile

import android.content.Context
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.CardColors
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.prop
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.material3.titleCard
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

private const val RESOURCES_VERSION = "0"

/** Base playground tile service for testing out features. */
class PlaygroundTileService : TileService() {
    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> = resources()

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> = tile(requestParams, this)
}

private fun resources() =
    Futures.immediateFuture(
        ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
    )

private fun tile(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
): ListenableFuture<TileBuilders.Tile> =
    Futures.immediateFuture(
        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(tileLayout(requestParams, context))
            )
            .build()
    )

private fun tileLayout(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
): LayoutElementBuilders.LayoutElement =
    materialScope(context = context, deviceConfiguration = requestParams.deviceConfiguration) {
        primaryLayout(
            mainSlot = {
                titleCard(
                    onClick = EMPTY_LOAD_CLICKABLE,
                    contentDescription = "Sample Card".prop(),
                    colors =
                        CardColors(
                            background = colorScheme.tertiary,
                            title = colorScheme.onTertiary,
                            content = colorScheme.onTertiary,
                            time = colorScheme.onTertiary
                        ),
                    height = expand(),
                    title = {
                        text(
                            "Title Card!".prop(),
                            maxLines = 1,
                        )
                    },
                    content = {
                        text(
                            "Hello and welcome Tiles in AndroidX!".prop(),
                            maxLines = 2,
                        )
                    },
                    time = {
                        text(
                            "NOW".prop(),
                        )
                    }
                )
            },
            bottomSlot = {
                textEdgeButton(
                    onClick = EMPTY_LOAD_CLICKABLE,
                    contentDescription = "EdgeButton".prop(),
                ) {
                    text("Edge".prop())
                }
            }
        )
    }
