package com.getsixtyfour.openvpnmgmt.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

public interface Status {

    @NotNull List<Client> getClients();

    @NotNull Set<Route> getRoutes();

    @Nullable LocalDateTime getUpdateTime();
}
