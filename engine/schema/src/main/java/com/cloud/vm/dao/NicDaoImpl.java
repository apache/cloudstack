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

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.Nic;
import com.cloud.vm.Nic.State;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Component
public class NicDaoImpl extends GenericDaoBase<NicVO, Long> implements NicDao {
    private SearchBuilder<NicVO> AllFieldsSearch;
    private GenericSearchBuilder<NicVO, String> IpSearch;
    private SearchBuilder<NicVO> NonReleasedSearch;
    private GenericSearchBuilder<NicVO, Integer> deviceIdSearch;
    private GenericSearchBuilder<NicVO, Integer> CountByForStartingVms;
    private SearchBuilder<NicVO> PeerRouterSearch;

    @Inject
    VMInstanceDao _vmDao;

    public NicDaoImpl() {

    }

    @PostConstruct
    protected void init() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("instance", AllFieldsSearch.entity().getInstanceId(), Op.EQ);
        AllFieldsSearch.and("network", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.and("gateway", AllFieldsSearch.entity().getIPv4Gateway(), Op.EQ);
        AllFieldsSearch.and("vmType", AllFieldsSearch.entity().getVmType(), Op.EQ);
        AllFieldsSearch.and("address", AllFieldsSearch.entity().getIPv4Address(), Op.LIKE);
        AllFieldsSearch.and("isDefault", AllFieldsSearch.entity().isDefaultNic(), Op.EQ);
        AllFieldsSearch.and("broadcastUri", AllFieldsSearch.entity().getBroadcastUri(), Op.EQ);
        AllFieldsSearch.and("secondaryip", AllFieldsSearch.entity().getSecondaryIp(), Op.EQ);
        AllFieldsSearch.and("nicid", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("strategy", AllFieldsSearch.entity().getReservationStrategy(), Op.EQ);
        AllFieldsSearch.and("reserverName",AllFieldsSearch.entity().getReserver(),Op.EQ);
        AllFieldsSearch.and("macAddress", AllFieldsSearch.entity().getMacAddress(), Op.EQ);
        AllFieldsSearch.done();

        IpSearch = createSearchBuilder(String.class);
        IpSearch.select(null, Func.DISTINCT, IpSearch.entity().getIPv4Address());
        IpSearch.and("network", IpSearch.entity().getNetworkId(), Op.EQ);
        IpSearch.and("address", IpSearch.entity().getIPv4Address(), Op.NNULL);
        IpSearch.done();

        NonReleasedSearch = createSearchBuilder();
        NonReleasedSearch.and("instance", NonReleasedSearch.entity().getInstanceId(), Op.EQ);
        NonReleasedSearch.and("network", NonReleasedSearch.entity().getNetworkId(), Op.EQ);
        NonReleasedSearch.and("state", NonReleasedSearch.entity().getState(), Op.NOTIN);
        NonReleasedSearch.done();

        deviceIdSearch = createSearchBuilder(Integer.class);
        deviceIdSearch.select(null, Func.DISTINCT, deviceIdSearch.entity().getDeviceId());
        deviceIdSearch.and("instance", deviceIdSearch.entity().getInstanceId(), Op.EQ);
        deviceIdSearch.done();

        CountByForStartingVms = createSearchBuilder(Integer.class);
        CountByForStartingVms.select(null, Func.COUNT, CountByForStartingVms.entity().getId());
        CountByForStartingVms.and("networkId", CountByForStartingVms.entity().getNetworkId(), Op.EQ);
        CountByForStartingVms.and("removed", CountByForStartingVms.entity().getRemoved(), Op.NULL);
        SearchBuilder<VMInstanceVO> join1 = _vmDao.createSearchBuilder();
        join1.and("state", join1.entity().getState(), Op.EQ);
        CountByForStartingVms.join("vm", join1, CountByForStartingVms.entity().getInstanceId(), join1.entity().getId(), JoinBuilder.JoinType.INNER);
        CountByForStartingVms.done();

        PeerRouterSearch = createSearchBuilder();
        PeerRouterSearch.and("instanceId", PeerRouterSearch.entity().getInstanceId(), Op.NEQ);
        PeerRouterSearch.and("macAddress", PeerRouterSearch.entity().getMacAddress(), Op.EQ);
        PeerRouterSearch.and("vmType", PeerRouterSearch.entity().getVmType(), Op.EQ);
        PeerRouterSearch.done();
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
    public List<NicVO> listByVmIdOrderByDeviceId(long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        return customSearch(sc, new Filter(NicVO.class, "deviceId", true, null, null));
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
    public NicVO findByNtwkIdAndInstanceId(long networkId, long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("instance", instanceId);
        return findOneBy(sc);
    }

    @Override
    public NicVO findByInstanceIdAndIpAddressAndVmtype(long instanceId, String ipaddress, VirtualMachine.Type type) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        sc.setParameters("address", ipaddress);
        sc.setParameters("vmType", type);
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
    public List<NicVO> listByNetworkIdTypeAndGatewayAndBroadcastUri(long networkId, VirtualMachine.Type vmType, String gateway, URI broadcasturi) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("vmType", vmType);
        sc.setParameters("gateway", gateway);
        sc.setParameters("broadcastUri", broadcasturi);
        return listBy(sc);
    }

    @Override
    public NicVO findByIp4AddressAndNetworkId(String ip4Address, long networkId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("address", ip4Address);
        sc.setParameters("network", networkId);
        return findOneBy(sc);
    }

    @Override
    public NicVO findByNetworkIdAndMacAddress(long networkId, String mac) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("macAddress", mac);
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
    public NicVO getControlNicForVM(long vmId){
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", vmId);
        sc.setParameters("reserverName", "ControlNetworkGuru");
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
        NicVO nicVo = findOneBy(sc);
        if (nicVo != null) {
            return nicVo.getIPv4Address();
        }
        return null;
    }

    @Override
    public int getFreeDeviceId(long instanceId) {
        Filter searchFilter = new Filter(NicVO.class, "deviceId", true, null, null);
        SearchCriteria<Integer> sc = deviceIdSearch.create();
        sc.setParameters("instance", instanceId);
        List<Integer> deviceIds = customSearch(sc, searchFilter);

        int freeDeviceId = 0;
        for (int deviceId : deviceIds) {
            if (deviceId > freeDeviceId)
                break;
            freeDeviceId ++;
        }

        return freeDeviceId;
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
    public List<NicVO> listByVmIdAndNicIdAndNtwkId(long vmId, Long nicId, Long networkId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", vmId);

        if (nicId != null) {
            sc.setParameters("nicid", nicId);
        }

        if (networkId != null) {
            sc.setParameters("network", networkId);
        }
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

    @Override
    public int countNicsForStartingVms(long networkId) {
        SearchCriteria<Integer> sc = CountByForStartingVms.create();
        sc.setParameters("networkId", networkId);
        sc.setJoinParameters("vm", "state", VirtualMachine.State.Starting);
        List<Integer> results = customSearch(sc, null);
        return results.get(0);
    }

    @Override
    public Long getPeerRouterId(String publicMacAddress, final long routerId) {
        final SearchCriteria<NicVO> sc = PeerRouterSearch.create();
        sc.setParameters("instanceId", routerId);
        sc.setParameters("macAddress", publicMacAddress);
        sc.setParameters("vmType", VirtualMachine.Type.DomainRouter);
        NicVO nicVo = findOneBy(sc);
        if (nicVo != null) {
            return nicVo.getInstanceId();
        }
        return null;
    }

    @Override
    public List<NicVO> listByVmIdAndKeyword(long instanceId, String keyword) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        sc.setParameters("address", "%" + keyword + "%");
        return listBy(sc);
    }
}
