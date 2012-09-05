package com.cloud.bridge.persist.dao;

import java.util.List;

import com.cloud.bridge.model.MultiPartUploadsVO;
import com.cloud.bridge.util.OrderedPair;
import com.cloud.utils.db.GenericDao;

public interface MultiPartUploadsDao extends
        GenericDao<MultiPartUploadsVO, Long> {

    OrderedPair<String, String> multipartExits(int uploadId);

    void deleteUpload(int uploadId);

    String getAtrributeValue(String attribute, int uploadid);

    List<MultiPartUploadsVO> getInitiatedUploads(String bucketName,
            int maxParts, String prefix, String keyMarker, String uploadIdMarker);

}
