/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;

import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import io.github.getsixtyfour.openpyn.R;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

public class DisconnectVPN extends AppCompatActivity implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

    private static final class stopVPNTask implements Runnable {

        private final WeakReference<IOpenVPNServiceInternal> mService;

        private stopVPNTask(IOpenVPNServiceInternal service) {
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
            mService = IOpenVPNServiceInternal.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE_NOT_STICKY);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        showDisconnectDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (mService != null) {
                Runnable target = new stopVPNTask(mService);
                new Thread(target).start();
            }
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
