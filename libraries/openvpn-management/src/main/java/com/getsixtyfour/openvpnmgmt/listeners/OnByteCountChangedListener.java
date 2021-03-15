package com.getsixtyfour.openvpnmgmt.listeners;

/**
 * @author 1951FDG
 */

@FunctionalInterface
public interface OnByteCountChangedListener {

    void onByteCountChanged(long in, long out, long diffIn, long diffOut);
}
