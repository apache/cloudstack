package com.cloud.bridge.persist.dao;

import java.util.List;

import com.cloud.bridge.model.SObjectItemVO;
import com.cloud.utils.db.GenericDao;

public interface SObjectItemDao extends GenericDao<SObjectItemVO, Long> {

    SObjectItemVO getByObjectIdNullVersion(long id);

    List<SObjectItemVO> getItems(long sobjectID);

}
