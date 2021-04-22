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
package org.apache.cloudstack.pki;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;

/**
 * @author Khosrow Moossavi
 * @since 4.12.0.0
 */
public enum PkiConfig {
    CertificateBrand("Network", String.class, "pki.engine.certificate.brand", "CloudStack", "Brand name to be used in Certificate's common name"),
    CertificateCommonName("Network", String.class, "pki.engine.certificate.common.name", "__BRAND__ VPN __DOMAIN__ CA",
            "Certificate's common name template (brand will be filled from 'pki.engine.certificate.brand', domain will be provided on the fly"),
    VaultEnabled("Network", Boolean.class, "pki.engine.vault.enabled", "false", "Enable Vault as the backend PKI engine"),
    VaultUrl("Network", String.class, "pki.engine.vault.url", "", "Full URL of Vault endpoint (e.g. http://127.0.0.1:8200)"),
    VaultToken("Network", String.class, "pki.engine.vault.token", "", "Token to access Vault"),
    VaultAppRoleId("Network", String.class, "pki.engine.vault.token.role.id", "", "App Role id to be used to fetch token to access Vault"),
    VaultAppSecretId("Network", String.class, "pki.engine.vault.token.secret.id", "", "Secret id to be used to fetch token to access Vault"),
    VaultPkiTtl("Network", String.class, "pki.engine.vault.ttl", "87600h", "Vault PKI TTL (e.g. 87600h)"),
    VaultCATtl("Network", String.class, "pki.engine.vault.cca.ttl", "87600h", "Vault PKI root CA TTL (e.g. 87600h)"),
    VaultRoleName("Network", String.class, "pki.engine.vault.role.name", "cloudstack-vpn", "Vault PKI role name"),
    VaultRoleTtl("Network", String.class, "pki.engine.vault.role.ttl", "43800h", "Vault PKI role TTL (e.g. 43800h)"),
    VaultMounthPath("Network", String.class, "pki.engine.vault.mount.path", "pki/cloudstack", "Vault PKI mount point prefix (must not end with trailing slash)");

    private final String _category;
    private final Class<?> _type;
    private final String _name;
    private final String _defaultValue;
    private final String _description;
    private final boolean _dynamic;
    private final ConfigKey.Scope _scope;

    private static final List<String> PkiEngineConfigKeys = new ArrayList<String>();

    static {
        Arrays.stream(PkiConfig.values()).forEach(c -> PkiEngineConfigKeys.add(c.key()));
    }

    private PkiConfig(String category, Class<?> type, String name, String defaultValue, String description) {
        _category = category;
        _type = type;
        _name = name;
        _defaultValue = defaultValue;
        _description = description;
        _dynamic = false;
        _scope = ConfigKey.Scope.Global;
    }

    public String getCategory() {
        return _category;
    }

    public Class<?> getType() {
        return _type;
    }

    public String getName() {
        return _name;
    }

    public String getDefaultValue() {
        return _defaultValue;
    }

    public String getDescription() {
        return _description;
    }

    public boolean isDynamic() {
        return _dynamic;
    }

    public ConfigKey.Scope getScope() {
        return _scope;
    }

    public String key() {
        return _name;
    }

    public static boolean doesKeyExist(String key) {
        return PkiEngineConfigKeys.contains(key);
    }

    public static ConfigKey<?>[] asConfigKeys() {
        return Arrays.stream(PkiConfig.values())
                .map(config -> asConfigKey(config))
                .toArray(ConfigKey[]::new);
    }

    public static ConfigKey<?> asConfigKey(PkiConfig config) {
        return new ConfigKey<>(
                config.getCategory(),
                config.getType(),
                config.getName(),
                config.getDefaultValue(),
                config.getDescription(),
                config.isDynamic(),
                config.getScope());
    }
}
