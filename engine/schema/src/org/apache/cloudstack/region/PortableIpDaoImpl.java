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
package org.apache.cloudstack.region;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = {PortableIpDao.class})
public class PortableIpDaoImpl extends GenericDaoBase<PortableIpVO, Long> implements PortableIpDao {

    private final SearchBuilder<PortableIpVO> listByRegionIDSearch;
    private final SearchBuilder<PortableIpVO> listByRangeIDSearch;
    private final SearchBuilder<PortableIpVO> listByRangeIDAndStateSearch;
    private final SearchBuilder<PortableIpVO> listByRegionIDAndStateSearch;
    private final SearchBuilder<PortableIpVO> findByIpAddressSearch;

    public PortableIpDaoImpl() {
        listByRegionIDSearch = createSearchBuilder();
        listByRegionIDSearch.and("regionId", listByRegionIDSearch.entity().getRegionId(), SearchCriteria.Op.EQ);
        listByRegionIDSearch.done();

        listByRangeIDSearch = createSearchBuilder();
        listByRangeIDSearch.and("rangeId", listByRangeIDSearch.entity().getRangeId(), SearchCriteria.Op.EQ);
        listByRangeIDSearch.done();

        listByRangeIDAndStateSearch = createSearchBuilder();
        listByRangeIDAndStateSearch.and("rangeId", listByRangeIDAndStateSearch.entity().getRangeId(), SearchCriteria.Op.EQ);
        listByRangeIDAndStateSearch.and("state", listByRangeIDAndStateSearch.entity().getState(), SearchCriteria.Op.EQ);
        listByRangeIDAndStateSearch.done();

        listByRegionIDAndStateSearch = createSearchBuilder();
        listByRegionIDAndStateSearch.and("regionId", listByRegionIDAndStateSearch.entity().getRegionId(), SearchCriteria.Op.EQ);
        listByRegionIDAndStateSearch.and("state", listByRegionIDAndStateSearch.entity().getState(), SearchCriteria.Op.EQ);
        listByRegionIDAndStateSearch.done();

        findByIpAddressSearch = createSearchBuilder();
        findByIpAddressSearch.and("address", findByIpAddressSearch.entity().getAddress(), SearchCriteria.Op.EQ);
        findByIpAddressSearch.done();
    }

    @Override
    public List<PortableIpVO> listByRegionId(int regionIdId) {
        SearchCriteria<PortableIpVO> sc = listByRegionIDSearch.create();
        sc.setParameters("regionId", regionIdId);
        return listBy(sc);
    }

    @Override
    public List<PortableIpVO> listByRangeId(long rangeId) {
        SearchCriteria<PortableIpVO> sc = listByRangeIDSearch.create();
        sc.setParameters("rangeId", rangeId);
        return listBy(sc);
    }

    @Override
    public List<PortableIpVO> listByRangeIdAndState(long rangeId, PortableIp.State state) {
        SearchCriteria<PortableIpVO> sc = listByRangeIDAndStateSearch.create();
        sc.setParameters("rangeId", rangeId);
        sc.setParameters("state", state);
        return listBy(sc);
    }

    @Override
    public List<PortableIpVO> listByRegionIdAndState(int regionId, PortableIp.State state) {
        SearchCriteria<PortableIpVO> sc = listByRegionIDAndStateSearch.create();
        sc.setParameters("regionId", regionId);
        sc.setParameters("state", state);
        return listBy(sc);
    }

    @Override
    public PortableIpVO findByIpAddress(String ipAddress) {
        SearchCriteria<PortableIpVO> sc = findByIpAddressSearch.create();
        sc.setParameters("address", ipAddress);
        return findOneBy(sc);
    }

    @Override
    public void unassignIpAddress(long ipAddressId) {
        PortableIpVO address = createForUpdate();
        address.setAllocatedToAccountId(null);
        address.setAllocatedInDomainId(null);
        address.setAllocatedTime(null);
        address.setState(PortableIp.State.Free);
        address.setAssociatedWithNetworkId(null);
        address.setAssociatedDataCenterId(null);
        address.setAssociatedWithVpcId(null);
        address.setPhysicalNetworkId(null);
        update(ipAddressId, address);
    }
}
