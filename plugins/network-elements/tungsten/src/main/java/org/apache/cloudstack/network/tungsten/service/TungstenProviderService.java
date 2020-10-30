package org.apache.cloudstack.network.tungsten.service;

import com.cloud.network.TungstenProvider;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenProvidersCmd;
import org.apache.cloudstack.network.tungsten.api.response.TungstenProviderResponse;

public interface TungstenProviderService {
    TungstenProvider addProvider(CreateTungstenProviderCmd cmd);

    TungstenProviderResponse getTungstenProvider(long zoneId);

    TungstenProviderResponse listTungstenProvider(ListTungstenProvidersCmd cmd);

    void deleteTungstenProvider(DeleteTungstenProviderCmd cmd);

    void disableTungstenNsp(long zoneId);
}
