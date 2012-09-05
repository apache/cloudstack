package com.cloud.bridge.persist.dao;

import java.util.List;

import com.cloud.bridge.model.MultipartMetaVO;
import com.cloud.utils.db.GenericDao;

public interface MultipartMetaDao extends GenericDao<MultipartMetaVO, Long> {

    List<MultipartMetaVO> getByUploadID(long uploadID);

}
