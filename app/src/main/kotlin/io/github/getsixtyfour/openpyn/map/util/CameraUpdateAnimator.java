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

package io.github.getsixtyfour.openpyn.map.util;

import android.annotation.SuppressLint;
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

public final class CameraUpdateAnimator implements OnCameraIdleListener {

    private final ArrayList<Animation> mCameraUpdates;

    private final CancelableCallback mCancelableCallback = new CancelableCallback();

    private final Handler mHandler = new Handler();

    private GoogleMap mGoogleMap;

    private OnCameraIdleListener mOnCameraIdleListener;

    private boolean mAnimating;

    @Nullable
    private AnimatorListener mAnimatorListener;

    private boolean mIsRotateGestureEnabled;

    private boolean mIsScrollGestureEnabled;

    private boolean mIsTiltGestureEnabled;

    private boolean mIsZoomControlsEnabled;

    private boolean mIsZoomGestureEnabled;

    public CameraUpdateAnimator(@NonNull GoogleMap googleMap, @NonNull OnCameraIdleListener onCameraIdleListener) {
        this(googleMap, onCameraIdleListener, new ArrayList<>());
    }

    @SuppressLint("LambdaLast")
    public CameraUpdateAnimator(@NonNull GoogleMap googleMap, @NonNull OnCameraIdleListener onCameraIdleListener, @NonNull Collection<Animation> animations) {
        mGoogleMap = googleMap;
        mOnCameraIdleListener = onCameraIdleListener;
        mCameraUpdates = new ArrayList<>(animations);
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

    public boolean addAll(@NonNull Collection<Animation> animations) {
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

    @SuppressWarnings({ "FieldMissingNullable", "AssignmentToNull" })
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        mCancelableCallback.setAnimatorListener(null);
        mAnimatorListener = null;
        mOnCameraIdleListener = null;
        mGoogleMap = null;
    }

    private void executeNext() {
        if (mCameraUpdates.isEmpty()) {
            onAnimationEnd();
        } else {
            Animation animation = mCameraUpdates.remove(0);
            if (animation.isCallback()) {
                mCancelableCallback.setAnimation(animation);
                mCancelableCallback.setAnimatorListener(mAnimatorListener);
                if (animation.getDelay() == 0L) {
                    startAnimation(mGoogleMap, animation, mCancelableCallback);
                } else {
                    mHandler.postDelayed(() -> startAnimation(mGoogleMap, animation, mCancelableCallback), animation.getDelay());
                }
            } else {
                if (animation.getDelay() == 0L) {
                    startAnimation(mGoogleMap, animation, null);
                } else {
                    mHandler.postDelayed(() -> startAnimation(mGoogleMap, animation, null), animation.getDelay());
                }
            }
        }
    }

    private void onAnimationEnd() {
        mOnCameraIdleListener.onCameraIdle();
        if (mAnimatorListener != null) {
            mAnimatorListener.onAnimationEnd();
        }
        mGoogleMap.setOnCameraIdleListener(mOnCameraIdleListener);
        UiSettings settings = mGoogleMap.getUiSettings();
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
        UiSettings settings = mGoogleMap.getUiSettings();
        settings.setRotateGesturesEnabled(false);
        settings.setScrollGesturesEnabled(false);
        settings.setTiltGesturesEnabled(false);
        settings.setZoomControlsEnabled(false);
        settings.setZoomGesturesEnabled(false);
        mGoogleMap.setOnCameraIdleListener(this);
        mAnimating = true;
    }

    private void setUiSettings() {
        UiSettings settings = mGoogleMap.getUiSettings();
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
    public static final class Animation {

        private final CameraUpdate mCameraUpdate;

        private boolean mAnimate;

        private boolean mCallback;

        private boolean mClosest;

        private long mDelay;

        @Nullable
        private Object mTag;

        @Nullable
        private LatLng mTarget;

        public Animation(@NonNull CameraUpdate cameraUpdate) {
            mCameraUpdate = cameraUpdate;
        }

        public boolean isCallback() {
            return mCallback;
        }

        public void setCallback(boolean callback) {
            mCallback = callback;
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

        @Nullable
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

        public void setAnimatorListener(@Nullable AnimatorListener animatorListener) {
            mAnimatorListener = animatorListener;
        }
    }
}
