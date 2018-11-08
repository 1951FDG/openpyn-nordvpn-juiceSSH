package io.errorlab.widget;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.customview.view.AbsSavedState;

class CheckedSavedState extends AbsSavedState {
    protected boolean checked;

    protected CheckedSavedState(Parcelable superState) {
        super(superState);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(checked ? 1 : 0);
    }

    public static final Creator<CheckedSavedState> CREATOR = new ClassLoaderCreator<CheckedSavedState>() {
        public CheckedSavedState createFromParcel(Parcel source, ClassLoader loader) {
            return new CheckedSavedState(source, loader);
        }

        public CheckedSavedState createFromParcel(Parcel source) {
            return new CheckedSavedState(source, null);
        }

        public CheckedSavedState[] newArray(int size) {
            return new CheckedSavedState[size];
        }
    };

    @SuppressWarnings("WeakerAccess")
    CheckedSavedState(Parcel source, ClassLoader loader) {
        super(source, loader);
        checked = source.readInt() == 1;
    }
}
