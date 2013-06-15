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

/**
 * ConfigKey supplants the original Config.java.  It is just a class
 * declaration where others can declare their config variables.
 * 
 * TODO: This class should be moved to a framework project where the gathering
 *       of these configuration keys should be done by a config server.  I
 *       don't have time yet to do this.  Ask me about it if you want to work
 *       in this area.  Right now, we'll just work with the actual names.
 */
public class ConfigKey<T> {
    
    private final String _category;

    public String category() {
        return _category;
    }

    public Class<?> component() {
        return _componentClass;
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

    public String range() {
        return _range;
    }

    public String scope() {
        return _scope;
    }

    public boolean isDynamic() {
        return _isDynamic;
    }

    @Override
    public String toString() {
        return _name;
    }

    private final Class<?> _componentClass;
    private final Class<T> _type;
    private final String _name;
    private final String _defaultValue;
    private final String _description;
    private final String _range;
    private final String _scope; // Parameter can be at different levels (Zone/cluster/pool/account), by default every parameter is at global
    private final boolean _isDynamic;

    public ConfigKey(Class<T> type, String name, String category, Class<?> componentClass, String defaultValue, String description, boolean isDynamic, String range,
            String scope) {
        _category = category;
        _componentClass = componentClass;
        _type = type;
        _name = name;
        _defaultValue = defaultValue;
        _description = description;
        _range = range;
        _scope = scope;
        _isDynamic = isDynamic;
    }

    public ConfigKey(Class<T> type, String name, String category, Class<?> componentClass, String defaultValue, String description, boolean isDynamic, String range) {
        this(type, name, category, componentClass, defaultValue, description, isDynamic, range, null);
    }
}
