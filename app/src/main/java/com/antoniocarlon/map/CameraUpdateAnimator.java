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
import androidx.annotation.Nullable;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class CameraUpdateAnimator implements OnCameraIdleListener {

    public static class Animation {

        private final CameraUpdate mCameraUpdate;

        private boolean mAnimate;

        private boolean mClosest;

        private long mDelay;

        private LatLng mTarget;

        public Animation(@NonNull CameraUpdate cameraUpdate) {
            mCameraUpdate = cameraUpdate;
        }

        @NonNull
        public CameraUpdate getCameraUpdate() {
            return mCameraUpdate;
        }

        public long getDelay() {
            return mDelay;
        }

        public void setDelay(long delay) {
            mDelay = delay;
        }

        @Nullable
        public LatLng getTarget() {
            return mTarget;
        }

        public void setTarget(@Nullable LatLng target) {
            mTarget = target;
        }

        public boolean isAnimate() {
            return mAnimate;
        }

        public void setAnimate(boolean animate) {
            mAnimate = animate;
        }

        public boolean isClosest() {
            return mClosest;
        }

        public void setClosest(boolean closest) {
            mClosest = closest;
        }
    }

    private static class CancelableCallback implements GoogleMap.CancelableCallback {

        private Animation mAnimation;

        private AnimatorListener mAnimatorListener;

        CancelableCallback(AnimatorListener animatorListener, Animation animation) {
            mAnimatorListener = animatorListener;
            mAnimation = animation;
        }

        @Override
        public void onCancel() {
            if (mAnimatorListener != null) {
                mAnimatorListener.onAnimationCancel(mAnimation);
            }
            onDestroy();
        }

        @Override
        public void onFinish() {
            if (mAnimatorListener != null) {
                mAnimatorListener.onAnimationFinish(mAnimation);
            }
            onDestroy();
        }

        public void onDestroy() {
            mAnimatorListener = null;
            mAnimation = null;
        }
    }

    public interface AnimatorListener {

        /**
         * <p>Notifies the start of the animation.</p>
         */
        void onAnimationStart();

        /**
         * <p>Notifies the end of the animation.</p>
         */
        void onAnimationEnd();

        /**
         * <p>Notifies the finishing of the animation.</p>
         *
         * @param animation The animation which was finished.
         */
        void onAnimationFinish(@NonNull Animation animation);

        /**
         * <p>Notifies the cancellation of the animation.</p>
         *
         * @param animation The animation which was canceled.
         */
        void onAnimationCancel(@NonNull Animation animation);
    }

    private final ArrayList<Animation> cameraUpdates = new ArrayList<>();

    private final Handler mHandler = new Handler();

    private AnimatorListener mAnimatorListener;

    private boolean mIsRotateGestureEnabled;

    private boolean mIsScrollGestureEnabled;

    private boolean mIsTiltGestureEnabled;

    private boolean mIsZoomControlsEnabled;

    private boolean mIsZoomGestureEnabled;

    private GoogleMap mMap;

    private OnCameraIdleListener mOnCameraIdleListener;

    public static void startAnimation(@NonNull GoogleMap googleMap, @NonNull Animation animation,
                                      @Nullable AnimatorListener animatorListener) {
        if (animation.isAnimate()) {
            if (animation.isClosest()) {
                googleMap.animateCamera(animation.getCameraUpdate(), new CancelableCallback(animatorListener, animation));
            } else {
                googleMap.animateCamera(animation.getCameraUpdate());
            }
        } else {
            googleMap.moveCamera(animation.getCameraUpdate());
        }
    }

    public CameraUpdateAnimator(@NonNull GoogleMap googleMap, @NonNull OnCameraIdleListener onCameraIdleListener) {
        mMap = googleMap;
        mOnCameraIdleListener = onCameraIdleListener;
    }

    public void add(@NonNull Animation animation) {
        cameraUpdates.add(animation);
    }

    public void clear() {
        mHandler.removeCallbacksAndMessages(null);
        cameraUpdates.clear();
    }

    public void execute() {
        if (!cameraUpdates.isEmpty()) {
            setUiSettings();
            onAnimationStart();
            executeNext();
        }
    }

    @Nullable
    public AnimatorListener getAnimatorListener() {
        return mAnimatorListener;
    }

    public void setAnimatorListener(@Nullable AnimatorListener animatorListener) {
        mAnimatorListener = animatorListener;
    }

    @Override
    public void onCameraIdle() {
        executeNext();
    }

    public void onDestroy() {
        clear();
        mAnimatorListener = null;
        mMap = null;
        mOnCameraIdleListener = null;
    }

    private void executeNext() {
        if (cameraUpdates.isEmpty()) {
            onAnimationEnd();
        } else {
            Animation animation = cameraUpdates.remove(0);
            if (animation.getDelay() == 0L) {
                startAnimation(mMap, animation, mAnimatorListener);
            } else {
                mHandler.postDelayed(() -> startAnimation(mMap, animation, mAnimatorListener), animation.getDelay());
            }
        }
    }

    private void onAnimationEnd() {
        mOnCameraIdleListener.onCameraIdle();
        if (mAnimatorListener != null) {
            mAnimatorListener.onAnimationEnd();
        }
        mMap.setOnCameraIdleListener(mOnCameraIdleListener);
        UiSettings settings = mMap.getUiSettings();
        settings.setRotateGesturesEnabled(mIsRotateGestureEnabled);
        settings.setScrollGesturesEnabled(mIsScrollGestureEnabled);
        settings.setTiltGesturesEnabled(mIsTiltGestureEnabled);
        settings.setZoomControlsEnabled(mIsZoomControlsEnabled);
        settings.setZoomGesturesEnabled(mIsZoomGestureEnabled);
    }

    private void onAnimationStart() {
        if (mAnimatorListener != null) {
            mAnimatorListener.onAnimationStart();
        }
        UiSettings settings = mMap.getUiSettings();
        settings.setRotateGesturesEnabled(false);
        settings.setScrollGesturesEnabled(false);
        settings.setTiltGesturesEnabled(false);
        settings.setZoomControlsEnabled(false);
        settings.setZoomGesturesEnabled(false);
        mMap.setOnCameraIdleListener(this);
    }

    private void setUiSettings() {
        UiSettings settings = mMap.getUiSettings();
        mIsRotateGestureEnabled = settings.isRotateGesturesEnabled();
        mIsScrollGestureEnabled = settings.isScrollGesturesEnabled();
        mIsTiltGestureEnabled = settings.isTiltGesturesEnabled();
        mIsZoomControlsEnabled = settings.isZoomControlsEnabled();
        mIsZoomGestureEnabled = settings.isZoomGesturesEnabled();
    }
}
