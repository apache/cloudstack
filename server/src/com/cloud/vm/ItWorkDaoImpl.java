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
package com.cloud.vm;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.time.InaccurateClock;
import com.cloud.vm.ItWorkVO.Step;
import com.cloud.vm.VirtualMachine.State;

@Local(value=ItWorkDao.class)
public class ItWorkDaoImpl extends GenericDaoBase<ItWorkVO, String> implements ItWorkDao {
    protected final SearchBuilder<ItWorkVO> AllFieldsSearch;
    protected final SearchBuilder<ItWorkVO> CleanupSearch;
    
    protected ItWorkDaoImpl() {
        super();
        
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("instance", AllFieldsSearch.entity().getInstanceId(), Op.EQ);
        AllFieldsSearch.and("op", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("step", AllFieldsSearch.entity().getStep(), Op.EQ);
        AllFieldsSearch.done();
        
        CleanupSearch = createSearchBuilder();
        CleanupSearch.and("step", CleanupSearch.entity().getState(), Op.IN);
        CleanupSearch.and("time", CleanupSearch.entity().getUpdatedAt(), Op.LT);
        CleanupSearch.done();
    }
    
    @Override
    public ItWorkVO findByInstance(long instanceId, State state) {
        SearchCriteria<ItWorkVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        sc.setParameters("op", state);
        
        return findOneBy(sc);
    }
    
    @Override
    public void cleanup(long wait) {
        SearchCriteria<ItWorkVO> sc = CleanupSearch.create();
        sc.setParameters("step", Step.Done, Step.Cancelled);
        sc.setParameters("time", InaccurateClock.getTimeInSeconds() - wait);
        
        remove(sc);
    }
    
    @Override
    public boolean update(String id, ItWorkVO work) {
        work.setUpdatedAt(InaccurateClock.getTimeInSeconds());
        
        return super.update(id, work);
    }
}
