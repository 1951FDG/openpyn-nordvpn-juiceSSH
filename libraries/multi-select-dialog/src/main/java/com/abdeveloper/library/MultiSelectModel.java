package com.abdeveloper.library;

import android.text.Spannable;
import android.text.SpannableString;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MultiSelectModel implements MultiSelectable, Iconable, Range {

    private final int mId;

    private final int mResId;

    private int mEnd;

    private int mStart;

    private CharSequence mName;

    public MultiSelectModel(int id, @NonNull CharSequence name) {
        this(id, name, 0);
    }

    public MultiSelectModel(int id, @NonNull CharSequence name, @DrawableRes int resId) {
        mId = id;
        mResId = resId;
        mName = name;
    }

    @SuppressWarnings("FinalMethod")
    @Nullable
    @Override
    public final MultiSelectModel clone() {
        try {
            MultiSelectModel clone = (MultiSelectModel) super.clone();
            CharSequence name = clone.getName();
            if (name instanceof Spannable) {
                clone.setName(new SpannableString(name.toString()));
            }
            return clone;
        } catch (CloneNotSupportedException ignored) {
        }
        return null;
    }

    @Override
    public int getEnd() {
        return mEnd;
    }

    @Override
    public void setEnd(int end) {
        mEnd = end;
    }

    @Override
    public int getId() {
        return mId;
    }

    @NonNull
    @Override
    public CharSequence getName() {
        return mName;
    }

    @Override
    public void setName(@NonNull CharSequence charSequence) {
        mName = charSequence;
    }

    @Override
    public int getResId() {
        return mResId;
    }

    @Override
    public int getStart() {
        return mStart;
    }

    @Override
    public void setStart(int start) {
        mStart = start;
    }
}
