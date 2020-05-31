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

import java.lang.ref.WeakReference;

import org.jetbrains.annotations.NonNls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.getsixtyfour.openpyn.R;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

public final class DisconnectActivity extends AppCompatActivity implements DialogInterface.OnClickListener {

    @NonNls
    static final Logger LOGGER = LoggerFactory.getLogger(DisconnectActivity.class);

    @Nullable
    private AlertDialog mDialog = null;

    @Nullable
    private IOpenVpnServiceInternal mService = null;

    private final ServiceConnection mConnection = new ServiceConnection() {

        // Called when the connection with the service is established
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LOGGER.info("onServiceConnected");
            setService(IOpenVpnServiceInternal.Stub.asInterface(service));
        }

        // Called when the connection with the service disconnects unexpectedly
        @Override
        public void onServiceDisconnected(ComponentName name) {
            LOGGER.error("onServiceDisconnected");
            setService(null);
        }
    };

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
        intent.setAction(Constants.ACTION_START_SERVICE_NOT_STICKY);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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
        if (mService != null) {
            unbindService(mConnection);
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
    public void onClick(@NonNull DialogInterface dialog, int which) {
        if ((which == DialogInterface.BUTTON_POSITIVE) && (mService != null)) {
            Thread thread = new ShutdownThread(mService);
            thread.start();
        }
    }

    @Nullable
    IOpenVpnServiceInternal getService() {
        return mService;
    }

    void setService(@Nullable IOpenVpnServiceInternal service) {
        mService = service;
    }

    @SuppressWarnings("ClassExplicitlyExtendsThread")
    private static final class ShutdownThread extends Thread {

        private final WeakReference<IOpenVpnServiceInternal> mService;

        ShutdownThread(@Nullable IOpenVpnServiceInternal service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void run() {
            IOpenVpnServiceInternal service = mService.get();
            if (service != null) {
                try {
                    service.disconnectVpn();
                } catch (RemoteException e) {
                    LOGGER.error("RemoteException during OpenVPN shutdown", e);
                } catch (RuntimeException e) {
                    LOGGER.error("", e);
                }
            }
        }
    }
}
