package io.github.getsixtyfour.openpyn

import android.content.Context
import org.jetbrains.annotations.NonNls
import tools.fastlane.screengrab.FileWritingScreenshotCallback
import java.io.File

class FileWritingScreenshotCustomCallback(appContext: Context?) : FileWritingScreenshotCallback(appContext) {

    override fun getScreenshotFile(screenshotDirectory: File, screenshotName: String): File {
        @NonNls val screenshotFileName = "$screenshotName.png"
        return File(screenshotDirectory, screenshotFileName)
    }
}
