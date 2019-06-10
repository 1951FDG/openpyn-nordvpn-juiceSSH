/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ua.pp.msk.openvpnstatus.implementation;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ua.pp.msk.openvpnstatus.api.Client;
import ua.pp.msk.openvpnstatus.api.Status;
import ua.pp.msk.openvpnstatus.exceptions.OpenVpnParseException;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class OpenVpnClient implements Client {

    private final Calendar connectedSince;

    private final String mCommonName;

    private final InetSocketAddress mIpAddress;

    private final long mReceivedBytes;

    private final long mSentBytes;

    OpenVpnClient(String s) throws OpenVpnParseException {
        String[] strings = s.split(",");
        if (strings.length != 5) {
            throw new OpenVpnParseException("Malformed route string! " + s + " Need to have 5 sections separated by commas");
        }
        try {
            String[] realConnection = strings[1].split(":");
            if (realConnection.length != 2) {
                throw new OpenVpnParseException(
                        "Malformed real connection string! " + strings[2] + " Need to have 2 sections separated by colon");
            }
            InetAddress realAddress = InetAddress.getByName(realConnection[0]);
            int port = Integer.parseInt(realConnection[1]);
            InetSocketAddress realIpSocket = new InetSocketAddress(realAddress, port);
            SimpleDateFormat sdf = new SimpleDateFormat(Status.DATE_FORMAT, Locale.ROOT);
            Date parsedDate = sdf.parse(strings[4]);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            long rb = Long.parseLong(strings[2]);
            long sb = Long.parseLong(strings[3]);
            mCommonName = strings[0];
            mIpAddress = realIpSocket;
            mReceivedBytes = rb;
            mSentBytes = sb;
            connectedSince = calendar;
        } catch (NumberFormatException ex) {
            throw new OpenVpnParseException("Cannot parse port number. ", ex);
        } catch (ParseException ex) {
            throw new OpenVpnParseException("Cannot Parse date." + ex);
        } catch (UnknownHostException ex) {
            throw new OpenVpnParseException("Cannot parse hostname.", ex);
        }
    }

    @NotNull
    @Override
    public String toString() {
        DateFormat df = DateFormat.getInstance();
        return "OpenVpnClient{" + "Common name: " + mCommonName + ", real IP address: " + mIpAddress.getHostString() + ", source port: "
                + mIpAddress.getPort() + ", received bytes: " + mReceivedBytes + ", sent bytes: " + mSentBytes + ", connected since: " + df
                .format(connectedSince.getTime()) + '}';
    }

    @Override
    public String getCommonName() {
        return mCommonName;
    }

    @Override
    public Calendar getConnectedSince() {
        return (Calendar) connectedSince.clone();
    }

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
