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

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.NicIpAlias;

@Component
@Local(value = NicIpAliasDao.class)
public class NicIpAliasDaoImpl extends GenericDaoBase<NicIpAliasVO, Long> implements NicIpAliasDao {
    private final SearchBuilder<NicIpAliasVO> AllFieldsSearch;
    private final GenericSearchBuilder<NicIpAliasVO, String> IpSearch;

    protected NicIpAliasDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("instanceId", AllFieldsSearch.entity().getVmId(), Op.EQ);
        AllFieldsSearch.and("network", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.and("address", AllFieldsSearch.entity().getIp4Address(), Op.EQ);
        AllFieldsSearch.and("nicId", AllFieldsSearch.entity().getNicId(), Op.EQ);
        AllFieldsSearch.and("gateway", AllFieldsSearch.entity().getGateway(), Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.done();

        IpSearch = createSearchBuilder(String.class);
        IpSearch.select(null, Func.DISTINCT, IpSearch.entity().getIp4Address());
        IpSearch.and("network", IpSearch.entity().getNetworkId(), Op.EQ);
        IpSearch.and("address", IpSearch.entity().getIp4Address(), Op.NNULL);
        IpSearch.done();
    }

    @Override
    public List<NicIpAliasVO> listByVmId(long instanceId) {
        SearchCriteria<NicIpAliasVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", instanceId);
        return listBy(sc);
    }

    @Override
    public List<NicIpAliasVO> listByNicId(long nicId) {
        SearchCriteria<NicIpAliasVO> sc = AllFieldsSearch.create();
        sc.setParameters("nicId", nicId);
        return listBy(sc);
    }

    @Override
    public List<String> listAliasIpAddressInNetwork(long networkId) {
        SearchCriteria<String> sc = IpSearch.create();
        sc.setParameters("network", networkId);
        return customSearch(sc, null);
    }

    @Override
    public List<NicIpAliasVO> listByNetworkId(long networkId) {
        SearchCriteria<NicIpAliasVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        return listBy(sc);
    }

    @Override
    public List<NicIpAliasVO> listByNetworkIdAndState(long networkId, NicIpAlias.state state) {
        SearchCriteria<NicIpAliasVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("state", state);
        return listBy(sc);
    }

    @Override
    public List<NicIpAliasVO> listByNicIdAndVmid(long nicId, long vmId) {
        SearchCriteria<NicIpAliasVO> sc = AllFieldsSearch.create();
        sc.setParameters("nicId", nicId);
        sc.setParameters("instanceId", vmId);
        return listBy(sc);
    }

    @Override
    public List<NicIpAliasVO> getAliasIpForVm(long vmId) {
        SearchCriteria<NicIpAliasVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", vmId);
        sc.setParameters("state", NicIpAlias.state.active);
        return listBy(sc);
    }

    @Override
    public List<String> getAliasIpAddressesForNic(long nicId) {
        SearchCriteria<NicIpAliasVO> sc = AllFieldsSearch.create();
        sc.setParameters("nicId", nicId);
        List<NicIpAliasVO> results = search(sc, null);
        List<String> ips = new ArrayList<String>(results.size());
        for (NicIpAliasVO result : results) {
            ips.add(result.getIp4Address());
        }
        return ips;
    }

    @Override
    public NicIpAliasVO findByInstanceIdAndNetworkId(long networkId, long instanceId) {
        SearchCriteria<NicIpAliasVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("instanceId", instanceId);
        sc.setParameters("state", NicIpAlias.state.active);
        return findOneBy(sc);
    }

    @Override
    public NicIpAliasVO findByIp4AddressAndNetworkId(String ip4Address, long networkId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NicIpAliasVO findByGatewayAndNetworkIdAndState(String gateway, long networkId, NicIpAlias.state state) {
        SearchCriteria<NicIpAliasVO> sc = AllFieldsSearch.create();
        sc.setParameters("gateway", gateway);
        sc.setParameters("network", networkId);
        sc.setParameters("state", state);
        return findOneBy(sc);
    }

    @Override
    public NicIpAliasVO findByIp4AddressAndVmId(String ip4Address, long vmId) {
        SearchCriteria<NicIpAliasVO> sc = AllFieldsSearch.create();
        sc.setParameters("address", ip4Address);
        sc.setParameters("instanceId", vmId);
        return findOneBy(sc);
    }

    @Override
    public NicIpAliasVO findByIp4AddressAndNicId(String ip4Address, long nicId) {
        SearchCriteria<NicIpAliasVO> sc = AllFieldsSearch.create();
        sc.setParameters("address", ip4Address);
        sc.setParameters("nicId", nicId);
        return findOneBy(sc);
    }

    @Override
    public NicIpAliasVO findByIp4AddressAndNetworkIdAndInstanceId(long networkId, Long vmId, String vmIp) {
        SearchCriteria<NicIpAliasVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("instanceId", vmId);
        sc.setParameters("address", vmIp);
        return findOneBy(sc);
    }

    @Override
    public Integer countAliasIps(long id) {
        SearchCriteria<NicIpAliasVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", id);
        List<NicIpAliasVO> list = listBy(sc);
        return list.size();
    }
}
