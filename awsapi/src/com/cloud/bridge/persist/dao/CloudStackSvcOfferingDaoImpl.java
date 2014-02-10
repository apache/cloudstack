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

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.bridge.model.CloudStackServiceOfferingVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = {CloudStackSvcOfferingDao.class})
public class CloudStackSvcOfferingDaoImpl extends GenericDaoBase<CloudStackServiceOfferingVO, String> implements CloudStackSvcOfferingDao {
    public static final Logger logger = Logger.getLogger(CloudStackSvcOfferingDaoImpl.class);

    public CloudStackSvcOfferingDaoImpl() {
    }

    @Override
    public CloudStackServiceOfferingVO getSvcOfferingByName(String name) {
        SearchBuilder<CloudStackServiceOfferingVO> searchByName = createSearchBuilder();
        searchByName.and("name", searchByName.entity().getName(), SearchCriteria.Op.EQ);
        searchByName.done();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        try {
            txn.start();
            SearchCriteria<CloudStackServiceOfferingVO> sc = searchByName.create();
            sc.setParameters("name", name);
            return findOneBy(sc);

        } finally {
            txn.commit();
            txn.close();
        }

    }

    @Override
    public CloudStackServiceOfferingVO getSvcOfferingById(String id) {
        SearchBuilder<CloudStackServiceOfferingVO> searchByID = createSearchBuilder();
        searchByID.and("uuid", searchByID.entity().getUuid(), SearchCriteria.Op.EQ);
        searchByID.done();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        try {
            txn.start();
            SearchCriteria<CloudStackServiceOfferingVO> sc = searchByID.create();
            sc.setParameters("uuid", id);
            return findOneBy(sc);

        } finally {
            txn.commit();
            txn.close();
        }

    }

}
