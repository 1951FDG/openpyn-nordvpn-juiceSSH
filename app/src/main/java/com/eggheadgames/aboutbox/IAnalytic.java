package com.eggheadgames.aboutbox;

import androidx.annotation.NonNull;

public interface IAnalytic {
    void logUiEvent(@NonNull String action, @NonNull String label);

    void logException(@NonNull Exception e, boolean fatal);
}

