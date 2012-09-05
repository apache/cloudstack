package com.cloud.bridge.persist.dao;

import java.util.List;

import com.cloud.bridge.model.SBucketVO;
import com.cloud.bridge.model.SObjectVO;
import com.cloud.utils.db.GenericDao;

public interface SObjectDao extends GenericDao<SObjectVO, Long> {

    List<SObjectVO> listBucketObjects(SBucketVO bucket, String prefix,
            String marker, int maxKeys);

    List<SObjectVO> listAllBucketObjects(SBucketVO bucket, String prefix,
            String marker, int maxKeys);

    SObjectVO getByNameKey(SBucketVO bucket, String nameKey);

}
