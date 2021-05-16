package io.github.getsixtyfour.timber

import android.content.Context
import android.os.Handler
import android.os.Looper
import info.hannes.logcat.Event
import info.hannes.timber.FileLoggingTree
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebugFileLoggingTree(externalCacheDir: File, context: Context?) : FileLoggingTree(externalCacheDir, context) {

    @Suppress("HardCodedStringLiteral", "MagicNumber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val logTimeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val priorityText = when (priority) {
                2 -> "V:"
                3 -> "D:"
                4 -> "I:"
                5 -> "W:"
                6 -> "E:"
                7 -> "A:"
                else -> "$priority"
            }
            val tagText = tag?.substring(tag.lastIndexOf('.') + 1)
            val writer = FileWriter(file, true)
            val textLine = "$priorityText $logTimeStamp $tagText $message\n"
            writer.append(textLine)
            writer.flush()
            writer.close()

            if (Looper.getMainLooper().thread == Thread.currentThread()) {
                _lastLogEntry.value = Event(textLine)
            } else {
                Handler(Looper.getMainLooper()).post { _lastLogEntry.value = Event(textLine) }
            }
        } catch (ignored: Exception) {
        }
    }
}
