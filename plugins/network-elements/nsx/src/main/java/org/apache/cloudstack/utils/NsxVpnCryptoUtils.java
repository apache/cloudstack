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
package org.apache.cloudstack.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.InvalidParameterValueException;

/**
 * Maps CloudStack Site-to-Site VPN policy strings (a comma-separated list of
 * "cipher-hash[;dhgroup]" proposals) to the NSX IPSec VPN profile enums, rejecting
 * parameters NSX does not support.
 */
public class NsxVpnCryptoUtils {

    public static final long NSX_MIN_IKE_SA_LIFETIME = 21600L;
    public static final long NSX_MIN_ESP_SA_LIFETIME = 900L;
    public static final long NSX_MAX_ESP_SA_LIFETIME = 31536000L;
    public static final int NSX_MAX_PSK_LENGTH = 128;

    private static final Map<String, String> ENCRYPTION_ALGORITHM_MAP = Map.of(
            "aes128", "AES_128",
            "aes256", "AES_256");

    private static final Map<String, String> DIGEST_ALGORITHM_MAP = Map.of(
            "sha1", "SHA1",
            "sha256", "SHA2_256",
            "sha384", "SHA2_384",
            "sha512", "SHA2_512");

    private static final Map<String, String> DH_GROUP_MAP = Map.of(
            "modp1024", "GROUP2",
            "modp1536", "GROUP5",
            "modp2048", "GROUP14",
            "modp3072", "GROUP15",
            "modp4096", "GROUP16");

    private static final Map<String, String> IKE_VERSION_MAP = Map.of(
            "ike", "IKE_FLEX",
            "ikev1", "IKE_V1",
            "ikev2", "IKE_V2");

    private NsxVpnCryptoUtils() {
    }

    public static List<String> getEncryptionAlgorithms(String policy) {
        Set<String> algorithms = new LinkedHashSet<>();
        for (String entry : getPolicyEntries(policy)) {
            String token = entry.split(";")[0].split("-")[0].toLowerCase(Locale.ROOT);
            String nsxAlgorithm = ENCRYPTION_ALGORITHM_MAP.get(token);
            if (nsxAlgorithm == null) {
                throw new InvalidParameterValueException(String.format(
                        "Encryption algorithm %s is not supported by NSX Site-to-Site VPN (supported: %s)",
                        token, "aes128, aes256"));
            }
            algorithms.add(nsxAlgorithm);
        }
        return new ArrayList<>(algorithms);
    }

    public static List<String> getDigestAlgorithms(String policy) {
        Set<String> algorithms = new LinkedHashSet<>();
        for (String entry : getPolicyEntries(policy)) {
            String[] tokens = entry.split(";")[0].split("-");
            if (tokens.length < 2) {
                throw new InvalidParameterValueException(String.format(
                        "Missing hash algorithm in VPN policy entry %s", entry));
            }
            String token = tokens[1].toLowerCase(Locale.ROOT);
            String nsxAlgorithm = DIGEST_ALGORITHM_MAP.get(token);
            if (nsxAlgorithm == null) {
                throw new InvalidParameterValueException(String.format(
                        "Hash algorithm %s is not supported by NSX Site-to-Site VPN (supported: %s)",
                        token, "sha1, sha256, sha384, sha512"));
            }
            algorithms.add(nsxAlgorithm);
        }
        return new ArrayList<>(algorithms);
    }

    public static List<String> getDhGroups(String policy) {
        Set<String> groups = new LinkedHashSet<>();
        for (String entry : getPolicyEntries(policy)) {
            String[] tokens = entry.split(";");
            if (tokens.length < 2 || StringUtils.isBlank(tokens[1])) {
                continue;
            }
            String dhGroup = tokens[1].trim().toLowerCase(Locale.ROOT);
            String nsxGroup = DH_GROUP_MAP.get(dhGroup);
            if (nsxGroup == null) {
                throw new InvalidParameterValueException(String.format(
                        "Diffie-Hellman group %s is not supported by NSX Site-to-Site VPN (supported: %s)",
                        dhGroup, "modp1024, modp1536, modp2048, modp3072, modp4096"));
            }
            groups.add(nsxGroup);
        }
        return new ArrayList<>(groups);
    }

    public static String getIkeVersion(String ikeVersion) {
        String token = StringUtils.isBlank(ikeVersion) ? "ike" : ikeVersion.toLowerCase(Locale.ROOT);
        String nsxIkeVersion = IKE_VERSION_MAP.get(token);
        if (nsxIkeVersion == null) {
            throw new InvalidParameterValueException(String.format(
                    "IKE version %s is not supported by NSX Site-to-Site VPN (supported: %s)",
                    token, "ike, ikev1, ikev2"));
        }
        return nsxIkeVersion;
    }

    public static void validateIkeLifetime(Long ikeLifetime) {
        if (ikeLifetime != null && ikeLifetime < NSX_MIN_IKE_SA_LIFETIME) {
            throw new InvalidParameterValueException(String.format(
                    "IKE lifetime %s is below the NSX minimum of %s seconds", ikeLifetime, NSX_MIN_IKE_SA_LIFETIME));
        }
    }

    public static void validateEspLifetime(Long espLifetime) {
        if (espLifetime != null && (espLifetime < NSX_MIN_ESP_SA_LIFETIME || espLifetime > NSX_MAX_ESP_SA_LIFETIME)) {
            throw new InvalidParameterValueException(String.format(
                    "ESP lifetime %s is outside the NSX supported range of %s-%s seconds",
                    espLifetime, NSX_MIN_ESP_SA_LIFETIME, NSX_MAX_ESP_SA_LIFETIME));
        }
    }

    public static void validatePresharedKey(String psk) {
        if (StringUtils.isBlank(psk)) {
            throw new InvalidParameterValueException("A pre-shared key is required for NSX Site-to-Site VPN");
        }
        if (psk.length() > NSX_MAX_PSK_LENGTH) {
            throw new InvalidParameterValueException(String.format(
                    "The pre-shared key exceeds the NSX maximum length of %s characters", NSX_MAX_PSK_LENGTH));
        }
    }

    public static void validate(String ikePolicy, String espPolicy, String ikeVersion,
                                Long ikeLifetime, Long espLifetime, String psk) {
        getEncryptionAlgorithms(ikePolicy);
        getDigestAlgorithms(ikePolicy);
        getDhGroups(ikePolicy);
        getEncryptionAlgorithms(espPolicy);
        getDigestAlgorithms(espPolicy);
        getDhGroups(espPolicy);
        getIkeVersion(ikeVersion);
        validateIkeLifetime(ikeLifetime);
        validateEspLifetime(espLifetime);
        validatePresharedKey(psk);
    }

    private static String[] getPolicyEntries(String policy) {
        if (StringUtils.isBlank(policy)) {
            throw new InvalidParameterValueException("An empty VPN policy cannot be mapped to NSX");
        }
        String[] entries = policy.split(",");
        for (int i = 0; i < entries.length; i++) {
            entries[i] = entries[i].trim();
        }
        return entries;
    }
}
