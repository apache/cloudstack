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
package com.cloud.network.dao;

import com.cloud.network.TungstenGuestNetworkIpAddressVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@DB
public class TungstenGuestNetworkIpAddressDaoImpl extends GenericDaoBase<TungstenGuestNetworkIpAddressVO, Long>
    implements TungstenGuestNetworkIpAddressDao {
    private static final Logger s_logger = Logger.getLogger(TungstenGuestNetworkIpAddressDaoImpl.class);
    final SearchBuilder<TungstenGuestNetworkIpAddressVO> AllFieldsSearch;
    final GenericSearchBuilder<TungstenGuestNetworkIpAddressVO, String> NetworkSearch;

    public TungstenGuestNetworkIpAddressDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("network_id", AllFieldsSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("guest_ip_address", AllFieldsSearch.entity().getGuestIpAddress(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("public_ip_address", AllFieldsSearch.entity().getPublicIpAddress(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
        NetworkSearch = createSearchBuilder(String.class);
        NetworkSearch.select(null, SearchCriteria.Func.DISTINCT, NetworkSearch.entity().getGuestIpAddress());
        NetworkSearch.and("network_id", NetworkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        NetworkSearch.done();
    }

    @Override
    public List<String> listByNetworkId(long networkId) {
        SearchCriteria<String> searchCriteria = NetworkSearch.create();
        searchCriteria.setParameters("network_id", networkId);
        return customSearch(searchCriteria, null);
    }

    @Override
    public TungstenGuestNetworkIpAddressVO findByNetworkIdAndPublicIp(final long networkId, final String publicIp) {
        SearchCriteria<TungstenGuestNetworkIpAddressVO> searchCriteria = AllFieldsSearch.create();
        searchCriteria.setParameters("network_id", networkId);
        searchCriteria.setParameters("public_ip_address", publicIp);
        return findOneBy(searchCriteria);
    }
}
