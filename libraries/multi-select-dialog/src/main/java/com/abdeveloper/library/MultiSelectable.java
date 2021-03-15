package com.abdeveloper.library;

import androidx.annotation.Nullable;

public interface MultiSelectable extends Cloneable, Identifiable, Nameable {

    @Nullable
    MultiSelectable clone();
}
