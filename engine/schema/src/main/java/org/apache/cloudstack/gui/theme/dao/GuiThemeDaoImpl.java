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
package org.apache.cloudstack.gui.theme.dao;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.gui.themes.GuiThemeVO;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
public class GuiThemeDaoImpl extends GenericDaoBase<GuiThemeVO, Long> implements GuiThemeDao {

    @Override
    public GuiThemeVO findDefaultTheme() {
        SearchBuilder<GuiThemeVO> searchBuilder = createSearchBuilder();
        searchBuilder.and("commonNames", searchBuilder.entity().getCommonNames(), SearchCriteria.Op.NULL);
        searchBuilder.and("domainUuids", searchBuilder.entity().getDomainUuids(), SearchCriteria.Op.NULL);
        searchBuilder.and("accountUuids", searchBuilder.entity().getAccountUuids(), SearchCriteria.Op.NULL);
        searchBuilder.done();

        SearchCriteria<GuiThemeVO> searchCriteria = searchBuilder.create();

        return findOneBy(searchCriteria);
    }

    @Override
    public Pair<List<GuiThemeVO>, Integer> listGuiThemes(Long id, String name, String commonName, String domainUuid, String accountUuid, boolean listAll, boolean showRemoved,
                                                         Boolean showPublic) {
        SearchCriteria<GuiThemeVO> searchCriteria = createGuiThemeSearchCriteria(id, name, commonName, domainUuid, accountUuid, showPublic, listAll);

        if (listAll) {
            showRemoved = false;
        }

        return searchAndCount(searchCriteria, null, showRemoved);
    }

    private SearchCriteria<GuiThemeVO> createGuiThemeSearchCriteria(Long id, String name, String commonName, String domainUuid, String accountUuid, Boolean showPublic, boolean listAll) {
        SearchCriteria<GuiThemeVO> searchCriteria = createGuiThemeSearchBuilder(accountUuid, domainUuid, commonName, listAll, showPublic).create();

        searchCriteria.setParametersIfNotNull("id", id);
        searchCriteria.setParametersIfNotNull("name", name);
        searchCriteria.setParametersIfNotNull("isPublic", showPublic);

        if (StringUtils.isNotBlank(commonName)) {
            searchCriteria.setParameters("commonName", "%" + commonName + "%");
        }

        if (StringUtils.isNotBlank(domainUuid)) {
            searchCriteria.setParameters("domainId", "%" + domainUuid + "%");
        }

        if (StringUtils.isNotBlank(accountUuid)) {
            searchCriteria.setParameters("accountId", "%" + accountUuid + "%");
        }

        return searchCriteria;
    }

    private SearchBuilder<GuiThemeVO> createGuiThemeSearchBuilder(String accountUuid, String domainUuid, String commonName, boolean listAll, Boolean showPublic) {
        SearchBuilder<GuiThemeVO> searchBuilder = createSearchBuilder();

        searchBuilder.and("id", searchBuilder.entity().getId(), SearchCriteria.Op.EQ);
        searchBuilder.and("name", searchBuilder.entity().getName(), SearchCriteria.Op.EQ);

        if (accountUuid != null) {
            searchBuilder.and("accountId", searchBuilder.entity().getAccountUuids(), SearchCriteria.Op.LIKE);
        }

        if (domainUuid != null) {
            searchBuilder.and("domainId", searchBuilder.entity().getDomainUuids(), SearchCriteria.Op.LIKE);
        }

        if (commonName != null) {
            searchBuilder.and("commonName", searchBuilder.entity().getCommonNames(), SearchCriteria.Op.LIKE);
        }

        if (!listAll && showPublic != null) {
            searchBuilder.and("isPublic", searchBuilder.entity().getIsPublic(), SearchCriteria.Op.EQ);
        }

        searchBuilder.done();
        return searchBuilder;
    }

    @Override
    public Pair<List<GuiThemeVO>, Integer> listGuiThemesWithNoAuthentication(String commonName) {
        SearchCriteria<GuiThemeVO> searchCriteria = createGuiThemeSearchCriteria(null, null, commonName, null, null, null, false);
        Filter filter = new Filter(GuiThemeVO.class, "created", false);

        return searchAndCount(searchCriteria, filter, false);
    }
}
