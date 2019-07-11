package io.github.getsixtyfour.openpyn.utilities;

import androidx.annotation.NonNull;

import com.abdeveloper.library.MultiSelectModel;
import com.abdeveloper.library.MultiSelectable;

public class MultiSelectModelExtra extends MultiSelectModel implements MultiSelectable {

    private CharSequence tag;

    public MultiSelectModelExtra(final int id, @NonNull final CharSequence name, final int resId, @NonNull String tag) {
        super(id, name, resId);
        this.tag = tag;
    }

    @NonNull
    public CharSequence getTag() {
        return tag;
    }

    public void setTag(@NonNull CharSequence charSequence) {
        tag = charSequence;
    }
}
