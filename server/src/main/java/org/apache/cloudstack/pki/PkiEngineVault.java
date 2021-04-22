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

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.api.pki.Credential;
import com.bettercloud.vault.api.pki.Pki;
import com.bettercloud.vault.api.pki.RoleOptions;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.response.PkiResponse;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import com.cloud.domain.Domain;
import com.cloud.utils.net.Ip;

/**
 * @author Khosrow Moossavi
 * @since 4.12.0.0
 */
public class PkiEngineVault implements PkiEngine {
    public static final int RETRY_COUNT = 2;
    public static final int RETRY_INTERVAL_MILISECONDS = 2000;
    public static final int OPEN_CONNECTION_TIMEOUT_SECONDS = 5;
    public static final int READ_CONNECTION_TIMEOUT_SECONDS = 5;

    private final String _vaultUrl;
    private final String _vaultToken;
    private final String _vaultTokenRoleId;
    private final String _vaultTokenSecretId;
    private final String _vaultRoleName;
    private final String _vaultMountPath;

    private final String _certificateCommonName;
    private final String _vaultPkiTtl;
    private final String _vaultCATtl;
    private final String _vaultRoleTtl;

    public PkiEngineVault(Map<String, String> configs) {
        _vaultUrl = configs.get(PkiConfig.VaultUrl.key());
        Assert.isTrue(!Strings.isNullOrEmpty(_vaultUrl), "PKI Engine: URL of Vault endpoint is missing");

        _vaultToken = configs.get(PkiConfig.VaultToken.key());

        // if Token provided ignore RoleId and SecretId
        if (!Strings.isNullOrEmpty(_vaultToken)) {
            _vaultTokenRoleId = null;
            _vaultTokenSecretId = null;
        } else {
            _vaultTokenRoleId = configs.get(PkiConfig.VaultAppRoleId.key());
            _vaultTokenSecretId = configs.get(PkiConfig.VaultAppSecretId.key());

            if (Strings.isNullOrEmpty(_vaultTokenRoleId) && Strings.isNullOrEmpty(_vaultTokenSecretId)) {
                throw new IllegalArgumentException("PKI Engine: Vault Token access and RoleId and SecretId are missing");
            }
        }

        _vaultRoleName = configs.get(PkiConfig.VaultRoleName.key());
        Assert.isTrue(!Strings.isNullOrEmpty(_vaultRoleName), "PKI Engine: Vault PKI role name is missing");

        String mountPath = configs.get(PkiConfig.VaultMounthPath.key());

        Assert.isTrue(!Strings.isNullOrEmpty(mountPath), "PKI Engine: Vault PKI mount path is missing");
        Assert.isTrue(!StringUtils.endsWith(mountPath, "/"), "PKI Engine: Vault PKI mount path must not end with trailing slash, current value: " + mountPath);

        _vaultMountPath = mountPath + "/%s";

        String certificateBrand = configs.get(PkiConfig.CertificateBrand.key());
        _certificateCommonName = configs.get(PkiConfig.CertificateCommonName.key()).replaceAll("__BRAND__", certificateBrand);

        _vaultPkiTtl = configs.get(PkiConfig.VaultPkiTtl.key());
        Assert.isTrue(!Strings.isNullOrEmpty(_vaultPkiTtl), "PKI Engine: Vault PKI TTL is missing");

        _vaultCATtl = configs.get(PkiConfig.VaultCATtl.key());
        Assert.isTrue(!Strings.isNullOrEmpty(_vaultCATtl), "PKI Engine: Vault PKI root CA TTL is missing");

        _vaultRoleTtl = configs.get(PkiConfig.VaultRoleTtl.key());
        Assert.isTrue(!Strings.isNullOrEmpty(_vaultRoleTtl), "PKI Engine: Vault PKI role TTL is missing");
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.pki.PkiEngine#issueCertificate(com.cloud.domain.Domain, com.cloud.utils.net.Ip)
     */
    @Override
    public PkiDetail issueCertificate(Domain domain, Ip publicIp) throws VaultException {
        Assert.notNull(domain, "PKI Engine: Cannot issue Certificate because domain is null");

        Vault vault = new VaultBuilder().build();

        createRoleIfMissing(vault, domain);

        final String path = String.format(_vaultMountPath, domain.getUuid());
        Pki pki = vault.pki(path);

        PkiResponse response = pki.issue(_vaultRoleName, publicIp.addr(), null, Arrays.asList(publicIp.addr()), null, null);
        Credential credential = response.getCredential();

        if (response.getRestResponse().getStatus() == 404) {
            throw new VaultException("Cannot find Vault PKI backend path for domain " + domain.getUuid());
        }

        return new PkiDetail()
                .certificate(credential.getCertificate())
                .issuingCa(credential.getIssuingCa())
                .privateKey(credential.getPrivateKey())
                .privateKeyType(credential.getPrivateKeyType())
                .serialNumber(credential.getSerialNumber());
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.pki.PkiEngine#getCertificate(com.cloud.domain.Domain)
     */
    @Override
    public PkiDetail getCertificate(Domain domain) throws VaultException {
        Assert.notNull(domain, "PKI Engine: Cannot get Certificate because domain is null");

        Vault vault = new VaultBuilder().build();
        Logical logical = vault.logical();

        final String path = String.format(_vaultMountPath, domain.getUuid());
        final String apiEndoint = new StringBuilder()
                .append(path)
                .append("/cert/ca")
                .toString();

        LogicalResponse response = logical.read(apiEndoint);
        Map<String, String> data = response.getData();

        Assert.hasLength(data.get("certificate"), "PKI Engine: Cannot get Certificate, Vault response is empty");

        return new PkiDetail().issuingCa(data.get("certificate"));
    }

    /**
     * Create Vault PKI role if it's missing or return the existing one
     *
     * @param vault object
     * @param domain object
     *
     * @return newly created or existing Vault PKI role
     *
     * @throws VaultException
     */
    private RoleOptions createRoleIfMissing(Vault vault, Domain domain) throws VaultException {
        final String path = String.format(_vaultMountPath, domain.getUuid());
        Pki pki = vault.pki(path);
        PkiResponse response = pki.getRole(_vaultRoleName);
        RoleOptions role = response.getRoleOptions();

        // role does exist
        if (response.getRestResponse().getStatus() == 200) {
            return role;
        }

        createMountPointIfMissing(vault, domain);
        createRootCertIfMissing(vault, domain);
        createConfigUrlIfMissing(vault, domain);

        // create new role
        RoleOptions options = new RoleOptions()
                .allowAnyName(true)
                .ttl(_vaultRoleTtl);

        return pki.createOrUpdateRole(_vaultRoleName, options).getRoleOptions();
    }

    /**
     * Create Vault PKI engine mount point if it's missing
     *
     * @param vault object
     * @param domain object
     *
     * @throws VaultException
     */
    private void createMountPointIfMissing(Vault vault, Domain domain) throws VaultException {
        final String sysMountBase = "sys/mounts";
        final String path = String.format(_vaultMountPath, domain.getUuid());
        final String apiEndpoint = new StringBuilder()
                .append(sysMountBase)
                .append("/")
                .append(path)
                .toString();

        try {
            vault.logical().read(sysMountBase + "/tune");
            return;
        } catch (VaultException e) {
            // mount point not found, continue to create it
        }

        // create mount point
        Map<String, Object> createPayload = ImmutableMap.of("type", "pki");
        vault.logical().write(apiEndpoint, createPayload);

        // tune mount point
        Map<String, Object> tunePayload = ImmutableMap.of(
                "default_lease_ttl", _vaultPkiTtl,
                "max_lease_ttl", _vaultPkiTtl,
                "description", domain.getName());
        vault.logical().write(apiEndpoint + "/tune", tunePayload);
    }

    /**
     * Create Vault root Certificate CA if it's missing
     *
     * @param vault object
     * @param domain object
     *
     * @throws VaultException
     */
    private void createRootCertIfMissing(Vault vault, Domain domain) throws VaultException {
        String path = String.format(_vaultMountPath, domain.getUuid());
        final String apiEndpoint = new StringBuilder()
                .append(path)
                .append("/root/generate/internal")
                .toString();

        final String commonName = _certificateCommonName.replaceAll("__DOMAIN__", domain.getName());
        Map<String, Object> payload = ImmutableMap.of("common_name", commonName, "ttl", _vaultCATtl);

        vault.logical().write(apiEndpoint, payload);
    }

    /**
     * create Vault PKI CRL config URLs if they are missing
     *
     * @param vault object
     * @param domain object
     *
     * @throws VaultException
     */
    private void createConfigUrlIfMissing(Vault vault, Domain domain) throws VaultException {
        final String path = String.format(_vaultMountPath, domain.getUuid());
        final String apiEndpoint = new StringBuilder()
                .append(path)
                .append("/config/urls")
                .toString();

        try {
            vault.logical().read(apiEndpoint);
            return;
        } catch (VaultException e) {
            // config urls for this pki endpoint don't exist, continue to create them
        }

        String caUrl = new StringBuilder()
                .append(_vaultUrl)
                .append("/v1/")
                .append(path)
                .append("/ca")
                .toString();

        String crlUrl = new StringBuilder()
                .append(_vaultUrl)
                .append("/v1/")
                .append(path)
                .append("/crl")
                .toString();

        // create CRL config urls
        Map<String, Object> createPayload = ImmutableMap.of("issuing_certificates", caUrl, "crl_distribution_points", crlUrl);
        vault.logical().write(apiEndpoint, createPayload);
    }

    /**
     * Vault object builder
     */
    private class VaultBuilder {
        private VaultBuilder() {
        }

        /**
         * Build Vault object based on provided information and scenarios
         *
         * 1) Vault Token is provided: create VaultConfig and Vault object right away
         * 2) Vault Token is not provided: fetching Vault Token based on provided RoleId and SecretId
         *
         * @return Vault object containing client token (provided or fetched)
         *
         * @throws VaultException
         */
        public Vault build() throws VaultException {
            final VaultConfig config = new VaultConfig()
                                            .address(_vaultUrl)
                                            .token(_vaultToken)
                                            .openTimeout(OPEN_CONNECTION_TIMEOUT_SECONDS)
                                            .readTimeout(READ_CONNECTION_TIMEOUT_SECONDS)
                                            .build();

            // Vault Token is provided, Vault object can be initialized right away
            if (!Strings.isNullOrEmpty(_vaultToken)) {
                return new Vault(config).withRetries(RETRY_COUNT, RETRY_INTERVAL_MILISECONDS);
            }

            // Vault Token is not provided, but AppRole information is.
            // We're going to fetch client token through REST API call.
            AuthResponse response = new Vault(config).auth().loginByAppRole(_vaultTokenRoleId, _vaultTokenSecretId);

            // putting back client token on VaultConfig for further use
            config.token(response.getAuthClientToken());

            return new Vault(config).withRetries(RETRY_COUNT, RETRY_INTERVAL_MILISECONDS);
        }
    }
}
