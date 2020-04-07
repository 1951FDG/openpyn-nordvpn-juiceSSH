package io.github.getsixtyfour.openpyn;

import android.content.Context;

import java.io.File;

import org.jetbrains.annotations.NonNls;

import tools.fastlane.screengrab.FileWritingScreenshotCallback;

public class FileWritingScreenshotCustomCallback extends FileWritingScreenshotCallback {

    public FileWritingScreenshotCustomCallback(Context appContext) {
        super(appContext);
    }

    @Override
    protected File getScreenshotFile(File screenshotDirectory, String screenshotName) {
        @NonNls String screenshotFileName = screenshotName + ".png";
        return new File(screenshotDirectory, screenshotFileName);
    }
}
