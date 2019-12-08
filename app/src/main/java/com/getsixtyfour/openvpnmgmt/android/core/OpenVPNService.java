/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.getsixtyfour.openvpnmgmt.android.core;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;

import com.getsixtyfour.openvpnmgmt.android.activities.DisconnectVPN;
import com.getsixtyfour.openvpnmgmt.android.constant.IntentConstants;
import com.getsixtyfour.openvpnmgmt.core.ConnectionStatus;
import com.getsixtyfour.openvpnmgmt.core.LogLevel;
import com.getsixtyfour.openvpnmgmt.core.VpnStatus;
import com.getsixtyfour.openvpnmgmt.listeners.ByteCountManager.ByteCountListener;
import com.getsixtyfour.openvpnmgmt.listeners.LogManager;
import com.getsixtyfour.openvpnmgmt.listeners.LogManager.LogListener;
import com.getsixtyfour.openvpnmgmt.listeners.StateManager.State;
import com.getsixtyfour.openvpnmgmt.listeners.StateManager.StateListener;
import com.getsixtyfour.openvpnmgmt.net.Connection;
import com.getsixtyfour.openvpnmgmt.net.ConnectionListener;
import com.getsixtyfour.openvpnmgmt.net.ManagementConnection;
import com.getsixtyfour.openvpnmgmt.utils.StringUtils;

import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.Locale;

import io.github.getsixtyfour.openpyn.R;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

@SuppressWarnings({ "OverlyComplexClass", "ClassWithTooManyDependencies" })
public final class OpenVPNService extends Service
        implements LogListener, StateListener, ByteCountListener, IOpenVPNServiceInternal, ConnectionListener {

    @NonNls
    public static final String NOTIFICATION_CHANNEL_BG_ID = "openvpn_bg";

    @NonNls
    public static final String NOTIFICATION_CHANNEL_NEW_STATUS_ID = "openvpn_newstat";

    private static final String TAG = "OpenVPNService";

    private static final String DEFAULT_REMOTE_SERVER = "127.0.0.1";

    private static final int DEFAULT_REMOTE_PORT = 23;

    private static final String THREAD_NAME = "OpenVPNManagementThread";

    private final IBinder mBinder = new IOpenVPNServiceInternal.Stub() {
        @Override
        public boolean stopVPN(boolean replaceConnection) {
            return OpenVPNService.this.stopVPN(replaceConnection);
        }
    };

    private long mConnectTime;

    private boolean mDisplayByteCount = false;

    private boolean mPostNotification = false;

    private boolean mSendBroadcast = false;

    @SuppressWarnings({ "RedundantNoArgConstructor", "UnnecessaryCallToSuper" })
    public OpenVPNService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels();
        }
        {
            Connection connection = ManagementConnection.getInstance();
            connection.addByteCountListener(this);
            connection.addLogListener(this);
            connection.addStateListener(this);
            connection.setConnectionListener(this);
        }
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if ((intent != null) && IntentConstants.ACTION_START_SERVICE_NOT_STICKY.equals(intent.getAction())) {
            return START_NOT_STICKY;
        }
        if ((intent != null) && IntentConstants.ACTION_START_SERVICE_STICKY.equals(intent.getAction())) {
            return START_REDELIVER_INTENT;
        }
        if (intent == null) {
            throw new IllegalArgumentException("intent can't be null");
        }

        mPostNotification = intent.getBooleanExtra(IntentConstants.EXTRA_POST_NOTIFICATION, false);
        mSendBroadcast = intent.getBooleanExtra(IntentConstants.EXTRA_SEND_BROADCAST, false);

        String host = StringUtils.defaultIfBlank(intent.getStringExtra(IntentConstants.EXTRA_HOST), DEFAULT_REMOTE_SERVER);
        int port = intent.getIntExtra(IntentConstants.EXTRA_PORT, DEFAULT_REMOTE_PORT);

        // Always show notification here to avoid problem with startForeground timeout
        {
            String text = getString(R.string.vpn_launch_title);
            String title = getString(R.string.notification_title, getString(R.string.state_disconnected));
            showNotification(title, text, text, NOTIFICATION_CHANNEL_NEW_STATUS_ID, 0L, ConnectionStatus.LEVEL_NOT_CONNECTED);
        }
        // todo inner class, check thread killed handler to end service
        // Connect to the management interface in a background thread
        Thread thread = new Thread(() -> {
            try {
                Connection connection = ManagementConnection.getInstance();
                connection.connect(host, port);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            } catch (Exception e) {
                Log.e(TAG, "Unknown exception thrown");
                Log.e(TAG, e.toString());
            }
        });
        thread.start();

        Log.i(TAG, "Starting OpenVPN Service");
        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(@Nullable Intent intent) {
        Log.i(TAG, "onBind");
        return ((intent != null) && IntentConstants.ACTION_START_SERVICE_NOT_STICKY.equals(intent.getAction())) ? mBinder : null;
    }

    @Override
    public void onDestroy() {
        {
            Connection connection = ManagementConnection.getInstance();
            connection.removeByteCountListener(this);
            connection.removeLogListener(this);
            connection.removeStateListener(this);
            connection.setConnectionListener(null);
        }
    }

    @NonNull
    @Override
    public IBinder asBinder() {
        return mBinder;
    }

    @Override
    public void onByteCountChanged(long in, long out, long diffIn, long diffOut) {
        if (mDisplayByteCount) {
            long byteCountInterval = ManagementConnection.BYTE_COUNT_INTERVAL.longValue();
            Resources resources = getResources();
            String sIn = humanReadableByteCount(resources, in, false);
            String sDiffIn = humanReadableByteCount(resources, diffIn / byteCountInterval, true);
            String sOut = humanReadableByteCount(resources, out, false);
            String sDiffOut = humanReadableByteCount(resources, diffOut / byteCountInterval, true);
            String text = getString(R.string.status_line_byte_count, sIn, sDiffIn, sOut, sDiffOut);
            String title = getString(R.string.notification_title, getString(R.string.state_connected));
            showNotification(title, text, null, NOTIFICATION_CHANNEL_BG_ID, mConnectTime, ConnectionStatus.LEVEL_CONNECTED);
        }
    }

    // todo check which thread these run on
    @Override
    public void onConnectError(@NonNull Throwable e) {
        // TODO
        endVpnService();
    }

    @Override
    public void onConnected() {
        // Start a background thread that handles incoming messages of the management interface
        Connection connection = ManagementConnection.getInstance();
        Thread thread = new Thread(connection, THREAD_NAME);
        thread.start();
        Log.i(TAG, "Starting OpenVPN Management");
    }

    @Override
    public void onDisconnected() {
        endVpnService();
    }

    @Override
    public void onLog(@NonNull LogManager.Log log) {
        LogLevel value = log.getLevel();
        switch (value) {
            case ERROR:
                if (Log.isLoggable(TAG, Log.ERROR)) {
                    Log.e(TAG, log.getMessage());
                }
                break;
            case WARNING:
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, log.getMessage());
                }
                break;
            case INFO:
                if (Log.isLoggable(TAG, Log.INFO)) {
                    Log.i(TAG, log.getMessage());
                }
                break;
            case DEBUG:
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, log.getMessage());
                }
                break;
            case VERBOSE:
                break;
        }
    }

    @Override
    public void onStateChanged(@NonNull State state) {
        @NonNls String name = state.getName();
        @NonNls String message = state.getMessage();
        @NonNls String address = state.getRemoteAddress();
        @NonNls String port = state.getRemotePort();
        ConnectionStatus level = VpnStatus.getLevel(name, message);
        if (mSendBroadcast) {
            doSendBroadcast(name, message);
        }
        if (mPostNotification) {
            // Display byte count only after being connected
            if (level == ConnectionStatus.LEVEL_CONNECTED) {
                mDisplayByteCount = true;
                mConnectTime = System.currentTimeMillis();
            } else {
                mDisplayByteCount = false;
            }
            String tickerText = getString(getLocalizedState(name));
            String title = getString(R.string.notification_title, tickerText);
            String text = message;
            // (x) optional address of remote server (OpenVPN 2.1 or higher)
            // (y) optional port of remote server (OpenVPN 2.4 or higher)
            // (x) and (y) are shown for ASSIGN_IP and CONNECTED states
            if ((VpnStatus.ASSIGN_IP.equals(name) || VpnStatus.CONNECTED.equals(name)) && !StringUtils.isBlank(address)) {
                @NonNls String prefix = null;
                if ((port != null) && !port.isEmpty()) {
                    if ("1194".equals(port)) {
                        prefix = "UDP";
                    } else {
                        prefix = "TCP";
                    }
                    prefix += ": ";
                }
                text = prefix + address;
            }
            showNotification(title, text, tickerText, NOTIFICATION_CHANNEL_NEW_STATUS_ID, 0L, level);
        }
        // todo test implications
        // if ((level == ConnectionStatus.LEVEL_NOT_CONNECTED) || (level == ConnectionStatus.LEVEL_AUTH_FAILED)) {
        //     endVpnService();
        // }
    }

    // todo test implications
    public boolean stopVPN(boolean replaceConnection) {
        boolean result = false;
        try {
            Connection connection = ManagementConnection.getInstance();
            connection.stopOpenVPN();
            result = true;
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return result;
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private void addVpnActionsToNotification(Builder builder) {
        Intent intent = new Intent(this, DisconnectVPN.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // The notification action icons are still required and continue to be used on older versions of Android
        builder.addAction(R.drawable.ic_close_white, getString(R.string.cancel_connection), pendingIntent);
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannels() {
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(getBaseContext());
        // Background message
        {
            CharSequence name = getString(R.string.channel_name_background);
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_BG_ID, name,
                    NotificationManagerCompat.IMPORTANCE_MIN);
            channel.setDescription(getString(R.string.channel_description_background));
            channel.enableLights(false);
            mNotificationManager.createNotificationChannel(channel);
        }
        // Connection status change messages
        {
            CharSequence name = getString(R.string.channel_name_status);
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_NEW_STATUS_ID, name,
                    NotificationManagerCompat.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.channel_description_status));
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    private void doSendBroadcast(String state, String message) {
        Intent intent = new Intent();
        intent.setAction(IntentConstants.ACTION_VPN_STATE_CHANGED);
        intent.putExtra(IntentConstants.EXTRA_STATE, state);
        intent.putExtra(IntentConstants.EXTRA_MESSAGE, message);
        sendBroadcast(intent, android.Manifest.permission.ACCESS_NETWORK_STATE);
    }

    private void endVpnService() {
        Log.i(TAG, "Stopping OpenVPN Service");
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @SuppressWarnings({ "TypeMayBeWeakened", "MethodWithTooManyParameters" })
    @SuppressLint("ObsoleteSdkInt")
    private void showNotification(String title, String text, String tickerText, String channel, long when, ConnectionStatus status) {
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(getBaseContext());
        Builder builder = new Builder(this, channel);
        {
            builder.setCategory(NotificationCompat.CATEGORY_SERVICE);
            builder.setContentText(text);
            builder.setContentTitle(title);
            builder.setLocalOnly(true);
            builder.setOngoing(true);
            builder.setOnlyAlertOnce(true);
            builder.setShowWhen(false);
            builder.setSmallIcon(getIconByConnectionStatus(status));
            if ((tickerText != null) && !tickerText.isEmpty()) {
                builder.setTicker(tickerText);
            }
            if (NOTIFICATION_CHANNEL_BG_ID.equals(channel)) {
                builder.setUsesChronometer(true);
            }
            if (when != 0L) {
                builder.setWhen(when);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                addVpnActionsToNotification(builder);
            }
        }
        Notification notification = builder.build();
        int notificationId = channel.hashCode();
        mNotificationManager.notify(notificationId, notification);
        startForeground(notificationId, notification);
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    private static int getIconByConnectionStatus(ConnectionStatus level) {
        switch (level) {
            case LEVEL_CONNECTED:
                return R.drawable.ic_stat_shield;
            case LEVEL_CONNECTING_SERVER_REPLIED:
                return R.drawable.ic_stat_shield_check_outline;
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_NOT_CONNECTED:
            case LEVEL_AUTH_FAILED:
            case UNKNOWN_LEVEL:
            default:
                return R.drawable.ic_stat_shield_outline;
        }
    }

    @SuppressWarnings({ "MethodWithMultipleReturnPoints", "OverlyComplexMethod", "OverlyLongMethod" })
    private static int getLocalizedState(String state) {
        switch (state) {
            case VpnStatus.CONNECTING:
                return R.string.state_connecting;
            case VpnStatus.WAIT:
                return R.string.state_wait;
            case VpnStatus.AUTH:
                return R.string.state_auth;
            case VpnStatus.GET_CONFIG:
                return R.string.state_get_config;
            case VpnStatus.ASSIGN_IP:
                return R.string.state_assign_ip;
            case VpnStatus.ADD_ROUTES:
                return R.string.state_add_routes;
            case VpnStatus.CONNECTED:
                return R.string.state_connected;
            case VpnStatus.DISCONNECTED:
                return R.string.state_disconnected;
            case VpnStatus.RECONNECTING:
                return R.string.state_reconnecting;
            case VpnStatus.EXITING:
                return R.string.state_exiting;
            case VpnStatus.RESOLVE:
                return R.string.state_resolve;
            case VpnStatus.TCP_CONNECT:
                return R.string.state_tcp_connect;
            case VpnStatus.AUTH_PENDING:
                return R.string.state_auth_pending;
            case VpnStatus.AUTH_FAILED:
                return R.string.state_auth_failed;
            default:
                return R.string.unknown_state;
        }
    }

    @SuppressWarnings({ "OverlyComplexMethod", "MagicNumber", "ImplicitNumericConversion" })
    private static String humanReadableByteCount(Resources res, long bytes, boolean speed) {
        float unit = speed ? 1000.0F : 1024.0F;
        float result = speed ? (bytes << 3) : bytes;
        String units = speed ? res.getString(R.string.bits_per_second) : res.getString(R.string.volume_byte);
        int exp = 0;
        if (result > 900.0F) {
            units = speed ? res.getString(R.string.kbits_per_second) : res.getString(R.string.volume_kbyte);
            exp = 1;
            result /= unit;
        }
        if (result > 900.0F) {
            units = speed ? res.getString(R.string.mbits_per_second) : res.getString(R.string.volume_mbyte);
            exp = 2;
            result /= unit;
        }
        if (result > 900.0F) {
            units = speed ? res.getString(R.string.gbits_per_second) : res.getString(R.string.volume_gbyte);
            exp = 3;
            result /= unit;
        }
        @NonNls String roundFormat = "%.0f";
        if (exp != 0) {
            if (result < 1.0F) {
                roundFormat = "%.2f";
            } else if (result < 10.0F) {
                roundFormat = "%.1f";
            }
        }
        String roundedString = String.format(Locale.ROOT, roundFormat, result);
        return res.getString(R.string.byteSizeSuffix, roundedString, units);
    }
}
