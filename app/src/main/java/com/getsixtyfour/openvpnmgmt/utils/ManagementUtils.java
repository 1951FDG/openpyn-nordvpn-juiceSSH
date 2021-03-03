package com.getsixtyfour.openvpnmgmt.utils;

import com.getsixtyfour.openvpnmgmt.api.Connection;
import com.getsixtyfour.openvpnmgmt.api.Status;
import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;
import com.getsixtyfour.openvpnmgmt.implementation.OpenVpnStatus;
import com.getsixtyfour.openvpnmgmt.net.Commands;

import java.io.IOException;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author 1951FDG
 */

@SuppressWarnings("UtilityClass")
public final class ManagementUtils {

    @NonNls
    private static final String MANAGEMENT_VERSION_PREFIX = "Management Version:";

    @NonNls
    private static final String OPEN_VPN_VERSION_PREFIX = "OpenVPN Version:";

    private ManagementUtils() {
    }

    public static @NotNull String getManagementVersion(@NotNull Connection connection) {
        @NotNull String result = "";
        try {
            String output = connection.executeCommand(Commands.VERSION_COMMAND);
            String[] lines = output.split(System.lineSeparator());
            String line = (lines.length >= 1) ? lines[lines.length - 1] : "";
            if (!line.isEmpty() && line.startsWith(MANAGEMENT_VERSION_PREFIX)) {
                result = line.substring(MANAGEMENT_VERSION_PREFIX.length() + 1);
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    public static @Nullable Status getVpnStatus(@NotNull Connection connection) {
        @Nullable Status result = null;
        try {
            String output = connection.executeCommand(Commands.STATUS_COMMAND);
            OpenVpnStatus ovs = new OpenVpnStatus();
            ovs.setCommandOutput(output);
            result = ovs;
        } catch (IOException | OpenVpnParseException ignored) {
        }
        return result;
    }

    public static @NotNull String getVpnVersion(@NotNull Connection connection) {
        @NotNull String result = "";
        try {
            String output = connection.executeCommand(Commands.VERSION_COMMAND);
            String[] lines = output.split(System.lineSeparator());
            String line = (lines.length >= 2) ? lines[lines.length - 2] : "";
            if (!line.isEmpty() && line.startsWith(OPEN_VPN_VERSION_PREFIX)) {
                result = line.substring(OPEN_VPN_VERSION_PREFIX.length() + 1);
            }
        } catch (IOException ignored) {
        }
        return result;
    }
}
