package com.cloud.tungsten;

import com.cloud.network.Network;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.ApiConnectorFactory;

import java.util.List;

import javax.inject.Inject;

public class TungstenResourceManager {

    @Inject
    TungstenProviderDao _tungstenProviderDao;

    public List<TungstenProviderVO> getTungstenProviders() {
        return _tungstenProviderDao.findAll();
    }

    public ApiConnector getApiConnector(TungstenProvider tungstenProvider) {
        return ApiConnectorFactory.build(tungstenProvider.getHostname(), Integer.parseInt(tungstenProvider.getPort()));
    }
}
