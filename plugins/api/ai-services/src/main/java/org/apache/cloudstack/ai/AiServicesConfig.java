// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai;

import org.apache.cloudstack.framework.config.ConfigKey;

/**
 * Global settings for the AI Services plugin (SPEC.md section 9).
 *
 * Scopes matter here: enablement is per zone because a provider may run
 * GPU capacity in one zone only, and Hugging Face approval is per domain
 * because section 5 lets a reseller mirror directly while others must
 * request it.
 */
public final class AiServicesConfig {

    private AiServicesConfig() {
    }

    private static final String CATEGORY = "Advanced";

    /**
     * The category CloudStack encrypts.
     *
     * <p>{@code ConfigurationVO.isEncrypted()} is a literal
     * {@code StringUtils.equalsAny(getCategory(), "Hidden", "Secure")},
     * so a password declared anywhere else is stored in the database —
     * and returned by {@code listConfigurations} — in cleartext.
     * "Secure" rather than "Hidden" because an operator still has to
     * find the setting to fill it in.
     */
    private static final String SECRET_CATEGORY = "Secure";

    public static final ConfigKey<Boolean> Enabled = new ConfigKey<>(
            CATEGORY, Boolean.class, "ai.services.enabled", "false",
            "Whether AI Services is enabled in this zone.", true, ConfigKey.Scope.Zone);

    /**
     * Comma-separated engine URLs. More than one because the engine runs at
     * least two replicas behind a load balancer and the plugin fails over
     * between them (SPEC.md section 7).
     */
    public static final ConfigKey<String> EngineUrls = new ConfigKey<>(
            CATEGORY, String.class, "ai.services.engine.urls", "",
            "Comma-separated base URLs of the AI Services engine API, tried in order.",
            true, ConfigKey.Scope.Global);

    public static final ConfigKey<Integer> EngineTimeout = new ConfigKey<>(
            CATEGORY, Integer.class, "ai.services.engine.timeout", "30",
            "Seconds to wait for an engine API response before failing over.",
            true, ConfigKey.Scope.Global);

    public static final ConfigKey<String> EngineClientCert = new ConfigKey<>(
            CATEGORY, String.class, "ai.services.engine.tls.client.cert", "",
            "Path to the PKCS12 key store holding the client certificate and key "
                    + "the plugin presents to the engine (mTLS).",
            true, ConfigKey.Scope.Global);

    public static final ConfigKey<String> EngineClientKey = new ConfigKey<>(
            SECRET_CATEGORY, String.class, "ai.services.engine.tls.client.key", "",
            "Password for that key store. Not a path — the description said one "
                    + "for a while, and the code has always read it as a password.",
            true, ConfigKey.Scope.Global);

    public static final ConfigKey<String> EngineTrustStore = new ConfigKey<>(
            CATEGORY, String.class, "ai.services.engine.tls.truststore", "",
            "Path to the PKCS12 trust store holding the engine's CA certificate.",
            true, ConfigKey.Scope.Global);

    public static final ConfigKey<String> EngineTrustStorePassword = new ConfigKey<>(
            CATEGORY, String.class, "ai.services.engine.tls.truststore.password", "changeit",
            "Password for the trust store. It needs one: keytool refuses a password "
                    + "under six characters, and a store openssl writes with an empty "
                    + "password loads as zero certificates rather than failing.",
            true, ConfigKey.Scope.Global);

    /**
     * Domain-scoped: where false, a domain admin may mirror from Hugging Face
     * directly; where true, their request goes to a root admin queue
     * (SPEC.md section 5, step 4).
     */
    public static final ConfigKey<Boolean> HfRequireApproval = new ConfigKey<>(
            CATEGORY, Boolean.class, "ai.services.hf.require.approval", "true",
            "Whether domain admin mirror requests need root admin approval.",
            true, ConfigKey.Scope.Domain);

    /**
     * How long host maintenance waits for a drain before the admin must
     * confirm a forced drain (SPEC.md section 7).
     */
    public static final ConfigKey<Integer> MaintenanceDrainTimeout = new ConfigKey<>(
            CATEGORY, Integer.class, "ai.services.maintenance.drain.timeout", "900",
            "Seconds to wait for GPU nodes to drain before a host enters maintenance.",
            true, ConfigKey.Scope.Global);

    /**
     * Whether the Tier 2 command surface is registered (SPEC.md section 8).
     *
     * <p>Off by default. The engine marks these {@code Release.V2}, and a
     * GA install should not grow a surface for features it does not run;
     * this is how the same boundary holds on the plugin side.
     */
    public static final ConfigKey<Boolean> V2Enabled = new ConfigKey<>(
            CATEGORY, Boolean.class, "ai.services.v2.enabled", "false",
            "Register the Tier 2 (v2) AI Services API commands.",
            true, ConfigKey.Scope.Global);

    public static ConfigKey<?>[] all() {
        return new ConfigKey<?>[] {
                Enabled,
                EngineUrls,
                EngineTimeout,
                EngineClientCert,
                EngineClientKey,
                EngineTrustStore,
                EngineTrustStorePassword,
                HfRequireApproval,
                MaintenanceDrainTimeout,
                V2Enabled,
        };
    }
}
