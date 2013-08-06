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

import com.cloud.org.Grouping;

/**
 * ConfigKey supplants the original Config.java.  It is just a class
 * declaration where others can declare their config variables.
 * 
 */
public class ConfigKey<T> {
    
    private final String _category;

    public String category() {
        return _category;
    }

    public Class<T> type() {
        return _type;
    }

    public String key() {
        return _name;
    }

    public String defaultValue() {
        return _defaultValue;
    }

    public String description() {
        return _description;
    }

    public Class<? extends Grouping> scope() {
        return _scope;
    }

    public boolean isDynamic() {
        return _isDynamic;
    }

    @Override
    public String toString() {
        return _name;
    }

    private final Class<T> _type;
    private final String _name;
    private final String _defaultValue;
    private final String _description;
    private final Class<? extends Grouping> _scope; // Parameter can be at different levels (Zone/cluster/pool/account), by default every parameter is at global
    private final boolean _isDynamic;

    public ConfigKey(Class<T> type, String name, String category, String defaultValue, String description, boolean isDynamic,
            Class<? extends Grouping> scope) {
        _category = category;
        _type = type;
        _name = name;
        _defaultValue = defaultValue;
        _description = description;
        _scope = scope;
        _isDynamic = isDynamic;
    }

    public ConfigKey(Class<T> type, String name, String category, String defaultValue, String description, boolean isDynamic) {
        this(type, name, category, defaultValue, description, isDynamic, null);
    }
}
