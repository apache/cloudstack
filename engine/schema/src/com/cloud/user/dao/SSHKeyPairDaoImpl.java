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
package com.cloud.user.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.user.SSHKeyPairVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = {SSHKeyPairDao.class})
public class SSHKeyPairDaoImpl extends GenericDaoBase<SSHKeyPairVO, Long> implements SSHKeyPairDao {

    @Override
    public List<SSHKeyPairVO> listKeyPairs(long accountId, long domainId) {
        SearchCriteria<SSHKeyPairVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        return listBy(sc);
    }

    @Override
    public List<SSHKeyPairVO> listKeyPairsByName(long accountId, long domainId, String name) {
        SearchCriteria<SSHKeyPairVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        return listBy(sc);
    }

    @Override
    public List<SSHKeyPairVO> listKeyPairsByFingerprint(long accountId, long domainId, String fingerprint) {
        SearchCriteria<SSHKeyPairVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        sc.addAnd("fingerprint", SearchCriteria.Op.EQ, fingerprint);
        return listBy(sc);
    }

    @Override
    public SSHKeyPairVO findByName(long accountId, long domainId, String name) {
        SearchCriteria<SSHKeyPairVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        return findOneBy(sc);
    }

    @Override
    public SSHKeyPairVO findByPublicKey(String publicKey) {
        SearchCriteria<SSHKeyPairVO> sc = createSearchCriteria();
        sc.addAnd("publicKey", SearchCriteria.Op.EQ, publicKey);
        return findOneBy(sc);
    }

    @Override
    public SSHKeyPairVO findByPublicKey(long accountId, long domainId, String publicKey) {
        SearchCriteria<SSHKeyPairVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        sc.addAnd("publicKey", SearchCriteria.Op.EQ, publicKey);
        return findOneBy(sc);
    }

    @Override
    public boolean deleteByName(long accountId, long domainId, String name) {
        SSHKeyPairVO pair = findByName(accountId, domainId, name);
        if (pair == null)
            return false;

        expunge(pair.getId());
        return true;
    }

}
