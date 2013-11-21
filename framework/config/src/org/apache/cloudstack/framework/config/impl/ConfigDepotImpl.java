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
package org.apache.cloudstack.framework.config.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigDepotAdmin;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.ScopedConfigStorage;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * ConfigDepotImpl implements the ConfigDepot and ConfigDepotAdmin interface.
 * Its functionalities include:
 *   - Control how dynamic config values are cached and refreshed.
 *   - Control how scoped config values are stored.
 *   - Gather all of the Configurable interfaces and insert their config
 *     variables into the config table.
 *   - Hide the data source where configs are stored and retrieved.
 *
 * When dealing with this class, we must be very careful on cluster situations.
 *
 * TODO:
 *   - Move the rest of the changes to the config table to here.
 *   - Add the code to mark the rows in configuration table without
 *     the corresponding keys to be null.
 *   - Move all of the configurations to using ConfigDepot
 *   - Completely eliminate Config.java
 *   - Figure out the correct categories.
 *   - Add a scope for management server, where if the scope is management server
 *     then the override is retrieved from a properties file.  Imagine adding a
 *     new management server node and it is much more capable system than previous
 *     management servers, you want the adjustments to thread pools etc to be
 *     very different than other management serves.
 *   - Add validation methods to ConfigKey<?>.  If a validation class is declared
 *     when constructing a ConfigKey then configuration server should use the
 *     validation class to validate the value the admin input for the key.
 */
public class ConfigDepotImpl implements ConfigDepot, ConfigDepotAdmin {
    private final static Logger s_logger = Logger.getLogger(ConfigDepotImpl.class);
    @Inject
    ConfigurationDao _configDao;
    List<Configurable> _configurables;
    List<ScopedConfigStorage> _scopedStorages;
    Set<Configurable> _configured = Collections.synchronizedSet(new HashSet<Configurable>());

    HashMap<String, Pair<String, ConfigKey<?>>> _allKeys = new HashMap<String, Pair<String, ConfigKey<?>>>(1007);

    HashMap<ConfigKey.Scope, List<ConfigKey<?>>> _scopeLevelConfigsMap = new HashMap<ConfigKey.Scope, List<ConfigKey<?>>>();

    public ConfigDepotImpl() {
        ConfigKey.init(this);
        _scopeLevelConfigsMap.put(ConfigKey.Scope.Zone, new ArrayList<ConfigKey<?>>());
        _scopeLevelConfigsMap.put(ConfigKey.Scope.Cluster, new ArrayList<ConfigKey<?>>());
        _scopeLevelConfigsMap.put(ConfigKey.Scope.StoragePool, new ArrayList<ConfigKey<?>>());
        _scopeLevelConfigsMap.put(ConfigKey.Scope.Account, new ArrayList<ConfigKey<?>>());
    }

    @Override
    public ConfigKey<?> get(String key) {
        Pair<String, ConfigKey<?>> value = _allKeys.get(key);
        return value != null ? value.second() : null;
    }

    @PostConstruct
    @Override
    public void populateConfigurations() {
        Date date = new Date();
        for (Configurable configurable : _configurables) {
            populateConfiguration(date, configurable);
        }
    }

    protected void populateConfiguration(Date date, Configurable configurable) {
        if (_configured.contains(configurable))
            return;

        s_logger.debug("Retrieving keys from " + configurable.getClass().getSimpleName());

        for (ConfigKey<?> key : configurable.getConfigKeys()) {
            Pair<String, ConfigKey<?>> previous = _allKeys.get(key.key());
            if (previous != null && !previous.first().equals(configurable.getConfigComponentName())) {
                throw new CloudRuntimeException("Configurable " + configurable.getConfigComponentName() + " is adding a key that has been added before by " +
                    previous.first() + ": " + key.toString());
            }
            _allKeys.put(key.key(), new Pair<String, ConfigKey<?>>(configurable.getConfigComponentName(), key));

            ConfigurationVO vo = _configDao.findById(key.key());
            if (vo == null) {
                vo = new ConfigurationVO(configurable.getConfigComponentName(), key);
                vo.setUpdated(date);
                _configDao.persist(vo);
            } else {
                if (vo.isDynamic() != key.isDynamic() || !ObjectUtils.equals(vo.getDescription(), key.description()) ||
                    !ObjectUtils.equals(vo.getDefaultValue(), key.defaultValue())) {
                    vo.setDynamic(key.isDynamic());
                    vo.setDescription(key.description());
                    vo.setDefaultValue(key.defaultValue());
                    vo.setUpdated(date);
                    _configDao.persist(vo);
                }
            }
            if (key.scope() != ConfigKey.Scope.Global) {
                List<ConfigKey<?>> currentConfigs = _scopeLevelConfigsMap.get(key.scope());
                currentConfigs.add(key);
            }
        }

        _configured.add(configurable);
    }

    @Override
    public void populateConfiguration(Configurable configurable) {
        populateConfiguration(new Date(), configurable);
    }

    @Override
    public List<String> getComponentsInDepot() {
        return new ArrayList<String>();
    }

    public ConfigurationDao global() {
        return _configDao;
    }

    public ScopedConfigStorage scoped(ConfigKey<?> config) {
        for (ScopedConfigStorage storage : _scopedStorages) {
            if (storage.getScope() == config.scope()) {
                return storage;
            }
        }

        throw new CloudRuntimeException("Unable to find config storage for this scope: " + config.scope() + " for " + config.key());
    }

    public List<ScopedConfigStorage> getScopedStorages() {
        return _scopedStorages;
    }

    @Inject
    public void setScopedStorages(List<ScopedConfigStorage> scopedStorages) {
        this._scopedStorages = scopedStorages;
    }

    public List<Configurable> getConfigurables() {
        return _configurables;
    }

    @Inject
    public void setConfigurables(List<Configurable> configurables) {
        this._configurables = configurables;
    }

    @Override
    public List<ConfigKey<?>> getConfigListByScope(String scope) {
        return _scopeLevelConfigsMap.get(ConfigKey.Scope.valueOf(scope));
    }

}
