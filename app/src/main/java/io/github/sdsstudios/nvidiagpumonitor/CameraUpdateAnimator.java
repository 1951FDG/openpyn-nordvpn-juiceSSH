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

package io.github.sdsstudios.nvidiagpumonitor;

import android.os.Handler;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;
import java.util.List;

public class CameraUpdateAnimator implements GoogleMap.OnCameraIdleListener {
    private final GoogleMap mMap;
    private final GoogleMap.OnCameraIdleListener mOnCameraIdleListener;
    private List<Animation> cameraUpdates = new ArrayList<>();
    private boolean mIsRotateGestureEnabled;
    private boolean mIsScrollGestureEnabled;
    private boolean mIsTiltGestureEnabled;
    private boolean mIsZoomGestureEnabled;

    public CameraUpdateAnimator(GoogleMap map,
                                GoogleMap.OnCameraIdleListener onCameraIdleListener) {
        mMap = map;
        mIsRotateGestureEnabled = mMap.getUiSettings().isRotateGesturesEnabled();
        mIsScrollGestureEnabled = mMap.getUiSettings().isScrollGesturesEnabled();
        mIsTiltGestureEnabled = mMap.getUiSettings().isTiltGesturesEnabled();
        mIsZoomGestureEnabled = mMap.getUiSettings().isZoomGesturesEnabled();

        mOnCameraIdleListener = onCameraIdleListener;
    }

    public void add(CameraUpdate cameraUpdate, boolean animate, long delay) {
        if (cameraUpdate != null) {
            cameraUpdates.add(new Animation(cameraUpdate, animate, delay));
        }
    }

    public void clear() {
        cameraUpdates.clear();
    }

    public void execute() {
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        mMap.getUiSettings().setTiltGesturesEnabled(false);
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
                        mMap.animateCamera(animation.mCameraUpdate);
                    } else {
                        mMap.moveCamera(animation.mCameraUpdate);
                    }
                }
            }, animation.mDelay);
        } else {
            mMap.setOnCameraIdleListener(mOnCameraIdleListener);
            mMap.getUiSettings().setRotateGesturesEnabled(mIsRotateGestureEnabled);
            mMap.getUiSettings().setScrollGesturesEnabled(mIsScrollGestureEnabled);
            mMap.getUiSettings().setTiltGesturesEnabled(mIsTiltGestureEnabled);
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

        public Animation(CameraUpdate cameraUpdate, boolean animate, long delay) {
            mCameraUpdate = cameraUpdate;
            mAnimate = animate;
            mDelay = delay;
        }
    }
}
