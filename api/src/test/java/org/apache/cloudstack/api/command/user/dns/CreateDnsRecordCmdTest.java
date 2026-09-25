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
package org.apache.cloudstack.api.command.user.dns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsRecordResponse;
import org.apache.cloudstack.dns.DnsRecord;
import org.junit.Test;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;

public class CreateDnsRecordCmdTest extends BaseDnsCmdTest {

    private CreateDnsRecordCmd createCmd() throws Exception {
        CreateDnsRecordCmd cmd = new CreateDnsRecordCmd();
        setField(cmd, "dnsProviderManager", dnsProviderManager);
        setField(cmd, "dnsZoneId", ENTITY_ID);
        setField(cmd, "name", "www");
        setField(cmd, "type", "A");
        setField(cmd, "contents", Arrays.asList("192.168.1.1"));
        setField(cmd, "ttl", 7200);
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        CreateDnsRecordCmd cmd = createCmd();

        assertEquals(Long.valueOf(ENTITY_ID), cmd.getDnsZoneId());
        assertEquals("www", cmd.getName());
        assertEquals(DnsRecord.RecordType.A, cmd.getType());
        assertEquals(Arrays.asList("192.168.1.1"), cmd.getContents());
        assertEquals(Integer.valueOf(7200), cmd.getTtl());
    }

    @Test
    public void testGetTtlDefault() throws Exception {
        CreateDnsRecordCmd cmd = createCmd();
        setField(cmd, "ttl", null);
        assertEquals(Integer.valueOf(3600), cmd.getTtl());
    }

    @Test
    public void testGetTypeCname() throws Exception {
        CreateDnsRecordCmd cmd = createCmd();
        setField(cmd, "type", "CNAME");
        assertEquals(DnsRecord.RecordType.CNAME, cmd.getType());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetTypeInvalid() throws Exception {
        CreateDnsRecordCmd cmd = createCmd();
        setField(cmd, "type", "INVALID");
        cmd.getType();
    }

    @Test
    public void testGetEntityOwnerId() throws Exception {
        CreateDnsRecordCmd cmd = createCmd();
        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testEventType() throws Exception {
        CreateDnsRecordCmd cmd = createCmd();
        assertEquals(EventTypes.EVENT_DNS_RECORD_CREATE, cmd.getEventType());
    }

    @Test
    public void testEventDescription() throws Exception {
        CreateDnsRecordCmd cmd = createCmd();
        assertEquals("Creating DNS Record: www", cmd.getEventDescription());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        CreateDnsRecordCmd cmd = createCmd();

        DnsRecordResponse mockResponse = new DnsRecordResponse();
        mockResponse.setName("www");
        when(dnsProviderManager.createDnsRecord(cmd)).thenReturn(mockResponse);

        cmd.execute();

        DnsRecordResponse response = (DnsRecordResponse) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("creatednsrecordresponse", response.getResponseName());
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteThrowsException() throws Exception {
        CreateDnsRecordCmd cmd = createCmd();
        when(dnsProviderManager.createDnsRecord(cmd)).thenThrow(new RuntimeException("Provider error"));
        cmd.execute();
    }
}
