/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.getsixtyfour.openvpnmgmt.android.core;

/**
 * @author Arne Schwabe
 */

interface IOpenVPNServiceInternal {
    boolean stopVPN(boolean replaceConnection);
}
