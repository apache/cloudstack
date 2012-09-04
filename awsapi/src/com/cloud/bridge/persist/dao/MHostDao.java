package com.cloud.bridge.persist.dao;

import com.cloud.bridge.model.MHostVO;
import com.cloud.utils.db.GenericDao;

public interface MHostDao extends GenericDao<MHostVO, Long> {

    MHostVO getByHostKey(String hostKey);

    public void updateHeartBeat(MHostVO mhost);

}
