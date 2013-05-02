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
package com.cloud.vm.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.Nic;
import com.cloud.vm.Nic.State;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;

@Component
@Local(value=NicDao.class)
public class NicDaoImpl extends GenericDaoBase<NicVO, Long> implements NicDao {
    private final SearchBuilder<NicVO> AllFieldsSearch;
    private final GenericSearchBuilder<NicVO, String> IpSearch;
    private final SearchBuilder<NicVO> NonReleasedSearch;
    final GenericSearchBuilder<NicVO, Integer> CountBy;

    
    public NicDaoImpl() {
        super();
        
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("instance", AllFieldsSearch.entity().getInstanceId(), Op.EQ);
        AllFieldsSearch.and("network", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.and("gateway", AllFieldsSearch.entity().getGateway(), Op.EQ);
        AllFieldsSearch.and("vmType", AllFieldsSearch.entity().getVmType(), Op.EQ);
        AllFieldsSearch.and("address", AllFieldsSearch.entity().getIp4Address(), Op.EQ);
        AllFieldsSearch.and("isDefault", AllFieldsSearch.entity().isDefaultNic(), Op.EQ);
        AllFieldsSearch.and("broadcastUri", AllFieldsSearch.entity().getBroadcastUri(), Op.EQ);
        AllFieldsSearch.and("secondaryip", AllFieldsSearch.entity().getSecondaryIp(), Op.EQ);
        AllFieldsSearch.and("nicid", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.done();

        IpSearch = createSearchBuilder(String.class);
        IpSearch.select(null, Func.DISTINCT, IpSearch.entity().getIp4Address());
        IpSearch.and("network", IpSearch.entity().getNetworkId(), Op.EQ);
        IpSearch.and("address", IpSearch.entity().getIp4Address(), Op.NNULL);
        IpSearch.done();
        
        NonReleasedSearch = createSearchBuilder();
        NonReleasedSearch.and("instance", NonReleasedSearch.entity().getInstanceId(), Op.EQ);
        NonReleasedSearch.and("network", NonReleasedSearch.entity().getNetworkId(), Op.EQ);
        NonReleasedSearch.and("state", NonReleasedSearch.entity().getState(), Op.NOTIN);
        NonReleasedSearch.done();
        
        CountBy = createSearchBuilder(Integer.class);
        CountBy.select(null, Func.COUNT, CountBy.entity().getId());
        CountBy.and("vmId", CountBy.entity().getInstanceId(), Op.EQ);
        CountBy.and("removed", CountBy.entity().getRemoved(), Op.NULL);
        CountBy.done();
    }
    
    @Override
    public void removeNicsForInstance(long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        remove(sc);
    }
    
    @Override
    public List<NicVO> listByVmId(long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        return listBy(sc);
    }
    
    @Override
    public List<NicVO> listByVmIdIncludingRemoved(long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        return listIncludingRemovedBy(sc);
    }
    
    
    @Override
    public List<String> listIpAddressInNetwork(long networkId) {
        SearchCriteria<String> sc = IpSearch.create();
        sc.setParameters("network", networkId);
        return customSearch(sc, null);
    }
    
    @Override
    public List<NicVO> listByNetworkId(long networkId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        return listBy(sc);
    }
    
    @Override
    public NicVO findByInstanceIdAndNetworkId(long networkId, long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("instance", instanceId);
        return findOneBy(sc);
    }
    
    @Override
    public NicVO findByInstanceIdAndNetworkIdIncludingRemoved(long networkId, long instanceId) {
        SearchCriteria<NicVO> sc = createSearchCriteria();
        sc.addAnd("networkId", SearchCriteria.Op.EQ, networkId);
        sc.addAnd("instanceId", SearchCriteria.Op.EQ, instanceId);
        return findOneIncludingRemovedBy(sc);
    }
    
    @Override
    public NicVO findByNetworkIdAndType(long networkId, VirtualMachine.Type vmType) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("vmType", vmType);
        return findOneBy(sc);
    }
    
    @Override
    public NicVO findByNetworkIdTypeAndGateway(long networkId, VirtualMachine.Type vmType, String gateway) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("vmType", vmType);
        sc.setParameters("gateway", gateway);
        return findOneBy(sc);
    }
    
    @Override
    public NicVO findByIp4AddressAndNetworkId(String ip4Address, long networkId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("address", ip4Address);
        sc.setParameters("network", networkId);
        return findOneBy(sc);
    }
    
    @Override
    public NicVO findDefaultNicForVM(long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        sc.setParameters("isDefault", 1);
        return findOneBy(sc);
    }
    
    @Override
    public NicVO findNonReleasedByInstanceIdAndNetworkId(long networkId, long instanceId) {
        SearchCriteria<NicVO> sc = NonReleasedSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("instance", instanceId);
        sc.setParameters("state", State.Releasing, Nic.State.Deallocating);
        return findOneBy(sc);
    }
    
    @Override
    public String getIpAddress(long networkId, long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("instance", instanceId);
        return findOneBy(sc).getIp4Address();
    }

    @Override
    public int countNics(long instanceId) {
        SearchCriteria<Integer> sc = CountBy.create();
        sc.setParameters("vmId", instanceId);
        List<Integer> results = customSearch(sc, null);
        return results.get(0);
    }


    @Override
    public NicVO findByNetworkIdInstanceIdAndBroadcastUri(long networkId, long instanceId, String broadcastUri) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("instance", instanceId);
        sc.setParameters("broadcastUri", broadcastUri);
        return findOneBy(sc);
    }
    
    @Override
    public NicVO findByIp4AddressAndNetworkIdAndInstanceId(long networkId, long instanceId, String ip4Address) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("instance", instanceId);
        sc.setParameters("address", ip4Address);
        return findOneBy(sc);
    }

    @Override
    public List<NicVO> listByVmIdAndNicId(Long vmId, Long nicId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", vmId);
        sc.setParameters("nicid", nicId);
        return listBy(sc);
    }

    @Override
    public NicVO findByIp4AddressAndVmId(String ip4Address, long instance) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("address", ip4Address);
        sc.setParameters("instance", instance);
        return findOneBy(sc);
    }

    @Override
    public List<NicVO> listPlaceholderNicsByNetworkId(long networkId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("strategy", Nic.ReservationStrategy.PlaceHolder.toString());
        return listBy(sc);
    }

    @Override
    public List<NicVO> listPlaceholderNicsByNetworkIdAndVmType(long networkId, VirtualMachine.Type vmType) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("strategy", Nic.ReservationStrategy.PlaceHolder.toString());
        sc.setParameters("vmType", vmType);
        return listBy(sc);
    }

}
