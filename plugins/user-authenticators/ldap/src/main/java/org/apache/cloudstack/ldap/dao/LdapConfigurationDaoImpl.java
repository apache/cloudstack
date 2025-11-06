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
package org.apache.cloudstack.ldap.dao;

import java.util.List;


import org.springframework.stereotype.Component;

import org.apache.cloudstack.ldap.LdapConfigurationVO;

import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
public class LdapConfigurationDaoImpl extends GenericDaoBase<LdapConfigurationVO, Long> implements LdapConfigurationDao {
    private final SearchBuilder<LdapConfigurationVO> hostnameSearch;
    private final SearchBuilder<LdapConfigurationVO> listGlobalConfigurationsSearch;
    private final SearchBuilder<LdapConfigurationVO> listDomainConfigurationsSearch;

    public LdapConfigurationDaoImpl() {
        super();
        hostnameSearch = createSearchBuilder();
        hostnameSearch.and("hostname", hostnameSearch.entity().getHostname(), SearchCriteria.Op.EQ);
        hostnameSearch.done();

        listGlobalConfigurationsSearch = createSearchBuilder();
        listGlobalConfigurationsSearch.and("hostname", listGlobalConfigurationsSearch.entity().getHostname(), Op.EQ);
        listGlobalConfigurationsSearch.and("port", listGlobalConfigurationsSearch.entity().getPort(), Op.EQ);
        listGlobalConfigurationsSearch.and("domain_id", listGlobalConfigurationsSearch.entity().getDomainId(),SearchCriteria.Op.NULL);
        listGlobalConfigurationsSearch.done();

        listDomainConfigurationsSearch = createSearchBuilder();
        listDomainConfigurationsSearch.and("id", listDomainConfigurationsSearch.entity().getId(), SearchCriteria.Op.EQ);
        listDomainConfigurationsSearch.and("hostname", listDomainConfigurationsSearch.entity().getHostname(), Op.EQ);
        listDomainConfigurationsSearch.and("port", listDomainConfigurationsSearch.entity().getPort(), Op.EQ);
        listDomainConfigurationsSearch.and("domain_id", listDomainConfigurationsSearch.entity().getDomainId(), Op.EQ);
        listDomainConfigurationsSearch.done();
    }

    @Override
    public LdapConfigurationVO findByHostname(final String hostname) {
        final SearchCriteria<LdapConfigurationVO> sc = hostnameSearch.create();
        sc.setParameters("hostname", hostname);
        return findOneBy(sc);
    }

    @Override
    public LdapConfigurationVO find(String hostname, int port, Long domainId) {
        SearchCriteria<LdapConfigurationVO> sc = getSearchCriteria(null, hostname, port, domainId, false);
        return findOneBy(sc);
    }

    @Override
    public LdapConfigurationVO find(String hostname, int port, Long domainId, boolean listAll) {
        SearchCriteria<LdapConfigurationVO> sc = getSearchCriteria(null, hostname, port, domainId, listAll);
        return findOneBy(sc);
    }

    @Override
    public Pair<List<LdapConfigurationVO>, Integer> searchConfigurations(final String hostname, final int port, final Long domainId) {
        SearchCriteria<LdapConfigurationVO> sc = getSearchCriteria(null, hostname, port, domainId, false);
        return searchAndCount(sc, null);
    }

    @Override
    public Pair<List<LdapConfigurationVO>, Integer> searchConfigurations(final Long id, final String hostname, final int port, final Long domainId, final boolean listAll) {
        SearchCriteria<LdapConfigurationVO> sc = getSearchCriteria(id, hostname, port, domainId, listAll);
        return searchAndCount(sc, null);
    }

    private SearchCriteria<LdapConfigurationVO> getSearchCriteria(Long id, String hostname, int port, Long domainId,boolean listAll) {
        SearchCriteria<LdapConfigurationVO> sc;
        if (id != null) {
            // If id is present, ignore all other parameters
            sc = listDomainConfigurationsSearch.create();
            sc.setParameters("id", id);
        } else if (domainId != null) {
            // If domainid is present, ignore listall
            sc = listDomainConfigurationsSearch.create();
            sc.setParameters("domain_id", domainId);
        } else if (listAll) {
            sc = listDomainConfigurationsSearch.create();
        } else {
            sc = listGlobalConfigurationsSearch.create();
        }
        if (hostname != null) {
            sc.setParameters("hostname", hostname);
        }
        if (port > 0) {
            sc.setParameters("port", port);
        }
        return sc;
    }
}
