package com.cloud.bridge.persist.dao;

import java.util.List;

import com.cloud.bridge.model.MultiPartPartsVO;
import com.cloud.utils.db.GenericDao;

public interface MultiPartPartsDao extends GenericDao<MultiPartPartsVO, Long> {

    List<MultiPartPartsVO> getParts(int uploadId, int maxParts, int startAt);

    int getnumParts(int uploadId, int endMarker);

    MultiPartPartsVO findByUploadID(int uploadId, int partNumber);

    void updateParts(MultiPartPartsVO partVO, int uploadId, int partNumber);

}
