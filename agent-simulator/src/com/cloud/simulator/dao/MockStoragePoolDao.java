package com.cloud.simulator.dao;

import com.cloud.simulator.MockStoragePoolVO;
import com.cloud.utils.db.GenericDao;

public interface MockStoragePoolDao extends GenericDao<MockStoragePoolVO, Long> {
    public MockStoragePoolVO findByUuid(String uuid);
    public MockStoragePoolVO findByHost(String hostUuid);
}
