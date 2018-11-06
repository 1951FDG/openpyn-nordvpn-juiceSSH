package io.github.sdsstudios.nvidiagpumonitor.custom;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Size;

import java.io.Serializable;
import java.util.Locale;

/**
 * Created by mancj on 27.01.17.
 */

public class Country implements Serializable {
    private String mName;
    private String mCode;
    private int mResource;

    public Country(@Size(min=4) @NonNull String name, @Size(2) @NonNull String countryCode, @DrawableRes int resId) {
        this.mName = name;
        this.mCode = countryCode;
        this.mResource = resId;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public void setName(@Size(min = 4) @NonNull String name) {
        this.mName = name;
    }

    public int getImageResource() {
        return mResource;
    }

    public void setImageResource(@DrawableRes int resId) {
        this.mResource = resId;
    }


    @NonNull
    public String getCountryCode() {
        return mCode;
    }

    public void setCountryCode(@Size(2) @NonNull String countryCode) {
        this.mCode = countryCode;
    }

    public void setCountryCode(@NonNull Locale locale) {
        setCountryCode(locale.getCountry());
    }

    public void setCountryCode() {
        setCountryCode(Locale.getDefault().getCountry());
    }
}
