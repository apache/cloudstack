// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.bridge.persist.dao;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.bridge.model.MultiPartUploadsVO;
import com.cloud.bridge.util.OrderedPair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = {MultiPartUploadsDao.class})
public class MultiPartUploadsDaoImpl extends GenericDaoBase<MultiPartUploadsVO, Long> implements MultiPartUploadsDao {

    @Override
    public OrderedPair<String, String> multipartExits(int uploadId) {
        MultiPartUploadsVO uploadvo = null;

        TransactionLegacy txn = null;
        try {
            txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
            uploadvo = findById(new Long(uploadId));
            if (null != uploadvo)
                return new OrderedPair<String, String>(uploadvo.getAccessKey(), uploadvo.getNameKey());

            return null;
        } finally {
            txn.close();
        }
    }

    @Override
    public void deleteUpload(int uploadId) {

        TransactionLegacy txn = null;
        try {
            txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
            remove(new Long(uploadId));
            txn.commit();
        } finally {
            txn.close();
        }
    }

    @Override
    public String getAtrributeValue(String attribute, int uploadid) {
        TransactionLegacy txn = null;
        MultiPartUploadsVO uploadvo = null;
        try {
            txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
            uploadvo = findById(new Long(uploadid));
            if (null != uploadvo) {
                if (attribute.equalsIgnoreCase("AccessKey"))
                    return uploadvo.getAccessKey();
                else if (attribute.equalsIgnoreCase("x_amz_acl"))
                    return uploadvo.getAmzAcl();
            }
            return null;
        } finally {
            txn.close();
        }
    }

    @Override
    public List<MultiPartUploadsVO> getInitiatedUploads(String bucketName, int maxParts, String prefix, String keyMarker, String uploadIdMarker) {

        List<MultiPartUploadsVO> uploadList = new ArrayList<MultiPartUploadsVO>();

        SearchBuilder<MultiPartUploadsVO> byBucket = createSearchBuilder();
        byBucket.and("BucketName", byBucket.entity().getBucketName(), SearchCriteria.Op.EQ);

        if (null != prefix)
            byBucket.and("NameKey", byBucket.entity().getNameKey(), SearchCriteria.Op.LIKE);
        if (null != uploadIdMarker)
            byBucket.and("NameKey", byBucket.entity().getNameKey(), SearchCriteria.Op.GT);
        if (null != uploadIdMarker)
            byBucket.and("ID", byBucket.entity().getId(), SearchCriteria.Op.GT);

        Filter filter = new Filter(MultiPartUploadsVO.class, "nameKey", Boolean.TRUE, null, null);
        filter.addOrderBy(MultiPartUploadsVO.class, "createTime", Boolean.TRUE);

        TransactionLegacy txn = TransactionLegacy.open("cloudbridge", TransactionLegacy.AWSAPI_DB, true);
        try {
            txn.start();
            SearchCriteria<MultiPartUploadsVO> sc = byBucket.create();
            sc.setParameters("BucketName", bucketName);
            if (null != prefix)
                sc.setParameters("NameKey", prefix);
            if (null != uploadIdMarker)
                sc.setParameters("NameKey", keyMarker);
            if (null != uploadIdMarker)
                sc.setParameters("ID", uploadIdMarker);
            listBy(sc, filter);

        } finally {
            txn.close();
        }
        return null;
    }

}
