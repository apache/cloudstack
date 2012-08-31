package com.cloud.bridge.persist.dao;

import com.cloud.bridge.model.BucketPolicyVO;
import com.cloud.utils.db.GenericDao;

public interface BucketPolicyDao extends GenericDao<BucketPolicyVO, Long> {

    void deletePolicy(String bucketName);

    BucketPolicyVO getByName(String bucketName);

}
