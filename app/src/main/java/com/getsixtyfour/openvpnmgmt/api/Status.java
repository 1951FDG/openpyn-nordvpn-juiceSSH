/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.getsixtyfour.openvpnmgmt.api;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

@SuppressWarnings({ "UseOfObsoleteDateTimeApi", "ConstantDeclaredInInterface" })
public interface Status {

    @SuppressWarnings("SpellCheckingInspection")
    String DATE_FORMAT = "EEE MMMM dd HH:mm:ss yyyy";

    @NotNull
    List<Client> getClients();

    @NotNull
    Set<Route> getRoutes();

    @NotNull
    Calendar getUpdateTime();
}
