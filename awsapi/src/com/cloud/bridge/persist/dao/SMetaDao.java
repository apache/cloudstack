package com.cloud.bridge.persist.dao;

import java.util.List;

import com.cloud.bridge.model.SMetaVO;
import com.cloud.bridge.service.core.s3.S3MetaDataEntry;
import com.cloud.utils.db.GenericDao;

public interface SMetaDao extends GenericDao<SMetaVO, Long> {

    List<SMetaVO> getByTarget(String target, long targetId);

    SMetaVO save(String target, long targetId, S3MetaDataEntry entry);

    void save(String target, long targetId, S3MetaDataEntry[] entries);

}
