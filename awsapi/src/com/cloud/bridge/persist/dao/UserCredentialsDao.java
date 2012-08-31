package com.cloud.bridge.persist.dao;

import com.cloud.bridge.model.UserCredentialsVO;
import com.cloud.utils.db.GenericDao;

public interface UserCredentialsDao extends GenericDao<UserCredentialsVO, Long> {

    UserCredentialsVO getByAccessKey(String cloudAccessKey);

    UserCredentialsVO getByCertUniqueId(String certId);

}
