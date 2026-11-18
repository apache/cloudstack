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
package com.cloud.network.vpc.dao;

import java.util.Date;
import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.network.vpc.PrivateIpVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;

@Component
@DB()
public class PrivateIpDaoImpl extends GenericDaoBase<PrivateIpVO, Long> implements PrivateIpDao {

    private final SearchBuilder<PrivateIpVO> AllFieldsSearch;
    private final GenericSearchBuilder<PrivateIpVO, Integer> CountAllocatedByNetworkId;
    private final GenericSearchBuilder<PrivateIpVO, Integer> CountByNetworkId;

    protected PrivateIpDaoImpl() {
        super();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("ip", AllFieldsSearch.entity().getIpAddress(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("networkId", AllFieldsSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("ipAddress", AllFieldsSearch.entity().getIpAddress(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("taken", AllFieldsSearch.entity().getTakenAt(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("vpcId", AllFieldsSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        CountAllocatedByNetworkId = createSearchBuilder(Integer.class);
        CountAllocatedByNetworkId.select(null, Func.COUNT, CountAllocatedByNetworkId.entity().getId());
        CountAllocatedByNetworkId.and("networkId", CountAllocatedByNetworkId.entity().getNetworkId(), Op.EQ);
        CountAllocatedByNetworkId.and("taken", CountAllocatedByNetworkId.entity().getTakenAt(), Op.NNULL);
        CountAllocatedByNetworkId.done();

        CountByNetworkId = createSearchBuilder(Integer.class);
        CountByNetworkId.select(null, Func.COUNT, CountByNetworkId.entity().getId());
        CountByNetworkId.and("networkId", CountByNetworkId.entity().getNetworkId(), Op.EQ);
        CountByNetworkId.done();
    }

    @Override
    public PrivateIpVO allocateIpAddress(long dcId, long networkId, String requestedIp) {
        SearchCriteria<PrivateIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("taken", (Date)null);

        if (requestedIp != null) {
            sc.setParameters("ipAddress", requestedIp);
        }

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        PrivateIpVO vo = lockOneRandomRow(sc, true);
        if (vo == null) {
            txn.rollback();
            return null;
        }
        vo.setTakenAt(new Date());
        update(vo.getId(), vo);
        txn.commit();
        return vo;
    }

    @Override
    public void releaseIpAddress(String ipAddress, long networkId) {
        if (logger.isDebugEnabled()) {
            logger.debug("Releasing private ip address: " + ipAddress + " network id " + networkId);
        }
        SearchCriteria<PrivateIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("ip", ipAddress);
        sc.setParameters("networkId", networkId);

        PrivateIpVO vo = createForUpdate();

        vo.setTakenAt(null);
        update(vo, sc);
    }

    @Override
    public PrivateIpVO findByIpAndSourceNetworkId(long networkId, String ip4Address) {
        SearchCriteria<PrivateIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("ip", ip4Address);
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }

    @Override
    public PrivateIpVO findByIpAndSourceNetworkIdAndVpcId(long networkId, String ip4Address, long vpcId) {
        SearchCriteria<PrivateIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("ip", ip4Address);
        sc.setParameters("networkId", networkId);
        sc.setParameters("vpcId", vpcId);
        return findOneBy(sc);
    }

    @Override
    public PrivateIpVO findByIpAndVpcId(long vpcId, String ip4Address) {
        SearchCriteria<PrivateIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("ip", ip4Address);
        sc.setParameters("vpcId", vpcId);
        return findOneBy(sc);
    }

    @Override
    public List<PrivateIpVO> listByNetworkId(long networkId) {
        SearchCriteria<PrivateIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        return listBy(sc);
    }

    @Override
    public int countAllocatedByNetworkId(long ntwkId) {
        SearchCriteria<Integer> sc = CountAllocatedByNetworkId.create();
        sc.setParameters("networkId", ntwkId);
        List<Integer> results = customSearch(sc, null);
        return results.get(0);
    }

    @Override
    public void deleteByNetworkId(long networkId) {
        SearchCriteria<PrivateIpVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        remove(sc);
    }

    @Override
    public int countByNetworkId(long ntwkId) {
        SearchCriteria<Integer> sc = CountByNetworkId.create();
        sc.setParameters("networkId", ntwkId);
        List<Integer> results = customSearch(sc, null);
        return results.get(0);
    }
}
