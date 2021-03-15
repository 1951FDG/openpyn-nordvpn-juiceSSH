package com.getsixtyfour.openvpnmgmt.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

@SuppressWarnings("SerializableHasSerializationMethods")
public final class OpenVpnParseException extends Exception {

    private static final long serialVersionUID = -3387516993124229948L;

    public OpenVpnParseException() {
    }

    public OpenVpnParseException(@NotNull String message) {
        super(message);
    }

    public OpenVpnParseException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }
}
