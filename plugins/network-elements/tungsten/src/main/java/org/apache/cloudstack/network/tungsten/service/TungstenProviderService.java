package org.apache.cloudstack.network.tungsten.service;

import com.cloud.network.TungstenProvider;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.response.TungstenProviderResponse;

public interface TungstenProviderService extends PluggableService {
    TungstenProvider addProvider(CreateTungstenProviderCmd cmd);

    TungstenProviderResponse getTungstenProvider(long zoneId);
}
