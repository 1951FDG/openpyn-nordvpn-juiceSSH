package com.abdeveloper.library;

import android.text.Spannable;

class MultiSelectFactory extends Spannable.Factory {

    @Override
    public Spannable newSpannable(CharSequence source) {
        return (Spannable) source;
    }
}
