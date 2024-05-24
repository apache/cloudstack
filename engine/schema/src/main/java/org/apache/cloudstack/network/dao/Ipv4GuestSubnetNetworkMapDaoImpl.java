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

import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMap;
import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMapVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@DB
public class Ipv4GuestSubnetNetworkMapDaoImpl extends GenericDaoBase<Ipv4GuestSubnetNetworkMapVO, Long> implements Ipv4GuestSubnetNetworkMapDao {

    protected SearchBuilder<Ipv4GuestSubnetNetworkMapVO> ParentStateSearch;
    protected SearchBuilder<Ipv4GuestSubnetNetworkMapVO> ParentIdSearch;
    protected SearchBuilder<Ipv4GuestSubnetNetworkMapVO> NetworkIdSearch;
    protected SearchBuilder<Ipv4GuestSubnetNetworkMapVO> SubnetSearch;
    protected SearchBuilder<Ipv4GuestSubnetNetworkMapVO> StatesSearch;

    @PostConstruct
    public void init() {
        ParentStateSearch = createSearchBuilder();
        ParentStateSearch.and("parentId", ParentStateSearch.entity().getParentId(), SearchCriteria.Op.EQ);
        ParentStateSearch.and("state", ParentStateSearch.entity().getState(), SearchCriteria.Op.IN);
        ParentStateSearch.done();
        ParentIdSearch = createSearchBuilder();
        ParentIdSearch.and("parentId", ParentIdSearch.entity().getParentId(), SearchCriteria.Op.EQ);
        ParentIdSearch.done();
        NetworkIdSearch = createSearchBuilder();
        NetworkIdSearch.and("networkId", NetworkIdSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        NetworkIdSearch.done();
        SubnetSearch = createSearchBuilder();
        SubnetSearch.and("subnet", SubnetSearch.entity().getSubnet(), SearchCriteria.Op.EQ);
        SubnetSearch.done();
        StatesSearch = createSearchBuilder();
        StatesSearch.and("state", StatesSearch.entity().getState(), SearchCriteria.Op.IN);
        StatesSearch.done();
    }

    @Override
    public List<Ipv4GuestSubnetNetworkMapVO> listUsedByParent(long parentId) {
        SearchCriteria<Ipv4GuestSubnetNetworkMapVO> sc = ParentStateSearch.create();
        sc.setParameters("parentId", parentId);
        sc.setParameters("state", (Object[]) new Ipv4GuestSubnetNetworkMap.State[]{Ipv4GuestSubnetNetworkMap.State.Allocated, Ipv4GuestSubnetNetworkMap.State.Allocating});
        Filter searchFilter = new Filter(Ipv4GuestSubnetNetworkMapVO.class, "id", true, null, 1L);
        return listBy(sc, searchFilter);
    }

    @Override
    public Ipv4GuestSubnetNetworkMapVO findFirstAvailable(long parentId) {
        SearchCriteria<Ipv4GuestSubnetNetworkMapVO> sc = ParentStateSearch.create();
        sc.setParameters("parentId", parentId);
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
}
