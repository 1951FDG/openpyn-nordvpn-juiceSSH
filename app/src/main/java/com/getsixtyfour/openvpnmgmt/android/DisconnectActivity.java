package com.getsixtyfour.openvpnmgmt.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NonNls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.getsixtyfour.openpyn.R;

/**
 * @author 1951FDG
 */

public final class DisconnectActivity extends AppCompatActivity implements DialogInterface.OnClickListener, ServiceConnection {

    @NonNls
    @NonNull
    private static final Logger LOGGER = LoggerFactory.getLogger(DisconnectActivity.class);

    @Nullable
    private AlertDialog mDialog = null;

    @Nullable
    private IOpenVpnService mService = null;

    private boolean mServiceBound;

    private static void onDismiss(DialogInterface dialog) {
        if (dialog instanceof AlertDialog) {
            Activity ownerActivity = ((AlertDialog) dialog).getOwnerActivity();
            if (ownerActivity != null) {
                ownerActivity.finish();
            }
        }
    }

    @Override
    @SuppressWarnings({ "deprecation", "RedundantSuppression" })
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int themeResId;
        /*TypedValue outValue = new TypedValue();
        Theme theme = getTheme();
        theme.resolveAttribute(R.attr.alertDialogTheme, outValue, true);
        themeResId = outValue.resourceId;*/
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        int currentNightMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            themeResId = AlertDialog.THEME_DEVICE_DEFAULT_DARK;
        } else {
            themeResId = AlertDialog.THEME_DEVICE_DEFAULT_LIGHT;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, themeResId);
        builder.setTitle(R.string.vpn_title_disconnect);
        builder.setMessage(R.string.vpn_msg_disconnect);
        //noinspection UnnecessaryFullyQualifiedName
        builder.setNegativeButton(android.R.string.cancel, null);
        //noinspection UnnecessaryFullyQualifiedName
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setOnDismissListener(DisconnectActivity::onDismiss);
        mDialog = builder.create();
        mDialog.setOwnerActivity(this);
        LOGGER.debug("onCreate");
    }

    @MainThread
    @Override
    public void onStart() {
        super.onStart();

        // Bind to the service
        Intent intent = new Intent(this, OpenVpnService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
        if (mDialog != null) {
            mDialog.show();
        }
        LOGGER.debug("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();

        LOGGER.debug("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();

        LOGGER.debug("onPause");
    }

    @MainThread
    @Override
    public void onStop() {
        super.onStop();

        // Unbind from the service
        if (mServiceBound) {
            unbindService(this);
            mServiceBound = false;
        }
        if (mDialog != null) {
            mDialog.hide();
        }
        LOGGER.debug("onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDialog != null) {
            mDialog.setOnDismissListener(null);
            mDialog.dismiss();
            mDialog = null;
        }
        LOGGER.debug("onDestroy");
    }

    @Override
    public void onClick(@Nullable DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (mService != null) {
                try {
                    mService.disconnectVpn();
                } catch (RemoteException e) {
                    LOGGER.error("", e);
                }
            }
        }
    }

    @Override
    public void onServiceConnected(@NonNull ComponentName name, @NonNull IBinder service) {
        mService = IOpenVpnService.Stub.asInterface(service);
        mServiceBound = true;
        LOGGER.debug("onServiceConnected");
    }

    @Override
    public void onServiceDisconnected(@NonNull ComponentName name) {
        mService = null;
        mServiceBound = false;
        LOGGER.debug("onServiceDisconnected");
    }
}
