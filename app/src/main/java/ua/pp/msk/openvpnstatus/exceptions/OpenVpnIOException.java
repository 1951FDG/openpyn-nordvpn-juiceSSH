/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ua.pp.msk.openvpnstatus.exceptions;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

@SuppressWarnings("SerializableHasSerializationMethods")
public class OpenVpnIOException extends Exception {

    private static final long serialVersionUID = -3387516993124229948L;

    public OpenVpnIOException() {
    }

    public OpenVpnIOException(String message) {
        super(message);
    }

    public OpenVpnIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
