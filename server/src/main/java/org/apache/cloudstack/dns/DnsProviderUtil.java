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

import java.net.Inet4Address;
import java.net.Inet6Address;

import org.apache.commons.validator.routines.DomainValidator;

import com.cloud.utils.StringUtils;
import com.google.common.net.InetAddresses;

public class DnsProviderUtil {
    static DomainValidator validator = DomainValidator.getInstance(true);

    public static String appendPublicSuffixToZone(String zoneName, String suffixDomain) {
        if (StringUtils.isBlank(suffixDomain)) {
            return zoneName;
        }
        suffixDomain = DnsProviderUtil.normalizeDomainForDb(suffixDomain);
        // Already suffixed → return as-is
        if (zoneName.toLowerCase().endsWith("." + suffixDomain)) {
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

    // lowercase, no trailing dot (used for DB storage, comparisons)
    public static String normalizeDomainForDb(String domain) {
        if (StringUtils.isBlank(domain)) {
            throw new IllegalArgumentException("Domain cannot be empty");
        }

        String normalized = domain.trim().toLowerCase();
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        // Validate domain, allow local/private TLDs
        boolean valid = isValidInternalDnsZoneName(normalized) || validator.isValid(normalized);
        if (!valid) {
            throw new IllegalArgumentException("Invalid domain name: " + domain);
        }
        return normalized;
    }

    static boolean isValidInternalDnsZoneName(String domain) {
        // Total length limit (DNS standard)
        if (domain.length() > 253) {
            return false;
        }

        String[] labels = domain.split("\\.");
        // Must have at least 2 labels (zone + suffix like internal/test/etc.)
        if (labels.length < 2) {
            return false;
        }
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63) {
                return false;
            }

            // Must start and end with alphanumeric (even if underscores exist inside)
            if (!label.matches("^[a-z0-9](?:[a-z0-9_-]{0,61}[a-z0-9])?$")) {
                return false;
            }

            // Prevent obviously invalid cases like "__" only labels
            if (label.equals("_")) {
                return false;
            }
        }
        return true;
    }

    // DNS wire form: lowercase, validated, WITH trailing dot (used in record values)
    public static String normalizeDnsRecordValue(String value, DnsRecord.RecordType recordType) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("DNS record value cannot be empty");
        }
        String trimmedValue = value.trim();
        switch (recordType) {
            case A:
                if (!(InetAddresses.forString(trimmedValue) instanceof Inet4Address)) {
                    throw new IllegalArgumentException(
                            String.format("Invalid IP address for %s record: %s", recordType, value));
                }
                return trimmedValue;
            case AAAA:
                if (!(InetAddresses.forString(trimmedValue) instanceof Inet6Address)) {
                    throw new IllegalArgumentException(
                            String.format("Invalid IP address for %s record: %s", recordType, value));
                }
                return trimmedValue;
            case CNAME:
            case NS:
            case PTR:
                return normalizeDomainForDnsRecord(trimmedValue);
            case SRV:
                return normalizeSrvRecord(trimmedValue);
            case MX:
                return normalizeMxRecord(trimmedValue);
            case TXT:
                // Free text: preserve exactly
                return trimmedValue;
            default:
                throw new IllegalArgumentException("Unsupported DNS record type: " + recordType);
        }
    }

    static String normalizeDomainForDnsRecord(String domain) {
        if (StringUtils.isBlank(domain)) {
            throw new IllegalArgumentException("Domain name cannot be empty");
        }
        String normalized = domain.trim().toLowerCase();
        // Strip trailing dot first (normalize input)
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        // Reject IP addresses
        if (InetAddresses.isInetAddress(normalized)) {
            throw new IllegalArgumentException("Domain cannot be an IP address: " + normalized);
        }

        // Validate total length (max 253 chars, excluding trailing dot)
        if (normalized.length() > 253) {
            throw new IllegalArgumentException(
                    "Domain name exceeds maximum length: " + normalized);
        }

        // Validate labels
        String[] labels = normalized.split("\\.", -1);
        for (String label : labels) {
            if (label.isEmpty()) {
                throw new IllegalArgumentException(
                        "Domain contains empty label: " + normalized);
            }
            if (label.length() > 63) {
                throw new IllegalArgumentException(
                        "Domain label too long: " + label);
            }
            if (!label.matches("[a-z0-9]([a-z0-9-]*[a-z0-9])?")) {
                throw new IllegalArgumentException(
                        "Invalid domain label: " + label);
            }
        }
        return normalized + ".";
    }

    private static String normalizeSrvRecord(String value) {
        String trimmed = value.trim();
        String[] parts = trimmed.split("\\s+", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "Invalid SRV record value (expected '<priority> <weight> <port> <target>'): " + trimmed);
        }

        int priority;
        int weight;
        int port;

        try {
            priority = Integer.parseInt(parts[0]);
            weight = Integer.parseInt(parts[1]);
            port = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "SRV priority/weight/port must be numeric: " + trimmed);
        }

        if (priority < 0 || priority > 65535) {
            throw new IllegalArgumentException("SRV priority out of range (0–65535): " + parts[0]);
        }

        if (weight < 0 || weight > 65535) {
            throw new IllegalArgumentException("SRV weight out of range (0–65535): " + parts[1]);
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("SRV port out of range (1–65535): " + parts[2]);
        }

        String target = normalizeDomainForDnsRecord(parts[3]);

        return priority + " " + weight + " " + port + " " + target;
    }

    private static String normalizeMxRecord(String value) {
        String trimmed = value.trim();
        String[] parts = trimmed.split("\\s+", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid MX record value (expected '<priority> <mail-exchanger>'): " + trimmed);
        }

        int priority;

        try {
            priority = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "MX priority must be numeric: " + parts[0]);
        }

        if (priority < 0 || priority > 65535) {
            throw new IllegalArgumentException(
                    "MX priority out of range (0–65535): " + parts[0]);
        }

        String mailExchanger = normalizeDomainForDnsRecord(parts[1]);

        return priority + " " + mailExchanger;
    }
}
