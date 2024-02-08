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
package com.cloud.upgrade;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationDaoImpl;
import org.apache.cloudstack.framework.config.dao.ConfigurationGroupDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationGroupDaoImpl;
import org.apache.cloudstack.framework.config.dao.ConfigurationSubGroupDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationSubGroupDaoImpl;
import org.apache.cloudstack.framework.config.impl.ConfigurationSubGroupVO;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigurationGroupsAggregator {

    protected Logger LOG = LogManager.getLogger(getClass());

    @Inject
    ConfigurationDao configDao;
    @Inject
    ConfigurationGroupDao configGroupDao;
    @Inject
    ConfigurationSubGroupDao configSubGroupDao;

    public ConfigurationGroupsAggregator() {
        configDao = new ConfigurationDaoImpl();
        configGroupDao = new ConfigurationGroupDaoImpl();
        configSubGroupDao = new ConfigurationSubGroupDaoImpl();
    }

    public void updateConfigurationGroups() {
        LOG.debug("Updating configuration groups");
        List<ConfigurationVO> configs =  configDao.listAllIncludingRemoved();
        if (CollectionUtils.isEmpty(configs)) {
            return;
        }

        for (final ConfigurationVO config : configs) {
            String configName = config.getName();
            if (StringUtils.isBlank(configName)) {
                continue;
            }

            try {
                Pair<Long, Long> configGroupAndSubGroup = getConfigurationGroupAndSubGroupByName(configName);
                if (configGroupAndSubGroup.first() != 1 && configGroupAndSubGroup.second() != 1) {
                    config.setGroupId(configGroupAndSubGroup.first());
                    config.setSubGroupId(configGroupAndSubGroup.second());
                    configDao.persist(config);
                }
            } catch (Exception e) {
                LOG.error("Failed to update group for configuration " + configName + " due to " + e.getMessage(), e);
            }
        }
        LOG.debug("Successfully updated configuration groups.");
    }

    private Pair<Long, Long> getConfigurationGroupAndSubGroupByName(String configName) {
        Long subGroupId = 1L;
        Long groupId = 1L;
        if (StringUtils.isNotBlank(configName)) {
            // Get words from the dot notation in the configuration
            String[] nameWords = configName.split("\\.");
            if (nameWords.length > 0) {
                for (int index = 0; index < nameWords.length; index++) {
                    ConfigurationSubGroupVO configSubGroup = configSubGroupDao.findByName(nameWords[index]);

                    if (configSubGroup == null) {
                        configSubGroup = configSubGroupDao.findByKeyword(nameWords[index]);
                    }

                    if (configSubGroup != null) {
                        subGroupId = configSubGroup.getId();
                        groupId = configSubGroup.getGroupId();
                        break;
                    }
                }
            }
        }

        return new Pair<>(groupId, subGroupId);
    }
}
