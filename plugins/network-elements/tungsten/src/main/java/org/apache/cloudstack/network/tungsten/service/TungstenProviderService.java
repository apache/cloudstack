package org.apache.cloudstack.network.tungsten.service;

import com.cloud.network.TungstenProvider;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenProvidersCmd;
import org.apache.cloudstack.network.tungsten.api.response.TungstenProviderResponse;

import java.util.List;

public interface TungstenProviderService {
    TungstenProvider addProvider(CreateTungstenProviderCmd cmd);

    TungstenProviderResponse getTungstenProvider();

    void deleteTungstenProvider(DeleteTungstenProviderCmd cmd);

    List<TungstenProviderResponse> listProviders(ListTungstenProvidersCmd cmd);

    void disableTungstenNsp();
}
