/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

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

import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.Locale;

import de.blinkt.openvpn.activities.DisconnectVPN;
import io.github.getsixtyfour.openpyn.R;
import ua.pp.msk.openvpnstatus.core.ConnectionStatus;
import ua.pp.msk.openvpnstatus.core.LogLevel;
import ua.pp.msk.openvpnstatus.core.VpnStatus;
import ua.pp.msk.openvpnstatus.listeners.ByteCountManager.ByteCountListener;
import ua.pp.msk.openvpnstatus.listeners.LogManager;
import ua.pp.msk.openvpnstatus.listeners.LogManager.LogListener;
import ua.pp.msk.openvpnstatus.listeners.StateManager.State;
import ua.pp.msk.openvpnstatus.listeners.StateManager.StateListener;
import ua.pp.msk.openvpnstatus.net.Connection;
import ua.pp.msk.openvpnstatus.net.ConnectionListener;
import ua.pp.msk.openvpnstatus.net.ManagementConnection;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

@SuppressWarnings({ "OverlyComplexClass", "ClassWithTooManyDependencies" })
public final class OpenVPNService extends Service implements LogListener, StateListener, ByteCountListener, IOpenVPNServiceInternal,
        ConnectionListener {

    @NonNls
    public static final String ACTION_VPN_STATUS = "de.blinkt.openvpn.action.VPN_STATUS";

    @NonNls
    public static final String EXTRA_ALWAYS_SHOW_NOTIFICATION = "de.blinkt.openvpn.extra.NOTIFICATION_ALWAYS_VISIBLE";

    @NonNls
    public static final String EXTRA_HOST = "de.blinkt.openvpn.extra.HOST";

    @NonNls
    public static final String EXTRA_PORT = "de.blinkt.openvpn.extra.PORT";

    @NonNls
    public static final String NOTIFICATION_CHANNEL_BG_ID = "openvpn_bg";

    @NonNls
    public static final String NOTIFICATION_CHANNEL_NEW_STATUS_ID = "openvpn_newstat";

    @NonNls
    public static final String START_SERVICE_NOT_STICKY = "de.blinkt.openvpn.START_SERVICE_NOT_STICKY";

    @NonNls
    public static final String START_SERVICE_STICKY = "de.blinkt.openvpn.START_SERVICE_STICKY";

    private static final String TAG = "OpenVPNService";

    private static final String DEFAULT_REMOTE_SERVER = "127.0.0.1";

    private static final int DEFAULT_REMOTE_PORT = 23;

    private final IBinder mBinder = new IOpenVPNServiceInternal.Stub() {
        @Override
        public boolean stopVPN(boolean replaceConnection) {
            return OpenVPNService.this.stopVPN(replaceConnection);
        }
    };

    private long mConnectTime;

    private boolean mDisplayByteCount = false;

    private boolean mNotificationAlwaysVisible = false;

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
        if ((intent != null) && START_SERVICE_NOT_STICKY.equals(intent.getAction())) {
            return START_NOT_STICKY;
        }
        if ((intent != null) && START_SERVICE_STICKY.equals(intent.getAction())) {
            return START_REDELIVER_INTENT;
        }
        if (intent == null) {
            throw new IllegalArgumentException("intent can't be null");
        }

        mNotificationAlwaysVisible = intent.getBooleanExtra(EXTRA_ALWAYS_SHOW_NOTIFICATION, true);

        String host = (intent.getStringExtra(EXTRA_HOST) != null) ? intent.getStringExtra(EXTRA_HOST) : DEFAULT_REMOTE_SERVER;
        int port = intent.getIntExtra(EXTRA_PORT, DEFAULT_REMOTE_PORT);

        // Always show notification here to avoid problem with startForeground timeout
        {
            String text = getString(R.string.vpn_launch_title);
            String title = getString(R.string.notifcation_title, getString(R.string.state_disconnected));
            showNotification(title, text, text, NOTIFICATION_CHANNEL_NEW_STATUS_ID, 0L, ConnectionStatus.LEVEL_NOT_CONNECTED);
        }
        // todo inner class, check thread killed handler to end service
        // Connect to the management interface in a background thread
        Thread thread = new Thread(() -> {
            try {
                Connection connection = ManagementConnection.getInstance();
                connection.connect(host, port);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "unknown exception thrown");
                Log.e(TAG, e.getMessage());
            }
        });
        thread.start();

        Log.i(TAG, "Starting OpenVPN Service");
        return Service.START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(@Nullable Intent intent) {
        Log.i(TAG, "onBind");
        if ((intent != null) && START_SERVICE_NOT_STICKY.equals(intent.getAction())) {
            return mBinder;
        }
        return null;
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
            String text = getString(R.string.statusline_bytecount, sIn, sDiffIn, sOut, sDiffOut);
            String title = getString(R.string.notifcation_title, getString(R.string.state_connected));
            showNotification(title, text, null, NOTIFICATION_CHANNEL_BG_ID, mConnectTime, ConnectionStatus.LEVEL_CONNECTED);
        }
    }
// todo check wich thread these run on
    @Override
    public void onConnectError(@NonNull Throwable e) {
        // TODO
        endVpnService();
    }

    @Override
    public void onConnected() {
        // Start a background thread that handles incoming messages of the management interface
        Connection connection = ManagementConnection.getInstance();
        Thread thread = new Thread(connection, "OpenVPNManagementThread");
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
        // doSendBroadcast(name, message);
        if (mNotificationAlwaysVisible) {
            // Display byte count only after being connected
            if (level == ConnectionStatus.LEVEL_CONNECTED) {
                mDisplayByteCount = true;
                mConnectTime = System.currentTimeMillis();
            } else {
                mDisplayByteCount = false;
            }
            String tickerText = getString(getLocalizedState(name));
            String title = getString(R.string.notifcation_title, tickerText);
            String text = message;
            // (x) optional address of remote server (OpenVPN 2.1 or higher)
            // (y) optional port of remote server (OpenVPN 2.4 or higher)
            // (x) and (y) are shown for ASSIGN_IP and CONNECTED states
            if (VpnStatus.ASSIGN_IP.equals(name) || VpnStatus.CONNECTED.equals(name)) {
                if ((address != null) && !address.isEmpty()) {
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
            Log.e(TAG, e.getMessage());
        }
        return result;
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private void addVpnActionsToNotification(Builder builder) {
        Intent intent = new Intent(this, DisconnectVPN.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // The notification action icons are still required and continue to be used on older versions of Android
        builder.addAction(R.drawable.ic_close_white_24dp, getString(R.string.cancel_connection), pendingIntent);
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
            channel.setLightColor(Color.DKGRAY); //todo why setcolor
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
        // TODO
        Intent intent = new Intent();
        intent.setAction(ACTION_VPN_STATUS);
        intent.putExtra("state", state);
        intent.putExtra("message", message);
        sendBroadcast(intent);
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
                builder.setPriority(NotificationCompat.PRIORITY_MIN);
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

    @SuppressWarnings({ "MethodWithMultipleReturnPoints", "UnnecessaryDefault" })
    private static int getIconByConnectionStatus(ConnectionStatus level) {
        switch (level) {
            case LEVEL_AUTH_FAILED:
                return R.drawable.ic_stat_vpn_outline;
            case LEVEL_NOT_CONNECTED:
                return R.drawable.ic_stat_vpn_offline;
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_CONNECTING_SERVER_REPLIED:
                return R.drawable.ic_stat_vpn_empty_halo;
            case LEVEL_CONNECTED:
            case UNKNOWN_LEVEL:
            default:
                return R.drawable.ic_stat_vpn;
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
        String roundFormat = "%.0f";
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
