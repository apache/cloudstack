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


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cloud.storage.GuestOS;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
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
    public GuestOSVO findOneByDisplayName(String displayName) {
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

    /**
     +       "select display_name from"
     +              "(select display_name, count(1) as count from guest_os go1 where removed is null group by display_name having count > 1) tab0";
     *
     * @return
     */
    @Override
    @DB
    public Set<String> findDoubleNames() {
        String selectSql = "SELECT display_name FROM (SELECT display_name, count(1) AS count FROM guest_os go1 WHERE removed IS NULL GROUP BY display_name HAVING count > 1) tab0";
        Set<String> names = new HashSet<>();
        Connection conn = TransactionLegacy.getStandaloneConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(selectSql);
            ResultSet rs = stmt.executeQuery();
            while (rs != null && rs.next()) {
                names.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            throw new CloudRuntimeException("Error while trying to find duplicate guest OSses", ex);
        }
        return names;
    }

    /**
     * get all with a certain display name
     * @param displayName
     * @return a list with GuestOS objects
     */
    @Override
    public List<GuestOSVO> listByDisplayName(String displayName) {
        SearchCriteria<GuestOSVO> sc = Search.create();
        sc.setParameters("display_name", displayName);
        return listBy(sc);
    }

    public Pair<List<? extends GuestOS>, Integer> listGuestOSByCriteria(Long startIndex, Long pageSize, Long id, Long osCategoryId, String description, String keyword, Boolean forDisplay) {
        final Filter searchFilter = new Filter(GuestOSVO.class, "displayName", true, startIndex, pageSize);
        final SearchCriteria<GuestOSVO> sc = createSearchCriteria();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (osCategoryId != null) {
            sc.addAnd("categoryId", SearchCriteria.Op.EQ, osCategoryId);
        }

        if (description != null) {
            sc.addAnd("displayName", SearchCriteria.Op.LIKE, "%" + description + "%");
        }

        if (keyword != null) {
            sc.addAnd("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        }

        if (forDisplay != null) {
            sc.addAnd("display", SearchCriteria.Op.EQ, forDisplay);
        }

        final Pair<List<GuestOSVO>, Integer> result = searchAndCount(sc, searchFilter);
        return new Pair<>(result.first(), result.second());
    }

}
