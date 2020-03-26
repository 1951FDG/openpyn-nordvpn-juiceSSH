package com.getsixtyfour.openvpnmgmt.net;

import org.jetbrains.annotations.NotNull;

/**
 * @author 1951FDG
 */

public interface UsernamePasswordHandler {

    @NotNull
    String getUserName();

    @NotNull
    String getUserPass();
}
