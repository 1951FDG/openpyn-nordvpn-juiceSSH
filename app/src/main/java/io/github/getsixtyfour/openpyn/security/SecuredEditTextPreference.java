package io.github.getsixtyfour.openpyn.security;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.EditTextPreference.OnBindEditTextListener;

import com.google.android.material.textfield.TextInputLayout;

public final class SecuredEditTextPreference extends EditTextPreference implements OnBindEditTextListener {

    private final SecurityCypher mSecurityCypher = SecurityCypher.getInstance(getContext());

    public SecuredEditTextPreference(@NonNull Context context) {
        super(context);
        // setOnBindEditTextListener(this);
    }

    public SecuredEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // setOnBindEditTextListener(this);
    }

    public SecuredEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // setOnBindEditTextListener(this);
    }

    public SecuredEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        // setOnBindEditTextListener(this);
    }

    @Nullable
    @Override
    public String getText() {
        String text = super.getText();
        return ((text == null) || text.isEmpty()) ? text : mSecurityCypher.decryptString(text);
    }

    @Override
    public void setText(@Nullable String text) {
        if ((text == null) || text.isEmpty()) {
            super.setText(text);
        } else {
            super.setText(mSecurityCypher.encryptString(text));
        }
    }

    @Override
    public void onBindEditText(@NonNull EditText editText) {
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        // Place cursor at the end
        editText.setSelection(editText.getText().length());
        // Add a trailing icon to toggle between the password being displayed as plain-text or disguised
        if ((editText.getParent() != null) && (editText.getParent().getParent() != null) && (editText.getParent()
                .getParent() instanceof TextInputLayout)) {
            ((TextInputLayout) editText.getParent().getParent()).setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        }
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        super.setText(getPersistedString((String) defaultValue));
    }
}
