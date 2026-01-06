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
package com.cloud.network.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.utils.db.TransactionLegacy;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@DB()
public class NetworkDomainDaoImpl extends GenericDaoBase<NetworkDomainVO, Long> implements NetworkDomainDao {
    final SearchBuilder<NetworkDomainVO> AllFieldsSearch;
    final SearchBuilder<NetworkDomainVO> DomainsSearch;

    private static final String LIST_DOMAINS_OF_SHARED_NETWORKS_USED_BY_DOMAIN_PATH = "SELECT shared_nw.domain_id, \n" +
            "GROUP_CONCAT('VM:', vm.uuid, ' | NW:' , network.uuid) \n" +
            "FROM   cloud.domain_network_ref AS shared_nw\n" +
            "INNER  JOIN cloud.nics AS nic ON (nic.network_id = shared_nw.network_id AND nic.removed IS NULL)\n" +
            "INNER  JOIN cloud.vm_instance AS vm ON (vm.id = nic.instance_id)\n" +
            "INNER  JOIN cloud.domain AS domain ON (domain.id = vm.domain_id)\n" +
            "INNER  JOIN cloud.domain AS domain_sn ON (domain_sn.id = shared_nw.domain_id)\n" +
            "INNER  JOIN cloud.networks AS network ON (shared_nw.network_id = network.id)\n" +
            "WHERE  shared_nw.subdomain_access = 1\n" +
            "AND    domain.path LIKE ?\n" +
            "AND    domain_sn.path NOT LIKE ?\n" +
            "GROUP  BY shared_nw.network_id";

    protected NetworkDomainDaoImpl() {
        super();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("domainId", AllFieldsSearch.entity().getDomainId(), Op.EQ);
        AllFieldsSearch.and("networkId", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.done();

        DomainsSearch = createSearchBuilder();
        DomainsSearch.and("domainId", DomainsSearch.entity().getDomainId(), Op.IN);
        DomainsSearch.done();
    }

    @Override
    public List<NetworkDomainVO> listDomainNetworkMapByDomain(Object... domainId) {
        SearchCriteria<NetworkDomainVO> sc = DomainsSearch.create();
        sc.setParameters("domainId", domainId);

        return listBy(sc);
    }

    @Override
    public NetworkDomainVO getDomainNetworkMapByNetworkId(long networkId) {
        SearchCriteria<NetworkDomainVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }

    @Override
    public List<Long> listNetworkIdsByDomain(long domainId) {
        List<Long> networkIdsToReturn = new ArrayList<Long>();
        List<NetworkDomainVO> maps = listDomainNetworkMapByDomain(domainId);
        for (NetworkDomainVO map : maps) {
            networkIdsToReturn.add(map.getNetworkId());
        }
        return networkIdsToReturn;
    }

    @Override
    public Map<Long, List<String>> listDomainsOfSharedNetworksUsedByDomainPath(String domainPath) {
        logger.debug(String.format("Retrieving the domains of the shared networks with subdomain access used by domain with path [%s].", domainPath));

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try (PreparedStatement pstmt = txn.prepareStatement(LIST_DOMAINS_OF_SHARED_NETWORKS_USED_BY_DOMAIN_PATH)) {
            Map<Long, List<String>> domainsOfSharedNetworksUsedByDomainPath = new HashMap<>();

            String domainSearch = domainPath.concat("%");
            pstmt.setString(1, domainSearch);
            pstmt.setString(2, domainSearch);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Long domainId = rs.getLong(1);
                    List<String> vmUuidsAndNetworkUuids = Arrays.asList(rs.getString(2).split(","));

                    domainsOfSharedNetworksUsedByDomainPath.put(domainId, vmUuidsAndNetworkUuids);
                }
            }

            return domainsOfSharedNetworksUsedByDomainPath;
        } catch (SQLException e) {
            logger.error(String.format("Failed to retrieve the domains of the shared networks with subdomain access used by domain with path [%s] due to [%s]. Returning an empty "
                    + "list of domains.", domainPath, e.getMessage()));

            logger.debug(String.format("Failed to retrieve the domains of the shared networks with subdomain access used by domain with path [%s]. Returning an empty "
                    + "list of domains.", domainPath), e);

            return new HashMap<>();
        }
    }
}
