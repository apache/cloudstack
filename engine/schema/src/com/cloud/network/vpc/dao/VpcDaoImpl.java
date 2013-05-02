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

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;

import com.cloud.network.Network;
import com.cloud.network.vpc.VpcServiceMapVO;
import org.springframework.stereotype.Component;

import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcVO;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.dao.ResourceTagDao;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;

@Component
@Local(value = VpcDao.class)
@DB(txn = false)
public class VpcDaoImpl extends GenericDaoBase<VpcVO, Long> implements VpcDao{
    final GenericSearchBuilder<VpcVO, Integer> CountByOfferingId;
    final SearchBuilder<VpcVO> AllFieldsSearch;
    final GenericSearchBuilder<VpcVO, Long> CountByAccountId;

    @Inject ResourceTagDao _tagsDao;
    @Inject VpcServiceMapDao _vpcSvcMap;

    protected VpcDaoImpl() {
        super();
        
        CountByOfferingId = createSearchBuilder(Integer.class);
        CountByOfferingId.select(null, Func.COUNT, CountByOfferingId.entity().getId());
        CountByOfferingId.and("offeringId", CountByOfferingId.entity().getVpcOfferingId(), Op.EQ);
        CountByOfferingId.and("removed", CountByOfferingId.entity().getRemoved(), Op.NULL);
        CountByOfferingId.done();
        
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), Op.EQ);
        AllFieldsSearch.done();
        
        CountByAccountId = createSearchBuilder(Long.class);
        CountByAccountId.select(null, Func.COUNT, CountByAccountId.entity().getId());
        CountByAccountId.and("offeringId", CountByAccountId.entity().getAccountId(), Op.EQ);
        CountByAccountId.and("removed", CountByAccountId.entity().getRemoved(), Op.NULL);
        CountByAccountId.done();
    }
    
    
    @Override
    public int getVpcCountByOfferingId(long offId) {
        SearchCriteria<Integer> sc = CountByOfferingId.create();
        sc.setParameters("offeringId", offId);
        List<Integer> results = customSearch(sc, null);
        return results.get(0);
    }
    
    @Override
    public Vpc getActiveVpcById(long vpcId) {
        SearchCriteria<VpcVO> sc = AllFieldsSearch.create();
        sc.setParameters("id", vpcId);
        sc.setParameters("state", Vpc.State.Enabled);
        return findOneBy(sc);
    }
    
    @Override
    public List<? extends Vpc> listByAccountId(long accountId) {
        SearchCriteria<VpcVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc, null);
    }
    
    @Override
    public List<VpcVO> listInactiveVpcs() {
        SearchCriteria<VpcVO> sc = AllFieldsSearch.create();
        sc.setParameters("state", Vpc.State.Inactive);
        return listBy(sc, null);
    }
    
    @Override
    @DB
    public boolean remove(Long id) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        VpcVO entry = findById(id);
        if (entry != null) {
            _tagsDao.removeByIdAndType(id, TaggedResourceType.Vpc);
        }
        boolean result = super.remove(id);
        txn.commit();
        return result;
    }
    
    @Override
    public long countByAccountId(long accountId) {
        SearchCriteria<Long> sc = CountByAccountId.create();
        sc.setParameters("accountId", accountId);
        List<Long> results = customSearch(sc, null);
        return results.get(0);
    }

    @Override
    @DB
    public VpcVO persist(VpcVO vpc, Map<String, String> serviceProviderMap) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        VpcVO newVpc = super.persist(vpc);
        persistVpcServiceProviders(vpc.getId(), serviceProviderMap);
        txn.commit();
        return newVpc;
    }

    @Override
    @DB
    public void persistVpcServiceProviders(long vpcId, Map<String, String> serviceProviderMap) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (String service : serviceProviderMap.keySet()) {
            VpcServiceMapVO serviceMap = new VpcServiceMapVO(vpcId, Network.Service.getService(service), Network.Provider.getProvider(serviceProviderMap.get(service)));
            _vpcSvcMap.persist(serviceMap);
        }
        txn.commit();
    }
}

