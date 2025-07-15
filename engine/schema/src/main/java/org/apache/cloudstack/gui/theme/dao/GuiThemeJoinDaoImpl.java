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
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.gui.theme.GuiThemeJoinVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Component
public class GuiThemeJoinDaoImpl extends GenericDaoBase<GuiThemeJoinVO, Long> implements GuiThemeJoinDao {
    @Inject
    GuiThemeDetailsDao guiThemeDetailsDao;

    public static final Long INVALID_ID = -1L;

    public GuiThemeJoinVO findDefaultTheme() {
        SearchBuilder<GuiThemeJoinVO> searchBuilder = createSearchBuilder();
        searchBuilder.and("commonNames", searchBuilder.entity().getCommonNames(), SearchCriteria.Op.NULL);
        searchBuilder.and("domainUuids", searchBuilder.entity().getDomains(), SearchCriteria.Op.NULL);
        searchBuilder.and("accountUuids", searchBuilder.entity().getAccounts(), SearchCriteria.Op.NULL);
        searchBuilder.done();

        SearchCriteria<GuiThemeJoinVO> searchCriteria = searchBuilder.create();

        return findOneBy(searchCriteria);
    }

    public Pair<List<GuiThemeJoinVO>, Integer> listGuiThemesWithNoAuthentication(String commonName) {
        SearchCriteria<GuiThemeJoinVO> searchCriteria = createGuiThemeSearchCriteria(null, null, commonName, null, null, null, false);
        return searchOrderByCreatedDate(searchCriteria, false);
    }

    public Pair<List<GuiThemeJoinVO>, Integer> listGuiThemes(Long id, String name, String commonName, String domainUuid, String accountUuid, boolean listAll,
                                                             boolean showRemoved, Boolean showPublic) {
        SearchCriteria<GuiThemeJoinVO> searchCriteria = createGuiThemeSearchCriteria(id, name, commonName, domainUuid, accountUuid, showPublic, listAll);

        if (listAll) {
            showRemoved = false;
        }

        return searchOrderByCreatedDate(searchCriteria, showRemoved);
    }

    private Pair<List<GuiThemeJoinVO>, Integer> searchOrderByCreatedDate(SearchCriteria<GuiThemeJoinVO> searchCriteria, boolean showRemoved) {
        Filter filter = new Filter(GuiThemeJoinVO.class, "created", false);
        return searchAndCount(searchCriteria, filter, showRemoved);
    }

    private SearchCriteria<GuiThemeJoinVO> createGuiThemeSearchCriteria(Long id, String name, String commonName, String domainUuid, String accountUuid, Boolean showPublic, boolean listAll) {
        SearchCriteria<GuiThemeJoinVO> searchCriteria = createGuiThemeJoinSearchBuilder(listAll, showPublic).create();
        List<Long> idList = new ArrayList<>();

        if (id != null) {
            idList.add(id);
        }

        searchCriteria.setParametersIfNotNull("name", name);
        searchCriteria.setParametersIfNotNull("isPublic", showPublic);

        if (StringUtils.isNotBlank(accountUuid)) {
            searchCriteria.setParameters("accountUuid", "%" + accountUuid + "%");
        }

        if (StringUtils.isNotBlank(commonName)) {
            setGuiThemeIdsFilteredByType(idList, ApiConstants.COMMON_NAME, commonName);
        }

        if (StringUtils.isNotBlank(domainUuid)) {
            setGuiThemeIdsFilteredByType(idList, ApiConstants.DOMAIN, domainUuid);
        }

        searchCriteria.setParametersIfNotNull("idIn", idList.toArray());

        return searchCriteria;
    }

    /**
     * Sets the `id IN ( )` clause of the query. If the informed value of common name or domain ID does not retrieve any GUI theme ID; then, an invalid ID (-1) is passed to the
     * list, as not a single entity has this ID. This is necessary as to set the parameter even if it did not find any GUI theme ID; otherwise, the query would not filter the
     * common name or domain ID passed.
     */
    public void setGuiThemeIdsFilteredByType(List<Long> idList, String type, String value) {
        List<Long> guiThemeIdsFilteredByType = new ArrayList<>();

        switch (type) {
            case ApiConstants.COMMON_NAME:
                guiThemeIdsFilteredByType = guiThemeDetailsDao.listGuiThemeIdsByCommonName(value);
                break;
            case ApiConstants.DOMAIN:
                guiThemeIdsFilteredByType = guiThemeDetailsDao.listGuiThemeIdsByDomainUuids(value);
                break;
        }

        if (CollectionUtils.isNotEmpty(guiThemeIdsFilteredByType)) {
            idList.addAll(guiThemeIdsFilteredByType);
            return;
        }
        logger.trace(String.format("No GUI theme with the specified [%s] with UUID [%s] was found, adding an invalid ID for filtering.", type, value));
        idList.add(INVALID_ID);
    }

    private SearchBuilder<GuiThemeJoinVO> createGuiThemeJoinSearchBuilder(boolean listAll, Boolean showPublic) {
        SearchBuilder<GuiThemeJoinVO> guiThemeJoinSearchBuilder = createSearchBuilder();
        guiThemeJoinSearchBuilder.and("idIn", guiThemeJoinSearchBuilder.entity().getId(), SearchCriteria.Op.IN);
        guiThemeJoinSearchBuilder.and("name", guiThemeJoinSearchBuilder.entity().getName(), SearchCriteria.Op.EQ);
        guiThemeJoinSearchBuilder.and("accountUuid", guiThemeJoinSearchBuilder.entity().getAccounts(), SearchCriteria.Op.LIKE);

        if (!listAll && showPublic != null) {
            guiThemeJoinSearchBuilder.and("isPublic", guiThemeJoinSearchBuilder.entity().getIsPublic(), SearchCriteria.Op.EQ);
        }

        return guiThemeJoinSearchBuilder;
    }
}
