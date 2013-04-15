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
package com.cloud.offerings.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.offerings.NetworkOfferingDetailsVO;
import com.cloud.storage.VMTemplateDetailVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class NetworkOfferingDetailsDaoImpl extends GenericDaoBase<NetworkOfferingDetailsVO, Long> implements NetworkOfferingDetailsDao{
    protected final SearchBuilder<NetworkOfferingDetailsVO> DetailSearch;
    
    public NetworkOfferingDetailsDaoImpl() {
        
        DetailSearch = createSearchBuilder();
        DetailSearch.and("offeringId", DetailSearch.entity().getOfferingId(), SearchCriteria.Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailSearch.done();
    }
    
    @Override
    public Map<String, String> getNtwkOffDetails(long offeringId) {
        SearchCriteria<NetworkOfferingDetailsVO> sc = DetailSearch.create();
        sc.setParameters("offeringId", offeringId);
        
        List<NetworkOfferingDetailsVO> results = search(sc, null);
        Map<String, String> details = new HashMap<String, String>(results.size());
        for (NetworkOfferingDetailsVO result : results) {
            details.put(result.getName(), result.getValue());
        }
        
        return details;
    }

}
