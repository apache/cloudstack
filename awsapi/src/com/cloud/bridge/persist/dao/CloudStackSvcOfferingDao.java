package com.cloud.bridge.persist.dao;

import com.cloud.bridge.model.CloudStackServiceOfferingVO;
import com.cloud.utils.db.GenericDao;

public interface CloudStackSvcOfferingDao extends GenericDao<CloudStackServiceOfferingVO, String>{

    public CloudStackServiceOfferingVO getSvcOfferingByName(String name);

    public CloudStackServiceOfferingVO getSvcOfferingById(String id);


}
