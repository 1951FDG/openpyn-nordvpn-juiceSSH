/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ua.pp.msk.openvpnstatus.api;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Calendar;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public interface Route {
    //Virtual Address,Common Name,Real Address,Last Ref

    String getCommonName();

    /**
     * As I understand it should return connection date
     *
     * @return date since client acquired the connection
     */
    Calendar getLastRef();

    InetSocketAddress getRealIpAddress();

    InetAddress getVirtualIpAddress();
}
