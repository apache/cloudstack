package com.cloud.bridge.persist.dao;

import java.util.List;

import com.cloud.bridge.model.SAcl;
import com.cloud.bridge.model.SAclVO;
import com.cloud.bridge.service.core.s3.S3AccessControlList;
import com.cloud.bridge.service.core.s3.S3Grant;
import com.cloud.utils.db.GenericDao;

public interface SAclDao extends GenericDao<SAclVO, Long> {

    List<SAclVO> listGrants(String target, long targetId, String userCanonicalId);

    void save(String target, long targetId, S3AccessControlList acl);

    SAcl save(String target, long targetId, S3Grant grant, int grantOrder);

    List<SAclVO> listGrants(String target, long targetId);

}
