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

import org.apache.cloudstack.framework.config.impl.ConfigurationSubGroupVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class ConfigurationSubGroupDaoImpl extends GenericDaoBase<ConfigurationSubGroupVO, Long> implements ConfigurationSubGroupDao {
    private static final Logger s_logger = Logger.getLogger(ConfigurationSubGroupDaoImpl.class);

    final SearchBuilder<ConfigurationSubGroupVO> NameSearch;
    final SearchBuilder<ConfigurationSubGroupVO> GroupSearch;
    final SearchBuilder<ConfigurationSubGroupVO> NameAndGroupSearch;

    public ConfigurationSubGroupDaoImpl() {
        super();

        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.LIKE);
        NameSearch.done();

        GroupSearch = createSearchBuilder();
        GroupSearch.and("groupId", GroupSearch.entity().getGroupId(), SearchCriteria.Op.EQ);
        GroupSearch.done();

        NameAndGroupSearch = createSearchBuilder();
        NameAndGroupSearch.and("name", NameAndGroupSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameAndGroupSearch.and("groupId", NameAndGroupSearch.entity().getGroupId(), SearchCriteria.Op.EQ);
        NameAndGroupSearch.done();
    }

    @Override
    public ConfigurationSubGroupVO findByName(String name) {
        SearchCriteria<ConfigurationSubGroupVO> sc = NameSearch.create();
        sc.setParameters("name", name);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public ConfigurationSubGroupVO startsWithName(String name) {
        SearchCriteria<ConfigurationSubGroupVO> sc = NameSearch.create();
        sc.setParameters("name", name + "%");
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public ConfigurationSubGroupVO findByKeyword(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }

        List<ConfigurationSubGroupVO> configurationSubGroups = listAll();
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
                    configKeyword = configKeyword.strip();
                    if (configKeyword.equalsIgnoreCase(keyword) || configKeyword.toLowerCase().startsWith(keyword.toLowerCase())) {
                        return configurationSubGroup;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public ConfigurationSubGroupVO findByNameAndGroup(String name, Long groupId) {
        SearchCriteria<ConfigurationSubGroupVO> sc = NameAndGroupSearch.create();
        sc.setParameters("name", name);
        sc.setParameters("groupId", groupId);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<ConfigurationSubGroupVO> findByGroup(Long groupId) {
        SearchCriteria<ConfigurationSubGroupVO> sc = GroupSearch.create();
        sc.setParameters("groupId", groupId);
        final Filter filter = new Filter(ConfigurationSubGroupVO.class, "precedence", true, null, null);
        return listIncludingRemovedBy(sc, filter);
    }
}
