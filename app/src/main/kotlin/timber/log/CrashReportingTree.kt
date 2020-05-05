package timber.log

import com.crashlytics.android.Crashlytics
import timber.log.Timber.DebugTree

class CrashReportingTree : DebugTree() {
    override fun v(message: String?, vararg args: Any?) {
        // NOP
    }

    override fun v(t: Throwable?, message: String?, vararg args: Any?) {
        // NOP
    }

    override fun v(t: Throwable?) {
        // NOP
    }

    override fun d(message: String?, vararg args: Any?) {
        // NOP
    }

    override fun d(t: Throwable?, message: String?, vararg args: Any?) {
        // NOP
    }

    override fun d(t: Throwable?) {
        // NOP
    }

    override fun log(priority: Int, message: String?, vararg args: Any) {
        prepareLog(priority, null, message, *args)
    }

    override fun log(priority: Int, t: Throwable?, message: String?, vararg args: Any) {
        prepareLog(priority, t, message, *args)
    }

    override fun log(priority: Int, t: Throwable?) {
        prepareLog(priority, t, null)
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority == android.util.Log.ERROR
    }

    private fun prepareLog(priority: Int, t: Throwable?, message: String?, vararg args: Any) {
        // Consume tag even when message is not loggable so that next message is correctly tagged.
        val tag = this.tag

        if (!isLoggable(tag, priority)) {
            return
        }

        var msg = message.orEmpty()

        if (msg.isEmpty()) {
            // Swallow message if it's empty and there's no throwable.
            if (t == null) {
                return
            }
        } else {
            if (!args.isNullOrEmpty()) {
                msg = formatMessage(msg, args)
            }
        }

        log(priority, tag, msg, t)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t == null) {
            return
        }

        try {
            // TODO: Add more custom keys
            // Crashlytics supports a maximum of 64 key/value pairs
            Crashlytics.setString(PRIORITY_KEY, "Error")
            tag?.let { Crashlytics.setString(TAG_KEY, tag) }
            Crashlytics.setString(MESSAGE_KEY, message)
            Crashlytics.logException(t)
        } catch (ignored: IllegalStateException) {
            // Must Initialize Fabric before using singleton()
        }
    }

    companion object {
        const val PRIORITY_KEY: String = "priority"
        const val TAG_KEY: String = "tag"
        const val MESSAGE_KEY: String = "message"
    }
}
