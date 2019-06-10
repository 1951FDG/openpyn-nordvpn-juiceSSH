/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ua.pp.msk.openvpnstatus.api;

import java.net.InetSocketAddress;
import java.util.Calendar;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public interface Client {

    String getCommonName();

    Calendar getConnectedSince();

    InetSocketAddress getIpAddress();

    long getReceivedBytes();

    long getSentBytes();
}
