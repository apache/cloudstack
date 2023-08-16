package com.cloud.network.dao;

import com.cloud.network.element.NsxProviderVO;
import com.cloud.utils.db.GenericDaoBase;

import java.util.List;

public class NsxProviderDaoImpl extends GenericDaoBase<NsxProviderVO, Long>
        implements NsxProviderDao {
    @Override
    public NsxProviderVO findByZoneId(long zoneId) {
        return null;
    }

    @Override
    public List<NsxProviderVO> findAll() {
        return null;
    }
}
