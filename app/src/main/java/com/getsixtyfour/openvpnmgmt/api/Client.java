/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.getsixtyfour.openvpnmgmt.api;

import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Calendar;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

@SuppressWarnings({ "UseOfObsoleteDateTimeApi", "unused" })
public interface Client {

    @Nullable
    String getCommonName();

    @Nullable
    Calendar getConnectedSince();

    @Nullable
    InetSocketAddress getIpAddress();

    long getReceivedBytes();

    long getSentBytes();
}
