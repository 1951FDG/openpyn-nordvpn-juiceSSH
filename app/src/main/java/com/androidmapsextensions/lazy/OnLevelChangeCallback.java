package com.androidmapsextensions.lazy;

import androidx.annotation.NonNull;

public interface OnLevelChangeCallback {

    void onLevelChange(@NonNull LazyMarker marker, int level);
}
