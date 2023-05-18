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

import org.apache.cloudstack.api.response.VpcOfferingResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.api.query.vo.VpcOfferingJoinVO;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.net.NetUtils;

public class VpcOfferingJoinDaoImpl extends GenericDaoBase<VpcOfferingJoinVO, Long> implements VpcOfferingJoinDao {
    public static final Logger s_logger = Logger.getLogger(VpcOfferingJoinDaoImpl.class);

    private SearchBuilder<VpcOfferingJoinVO> sofIdSearch;

    protected VpcOfferingJoinDaoImpl() {

        sofIdSearch = createSearchBuilder();
        sofIdSearch.and("id", sofIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        sofIdSearch.done();

        this._count = "select count(distinct service_offering_view.id) from service_offering_view WHERE ";
    }

    @Override
    public List<VpcOfferingJoinVO> findByDomainId(long domainId) {
        SearchBuilder<VpcOfferingJoinVO> sb = createSearchBuilder();
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.FIND_IN_SET);
        sb.done();

        SearchCriteria<VpcOfferingJoinVO> sc = sb.create();
        sc.setParameters("domainId", String.valueOf(domainId));
        return listBy(sc);
    }

    @Override
    public VpcOfferingResponse newVpcOfferingResponse(VpcOffering offering) {
        VpcOfferingResponse offeringResponse = new VpcOfferingResponse();
        offeringResponse.setId(offering.getUuid());
        offeringResponse.setName(offering.getName());
        offeringResponse.setDisplayText(offering.getDisplayText());
        offeringResponse.setIsDefault(offering.isDefault());
        offeringResponse.setState(offering.getState().name());
        offeringResponse.setSupportsDistributedRouter(offering.isSupportsDistributedRouter());
        offeringResponse.setSupportsRegionLevelVpc(offering.isOffersRegionLevelVPC());
        offeringResponse.setCreated(offering.getCreated());
        if (offering instanceof VpcOfferingJoinVO) {
            VpcOfferingJoinVO offeringJoinVO = (VpcOfferingJoinVO) offering;
            offeringResponse.setDomainId(offeringJoinVO.getDomainUuid());
            offeringResponse.setDomain(offeringJoinVO.getDomainPath());
            offeringResponse.setZoneId(offeringJoinVO.getZoneUuid());
            offeringResponse.setZone(offeringJoinVO.getZoneName());
            String protocol = offeringJoinVO.getInternetProtocol();
            if (StringUtils.isEmpty(protocol)) {
                protocol = NetUtils.InternetProtocol.IPv4.toString();
            }
            offeringResponse.setInternetProtocol(protocol);
        }
        offeringResponse.setObjectName("vpcoffering");

        return offeringResponse;
    }

    @Override
    public VpcOfferingJoinVO newVpcOfferingView(VpcOffering offering) {
        SearchCriteria<VpcOfferingJoinVO> sc = sofIdSearch.create();
        sc.setParameters("id", offering.getId());
        List<VpcOfferingJoinVO> offerings = searchIncludingRemoved(sc, null, null, false);
        assert offerings != null && offerings.size() == 1 : "No VPC offering found for offering id " + offering.getId();
        return offerings.get(0);
    }
}
