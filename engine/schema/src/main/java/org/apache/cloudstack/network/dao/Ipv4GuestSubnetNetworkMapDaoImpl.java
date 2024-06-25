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

package org.apache.cloudstack.network.dao;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMap;
import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMapVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@DB
public class Ipv4GuestSubnetNetworkMapDaoImpl extends GenericDaoBase<Ipv4GuestSubnetNetworkMapVO, Long> implements Ipv4GuestSubnetNetworkMapDao {

    protected SearchBuilder<Ipv4GuestSubnetNetworkMapVO> ParentStateSearch;
    protected SearchBuilder<Ipv4GuestSubnetNetworkMapVO> ParentIdSearch;
    protected SearchBuilder<Ipv4GuestSubnetNetworkMapVO> NoParentSearch;
    protected SearchBuilder<Ipv4GuestSubnetNetworkMapVO> NetworkIdSearch;
    protected SearchBuilder<Ipv4GuestSubnetNetworkMapVO> SubnetSearch;
    protected SearchBuilder<Ipv4GuestSubnetNetworkMapVO> StatesSearch;
    protected SearchBuilder<Ipv4GuestSubnetNetworkMapVO> DomainAccountNeqSearch;

    @Inject
    NetworkDao networkDao;

    @PostConstruct
    public void init() {
        ParentStateSearch = createSearchBuilder();
        ParentStateSearch.and("parentId", ParentStateSearch.entity().getParentId(), SearchCriteria.Op.EQ);
        ParentStateSearch.and("state", ParentStateSearch.entity().getState(), SearchCriteria.Op.IN);
        ParentStateSearch.and("subnet", ParentStateSearch.entity().getSubnet(), SearchCriteria.Op.LIKE);
        ParentStateSearch.done();
        ParentIdSearch = createSearchBuilder();
        ParentIdSearch.and("parentId", ParentIdSearch.entity().getParentId(), SearchCriteria.Op.EQ);
        ParentIdSearch.done();
        NoParentSearch = createSearchBuilder();
        NoParentSearch.and("parentId", NoParentSearch.entity().getParentId(), SearchCriteria.Op.NULL);
        NoParentSearch.done();
        NetworkIdSearch = createSearchBuilder();
        NetworkIdSearch.and("networkId", NetworkIdSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        NetworkIdSearch.and("vpcId", NetworkIdSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        NetworkIdSearch.done();
        SubnetSearch = createSearchBuilder();
        SubnetSearch.and("subnet", SubnetSearch.entity().getSubnet(), SearchCriteria.Op.EQ);
        SubnetSearch.done();
        StatesSearch = createSearchBuilder();
        StatesSearch.and("state", StatesSearch.entity().getState(), SearchCriteria.Op.IN);
        StatesSearch.done();

        final SearchBuilder<NetworkVO> networkSearchBuilder = networkDao.createSearchBuilder();
        networkSearchBuilder.and("domainId", networkSearchBuilder.entity().getDomainId(), SearchCriteria.Op.NEQ);
        networkSearchBuilder.and("accountId", networkSearchBuilder.entity().getAccountId(), SearchCriteria.Op.NEQ);
        DomainAccountNeqSearch = createSearchBuilder();
        DomainAccountNeqSearch.and("parentId", DomainAccountNeqSearch.entity().getParentId(), SearchCriteria.Op.EQ);
        DomainAccountNeqSearch.join("network", networkSearchBuilder, networkSearchBuilder.entity().getId(),
                DomainAccountNeqSearch.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        DomainAccountNeqSearch.done();
    }

    @Override
    public List<Ipv4GuestSubnetNetworkMapVO> listByParent(long parentId) {
        SearchCriteria<Ipv4GuestSubnetNetworkMapVO> sc = ParentIdSearch.create();
        sc.setParameters("parentId", parentId);
        return listBy(sc, null);
    }

    @Override
    public List<Ipv4GuestSubnetNetworkMapVO> listUsedByParent(long parentId) {
        SearchCriteria<Ipv4GuestSubnetNetworkMapVO> sc = ParentStateSearch.create();
        sc.setParameters("parentId", parentId);
        sc.setParameters("state", (Object[]) new Ipv4GuestSubnetNetworkMap.State[]{Ipv4GuestSubnetNetworkMap.State.Allocated, Ipv4GuestSubnetNetworkMap.State.Allocating});
        return listBy(sc, null);
    }

    @Override
    public List<Ipv4GuestSubnetNetworkMapVO> listUsedByOtherDomains(long parentId, Long domainId) {
        SearchCriteria<Ipv4GuestSubnetNetworkMapVO> sc = DomainAccountNeqSearch.create();
        sc.setParameters("parentId", parentId);
        sc.setJoinParameters("network", "domainId", domainId);
        return listBy(sc);
    }

    @Override
    public List<Ipv4GuestSubnetNetworkMapVO> listUsedByOtherAccounts(long parentId, Long accountId) {
        SearchCriteria<Ipv4GuestSubnetNetworkMapVO> sc = DomainAccountNeqSearch.create();
        sc.setParameters("parentId", parentId);
        sc.setJoinParameters("network", "accountId", accountId);
        return listBy(sc);
    }

    @Override
    public Ipv4GuestSubnetNetworkMapVO findFirstAvailable(long parentId, long cidrSize) {
        SearchCriteria<Ipv4GuestSubnetNetworkMapVO> sc = ParentStateSearch.create();
        sc.setParameters("parentId", parentId);
        sc.setParameters("subnet", "%/" + cidrSize);
        sc.setParameters("state", (Object[]) new Ipv4GuestSubnetNetworkMap.State[]{Ipv4GuestSubnetNetworkMap.State.Free});
        Filter searchFilter = new Filter(Ipv4GuestSubnetNetworkMapVO.class, "id", true, null, 1L);
        List<Ipv4GuestSubnetNetworkMapVO> list = listBy(sc, searchFilter);
        return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
    }

    @Override
    public Ipv4GuestSubnetNetworkMapVO findByNetworkId(long networkId) {
        SearchCriteria<Ipv4GuestSubnetNetworkMapVO> sc = NetworkIdSearch.create();
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }

    @Override
    public Ipv4GuestSubnetNetworkMapVO findByVpcId(long vpcId) {
        SearchCriteria<Ipv4GuestSubnetNetworkMapVO> sc = NetworkIdSearch.create();
        sc.setParameters("vpcId", vpcId);
        return findOneBy(sc);
    }

    @Override
    public Ipv4GuestSubnetNetworkMapVO findBySubnet(String subnet) {
        SearchCriteria<Ipv4GuestSubnetNetworkMapVO> sc = SubnetSearch.create();
        sc.setParameters("subnet", subnet);
        return findOneBy(sc);
    }

    @Override
    public List<Ipv4GuestSubnetNetworkMapVO> findSubnetsInStates(Ipv4GuestSubnetNetworkMap.State... states) {
        SearchCriteria<Ipv4GuestSubnetNetworkMapVO> sc = StatesSearch.create();
        sc.setParameters("state", (Object[])states);
        return listBy(sc);
    }

    @Override
    public void deleteByParentId(long parentId) {
        SearchCriteria<Ipv4GuestSubnetNetworkMapVO> sc = ParentIdSearch.create();
        sc.setParameters("parentId", parentId);
        remove(sc);
    }

    @Override
    public List<Ipv4GuestSubnetNetworkMapVO> listAllNoParent() {
        SearchCriteria<Ipv4GuestSubnetNetworkMapVO> sc = NoParentSearch.create();
        return listBy(sc, null);
    }
}
