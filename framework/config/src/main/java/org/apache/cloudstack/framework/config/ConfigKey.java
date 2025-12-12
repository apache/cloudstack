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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * ConfigKey supplants the original Config.java.  It is just a class
 * declaration where others can declare their config variables.
 *
 */
public class ConfigKey<T> {
    private static final Logger logger = LogManager.getLogger(ConfigKey.class);

    public static final String CATEGORY_ADVANCED = "Advanced";
    public static final String CATEGORY_ALERT = "Alert";
    public static final String CATEGORY_NETWORK = "Network";
    public static final String CATEGORY_SYSTEM = "System";

    // Configuration Groups to be used to define group for a config key
    // Group name, description, precedence
    public static final Ternary<String, String, Long> GROUP_MISCELLANEOUS = new Ternary<>("Miscellaneous", "Miscellaneous configuration", 999L);
    public static final Ternary<String, String, Long> GROUP_ACCESS = new Ternary<>("Access", "Identity and Access management configuration", 1L);
    public static final Ternary<String, String, Long> GROUP_COMPUTE = new Ternary<>("Compute", "Compute configuration", 2L);
    public static final Ternary<String, String, Long> GROUP_STORAGE = new Ternary<>("Storage", "Storage configuration", 3L);
    public static final Ternary<String, String, Long> GROUP_NETWORK = new Ternary<>("Network", "Network configuration", 4L);
    public static final Ternary<String, String, Long> GROUP_HYPERVISOR = new Ternary<>("Hypervisor", "Hypervisor specific configuration", 5L);
    public static final Ternary<String, String, Long> GROUP_MANAGEMENT_SERVER = new Ternary<>("Management Server", "Management Server configuration", 6L);
    public static final Ternary<String, String, Long> GROUP_SYSTEM_VMS = new Ternary<>("System VMs", "System VMs related configuration", 7L);
    public static final Ternary<String, String, Long> GROUP_INFRASTRUCTURE = new Ternary<>("Infrastructure", "Infrastructure configuration", 8L);
    public static final Ternary<String, String, Long> GROUP_USAGE_SERVER = new Ternary<>("Usage Server", "Usage Server related configuration", 9L);

    // Configuration Subgroups to be used to define subgroup for a config key
    // Subgroup name, description, precedence
    public static final Pair<String, Long> SUBGROUP_OTHERS = new Pair<>("Others", 999L);
    public static final Pair<String, Long> SUBGROUP_ACCOUNT = new Pair<>("Account", 1L);
    public static final Pair<String, Long> SUBGROUP_DOMAIN = new Pair<>("Domain", 2L);
    public static final Pair<String, Long> SUBGROUP_PROJECT = new Pair<>("Project", 3L);
    public static final Pair<String, Long> SUBGROUP_LDAP = new Pair<>("LDAP", 4L);
    public static final Pair<String, Long> SUBGROUP_SAML = new Pair<>("SAML", 5L);
    public static final Pair<String, Long> SUBGROUP_VIRTUAL_MACHINE = new Pair<>("Virtual Machine", 1L);
    public static final Pair<String, Long> SUBGROUP_KUBERNETES = new Pair<>("Kubernetes", 2L);
    public static final Pair<String, Long> SUBGROUP_HIGH_AVAILABILITY = new Pair<>("High Availability", 3L);
    public static final Pair<String, Long> SUBGROUP_IMAGES = new Pair<>("Images", 1L);
    public static final Pair<String, Long> SUBGROUP_VOLUME = new Pair<>("Volume", 2L);
    public static final Pair<String, Long> SUBGROUP_SNAPSHOT = new Pair<>("Snapshot", 3L);
    public static final Pair<String, Long> SUBGROUP_VM_SNAPSHOT = new Pair<>("VM Snapshot", 4L);
    public static final Pair<String, Long> SUBGROUP_NETWORK = new Pair<>("Network", 1L);
    public static final Pair<String, Long> SUBGROUP_DHCP = new Pair<>("DHCP", 2L);
    public static final Pair<String, Long> SUBGROUP_VPC = new Pair<>("VPC", 3L);
    public static final Pair<String, Long> SUBGROUP_LOADBALANCER = new Pair<>("LoadBalancer", 4L);
    public static final Pair<String, Long> SUBGROUP_API = new Pair<>("API", 1L);
    public static final Pair<String, Long> SUBGROUP_ALERTS = new Pair<>("Alerts", 2L);
    public static final Pair<String, Long> SUBGROUP_EVENTS = new Pair<>("Events", 3L);
    public static final Pair<String, Long> SUBGROUP_SECURITY = new Pair<>("Security", 4L);
    public static final Pair<String, Long> SUBGROUP_USAGE = new Pair<>("Usage", 1L);
    public static final Pair<String, Long> SUBGROUP_LIMITS = new Pair<>("Limits", 6L);
    public static final Pair<String, Long> SUBGROUP_JOBS = new Pair<>("Jobs", 7L);
    public static final Pair<String, Long> SUBGROUP_AGENT = new Pair<>("Agent", 8L);
    public static final Pair<String, Long> SUBGROUP_HYPERVISOR = new Pair<>("Hypervisor", 1L);
    public static final Pair<String, Long> SUBGROUP_KVM = new Pair<>("KVM", 2L);
    public static final Pair<String, Long> SUBGROUP_VMWARE = new Pair<>("VMware", 3L);
    public static final Pair<String, Long> SUBGROUP_XENSERVER = new Pair<>("XenServer", 4L);
    public static final Pair<String, Long> SUBGROUP_OVM = new Pair<>("OVM", 5L);
    public static final Pair<String, Long> SUBGROUP_BAREMETAL = new Pair<>("Baremetal", 6L);
    public static final Pair<String, Long> SUBGROUP_CONSOLE_PROXY_VM = new Pair<>("ConsoleProxyVM", 1L);
    public static final Pair<String, Long> SUBGROUP_SEC_STORAGE_VM = new Pair<>("SecStorageVM", 2L);
    public static final Pair<String, Long> SUBGROUP_VIRTUAL_ROUTER = new Pair<>("VirtualRouter", 3L);
    public static final Pair<String, Long> SUBGROUP_DIAGNOSTICS = new Pair<>("Diagnostics", 4L);
    public static final Pair<String, Long> SUBGROUP_PRIMARY_STORAGE = new Pair<>("Primary Storage", 1L);
    public static final Pair<String, Long> SUBGROUP_SECONDARY_STORAGE = new Pair<>("Secondary Storage", 2L);
    public static final Pair<String, Long> SUBGROUP_BACKUP_AND_RECOVERY = new Pair<>("Backup & Recovery", 1L);
    public static final Pair<String, Long> SUBGROUP_CERTIFICATE_AUTHORITY = new Pair<>("Certificate Authority", 2L);
    public static final Pair<String, Long> SUBGROUP_QUOTA = new Pair<>("Quota", 3L);
    public static final Pair<String, Long> SUBGROUP_CLOUDIAN = new Pair<>("Cloudian", 4L);
    public static final Pair<String, Long> SUBGROUP_DRS = new Pair<>("DRS", 4L);

    public enum Scope {
        Global(null, 1),
        Zone(Global, 1 << 1),
        Cluster(Zone, 1 << 2),
        StoragePool(Cluster, 1 << 3),
        ManagementServer(Global, 1 << 4),
        ImageStore(Zone, 1 << 5),
        Domain(Global, 1 << 6),
        Account(Domain, 1 << 7);

        private final Scope parent;
        private final int bitValue;

        Scope(Scope parent, int bitValue) {
            this.parent = parent;
            this.bitValue = bitValue;
        }

        public Scope getParent() {
            return parent;
        }

        public int getBitValue() {
            return bitValue;
        }

        public boolean isDescendantOf(Scope other) {
            Scope parent = this.getParent();
            while (parent != null) {
                if (parent == other) {
                    return true;
                }
                parent = parent.getParent();
            }
            return false;
        }

        public static List<Scope> getAllDescendants(String str) {
            Scope s1 = Scope.valueOf(str);
            List<Scope> scopes = new ArrayList<>();
            for (Scope s : Scope.values()) {
                if (s.isDescendantOf(s1)) {
                    scopes.add(s);
                }
            }
            return scopes;
        }

        public static List<Scope> decode(int bitmask) {
            if (bitmask == 0) {
                return Collections.emptyList();
            }
            List<Scope> scopes = new ArrayList<>();
            for (Scope scope : Scope.values()) {
                if ((bitmask & scope.getBitValue()) != 0) {
                    scopes.add(scope);
                }
            }
            return scopes;
        }

        public static String decodeAsCsv(int bitmask) {
            if (bitmask == 0) {
                return null;
            }
            StringBuilder builder = new StringBuilder();
            for (Scope scope : Scope.values()) {
                if ((bitmask & scope.getBitValue()) != 0) {
                    builder.append(scope.name()).append(", ");
                }
            }
            if (builder.length() > 0) {
                builder.setLength(builder.length() - 2);
            }
            return builder.toString();
        }

        public static int getBitmask(Scope... scopes) {
            int bitmask = 0;
            for (Scope scope : scopes) {
                bitmask |= scope.getBitValue();
            }
            return bitmask;
        }
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

    public List<Scope> getScopes() {
        return scopes;
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
    private final List<Scope> scopes; // Parameter can be at different levels (Zone/cluster/pool/account), by default every parameter is at global
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

    public ConfigKey(String category, Class<T> type, String name, String defaultValue, String description, boolean isDynamic, List<Scope> scopes) {
        this(type, name, category, defaultValue, description, isDynamic, scopes, null);
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

    public ConfigKey(Class<T> type, String name, String category, String defaultValue, String description, boolean isDynamic, List<Scope> scopes, T multiplier) {
        this(type, name, category, defaultValue, description, isDynamic, scopes, multiplier, null, null, null, null, null, null);
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
        this(type, name, category, defaultValue, description, isDynamic, scope == null ? null : List.of(scope), multiplier,
                displayText, parent, group, subGroup, kind, options);
    }

    public ConfigKey(Class<T> type, String name, String category, String defaultValue, String description, boolean isDynamic, List<Scope> scopes, T multiplier,
                     String displayText, String parent, Ternary<String, String, Long> group, Pair<String, Long> subGroup, Kind kind, String options) {
        _category = category;
        _type = type;
        _name = name;
        _defaultValue = defaultValue;
        _description = description;
        _displayText = displayText;
        this.scopes = new ArrayList<>();
        if (scopes != null) {
            this.scopes.addAll(scopes);
        }
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

    protected T valueInGlobalOrAvailableParentScope(Scope scope, Long id) {
        if (scopes.size() <= 1) {
            return value();
        }
        Pair<Scope, Long> s = new Pair<>(scope, id);
        do {
            s = s_depot != null ? s_depot.getParentScope(s.first(), s.second()) : null;
            if (s != null && scopes.contains(s.first())) {
                return valueInScope(s.first(), s.second());
            }
        } while (s != null);
        logger.trace("Global value for config ({}): {}", _name, _value);
        return value();
    }

    public T valueInScope(Scope scope, Long id) {
        if (id == null) {
            return value();
        }
        String value = s_depot != null ? s_depot.getConfigStringValue(_name, scope, id) : null;
        if (value == null) {
            return valueInGlobalOrAvailableParentScope(scope, id);
        }
        logger.trace("Scope({}) value for config ({}): {}", scope, _name, _value);
        return valueOf(value);
    }

    protected Scope getPrimaryScope() {
        if (CollectionUtils.isNotEmpty(scopes)) {
            return scopes.get(0);
        }
        return null;
    }

    public T valueIn(Long id) {
        return valueInScope(getPrimaryScope(), id);
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
            return (T)Integer.valueOf(Integer.parseInt(value) * multiplier.intValue());
        } else if (type.isAssignableFrom(Long.class)) {
            return (T)Long.valueOf(Long.parseLong(value) * multiplier.longValue());
        } else if (type.isAssignableFrom(Short.class)) {
            return (T)Short.valueOf(Short.parseShort(value));
        } else if (type.isAssignableFrom(String.class)) {
            return (T)value;
        } else if (type.isAssignableFrom(Float.class)) {
            return (T)Float.valueOf(Float.parseFloat(value) * multiplier.floatValue());
        } else if (type.isAssignableFrom(Double.class)) {
            return (T)Double.valueOf(Double.parseDouble(value) * multiplier.doubleValue());
        } else if (type.isAssignableFrom(Date.class)) {
            return (T)Date.valueOf(value);
        } else if (type.isAssignableFrom(Character.class)) {
            return (T)Character.valueOf(value.charAt(0));
        } else {
            throw new CloudRuntimeException("Unsupported data type for config values: " + type);
        }
    }

    public boolean isGlobalOrEmptyScope() {
        return CollectionUtils.isEmpty(scopes) ||
                (scopes.size() == 1 && scopes.get(0) == Scope.Global);
    }

    public int getScopeBitmask() {
        int bitmask = 0;
        if (CollectionUtils.isEmpty(scopes)) {
            return bitmask;
        }
        for (Scope scope : scopes) {
            bitmask |= scope.getBitValue();
        }
        return bitmask;
    }

}
