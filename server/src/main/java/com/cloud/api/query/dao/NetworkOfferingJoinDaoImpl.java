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

package com.cloud.api.query.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.utils.db.TransactionLegacy;
import org.apache.commons.lang3.StringUtils;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;

import com.cloud.api.query.vo.NetworkOfferingJoinVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.net.NetUtils;

public class NetworkOfferingJoinDaoImpl extends GenericDaoBase<NetworkOfferingJoinVO, Long> implements NetworkOfferingJoinDao {

    private final SearchBuilder<NetworkOfferingJoinVO> nofIdSearch;

    protected NetworkOfferingJoinDaoImpl() {

        nofIdSearch = createSearchBuilder();
        nofIdSearch.and("id", nofIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        nofIdSearch.done();

        _count = "select count(distinct id) from network_offering_view WHERE ";
    }

    private static final String LIST_DOMAINS_OF_NETWORK_OFFERINGS_USED_BY_DOMAIN_PATH = "SELECT nov.domain_id, \n" +
            "            GROUP_CONCAT('Network:', net.uuid) \n" +
            "            FROM   cloud.network_offering_view AS nov\n" +
            "            INNER  JOIN cloud.networks AS net ON (net.network_offering_id  = nov.id) \n" +
            "            INNER  JOIN cloud.domain AS domain ON (domain.id = net.domain_id) \n" +
            "            INNER  JOIN cloud.domain AS domain_so ON (domain_so.id = nov.domain_id) \n" +
            "            WHERE  domain.path LIKE ? \n" +
            "            AND    domain_so.path NOT LIKE ? \n" +
            "            AND    net.removed IS NULL \n" +
            "            GROUP  BY nov.id";

    @Override
    public List<NetworkOfferingJoinVO> findByDomainId(long domainId, Boolean includeAllDomainOffering) {
        SearchBuilder<NetworkOfferingJoinVO> sb = createSearchBuilder();
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.FIND_IN_SET);
        if (includeAllDomainOffering) {
            sb.or("dId", sb.entity().getDomainId(), SearchCriteria.Op.NULL);
        }
        sb.done();

        SearchCriteria<NetworkOfferingJoinVO> sc = sb.create();
        sc.setParameters("domainId", String.valueOf(domainId));
        return listBy(sc);
    }

    @Override
    public List<NetworkOfferingJoinVO> findByZoneId(long zoneId, Boolean includeAllZoneOffering) {
        SearchBuilder<NetworkOfferingJoinVO> sb = createSearchBuilder();
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.FIND_IN_SET);
        if (includeAllZoneOffering) {
            sb.or("zId", sb.entity().getZoneId(), SearchCriteria.Op.NULL);
        }
        sb.done();

        SearchCriteria<NetworkOfferingJoinVO> sc = sb.create();
        sc.setParameters("zoneId", String.valueOf(zoneId));
        return listBy(sc);
    }

    @Override
    public NetworkOfferingResponse newNetworkOfferingResponse(NetworkOffering offering) {
        NetworkOfferingResponse networkOfferingResponse = new NetworkOfferingResponse();
        networkOfferingResponse.setId(offering.getUuid());
        networkOfferingResponse.setName(offering.getName());
        networkOfferingResponse.setDisplayText(offering.getDisplayText());
        networkOfferingResponse.setTags(offering.getTags());
        networkOfferingResponse.setTrafficType(offering.getTrafficType().toString());
        networkOfferingResponse.setIsDefault(offering.isDefault());
        networkOfferingResponse.setSpecifyVlan(offering.isSpecifyVlan());
        networkOfferingResponse.setConserveMode(offering.isConserveMode());
        networkOfferingResponse.setSpecifyIpRanges(offering.isSpecifyIpRanges());
        networkOfferingResponse.setAvailability(offering.getAvailability().toString());
        networkOfferingResponse.setIsPersistent(offering.isPersistent());
        networkOfferingResponse.setEgressDefaultPolicy(offering.isEgressDefaultPolicy());
        networkOfferingResponse.setConcurrentConnections(offering.getConcurrentConnections());
        networkOfferingResponse.setSupportsStrechedL2Subnet(offering.isSupportingStrechedL2());
        networkOfferingResponse.setSupportsPublicAccess(offering.isSupportingPublicAccess());
        networkOfferingResponse.setSupportsInternalLb(offering.isInternalLb());
        networkOfferingResponse.setCreated(offering.getCreated());
        if (offering.getGuestType() != null) {
            networkOfferingResponse.setGuestIpType(offering.getGuestType().toString());
        }
        networkOfferingResponse.setState(offering.getState().name());
        if (offering instanceof NetworkOfferingJoinVO) {
            NetworkOfferingJoinVO networkOfferingJoinVO = (NetworkOfferingJoinVO)offering;
            networkOfferingResponse.setDomainId(networkOfferingJoinVO.getDomainUuid());
            networkOfferingResponse.setDomain(networkOfferingJoinVO.getDomainPath());
            networkOfferingResponse.setZoneId(networkOfferingJoinVO.getZoneUuid());
            networkOfferingResponse.setZone(networkOfferingJoinVO.getZoneName());
            String protocol = networkOfferingJoinVO.getInternetProtocol();
            if (StringUtils.isEmpty(protocol)) {
                protocol = NetUtils.InternetProtocol.IPv4.toString();
            }
            networkOfferingResponse.setInternetProtocol(protocol);
        }
        if (offering.getRoutingMode() != null) {
            networkOfferingResponse.setRoutingMode(offering.getRoutingMode().toString());
        }
        if (offering.isSpecifyAsNumber() != null) {
            networkOfferingResponse.setSpecifyAsNumber(offering.isSpecifyAsNumber());
        }
        networkOfferingResponse.setObjectName("networkoffering");

        return networkOfferingResponse;
    }

    @Override
    public NetworkOfferingJoinVO newNetworkOfferingView(NetworkOffering offering) {
        SearchCriteria<NetworkOfferingJoinVO> sc = nofIdSearch.create();
        sc.setParameters("id", offering.getId());
        List<NetworkOfferingJoinVO> offerings = searchIncludingRemoved(sc, null, null, false);
        assert offerings != null && offerings.size() == 1 : "No network offering found for offering id " + offering.getId();
        return offerings.get(0);
    }



    @Override
    public Map<Long, List<String>> listDomainsOfNetworkOfferingsUsedByDomainPath(String domainPath) {
        logger.debug(String.format("Retrieving the domains of the network offerings used by domain with path [%s].", domainPath));

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try (PreparedStatement pstmt = txn.prepareStatement(LIST_DOMAINS_OF_NETWORK_OFFERINGS_USED_BY_DOMAIN_PATH)) {
            Map<Long, List<String>> domainsOfNetworkOfferingsUsedByDomainPath = new HashMap<>();

            String domainSearch = domainPath.concat("%");
            pstmt.setString(1, domainSearch);
            pstmt.setString(2, domainSearch);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Long domainId = rs.getLong(1);
                    List<String> networkUuids = Arrays.asList(rs.getString(2).split(","));

                    domainsOfNetworkOfferingsUsedByDomainPath.put(domainId, networkUuids);
                }
            }

            return domainsOfNetworkOfferingsUsedByDomainPath;
        } catch (SQLException e) {
            logger.error(String.format("Failed to retrieve the domains of the network offerings used by domain with path [%s] due to [%s]. Returning an empty "
                    + "list of domains.", domainPath, e.getMessage()));

            logger.debug(String.format("Failed to retrieve the domains of the network offerings used by domain with path [%s]. Returning an empty " +
                    "list of domains.", domainPath), e);

            return new HashMap<>();
        }
    }
}
