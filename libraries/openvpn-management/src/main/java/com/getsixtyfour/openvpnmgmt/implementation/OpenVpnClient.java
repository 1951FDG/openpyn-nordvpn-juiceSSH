package com.getsixtyfour.openvpnmgmt.implementation;

import com.getsixtyfour.openvpnmgmt.api.Client;
import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

public final class OpenVpnClient implements Client {

    private final String mCommonName;

    private final LocalDateTime mConnectedSince;

    private final InetSocketAddress mIpAddress;

    private final long mReceivedBytes;

    private final long mSentBytes;

    OpenVpnClient(String s) throws OpenVpnParseException {
        String[] strings = s.split(",");
        if (strings.length != 5) {
            throw new OpenVpnParseException(String.format((Locale) null, Constants.MALFORMED_ROUTE_STRING, s, 5));
        }
        try {
            String[] realConnection = strings[1].split(":");
            if (realConnection.length != 2) {
                throw new OpenVpnParseException(String.format((Locale) null, Constants.MALFORMED_REAL_CONNECTION_STRING, strings[2]));
            }
            InetAddress realAddress = InetAddress.getByName(realConnection[0]);
            int port = Integer.parseInt(realConnection[1]);
            InetSocketAddress realIpSocket = new InetSocketAddress(realAddress, port);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT, Locale.ENGLISH);
            LocalDateTime date = LocalDateTime.parse(strings[4], formatter);
            mCommonName = strings[0];
            mIpAddress = realIpSocket;
            mReceivedBytes = Long.parseLong(strings[2]);
            mSentBytes = Long.parseLong(strings[3]);
            mConnectedSince = date;
        } catch (NumberFormatException e) {
            throw new OpenVpnParseException(Constants.CANNOT_PARSE_PORT_NUMBER, e);
        } catch (DateTimeParseException e) {
            throw new OpenVpnParseException(Constants.CANNOT_PARSE_DATE, e);
        } catch (UnknownHostException e) {
            throw new OpenVpnParseException(Constants.CANNOT_PARSE_HOSTNAME, e);
        }
    }

    @Override
    public @NotNull String toString() {
        return "OpenVpnClient{" + "Common name: " + mCommonName + ", real IP address: " + mIpAddress.getHostString() + ", source port: "
                + mIpAddress.getPort() + ", received bytes: " + mReceivedBytes + ", sent bytes: " + mSentBytes + ", connected since: "
                + mConnectedSince + "}";
    }

    @Override
    public @Nullable String getCommonName() {
        return mCommonName;
    }

    @Override
    public @Nullable LocalDateTime getConnectedSince() {
        return mConnectedSince;
    }

    @Override
    public @Nullable InetSocketAddress getIpAddress() {
        return mIpAddress;
    }

    @Override
    public long getReceivedBytes() {
        return mReceivedBytes;
    }

    @Override
    public long getSentBytes() {
        return mSentBytes;
    }
}
