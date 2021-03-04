package com.getsixtyfour.openvpnmgmt.core;

import org.jetbrains.annotations.Nullable;

/**
 * @author 1951FDG
 */

public interface UsernamePasswordHandler {

    @Nullable CharSequence getUser();

    @Nullable CharSequence getPassword();
}
