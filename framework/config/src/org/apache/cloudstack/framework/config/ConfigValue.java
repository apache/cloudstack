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

import org.apache.cloudstack.config.Configuration;

import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 *  This is a match set to ConfigKey.
 *
 * TODO: When we create a framework project for configuration, this should be
 * moved there.
 */
public class ConfigValue<T> {

    ConfigKey<T> _config;
    EntityManager _entityMgr;
    Number _multiplier;
    T _value;

    public ConfigValue(EntityManager entityMgr, ConfigKey<T> config) {
        _entityMgr = entityMgr;
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
        if (_value == null || _config.isDynamic()) {
            Configuration vo = _entityMgr.findById(Configuration.class, _config.key());
            String value = vo != null ? vo.getValue() : _config.defaultValue();

            Class<T> type = _config.type();
            if (type.isAssignableFrom(Boolean.class)) {
                _value = (T)Boolean.valueOf(value);
            } else if (type.isAssignableFrom(Integer.class)) {
                _value = (T)new Integer((Integer.parseInt(value) * _multiplier.intValue()));
            } else if (type.isAssignableFrom(Long.class)) {
                _value = (T)new Long(Long.parseLong(value) * _multiplier.longValue());
            } else if (type.isAssignableFrom(Short.class)) {
                _value = (T)new Short(Short.parseShort(value));
            } else if (type.isAssignableFrom(String.class)) {
                _value = (T)value;
            } else if (type.isAssignableFrom(Float.class)) {
                _value = (T)new Float(Float.parseFloat(value) * _multiplier.floatValue());
            } else {
                throw new CloudRuntimeException("Unsupported data type for config values: " + type);
            }
        }

        return _value;
    }
}
