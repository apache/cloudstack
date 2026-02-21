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

package org.apache.cloudstack.dns;

import org.apache.commons.validator.routines.DomainValidator;

import com.cloud.utils.StringUtils;

public class DnsUtil {
    static DomainValidator validator = DomainValidator.getInstance(true);

    public static String appendPublicSuffixToZone(String zoneName, String suffixDomain) {
        if (StringUtils.isBlank(suffixDomain)) {
            return zoneName;
        }
        // Already suffixed → return as-is
        if (zoneName.toLowerCase().endsWith("." + suffixDomain.toLowerCase())) {
            return zoneName;
        }

        if (zoneName.equals(suffixDomain)) {
            throw new IllegalArgumentException("Cannot create DNS zone at root-level: " + suffixDomain);
        }
        // Check TLD matches
        String tldUser = getTld(zoneName);
        String tldSuffix = getTld(suffixDomain);

        if (!tldUser.equalsIgnoreCase(tldSuffix)) {
            throw new IllegalArgumentException("TLD mismatch between user zone and domain suffix");
        }
        // Remove TLD from userZone
        int lastDot = zoneName.lastIndexOf('.');
        String zonePrefix = zoneName.substring(0, lastDot);
        return zonePrefix + "." + suffixDomain;
    }

    private static String getTld(String domain) {
        String[] labels = domain.split("\\.");
        return labels[labels.length - 1];
    }

    public static String normalizeDomain(String domain) {
        if (StringUtils.isBlank(domain)) {
            throw new IllegalArgumentException("Domain cannot be empty");
        }

        String normalized = domain.trim().toLowerCase();
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        // Validate domain, allow local/private TLDs
        if (!validator.isValid(normalized)) {
            throw new IllegalArgumentException("Invalid domain name: " + domain);
        }
        return normalized;
    }

    public static String normalizeDnsRecordValue(String value, DnsRecord.RecordType recordType) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("DNS record value cannot be empty");
        }
        switch (recordType) {
            case A:
            case AAAA:
                // IP addresses: trim only
                return value.trim();

            case CNAME:
            case NS:
            case PTR:
            case SRV:
                // Domain names: normalize like zones
                return normalizeDomain(value);
            case MX:
                // PowerDNS MX: contains priority + domain, only trim and lowercase
                return value.trim().toLowerCase();

            case TXT:
                // Free text: preserve exactly
                return value;

            default:
                throw new IllegalArgumentException("Unsupported DNS record type: " + recordType);
        }
    }
}
