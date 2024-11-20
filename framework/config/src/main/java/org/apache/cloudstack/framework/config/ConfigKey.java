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

import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;

import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * ConfigKey supplants the original Config.java.  It is just a class
 * declaration where others can declare their config variables.
 *
 */
public class ConfigKey<T> {

    public static final String CATEGORY_ADVANCED = "Advanced";
    public static final String CATEGORY_ALERT = "Alert";
    public static final String CATEGORY_NETWORK = "Network";

    public enum Scope {
        Global, Zone, Cluster, StoragePool, Account, ManagementServer, ImageStore, Domain
    }

    public enum Kind {
        CSV, Order, Select, WhitespaceSeparatedListWithOptions
    }

    private final String _category;

    public String category() {
        return _category;
    }

    public Class<T> type() {
        return _type;
    }

    public final String key() {
        return _name;
    }

    public String defaultValue() {
        return _defaultValue;
    }

    public String description() {
        return _description;
    }

    public String displayText() {
        return _displayText;
    }

    public Scope scope() {
        return _scope;
    }

    public boolean isDynamic() {
        return _isDynamic;
    }

    public Ternary<String, String, Long> group() {
        return _group;
    }

    public Pair<String, Long> subGroup() {
        return _subGroup;
    }

    public final String parent() {
        return _parent;
    }

    public final Kind kind() {
        return _kind;
    }

    public final String options() {
        return _options;
    }

    @Override
    public String toString() {
        return _name;
    }

    private final Class<T> _type;
    private final String _name;
    private final String _defaultValue;
    private final String _description;
    private final String _displayText;
    private final Scope _scope; // Parameter can be at different levels (Zone/cluster/pool/account), by default every parameter is at global
    private final boolean _isDynamic;
    private final String _parent;
    private final Ternary<String, String, Long> _group; // Group name, description with precedence
    private final Pair<String, Long> _subGroup; // SubGroup name with precedence
    private final Kind _kind; // Kind such as order, csv, etc
    private final String _options; // list of possible options in case of order, list, etc
    private final T _multiplier;
    T _value = null;

    static ConfigDepotImpl s_depot = null;

    static public void init(ConfigDepotImpl depot) {
        s_depot = depot;
    }

    public ConfigKey(String category, Class<T> type, String name, String defaultValue, String description, boolean isDynamic, Scope scope) {
        this(type, name, category, defaultValue, description, isDynamic, scope, null);
    }

    public ConfigKey(String category, Class<T> type, String name, String defaultValue, String description, boolean isDynamic, Scope scope, String parent) {
        this(type, name, category, defaultValue, description, isDynamic, scope, null, null, parent, null, null, null, null);
    }

    public ConfigKey(String category, Class<T> type, String name, String defaultValue, String description, boolean isDynamic) {
        this(type, name, category, defaultValue, description, isDynamic, Scope.Global, null);
    }

    public ConfigKey(String category, Class<T> type, String name, String defaultValue, String description, boolean isDynamic, Kind kind, String options) {
        this(type, name, category, defaultValue, description, isDynamic, Scope.Global, null, null, null, null, null, kind, options);
    }

    public ConfigKey(String category, Class<T> type, String name, String defaultValue, String description, boolean isDynamic, String parent) {
        this(type, name, category, defaultValue, description, isDynamic, Scope.Global, null, null, parent, null, null, null, null);
    }

    public ConfigKey(Class<T> type, String name, String category, String defaultValue, String description, boolean isDynamic, Scope scope, T multiplier) {
        this(type, name, category, defaultValue, description, isDynamic, scope, multiplier, null, null, null, null, null, null);
    }

    public ConfigKey(Class<T> type, String name, String category, String defaultValue, String description, boolean isDynamic, Scope scope, T multiplier, String parent) {
        this(type, name, category, defaultValue, description, isDynamic, scope, multiplier, null, parent, null, null, null, null);
    }

    public ConfigKey(Class<T> type, String name, String category, String defaultValue, String description, boolean isDynamic, Scope scope, T multiplier,
                     String displayText, String parent, Ternary<String, String, Long> group, Pair<String, Long> subGroup) {
        this(type, name, category, defaultValue, description, isDynamic, scope, multiplier, displayText, parent, group, subGroup, null, null);
    }

    public ConfigKey(Class<T> type, String name, String category, String defaultValue, String description, boolean isDynamic, Scope scope, T multiplier,
                     String displayText, String parent, Ternary<String, String, Long> group, Pair<String, Long> subGroup, Kind kind, String options) {
        _category = category;
        _type = type;
        _name = name;
        _defaultValue = defaultValue;
        _description = description;
        _displayText = displayText;
        _scope = scope;
        _isDynamic = isDynamic;
        _multiplier = multiplier;
        _parent = parent;
        _group = group;
        _subGroup = subGroup;
        _kind = kind;
        _options = options;
    }

    @Deprecated
    public ConfigKey(Class<T> type, String name, String category, String defaultValue, String description, boolean isDynamic) {
        this(type, name, category, defaultValue, description, isDynamic, Scope.Global, null);
    }

    public ConfigKey(Class<T> type, String name, String category, String defaultValue, String description, boolean isDynamic, String parent) {
        this(type, name, category, defaultValue, description, isDynamic, Scope.Global, null, null, parent, null, null);
    }

    public T multiplier() {
        return _multiplier;
    }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConfigKey) {
            ConfigKey<?> that = (ConfigKey<?>)obj;
            return this._name.equals(that._name);
        }
        return false;
    }

    public boolean isSameKeyAs(Object obj) {
        if(this.equals(obj)) {
            return true;
        } else if (obj instanceof String) {
            String key = (String)obj;
            return key.equals(_name);
        }

        throw new CloudRuntimeException("Comparing ConfigKey to " + obj.toString());
    }

    public T value() {
        if (_value == null || isDynamic()) {
            String value = s_depot != null ? s_depot.getConfigStringValue(_name, Scope.Global, null) : null;
            _value = valueOf((value == null) ? defaultValue() : value);
        }

        return _value;
    }

    protected T valueInScope(Scope scope, Long id) {
        if (id == null) {
            return value();
        }

        String value = s_depot != null ? s_depot.getConfigStringValue(_name, scope, id) : null;
        if (value == null) {
            return value();
        }
        return valueOf(value);
    }

    public T valueIn(Long id) {
        return valueInScope(_scope, id);
    }

    public T valueInDomain(Long domainId) {
        return valueInScope(Scope.Domain, domainId);
    }

    @SuppressWarnings("unchecked")
    protected T valueOf(String value) {
        if (value == null) {
            return null;
        }
        Number multiplier = 1;
        if (multiplier() != null) {
            multiplier = (Number)multiplier();
        }
        Class<T> type = type();
        if (type.isAssignableFrom(Boolean.class)) {
            return (T)Boolean.valueOf(value);
        } else if (type.isAssignableFrom(Integer.class)) {
            return (T)new Integer(Integer.parseInt(value) * multiplier.intValue());
        } else if (type.isAssignableFrom(Long.class)) {
            return (T)new Long(Long.parseLong(value) * multiplier.longValue());
        } else if (type.isAssignableFrom(Short.class)) {
            return (T)new Short(Short.parseShort(value));
        } else if (type.isAssignableFrom(String.class)) {
            return (T)value;
        } else if (type.isAssignableFrom(Float.class)) {
            return (T)new Float(Float.parseFloat(value) * multiplier.floatValue());
        } else if (type.isAssignableFrom(Double.class)) {
            return (T)new Double(Double.parseDouble(value) * multiplier.doubleValue());
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
