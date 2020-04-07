package com.getsixtyfour.openvpnmgmt.api;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public interface Status {

    @NotNull
    List<Client> getClients();

    @NotNull
    Set<Route> getRoutes();

    @Nullable
    Calendar getUpdateTime();
}
