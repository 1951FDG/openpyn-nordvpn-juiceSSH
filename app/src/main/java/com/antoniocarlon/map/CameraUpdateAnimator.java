/*
 * Copyright 2016 ANTONIO CARLON
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.antoniocarlon.map;

import android.os.Handler;
import androidx.annotation.NonNull;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;
import java.util.List;

public class CameraUpdateAnimator implements GoogleMap.OnCameraIdleListener {
    private final GoogleMap mMap;
    private final GoogleMap.OnCameraIdleListener mOnCameraIdleListener;
    private final List<Animation> cameraUpdates = new ArrayList<>();
    private final boolean mIsRotateGestureEnabled;
    private final boolean mIsScrollGestureEnabled;
    private final boolean mIsTiltGestureEnabled;
    private final boolean mIsZoomControlsEnabled;
    private final boolean mIsZoomGestureEnabled;

    public CameraUpdateAnimator(@NonNull GoogleMap map,
                                @NonNull GoogleMap.OnCameraIdleListener onCameraIdleListener) {
        mMap = map;
        mIsRotateGestureEnabled = mMap.getUiSettings().isRotateGesturesEnabled();
        mIsScrollGestureEnabled = mMap.getUiSettings().isScrollGesturesEnabled();
        mIsTiltGestureEnabled = mMap.getUiSettings().isTiltGesturesEnabled();
        mIsZoomControlsEnabled = mMap.getUiSettings().isZoomControlsEnabled();
        mIsZoomGestureEnabled = mMap.getUiSettings().isZoomGesturesEnabled();

        mOnCameraIdleListener = onCameraIdleListener;
    }

    public void add(@NonNull CameraUpdate cameraUpdate, boolean animate, long delay) {
        cameraUpdates.add(new Animation(cameraUpdate, animate, delay, null));
    }

    public void add(@NonNull CameraUpdate cameraUpdate, boolean animate, long delay, @NonNull GoogleMap.CancelableCallback cancelableCallback) {
        cameraUpdates.add(new Animation(cameraUpdate, animate, delay, cancelableCallback));
    }

    @SuppressWarnings("unused")
    public void clear() {
        cameraUpdates.clear();
    }

    public void execute() {
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        mMap.getUiSettings().setTiltGesturesEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(false);

        mMap.setOnCameraIdleListener(this);
        executeNext();
    }

    private void executeNext() {
        if (!cameraUpdates.isEmpty()) {
            final Animation animation = cameraUpdates.remove(0);

            Handler handler = new android.os.Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (animation.mAnimate) {
                        if (animation.mCancelableCallback != null) {
                            mMap.animateCamera(animation.mCameraUpdate, animation.mCancelableCallback);
                        }
                        else
                        {
                            mMap.animateCamera(animation.mCameraUpdate);
                        }
                    } else {
                        mMap.moveCamera(animation.mCameraUpdate);
                    }
                }
            }, animation.mDelay);
        } else {
            mOnCameraIdleListener.onCameraIdle();
            mMap.setOnCameraIdleListener(mOnCameraIdleListener);
            mMap.getUiSettings().setRotateGesturesEnabled(mIsRotateGestureEnabled);
            mMap.getUiSettings().setScrollGesturesEnabled(mIsScrollGestureEnabled);
            mMap.getUiSettings().setTiltGesturesEnabled(mIsTiltGestureEnabled);
            mMap.getUiSettings().setZoomControlsEnabled(mIsZoomControlsEnabled);
            mMap.getUiSettings().setZoomGesturesEnabled(mIsZoomGestureEnabled);
        }
    }

    @Override
    public void onCameraIdle() {
        executeNext();
    }

    private class Animation {
        private final CameraUpdate mCameraUpdate;
        private final boolean mAnimate;
        private final long mDelay;
        private final GoogleMap.CancelableCallback mCancelableCallback;

        Animation(CameraUpdate cameraUpdate, boolean animate, long delay, GoogleMap.CancelableCallback cancelableCallback) {
            mCameraUpdate = cameraUpdate;
            mAnimate = animate;
            mDelay = delay;
            mCancelableCallback = cancelableCallback;
        }
    }
}
