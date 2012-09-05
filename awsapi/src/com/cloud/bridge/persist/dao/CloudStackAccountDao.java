package com.cloud.bridge.persist.dao;

import com.cloud.bridge.model.CloudStackAccountVO;
import com.cloud.utils.db.GenericDao;

public interface CloudStackAccountDao extends
        GenericDao<CloudStackAccountVO, String> {
    String getDefaultZoneId(String accountId);
    

}
