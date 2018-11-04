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
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(checked ? 1 : 0);
    }

    public static final Creator<CheckedSavedState> CREATOR = new ClassLoaderCreator<CheckedSavedState>() {
        public CheckedSavedState createFromParcel(Parcel in, ClassLoader loader) {
            return new CheckedSavedState(in, loader);
        }

        public CheckedSavedState createFromParcel(Parcel in) {
            return new CheckedSavedState(in, null);
        }

        public CheckedSavedState[] newArray(int size) {
            return new CheckedSavedState[size];
        }
    };

    private CheckedSavedState(Parcel in, ClassLoader loader) {
        super(in, loader);
        checked = in.readInt() == 1;
    }
}
