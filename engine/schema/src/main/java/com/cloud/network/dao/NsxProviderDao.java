package com.cloud.network.dao;

import com.cloud.network.element.NsxProviderVO;
import com.cloud.utils.db.GenericDao;

import java.util.List;

public interface NsxProviderDao extends GenericDao<NsxProviderVO, Long> {
    NsxProviderVO findByZoneId(long zoneId);

    NsxProviderVO findByUuid(String uuid);

    List<NsxProviderVO> findAll();
}
