/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.viewfinder;

import android.graphics.Bitmap;
import android.os.Handler;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest;
import androidx.camera.viewfinder.core.impl.RefCounted;
import androidx.camera.viewfinder.internal.utils.Logger;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;

import kotlin.Unit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * The SurfaceView implementation for {@link CameraViewfinder}.
 */
final class SurfaceViewImplementation extends ViewfinderImplementation {

    private static final String TAG = "SurfaceViewImpl";

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    @Nullable
    SurfaceView mSurfaceView;

    // Target SurfaceRequest. Only complete the SurfaceResponse when the size of the Surface
    // matches this request.
    // Guarded by the UI thread.
    private ViewfinderSurfaceRequest mSurfaceRequest = null;

    // SurfaceRequest to check when the target size is met.
    // Guarded by the UI thread.
    private CallbackToFutureAdapter.Completer<RefCounted<Surface>> mSurfaceResponse = null;

    // Surface that is active between the surfaceCreated and surfaceDestroyed callbacks.
    // Guarded by the UI thread.
    private RefCounted<Surface> mActiveSurface = null;

    // The cached size of the current Surface.
    // Guarded by the UI thread.
    private int mCurrentSurfaceWidth = -1;
    private int mCurrentSurfaceHeight = -1;

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    final SurfaceHolder.@NonNull Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
            Logger.d(TAG, "Surface created.");
            // No-op. Handling surfaceChanged() is enough because it's always called afterwards.
            mActiveSurface = new RefCounted<>(false, (surface) -> {
                surface.release();
                return Unit.INSTANCE;
            });
            mActiveSurface.initialize(surfaceHolder.getSurface());
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width,
                int height) {
            Logger.d(TAG, "Surface changed. Size: " + width + "x" + height);
            mCurrentSurfaceWidth = width;
            mCurrentSurfaceHeight = height;
            tryToComplete();
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            Logger.d(TAG, "Surface destroyed.");

            // If a surface was already provided to the camera, invalidate it so that it requests
            // a new valid one. Otherwise, cancel the surface request.
            if (mActiveSurface != null) {
                invalidateSurface();
                mActiveSurface.release();
            } else {
                cancelPreviousRequest();
            }

            // Reset state
            mActiveSurface = null;
            mCurrentSurfaceWidth = -1;
            mCurrentSurfaceHeight = -1;
        }
    };

    SurfaceViewImplementation(@NonNull FrameLayout parent,
            @NonNull ViewfinderTransformation viewfinderTransformation) {
        super(parent, viewfinderTransformation);
    }

    void initializeViewfinder(int width, int height) {
        Preconditions.checkNotNull(mParent);
        Preconditions.checkArgument(width > 0);
        Preconditions.checkArgument(height > 0);
        mSurfaceView = new SurfaceView(mParent.getContext());
        mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
        mParent.removeAllViews();
        mParent.addView(mSurfaceView);
        mSurfaceView.getHolder().addCallback(mSurfaceHolderCallback);
    }

    @Override
    void onSurfaceRequested(@NonNull ViewfinderSurfaceRequest surfaceRequest,
            CallbackToFutureAdapter.Completer<RefCounted<Surface>> surfaceResponse) {
        // Cancel the previous request, if any
        cancelPreviousRequest();

        initializeViewfinder(surfaceRequest.getWidth(), surfaceRequest.getHeight());

        mSurfaceRequest = surfaceRequest;
        mSurfaceResponse = surfaceResponse;
        mSurfaceResponse.addCancellationListener(
                () -> {
                    if (Objects.equals(mSurfaceResponse, surfaceResponse)) {
                        // Clean up the request
                        cancelPreviousRequest();
                    }
                },
                ContextCompat.getMainExecutor(
                        Objects.requireNonNull(mSurfaceView).getContext())
        );

        if (!tryToComplete()) {
            // The current size is incorrect. Wait for it to change.
            Logger.d(TAG, "Wait for new Surface creation.");
            Objects.requireNonNull(mSurfaceView).getHolder().setFixedSize(
                    mSurfaceRequest.getWidth(),
                    mSurfaceRequest.getHeight()
            );
        }
    }

    @Override
    void onImplementationReplaced() {
        cancelPreviousRequest();
    }

    /**
     * Getting a Bitmap from a Surface is achieved using the `PixelCopy#request()` API, which
     * would introduced in API level 24. CameraViewfinder doesn't currently use a SurfaceView on API
     * levels below 24.
     */
    @RequiresApi(24)
    @Override
    @Nullable
    Bitmap getViewfinderBitmap() {
        // If the viewfinder surface isn't ready yet or isn't valid, return null
        if (mSurfaceView == null || mSurfaceView.getHolder().getSurface() == null
                || !mSurfaceView.getHolder().getSurface().isValid()) {
            return null;
        }

        // Copy display contents of the surfaceView's surface into a Bitmap.
        final Bitmap bitmap = Bitmap.createBitmap(mSurfaceView.getWidth(), mSurfaceView.getHeight(),
                Bitmap.Config.ARGB_8888);
        Api24Impl.pixelCopyRequest(mSurfaceView, bitmap, copyResult -> {
            if (copyResult == PixelCopy.SUCCESS) {
                Logger.d(TAG,
                        "CameraViewfinder.SurfaceViewImplementation.getBitmap() succeeded");
            } else {
                Logger.e(TAG,
                        "CameraViewfinder.SurfaceViewImplementation.getBitmap() failed with error "
                                + copyResult);
            }
        }, mSurfaceView.getHandler());

        return bitmap;
    }

    @Override
    @Nullable
    View getViewfinder() {
        return mSurfaceView;
    }

    /**
     * Sets the completer if size matches.
     *
     * @return true if the completer is set.
     */
    @UiThread
    private boolean tryToComplete() {
        if (mSurfaceView == null || mSurfaceResponse == null) {
            return false;
        }
        if (canProvideSurface()) {
            Logger.d(TAG, "Surface set on viewfinder.");

            if (mSurfaceResponse.set(mActiveSurface)) {
                onSurfaceProvided();
                return true;
            }

            mSurfaceRequest = null;
            mSurfaceResponse = null;
        }
        return false;
    }

    private boolean canProvideSurface() {
        return mActiveSurface != null
                && mSurfaceRequest != null
                && mSurfaceRequest.getWidth() == mCurrentSurfaceWidth
                && mSurfaceRequest.getHeight() == mCurrentSurfaceHeight;
    }

    @UiThread
    private void cancelPreviousRequest() {
        if (mSurfaceRequest != null) {
            Logger.d(TAG, "Request canceled: " + mSurfaceRequest);
            mSurfaceRequest = null;
            mSurfaceResponse.setCancelled();
            mSurfaceResponse = null;
        }
    }

    @UiThread
    private void invalidateSurface() {
        if (mSurfaceRequest != null) {
            Logger.d(TAG, "Surface invalidated: " + mSurfaceRequest);
            // TODO(b/323226220): Differentiate between surface being released by consumer
            //  vs producer
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 7.0 (API 24).
     */
    @RequiresApi(24)
    private static class Api24Impl {

        private Api24Impl() {
        }

        static void pixelCopyRequest(@NonNull SurfaceView source, @NonNull Bitmap dest,
                PixelCopy.@NonNull OnPixelCopyFinishedListener listener, @NonNull Handler handler) {
            PixelCopy.request(source, dest, listener, handler);
        }
    }
}

