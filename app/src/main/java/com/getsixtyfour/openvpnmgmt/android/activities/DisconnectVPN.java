/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.getsixtyfour.openvpnmgmt.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.getsixtyfour.openvpnmgmt.android.constant.IntentConstants;
import com.getsixtyfour.openvpnmgmt.android.core.IOpenVPNServiceInternal;
import com.getsixtyfour.openvpnmgmt.android.core.OpenVPNService;

import java.lang.ref.WeakReference;

import io.github.getsixtyfour.openpyn.R;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

public class DisconnectVPN extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

    private static final class StopTask implements Runnable {

        private final WeakReference<IOpenVPNServiceInternal> mService;

        StopTask(IOpenVPNServiceInternal service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void run() {
            IOpenVPNServiceInternal service = mService.get();
            if (service != null) {
                try {
                    service.stopVPN(false);
                } catch (RemoteException e) {
                    // TODO
                    e.printStackTrace();
                }
            }
        }
    }

    @Nullable
    private IOpenVPNServiceInternal mService;

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            setService(IOpenVPNServiceInternal.Stub.asInterface(service));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            setService(null);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, OpenVPNService.class);
        intent.setAction(IntentConstants.ACTION_START_SERVICE_NOT_STICKY);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        showDisconnectDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    @Nullable
    public IOpenVPNServiceInternal getService() {
        return mService;
    }

    public void setService(@Nullable IOpenVPNServiceInternal service) {
        mService = service;
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        finish();
    }

    @Override
    public void onClick(@NonNull DialogInterface dialog, int which) {
        if ((which == DialogInterface.BUTTON_POSITIVE) && (mService != null)) {
            Runnable target = new StopTask(mService);
            Thread thread = new Thread(target);
            thread.start();
        }
        finish();
    }

    private void showDisconnectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_cancel);
        builder.setMessage(R.string.cancel_connection_query);
        builder.setNegativeButton(android.R.string.cancel, this);
        builder.setPositiveButton(R.string.cancel_connection, this);
        builder.setOnCancelListener(this);
        builder.show();
    }
}
