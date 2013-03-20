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

import javax.ejb.Local;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.StaticRouteVO;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.tags.dao.ResourceTagsDaoImpl;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;

@Component
@Local(value = StaticRouteDao.class)
@DB(txn = false)
public class StaticRouteDaoImpl extends GenericDaoBase<StaticRouteVO, Long> implements StaticRouteDao{
    protected final SearchBuilder<StaticRouteVO> AllFieldsSearch;
    protected final SearchBuilder<StaticRouteVO> NotRevokedSearch;
    protected final GenericSearchBuilder<StaticRouteVO, Long> RoutesByGatewayCount;
    @Inject ResourceTagDao _tagsDao;
    
    protected StaticRouteDaoImpl() {
        super();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("gatewayId", AllFieldsSearch.entity().getVpcGatewayId(), Op.EQ);
        AllFieldsSearch.and("vpcId", AllFieldsSearch.entity().getVpcId(), Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.done();
        
        NotRevokedSearch = createSearchBuilder();
        NotRevokedSearch.and("gatewayId", NotRevokedSearch.entity().getVpcGatewayId(), Op.EQ);
        NotRevokedSearch.and("state", NotRevokedSearch.entity().getState(), Op.NEQ);
        NotRevokedSearch.done();
        
        RoutesByGatewayCount = createSearchBuilder(Long.class);
        RoutesByGatewayCount.select(null, Func.COUNT, RoutesByGatewayCount.entity().getId());
        RoutesByGatewayCount.and("gatewayId", RoutesByGatewayCount.entity().getVpcGatewayId(), Op.EQ);
        RoutesByGatewayCount.done();
    }

    
    @Override
    public boolean setStateToAdd(StaticRouteVO rule) {
        SearchCriteria<StaticRouteVO> sc = AllFieldsSearch.create();
        sc.setParameters("id", rule.getId());
        sc.setParameters("state", StaticRoute.State.Staged);

        rule.setState(StaticRoute.State.Add);

        return update(rule, sc) > 0;
    }


    @Override
    public List<? extends StaticRoute> listByGatewayIdAndNotRevoked(long gatewayId) {
        SearchCriteria<StaticRouteVO> sc = NotRevokedSearch.create();
        sc.setParameters("gatewayId", gatewayId);
        sc.setParameters("state", StaticRoute.State.Revoke);
        return listBy(sc);
    }

    @Override
    public List<StaticRouteVO> listByVpcId(long vpcId) {
        SearchCriteria<StaticRouteVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpcId", vpcId);
        return listBy(sc);
    }

    @Override
    public long countRoutesByGateway(long gatewayId) {
        SearchCriteria<Long> sc = RoutesByGatewayCount.create();
        sc.setParameters("gatewayId", gatewayId);
        return customSearch(sc, null).get(0);
    }
    
    @Override
    @DB
    public boolean remove(Long id) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        StaticRouteVO entry = findById(id);
        if (entry != null) {
            _tagsDao.removeByIdAndType(id, TaggedResourceType.StaticRoute);
        }
        boolean result = super.remove(id);
        txn.commit();
        return result;
    }
}
