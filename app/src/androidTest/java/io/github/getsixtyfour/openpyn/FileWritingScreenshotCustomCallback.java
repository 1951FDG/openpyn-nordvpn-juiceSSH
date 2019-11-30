package io.github.getsixtyfour.openpyn;

import android.content.Context;

import java.io.File;

import tools.fastlane.screengrab.FileWritingScreenshotCallback;

public class FileWritingScreenshotCustomCallback extends FileWritingScreenshotCallback {

    public FileWritingScreenshotCustomCallback(Context appContext) {
        super(appContext);
    }

    @Override
    protected File getScreenshotFile(File screenshotDirectory, String screenshotName) {
        String screenshotFileName = screenshotName + ".png";
        return new File(screenshotDirectory, screenshotFileName);
    }
}
