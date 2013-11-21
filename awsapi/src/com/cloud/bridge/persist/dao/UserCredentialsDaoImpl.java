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

import com.cloud.bridge.model.UserCredentialsVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = {UserCredentialsDao.class})
public class UserCredentialsDaoImpl extends GenericDaoBase<UserCredentialsVO, Long> implements UserCredentialsDao {
    public static final Logger logger = Logger.getLogger(UserCredentialsDaoImpl.class);

    public UserCredentialsDaoImpl() {
    }

    @DB
    @Override
    public UserCredentialsVO getByAccessKey(String cloudAccessKey) {
        SearchBuilder<UserCredentialsVO> SearchByAccessKey = createSearchBuilder();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
        try {
            txn.start();
            SearchByAccessKey.and("AccessKey", SearchByAccessKey.entity().getAccessKey(), SearchCriteria.Op.EQ);
            SearchByAccessKey.done();
            SearchCriteria<UserCredentialsVO> sc = SearchByAccessKey.create();
            sc.setParameters("AccessKey", cloudAccessKey);
            return findOneBy(sc);
        } finally {
            txn.commit();
            txn.close();
        }
    }

    @Override
    public UserCredentialsVO getByCertUniqueId(String certId) {
        SearchBuilder<UserCredentialsVO> SearchByCertID = createSearchBuilder();
        SearchByCertID.and("CertUniqueId", SearchByCertID.entity().getCertUniqueId(), SearchCriteria.Op.EQ);
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
        try {
            txn.start();
            SearchCriteria<UserCredentialsVO> sc = SearchByCertID.create();
            sc.setParameters("CertUniqueId", certId);
            return findOneBy(sc);
        } finally {
            txn.commit();
            txn.close();
        }

    }

}
