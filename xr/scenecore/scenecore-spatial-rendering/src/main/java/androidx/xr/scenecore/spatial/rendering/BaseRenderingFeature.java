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

package androidx.xr.scenecore.spatial.rendering;

import androidx.xr.runtime.NodeHolder;
import androidx.xr.scenecore.internal.RenderingFeature;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;

import org.jspecify.annotations.NonNull;

abstract class BaseRenderingFeature implements RenderingFeature {
    public final Node mNode;
    BaseRenderingFeature(@NonNull XrExtensions extensions) {
        mNode = extensions.createNode();
    }

    @Override
    @NonNull
    public NodeHolder<?> getNodeHolder() {
        return new NodeHolder<>(mNode, Node.class);
    }
}
