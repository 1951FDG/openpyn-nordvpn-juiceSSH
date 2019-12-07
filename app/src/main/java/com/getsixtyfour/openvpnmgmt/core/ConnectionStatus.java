package com.getsixtyfour.openvpnmgmt.core;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

public enum ConnectionStatus {
    LEVEL_CONNECTED,
    LEVEL_CONNECTING_SERVER_REPLIED,
    LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
    LEVEL_NOT_CONNECTED,
    LEVEL_AUTH_FAILED,
    UNKNOWN_LEVEL
}
