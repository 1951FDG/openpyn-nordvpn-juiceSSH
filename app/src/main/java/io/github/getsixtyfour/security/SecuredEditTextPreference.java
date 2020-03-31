package io.github.getsixtyfour.security;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.EditTextPreference.OnBindEditTextListener;

public final class SecuredEditTextPreference extends EditTextPreference implements OnBindEditTextListener {

    private final SecurityManager mSecurityManager = SecurityManager.getInstance(getContext());

    public SecuredEditTextPreference(@NonNull Context context) {
        super(context);
        setOnBindEditTextListener(this);
    }

    public SecuredEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOnBindEditTextListener(this);
    }

    public SecuredEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnBindEditTextListener(this);
    }

    public SecuredEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOnBindEditTextListener(this);
    }

    @Nullable
    @Override
    public String getText() {
        String text = super.getText();
        return ((text == null) || (text.isEmpty())) ? text : mSecurityManager.decryptString(text);
    }

    @Override
    public void setText(@Nullable String text) {
        if ((text == null) || text.isEmpty()) {
            super.setText(text);
        } else {
            super.setText(mSecurityManager.encryptString(text));
        }
    }

    @Override
    public void onBindEditText(@NonNull EditText editText) {
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        super.setText(getPersistedString((String) defaultValue));
    }
}
