package com.cloud.network.dao;

import com.cloud.network.element.TungstenProviderVO;
import com.cloud.utils.db.GenericDao;

public interface TungstenProviderDao extends GenericDao<TungstenProviderVO, Long> {
    TungstenProviderVO findByNspId(long nspId);

    TungstenProviderVO findByUuid(String uuid);

    TungstenProviderVO findByPhysicalNetworkId(long physicalNetworkId);

    void deleteProviderByUuid(String providerUuid);
}
