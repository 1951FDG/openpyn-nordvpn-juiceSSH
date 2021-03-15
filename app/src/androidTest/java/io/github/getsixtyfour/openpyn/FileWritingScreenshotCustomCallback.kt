package io.github.getsixtyfour.openpyn

import android.content.Context
import tools.fastlane.screengrab.FileWritingScreenshotCallback
import java.io.File

class FileWritingScreenshotCustomCallback(appContext: Context?) : FileWritingScreenshotCallback(appContext) {

    override fun getScreenshotFile(screenshotDirectory: File, screenshotName: String): File {
        val screenshotFileName = "$screenshotName.png"
        return File(screenshotDirectory, screenshotFileName)
    }
}
