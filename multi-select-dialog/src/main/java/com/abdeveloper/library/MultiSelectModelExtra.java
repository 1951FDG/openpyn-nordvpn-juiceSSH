package com.abdeveloper.library;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public final class MultiSelectModelExtra extends MultiSelectModel {

    private String mTag;

    private String mUnicode;

    public MultiSelectModelExtra(int id, @NonNull CharSequence name, @DrawableRes int resId, @NonNull String tag, @NonNull String unicode) {
        super(id, name, resId);
        mTag = tag;
        mUnicode = unicode;
    }

    @NonNull
    public String getTag() {
        return mTag;
    }

    public void setTag(@NonNull String charSequence) {
        mTag = charSequence;
    }

    @NonNull
    public String getUnicode() {
        return mUnicode;
    }

    public void setUnicode(@NonNull String unicode) {
        mUnicode = unicode;
    }
}
