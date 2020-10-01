package org.apache.cloudstack.network.tungsten.service;

import com.cloud.network.TungstenProvider;
import com.cloud.network.element.TungstenProviderVO;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenProvidersCmd;

import java.util.List;

public interface TungstenProviderService {
    TungstenProvider addProvider(CreateTungstenProviderCmd cmd);

    TungstenProviderVO getTungstenProvider();

    void deleteTungstenProvider(DeleteTungstenProviderCmd cmd);

    List<TungstenProviderVO> listProviders(ListTungstenProvidersCmd cmd);

    void disableTungstenNsp();
}
