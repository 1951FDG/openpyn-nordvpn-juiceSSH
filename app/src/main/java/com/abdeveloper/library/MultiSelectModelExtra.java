package com.abdeveloper.library;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import io.github.getsixtyfour.openpyn.R;

@SuppressWarnings("TransientFieldInNonSerializableClass")
public class MultiSelectModelExtra extends MultiSelectModel {

    private static final Pattern COMPILE = Pattern.compile(" ");

    private transient int mInt;

    private String mTag;

    private String mUnicode;

    public MultiSelectModelExtra(int id, @NonNull CharSequence name, @DrawableRes int resId, @NonNull String tag, @NonNull String unicode) {
        super(id, name, resId);
        mTag = tag;
        mUnicode = unicode;
    }

    @NotNull
    @Override
    public String toString() {
        return "MultiSelectModel{" + "id=" + getId() + ", name=" + getName() + ", resId=" + mInt + ", mTag='" + mTag + "\'" + ", mUnicode='"
                + mUnicode + "\'" + ", start=" + getStart() + ", end=" + getEnd() + "}";
    }

    @Override
    public int getResId() {
        if (mInt == 0) {
            int drawableId = 0;
            try {
                Class<R.drawable> res = R.drawable.class;
                CharSequence charSequence = getName();
                Matcher matcher = COMPILE.matcher(charSequence.toString());
                String s = matcher.replaceAll("_");
                @NonNls String format = "ic_%s_40dp";
                String name = String.format(format, s.toLowerCase(Locale.ROOT));
                Field field = res.getField(name);
                drawableId = field.getInt(null);
            } catch (IllegalAccessException | NoSuchFieldException ignored) {
            }
            mInt = drawableId;
        }
        return mInt;
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
