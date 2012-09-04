package com.cloud.bridge.persist.dao;

import com.cloud.bridge.model.SHostVO;
import com.cloud.utils.db.GenericDao;

public interface SHostDao extends GenericDao<SHostVO, Long> {

    SHostVO getByHost(String host);

    SHostVO getLocalStorageHost(long mhostId, String storageRoot);

}
