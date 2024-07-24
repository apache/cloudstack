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
package org.apache.cloudstack.framework.config.dao;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

import org.apache.cloudstack.framework.config.impl.ConfigurationSubGroupVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class ConfigurationSubGroupDaoImpl extends GenericDaoBase<ConfigurationSubGroupVO, Long> implements ConfigurationSubGroupDao {

    final SearchBuilder<ConfigurationSubGroupVO> nameSearch;
    final SearchBuilder<ConfigurationSubGroupVO> groupSearch;
    final SearchBuilder<ConfigurationSubGroupVO> nameAndGroupSearch;
    final SearchBuilder<ConfigurationSubGroupVO> keywordSearch;

    public ConfigurationSubGroupDaoImpl() {
        super();

        nameSearch = createSearchBuilder();
        nameSearch.and("name", nameSearch.entity().getName(), SearchCriteria.Op.LIKE);
        nameSearch.done();

        groupSearch = createSearchBuilder();
        groupSearch.and("groupId", groupSearch.entity().getGroupId(), SearchCriteria.Op.EQ);
        groupSearch.done();

        nameAndGroupSearch = createSearchBuilder();
        nameAndGroupSearch.and("name", nameAndGroupSearch.entity().getName(), SearchCriteria.Op.EQ);
        nameAndGroupSearch.and("groupId", nameAndGroupSearch.entity().getGroupId(), SearchCriteria.Op.EQ);
        nameAndGroupSearch.done();

        keywordSearch = createSearchBuilder();
        keywordSearch.and("keywords", keywordSearch.entity().getKeywords(), SearchCriteria.Op.NNULL);
        keywordSearch.done();
    }

    @Override
    public ConfigurationSubGroupVO findByName(String name) {
        SearchCriteria<ConfigurationSubGroupVO> sc = nameSearch.create();
        sc.setParameters("name", name);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public ConfigurationSubGroupVO startsWithName(String name) {
        SearchCriteria<ConfigurationSubGroupVO> sc = nameSearch.create();
        sc.setParameters("name", name + "%");
        return findOneIncludingRemovedBy(sc);
    }

    private ConfigurationSubGroupVO matchKeywordBy(BiPredicate<String, String> matcher, List<ConfigurationSubGroupVO> configurationSubGroups, String keyword) {
        for (ConfigurationSubGroupVO configurationSubGroup : configurationSubGroups) {
            if (StringUtils.isBlank(configurationSubGroup.getKeywords())) {
                continue;
            }

            String[] configKeywords = configurationSubGroup.getKeywords().split(",");
            if (configKeywords.length <= 0) {
                continue;
            }

            List<String> keywords = Arrays.asList(configKeywords);
            for (String configKeyword : keywords) {
                if (StringUtils.isNotBlank(configKeyword)) {
                    configKeyword = configKeyword.strip().toLowerCase();
                    if (matcher.test(keyword, configKeyword)) {
                        return configurationSubGroup;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public ConfigurationSubGroupVO findByKeyword(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }

        SearchCriteria<ConfigurationSubGroupVO> sc = keywordSearch.create();
        List<ConfigurationSubGroupVO> configurationSubGroups = listBy(sc);
        BiPredicate<String, String> equals = (a, b) -> { return a.equalsIgnoreCase(b); };
        ConfigurationSubGroupVO configSubGroup = matchKeywordBy(equals, configurationSubGroups, keyword);
        if (configSubGroup == null) {
            BiPredicate<String, String> startsWith = (a, b) -> { return a.startsWith(b); };
            configSubGroup = matchKeywordBy(startsWith, configurationSubGroups, keyword.toLowerCase());
        }
        return configSubGroup;
    }

    @Override
    public ConfigurationSubGroupVO findByNameAndGroup(String name, Long groupId) {
        SearchCriteria<ConfigurationSubGroupVO> sc = nameAndGroupSearch.create();
        sc.setParameters("name", name);
        sc.setParameters("groupId", groupId);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<ConfigurationSubGroupVO> findByGroup(Long groupId) {
        SearchCriteria<ConfigurationSubGroupVO> sc = groupSearch.create();
        sc.setParameters("groupId", groupId);
        final Filter filter = new Filter(ConfigurationSubGroupVO.class, "precedence", true, null, null);
        return listIncludingRemovedBy(sc, filter);
    }
}
