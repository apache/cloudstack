package com.cloud.bridge.persist.dao;

import com.cloud.bridge.model.MHostMountVO;
import com.cloud.utils.db.GenericDao;

public interface MHostMountDao extends GenericDao<MHostMountVO, Long> {

    MHostMountVO getHostMount(long mHostId, long sHostId);
    

}
