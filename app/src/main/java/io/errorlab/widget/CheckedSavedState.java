package io.errorlab.widget;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View.BaseSavedState;

class CheckedSavedState extends BaseSavedState {
    protected boolean checked;

    protected CheckedSavedState(Parcelable superState) {
        super(superState);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(checked ? 1 : 0);
    }

    public static final Parcelable.Creator<CheckedSavedState> CREATOR = new Parcelable.Creator<CheckedSavedState>() {
        public CheckedSavedState createFromParcel(Parcel in) {
            return new CheckedSavedState(in);
        }

        public CheckedSavedState[] newArray(int size) {
            return new CheckedSavedState[size];
        }
    };

    private CheckedSavedState(Parcel in) {
        super(in);
        checked = in.readInt() == 1;
    }
}
