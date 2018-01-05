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
    private final SearchBuilder<LdapConfigurationVO> listAllConfigurationsSearch;

    public LdapConfigurationDaoImpl() {
        super();
        hostnameSearch = createSearchBuilder();
        hostnameSearch.and("hostname", hostnameSearch.entity().getHostname(), SearchCriteria.Op.EQ);
        hostnameSearch.done();

        listAllConfigurationsSearch = createSearchBuilder();
        listAllConfigurationsSearch.and("hostname", listAllConfigurationsSearch.entity().getHostname(), Op.EQ);
        listAllConfigurationsSearch.and("port", listAllConfigurationsSearch.entity().getPort(), Op.EQ);
        listAllConfigurationsSearch.and("domain_id", listAllConfigurationsSearch.entity().getDomainId(), Op.EQ);
        listAllConfigurationsSearch.done();
    }

    @Override
    public LdapConfigurationVO findByHostname(final String hostname) {
        final SearchCriteria<LdapConfigurationVO> sc = hostnameSearch.create();
        sc.setParameters("hostname", hostname);
        return findOneBy(sc);
    }

    @Override
    public LdapConfigurationVO find(String hostname, int port, Long domainId) {
        final SearchCriteria<LdapConfigurationVO> sc = hostnameSearch.create();
        if (hostname != null) {
            sc.setParameters("hostname", hostname);
        }
        if (port > 0) {
            sc.setParameters("port", port);
        }
        if (domainId != null) {
            sc.setParameters("domain_id", domainId);
        }
        return findOneBy(sc);
    }

    @Override
    public Pair<List<LdapConfigurationVO>, Integer> searchConfigurations(final String hostname, final int port, final Long domainId) {
        final SearchCriteria<LdapConfigurationVO> sc = listAllConfigurationsSearch.create();
        if (hostname != null) {
            sc.setParameters("hostname", hostname);
        }
        if (port > 0) {
            sc.setParameters("port", port);
        }
        if (domainId != null) {
            sc.setParameters("domain_id", domainId);
        }
        return searchAndCount(sc, null);
    }
}