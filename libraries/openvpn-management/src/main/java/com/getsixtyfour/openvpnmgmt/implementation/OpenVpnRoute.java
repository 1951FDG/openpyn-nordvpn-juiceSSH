package com.getsixtyfour.openvpnmgmt.implementation;

import com.getsixtyfour.openvpnmgmt.api.Route;
import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

public final class OpenVpnRoute implements Route {

    private final String mCommonName;

    private final LocalDateTime mLastRef;

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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT, Locale.ENGLISH);
            LocalDateTime date = LocalDateTime.parse(strings[3], formatter);
            InetAddress realAddress = InetAddress.getByName(realConnection[0]);
            int port = Integer.parseInt(realConnection[1]);
            mCommonName = strings[1];
            mVirtualIpAddress = InetAddress.getByName(strings[0]);
            mRealIpAddress = new InetSocketAddress(realAddress, port);
            mLastRef = date;
        } catch (NumberFormatException e) {
            throw new OpenVpnParseException(Constants.CANNOT_PARSE_PORT_NUMBER, e);
        } catch (DateTimeParseException e) {
            throw new OpenVpnParseException(Constants.CANNOT_PARSE_DATE, e);
        } catch (UnknownHostException e) {
            throw new OpenVpnParseException(Constants.CANNOT_PARSE_HOSTNAME, e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        // TODO:
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
        // TODO:
        hash = (79 * hash) + Objects.hashCode(mLastRef);
        return hash;
    }

    @Override
    public @NotNull String toString() {
        return "OpenVpnRoute{" + "Virtual IP address " + mVirtualIpAddress.getHostAddress() + ", common name: " + mCommonName
                + ", real IP address: " + mRealIpAddress + ", source port: " + mRealIpAddress.getPort() + ", last reference: " + mLastRef
                + "}";
    }

    @Override
    public @Nullable String getCommonName() {
        return mCommonName;
    }

    @Override
    public @Nullable LocalDateTime getLastRef() {
        return mLastRef;
    }

    @Override
    public @Nullable InetSocketAddress getRealIpAddress() {
        return mRealIpAddress;
    }

    @Override
    public @Nullable InetAddress getVirtualIpAddress() {
        return mVirtualIpAddress;
    }
}
