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

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
public class NicSecondaryIpDaoImpl extends GenericDaoBase<NicSecondaryIpVO, Long> implements NicSecondaryIpDao {

    private final SearchBuilder<NicSecondaryIpVO> AllFieldsSearch;
    private final GenericSearchBuilder<NicSecondaryIpVO, String> IpSearch;
    protected GenericSearchBuilder<NicSecondaryIpVO, Long> CountByNicId;

    public NicSecondaryIpDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("instanceId", AllFieldsSearch.entity().getVmId(), Op.EQ);
        AllFieldsSearch.and("network", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.and("address", AllFieldsSearch.entity().getIp4Address(), Op.LIKE);
        AllFieldsSearch.and("ip6address", AllFieldsSearch.entity().getIp6Address(), Op.LIKE);
        AllFieldsSearch.and("nicId", AllFieldsSearch.entity().getNicId(), Op.EQ);
        AllFieldsSearch.done();

        IpSearch = createSearchBuilder(String.class);
        IpSearch.select(null, Func.DISTINCT, IpSearch.entity().getIp4Address());
        IpSearch.and("network", IpSearch.entity().getNetworkId(), Op.EQ);
        IpSearch.and("address", IpSearch.entity().getIp4Address(), Op.NNULL);
        IpSearch.done();

        CountByNicId = createSearchBuilder(Long.class);
        CountByNicId.select(null, Func.COUNT, null);
        CountByNicId.and("nic", CountByNicId.entity().getNicId(), SearchCriteria.Op.EQ);
        CountByNicId.done();
    }

    @Override
    public List<NicSecondaryIpVO> listByVmId(long instanceId) {
        SearchCriteria<NicSecondaryIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", instanceId);
        return listBy(sc);
    }

    @Override
    public List<NicSecondaryIpVO> listByNicId(long nicId) {
        SearchCriteria<NicSecondaryIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("nicId", nicId);
        return listBy(sc);
    }

    @Override
    public List<String> listSecondaryIpAddressInNetwork(long networkId) {
        SearchCriteria<String> sc = IpSearch.create();
        sc.setParameters("network", networkId);
        return customSearch(sc, null);
    }

    @Override
    public List<NicSecondaryIpVO> listByNetworkId(long networkId) {
        SearchCriteria<NicSecondaryIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        return listBy(sc);
    }

    @Override
    public List<NicSecondaryIpVO> listByNicIdAndVmid(long nicId, long vmId) {
        SearchCriteria<NicSecondaryIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("nicId", nicId);
        sc.setParameters("instanceId", vmId);
        return listBy(sc);
    }

    @Override
    public List<NicSecondaryIpVO> getSecondaryIpAddressesForVm(long vmId) {
        SearchCriteria<NicSecondaryIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", vmId);
        return listBy(sc);
    }

    @Override
    public List<String> getSecondaryIpAddressesForNic(long nicId) {
        SearchCriteria<NicSecondaryIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("nicId", nicId);
        List<NicSecondaryIpVO> results = search(sc, null);
        List<String> ips = new ArrayList<String>(results.size());
        for (NicSecondaryIpVO result : results) {
            if (StringUtils.isNotBlank(result.getIp4Address())) {
                ips.add(result.getIp4Address());
            }

            if (StringUtils.isNotBlank(result.getIp6Address())) {
                ips.add(result.getIp6Address());
            }
        }
        return ips;
    }

    @Override
    public NicSecondaryIpVO findByInstanceIdAndNetworkId(long networkId, long instanceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NicSecondaryIpVO findByIp4AddressAndNetworkId(String ip4Address, long networkId) {
        SearchCriteria<NicSecondaryIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("address", ip4Address);
        return findOneBy(sc);
    }

    @Override
    public NicSecondaryIpVO findByIp6AddressAndNetworkId(String ip6Address, long networkId) {
        SearchCriteria<NicSecondaryIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("ip6address", ip6Address);
        return findOneBy(sc);
    }

    @Override
    public NicSecondaryIpVO findByIp4AddressAndNicId(String ip4Address, long nicId) {
        SearchCriteria<NicSecondaryIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("address", ip4Address);
        sc.setParameters("nicId", nicId);
        return findOneBy(sc);
    }

    @Override
    public NicSecondaryIpVO findByIp4AddressAndInstanceId(Long vmId, String vmIp) {
        SearchCriteria<NicSecondaryIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", vmId);
        sc.setParameters("address", vmIp);
        return findOneBy(sc);
    }

    @Override
    public NicSecondaryIpVO findByIp4AddressAndNetworkIdAndInstanceId(long networkId, Long vmId, String vmIp) {
        SearchCriteria<NicSecondaryIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("instanceId", vmId);
        sc.setParameters("address", vmIp);
        return findOneBy(sc);
    }

    @Override
    public Long countByNicId(long nicId) {
        SearchCriteria<Long> sc = CountByNicId.create();
        sc.setParameters("nic", nicId);
        return customSearch(sc, null).get(0);
    }

    @Override
    public List<NicSecondaryIpVO> listSecondaryIpUsingKeyword(long nicId, String keyword)
    {
        SearchCriteria<NicSecondaryIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("nicId", nicId);
        sc.setParameters("address", "%" + keyword + "%");
        return listBy(sc);
    }

    @Override
    public int moveSecondaryIps(long fromNicId, long toNicId) {
        NicSecondaryIpVO update = createForUpdate();
        update.setNicId(toNicId);

        SearchCriteria<NicSecondaryIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("nicId", fromNicId);

        return update(update, sc);
    }
}
