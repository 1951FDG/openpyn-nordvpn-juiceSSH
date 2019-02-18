package io.github.getsixtyfour.openpyn;

import android.content.Context;
import android.graphics.Bitmap;
import tools.fastlane.screengrab.FileWritingScreenshotCallback;

import java.io.File;

public class FileWritingScreenshotCustomCallback extends FileWritingScreenshotCallback {
    public FileWritingScreenshotCustomCallback(Context appContext) {
        super(appContext);
    }

    @Override
    public void screenshotCaptured(String screenshotName, Bitmap screenshot) {
        super.screenshotCaptured(screenshotName, screenshot);
    }

    @Override
    protected File getScreenshotFile(File screenshotDirectory, String screenshotName) {
        String screenshotFileName = screenshotName + ".png";
        return new File(screenshotDirectory, screenshotFileName);
    }
}
