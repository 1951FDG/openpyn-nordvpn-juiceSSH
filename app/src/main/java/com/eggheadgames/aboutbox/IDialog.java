package com.eggheadgames.aboutbox;

import android.app.Activity;
import androidx.annotation.NonNull;

public interface IDialog {
    void open(@NonNull Activity activity, @NonNull String url, @NonNull String tag);
}
