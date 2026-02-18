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

import org.apache.cloudstack.dns.DnsProvider;
import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.DnsRecord;
import org.apache.cloudstack.dns.DnsServer;
import org.apache.cloudstack.dns.DnsZone;

import com.cloud.utils.StringUtils;
import com.cloud.utils.component.AdapterBase;
import com.fasterxml.jackson.databind.JsonNode;

public class PowerDnsProvider extends AdapterBase implements DnsProvider {

    private PowerDnsClient client;

    @Override
    public DnsProviderType getProviderType() {
        return DnsProviderType.PowerDNS;
    }

    public void validate(DnsServer server) {
        validateServerParams(server);
        client.validate(server.getUrl(), server.getApiKey());
    }

    @Override
    public String provisionZone(DnsServer server, DnsZone zone) {
        validateServerZoneParams(server, zone);
        return client.createZone(server.getUrl(), server.getApiKey(), zone.getName(), server.getNameServers());
    }

    @Override
    public void deleteZone(DnsServer server, DnsZone zone) {
        validateServerZoneParams(server, zone);
        client.deleteZone(server.getUrl(), server.getApiKey(), zone.getName());
    }

    public enum ChangeType {
        REPLACE, DELETE
    }

    @Override
    public void addRecord(DnsServer server, DnsZone zone, DnsRecord record) {
        validateServerZoneParams(server, zone);
        applyRecord(server.getUrl(), server.getApiKey(), zone.getName(), record, ChangeType.REPLACE);
    }

    @Override
    public void updateRecord(DnsServer server, DnsZone zone, DnsRecord record) {
        addRecord(server, zone, record);
    }


    @Override
    public void deleteRecord(DnsServer server, DnsZone zone, DnsRecord record) {
        validateServerZoneParams(server, zone);
        applyRecord(server.getUrl(), server.getApiKey(), zone.getName(), record, ChangeType.DELETE);
    }

    public void applyRecord(String serverUrl, String apiKey, String zoneName, DnsRecord record, ChangeType changeType) {
        client.modifyRecord(serverUrl, apiKey, zoneName, record.getName(), record.getType().name(),
                record.getTtl(), record.getContents(), changeType.name());
    }



    @Override
    public List<DnsRecord> listRecords(DnsServer server, DnsZone zone) {
        List<DnsRecord> records = new ArrayList<>();
        for (JsonNode rrset: client.listRecords(server.getUrl(), server.getApiKey(), zone.getName())) {
            String name = rrset.path("name").asText();
            String typeStr = rrset.path("type").asText();
            int ttl = rrset.path("ttl").asInt(0);
            if (!"SOA".equalsIgnoreCase(typeStr)) {
                try {
                    List<String> contents = new ArrayList<>();
                    JsonNode recordsNode = rrset.path("records");
                    if (recordsNode.isArray()) {
                        for (JsonNode rec : recordsNode) {
                            String content = rec.path("content").asText();
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

    void validateServerZoneParams(DnsServer server, DnsZone zone) {
        validateServerParams(server);
        if (StringUtils.isBlank(zone.getName())) {
            throw new IllegalArgumentException("Zone name cannot be empty");
        }
    }

    void validateServerParams(DnsServer server) {
        if (StringUtils.isBlank(server.getUrl())) {
            throw new IllegalArgumentException("PowerDNS API URL cannot be empty");
        }
        if (StringUtils.isBlank(server.getApiKey())) {
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
