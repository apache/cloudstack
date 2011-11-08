package com.cloud.network.dao;

import java.util.List;

import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkServiceMapVO;
import com.cloud.utils.db.GenericDao;

/**
 * NetworkServiceDao deals with searches and operations done on the
 * ntwk_service_map table.
 *
 */
public interface NetworkServiceMapDao extends GenericDao<NetworkServiceMapVO, Long>{
    boolean areServicesSupportedInNetwork(long networkId, Service... services);
    boolean isProviderSupportedInNetwork(long networkId, Service service, Provider provider);
    List<NetworkServiceMapVO> getServicesInNetwork(long networkId);
    String getProviderForServiceInNetwork(long networkid, Service service);
}
