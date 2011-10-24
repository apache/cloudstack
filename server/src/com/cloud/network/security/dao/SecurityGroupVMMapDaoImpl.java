/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.network.security.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.network.security.SecurityGroupVMMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VirtualMachine.State;

@Local(value={SecurityGroupVMMapDao.class})
public class SecurityGroupVMMapDaoImpl extends GenericDaoBase<SecurityGroupVMMapVO, Long> implements SecurityGroupVMMapDao {
    private SearchBuilder<SecurityGroupVMMapVO> ListByIpAndVmId;
    private SearchBuilder<SecurityGroupVMMapVO> ListByVmId;
    private SearchBuilder<SecurityGroupVMMapVO> ListByVmIdGroupId;

    private GenericSearchBuilder<SecurityGroupVMMapVO, Long> ListVmIdBySecurityGroup;

    private SearchBuilder<SecurityGroupVMMapVO> ListByIp;
    private SearchBuilder<SecurityGroupVMMapVO> ListBySecurityGroup;
    private SearchBuilder<SecurityGroupVMMapVO> ListBySecurityGroupAndStates;

    protected SecurityGroupVMMapDaoImpl() {
        ListByIpAndVmId  = createSearchBuilder();
        ListByIpAndVmId.and("ipAddress", ListByIpAndVmId.entity().getGuestIpAddress(), SearchCriteria.Op.EQ);
        ListByIpAndVmId.and("instanceId", ListByIpAndVmId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByIpAndVmId.done();

        ListVmIdBySecurityGroup = createSearchBuilder(Long.class);
        ListVmIdBySecurityGroup.and("securityGroupId", ListVmIdBySecurityGroup.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        ListVmIdBySecurityGroup.selectField(ListVmIdBySecurityGroup.entity().getInstanceId());
        ListVmIdBySecurityGroup.done();
        
        ListBySecurityGroup = createSearchBuilder();
        ListBySecurityGroup.and("securityGroupId", ListBySecurityGroup.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        ListBySecurityGroup.done();

        ListByIp  = createSearchBuilder();
        ListByIp.and("ipAddress", ListByIp.entity().getGuestIpAddress(), SearchCriteria.Op.EQ);
        ListByIp.done();

        ListByVmId  = createSearchBuilder();
        ListByVmId.and("instanceId", ListByVmId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByVmId.done();
        
        ListBySecurityGroupAndStates = createSearchBuilder();
        ListBySecurityGroupAndStates.and("securityGroupId", ListBySecurityGroupAndStates.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        ListBySecurityGroupAndStates.and("states", ListBySecurityGroupAndStates.entity().getVmState(), SearchCriteria.Op.IN);
        ListBySecurityGroupAndStates.done();
        
        ListByVmIdGroupId  = createSearchBuilder();
        ListByVmIdGroupId.and("instanceId", ListByVmIdGroupId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.and("securityGroupId", ListByVmIdGroupId.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.done();
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
	
}
