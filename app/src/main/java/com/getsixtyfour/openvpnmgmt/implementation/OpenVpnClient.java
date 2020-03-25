package com.getsixtyfour.openvpnmgmt.implementation;

import com.getsixtyfour.openvpnmgmt.api.Client;
import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;

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

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

// TODO: time api
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class OpenVpnClient implements Client {

    private final String mCommonName;

    private final Calendar mConnectedSince;

    private final InetSocketAddress mIpAddress;

    private final long mReceivedBytes;

    private final long mSentBytes;

    OpenVpnClient(String s) throws OpenVpnParseException {
        String[] strings = s.split(",");
        if (strings.length != 5) {
            //noinspection ConstantConditions
            throw new OpenVpnParseException(String.format((Locale) null, Constants.MALFORMED_ROUTE_STRING, s, 5));
        }
        try {
            String[] realConnection = strings[1].split(":");
            if (realConnection.length != 2) {
                //noinspection ConstantConditions
                throw new OpenVpnParseException(String.format((Locale) null, Constants.MALFORMED_REAL_CONNECTION_STRING, strings[2]));
            }
            InetAddress realAddress = InetAddress.getByName(realConnection[0]);
            int port = Integer.parseInt(realConnection[1]);
            InetSocketAddress realIpSocket = new InetSocketAddress(realAddress, port);
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.ROOT);
            Date parsedDate = sdf.parse(strings[4]);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            long rb = Long.parseLong(strings[2]);
            long sb = Long.parseLong(strings[3]);
            mCommonName = strings[0];
            mIpAddress = realIpSocket;
            mReceivedBytes = rb;
            mSentBytes = sb;
            mConnectedSince = calendar;
        } catch (NumberFormatException e) {
            throw new OpenVpnParseException(Constants.CANNOT_PARSE_PORT_NUMBER, e);
        } catch (ParseException e) {
            throw new OpenVpnParseException(Constants.CANNOT_PARSE_DATE, e);
        } catch (UnknownHostException e) {
            throw new OpenVpnParseException(Constants.CANNOT_PARSE_HOSTNAME, e);
        }
    }

    @NotNull
    @Override
    public String toString() {
        DateFormat df = DateFormat.getInstance();
        return "OpenVpnClient{" + "Common name: " + mCommonName + ", real IP address: " + mIpAddress.getHostString() + ", source port: "
                + mIpAddress.getPort() + ", received bytes: " + mReceivedBytes + ", sent bytes: " + mSentBytes + ", connected since: " + df
                .format(mConnectedSince.getTime()) + "}";
    }

    @Nullable
    @Override
    public String getCommonName() {
        return mCommonName;
    }

    @Nullable
    @Override
    public Calendar getConnectedSince() {
        return (Calendar) mConnectedSince.clone();
    }

    @Nullable
    @Override
    public InetSocketAddress getIpAddress() {
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
