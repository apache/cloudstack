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
package com.cloud.network.security.dao;

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.network.security.SecurityGroupVMMapVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.vm.VirtualMachine.State;

@Component
public class SecurityGroupVMMapDaoImpl extends GenericDaoBase<SecurityGroupVMMapVO, Long> implements SecurityGroupVMMapDao {
    private SearchBuilder<SecurityGroupVMMapVO> ListByIpAndVmId;
    private SearchBuilder<SecurityGroupVMMapVO> ListByVmId;
    private SearchBuilder<SecurityGroupVMMapVO> ListByVmIdGroupId;
    protected GenericSearchBuilder<SecurityGroupVMMapVO, Long> CountSGForVm;

    private GenericSearchBuilder<SecurityGroupVMMapVO, Long> ListVmIdBySecurityGroup;

    private SearchBuilder<SecurityGroupVMMapVO> ListByIp;
    private SearchBuilder<SecurityGroupVMMapVO> ListBySecurityGroup;
    private SearchBuilder<SecurityGroupVMMapVO> ListBySecurityGroupAndStates;

    protected SecurityGroupVMMapDaoImpl() {
        ListByIpAndVmId = createSearchBuilder();
        ListByIpAndVmId.and("ipAddress", ListByIpAndVmId.entity().getGuestIpAddress(), SearchCriteria.Op.EQ);
        ListByIpAndVmId.and("instanceId", ListByIpAndVmId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByIpAndVmId.done();

        ListVmIdBySecurityGroup = createSearchBuilder(Long.class);
        ListVmIdBySecurityGroup.and("securityGroupId", ListVmIdBySecurityGroup.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        ListVmIdBySecurityGroup.selectFields(ListVmIdBySecurityGroup.entity().getInstanceId());
        ListVmIdBySecurityGroup.done();

        ListBySecurityGroup = createSearchBuilder();
        ListBySecurityGroup.and("securityGroupId", ListBySecurityGroup.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        ListBySecurityGroup.done();

        ListByIp = createSearchBuilder();
        ListByIp.and("ipAddress", ListByIp.entity().getGuestIpAddress(), SearchCriteria.Op.EQ);
        ListByIp.done();

        ListByVmId = createSearchBuilder();
        ListByVmId.and("instanceId", ListByVmId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByVmId.done();

        ListBySecurityGroupAndStates = createSearchBuilder();
        ListBySecurityGroupAndStates.and("securityGroupId", ListBySecurityGroupAndStates.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        ListBySecurityGroupAndStates.and("states", ListBySecurityGroupAndStates.entity().getVmState(), SearchCriteria.Op.IN);
        ListBySecurityGroupAndStates.done();

        ListByVmIdGroupId = createSearchBuilder();
        ListByVmIdGroupId.and("instanceId", ListByVmIdGroupId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.and("securityGroupId", ListByVmIdGroupId.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.done();

        CountSGForVm = createSearchBuilder(Long.class);
        CountSGForVm.select(null, Func.COUNT, null);
        CountSGForVm.and("vmId", CountSGForVm.entity().getInstanceId(), SearchCriteria.Op.EQ);
        CountSGForVm.done();
    }

    @Override
    public List<SecurityGroupVMMapVO> listByIpAndInstanceId(String ipAddress, long vmId) {
        SearchCriteria<SecurityGroupVMMapVO> sc = ListByIpAndVmId.create();
        sc.setParameters("ipAddress", ipAddress);
        sc.setParameters("instanceId", vmId);
        return listBy(sc);
    }

    @Override
    public List<SecurityGroupVMMapVO> listBySecurityGroup(long securityGroupId) {
        SearchCriteria<SecurityGroupVMMapVO> sc = ListBySecurityGroup.create();
        sc.setParameters("securityGroupId", securityGroupId);
        return listBy(sc);
    }

    @Override
    public List<SecurityGroupVMMapVO> listByIp(String ipAddress) {
        SearchCriteria<SecurityGroupVMMapVO> sc = ListByIp.create();
        sc.setParameters("ipAddress", ipAddress);
        return listBy(sc);
    }

    @Override
    public List<SecurityGroupVMMapVO> listByInstanceId(long vmId) {
        SearchCriteria<SecurityGroupVMMapVO> sc = ListByVmId.create();
        sc.setParameters("instanceId", vmId);
        return listBy(sc);
    }

    @Override
    public Pair<List<SecurityGroupVMMapVO>, Integer> listByInstanceId(long instanceId, Filter filter) {
        SearchCriteria<SecurityGroupVMMapVO> sc = ListByVmId.create();
        sc.setParameters("instanceId", instanceId);
        return this.searchAndCount(sc, filter);
    }

    @Override
    public int deleteVM(long instanceId) {
        SearchCriteria<SecurityGroupVMMapVO> sc = ListByVmId.create();
        sc.setParameters("instanceId", instanceId);
        return super.expunge(sc);
    }

    @Override
    public List<SecurityGroupVMMapVO> listBySecurityGroup(long securityGroupId, State... vmStates) {
        SearchCriteria<SecurityGroupVMMapVO> sc = ListBySecurityGroupAndStates.create();
        sc.setParameters("securityGroupId", securityGroupId);
        sc.setParameters("states", (Object[])vmStates);
        return listBy(sc, null, true);
    }

    @Override
    public List<Long> listVmIdsBySecurityGroup(long securityGroupId) {
        SearchCriteria<Long> sc = ListVmIdBySecurityGroup.create();
        sc.setParameters("securityGroupId", securityGroupId);
        return customSearchIncludingRemoved(sc, null);
    }

    @Override
    public SecurityGroupVMMapVO findByVmIdGroupId(long instanceId, long securityGroupId) {
        SearchCriteria<SecurityGroupVMMapVO> sc = ListByVmIdGroupId.create();
        sc.setParameters("securityGroupId", securityGroupId);
        sc.setParameters("instanceId", instanceId);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public long countSGForVm(long instanceId) {
        SearchCriteria<Long> sc = CountSGForVm.create();
        sc.setParameters("vmId", instanceId);
        return customSearch(sc, null).get(0);
    }

}
