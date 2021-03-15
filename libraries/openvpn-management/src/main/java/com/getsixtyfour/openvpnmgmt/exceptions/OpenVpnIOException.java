package com.getsixtyfour.openvpnmgmt.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

@SuppressWarnings("SerializableHasSerializationMethods")
public final class OpenVpnIOException extends Exception {

    private static final long serialVersionUID = -3387516993124229948L;

    public OpenVpnIOException() {
    }

    public OpenVpnIOException(@NotNull String message) {
        super(message);
    }

    public OpenVpnIOException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }
}
