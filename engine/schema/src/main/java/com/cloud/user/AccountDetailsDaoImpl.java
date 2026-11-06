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
package com.cloud.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.ConfigKey.Scope;
import org.apache.cloudstack.framework.config.ScopedConfigStorage;

import com.cloud.domain.DomainDetailVO;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.domain.dao.DomainDetailsDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;

public class AccountDetailsDaoImpl extends ResourceDetailsDaoBase<AccountDetailVO> implements AccountDetailsDao, ScopedConfigStorage {
    protected final SearchBuilder<AccountDetailVO> accountSearch;

    @Inject
    protected AccountDao _accountDao;
    @Inject
    protected DomainDao _domainDao;
    @Inject
    protected DomainDetailsDao _domainDetailsDao;

    // Registered/owned by com.cloud.configuration.ConfigurationManagerImpl#ENABLE_ACCOUNT_SETTINGS_FOR_DOMAIN;
    // declared here too so this module can read it without a reverse dependency on the server module.
    private static final ConfigKey<Boolean> EnableAccountSettingsForDomain = new ConfigKey<>("Advanced", Boolean.class, "enable.account.settings.for.domain", "false",
            "Indicates whether to add Account settings for domain. If True, Account settings will be added to domain settings, all Accounts in the domain will inherit the domain setting if Account setting is not set.", true);

    // Registered/owned by com.cloud.configuration.ConfigurationManagerImpl#ENABLE_DOMAIN_SETTINGS_FOR_CHILD_DOMAIN;
    // declared here too so this module can read it without a reverse dependency on the server module.
    private static final ConfigKey<Boolean> EnableDomainSettingsForChildDomain = new ConfigKey<>("Advanced", Boolean.class, "enable.domain.settings.for.child.domain", "false",
            "Indicates whether the settings of parent domain should be applied for child domain. If true, the child domain will get value from parent domain if its not configured in child domain else global value is taken.",
            true);

    protected AccountDetailsDaoImpl() {
        accountSearch = createSearchBuilder();
        accountSearch.and("accountId", accountSearch.entity().getResourceId(), Op.EQ);
        accountSearch.done();
    }

    @Override
    public Map<String, String> findDetails(long accountId) {
        QueryBuilder<AccountDetailVO> sc = QueryBuilder.create(AccountDetailVO.class);
        sc.and(sc.entity().getResourceId(), Op.EQ, accountId);
        List<AccountDetailVO> results = sc.list();
        Map<String, String> details = new HashMap<>(results.size());
        for (AccountDetailVO r : results) {
            details.put(r.getName(), r.getValue());
        }
        return details;
    }

    @Override
    public void persist(long accountId, Map<String, String> details) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<AccountDetailVO> sc = accountSearch.create();
        sc.setParameters("accountId", accountId);
        expunge(sc);
        for (Map.Entry<String, String> detail : details.entrySet()) {
            AccountDetailVO vo = new AccountDetailVO(accountId, detail.getKey(), detail.getValue());
            persist(vo);
        }
        txn.commit();
    }

    @Override
    public AccountDetailVO findDetail(long accountId, String name) {
        QueryBuilder<AccountDetailVO> sc = QueryBuilder.create(AccountDetailVO.class);
        sc.and(sc.entity().getResourceId(), Op.EQ, accountId);
        sc.and(sc.entity().getName(), Op.EQ, name);
        return sc.find();
    }

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new AccountDetailVO(resourceId, key, value));
    }

    @Override
    public void deleteDetails(long accountId) {
        SearchCriteria<AccountDetailVO> sc = accountSearch.create();
        sc.setParameters("accountId", accountId);
        List<AccountDetailVO> results = search(sc, null);
        for (AccountDetailVO result : results) {
            remove(result.getId());
        }
    }

    @Override
    public void update(long accountId, Map<String, String> details) {
        Map<String, String> oldDetails = findDetails(accountId);
        oldDetails.putAll(details);
        persist(accountId, oldDetails);
    }

    @Override
    public Scope getScope() {
        return ConfigKey.Scope.Account;
    }

    @Override
    public String getConfigValue(long id, String key) {
        // check if account level setting is configured
        AccountDetailVO vo = findDetail(id, key);
        String value = vo == null ? null : getActualValue(vo);
        if (value != null) {
            return value;
        }

        // if account level setting is not configured then check if
        // we can take value from domain
        if (!EnableAccountSettingsForDomain.value()) {
            return null;
        }

        // check if we can traverse till ROOT domain to get the value
        if (EnableDomainSettingsForChildDomain.value()) {
            Optional<AccountVO> account = Optional.ofNullable(_accountDao.findById(id));
            if (account.isPresent()) {
                DomainVO domain = _domainDao.findById(account.get().getDomainId());
                while (domain != null) {
                    DomainDetailVO domainVO = _domainDetailsDao.findDetail(domain.getId(), key);
                    if (domainVO != null) {
                        value = _domainDetailsDao.getActualValue(domainVO);
                        break;
                    } else if (domain.getParent() != null) {
                        domain = _domainDao.findById(domain.getParent());
                    } else {
                        break;
                    }
                }
            }
        }
        return value;
    }

    @Override
    public Pair<Scope, Long> getParentScope(long id) {
        Account account = _accountDao.findById(id);
        if (account == null) {
            return null;
        }
        return new Pair<>(getScope().getParent(), account.getDomainId());
    }
}
