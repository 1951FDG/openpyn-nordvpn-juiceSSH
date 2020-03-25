/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

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
    LEVEL_UNKNOWN
}
