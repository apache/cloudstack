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

import java.sql.Date;

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
    T _value;
    ScopedConfigStorage _storage;

    public ConfigValue(EntityManager entityMgr, ConfigKey<T> config) {
        _entityMgr = entityMgr;
        _config = config;
    }

    protected ConfigValue(EntityManager entityMgr, ConfigKey<T> key, ScopedConfigStorage storage) {
        this(entityMgr, key);
        _storage = storage;
    }

    public ConfigKey<T> getConfigKey() {
        return _config;
    }

    public T value() {
        if (_value == null || _config.isDynamic()) {
            Configuration vo = _entityMgr.findById(Configuration.class, _config.key());
            _value = valueOf(vo != null ? vo.getValue() : _config.defaultValue());
        }

        return _value;
    }

    public T valueIn(long id) {
        String value = _storage.getConfigValue(id, _config);
        if (value == null) {
            return value();
        } else {
            return valueOf(value);
        }
    }

    @SuppressWarnings("unchecked")
    protected T valueOf(String value) {
        Class<T> type = _config.type();
        if (type.isAssignableFrom(Boolean.class)) {
            return (T)Boolean.valueOf(value);
        } else if (type.isAssignableFrom(Integer.class)) {
            return (T)new Integer((Integer.parseInt(value) * ((Number)_config.multiplier()).intValue()));
        } else if (type.isAssignableFrom(Long.class)) {
            return (T)new Long(Long.parseLong(value) * ((Number)_config.multiplier()).longValue());
        } else if (type.isAssignableFrom(Short.class)) {
            return (T)new Short(Short.parseShort(value));
        } else if (type.isAssignableFrom(String.class)) {
            return (T)value;
        } else if (type.isAssignableFrom(Float.class)) {
            return (T)new Float(Float.parseFloat(value) * ((Number)_config.multiplier()).floatValue());
        } else if (type.isAssignableFrom(Double.class)) {
            return (T)new Double(Double.parseDouble(value));
        } else if (type.isAssignableFrom(String.class)) {
            return (T)value;
        } else if (type.isAssignableFrom(Date.class)) {
            return (T)Date.valueOf(value);
        } else if (type.isAssignableFrom(Character.class)) {
            return (T)new Character(value.charAt(0));
        } else {
            throw new CloudRuntimeException("Unsupported data type for config values: " + type);
        }
    }
}
