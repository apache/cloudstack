package com.cloud.simulator.dao;

import com.cloud.simulator.MockSecStorageVO;
import com.cloud.utils.db.GenericDao;

public interface MockSecStorageDao extends GenericDao<MockSecStorageVO, Long> {
    public MockSecStorageVO findByUrl(String url);
}
