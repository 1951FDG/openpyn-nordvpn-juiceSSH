/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.getsixtyfour.openvpnmgmt.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

@SuppressWarnings("SerializableHasSerializationMethods")
public class OpenVpnParseException extends Exception {

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
