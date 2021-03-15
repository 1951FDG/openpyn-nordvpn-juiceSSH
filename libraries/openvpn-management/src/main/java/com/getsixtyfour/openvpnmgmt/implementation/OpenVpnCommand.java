package com.getsixtyfour.openvpnmgmt.implementation;

import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;

import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

public abstract class OpenVpnCommand {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public void setCommandOutput(@NotNull String output) throws OpenVpnParseException {
        String[] lines = output.split(System.lineSeparator());
        setCommandOutput(lines);
    }

    public void setCommandOutput(@NotNull List<String> output) throws OpenVpnParseException {
        setCommandOutput(output.toArray(EMPTY_STRING_ARRAY));
    }

    protected abstract void setCommandOutput(@NotNull String[] lines) throws OpenVpnParseException;
}