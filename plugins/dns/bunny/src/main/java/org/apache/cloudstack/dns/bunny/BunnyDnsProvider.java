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

package org.apache.cloudstack.dns.bunny;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.dns.DnsProvider;
import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.DnsRecord;
import org.apache.cloudstack.dns.DnsServer;
import org.apache.cloudstack.dns.DnsZone;
import org.apache.cloudstack.dns.exception.DnsProviderException;

import com.cloud.utils.StringUtils;
import com.cloud.utils.component.AdapterBase;
import com.fasterxml.jackson.databind.JsonNode;

public class BunnyDnsProvider extends AdapterBase implements DnsProvider {
    private BunnyDnsClient client;

    @Override
    public DnsProviderType getProviderType() {
        return DnsProviderType.BunnyDNS;
    }

    @Override
    public void validate(DnsServer server) throws Exception {
        validateRequiredServerFields(server);
        client.validateAccess(server.getUrl(), server.getPort(), server.getDnsApiKey());
    }

    /** Bunny has no server id to resolve, the AccessKey alone identifies the account. */
    @Override
    public String validateAndResolveServer(DnsServer server) throws Exception {
        validate(server);
        return null;
    }

    @Override
    public String provisionZone(DnsServer server, DnsZone zone) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        long zoneId = client.createZone(server.getUrl(), server.getPort(), server.getDnsApiKey(), zone.getName());
        return Long.toString(zoneId);
    }

    @Override
    public void deleteZone(DnsServer server, DnsZone zone) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        client.deleteZone(server.getUrl(), server.getPort(), server.getDnsApiKey(), zone.getName());
    }

    /** Nothing in DnsZone maps to a Bunny zone setting, so this only verifies the zone still exists. */
    @Override
    public void updateZone(DnsServer server, DnsZone zone) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        client.resolveZoneId(server.getUrl(), server.getPort(), server.getDnsApiKey(), zone.getName());
    }

    /** The DNS framework expects add to replace all records of this name and type, as a PowerDNS rrset does. */
    @Override
    public String addRecord(DnsServer server, DnsZone zone, DnsRecord record) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        long zoneId = client.resolveZoneId(server.getUrl(), server.getPort(), server.getDnsApiKey(), zone.getName());
        return client.replaceRecords(server.getUrl(), server.getPort(), server.getDnsApiKey(), zoneId,
                zone.getName(), record.getName(), record.getType().name(), record.getTtl(), record.getContents());
    }

    @Override
    public String updateRecord(DnsServer server, DnsZone zone, DnsRecord record) throws DnsProviderException {
        return addRecord(server, zone, record);
    }

    @Override
    public String deleteRecord(DnsServer server, DnsZone zone, DnsRecord record) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        long zoneId = client.resolveZoneId(server.getUrl(), server.getPort(), server.getDnsApiKey(), zone.getName());
        return client.deleteRecordsByNameAndType(server.getUrl(), server.getPort(), server.getDnsApiKey(), zoneId,
                zone.getName(), record.getName(), record.getType().name());
    }

    @Override
    public List<DnsRecord> listRecords(DnsServer server, DnsZone zone) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        long zoneId = client.resolveZoneId(server.getUrl(), server.getPort(), server.getDnsApiKey(), zone.getName());
        List<JsonNode> rawRecords = client.listRecords(server.getUrl(), server.getPort(), server.getDnsApiKey(), zoneId);

        Map<String, DnsRecord> aggregated = new LinkedHashMap<>();
        for (JsonNode raw : rawRecords) {
            String typeName = BunnyDnsClient.typeName(raw.path("Type").asInt(-1));
            if (typeName == null) {
                continue;
            }
            DnsRecord.RecordType type = DnsRecord.RecordType.fromString(typeName);
            String fqdn = client.toFqdn(raw.path("Name").asText(""), zone.getName());
            String content = BunnyDnsClient.formatContent(typeName, raw);
            int ttl = raw.path("Ttl").asInt(0);

            String key = fqdn + "|" + type.name();
            DnsRecord dnsRecord = aggregated.get(key);
            if (dnsRecord == null) {
                dnsRecord = new DnsRecord(fqdn, type, new ArrayList<>(), ttl);
                aggregated.put(key, dnsRecord);
            }
            dnsRecord.getContents().add(content);
        }
        return new ArrayList<>(aggregated.values());
    }

    @Override
    public boolean dnsRecordExists(DnsServer server, DnsZone zone, String recordName, String recordType) throws DnsProviderException {
        validateRequiredServerAndZoneFields(server, zone);
        long zoneId = client.resolveZoneId(server.getUrl(), server.getPort(), server.getDnsApiKey(), zone.getName());
        return !client.findRecordsByNameAndType(server.getUrl(), server.getPort(), server.getDnsApiKey(), zoneId,
                zone.getName(), recordName, recordType).isEmpty();
    }

    @Override
    public boolean dnsZoneExists(DnsServer server, DnsZone zone) {
        return client.zoneExists(server.getUrl(), server.getPort(), server.getDnsApiKey(), zone.getName());
    }

    void validateRequiredServerAndZoneFields(DnsServer server, DnsZone zone) {
        validateRequiredServerFields(server);
        if (StringUtils.isBlank(zone.getName())) {
            throw new IllegalArgumentException("Zone name cannot be empty");
        }
    }

    void validateRequiredServerFields(DnsServer server) {
        if (StringUtils.isBlank(server.getUrl())) {
            throw new IllegalArgumentException("Bunny DNS API URL cannot be empty");
        }
        if (StringUtils.isBlank(server.getDnsApiKey())) {
            throw new IllegalArgumentException("Bunny DNS API key cannot be empty");
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        if (client == null) {
            client = new BunnyDnsClient();
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
