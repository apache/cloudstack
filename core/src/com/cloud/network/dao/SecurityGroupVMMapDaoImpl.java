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

package com.cloud.network.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.network.SecurityGroupVMMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={SecurityGroupVMMapDao.class})
public class SecurityGroupVMMapDaoImpl extends GenericDaoBase<SecurityGroupVMMapVO, Long> implements SecurityGroupVMMapDao {
    private SearchBuilder<SecurityGroupVMMapVO> ListByIpAndVmId;
    private SearchBuilder<SecurityGroupVMMapVO> ListByVmId;
    private SearchBuilder<SecurityGroupVMMapVO> ListByIp;
    private SearchBuilder<SecurityGroupVMMapVO> ListBySecurityGroup;

    protected SecurityGroupVMMapDaoImpl() {
        ListByIpAndVmId  = createSearchBuilder();
        ListByIpAndVmId.and("ipAddress", ListByIpAndVmId.entity().getIpAddress(), SearchCriteria.Op.EQ);
        ListByIpAndVmId.and("instanceId", ListByIpAndVmId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByIpAndVmId.done();

        ListBySecurityGroup = createSearchBuilder();
        ListBySecurityGroup.and("securityGroupId", ListBySecurityGroup.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        ListBySecurityGroup.done();

        ListByIp  = createSearchBuilder();
        ListByIp.and("ipAddress", ListByIp.entity().getIpAddress(), SearchCriteria.Op.EQ);
        ListByIp.done();

        ListByVmId  = createSearchBuilder();
        ListByVmId.and("instanceId", ListByVmId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByVmId.done();
    }

    public List<SecurityGroupVMMapVO> listByIpAndInstanceId(String ipAddress, long vmId) {
        SearchCriteria sc = ListByIpAndVmId.create();
        sc.setParameters("ipAddress", ipAddress);
        sc.setParameters("instanceId", vmId);
        return listActiveBy(sc);
    }

    @Override
    public List<SecurityGroupVMMapVO> listBySecurityGroup(long securityGroupId) {
        SearchCriteria sc = ListBySecurityGroup.create();
        sc.setParameters("securityGroupId", securityGroupId);
        return listActiveBy(sc);
    }

    @Override
    public List<SecurityGroupVMMapVO> listByIp(String ipAddress) {
        SearchCriteria sc = ListByIp.create();
        sc.setParameters("ipAddress", ipAddress);
        return listActiveBy(sc);
    }

    @Override
    public List<SecurityGroupVMMapVO> listByInstanceId(long vmId) {
        SearchCriteria sc = ListByVmId.create();
        sc.setParameters("instanceId", vmId);
        return listActiveBy(sc);
    }

    @Override
    public int delete(SearchCriteria sc) {
        return super.delete(sc);
    }
}
