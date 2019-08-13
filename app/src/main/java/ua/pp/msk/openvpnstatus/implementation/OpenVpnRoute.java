/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ua.pp.msk.openvpnstatus.implementation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import ua.pp.msk.openvpnstatus.api.Route;
import ua.pp.msk.openvpnstatus.api.Status;
import ua.pp.msk.openvpnstatus.exceptions.OpenVpnParseException;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class OpenVpnRoute implements Route {

    private final String mCommonName;

    private final Calendar mLastRef;

    private final InetSocketAddress mRealIpAddress;

    private final InetAddress mVirtualIpAddress;

    OpenVpnRoute(String s) throws OpenVpnParseException {
        String[] strings = s.split(",");
        if (strings.length != 4) {
            throw new OpenVpnParseException(String.format((Locale) null, Constants.MALFORMED_ROUTE_STRING, s, 4));
        }
        try {
            String[] realConnection = strings[2].split(":");
            if (realConnection.length != 2) {
                throw new OpenVpnParseException(String.format((Locale) null, Constants.MALFORMED_REAL_CONNECTION_STRING, strings[2]));
            }
            InetAddress virtualAddress = InetAddress.getByName(strings[0]);
            InetAddress realAddress = InetAddress.getByName(realConnection[0]);
            int port = Integer.parseInt(realConnection[1]);
            InetSocketAddress realIpSocket = new InetSocketAddress(realAddress, port);
            SimpleDateFormat sdf = new SimpleDateFormat(Status.DATE_FORMAT, Locale.ROOT);
            Date parsedDate = sdf.parse(strings[3]);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            mCommonName = strings[1];
            mVirtualIpAddress = virtualAddress;
            mRealIpAddress = realIpSocket;
            mLastRef = calendar;
        } catch (NumberFormatException ex) {
            throw new OpenVpnParseException(Constants.CANNOT_PARSE_PORT_NUMBER, ex);
        } catch (ParseException ex) {
            throw new OpenVpnParseException(Constants.CANNOT_PARSE_DATE, ex);
        } catch (UnknownHostException ex) {
            throw new OpenVpnParseException(Constants.CANNOT_PARSE_HOSTNAME, ex);
        }
    }

    @Override
    public boolean equals(Object obj) {
        // TODO
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        OpenVpnRoute other = (OpenVpnRoute) obj;
        if (!Objects.equals(mVirtualIpAddress, other.mVirtualIpAddress)) {
            return false;
        }
        if (!Objects.equals(mCommonName, other.mCommonName)) {
            return false;
        }
        return Objects.equals(mRealIpAddress, other.mRealIpAddress);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = (79 * hash) + Objects.hashCode(mVirtualIpAddress);
        hash = (79 * hash) + Objects.hashCode(mCommonName);
        hash = (79 * hash) + Objects.hashCode(mRealIpAddress);
        // TODO
        hash = (79 * hash) + Objects.hashCode(mLastRef);
        return hash;
    }

    @NotNull
    @Override
    public String toString() {
        DateFormat df = DateFormat.getInstance();
        return "OpenVpnRoute{" + "Virtual IP address " + mVirtualIpAddress.getHostAddress() + ", common name: " + mCommonName
                + ", real IP address: " + mRealIpAddress + ", source port: " + mRealIpAddress.getPort() + ", last reference: " + df
                .format(mLastRef.getTime()) + "}";
    }

    @Nullable
    @Override
    public String getCommonName() {
        return mCommonName;
    }

    @Nullable
    @Override
    public Calendar getLastRef() {
        return (Calendar) mLastRef.clone();
    }

    @Nullable
    @Override
    public InetSocketAddress getRealIpAddress() {
        return mRealIpAddress;
    }

    @Nullable
    @Override
    public InetAddress getVirtualIpAddress() {
        return mVirtualIpAddress;
    }
}
