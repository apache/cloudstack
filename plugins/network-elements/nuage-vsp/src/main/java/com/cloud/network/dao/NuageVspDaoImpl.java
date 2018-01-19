//
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
//

package com.cloud.network.dao;

import com.cloud.network.NuageVspDeviceVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NuageVspDaoImpl extends GenericDaoBase<NuageVspDeviceVO, Long>
        implements NuageVspDao {

    private final SearchBuilder<NuageVspDeviceVO> physicalNetworkIdSearch;
    private final SearchBuilder<NuageVspDeviceVO> hostIdSearch;

    public NuageVspDaoImpl() {
        physicalNetworkIdSearch = createSearchBuilder();
        physicalNetworkIdSearch.and("physicalNetworkId", physicalNetworkIdSearch.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
        physicalNetworkIdSearch.done();

        hostIdSearch = createSearchBuilder();
        hostIdSearch.and("hostId", hostIdSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        hostIdSearch.done();
    }

    @Override
    public List<NuageVspDeviceVO> listByPhysicalNetwork(long physicalNetworkId) {
        SearchCriteria<NuageVspDeviceVO> sc = physicalNetworkIdSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return search(sc, null);
    }

    @Override
    public List<NuageVspDeviceVO> listByHost(long hostId) {
        SearchCriteria<NuageVspDeviceVO> sc = hostIdSearch.create();
        sc.setParameters("hostId", hostId);
        return search(sc, null);
    }
}
