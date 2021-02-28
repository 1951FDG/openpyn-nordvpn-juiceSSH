package io.github.getsixtyfour.openpyn.minibar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textview.MaterialTextView;

/**
 * TODO implement queuing for rapid show() events
 */

public class MinibarView extends MaterialTextView {

    private static final long DISMISS_DURATION = 500L;

    private static final long SHOW_DURATION = 500L;

    private ViewPropertyAnimator mAnimator = null;

    private long mDelayMillis = 1000L;

    private Interpolator mDismissInterpolator = new DecelerateInterpolator();

    private float mHeight = 0.0F;

    private Interpolator mShowInterpolator = new AccelerateInterpolator();

    private boolean mShowing = false;

    public MinibarView(@NonNull Context context) {
        this(context, null);
    }

    public MinibarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public MinibarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MinibarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = (float) getHeight();
        if (!mShowing) {
            setTranslationY(-mHeight);
            setAlpha(0.0F);
        }
    }

    /**
     * Get dismissInterpolator
     *
     * @return dismissInterpolator which is a {@link Interpolator}
     */
    @Nullable
    public Interpolator getDismissInterpolator() {
        return mDismissInterpolator;
    }

    /**
     * Set dismissInterpolator
     *
     * @param dismissInterpolator which is a {@link Interpolator}
     */
    public void setDismissInterpolator(@Nullable Interpolator dismissInterpolator) {
        mDismissInterpolator = dismissInterpolator;
    }

    /**
     * Get showInterpolator
     *
     * @return showInterpolator which a {@link Interpolator}
     */
    @Nullable
    public Interpolator getShowInterpolator() {
        return mShowInterpolator;
    }

    /**
     * Set showInterpolator
     *
     * @param showInterpolator which is a {@link Interpolator}
     */
    public void setShowInterpolator(@Nullable Interpolator showInterpolator) {
        mShowInterpolator = showInterpolator;
    }

    public boolean isShowing() {
        return mShowing;
    }

    public void setShowing(boolean showing) {
        mShowing = showing;
    }

    /**
     * Show the message in {@link MinibarView}
     */
    public void show(long duration) {
        mDelayMillis = duration;
        show();
    }

    private void show() {
        mAnimator = animate();
        setAlpha(1.0F);
        mShowing = true;
        mAnimator.setDuration(SHOW_DURATION).translationY(0.0F).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Handler handler = new Handler();
                handler.postDelayed(() -> dismiss(), mDelayMillis);
                setSelected(true);
            }
        });
        if (mShowInterpolator != null) {
            mAnimator.setInterpolator(mShowInterpolator);
        }
        mAnimator.start();
    }

    private void dismiss() {
        mAnimator = animate();
        mAnimator.setDuration(DISMISS_DURATION).translationY(-mHeight).alpha(0.0F).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setShowing(false);
                setSelected(false);
            }
        });
        if (mDismissInterpolator != null) {
            mAnimator.setInterpolator(mDismissInterpolator);
        }
        mAnimator.start();
    }

    private void fastDismiss() {
        if (mShowing) {
            if (mAnimator != null) {
                mAnimator.cancel();
            }
            mShowing = false;
            setTranslationY(-mHeight);
            setAlpha(0.0F);
        }
    }
}
