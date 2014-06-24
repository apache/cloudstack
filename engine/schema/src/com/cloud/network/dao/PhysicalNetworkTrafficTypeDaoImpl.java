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

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value = PhysicalNetworkTrafficTypeDao.class)
@DB()
public class PhysicalNetworkTrafficTypeDaoImpl extends GenericDaoBase<PhysicalNetworkTrafficTypeVO, Long> implements PhysicalNetworkTrafficTypeDao {
    final SearchBuilder<PhysicalNetworkTrafficTypeVO> physicalNetworkSearch;
    final GenericSearchBuilder<PhysicalNetworkTrafficTypeVO, String> kvmAllFieldsSearch;
    final GenericSearchBuilder<PhysicalNetworkTrafficTypeVO, String> xenAllFieldsSearch;
    final GenericSearchBuilder<PhysicalNetworkTrafficTypeVO, String> vmWareAllFieldsSearch;
    final GenericSearchBuilder<PhysicalNetworkTrafficTypeVO, String> simulatorAllFieldsSearch;
    final GenericSearchBuilder<PhysicalNetworkTrafficTypeVO, String> ovmAllFieldsSearch;
    final GenericSearchBuilder<PhysicalNetworkTrafficTypeVO, String> hypervAllFieldsSearch;

    protected PhysicalNetworkTrafficTypeDaoImpl() {
        super();
        physicalNetworkSearch = createSearchBuilder();
        physicalNetworkSearch.and("physicalNetworkId", physicalNetworkSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkSearch.and("trafficType", physicalNetworkSearch.entity().getTrafficType(), Op.EQ);
        physicalNetworkSearch.done();

        kvmAllFieldsSearch = createSearchBuilder(String.class);
        kvmAllFieldsSearch.and("physicalNetworkId", kvmAllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        kvmAllFieldsSearch.and("trafficType", kvmAllFieldsSearch.entity().getTrafficType(), Op.EQ);
        kvmAllFieldsSearch.selectFields(kvmAllFieldsSearch.entity().getKvmNetworkLabel());
        kvmAllFieldsSearch.done();

        hypervAllFieldsSearch = createSearchBuilder(String.class);
        hypervAllFieldsSearch.and("physicalNetworkId", hypervAllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        hypervAllFieldsSearch.and("trafficType", hypervAllFieldsSearch.entity().getTrafficType(), Op.EQ);
        hypervAllFieldsSearch.selectFields(hypervAllFieldsSearch.entity().getHypervNetworkLabel());
        hypervAllFieldsSearch.done();

        xenAllFieldsSearch = createSearchBuilder(String.class);
        xenAllFieldsSearch.and("physicalNetworkId", xenAllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        xenAllFieldsSearch.and("trafficType", xenAllFieldsSearch.entity().getTrafficType(), Op.EQ);
        xenAllFieldsSearch.selectFields(xenAllFieldsSearch.entity().getXenNetworkLabel());
        xenAllFieldsSearch.done();

        vmWareAllFieldsSearch = createSearchBuilder(String.class);
        vmWareAllFieldsSearch.and("physicalNetworkId", vmWareAllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        vmWareAllFieldsSearch.and("trafficType", vmWareAllFieldsSearch.entity().getTrafficType(), Op.EQ);
        vmWareAllFieldsSearch.selectFields(vmWareAllFieldsSearch.entity().getVmwareNetworkLabel());
        vmWareAllFieldsSearch.done();

        simulatorAllFieldsSearch = createSearchBuilder(String.class);
        simulatorAllFieldsSearch.and("physicalNetworkId", simulatorAllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        simulatorAllFieldsSearch.and("trafficType", simulatorAllFieldsSearch.entity().getTrafficType(), Op.EQ);
        simulatorAllFieldsSearch.selectFields(simulatorAllFieldsSearch.entity().getSimulatorNetworkLabel());
        simulatorAllFieldsSearch.done();

        ovmAllFieldsSearch = createSearchBuilder(String.class);
        ovmAllFieldsSearch.and("physicalNetworkId", ovmAllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        ovmAllFieldsSearch.and("trafficType", ovmAllFieldsSearch.entity().getTrafficType(), Op.EQ);
        ovmAllFieldsSearch.selectFields(ovmAllFieldsSearch.entity().getSimulatorNetworkLabel());
        ovmAllFieldsSearch.done();
    }

    @Override
    public Pair<List<PhysicalNetworkTrafficTypeVO>, Integer> listAndCountBy(long physicalNetworkId) {
        SearchCriteria<PhysicalNetworkTrafficTypeVO> sc = physicalNetworkSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return searchAndCount(sc, null);
    }

    @Override
    public boolean isTrafficTypeSupported(long physicalNetworkId, TrafficType trafficType) {
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
        } else if (hType == HypervisorType.Hyperv) {
            sc = hypervAllFieldsSearch.create();
        } else {
            assert (false) : "We don't handle this hypervisor type";
            return null;
        }

        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("trafficType", trafficType);
        List<String> tag = customSearch(sc, null);

        return tag.size() == 0 ? null : tag.get(0);
    }

    @Override
    public PhysicalNetworkTrafficTypeVO findBy(long physicalNetworkId, TrafficType trafficType) {
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
