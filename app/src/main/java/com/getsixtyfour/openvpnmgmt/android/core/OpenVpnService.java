package com.getsixtyfour.openvpnmgmt.android.core;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.getsixtyfour.openvpnmgmt.core.ConnectionStatus;
import com.getsixtyfour.openvpnmgmt.core.LogLevel;
import com.getsixtyfour.openvpnmgmt.core.LogManager.OpenVpnLogRecord;
import com.getsixtyfour.openvpnmgmt.core.StateManager.OpenVpnNetworkState;
import com.getsixtyfour.openvpnmgmt.core.VpnStatus;
import com.getsixtyfour.openvpnmgmt.listeners.ConnectionListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnByteCountChangedListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnRecordChangedListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnStateChangedListener;
import com.getsixtyfour.openvpnmgmt.api.Connection;
import com.getsixtyfour.openvpnmgmt.net.ManagementConnection;
import com.getsixtyfour.openvpnmgmt.utils.StringUtils;

import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Locale;

import io.github.getsixtyfour.openpyn.R;

// TODO: change listeners, move them to separate package maybe rename listeners IOpenVpnStateListener

/**
 * @author 1951FDG
 */

@SuppressWarnings({ "OverlyComplexClass", "ClassWithTooManyDependencies" })
public final class OpenVpnService extends Service
        implements OnRecordChangedListener, OnStateChangedListener, OnByteCountChangedListener, ConnectionListener,
        UncaughtExceptionHandler {

    private static final String TAG = "OpenVpnService";

    private static final int DEFAULT_REMOTE_PORT = 23;

    private static final String DEFAULT_REMOTE_SERVER = "127.0.0.1";

    private final IBinder mBinder = new IOpenVpnServiceInternal.Stub() {
        @Override
        public void disconnectVpn() {
            try {
                Connection connection = ManagementConnection.getInstance();
                if (connection.isConnected()) {
                    connection.stopVpn();
                }
            } catch (IOException e) {
                //noinspection ProhibitedExceptionThrown
                throw new RuntimeException(e);
            }
        }
    };

    private final boolean mDisplayByteCount = true;

    private boolean mPostNotification = false;

    // TODO: test
    private boolean mSendBroadcast = false;

    /**
     * the connection start time in UTC milliseconds (could be some time in the past)
     */
    private long mStartTime;

    @Nullable
    private Thread mThread = null;

    @SuppressWarnings({ "MethodWithMultipleReturnPoints", "WeakerAccess" })
    public static int getIconByConnectionStatus(@NonNull ConnectionStatus level) {
        switch (level) {
            case LEVEL_CONNECTED:
                return R.drawable.ic_stat_shield;
            case LEVEL_CONNECTING_SERVER_REPLIED:
                return R.drawable.ic_stat_shield_check_outline;
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_NOT_CONNECTED:
            case LEVEL_AUTH_FAILED:
            case LEVEL_UNKNOWN:
            default:
                return R.drawable.ic_stat_shield_outline;
        }
    }

    @SuppressWarnings({ "MethodWithMultipleReturnPoints", "OverlyComplexMethod", "OverlyLongMethod", "WeakerAccess" })
    public static int getLocalizedState(@NonNull String state) {
        switch (state) {
            case VpnStatus.ADD_ROUTES:
                return R.string.vpn_state_add_routes;
            case VpnStatus.ASSIGN_IP:
                return R.string.vpn_state_assign_ip;
            case VpnStatus.AUTH:
                return R.string.vpn_state_auth;
            case VpnStatus.AUTH_FAILED:
                return R.string.vpn_state_auth_failed;
            case VpnStatus.AUTH_PENDING:
                return R.string.vpn_state_auth_pending;
            case VpnStatus.CONNECTED:
                return R.string.vpn_state_connected;
            case VpnStatus.CONNECTING:
                return R.string.vpn_state_connecting;
            case VpnStatus.DISCONNECTED:
                return R.string.vpn_state_disconnected;
            case VpnStatus.EXITING:
                return R.string.vpn_state_exiting;
            case VpnStatus.GET_CONFIG:
                return R.string.vpn_state_get_config;
            case VpnStatus.RECONNECTING:
                return R.string.vpn_state_reconnecting;
            case VpnStatus.RESOLVE:
                return R.string.vpn_state_resolve;
            case VpnStatus.TCP_CONNECT:
                return R.string.vpn_state_tcp_connect;
            case VpnStatus.WAIT:
                return R.string.vpn_state_wait;
            default:
                return R.string.vpn_state_unknown;
        }
    }

    @NonNull
    @SuppressWarnings({ "OverlyComplexMethod", "MagicNumber", "ImplicitNumericConversion", "BooleanParameter", "WeakerAccess" })
    public static String humanReadableByteCount(@NonNull Context context, long bytes, boolean speed) {
        Resources res = context.getResources();
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

    @NonNull
    @SuppressWarnings({ "TypeMayBeWeakened", "MethodWithTooManyParameters" })
    private static Notification getNotification(@NonNull Context context, @NonNull String title, @NonNull String text,
                                                @NonNull String channel, long when, @IdRes int icon) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);
        builder.setContentText(text);
        builder.setContentTitle(title);
        builder.setLocalOnly(true);
        builder.setOngoing(true);
        builder.setOnlyAlertOnce(true);
        builder.setShowWhen(false);
        builder.setSmallIcon(icon);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setWhen(when);
        if (Constants.BG_CHANNEL_ID.equals(channel)) {
            Intent intent = new Intent(context, DisconnectActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            // The notification action icons are still required and continue to be used on older versions of Android
            builder.addAction(R.drawable.ic_close_white, context.getString(R.string.vpn_action_close), pendingIntent);
            builder.setUsesChronometer(true);
        }
        return builder.build();
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(Build.VERSION_CODES.O)
    private static void setUpNotificationChannels(@NonNull Context context) {
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);
        // Real-time notification of OpenVPN bandwidth usage
        {
            String name = context.getString(R.string.vpn_channel_name_background);
            String description = context.getString(R.string.vpn_channel_description_background);
            NotificationChannel channel = new NotificationChannel(Constants.BG_CHANNEL_ID, name, NotificationManagerCompat.IMPORTANCE_LOW);
            channel.setDescription(description);
            mNotificationManager.createNotificationChannel(channel);
        }
        // Real-time notification of OpenVPN state changes
        {
            String name = context.getString(R.string.vpn_channel_name_status);
            String description = context.getString(R.string.vpn_channel_description_status);
            NotificationChannel channel = new NotificationChannel(Constants.NEW_STATUS_CHANNEL_ID, name,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT);
            channel.setDescription(description);
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    private static boolean isMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setUpNotificationChannels(this);
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
        Log.d(TAG, "onStartCommand"); //NON-NLS
        if ((intent != null) && Constants.ACTION_START_SERVICE_NOT_STICKY.equals(intent.getAction())) {
            return START_NOT_STICKY;
        }
        if ((intent != null) && Constants.ACTION_START_SERVICE_STICKY.equals(intent.getAction())) {
            return START_REDELIVER_INTENT;
        }
        if (intent == null) {
            throw new IllegalArgumentException("intent can't be null");
        }

        mPostNotification = intent.getBooleanExtra(Constants.EXTRA_POST_NOTIFICATION, false);
        mSendBroadcast = intent.getBooleanExtra(Constants.EXTRA_SEND_BROADCAST, false);

        String host = StringUtils.defaultIfBlank(intent.getStringExtra(Constants.EXTRA_HOST), DEFAULT_REMOTE_SERVER);
        int port = intent.getIntExtra(Constants.EXTRA_PORT, DEFAULT_REMOTE_PORT);

        // Always show notification here to avoid problem with startForeground timeout
        {
            int icon = getIconByConnectionStatus(ConnectionStatus.LEVEL_NOT_CONNECTED);
            String text = getString(R.string.vpn_msg_launch);
            String title = getString(R.string.vpn_title_status, getString(R.string.vpn_state_disconnected));

            Notification notification = getNotification(this, title, text, Constants.NEW_STATUS_CHANNEL_ID, 0L, icon);
            startForeground(Constants.NEW_STATUS_CHANNEL_ID.hashCode(), notification);
        }
        // Connect the management interface in a background thread
        Thread thread = new Thread(() -> {
            try {
                // TODO: only in debug?
                // When a socket is created, it inherits the tag of its creating thread
                // TrafficStats.setThreadStatsTag(Constants.THREAD_STATS_TAG);
                Connection connection = ManagementConnection.getInstance();
                //noinspection ConstantConditions
                connection.connect(host, port);
            } catch (IOException ignored) {
            }
        });
        thread.start();

        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(@Nullable Intent intent) {
        Log.d(TAG, "onBind"); //NON-NLS
        return ((intent != null) && Constants.ACTION_START_SERVICE_NOT_STICKY.equals(intent.getAction())) ? mBinder : null;
    }

    @Override
    public boolean onUnbind(@Nullable Intent intent) {
        Log.d(TAG, "onUnbind"); //NON-NLS
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy"); //NON-NLS

        // TODO: only in debug?
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        //     long now = System.currentTimeMillis();
        //     long fourWeeksAgo = now - (DateUtils.WEEK_IN_MILLIS * 4L);
        //     long oneDaysAhead = now + (DateUtils.DAY_IN_MILLIS * 2L);
        //     long usage = Utils.getTotalUsage(this, fourWeeksAgo, oneDaysAhead, android.os.Process.myUid(), Constants.THREAD_STATS_TAG);
        //     // long usage = Utils.getTotalUsage(this, fourWeeksAgo, oneDaysAhead, android.os.Process.myUid(), android.app.usage.NetworkStats.Bucket.TAG_NONE);
        //     Log.d(TAG, String.format("Usage: %s", humanReadableByteCount(this, usage, false))); //NON-NLS
        // }

        {
            if (mThread != null) {
                mThread.setUncaughtExceptionHandler(null);
                mThread = null;
            }

            // Disconnect the management interface in a background thread
            Thread thread = new Thread(() -> {
                Connection connection = ManagementConnection.getInstance();
                connection.disconnect();
            });
            thread.start();

            Connection connection = ManagementConnection.getInstance();
            connection.removeByteCountListener(this);
            connection.removeLogListener(this);
            connection.removeStateListener(this);
            connection.setConnectionListener(null);
        }
    }

    // TODO: make use mDisplayByteCount a settable option, must show disconnect then for status notifications...
    @Override
    public void onByteCountChanged(long in, long out, long diffIn, long diffOut) {
        if (mDisplayByteCount) {
            long byteCountInterval = ManagementConnection.BYTE_COUNT_INTERVAL.longValue();
            String sIn = humanReadableByteCount(this, in, false);
            String sDiffIn = humanReadableByteCount(this, diffIn / byteCountInterval, true);
            String sOut = humanReadableByteCount(this, out, false);
            String sDiffOut = humanReadableByteCount(this, diffOut / byteCountInterval, true);
            int icon = getIconByConnectionStatus(ConnectionStatus.LEVEL_CONNECTED);
            String text = getString(R.string.vpn_msg_byte_count, sIn, sDiffIn, sOut, sDiffOut);
            String title = getString(R.string.vpn_title_status, getString(R.string.vpn_state_connected));

            Notification notification = getNotification(this, title, text, Constants.BG_CHANNEL_ID, mStartTime, icon);
            startForeground(Constants.BG_CHANNEL_ID.hashCode(), notification);
        }
    }

    @Override
    public void onConnectError(@NonNull Throwable e) {
        Log.d(TAG, "onConnectError"); //NON-NLS
        if (isMainThread()) {
            Log.e(TAG, "", new NetworkOnMainThreadException());
        }

        uncaughtException(Thread.currentThread(), e);
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected"); //NON-NLS
        if (isMainThread()) {
            Log.e(TAG, "", new NetworkOnMainThreadException());
        }
        // Start a background thread that handles incoming messages of the management interface
        Connection connection = ManagementConnection.getInstance();
        mThread = new Thread(connection, Constants.THREAD_NAME);
        // Report death-by-uncaught-exception
        mThread.setUncaughtExceptionHandler(this);
        mThread.start();

        Log.i(TAG, String.format("OpenVPN Management started in background thread: \"%s\"", mThread.getName())); //NON-NLS
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected"); //NON-NLS
        if (isMainThread()) {
            Log.w(TAG, "", new NetworkOnMainThreadException());
        }
    }

    @Override
    public void onRecordChanged(@NonNull OpenVpnLogRecord record) {
        LogLevel value = record.getLevel();
        switch (value) {
            case ERROR:
                if (Log.isLoggable(TAG, Log.ERROR)) {
                    Log.e(TAG, record.getMessage());
                }
                break;
            case WARNING:
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, record.getMessage());
                }
                break;
            case INFO:
                if (Log.isLoggable(TAG, Log.INFO)) {
                    Log.i(TAG, record.getMessage());
                }
                break;
            case DEBUG:
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, record.getMessage());
                }
                break;
            case VERBOSE:
            case UNKNOWN:
                break;
        }
    }

    @Override
    public void onStateChanged(@NonNull OpenVpnNetworkState state) {
        @NonNls String name = state.getName();
        @NonNls String message = state.getDescription();
        @NonNls String address = state.getRemoteAddress();
        @NonNls String port = state.getRemotePort();

        ConnectionStatus level = VpnStatus.getLevel(name, message);
        if (mSendBroadcast) {
            Utils.doSendBroadcast(this, name, message);
        }
        if (mPostNotification) {
            if (level == ConnectionStatus.LEVEL_CONNECTED) {
                mStartTime = state.getMillis();
            }

            int icon = getIconByConnectionStatus(level);
            String text = message;
            String title = getString(R.string.vpn_title_status, getString(getLocalizedState(name)));
            // (x) optional address of remote server (OpenVPN 2.1 or higher)
            // (y) optional port of remote server (OpenVPN 2.4 or higher)
            // (x) and (y) are shown for ASSIGN_IP and CONNECTED states
            if ((VpnStatus.ASSIGN_IP.equals(name) || VpnStatus.CONNECTED.equals(name)) && !StringUtils.isBlank(address)) {
                @NonNls String prefix = null;
                if (!port.isEmpty()) {
                    prefix = "1194".equals(port) ? "UDP" : "TCP";
                    prefix += ": ";
                }
                text = prefix + address;
            }

            Notification notification = getNotification(this, title, text, Constants.NEW_STATUS_CHANNEL_ID, 0L, icon);
            startForeground(Constants.NEW_STATUS_CHANNEL_ID.hashCode(), notification);
        }
    }

    @SuppressWarnings({ "HardCodedStringLiteral", "HardcodedLineSeparator" })
    private static void logUncaught(@NonNull String threadName, @Nullable String processName, int pid, @NonNull Throwable e) {
        StringBuilder message = new StringBuilder();
        // The "FATAL EXCEPTION" string is still used on Android even though
        // apps can set a custom UncaughtExceptionHandler that renders uncaught
        // exceptions non-fatal.
        message.append("FATAL EXCEPTION: ").append(threadName).append("\n");
        if (processName != null) {
            message.append("Process: ").append(processName).append(", ");
        }
        message.append("PID: ").append(pid);
        Log.e(TAG, message.toString(), e);
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        // Apps can replace the default handler, but not the pre handler.
        // Logs a message when a thread encounters an uncaught exception. Handled by the pre handler.
        // logUncaught(t.getName(), getPackageName(), android.os.Process.myPid(), e);

        if (isMainThread()) {
            Log.e(TAG, "", new NetworkOnMainThreadException());
        }

        boolean background = t != getMainLooper().getThread();
        if (background && (e instanceof ThreadDeath)) {
            Log.i(TAG, String.format("OpenVPN Management stopped in background thread: \"%s\"", t.getName())); //NON-NLS
        }

        Handler handler = new Handler(getMainLooper());
        handler.post(this::stopSelf);
    }
}
