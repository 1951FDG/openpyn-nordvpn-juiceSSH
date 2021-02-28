package com.getsixtyfour.openvpnmgmt.net;

import com.getsixtyfour.openvpnmgmt.api.Connection;
import com.getsixtyfour.openvpnmgmt.api.Status;
import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;
import com.getsixtyfour.openvpnmgmt.implementation.OpenVpnStatus;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author 1951FDG
 */

@SuppressWarnings("UtilityClass")
public final class ManagementUtils {

    private ManagementUtils() {
    }

    public static @NotNull String getManagementVersion(@NotNull Connection connection) {
        @NotNull String result = "";
        try {
            String output = connection.executeCommand(Commands.VERSION_COMMAND);
            String[] lines = output.split(System.lineSeparator());
            String line = (lines.length >= 1) ? lines[lines.length - 1] : "";
            if (!line.isEmpty() && line.startsWith(Constants.MANAGEMENT_VERSION_PREFIX)) {
                result = line.substring(Constants.MANAGEMENT_VERSION_PREFIX.length() + 1);
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
            if (!line.isEmpty() && line.startsWith(Constants.OPEN_VPN_VERSION_PREFIX)) {
                result = line.substring(Constants.OPEN_VPN_VERSION_PREFIX.length() + 1);
            }
        } catch (IOException ignored) {
        }
        return result;
    }
}
