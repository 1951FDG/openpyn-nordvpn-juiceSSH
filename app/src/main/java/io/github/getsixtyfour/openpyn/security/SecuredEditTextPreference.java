package io.github.getsixtyfour.openpyn.security;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;

public final class SecuredEditTextPreference extends EditTextPreference {

    private final SecurityManager securityManager = SecurityManager.getInstance(getContext());

    public SecuredEditTextPreference(@NonNull Context context) {
        super(context);
    }

    public SecuredEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SecuredEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SecuredEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Nullable
    @Override
    public String getText() {
        String text = super.getText();
        return ((text == null) || (text.isEmpty())) ? text : securityManager.decryptString(text);
    }

    @Override
    public void setText(@Nullable String text) {
        if ((text == null) || text.isEmpty()) {
            super.setText(text);
        } else {
            super.setText(securityManager.encryptString(text));
        }
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        super.setText(getPersistedString(null));
    }
}
