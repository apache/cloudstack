package com.cloud.tungsten;

import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.ApiConnectorFactory;

import javax.inject.Inject;
import java.util.List;

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
