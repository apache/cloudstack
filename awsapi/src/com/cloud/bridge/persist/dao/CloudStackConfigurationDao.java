package com.cloud.bridge.persist.dao;

import com.cloud.bridge.model.CloudStackConfigurationVO;
import com.cloud.utils.db.GenericDao;

public interface CloudStackConfigurationDao extends GenericDao<CloudStackConfigurationVO, String> {

    public String getConfigValue(String name);
}
