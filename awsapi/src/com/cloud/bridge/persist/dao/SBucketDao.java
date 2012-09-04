package com.cloud.bridge.persist.dao;

import java.util.List;

import com.cloud.bridge.model.SBucketVO;
import com.cloud.utils.db.GenericDao;

public interface SBucketDao extends GenericDao<SBucketVO, Long> {

    SBucketVO getByName(String bucketName);

    List<SBucketVO> listBuckets(String canonicalId);

}
