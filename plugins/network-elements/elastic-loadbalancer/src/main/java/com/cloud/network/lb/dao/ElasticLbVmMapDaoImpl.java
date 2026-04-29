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
package com.cloud.network.lb.dao;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.network.ElasticLbVmMapVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.dao.DomainRouterDao;

@Component
public class ElasticLbVmMapDaoImpl extends GenericDaoBase<ElasticLbVmMapVO, Long> implements ElasticLbVmMapDao {
    @Inject
    protected DomainRouterDao _routerDao;
    @Inject
    protected LoadBalancerDao _loadbalancerDao;

    protected SearchBuilder<ElasticLbVmMapVO> AllFieldsSearch;
    protected SearchBuilder<ElasticLbVmMapVO> UnusedVmSearch;
    protected SearchBuilder<ElasticLbVmMapVO> LoadBalancersForElbVmSearch;

    protected SearchBuilder<DomainRouterVO> ElbVmSearch;

    protected SearchBuilder<LoadBalancerVO> LoadBalancerSearch;

    public ElasticLbVmMapDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("ipId", AllFieldsSearch.entity().getIpAddressId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("lbId", AllFieldsSearch.entity().getLbId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("elbVmId", AllFieldsSearch.entity().getElbVmId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        ElbVmSearch = _routerDao.createSearchBuilder();
        ElbVmSearch.and("role", ElbVmSearch.entity().getRole(), SearchCriteria.Op.EQ);
        UnusedVmSearch = createSearchBuilder();
        UnusedVmSearch.and("elbVmId", UnusedVmSearch.entity().getElbVmId(), SearchCriteria.Op.NULL);
        ElbVmSearch.join("UnusedVmSearch", UnusedVmSearch, ElbVmSearch.entity().getId(), UnusedVmSearch.entity().getElbVmId(), JoinType.LEFTOUTER);
        ElbVmSearch.done();
        UnusedVmSearch.done();

        LoadBalancerSearch = _loadbalancerDao.createSearchBuilder();
        LoadBalancersForElbVmSearch = createSearchBuilder();
        LoadBalancersForElbVmSearch.and("elbVmId", LoadBalancersForElbVmSearch.entity().getElbVmId(), SearchCriteria.Op.EQ);
        LoadBalancerSearch.join("LoadBalancersForElbVm", LoadBalancersForElbVmSearch, LoadBalancerSearch.entity().getId(),
            LoadBalancersForElbVmSearch.entity().getLbId(), JoinType.INNER);
        LoadBalancersForElbVmSearch.done();
        LoadBalancerSearch.done();

    }

    @Override
    public ElasticLbVmMapVO findOneByLbIdAndElbVmId(long lbId, long elbVmId) {
        SearchCriteria<ElasticLbVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("lbId", lbId);
        sc.setParameters("elbVmId", elbVmId);
        return findOneBy(sc);
    }

    @Override
    public List<ElasticLbVmMapVO> listByLbId(long lbId) {
        SearchCriteria<ElasticLbVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("lbId", lbId);
        return listBy(sc);
    }

    @Override
    public List<ElasticLbVmMapVO> listByElbVmId(long elbVmId) {
        SearchCriteria<ElasticLbVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("elbVmId", elbVmId);
        return listBy(sc);
    }

    @Override
    public int deleteLB(long lbId) {
        SearchCriteria<ElasticLbVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("lbId", lbId);
        return super.expunge(sc);
    }

    @Override
    public ElasticLbVmMapVO findOneByIpIdAndElbVmId(long ipId, long elbVmId) {
        SearchCriteria<ElasticLbVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("ipId", ipId);
        sc.setParameters("elbVmId", elbVmId);
        return findOneBy(sc);
    }

    @Override
    public ElasticLbVmMapVO findOneByIp(long ipId) {
        SearchCriteria<ElasticLbVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("ipId", ipId);
        return findOneBy(sc);
    }

    @Override
    public List<DomainRouterVO> listUnusedElbVms() {
        SearchCriteria<DomainRouterVO> sc = ElbVmSearch.create();
        sc.setParameters("role", Role.LB);
        return _routerDao.search(sc, null);
    }

    @Override
    public List<LoadBalancerVO> listLbsForElbVm(long elbVmId) {
        SearchCriteria<LoadBalancerVO> sc = LoadBalancerSearch.create();
        sc.setJoinParameters("LoadBalancersForElbVm", "elbVmId", elbVmId);
        return _loadbalancerDao.search(sc, null);
    }

    @Override
    public int expungeByLbVmList(List<Long> vmIds, Long batchSize) {
        if (CollectionUtils.isEmpty(vmIds)) {
            return 0;
        }
        SearchBuilder<ElasticLbVmMapVO> sb = createSearchBuilder();
        sb.and("vmIds", sb.entity().getElbVmId(), SearchCriteria.Op.IN);
        SearchCriteria<ElasticLbVmMapVO> sc = sb.create();
        sc.setParameters("vmIds", vmIds.toArray());
        return batchExpunge(sc, batchSize);
    }

}
