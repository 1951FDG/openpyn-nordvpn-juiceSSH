package timber.log

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber.Tree

open class CrashReportingTree : Tree() {

    override fun v(message: String, vararg args: Any) {
        // NOP
    }

    override fun v(t: Throwable, message: String, vararg args: Any) {
        // NOP
    }

    override fun v(t: Throwable) {
        // NOP
    }

    override fun d(message: String, vararg args: Any) {
        // NOP
    }

    override fun d(t: Throwable, message: String, vararg args: Any) {
        // NOP
    }

    override fun d(t: Throwable) {
        // NOP
    }

    override fun i(message: String, vararg args: Any) {
        // NOP
    }

    override fun i(t: Throwable, message: String, vararg args: Any) {
        // NOP
    }

    override fun i(t: Throwable) {
        // NOP
    }

    override fun w(message: String, vararg args: Any) {
        // NOP
    }

    override fun w(t: Throwable, message: String, vararg args: Any) {
        // NOP
    }

    override fun w(t: Throwable) {
        // NOP
    }

    override fun e(message: String, vararg args: Any) {
        prepareLog(Log.ERROR, null, message, *args)
    }

    override fun e(t: Throwable, message: String, vararg args: Any) {
        prepareLog(Log.ERROR, t, message, *args)
    }

    override fun e(t: Throwable) {
        prepareLog(Log.ERROR, t, null)
    }

    override fun wtf(message: String, vararg args: Any) {
        prepareLog(Log.ERROR, null, message, *args)
    }

    override fun wtf(t: Throwable, message: String, vararg args: Any) {
        prepareLog(Log.ERROR, t, message, *args)
    }

    override fun wtf(t: Throwable) {
        prepareLog(Log.ERROR, t, null)
    }

    override fun log(priority: Int, message: String, vararg args: Any) {
        prepareLog(priority, null, message, *args)
    }

    override fun log(priority: Int, t: Throwable, message: String, vararg args: Any) {
        prepareLog(priority, t, message, *args)
    }

    override fun log(priority: Int, t: Throwable) {
        prepareLog(priority, t, null)
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority == Log.ERROR
    }

    protected fun prepareLog(priority: Int, t: Throwable?, message: String?, vararg args: Any) {
        // Consume tag even when message is not loggable so that next message is correctly tagged
        val tag = tag

        if (t == null) {
            return
        }

        if (!isLoggable(tag, priority)) {
            return
        }

        log(priority, tag, formatMessage(message.orEmpty(), *args), t)
    }

    override fun formatMessage(message: String, vararg args: Any): String {
        return when {
            message.isEmpty() -> message
            message.isBlank() -> message
            args.isNotEmpty() -> message.format(*args)
            else -> message
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            // TODO: Add more custom keys
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey(PRIORITY_KEY, "Error")
            tag?.let { crashlytics.setCustomKey(TAG_KEY, it) }
            crashlytics.setCustomKey(MESSAGE_KEY, message)
            t?.let(crashlytics::recordException)
        } catch (ignored: IllegalStateException) {
        }
    }

    companion object {

        const val PRIORITY_KEY: String = "priority"
        const val TAG_KEY: String = "tag"
        const val MESSAGE_KEY: String = "message"
    }
}
