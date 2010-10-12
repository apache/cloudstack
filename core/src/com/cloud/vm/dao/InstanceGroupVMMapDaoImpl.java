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

package com.cloud.vm.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.InstanceGroupVMMapVO;

@Local(value={InstanceGroupVMMapDao.class})
public class InstanceGroupVMMapDaoImpl extends GenericDaoBase<InstanceGroupVMMapVO, Long> implements InstanceGroupVMMapDao{
	
	private SearchBuilder<InstanceGroupVMMapVO> ListByVmId;
	private SearchBuilder<InstanceGroupVMMapVO> ListByGroupId;
    private SearchBuilder<InstanceGroupVMMapVO> ListByVmIdGroupId;
	
	protected InstanceGroupVMMapDaoImpl() {
		ListByVmId  = createSearchBuilder();
		ListByVmId.and("instanceId", ListByVmId.entity().getInstanceId(), SearchCriteria.Op.EQ);
		ListByVmId.done();
		
		ListByGroupId  = createSearchBuilder();
		ListByGroupId.and("groupId", ListByGroupId.entity().getGroupId(), SearchCriteria.Op.EQ);
		ListByGroupId.done();
		
        ListByVmIdGroupId  = createSearchBuilder();
        ListByVmIdGroupId.and("instanceId", ListByVmIdGroupId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.and("groupId", ListByVmIdGroupId.entity().getGroupId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.done();
	}
	
    @Override
    public List<InstanceGroupVMMapVO> listByInstanceId(long vmId) {
        SearchCriteria<InstanceGroupVMMapVO> sc = ListByVmId.create();
        sc.setParameters("instanceId", vmId);
        return listBy(sc);
    }
    
    @Override
    public List<InstanceGroupVMMapVO> listByGroupId(long groupId) {
        SearchCriteria<InstanceGroupVMMapVO> sc = ListByGroupId.create();
        sc.setParameters("groupId", groupId);
        return listBy(sc);
    }
    
	@Override
	public InstanceGroupVMMapVO findByVmIdGroupId(long instanceId, long groupId) {
        SearchCriteria<InstanceGroupVMMapVO> sc = ListByVmIdGroupId.create();
        sc.setParameters("groupId", groupId);
        sc.setParameters("instanceId", instanceId);
		return findOneBy(sc);
	}

}
