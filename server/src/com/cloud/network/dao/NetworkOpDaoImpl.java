/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.network.dao;

import java.util.List;

import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.UpdateBuilder;


public class NetworkOpDaoImpl extends GenericDaoBase<NetworkOpVO, Long> implements GenericDao<NetworkOpVO, Long> {
    protected final SearchBuilder<NetworkOpVO> AllFieldsSearch;
    protected final GenericSearchBuilder<NetworkOpVO, Integer> ActiveNicsSearch;
    protected final GenericSearchBuilder<NetworkOpVO, Long> GarbageCollectSearch;
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
        
        GarbageCollectSearch = createSearchBuilder(Long.class);
        GarbageCollectSearch.selectField(GarbageCollectSearch.entity().getId());
        GarbageCollectSearch.and("activenics", GarbageCollectSearch.entity().getActiveNicsCount(), Op.EQ);
        GarbageCollectSearch.and("gc", GarbageCollectSearch.entity().isGarbageCollected(), Op.EQ);
        GarbageCollectSearch.and("check", GarbageCollectSearch.entity().isCheckForGc(), Op.EQ);
        GarbageCollectSearch.done();
        
        _activeNicsAttribute = _allAttributes.get("activeNicsCount");
        assert _activeNicsAttribute != null : "Cannot find activeNicsCount";
    }
    
    public List<Long> getNetworksToGarbageCollect() {
        SearchCriteria<Long> sc = GarbageCollectSearch.create();
        sc.setParameters("activenics", 0);
        sc.setParameters("gc", true);
        sc.setParameters("check", true);
        
        return customSearch(sc, null);
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
