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
package com.cloud.storage.dao;


import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.storage.GuestOSVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class GuestOSDaoImpl extends GenericDaoBase<GuestOSVO, Long> implements GuestOSDao {

    protected final SearchBuilder<GuestOSVO> Search;

    protected final SearchBuilder<GuestOSVO> displayNameSearch;

    public GuestOSDaoImpl() {
        Search = createSearchBuilder();
        Search.and("category_id", Search.entity().getCategoryId(), SearchCriteria.Op.EQ);
        Search.and("display_name", Search.entity().getDisplayName(), SearchCriteria.Op.EQ);
        Search.and("is_user_defined", Search.entity().getIsUserDefined(), SearchCriteria.Op.EQ);
        Search.done();

        displayNameSearch = createSearchBuilder();
        displayNameSearch.and("display_name", displayNameSearch.entity().getDisplayName(), SearchCriteria.Op.LIKE);
        displayNameSearch.done();

    }

    @Override
    public GuestOSVO listByDisplayName(String displayName) {
        SearchCriteria<GuestOSVO> sc = Search.create();
        sc.setParameters("display_name", displayName);
        return findOneBy(sc);
    }

    @Override
    public List<GuestOSVO> listLikeDisplayName(String displayName) {
        SearchCriteria<GuestOSVO> sc = displayNameSearch.create();
        sc.setParameters("display_name", "%" + displayName + "%");
        return listBy(sc);
    }

    @Override
    public GuestOSVO findByCategoryIdAndDisplayNameOrderByCreatedDesc(long categoryId, String displayName) {
        SearchCriteria<GuestOSVO> sc = Search.create();
        sc.setParameters("category_id", categoryId);
        sc.setParameters("display_name", displayName);
        sc.setParameters("is_user_defined", false);

        Filter orderByFilter = new Filter(GuestOSVO.class, "created", false, null, 1L);
        List<GuestOSVO> guestOSlist = listBy(sc, orderByFilter);
        if (CollectionUtils.isNotEmpty(guestOSlist)) {
            return guestOSlist.get(0);
        }
        return null;
    }
}
