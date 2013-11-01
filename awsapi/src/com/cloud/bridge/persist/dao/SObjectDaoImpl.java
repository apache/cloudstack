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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.bridge.model.SBucketVO;
import com.cloud.bridge.model.SObjectItemVO;
import com.cloud.bridge.model.SObjectVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value={SObjectDao.class})
public class SObjectDaoImpl extends GenericDaoBase<SObjectVO, Long> implements SObjectDao {
    @Inject SObjectItemDao itemDao;

    public SObjectDaoImpl() {}

    @Override
    public SObjectVO getByNameKey(SBucketVO bucket, String nameKey) {
        SObjectVO object = null; 
        SearchBuilder<SObjectVO> SearchByName = createSearchBuilder();
        SearchByName.and("SBucketID", SearchByName.entity().getBucketID() , SearchCriteria.Op.EQ);
        SearchByName.and("NameKey", SearchByName.entity().getNameKey() , SearchCriteria.Op.EQ);
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
        try {
            txn.start();
            SearchCriteria<SObjectVO> sc = SearchByName.create();
            sc.setParameters("SBucketID", bucket.getId());
            sc.setParameters("NameKey", nameKey);
            object = findOneBy(sc);
            if (null != object) {
                Set<SObjectItemVO> items = new HashSet<SObjectItemVO>(
                        itemDao.getItems(object.getId()));
                object.setItems(items);
            }
            return object;

        }finally {
            txn.close();
        }

    }

    @Override
    public List<SObjectVO> listBucketObjects(SBucketVO bucket, String prefix, String marker, int maxKeys) {
        StringBuffer sb = new StringBuffer();
        List<Object> params = new ArrayList<Object>();
        SearchBuilder<SObjectVO> SearchByBucket = createSearchBuilder();
        List<SObjectVO> objects = new ArrayList<SObjectVO>();

        SearchByBucket.and("SBucketID", SearchByBucket.entity().getBucketID(), SearchCriteria.Op.EQ);
        SearchByBucket.and("DeletionMark", SearchByBucket.entity().getDeletionMark(), SearchCriteria.Op.NULL);		
        TransactionLegacy txn = TransactionLegacy.currentTxn();  // Transaction.open("cloudbridge", Transaction.AWSAPI_DB, true);
        try {
            txn.start();
            SearchCriteria<SObjectVO> sc = SearchByBucket.create();
            sc.setParameters("SBucketID", bucket.getId());
            objects = listBy(sc);
            for (SObjectVO sObjectVO : objects) {
                Set<SObjectItemVO> items = new HashSet<SObjectItemVO>(itemDao.getItems(sObjectVO.getId()));
                sObjectVO.setItems(items);
            }
            return objects;
        }finally {
            txn.close();
        }
    }

    @Override
    public List<SObjectVO> listAllBucketObjects(SBucketVO bucket, String prefix, String marker, int maxKeys) {
        StringBuffer sb = new StringBuffer();
        List<Object> params = new ArrayList<Object>();
        SearchBuilder<SObjectVO> getAllBuckets = createSearchBuilder();
        List<SObjectVO> objects = new ArrayList<SObjectVO>();
        getAllBuckets.and("SBucketID", getAllBuckets.entity().getBucketID(), SearchCriteria.Op.EQ);

        TransactionLegacy txn = TransactionLegacy.currentTxn();  // Transaction.open("cloudbridge", Transaction.AWSAPI_DB, true);
        try {
            txn.start();
            SearchCriteria<SObjectVO> sc = getAllBuckets.create();
            sc.setParameters("SBucketID", bucket.getId());
            objects = listBy(sc);
            for (SObjectVO sObjectVO : objects) {
                Set<SObjectItemVO> items = new HashSet<SObjectItemVO>(itemDao.getItems(sObjectVO.getId()));
                sObjectVO.setItems(items);
            }
            return objects;
        }finally {
            txn.close();
        }

    }
}
