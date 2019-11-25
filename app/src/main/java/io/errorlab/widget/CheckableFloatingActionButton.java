package io.errorlab.widget;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.Checkable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CheckableFloatingActionButton extends FloatingActionButton implements Checkable {

    @SuppressWarnings("PublicInnerClass")
    @FunctionalInterface
    public interface OnCheckedChangeListener {

        void onCheckedChanged(@NonNull FloatingActionButton fabView, boolean isChecked);
    }

    private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked, };

    private boolean mChecked;

    private OnCheckedChangeListener mOnCheckedChangeListener;

    public CheckableFloatingActionButton(@NonNull Context ctx) {
        this(ctx, null);
    }

    public CheckableFloatingActionButton(@NonNull Context ctx, @Nullable AttributeSet attrs) {
        this(ctx, attrs, 0);
    }

    public CheckableFloatingActionButton(@NonNull Context ctx, @Nullable AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
    }

    @Override
    protected void onRestoreInstanceState(@Nullable Parcelable state) {
        if (!(state instanceof CheckedSavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        CheckedSavedState ss = (CheckedSavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setChecked(ss.checked);
    }

    @NonNull
    @Override
    protected Parcelable onSaveInstanceState() {
        CheckedSavedState result = new CheckedSavedState(super.onSaveInstanceState());
        result.checked = mChecked;
        return result;
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (checked != mChecked) {
            mChecked = checked;
            refreshDrawableState();
            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(this, checked);
            }
        }
    }

    @NonNull
    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (mChecked) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }
}
