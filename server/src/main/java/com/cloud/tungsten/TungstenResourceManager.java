package com.cloud.tungsten;

import com.cloud.network.Network;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.ApiConnectorFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class TungstenResourceManager {

    @Inject
    TungstenProviderDao _tungstenProviderDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;

    public List<TungstenProvider> getTungstenProviders() {
        List<TungstenProvider> tungstenProviders = new ArrayList<>();
        List<PhysicalNetworkServiceProviderVO> physicalNetworkServiceProviders = _physicalNetworkServiceProviderDao.listByProviderName(Network.Provider.Tungsten.getName());
        for(PhysicalNetworkServiceProviderVO physicalNetworkServiceProvider : physicalNetworkServiceProviders) {
            TungstenProviderVO tungstenProvider = _tungstenProviderDao.findByNspId(physicalNetworkServiceProvider.getId());
            if(tungstenProvider != null)
                tungstenProviders.add(tungstenProvider);
        }
        return tungstenProviders;
    }

    public ApiConnector getApiConnector(TungstenProvider tungstenProvider){
        return ApiConnectorFactory.build(tungstenProvider.getHostname(), Integer.parseInt(tungstenProvider.getPort()));
    }
}
