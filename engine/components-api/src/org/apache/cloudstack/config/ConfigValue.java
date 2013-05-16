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
package org.apache.cloudstack.config;

import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 *  This is a match set to ConfigKey.
 *
 * TODO: When we create a framework project for configuration, this should be
 * moved there.
 */
public class ConfigValue<T> {

    ConfigKey<T> _config;
    ConfigurationDao _dao;
    Number _multiplier;

    public ConfigValue(ConfigurationDao dao, ConfigKey<T> config) {
        _dao = dao;
        _config = config;
        _multiplier = 1;
    }

    public ConfigKey<T> getConfigKey() {
        return _config;
    }

    public ConfigValue<T> setMultiplier(Number multiplier) {  // Convience method
        _multiplier = multiplier;
        return this;
    }

    @SuppressWarnings("unchecked")
    public T value() {
        ConfigurationVO vo = _dao.findByName(_config.key());
        String value = vo != null ? vo.getValue() : _config.defaultValue();
        
        Class<T> type = _config.type();
        if (type.isAssignableFrom(Boolean.class)) {
            return (T)Boolean.valueOf(value);
        } else if (type.isAssignableFrom(Integer.class)) {
            return (T)new Integer((Integer.parseInt(value) * _multiplier.intValue()));
        } else if (type.isAssignableFrom(Long.class)) {
            return (T)new Long(Long.parseLong(value) * _multiplier.longValue());
        } else if (type.isAssignableFrom(Short.class)) {
            return (T)new Short(Short.parseShort(value));
        } else if (type.isAssignableFrom(String.class)) {
            return (T)value;
        } else if (type.isAssignableFrom(Float.class)) {
            return (T)new Float(Float.parseFloat(value) * _multiplier.floatValue());
        }

        throw new CloudRuntimeException("Unsupported data type for config values: " + type);
    }
}
