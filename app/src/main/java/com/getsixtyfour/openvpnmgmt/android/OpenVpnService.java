package com.getsixtyfour.openvpnmgmt.android;

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
import android.os.Process;
import android.os.NetworkOnMainThreadException;
import android.text.format.DateUtils;

import androidx.annotation.CheckResult;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.getsixtyfour.openvpnmgmt.api.Connection;
import com.getsixtyfour.openvpnmgmt.core.ConnectionStatus;
import com.getsixtyfour.openvpnmgmt.model.OpenVpnLogRecord;
import com.getsixtyfour.openvpnmgmt.model.OpenVpnNetworkState;
import com.getsixtyfour.openvpnmgmt.core.VpnStatus;
import com.getsixtyfour.openvpnmgmt.listeners.ConnectionListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnByteCountChangedListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnStateChangedListener;
import com.getsixtyfour.openvpnmgmt.net.ManagementConnection;
import com.getsixtyfour.openvpnmgmt.utils.StringUtils;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Locale;

import org.jetbrains.annotations.NonNls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.getsixtyfour.openpyn.R;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

@SuppressWarnings({ "OverlyComplexClass", "ClassWithTooManyDependencies" })
public final class OpenVpnService extends Service
        implements ConnectionListener, OnByteCountChangedListener, OnStateChangedListener, UncaughtExceptionHandler {

    @NonNls
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenVpnService.class);

    private static final int DEFAULT_REMOTE_PORT = 23;

    private static final String DEFAULT_REMOTE_SERVER = "192.168.1.1";

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

    private boolean mPostByteCountNotification = false;

    private boolean mPostStateNotification = false;

    // TODO: test
    private boolean mSendStateBroadcast = false;

    /**
     * the connection start time in UTC milliseconds (could be some time in the past)
     */
    private long mStartTime;

    @Nullable
    private Thread mThread = null;

    private int mStartId;

    @CheckResult
    @DrawableRes
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

    @CheckResult
    @StringRes
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

    @CheckResult
    @NonNull
    @SuppressWarnings({ "OverlyComplexMethod", "MagicNumber", "ImplicitNumericConversion", "WeakerAccess" })
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
    private Notification createNotification(@NonNull Context context, @NonNull String title, @NonNull String text, @NonNull String channel,
                                            long when, @DrawableRes int icon) {
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

        if (!mPostByteCountNotification || Constants.BG_CHANNEL_ID.equals(channel)) {
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
    private static void createNotificationChannels(@NonNull Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // Real-time notification of OpenVPN bandwidth usage
        {
            String name = context.getString(R.string.vpn_channel_name_background);
            String description = context.getString(R.string.vpn_channel_description_background);
            NotificationChannel channel = new NotificationChannel(Constants.BG_CHANNEL_ID, name, NotificationManagerCompat.IMPORTANCE_LOW);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }
        // Real-time notification of OpenVPN state changes
        {
            String name = context.getString(R.string.vpn_channel_name_status);
            String description = context.getString(R.string.vpn_channel_description_status);
            NotificationChannel channel = new NotificationChannel(Constants.NEW_STATUS_CHANNEL_ID, name,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onCreate() {
        LOGGER.debug("onCreate");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels(this);
        }

        Connection connection = ManagementConnection.getInstance();
        connection.addOnByteCountChangedListener(this);
        connection.addOnRecordChangedListener(OpenVpnService::onRecordChanged);
        connection.addOnStateChangedListener(this);
        connection.setConnectionListener(this);
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        LOGGER.debug("onStartCommand");
        if (intent == null) {
            throw new IllegalArgumentException("intent can't be null");
        }

        if (mStartId > 0) {
            return START_NOT_STICKY;
        }

        mStartId = startId;

        mPostByteCountNotification = intent.getBooleanExtra(Constants.EXTRA_POST_BYTE_COUNT_NOTIFICATION, false);
        mPostStateNotification = intent.getBooleanExtra(Constants.EXTRA_POST_STATE_NOTIFICATION, false);
        mSendStateBroadcast = intent.getBooleanExtra(Constants.EXTRA_SEND_STATE_BROADCAST, false);

        String host = StringUtils.defaultIfBlank(intent.getStringExtra(Constants.EXTRA_HOST), DEFAULT_REMOTE_SERVER);
        int port = intent.getIntExtra(Constants.EXTRA_PORT, DEFAULT_REMOTE_PORT);
        char[] password = intent.getCharArrayExtra(Constants.EXTRA_PASSWORD);

        // Always show notification here to avoid problem with startForeground timeout
        int icon = getIconByConnectionStatus(ConnectionStatus.LEVEL_NOT_CONNECTED);
        String text = getString(R.string.vpn_msg_launch);
        String title = getString(R.string.vpn_title_status, getString(R.string.vpn_state_disconnected));

        Notification notification = createNotification(this, title, text, Constants.NEW_STATUS_CHANNEL_ID, System.currentTimeMillis(), icon);
        startForeground(Constants.NEW_STATUS_NOTIFICATION_ID, notification);

        // Connect the management interface in a background thread
        Thread thread = new Thread(() -> {
            try {
                // TODO: only in debug?
                // When a socket is created, it inherits the tag of its creating thread
                /*TrafficStats.setThreadStatsTag(Constants.THREAD_STATS_TAG);*/
                Connection connection = ManagementConnection.getInstance();
                connection.connect(host, port, password);
            } catch (IOException ignored) {
            }
        });
        thread.start();

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(@Nullable Intent intent) {
        LOGGER.debug("onBind");
        return (intent != null) ? mBinder : null;
    }

    @Override
    public boolean onUnbind(@Nullable Intent intent) {
        LOGGER.debug("onUnbind");
        return false;
    }

    @Override
    public void onDestroy() {
        LOGGER.debug("onDestroy");

        // TODO: only in debug?
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            long now = System.currentTimeMillis();
            long fourWeeksAgo = now - (DateUtils.WEEK_IN_MILLIS * 4L);
            long oneDaysAhead = now + (DateUtils.DAY_IN_MILLIS * 2L);
            int uid = Process.myUid();
            try {
                long usage = Utils.getTotalUsage(this, fourWeeksAgo, oneDaysAhead, uid, Constants.THREAD_STATS_TAG);
                // long usage = Utils.getTotalUsage(this, fourWeeksAgo, oneDaysAhead, uid, android.app.usage.NetworkStats.Bucket.TAG_NONE);
                LOGGER.info("Usage: {}", humanReadableByteCount(this, usage, false));
            } catch (SecurityException e) {
                LOGGER.error("Exception querying network detail.", e);
            }
        }*/

        if (mThread != null) {
            mThread.setUncaughtExceptionHandler(null);
            mThread = null;
        }

        Connection connection = ManagementConnection.getInstance();
        connection.clearOnByteCountChangedListeners();
        connection.clearOnRecordChangedListeners();
        connection.clearOnStateChangedListeners();
        connection.setConnectionListener(null);

        connection.disconnect();
    }

    @Override
    public void onConnectError(@NonNull Thread t, @NonNull Throwable e) {
        LOGGER.debug("onConnectError");
        if (t.equals(getMainLooper().getThread())) {
            LOGGER.error("", new NetworkOnMainThreadException());
        }

        uncaughtException(t, e);
    }

    @Override
    public void onConnected() {
        LOGGER.debug("onConnected");
        if (Thread.currentThread().equals(getMainLooper().getThread())) {
            LOGGER.error("", new NetworkOnMainThreadException());
        }

        // Start a background thread that handles incoming messages of the management interface
        Connection connection = ManagementConnection.getInstance();
        mThread = new Thread(connection, Constants.THREAD_NAME);
        // Report death-by-uncaught-exception
        mThread.setUncaughtExceptionHandler(this); // Apps can replace the default handler, but not the pre handler
        mThread.start();

        LOGGER.info("OpenVPN Management started in background thread: \"{}\"", mThread.getName());
    }

    @Override
    public void onDisconnected() {
        LOGGER.debug("onDisconnected");
        if (Thread.currentThread().equals(getMainLooper().getThread())) {
            LOGGER.warn("", new NetworkOnMainThreadException());
        }
    }

    @Override
    public void onByteCountChanged(long in, long out, long diffIn, long diffOut) {
        if (!mPostByteCountNotification) {
            return;
        }

        long byteCountInterval = ManagementConnection.BYTE_COUNT_INTERVAL.longValue();
        String strIn = humanReadableByteCount(this, in, false);
        String strDiffIn = humanReadableByteCount(this, diffIn / byteCountInterval, true);
        String strOut = humanReadableByteCount(this, out, false);
        String strDiffOut = humanReadableByteCount(this, diffOut / byteCountInterval, true);
        int icon = getIconByConnectionStatus(ConnectionStatus.LEVEL_CONNECTED);
        String text = getString(R.string.vpn_msg_byte_count, strIn, strDiffIn, strOut, strDiffOut);
        String title = getString(R.string.vpn_title_status, getString(R.string.vpn_state_connected));

        Notification notification = createNotification(this, title, text, Constants.BG_CHANNEL_ID, mStartTime, icon);
        startForeground(Constants.BG_NOTIFICATION_ID, notification);
    }

    private static void onRecordChanged(@NonNull OpenVpnLogRecord record) {
        @NonNls Logger logger = LoggerFactory.getLogger("OpenVPN"); //NON-NLS

        switch (record.getLevel()) {
            case ERROR:
                if (logger.isErrorEnabled()) {
                    logger.error(record.getMessage());
                }
                break;
            case WARNING:
                if (logger.isWarnEnabled()) {
                    logger.warn(record.getMessage());
                }
                break;
            case INFO:
                if (logger.isInfoEnabled()) {
                    logger.info(record.getMessage());
                }
                break;
            case DEBUG:
                if (logger.isDebugEnabled()) {
                    logger.debug(record.getMessage());
                }
                break;
            case VERBOSE:
            case UNKNOWN:
        }
    }

    @Override
    public void onStateChanged(@NonNull OpenVpnNetworkState state) {
        @NonNls String name = state.getName();
        @NonNls String message = state.getDescription();
        @NonNls String address = state.getRemoteAddress();
        @NonNls String port = state.getRemotePort();

        if (mSendStateBroadcast) {
            Utils.doSendBroadcast(this, name, message);
        }

        ConnectionStatus level = VpnStatus.getLevel(name, message);
        boolean isConnected = level == ConnectionStatus.LEVEL_CONNECTED;
        boolean isDisconnected = level == ConnectionStatus.LEVEL_NOT_CONNECTED;

        if (isConnected) {
            mStartTime = state.getMillis();
        }

        if (mPostStateNotification || isConnected || isDisconnected) {
            int icon = getIconByConnectionStatus(level);
            String text = message;
            String title = getString(R.string.vpn_title_status, getString(getLocalizedState(name)));
            // (x) optional address of remote server (OpenVPN 2.1 or higher)
            // (y) optional port of remote server (OpenVPN 2.4 or higher)
            // (x) and (y) are shown for ASSIGN_IP and CONNECTED states
            if ((VpnStatus.ASSIGN_IP.equals(name) || VpnStatus.CONNECTED.equals(name)) && !address.isEmpty()) {
                @NonNls String prefix = null;
                if (!port.isEmpty()) {
                    prefix = "1194".equals(port) ? "UDP" : "TCP";
                    prefix += ": ";
                }
                text = prefix + address;
            }

            Notification notification = createNotification(this, title, text, Constants.NEW_STATUS_CHANNEL_ID, state.getMillis(), icon);
            startForeground(Constants.NEW_STATUS_NOTIFICATION_ID, notification);
        }
    }

    @SuppressWarnings({ "HardCodedStringLiteral", "HardcodedLineSeparator", "StringBufferWithoutInitialCapacity", "unused" })
    private static void logUncaught(@NonNull String threadName, @Nullable String processName, int pid, @NonNull Throwable e) {
        StringBuilder message = new StringBuilder();
        // The "FATAL EXCEPTION" string is still used on Android even though apps can set a custom
        // UncaughtExceptionHandler that renders uncaught exceptions non-fatal
        message.append("FATAL EXCEPTION: ").append(threadName).append("\n");
        if (processName != null) {
            message.append("Process: ").append(processName).append(", ");
        }
        message.append("PID: ").append(pid);
        LOGGER.error(message.toString(), e);
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        // Logs a message when a thread encounters an uncaught exception
        // logUncaught(t.getName(), getPackageName(), Process.myPid(), e); // Handled by the pre handler

        if (e instanceof ThreadDeath) {
            LOGGER.info("OpenVPN Management stopped in background thread: \"{}\"", t.getName());
        }

        if (t.equals(getMainLooper().getThread())) {
            stopSelf();
        } else {
            new Handler(getMainLooper()).post(this::stopSelf);
        }
    }
}
