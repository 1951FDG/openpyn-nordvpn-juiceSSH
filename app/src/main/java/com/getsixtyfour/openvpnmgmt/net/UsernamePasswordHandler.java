package com.getsixtyfour.openvpnmgmt.net;

import org.jetbrains.annotations.NotNull;

/**
 * @author 1951FDG
 */

@SuppressWarnings("WeakerAccess")
public interface UsernamePasswordHandler {

    @NotNull
    String getUserName();

    @NotNull
    String getUserPass();
}
