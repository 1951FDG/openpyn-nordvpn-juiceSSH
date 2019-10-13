/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.getsixtyfour.openvpnmgmt.api;

import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Calendar;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

@SuppressWarnings({ "UseOfObsoleteDateTimeApi", "unused" })
public interface Route {

    //Virtual Address,Common Name,Real Address,Last Ref
    @Nullable
    String getCommonName();

    /**
     * As I understand it should return connection date
     *
     * @return date since client acquired the connection
     */
    @Nullable
    Calendar getLastRef();

    @Nullable
    InetSocketAddress getRealIpAddress();

    @Nullable
    InetAddress getVirtualIpAddress();
}
