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
import java.util.Collection;

public class CameraUpdateAnimator implements OnCameraIdleListener {

    private final ArrayList<Animation> mCameraUpdates;

    private final CancelableCallback mCancelableCallback = new CancelableCallback();

    private final Handler mHandler = new Handler();

    private final GoogleMap mMap;

    private final OnCameraIdleListener mOnCameraIdleListener;

    private AnimatorListener mAnimatorListener;

    private boolean mIsRotateGestureEnabled;

    private boolean mIsScrollGestureEnabled;

    private boolean mIsTiltGestureEnabled;

    private boolean mIsZoomControlsEnabled;

    private boolean mIsZoomGestureEnabled;

    private boolean mAnimating;

    public CameraUpdateAnimator(@NonNull GoogleMap googleMap, @NonNull OnCameraIdleListener onCameraIdleListener) {
        this(googleMap, new ArrayList<>(), onCameraIdleListener);
    }

    public CameraUpdateAnimator(@NonNull GoogleMap googleMap, @NonNull ArrayList<Animation> cameraUpdates,
                                @NonNull OnCameraIdleListener onCameraIdleListener) {
        mMap = googleMap;
        mCameraUpdates = cameraUpdates;
        mOnCameraIdleListener = onCameraIdleListener;
    }

    private static void startAnimation(@NonNull GoogleMap googleMap, @NonNull Animation animation,
                                       @Nullable GoogleMap.CancelableCallback cancelableCallback) {
        if (animation.isAnimate()) {
            if (cancelableCallback != null) {
                googleMap.animateCamera(animation.getCameraUpdate(), cancelableCallback);
            } else {
                googleMap.animateCamera(animation.getCameraUpdate());
            }
        } else {
            googleMap.moveCamera(animation.getCameraUpdate());
        }
    }

    @Override
    public void onCameraIdle() {
        executeNext();
    }

    public boolean add(@NonNull Animation animation) {
        return mCameraUpdates.add(animation);
    }

    public boolean addAll(@NonNull Collection<? extends Animation> animations) {
        return mCameraUpdates.addAll(animations);
    }

    public void clear() {
        mCameraUpdates.clear();
    }

    public void execute() {
        if (!mCameraUpdates.isEmpty()) {
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

    public boolean isAnimating() {
        return mAnimating;
    }

    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
    }

    private void executeNext() {
        if (mCameraUpdates.isEmpty()) {
            onAnimationEnd();
        } else {
            Animation animation = mCameraUpdates.remove(0);
            if (animation.getCallback()) {
                mCancelableCallback.setAnimation(animation);
                mCancelableCallback.setAnimatorListener(mAnimatorListener);
                if (animation.getDelay() == 0L) {
                    startAnimation(mMap, animation, mCancelableCallback);
                } else {
                    mHandler.postDelayed(() -> startAnimation(mMap, animation, mCancelableCallback), animation.getDelay());
                }
            } else {
                if (animation.getDelay() == 0L) {
                    startAnimation(mMap, animation, null);
                } else {
                    mHandler.postDelayed(() -> startAnimation(mMap, animation, null), animation.getDelay());
                }
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
        mAnimating = false;
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
        mAnimating = true;
    }

    private void setUiSettings() {
        UiSettings settings = mMap.getUiSettings();
        mIsRotateGestureEnabled = settings.isRotateGesturesEnabled();
        mIsScrollGestureEnabled = settings.isScrollGesturesEnabled();
        mIsTiltGestureEnabled = settings.isTiltGesturesEnabled();
        mIsZoomControlsEnabled = settings.isZoomControlsEnabled();
        mIsZoomGestureEnabled = settings.isZoomGesturesEnabled();
    }

    @SuppressWarnings("PublicInnerClass")
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

    @SuppressWarnings("PublicInnerClass")
    public static class Animation {

        private final CameraUpdate mCameraUpdate;

        private boolean mAnimate;

        private boolean mClosest;

        private boolean mCallback;

        private long mDelay;

        private LatLng mTarget;

        private Object mTag;

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
        public Object getTag() {
            return mTag;
        }

        public void setTag(@Nullable Object tag) {
            mTag = tag;
        }

        public boolean getCallback() {
            return mCallback;
        }

        public void setCallback(boolean callback) {
            mCallback = callback;
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

        CancelableCallback() {
        }

        @Override
        public void onCancel() {
            if (mAnimatorListener != null) {
                mAnimatorListener.onAnimationCancel(mAnimation);
            }
        }

        @Override
        public void onFinish() {
            if (mAnimatorListener != null) {
                mAnimatorListener.onAnimationFinish(mAnimation);
            }
        }

        public void setAnimation(Animation animation) {
            mAnimation = animation;
        }

        public void setAnimatorListener(AnimatorListener animatorListener) {
            mAnimatorListener = animatorListener;
        }
    }
}
