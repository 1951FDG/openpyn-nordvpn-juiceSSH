package io.errorlab.widget;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.customview.view.AbsSavedState;

public final class CheckedSavedState extends AbsSavedState {

    public static final Creator<CheckedSavedState> CREATOR = new ClassLoaderCreator<CheckedSavedState>() {
        @Override
        public CheckedSavedState createFromParcel(Parcel source, ClassLoader loader) {
            return new CheckedSavedState(source, loader);
        }

        @Override
        public CheckedSavedState createFromParcel(Parcel source) {
            return new CheckedSavedState(source, null);
        }

        @Override
        public CheckedSavedState[] newArray(int size) {
            return new CheckedSavedState[size];
        }
    };

    protected boolean mChecked;

    protected CheckedSavedState(@NonNull Parcelable superState) {
        super(superState);
    }

    CheckedSavedState(@NonNull Parcel source, @Nullable ClassLoader loader) {
        super(source, loader);
        mChecked = source.readInt() == 1;
    }

    @Override
    public void writeToParcel(@Nullable Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mChecked ? 1 : 0);
    }
}
