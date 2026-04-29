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

package org.apache.cloudstack.dns.powerdns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.dns.DnsProvider;
import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.DnsRecord;
import org.apache.cloudstack.dns.DnsServer;
import org.apache.cloudstack.dns.DnsZone;
import org.apache.cloudstack.dns.exception.DnsProviderException;
import org.apache.logging.log4j.util.Strings;

import com.cloud.utils.StringUtils;
import com.cloud.utils.component.AdapterBase;
import com.fasterxml.jackson.databind.JsonNode;

public class PowerDnsProvider extends AdapterBase implements DnsProvider {
    private PowerDnsClient client;
    static String PDNS_SERVER_ID = "pdnsServerId";

    @Override
    public DnsProviderType getProviderType() {
        return DnsProviderType.PowerDNS;
    }

    public void validate(DnsServer server) throws DnsProviderException {
        validateRequiredServerFields(server);
        client.validateServerId(server.getUrl(), server.getPort(), server.getDnsApiKey(), server.getDetail(PDNS_SERVER_ID));
    }

    @Override
    public String validateAndResolveServer(DnsServer server) throws Exception {
        validateRequiredServerFields(server);
        String resolvedDnsServerId = client.resolveServerId(server.getUrl(), server.getPort(), server.getDnsApiKey(), server.getDetail(PDNS_SERVER_ID));
        if (Strings.isNotBlank(resolvedDnsServerId)) {
            server.appendDetails(PDNS_SERVER_ID, resolvedDnsServerId);
        }
        return resolvedDnsServerId;
    }

    @Override
    public String provisionZone(DnsServer server, DnsZone zone) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        return client.createZone(
                server.getUrl(),
                server.getPort(),
                server.getDnsApiKey(),
                server.getDetail(PDNS_SERVER_ID),
                zone.getName(),
                ApiConstants.NATIVE_ZONE, false, server.getNameServers()
        );
    }

    @Override
    public void deleteZone(DnsServer server, DnsZone zone) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        client.deleteZone(server.getUrl(), server.getPort(), server.getDnsApiKey(), server.getDetail(PDNS_SERVER_ID), zone.getName());
    }

    @Override
    public void updateZone(DnsServer server, DnsZone zone) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        client.updateZone(
                server.getUrl(),
                server.getPort(),
                server.getDnsApiKey(),
                server.getDetail(PDNS_SERVER_ID),
                zone.getName(), ApiConstants.NATIVE_ZONE, false, server.getNameServers());
    }

    public enum ChangeType {
        REPLACE, DELETE
    }

    @Override
    public String addRecord(DnsServer server, DnsZone zone, DnsRecord record) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        return applyRecord(
                server.getUrl(),
                server.getPort(),
                server.getDnsApiKey(),
                server.getDetail(PDNS_SERVER_ID),
                zone.getName(), record, ChangeType.REPLACE);
    }

    @Override
    public String updateRecord(DnsServer server, DnsZone zone, DnsRecord record) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        return addRecord(server, zone, record);
    }

    @Override
    public String deleteRecord(DnsServer server, DnsZone zone, DnsRecord record) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        return applyRecord(server.getUrl(),
                server.getPort(),
                server.getDnsApiKey(),
                server.getDetail(PDNS_SERVER_ID),
                zone.getName(), record, ChangeType.DELETE);
    }

    public String applyRecord(String serverUrl, Integer port, String apiKey, String externalServerId, String zoneName,
                              DnsRecord record, ChangeType changeType) throws DnsProviderException {

        return client.modifyRecord(serverUrl, port, apiKey, externalServerId, zoneName, record.getName(),
                record.getType().name(), record.getTtl(), record.getContents(), changeType.name());
    }

    @Override
    public List<DnsRecord> listRecords(DnsServer server, DnsZone zone) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        List<DnsRecord> records = new ArrayList<>();
        Iterable<JsonNode> rrsetNodes = client.listRecords(server.getUrl(), server.getPort(), server.getDnsApiKey(),
                server.getDetail(PDNS_SERVER_ID), zone.getName());

        for (JsonNode rrset : rrsetNodes) {
            String name = rrset.path(ApiConstants.NAME).asText();
            String typeStr = rrset.path(ApiConstants.TYPE).asText();
            int ttl = rrset.path(ApiConstants.TTL).asInt(0);
            if (!"SOA".equalsIgnoreCase(typeStr)) {
                try {
                    List<String> contents = new ArrayList<>();
                    JsonNode recordsNode = rrset.path(ApiConstants.RECORDS);
                    if (recordsNode.isArray()) {
                        for (JsonNode rec : recordsNode) {
                            String content = rec.path(ApiConstants.CONTENT).asText();
                            if (!content.isEmpty()) {
                                contents.add(content);
                            }
                        }
                    }
                    records.add(new DnsRecord(name, DnsRecord.RecordType.valueOf(typeStr), contents, ttl));
                } catch (Exception ignored) {
                    // Skip unsupported record types
                }
            }
        }
        return records;
    }

    public boolean dnsRecordExists(DnsServer server, DnsZone zone, String recordName, String recordType) throws DnsProviderException {
        return client.dnsRecordExists(server.getUrl(), server.getPort(), server.getDnsApiKey(),
                server.getDetail(PDNS_SERVER_ID), zone.getName(), recordName, recordType);
    }

    void validateRequiredServerAndZoneFields(DnsServer server, DnsZone zone) {
        validateRequiredServerFields(server);
        if (StringUtils.isBlank(zone.getName())) {
            throw new IllegalArgumentException("Zone name cannot be empty");
        }
    }

    void validateRequiredServerFields(DnsServer server) {
        if (StringUtils.isBlank(server.getUrl())) {
            throw new IllegalArgumentException("PowerDNS API URL cannot be empty");
        }
        if (StringUtils.isBlank(server.getDnsApiKey())) {
            throw new IllegalArgumentException("PowerDNS API key cannot be empty");
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        if (client == null) {
            client = new PowerDnsClient();
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (client != null) {
           client.close();
        }
        return true;
    }
}
