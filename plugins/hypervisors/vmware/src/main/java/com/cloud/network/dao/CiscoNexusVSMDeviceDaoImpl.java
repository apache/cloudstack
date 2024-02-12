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

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.network.CiscoNexusVSMDeviceVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@DB
public class CiscoNexusVSMDeviceDaoImpl extends GenericDaoBase<CiscoNexusVSMDeviceVO, Long> implements CiscoNexusVSMDeviceDao {
    final SearchBuilder<CiscoNexusVSMDeviceVO> mgmtVlanIdSearch;
    final SearchBuilder<CiscoNexusVSMDeviceVO> domainIdSearch;
    final SearchBuilder<CiscoNexusVSMDeviceVO> nameSearch;
    final SearchBuilder<CiscoNexusVSMDeviceVO> ipaddrSearch;
    final SearchBuilder<CiscoNexusVSMDeviceVO> genericVlanIdSearch;
    final SearchBuilder<CiscoNexusVSMDeviceVO> fullTableSearch;

    // We will add more searchbuilder objects.

    public CiscoNexusVSMDeviceDaoImpl() {
        super();

        mgmtVlanIdSearch = createSearchBuilder();
        mgmtVlanIdSearch.and("managementVlan", mgmtVlanIdSearch.entity().getManagementVlan(), Op.EQ);
        mgmtVlanIdSearch.done();

        genericVlanIdSearch = createSearchBuilder();
        genericVlanIdSearch.and("managementVlan", genericVlanIdSearch.entity().getManagementVlan(), Op.EQ);
        genericVlanIdSearch.or("controlVlan", genericVlanIdSearch.entity().getControlVlan(), Op.EQ);
        genericVlanIdSearch.or("packetVlan", genericVlanIdSearch.entity().getPacketVlan(), Op.EQ);
        genericVlanIdSearch.or("storageVlan", genericVlanIdSearch.entity().getStorageVlan(), Op.EQ);
        genericVlanIdSearch.done();

        domainIdSearch = createSearchBuilder();
        domainIdSearch.and("vsmSwitchDomainId", domainIdSearch.entity().getvsmDomainId(), Op.EQ);
        domainIdSearch.done();

        nameSearch = createSearchBuilder();
        nameSearch.and("vsmName", nameSearch.entity().getvsmName(), Op.EQ);
        nameSearch.done();

        ipaddrSearch = createSearchBuilder();
        ipaddrSearch.and("ipaddr", ipaddrSearch.entity().getipaddr(), Op.EQ);
        ipaddrSearch.done();

        fullTableSearch = createSearchBuilder();
        fullTableSearch.done();

        // We may add more and conditions by specifying more fields, like say, accountId.
    }

    @Override
    public CiscoNexusVSMDeviceVO getVSMbyDomainId(long domId) {
        SearchCriteria<CiscoNexusVSMDeviceVO> sc = domainIdSearch.create();
        sc.setParameters("vsmSwitchDomainId", domId);
        return findOneBy(sc);
    }

    @Override
    public CiscoNexusVSMDeviceVO getVSMbyName(String vsmName) {
        SearchCriteria<CiscoNexusVSMDeviceVO> sc = nameSearch.create();
        sc.setParameters("vsmName", vsmName);
        return findOneBy(sc);
    }

    @Override
    public CiscoNexusVSMDeviceVO getVSMbyIpaddress(String ipaddress) {
        SearchCriteria<CiscoNexusVSMDeviceVO> sc = ipaddrSearch.create();
        sc.setParameters("ipaddr", ipaddress);
        return findOneBy(sc);
    }

    @Override
    public List<CiscoNexusVSMDeviceVO> listByMgmtVlan(int vlanId) {
        SearchCriteria<CiscoNexusVSMDeviceVO> sc = mgmtVlanIdSearch.create();
        sc.setParameters("managementVlan", vlanId);
        return search(sc, null);
    }

    @Override
    public List<CiscoNexusVSMDeviceVO> listAllVSMs() {
        SearchCriteria<CiscoNexusVSMDeviceVO> sc = fullTableSearch.create();
        return search(sc, null);
    }

    @Override
    public List<CiscoNexusVSMDeviceVO> listByVlanId(int vlanId) {
        SearchCriteria<CiscoNexusVSMDeviceVO> sc = genericVlanIdSearch.create();
        sc.setParameters("managementVlan", vlanId);
        sc.setParameters("storageVlan", vlanId);
        sc.setParameters("packetVlan", vlanId);
        sc.setParameters("controlVlan", vlanId);
        return search(sc, null);
    }
}
