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
package org.apache.cloudstack.affinity.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.cloudstack.affinity.AffinityGroupDomainMapVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;

public class AffinityGroupDomainMapDaoImpl extends GenericDaoBase<AffinityGroupDomainMapVO, Long> implements AffinityGroupDomainMapDao {

    private SearchBuilder<AffinityGroupDomainMapVO> ListByAffinityGroup;

    private SearchBuilder<AffinityGroupDomainMapVO> DomainsSearch;

    private static final String LIST_DOMAINS_WITH_AFFINITY_GROUPS_WITH_SUBDOMAIN_ACCESS_USED_BY_DOMAIN_PATH = "SELECT affinity_group_domain_map.domain_id, \n" +
            "GROUP_CONCAT('VM:', vm.uuid, ' | AG:' , affinity_group.uuid) \n" +
            "FROM  cloud.affinity_group_domain_map AS affinity_group_domain_map\n" +
            "INNER JOIN cloud.affinity_group_vm_map AS affinity_group_vm_map ON (cloud.affinity_group_domain_map.affinity_group_id = affinity_group_vm_map.affinity_group_id)\n" +
            "INNER JOIN cloud.vm_instance AS vm ON (vm.id = affinity_group_vm_map.instance_id)\n" +
            "INNER JOIN cloud.domain AS domain ON (domain.id = vm.domain_id)\n" +
            "INNER  JOIN cloud.domain AS domain_sn ON (domain_sn.id = affinity_group_domain_map.domain_id)\n" +
            "INNER JOIN cloud.affinity_group AS affinity_group ON (affinity_group.id = affinity_group_domain_map.affinity_group_id)\n" +
            "WHERE affinity_group_domain_map.subdomain_access = 1\n" +
            "AND   domain.path LIKE ?\n" +
            "AND   domain_sn.path NOT LIKE ?\n" +
            "GROUP BY affinity_group.id";

    public AffinityGroupDomainMapDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        ListByAffinityGroup = createSearchBuilder();
        ListByAffinityGroup.and("affinityGroupId", ListByAffinityGroup.entity().getAffinityGroupId(), SearchCriteria.Op.EQ);
        ListByAffinityGroup.done();

        DomainsSearch = createSearchBuilder();
        DomainsSearch.and("domainId", DomainsSearch.entity().getDomainId(), Op.IN);
        DomainsSearch.done();
    }

    @Override
    public AffinityGroupDomainMapVO findByAffinityGroup(long affinityGroupId) {
        SearchCriteria<AffinityGroupDomainMapVO> sc = ListByAffinityGroup.create();
        sc.setParameters("affinityGroupId", affinityGroupId);
        return findOneBy(sc);
    }

    @Override
    public List<AffinityGroupDomainMapVO> listByDomain(Object... domainId) {
        SearchCriteria<AffinityGroupDomainMapVO> sc = DomainsSearch.create();
        sc.setParameters("domainId", domainId);

        return listBy(sc);
    }

    @Override
    public Map<Long, List<String>> listDomainsOfAffinityGroupsUsedByDomainPath(String domainPath) {
        logger.debug(String.format("Retrieving the domains of the affinity groups with subdomain access used by domain with path [%s].", domainPath));

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try (PreparedStatement pstmt = txn.prepareStatement(LIST_DOMAINS_WITH_AFFINITY_GROUPS_WITH_SUBDOMAIN_ACCESS_USED_BY_DOMAIN_PATH)) {
            Map<Long, List<String>> domainsOfAffinityGroupsUsedByDomainPath = new HashMap<>();

            String domainSearch = domainPath.concat("%");
            pstmt.setString(1, domainSearch);
            pstmt.setString(2, domainSearch);


            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Long domainId = rs.getLong(1);
                    List<String> vmUuidsAndAffinityGroupUuids = Arrays.asList(rs.getString(2).split(","));

                    domainsOfAffinityGroupsUsedByDomainPath.put(domainId, vmUuidsAndAffinityGroupUuids);
                }
            }

            return domainsOfAffinityGroupsUsedByDomainPath;
        } catch (SQLException e) {
            logger.error(String.format("Failed to retrieve the domains of the affinity groups with subdomain access used by domain with path [%s] due to [%s]. Returning an " +
                    "empty list of domains.", domainPath, e.getMessage()));

            logger.debug(String.format("Failed to retrieve the domains of the affinity groups with subdomain access used by domain with path [%s]. Returning an empty "
                    + "list of domains.", domainPath), e);

            return new HashMap<>();
        }
    }

}
