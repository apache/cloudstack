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

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.UpdateBuilder;

@Component
public class NetworkOpDaoImpl extends GenericDaoBase<NetworkOpVO, Long> implements NetworkOpDao {
    protected final SearchBuilder<NetworkOpVO> AllFieldsSearch;
    protected final GenericSearchBuilder<NetworkOpVO, Integer> ActiveNicsSearch;
    protected final Attribute _activeNicsAttribute;
    
    protected NetworkOpDaoImpl() {
        super();
        
        ActiveNicsSearch = createSearchBuilder(Integer.class);
        ActiveNicsSearch.selectField(ActiveNicsSearch.entity().getActiveNicsCount());
        ActiveNicsSearch.and("network", ActiveNicsSearch.entity().getId(), Op.EQ);
        ActiveNicsSearch.done();
        
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("network", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.done();

        _activeNicsAttribute = _allAttributes.get("activeNicsCount");
        assert _activeNicsAttribute != null : "Cannot find activeNicsCount";
    }

    public int getActiveNics(long networkId) {
        SearchCriteria<Integer> sc = ActiveNicsSearch.create();
        sc.setParameters("network", networkId);
        
        return customSearch(sc, null).get(0);
    }
    
    public void changeActiveNicsBy(long networkId, int count) {
        
        SearchCriteria<NetworkOpVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);

        NetworkOpVO vo = createForUpdate();
        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.incr(_activeNicsAttribute, count);
        
        update(builder, sc, null);
    }
    
    public void setCheckForGc(long networkId) {
        NetworkOpVO vo = createForUpdate();
        vo.setCheckForGc(true);
        update(networkId, vo);
    }
    
    public void clearCheckForGc(long networkId) {
        NetworkOpVO vo = createForUpdate();
        vo.setCheckForGc(false);
        update(networkId, vo);
    }
}
