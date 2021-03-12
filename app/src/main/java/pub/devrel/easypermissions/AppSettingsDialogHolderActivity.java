package pub.devrel.easypermissions;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppSettingsDialogHolderActivity extends AppCompatActivity implements DialogInterface.OnClickListener {
    private static final int APP_SETTINGS_RC = 7534;

    private AlertDialog mDialog;
    private int mIntentFlags;

    @NonNull
    public static Intent createShowDialogIntent(@NonNull Context context, @NonNull AppSettingsDialog dialog) {
        Intent intent = new Intent(context, AppSettingsDialogHolderActivity.class);
        intent.putExtra(AppSettingsDialog.EXTRA_APP_SETTINGS, dialog);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSettingsDialog appSettingsDialog = AppSettingsDialog.fromIntent(getIntent(), this);
        mIntentFlags = appSettingsDialog.getIntentFlags();
        mDialog = appSettingsDialog.showDialog(this, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if ((mDialog != null) && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @Override
    public void onClick(@Nullable DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", getPackageName(), null));
            intent.addFlags(mIntentFlags);
            startActivityForResult(intent, APP_SETTINGS_RC);
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        } else {
            throw new IllegalStateException("Unknown button type: " + which);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
    }
}