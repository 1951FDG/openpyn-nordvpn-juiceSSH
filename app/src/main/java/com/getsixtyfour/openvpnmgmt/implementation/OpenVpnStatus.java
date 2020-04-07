package com.getsixtyfour.openvpnmgmt.implementation;

import com.getsixtyfour.openvpnmgmt.api.Client;
import com.getsixtyfour.openvpnmgmt.api.Route;
import com.getsixtyfour.openvpnmgmt.api.Status;
import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class OpenVpnStatus extends OpenVpnCommand implements Status {

    @NonNls
    private static final Pattern CLIENTS_HEADER = Pattern.compile("^OpenVPN CLIENT LIST");

    @NonNls
    private static final Pattern CLIENT_COLUMNS = Pattern.compile("Common Name,Real Address,Bytes Received,Bytes Sent,Connected Since");

    @NonNls
    private static final Pattern GLOBAL_STATS = Pattern.compile("GLOBAL STATS");

    @NonNls
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenVpnStatus.class);

    @NonNls
    private static final Pattern ROUTES_COLUMNS = Pattern.compile("Virtual Address,Common Name,Real Address,Last Ref");

    @NonNls
    private static final Pattern ROUTES_HEADER = Pattern.compile("^ROUTING TABLE");

    @NonNls
    private static final Pattern STATS_HEADER = Pattern.compile("^OpenVPN STATISTICS");

    @NonNls
    private static final Pattern UPDATED = Pattern.compile("^Updated,.*");

    private final List<Client> mClients = new ArrayList<>(10);

    private final Set<Route> mRoutes = new HashSet<>(10);

    private Calendar mUpdateTime = null;

    private static Calendar parseUpdatedTime(String updatedString) throws OpenVpnParseException {
        try {
            String[] components = updatedString.split(",");
            if (components.length != 2) {
                throw new OpenVpnParseException("Cannot parse update time string. There should be 2 components separated by comma");
            }
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.ROOT);
            Date parsedDate = sdf.parse(components[1]);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            return calendar;
        } catch (ParseException e) {
            throw new OpenVpnParseException("Cannot parse update time string", e);
        }
    }

    @NotNull
    @Override
    public String toString() {
        DateFormat df = DateFormat.getInstance();
        StringBuilder sb = new StringBuilder("Updated:\t");
        sb.append(df.format(mUpdateTime.getTime()));
        sb.append(System.lineSeparator());
        sb.append("Client List:");
        sb.append(System.lineSeparator());
        String str = "\t";
        for (Client client : mClients) {
            sb.append(str);
            sb.append(client);
            sb.append(System.lineSeparator());
        }
        sb.append("Routes list:");
        sb.append(System.lineSeparator());
        for (Route route : mRoutes) {
            sb.append(str);
            sb.append(route);
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    @NotNull
    @Override
    public List<Client> getClients() {
        return Collections.unmodifiableList(mClients);
    }

    @NotNull
    @Override
    public Set<Route> getRoutes() {
        return Collections.unmodifiableSet(mRoutes);
    }

    @Nullable
    @Override
    public Calendar getUpdateTime() {
        return (mUpdateTime == null) ? null : (Calendar) mUpdateTime.clone();
    }

    @Override
    public void setCommandOutput(@NotNull String output) throws OpenVpnParseException {
        LOGGER.info("Parsing:{}{}", System.lineSeparator(), output);
        super.setCommandOutput(output);
    }

    @SuppressWarnings({ "AssignmentToForLoopParameter", "ValueOfIncrementOrDecrementUsed", "MethodCallInLoopCondition", "ChainedMethodCall",
            "OverlyComplexMethod" })
    @Override
    public void setCommandOutput(@NotNull String[] lines) throws OpenVpnParseException {
        @NonNls String msg = "Cannot parse OpenVPN status. Wrong lines sequence.";
        int length = lines.length;
        for (int i = 0; i < length; i++) {
            if (STATS_HEADER.matcher(lines[i]).matches()) {
                if (UPDATED.matcher(lines[++i]).matches()) {
                    try {
                        mUpdateTime = parseUpdatedTime(lines[i++]);
                    } catch (OpenVpnParseException e) {
                        LOGGER.error(Constants.CANNOT_PARSE_UPDATE_DATE, e);
                    }
                } else {
                    throw new OpenVpnParseException(msg);
                }
            }
            if (CLIENTS_HEADER.matcher(lines[i]).matches()) {
                if (UPDATED.matcher(lines[++i]).matches()) {
                    try {
                        mUpdateTime = parseUpdatedTime(lines[i++]);
                    } catch (OpenVpnParseException e) {
                        LOGGER.error(Constants.CANNOT_PARSE_UPDATE_DATE, e);
                    }
                } else {
                    throw new OpenVpnParseException(msg);
                }
                if (CLIENT_COLUMNS.matcher(lines[i++]).matches()) {
                    while (!ROUTES_HEADER.matcher(lines[i]).matches()) {
                        addClient(lines[i++]);
                    }
                } else {
                    throw new OpenVpnParseException(msg);
                }
            }
            if (ROUTES_HEADER.matcher(lines[i]).matches()) {
                i++;
                if (ROUTES_COLUMNS.matcher(lines[i]).matches()) {
                    i++;
                    while (!GLOBAL_STATS.matcher(lines[i]).matches()) {
                        addRoute(lines[i++]);
                    }
                    break;
                } else {
                    throw new OpenVpnParseException(msg);
                }
            }
        }
        LOGGER.info("Successfully parsed{}{}", System.lineSeparator(), this);
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    private void addClient(String clientString) {
        try {
            Client ovc = new OpenVpnClient(clientString);
            mClients.add(ovc);
        } catch (OpenVpnParseException e) {
            LOGGER.error("Cannot add the client", e);
        }
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    private void addRoute(String routeString) {
        try {
            Route ovr = new OpenVpnRoute(routeString);
            mRoutes.add(ovr);
        } catch (OpenVpnParseException e) {
            LOGGER.error("Cannot add route", e);
        }
    }
}
