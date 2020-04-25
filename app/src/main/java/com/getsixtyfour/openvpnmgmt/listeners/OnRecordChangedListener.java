package com.getsixtyfour.openvpnmgmt.listeners;

import com.getsixtyfour.openvpnmgmt.model.OpenVpnLogRecord;

import org.jetbrains.annotations.NotNull;

/**
 * @author 1951FDG
 */

@FunctionalInterface
public interface OnRecordChangedListener {

    void onRecordChanged(@NotNull OpenVpnLogRecord record);
}
