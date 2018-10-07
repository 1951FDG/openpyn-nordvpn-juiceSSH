package io.github.sdsstudios.nvidiagpumonitor;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;

public class SecuredEditTextPreference extends EditTextPreference {

    public SecuredEditTextPreference(Context context) {
        super(context);
    }

    public SecuredEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SecuredEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SecuredEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr,
                                     int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        super.setText(getPersistedString(null));
    }

    @Override
    public String getText() {
        String text = super.getText();
        if (text == null || text.length() == 0) {
            return text;
        }
        return SecurityManager
                .getInstance(getContext())
                .decryptString(text);
    }

    @Override
    public void setText(String text) {
        if (text == null || text.length() == 0) {
            super.setText(text);
            return;
        }
        super.setText(SecurityManager
                .getInstance(getContext())
                .encryptString(text));
    }
}
