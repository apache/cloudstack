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
package org.apache.cloudstack.framework.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.ConfigKey.Scope;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.utils.component.ConfigInjector;
import com.cloud.utils.component.SystemIntegrityChecker;
import com.cloud.utils.db.EntityManager;
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
 *
 */
class ConfigDepotImpl implements ConfigDepot, ConfigDepotAdmin, SystemIntegrityChecker, ConfigInjector {
    @Inject
    EntityManager      _entityMgr;
    @Inject
    ConfigurationDao   _configDao;
    @Inject
    List<Configurable> _configurables;
    @Inject
    List<ScopedConfigStorage> _scopedStorage;

    HashMap<String, ConfigKey<?>> _allKeys = new HashMap<String, ConfigKey<?>>(1007);

    public ConfigDepotImpl() {
    }

    @Override
    public <T> ConfigValue<T> get(ConfigKey<T> config) {
        if (config.scope() == Scope.Global) {
            return new ConfigValue<T>(_entityMgr, config);
        } else {
            for (ScopedConfigStorage storage : _scopedStorage) {
                if (storage.getScope() == config.scope()) {
                    return new ConfigValue<T>(_entityMgr, config, storage);
                }
            }
            throw new CloudRuntimeException("Unable to find config storage for this scope: " + config.scope());
        }
    }

    @Override
    public void populateConfigurations() {
        Date date = new Date();
        for (Configurable configurable : _configurables) {
            for (ConfigKey<?> key : configurable.getConfigKeys()) {
                ConfigurationVO vo = _configDao.findById(key.key());
                if (vo == null) {
                    vo = new ConfigurationVO(configurable.getConfigComponentName(), key);
                    vo.setUpdated(date);
                    _configDao.persist(vo);
                } else {
                    if (vo.isDynamic() != key.isDynamic() ||
                        !vo.getDescription().equals(key.description()) ||
                        ((vo.getDefaultValue() != null && key.defaultValue() == null) ||
                         (vo.getDefaultValue() == null && key.defaultValue() != null) ||
                        !vo.getDefaultValue().equals(key.defaultValue()))) {
                        vo.setDynamic(key.isDynamic());
                        vo.setDescription(key.description());
                        vo.setDefaultValue(key.defaultValue());
                        vo.setUpdated(date);
                        _configDao.persist(vo);
                    }
                }
            }
        }
    }

    @Override
    public List<String> getComponentsInDepot() {
        return new ArrayList<String>();
    }

    @Override
    public void check() {
        for (Configurable configurable : _configurables) {
            for (ConfigKey<?> key : configurable.getConfigKeys()) {
                if (_allKeys.containsKey(key.key())) {
                    throw new CloudRuntimeException("Configurable " + configurable.getConfigComponentName() + " is adding a key that has been added before: " + key.toString());
                }
                _allKeys.put(key.key(), key);
            }
        }
    }

    @Override
    public void inject(Field field, Object obj, String key) {
        ConfigKey<?> configKey = _allKeys.get(key);
        try {
            field.set(obj, get(configKey));
        } catch (IllegalArgumentException e) {
            throw new CloudRuntimeException("Unable to inject configuration due to ", e);
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException("Unable to inject configuration due to ", e);
        }
    }

    @Override
    public ConfigValue<?> get(String name) {
        ConfigKey<?> configKey = _allKeys.get(name);
        if (configKey == null) {
            throw new CloudRuntimeException("Unable to find a registered config key for " + name);
        }
        return get(configKey);
    }
}
