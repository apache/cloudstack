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

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.gui.theme.GuiThemeDetailsVO;
import org.apache.cloudstack.gui.theme.GuiThemeVO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Component
public class GuiThemeDetailsDaoImpl extends GenericDaoBase<GuiThemeDetailsVO, Long> implements GuiThemeDetailsDao {

    @Inject
    DomainDao domainDao;

    @Inject
    GuiThemeDao guiThemeDao;

    public List<Long> listGuiThemeIdsByCommonName(String commonName) {
        GenericSearchBuilder<GuiThemeDetailsVO, Long> detailsDaoSearchBuilder = createSearchBuilder(Long.class);
        detailsDaoSearchBuilder.selectFields(detailsDaoSearchBuilder.entity().getGuiThemeId());
        detailsDaoSearchBuilder.and("commonNameType", detailsDaoSearchBuilder.entity().getType(), SearchCriteria.Op.EQ);
        detailsDaoSearchBuilder.and().op("firstReplace", detailsDaoSearchBuilder.entity().getValue(), SearchCriteria.Op.LIKE_REPLACE);
        detailsDaoSearchBuilder.or("secondReplace", detailsDaoSearchBuilder.entity().getValue(), SearchCriteria.Op.LIKE_REPLACE).cp();
        detailsDaoSearchBuilder.done();

        SearchCriteria<Long> searchCriteria = detailsDaoSearchBuilder.create();
        searchCriteria.setParameters("commonNameType", "commonName");
        searchCriteria.setParameters("firstReplace", commonName, "*", "%");
        searchCriteria.setParameters("secondReplace", commonName, "*.", "%");

        return customSearch(searchCriteria, null);
    }

    public List<Long> listGuiThemeIdsByDomainUuids(String domainUuid) {
        List<Long> guiThemeIds = new ArrayList<>();
        String requestedDomainPath = domainDao.findByUuid(domainUuid).getPath();

        SearchBuilder<DomainVO> domainSearchBuilderPathLike = domainDao.createSearchBuilder();
        domainSearchBuilderPathLike.and("pathLike", domainSearchBuilderPathLike.entity().getPath(), SearchCriteria.Op.LIKE_CONCAT);

        SearchBuilder<DomainVO> domainSearchBuilderPathEq = domainDao.createSearchBuilder();
        domainSearchBuilderPathEq.and("pathEq", domainSearchBuilderPathEq.entity().getPath(), SearchCriteria.Op.EQ);

        GenericSearchBuilder<GuiThemeDetailsVO, Long> detailsSearchBuilderPathLike = createDetailsSearchBuilder(domainSearchBuilderPathLike);
        SearchCriteria<Long> searchCriteriaDomainPathLike = setParametersDomainPathLike(detailsSearchBuilderPathLike, requestedDomainPath);

        GenericSearchBuilder<GuiThemeDetailsVO, Long> detailsSearchBuilderPathEq = createDetailsSearchBuilder(domainSearchBuilderPathEq);
        SearchCriteria<Long> searchCriteriaDomainPathEq = setParametersDomainPathEq(detailsSearchBuilderPathEq, requestedDomainPath);

        guiThemeIds.addAll(customSearch(searchCriteriaDomainPathLike, null));
        guiThemeIds.addAll(customSearch(searchCriteriaDomainPathEq, null));
        return guiThemeIds;
    }

    private SearchCriteria<Long> setParametersDomainPathLike(GenericSearchBuilder<GuiThemeDetailsVO, Long> detailsSearchBuilderPathLike, String requestedDomainPath) {
        SearchCriteria<Long> searchCriteria = detailsSearchBuilderPathLike.create();
        searchCriteria.setParameters("domainUuidType", "domain");
        searchCriteria.setJoinParameters("domainJoin", "pathLike", requestedDomainPath, "%");
        searchCriteria.setJoinParameters("guiThemesJoin", "recursiveDomains", true);

        return searchCriteria;
    }

    private SearchCriteria<Long> setParametersDomainPathEq(GenericSearchBuilder<GuiThemeDetailsVO, Long> detailsSearchBuilderPathEq, String requestedDomainPath) {
        SearchCriteria<Long> searchCriteria = detailsSearchBuilderPathEq.create();
        searchCriteria.setParameters("domainUuidType", "domain");
        searchCriteria.setJoinParameters("domainJoin", "pathEq", requestedDomainPath);
        searchCriteria.setJoinParameters("guiThemesJoin", "recursiveDomains", false);

        return searchCriteria;
    }

    private GenericSearchBuilder<GuiThemeDetailsVO, Long> createDetailsSearchBuilder(SearchBuilder<DomainVO> domainSearchBuilder) {
        SearchBuilder<GuiThemeVO> guiThemeDaoSearchBuilder = guiThemeDao.createSearchBuilder();
        guiThemeDaoSearchBuilder.and("recursiveDomains", guiThemeDaoSearchBuilder.entity().isRecursiveDomains(), SearchCriteria.Op.EQ);

        GenericSearchBuilder<GuiThemeDetailsVO, Long> guiThemesDetailsJoinDomainJoinGuiThemesSearchBuilder = createSearchBuilder(Long.class);
        guiThemesDetailsJoinDomainJoinGuiThemesSearchBuilder.selectFields(guiThemesDetailsJoinDomainJoinGuiThemesSearchBuilder.entity().getGuiThemeId());
        guiThemesDetailsJoinDomainJoinGuiThemesSearchBuilder.and("domainUuidType", guiThemesDetailsJoinDomainJoinGuiThemesSearchBuilder.entity().getType(), SearchCriteria.Op.EQ);
        guiThemesDetailsJoinDomainJoinGuiThemesSearchBuilder.join("domainJoin", domainSearchBuilder, domainSearchBuilder.entity().getUuid(),
                guiThemesDetailsJoinDomainJoinGuiThemesSearchBuilder.entity().getValue(), JoinBuilder.JoinType.INNER);
        guiThemesDetailsJoinDomainJoinGuiThemesSearchBuilder.join("guiThemesJoin", guiThemeDaoSearchBuilder, guiThemeDaoSearchBuilder.entity().getId(),
                guiThemesDetailsJoinDomainJoinGuiThemesSearchBuilder.entity().getGuiThemeId(), JoinBuilder.JoinType.INNER);

        domainSearchBuilder.done();
        guiThemeDaoSearchBuilder.done();
        guiThemesDetailsJoinDomainJoinGuiThemesSearchBuilder.done();

        return guiThemesDetailsJoinDomainJoinGuiThemesSearchBuilder;
    }

    public void expungeByGuiThemeId(long guiThemeId) {
        SearchBuilder<GuiThemeDetailsVO> searchBuilder = createSearchBuilder();
        searchBuilder.and("guiThemeId", searchBuilder.entity().getGuiThemeId(), SearchCriteria.Op.EQ);
        searchBuilder.done();

        SearchCriteria<GuiThemeDetailsVO> searchCriteria = searchBuilder.create();
        searchCriteria.setParameters("guiThemeId", guiThemeId);
        expunge(searchCriteria);
    }
}
