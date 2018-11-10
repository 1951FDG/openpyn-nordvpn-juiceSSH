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

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.UiSettings;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class CameraUpdateAnimator implements OnCameraIdleListener {
    @SuppressWarnings("WeakerAccess")
    final GoogleMap mMap;
    private final OnCameraIdleListener mOnCameraIdleListener;
    private final List<Animation> cameraUpdates = new ArrayList<>();
    private boolean mIsRotateGestureEnabled;
    private boolean mIsScrollGestureEnabled;
    private boolean mIsTiltGestureEnabled;
    private boolean mIsZoomControlsEnabled;
    private boolean mIsZoomGestureEnabled;

    public CameraUpdateAnimator(@NonNull GoogleMap map,
                                @NonNull OnCameraIdleListener onCameraIdleListener) {
        mMap = map;
        mOnCameraIdleListener = onCameraIdleListener;
    }

    public void add(@NonNull CameraUpdate cameraUpdate, boolean animate, long delay) {
        cameraUpdates.add(new Animation(cameraUpdate, animate, delay, null));
    }

    public void add(@NonNull CameraUpdate cameraUpdate, boolean animate, long delay, @NonNull CancelableCallback cancelableCallback) {
        cameraUpdates.add(new Animation(cameraUpdate, animate, delay, cancelableCallback));
    }

    @SuppressWarnings("unused")
    public void clear() {
        cameraUpdates.clear();
    }

    public void execute() {
        UiSettings settings = mMap.getUiSettings();
        mIsRotateGestureEnabled = settings.isRotateGesturesEnabled();
        mIsScrollGestureEnabled = settings.isScrollGesturesEnabled();
        mIsTiltGestureEnabled = settings.isTiltGesturesEnabled();
        mIsZoomControlsEnabled = settings.isZoomControlsEnabled();
        mIsZoomGestureEnabled = settings.isZoomGesturesEnabled();

        settings.setRotateGesturesEnabled(false);
        settings.setScrollGesturesEnabled(false);
        settings.setTiltGesturesEnabled(false);
        settings.setZoomControlsEnabled(false);
        settings.setZoomGesturesEnabled(false);

        mMap.setOnCameraIdleListener(this);
        executeNext();
    }

    private void executeNext() {
        if (cameraUpdates.isEmpty()) {
            mOnCameraIdleListener.onCameraIdle();
            mMap.setOnCameraIdleListener(mOnCameraIdleListener);
            UiSettings settings = mMap.getUiSettings();
            settings.setRotateGesturesEnabled(mIsRotateGestureEnabled);
            settings.setScrollGesturesEnabled(mIsScrollGestureEnabled);
            settings.setTiltGesturesEnabled(mIsTiltGestureEnabled);
            settings.setZoomControlsEnabled(mIsZoomControlsEnabled);
            settings.setZoomGesturesEnabled(mIsZoomGestureEnabled);
        } else {
            Animation animation = cameraUpdates.remove(0);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (animation.mAnimate) {
                        if (animation.mCancelableCallback != null) {
                            mMap.animateCamera(animation.mCameraUpdate, animation.mCancelableCallback);
                        } else {
                            mMap.animateCamera(animation.mCameraUpdate);
                        }
                    } else {
                        mMap.moveCamera(animation.mCameraUpdate);
                    }
                }
            }, animation.mDelay);
        }
    }

    @Override
    public void onCameraIdle() {
        executeNext();
    }

    private static class Animation {
        final CameraUpdate mCameraUpdate;
        final boolean mAnimate;
        final long mDelay;
        final CancelableCallback mCancelableCallback;

        Animation(CameraUpdate cameraUpdate, boolean animate, long delay, CancelableCallback cancelableCallback) {
            mCameraUpdate = cameraUpdate;
            mAnimate = animate;
            mDelay = delay;
            mCancelableCallback = cancelableCallback;
        }
    }
}
