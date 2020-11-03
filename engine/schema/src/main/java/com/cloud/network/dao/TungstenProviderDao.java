package com.cloud.network.dao;

import com.cloud.network.element.TungstenProviderVO;
import com.cloud.utils.db.GenericDao;

import java.util.List;

public interface TungstenProviderDao extends GenericDao<TungstenProviderVO, Long> {
    TungstenProviderVO findByZoneId(long zoneId);

    TungstenProviderVO findByUuid(String uuid);

    List<TungstenProviderVO> findAll();

    void deleteProviderByUuid(String providerUuid);
}
