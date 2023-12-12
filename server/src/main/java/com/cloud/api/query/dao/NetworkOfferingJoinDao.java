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

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.response.NetworkOfferingResponse;

import com.cloud.api.query.vo.NetworkOfferingJoinVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.db.GenericDao;

public interface NetworkOfferingJoinDao extends GenericDao<NetworkOfferingJoinVO, Long> {

    /**
     * Returns list of network offerings for a given domain
     * NetworkOfferingJoinVO can have multiple domains set. Method will search for
     * given domainId in list of domains for the offering.
     * @param long domainId
     * @param Boolean includeAllDomainOffering (if set to true offerings for which domain
     *                is not set will also be returned)
     * @return List<NetworkOfferingJoinVO> List of network offerings
     */
    List<NetworkOfferingJoinVO> findByDomainId(long domainId, Boolean includeAllDomainOffering);

    /**
     * Returns list of network offerings for a given zone
     * NetworkOfferingJoinVO can have multiple zones set. Method will search for
     * given zoneId in list of zones for the offering.
     * @param long zoneId
     * @param Boolean includeAllZoneOffering (if set to true offerings for which zone
     *                is not set will also be returned)
     * @return List<NetworkOfferingJoinVO> List of network offerings
     */
    List<NetworkOfferingJoinVO> findByZoneId(long zoneId, Boolean includeAllZoneOffering);

    NetworkOfferingResponse newNetworkOfferingResponse(NetworkOffering nof);

    NetworkOfferingJoinVO newNetworkOfferingView(NetworkOffering nof);

    Map<Long, List<String>> listDomainsOfNetworkOfferingsUsedByDomainPath(String domainPath);
}
