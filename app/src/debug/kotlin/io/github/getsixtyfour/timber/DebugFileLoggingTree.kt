package io.github.getsixtyfour.timber

import android.annotation.SuppressLint
import android.content.Context
import info.hannes.timber.FileLoggingTree
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Suppress("unused")
@SuppressLint("LogNotTimber")
class DebugFileLoggingTree(externalCacheDir: File, context: Context? = null, filename: String = UUID.randomUUID().toString()) :
    FileLoggingTree(externalCacheDir, context, filename) {

    @Suppress("HardCodedStringLiteral", "MagicNumber")
    @SuppressLint("LogNotTimber")
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
            lastLogEntry.value = textLine
        } catch (ignored: Exception) {
        }
    }
}
