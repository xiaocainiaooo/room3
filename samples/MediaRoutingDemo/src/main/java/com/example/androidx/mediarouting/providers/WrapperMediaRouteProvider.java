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

package com.example.androidx.mediarouting.providers;

import android.content.Context;

import androidx.mediarouter.media.MediaRouteProvider;

import org.jspecify.annotations.NonNull;

public class WrapperMediaRouteProvider extends MediaRouteProvider {
    private static final String TAG = "WrapperMrp";

    /**
     * A custom media control intent category for special requests that are supported by this
     * provider's routes.
     */
    public static final String CATEGORY_WRAPPER_ROUTE =
            "com.example.androidx.media.CATEGORY_WRAPPER_ROUTE";

    /**
     * Creates a media route provider to provide wrapper routes.
     *
     * @param context the context
     */
    public WrapperMediaRouteProvider(@NonNull Context context) {
        super(context);
    }
}
