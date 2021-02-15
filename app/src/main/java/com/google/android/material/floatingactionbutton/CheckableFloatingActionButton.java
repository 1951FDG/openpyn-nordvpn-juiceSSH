package com.google.android.material.floatingactionbutton;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.Checkable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.customview.view.AbsSavedState;

import java.util.LinkedHashSet;

public final class CheckableFloatingActionButton extends FloatingActionButton implements Checkable {

    /**
     * Interface definition for a callback to be invoked when the button checked state changes.
     */
    @SuppressWarnings({ "PublicInnerClass", "WeakerAccess" })
    @FunctionalInterface
    public interface OnCheckedChangeListener {

        /**
         * Called when the checked state of a FloatingActionButton has changed.
         *
         * @param button    The FloatingActionButton whose state has changed.
         * @param isChecked The new checked state of FloatingActionButton.
         */
        void onCheckedChanged(@NonNull FloatingActionButton button, boolean isChecked);
    }

    private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked, };

    private boolean mChecked = false;

    private boolean mBroadcasting = false;

    @NonNull
    private final LinkedHashSet<OnCheckedChangeListener> mOnCheckedChangeListeners = new LinkedHashSet<>();

    public CheckableFloatingActionButton(@NonNull Context context) {
        super(context, null);
    }

    public CheckableFloatingActionButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckableFloatingActionButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @NonNull
    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.mChecked = mChecked;
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(@Nullable Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        setChecked(savedState.mChecked);
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

    /**
     * Add a listener that will be invoked when the checked state of this FloatingActionButton changes. See
     * {@link OnCheckedChangeListener}.
     *
     * <p>Components that add a listener should take care to remove it when finished via {@link
     * #removeOnCheckedChangeListener(OnCheckedChangeListener)}.
     *
     * @param listener listener to add
     */
    public void addOnCheckedChangeListener(@NonNull OnCheckedChangeListener listener) {
        mOnCheckedChangeListeners.add(listener);
    }

    /**
     * Remove a listener that was previously added via {@link
     * #addOnCheckedChangeListener(OnCheckedChangeListener)}.
     *
     * @param listener listener to remove
     */
    public void removeOnCheckedChangeListener(@NonNull OnCheckedChangeListener listener) {
        mOnCheckedChangeListeners.remove(listener);
    }

    /**
     * Remove all previously added {@link OnCheckedChangeListener}s.
     */
    public void clearOnCheckedChangeListeners() {
        mOnCheckedChangeListeners.clear();
    }

    @Override
    public void setChecked(boolean checked) {
        if (isEnabled() && (checked != mChecked)) {
            mChecked = checked;
            refreshDrawableState();
            // Avoid infinite recursions if setChecked() is called from a listener
            if (mBroadcasting) {
                return;
            }
            mBroadcasting = true;
            for (OnCheckedChangeListener listener : mOnCheckedChangeListeners) {
                listener.onCheckedChanged(this, mChecked);
            }
            mBroadcasting = false;
        }
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    static class SavedState extends AbsSavedState {

        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @NonNull
            @Override
            public SavedState createFromParcel(@NonNull Parcel source, ClassLoader loader) {
                return new SavedState(source, loader);
            }

            @NonNull
            @Override
            public SavedState createFromParcel(@NonNull Parcel source) {
                return new SavedState(source, null);
            }

            @NonNull
            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        boolean mChecked;

        SavedState(@NonNull Parcelable superState) {
            super(superState);
        }

        SavedState(@NonNull Parcel source, @Nullable ClassLoader loader) {
            super(source, loader);
            readFromParcel(source);
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mChecked ? 1 : 0);
        }

        private void readFromParcel(@NonNull Parcel in) {
            mChecked = in.readInt() == 1;
        }
    }
}
