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

import java.util.List;

import org.apache.cloudstack.dns.DnsProvider;
import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.DnsRecord;
import org.apache.cloudstack.dns.DnsServer;
import org.apache.cloudstack.dns.DnsZone;

import com.cloud.utils.component.AdapterBase;

public class PowerDnsProvider extends AdapterBase implements DnsProvider {
    @Override
    public DnsProviderType getProviderType() {
        return DnsProviderType.PowerDNS;
    }

    @Override
    public boolean validate(DnsServer server) {
        return false;
    }

    @Override
    public boolean createZone(DnsServer server, DnsZone zone) {
        return false;
    }

    @Override
    public boolean deleteZone(DnsServer server, DnsZone zone) {
        return false;
    }

    @Override
    public DnsRecord createRecord(DnsServer server, DnsZone zone, DnsRecord record) {
        return null;
    }

    @Override
    public boolean updateRecord(DnsServer server, DnsZone zone, DnsRecord record) {
        return false;
    }

    @Override
    public boolean deleteRecord(DnsServer server, DnsZone zone, DnsRecord record) {
        return false;
    }

    @Override
    public List<DnsRecord> listRecords(DnsServer server, DnsZone zone) {
        return List.of();
    }
}
