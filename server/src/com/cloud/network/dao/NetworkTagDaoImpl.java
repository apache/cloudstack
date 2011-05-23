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

import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;


public class NetworkTagDaoImpl extends GenericDaoBase<NetworkTagVO, Long> implements GenericDao<NetworkTagVO, Long> {
    private final GenericSearchBuilder<NetworkTagVO, String> TagSearch;
    private final SearchBuilder<NetworkTagVO> AllFieldsSearch;

    protected NetworkTagDaoImpl() {
        super();
        TagSearch = createSearchBuilder(String.class);
        TagSearch.selectField(TagSearch.entity().getTag());
        TagSearch.and("networkid", TagSearch.entity().getNetworkId(), Op.EQ);
        TagSearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("networkid", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.and("tag", AllFieldsSearch.entity().getTag(), Op.EQ);
        AllFieldsSearch.done();
    }

    public List<String> getTags(long networkId) {
        SearchCriteria<String> sc = TagSearch.create();
        sc.setParameters("networkid", networkId);

        return customSearch(sc, null);
    }

    public int clearTags(long networkId) {
        SearchCriteria<NetworkTagVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkid", networkId);

        return remove(sc);
    }

}
