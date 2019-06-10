package ua.pp.msk.openvpnstatus.implementation;

import java.util.List;

import ua.pp.msk.openvpnstatus.exceptions.OpenVpnParseException;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

public abstract class OpenVpnCommand {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public void setCommandOutput(String output) throws OpenVpnParseException {
        String[] lines = output.split(System.lineSeparator());
        setCommandOutput(lines);
    }

    public void setCommandOutput(List<String> output) throws OpenVpnParseException {
        setCommandOutput(output.toArray(EMPTY_STRING_ARRAY));
    }

    protected abstract void setCommandOutput(String[] lines) throws OpenVpnParseException;
}
