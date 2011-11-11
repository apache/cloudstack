/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;


public class PhysicalNetworkIsolationMethodDaoImpl extends GenericDaoBase<PhysicalNetworkIsolationMethodVO, Long> implements GenericDao<PhysicalNetworkIsolationMethodVO, Long> {
    private final GenericSearchBuilder<PhysicalNetworkIsolationMethodVO, String> IsolationMethodSearch;
    private final SearchBuilder<PhysicalNetworkIsolationMethodVO> AllFieldsSearch;

    protected PhysicalNetworkIsolationMethodDaoImpl() {
        super();
        IsolationMethodSearch = createSearchBuilder(String.class);
        IsolationMethodSearch.selectField(IsolationMethodSearch.entity().getIsolationMethod());
        IsolationMethodSearch.and("physicalNetworkId", IsolationMethodSearch.entity().getPhysicalNetworkId(), Op.EQ);
        IsolationMethodSearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("physicalNetworkId", AllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        AllFieldsSearch.and("isolationMethod", AllFieldsSearch.entity().getIsolationMethod(), Op.EQ);
        AllFieldsSearch.done();
    }

    public List<String> getAllIsolationMethod(long physicalNetworkId) {
        SearchCriteria<String> sc = IsolationMethodSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);

        return customSearch(sc, null);
    }

    public String getIsolationMethod(long physicalNetworkId) {
        SearchCriteria<String> sc = IsolationMethodSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);

        return customSearch(sc, null).get(0);
    }
    
    public int clearIsolationMethods(long physicalNetworkId) {
        SearchCriteria<PhysicalNetworkIsolationMethodVO> sc = AllFieldsSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);

        return remove(sc);
    }

}
