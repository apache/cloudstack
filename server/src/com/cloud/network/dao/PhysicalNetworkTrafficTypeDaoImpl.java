/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value=PhysicalNetworkTrafficTypeDao.class) @DB(txn=false)
public class PhysicalNetworkTrafficTypeDaoImpl extends GenericDaoBase<PhysicalNetworkTrafficTypeVO, Long> implements PhysicalNetworkTrafficTypeDao {
    final SearchBuilder<PhysicalNetworkTrafficTypeVO> physicalNetworkSearch;
    final GenericSearchBuilder<PhysicalNetworkTrafficTypeVO, String> kvmAllFieldsSearch;
    final GenericSearchBuilder<PhysicalNetworkTrafficTypeVO, String> xenAllFieldsSearch;
    final GenericSearchBuilder<PhysicalNetworkTrafficTypeVO, String> vmWareAllFieldsSearch;
    final GenericSearchBuilder<PhysicalNetworkTrafficTypeVO, String> simulatorAllFieldsSearch;
    final GenericSearchBuilder<PhysicalNetworkTrafficTypeVO, String> ovmAllFieldsSearch;

    protected PhysicalNetworkTrafficTypeDaoImpl() {
        super();
        physicalNetworkSearch = createSearchBuilder();
        physicalNetworkSearch.and("physicalNetworkId", physicalNetworkSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkSearch.and("trafficType", physicalNetworkSearch.entity().getTrafficType(), Op.EQ);
        physicalNetworkSearch.done();

        kvmAllFieldsSearch = createSearchBuilder(String.class);
        kvmAllFieldsSearch.and("physicalNetworkId", kvmAllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        kvmAllFieldsSearch.and("trafficType", kvmAllFieldsSearch.entity().getTrafficType(), Op.EQ);
        kvmAllFieldsSearch.selectField(kvmAllFieldsSearch.entity().getKvmNetworkLabel());
        kvmAllFieldsSearch.done();

        xenAllFieldsSearch = createSearchBuilder(String.class);
        xenAllFieldsSearch.and("physicalNetworkId", xenAllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        xenAllFieldsSearch.and("trafficType", xenAllFieldsSearch.entity().getTrafficType(), Op.EQ);
        xenAllFieldsSearch.selectField(xenAllFieldsSearch.entity().getXenNetworkLabel());
        xenAllFieldsSearch.done();

        vmWareAllFieldsSearch = createSearchBuilder(String.class);
        vmWareAllFieldsSearch.and("physicalNetworkId", vmWareAllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        vmWareAllFieldsSearch.and("trafficType", vmWareAllFieldsSearch.entity().getTrafficType(), Op.EQ);
        vmWareAllFieldsSearch.selectField(vmWareAllFieldsSearch.entity().getVmwareNetworkLabel());
        vmWareAllFieldsSearch.done();
        
        simulatorAllFieldsSearch = createSearchBuilder(String.class);
        simulatorAllFieldsSearch.and("physicalNetworkId", simulatorAllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        simulatorAllFieldsSearch.and("trafficType", simulatorAllFieldsSearch.entity().getTrafficType(), Op.EQ);
        simulatorAllFieldsSearch.selectField(simulatorAllFieldsSearch.entity().getSimulatorNetworkLabel());
        simulatorAllFieldsSearch.done();
        
        ovmAllFieldsSearch = createSearchBuilder(String.class);
        ovmAllFieldsSearch.and("physicalNetworkId", ovmAllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        ovmAllFieldsSearch.and("trafficType", ovmAllFieldsSearch.entity().getTrafficType(), Op.EQ);
        ovmAllFieldsSearch.selectField(ovmAllFieldsSearch.entity().getSimulatorNetworkLabel());
        ovmAllFieldsSearch.done();
    }

    @Override
    public List<PhysicalNetworkTrafficTypeVO> listBy(long physicalNetworkId) {
        SearchCriteria<PhysicalNetworkTrafficTypeVO> sc = physicalNetworkSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return search(sc, null);
    }

    @Override
    public boolean isTrafficTypeSupported(long physicalNetworkId, TrafficType trafficType){
        SearchCriteria<PhysicalNetworkTrafficTypeVO> sc = physicalNetworkSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("trafficType", trafficType);
        if (findOneBy(sc) != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getNetworkTag(long physicalNetworkId, TrafficType trafficType, HypervisorType hType) {
        SearchCriteria<String> sc = null;
        if (hType == HypervisorType.XenServer) {
            sc = xenAllFieldsSearch.create();
        } else if (hType == HypervisorType.KVM) {
            sc = kvmAllFieldsSearch.create();
        } else if (hType == HypervisorType.VMware) {
            sc = vmWareAllFieldsSearch.create();
        } else if (hType == HypervisorType.Simulator) {
        	sc = simulatorAllFieldsSearch.create();
        } else if (hType == HypervisorType.Ovm) {
        	sc = ovmAllFieldsSearch.create();
        } else if (hType == HypervisorType.BareMetal) {
        	return null;
        } else {
            assert(false) : "We don't handle this hypervisor type";
            return null;
        }

        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("trafficType", trafficType);
        List<String> tag = customSearch(sc, null);

        return tag.size() == 0 ? null : tag.get(0);
    }

    @Override
    public PhysicalNetworkTrafficTypeVO findBy(long physicalNetworkId, TrafficType trafficType){
        SearchCriteria<PhysicalNetworkTrafficTypeVO> sc = physicalNetworkSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("trafficType", trafficType);
        return findOneBy(sc);
    }

    @Override
    public void deleteTrafficTypes(long physicalNetworkId) {
        SearchCriteria<PhysicalNetworkTrafficTypeVO> sc = physicalNetworkSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        remove(sc);        
    }
}
