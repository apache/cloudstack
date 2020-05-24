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
package org.apache.cloudstack.network.lb;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.cloud.network.rules.LoadBalancerConfig.Scope;
import com.cloud.utils.Pair;

public enum LoadBalancerConfigKey {

    HAproxyStatsEnable(Category.Stats, "haproxy.stats.enable", "HAProxy stats enable", Boolean.class, "true", "Enable or Disable HAProxy stats. To access the dashboard, please add firewall rule", Scope.Network, Scope.Vpc),

    HAproxyStatsUri(Category.Stats, "haproxy.stats.uri", "HAProxy stats URI", String.class, "/admin?stats", "URI of HAProxy stats, default is '/admin?stats'", Scope.Network, Scope.Vpc),

    HAproxyStatsAuth(Category.Stats, "haproxy.stats.auth", "HAProxy stats auth", String.class, "admin1:AdMiN123", "HAproxy stats username and password, default is 'admin1:AdMiN123'", Scope.Network, Scope.Vpc),

    LbHttp(Category.LoadBalancer, "lb.http", "LB http", Boolean.class, "false", "If LB is http, default is 'true' for port 80 and 'false' for others'", Scope.LoadBalancerRule),

    LbHttpKeepalive(Category.LoadBalancer, "lb.http.keepalive", "LB http keepalive enabled/disabled", Boolean.class, "false", "If LB http is enabled, default is 'false'", Scope.LoadBalancerRule),

    LbMaxConn(Category.LoadBalancer, "lb.max.conn", "LB max connection", Long.class, "", "LB max connection, default is ''", Scope.LoadBalancerRule),

    LbFullConn(Category.LoadBalancer, "lb.full.conn", "LB full connection", Long.class, "", "LB full connection, default is 'maxconn/10'", Scope.LoadBalancerRule);

    public static enum Category {
        General, Advanced, Stats, LoadBalancer
    }

    private final Category _category;
    private final Scope[] _scope;
    private final String _key;
    private final String _displayText;
    private final String _description;
    private final Class<?> _type;
    private final String _defaultValue;

    private LoadBalancerConfigKey(Category category, String key, String displayText, Class<?> type, String defaultValue, String description, Scope... scope) {
        _category = category;
        _scope = scope;
        _key = key;
        _displayText = displayText;
        _type = type;
        _defaultValue = defaultValue;
        _description = description;
    }

    public Category category() {
        return _category;
    }

    public Class<?> type() {
        return _type;
    }

    public String key() {
        return _key;
    }

    public String displayText() {
        return _displayText;
    }

    public String defaultValue() {
        return _defaultValue;
    }

    public String description() {
        return _description;
    }

    public Scope[] scope() {
        return _scope;
    }

    @Override
    public String toString() {
        return _key;
    }

    private static final HashMap<Scope, Map<String, LoadBalancerConfigKey>> Configs = new HashMap<Scope, Map<String, LoadBalancerConfigKey>>();
    static {
        Configs.put(Scope.Network, new LinkedHashMap<String, LoadBalancerConfigKey>());
        Configs.put(Scope.Vpc, new LinkedHashMap<String, LoadBalancerConfigKey>());
        Configs.put(Scope.LoadBalancerRule, new LinkedHashMap<String, LoadBalancerConfigKey>());
        for (LoadBalancerConfigKey c : LoadBalancerConfigKey.values()) {
            Scope[] scopes = c.scope();
            for (Scope scope : scopes) {
                Map<String, LoadBalancerConfigKey> currentConfigs = Configs.get(scope);
                currentConfigs.put(c.key(), c);
                Configs.put(scope, currentConfigs);
            }
        }
    }

    public static Map<String, LoadBalancerConfigKey> getConfigsByScope(Scope scope) {
        return Configs.get(scope);
    }

    public static LoadBalancerConfigKey getConfigsByScopeAndName(Scope scope, String name) {
        Map<String, LoadBalancerConfigKey> configs = Configs.get(scope);
        if (configs.keySet().contains(name)) {
            return configs.get(name);
        }
        return null;
    }

    public static Scope getScopeFromString(String scope) {
        if (scope == null) {
            return null;
        }
        for (Scope offScope : Scope.values()) {
            if (offScope.name().equalsIgnoreCase(scope)) {
                return offScope;
            }
        }
        return null;
    }

    public static Pair<LoadBalancerConfigKey, String> validate(Scope scope, String key, String value) {
        Map<String, LoadBalancerConfigKey> configs = Configs.get(scope);
        if (configs == null) {
            return new Pair<LoadBalancerConfigKey, String>(null, "Invalid scope " + scope);
        }
        LoadBalancerConfigKey config = null;
        for (LoadBalancerConfigKey c : configs.values()) {
            if (c.key().equals(key)) {
                config = c;
                break;
            }
        }
        if (config == null) {
            return new Pair<LoadBalancerConfigKey, String>(null, "Invalid key " + key);
        }
        if (value == null) {
            return new Pair<LoadBalancerConfigKey, String>(null, "Invalid null value for parameter " + key);
        }
        Class<?> type = config.type();
        String errMsg = null;
        try {
            if (type.equals(Integer.class)) {
                errMsg = "Please enter a valid integer value for parameter " + key;
                Integer.parseInt(value);
            } else if (type.equals(Float.class)) {
                errMsg = "Please enter a valid float value for parameter " + key;
                Float.parseFloat(value);
            } else if (type.equals(Long.class)) {
                errMsg = "Please enter a valid long value for parameter " + key;
                Long.parseLong(value);
            }
        } catch (final Exception e) {
            // catching generic exception as some throws NullPointerException and some throws NumberFormatExcpeion
            return new Pair<LoadBalancerConfigKey, String>(null, errMsg);
        }
        if (type.equals(Boolean.class)) {
            if (!(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))) {
                return new Pair<LoadBalancerConfigKey, String>(null, "Please enter either 'true' or 'false' for parameter " + key);
            }
        }
        return new Pair<LoadBalancerConfigKey, String>(config, null);
    }
}
