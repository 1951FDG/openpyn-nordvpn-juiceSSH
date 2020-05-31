package com.getsixtyfour.openvpnmgmt.implementation;

import org.jetbrains.annotations.NonNls;

@SuppressWarnings("UtilityClass")
final class Constants {
    static final String DATE_FORMAT = "EEE MMM dd HH:mm:ss yyyy";

    @NonNls
    static final String CANNOT_PARSE_DATE = "Cannot Parse date.";

    @NonNls
    static final String CANNOT_PARSE_HOSTNAME = "Cannot parse hostname.";

    @NonNls
    static final String CANNOT_PARSE_PORT_NUMBER = "Cannot parse port number.";

    @NonNls
    static final String CANNOT_PARSE_UPDATE_DATE = "Cannot parse update date";

    @NonNls
    static final String MALFORMED_REAL_CONNECTION_STRING
            = "Malformed real connection string! %s Need to have 2 sections separated by colon";

    @NonNls
    static final String MALFORMED_ROUTE_STRING = "Malformed route string! %s Need to have %d sections separated by commas";

    private Constants() {
    }
}
