package com.abdeveloper.library;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.getsixtyfour.openpyn.R;

@SuppressWarnings("TransientFieldInNonSerializableClass")
public class MultiSelectModelExtra extends MultiSelectModel {

    private static final Pattern COMPILE = Pattern.compile(" ");

    @SuppressWarnings("FieldNotUsedInToString")
    private transient int mInt;

    private String mTag;

    private String mUnicode;

    public MultiSelectModelExtra(int id, @NonNull CharSequence name, int resId, @NonNull String tag, @NonNull String unicode) {
        super(id, name, resId);
        mTag = tag;
        mUnicode = unicode;
    }

    @NotNull
    @SuppressWarnings("MagicCharacter")
    @Override
    public String toString() {
        return "MultiSelectModel{" + "id=" + getId() + ", name=" + getName() + ", resId=" + getResId() + ", mTag='" + mTag + '\''
                + ", mUnicode='" + mUnicode + '\'' + ", start=" + getStart() + ", end=" + getEnd() + '}';
    }

    @Override
    public int getResId() {
        if (mInt == 0) {
            int drawableId = 0;
            try {
                Class res = R.drawable.class;
                CharSequence charSequence = getName();
                Matcher matcher = COMPILE.matcher(charSequence.toString());
                String s = matcher.replaceAll("_");
                String name = String.format("ic_%s_40dp", s.toLowerCase(Locale.ROOT));
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
