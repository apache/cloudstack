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
package com.cloud.domain.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.cloud.domain.DomainDetailVO;
import com.cloud.domain.DomainVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.ConfigKey.Scope;
import org.apache.cloudstack.framework.config.ScopedConfigStorage;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

public class DomainDetailsDaoImpl extends GenericDaoBase<DomainDetailVO, Long> implements DomainDetailsDao, ScopedConfigStorage {
    protected final SearchBuilder<DomainDetailVO> domainSearch;

    @Inject
    protected DomainDao _domainDao;
    @Inject
    private ConfigurationDao _configDao;

    protected DomainDetailsDaoImpl() {
        domainSearch = createSearchBuilder();
        domainSearch.and("domainId", domainSearch.entity().getDomainId(), Op.EQ);
        domainSearch.done();
    }

    @Override
    public Map<String, String> findDetails(long domainId) {
        QueryBuilder<DomainDetailVO> sc = QueryBuilder.create(DomainDetailVO.class);
        sc.and(sc.entity().getDomainId(), Op.EQ, domainId);
        List<DomainDetailVO> results = sc.list();
        Map<String, String> details = new HashMap<String, String>(results.size());
        for (DomainDetailVO r : results) {
            details.put(r.getName(), r.getValue());
        }
        return details;
    }

    @Override
    public void persist(long domainId, Map<String, String> details) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<DomainDetailVO> sc = domainSearch.create();
        sc.setParameters("domainId", domainId);
        expunge(sc);
        for (Map.Entry<String, String> detail : details.entrySet()) {
            DomainDetailVO vo = new DomainDetailVO(domainId, detail.getKey(), detail.getValue());
            persist(vo);
        }
        txn.commit();
    }

    @Override
    public DomainDetailVO findDetail(long domainId, String name) {
        QueryBuilder<DomainDetailVO> sc = QueryBuilder.create(DomainDetailVO.class);
        sc.and(sc.entity().getDomainId(), Op.EQ, domainId);
        sc.and(sc.entity().getName(), Op.EQ, name);
        return sc.find();
    }

    @Override
    public void deleteDetails(long domainId) {
        SearchCriteria<DomainDetailVO> sc = domainSearch.create();
        sc.setParameters("domainId", domainId);
        List<DomainDetailVO> results = search(sc, null);
        for (DomainDetailVO result : results) {
            remove(result.getId());
        }
    }

    @Override
    public void update(long domainId, Map<String, String> details) {
        Map<String, String> oldDetails = findDetails(domainId);
        oldDetails.putAll(details);
        persist(domainId, oldDetails);
    }

    @Override
    public Scope getScope() {
        return Scope.Domain;
    }

    @Override
    public String getConfigValue(long id, ConfigKey<?> key) {
        DomainDetailVO vo = null;
        String enableDomainSettingsForChildDomain = _configDao.getValue("enable.domain.settings.for.child.domain");
        if (!Boolean.parseBoolean(enableDomainSettingsForChildDomain)) {
            vo = findDetail(id, key.key());
            return vo == null ? null : vo.getValue();
        }
        DomainVO domain = _domainDao.findById(id);
        // if value is not configured in domain then check its parent domain till ROOT
        while (domain != null) {
            vo = findDetail(domain.getId(), key.key());
            if (vo != null) {
                break;
            } else if (domain.getParent() != null) {
                domain = _domainDao.findById(domain.getParent());
            } else {
                break;
            }
        }
        return vo == null ? null : vo.getValue();
    }
}
