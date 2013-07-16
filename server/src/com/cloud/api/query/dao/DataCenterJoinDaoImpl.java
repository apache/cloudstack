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

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.dc.DataCenter;

import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.user.Account;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import org.springframework.stereotype.Component;

@Component
@Local(value={DataCenterJoinDao.class})
public class DataCenterJoinDaoImpl extends GenericDaoBase<DataCenterJoinVO, Long> implements DataCenterJoinDao {
    public static final Logger s_logger = Logger.getLogger(DataCenterJoinDaoImpl.class);


    private SearchBuilder<DataCenterJoinVO> dofIdSearch;

    protected DataCenterJoinDaoImpl() {

        dofIdSearch = createSearchBuilder();
        dofIdSearch.and("id", dofIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        dofIdSearch.done();

        this._count = "select count(distinct id) from data_center_view WHERE ";
    }



    @Override
    public ZoneResponse newDataCenterResponse(DataCenterJoinVO dataCenter, Boolean showCapacities) {

        Account account = CallContext.current().getCallingAccount();
        ZoneResponse zoneResponse = new ZoneResponse();
        zoneResponse.setId(dataCenter.getUuid());
        zoneResponse.setName(dataCenter.getName());
        zoneResponse.setSecurityGroupsEnabled(ApiDBUtils.isSecurityGroupEnabledInZone(dataCenter.getId()));
        zoneResponse.setLocalStorageEnabled(dataCenter.isLocalStorageEnabled());

        if ((dataCenter.getDescription() != null) && !dataCenter.getDescription().equalsIgnoreCase("null")) {
            zoneResponse.setDescription(dataCenter.getDescription());
        }

        if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN)) {
            zoneResponse.setDns1(dataCenter.getDns1());
            zoneResponse.setDns2(dataCenter.getDns2());
            zoneResponse.setIp6Dns1(dataCenter.getIp6Dns1());
            zoneResponse.setIp6Dns2(dataCenter.getIp6Dns2());
            zoneResponse.setInternalDns1(dataCenter.getInternalDns1());
            zoneResponse.setInternalDns2(dataCenter.getInternalDns2());
            // FIXME zoneResponse.setVlan(dataCenter.get.getVnet());
            zoneResponse.setGuestCidrAddress(dataCenter.getGuestNetworkCidr());
        }

        if (showCapacities != null && showCapacities) {
            zoneResponse.setCapacitites(ApiResponseHelper.getDataCenterCapacityResponse(dataCenter.getId()));
        }

        // set network domain info
        zoneResponse.setDomain(dataCenter.getDomain());

        // set domain info

        zoneResponse.setDomainId(dataCenter.getDomainUuid());
        zoneResponse.setDomainName(dataCenter.getDomainName());

        zoneResponse.setType(dataCenter.getNetworkType().toString());
        zoneResponse.setAllocationState(dataCenter.getAllocationState().toString());
        zoneResponse.setZoneToken(dataCenter.getZoneToken());
        zoneResponse.setDhcpProvider(dataCenter.getDhcpProvider());
        zoneResponse.setObjectName("zone");
        return zoneResponse;
    }


    @Override
    public DataCenterJoinVO newDataCenterView(DataCenter dataCenter) {
        SearchCriteria<DataCenterJoinVO> sc = dofIdSearch.create();
        sc.setParameters("id", dataCenter.getId());
        List<DataCenterJoinVO> dcs = searchIncludingRemoved(sc, null, null, false);
        assert dcs != null && dcs.size() == 1 : "No data center found for data center id " + dataCenter.getId();
        return dcs.get(0);
    }


}
