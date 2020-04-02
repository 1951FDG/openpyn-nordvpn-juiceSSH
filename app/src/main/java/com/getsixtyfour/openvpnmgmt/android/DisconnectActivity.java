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
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;

import io.github.getsixtyfour.openpyn.R;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

public class DisconnectActivity extends AppCompatActivity implements DialogInterface.OnClickListener {

    private static final String TAG = "DisconnectActivity";

    private static final DialogInterface.OnDismissListener ON_DISMISS_LISTENER = (DialogInterface dialog) -> {
        if (dialog instanceof AlertDialog) {
            Activity ownerActivity = ((AlertDialog) dialog).getOwnerActivity();
            if (ownerActivity != null) {
                ownerActivity.finish();
            }
        }
    };

    private final ServiceConnection mConnection = new ServiceConnection() {

        // Called when the connection with the service is established
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected"); //NON-NLS
            setService(IOpenVpnServiceInternal.Stub.asInterface(service));
        }

        // Called when the connection with the service disconnects unexpectedly
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceDisconnected"); //NON-NLS
            setService(null);
        }
    };

    @Nullable
    private AlertDialog mDialog;

    @Nullable
    private IOpenVpnServiceInternal mService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int themeResId;
        // TypedValue outValue = new TypedValue();
        // Theme theme = getTheme();
        // theme.resolveAttribute(R.attr.alertDialogTheme, outValue, true);
        // themeResId = outValue.resourceId;
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
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setOnDismissListener(ON_DISMISS_LISTENER);
        mDialog = builder.create();
        mDialog.setOwnerActivity(this);
        Log.d(TAG, "onCreate"); //NON-NLS
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
        Log.d(TAG, "onStart"); //NON-NLS
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume"); //NON-NLS
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause"); //NON-NLS
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
        Log.d(TAG, "onStop"); //NON-NLS
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDialog != null) {
            mDialog.setOnDismissListener(null);
            mDialog.dismiss();
            mDialog = null;
        }
        Log.d(TAG, "onDestroy"); //NON-NLS
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

        ShutdownThread(IOpenVpnServiceInternal service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void run() {
            IOpenVpnServiceInternal service = mService.get();
            if (service != null) {
                try {
                    service.disconnectVpn();
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during OpenVPN shutdown", e); //NON-NLS
                } catch (RuntimeException e) {
                    Log.e(TAG, "", e);
                }
            }
        }
    }
}