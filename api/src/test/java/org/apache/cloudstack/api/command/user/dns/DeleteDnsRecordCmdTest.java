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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.dns.DnsRecord;
import org.junit.Test;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;

public class DeleteDnsRecordCmdTest extends BaseDnsCmdTest {

    private DeleteDnsRecordCmd createCmd() throws Exception {
        DeleteDnsRecordCmd cmd = new DeleteDnsRecordCmd();
        setField(cmd, "dnsProviderManager", dnsProviderManager);
        setField(cmd, "dnsZoneId", ENTITY_ID);
        setField(cmd, "name", "www");
        setField(cmd, "type", "A");
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        DeleteDnsRecordCmd cmd = createCmd();

        assertEquals(Long.valueOf(ENTITY_ID), cmd.getDnsZoneId());
        assertEquals("www", cmd.getName());
        assertEquals(DnsRecord.RecordType.A, cmd.getType());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetTypeInvalid() throws Exception {
        DeleteDnsRecordCmd cmd = createCmd();
        setField(cmd, "type", "BOGUS");
        cmd.getType();
    }

    @Test
    public void testGetEntityOwnerId() throws Exception {
        DeleteDnsRecordCmd cmd = createCmd();
        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testEventType() throws Exception {
        DeleteDnsRecordCmd cmd = createCmd();
        assertEquals(EventTypes.EVENT_DNS_RECORD_DELETE, cmd.getEventType());
    }

    @Test
    public void testEventDescription() throws Exception {
        DeleteDnsRecordCmd cmd = createCmd();
        assertEquals("Deleting DNS Record: www", cmd.getEventDescription());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        DeleteDnsRecordCmd cmd = createCmd();
        when(dnsProviderManager.deleteDnsRecord(cmd)).thenReturn(true);

        cmd.execute();

        SuccessResponse response = (SuccessResponse) cmd.getResponseObject();
        assertNotNull(response);
        verify(dnsProviderManager).deleteDnsRecord(cmd);
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsFalse() throws Exception {
        DeleteDnsRecordCmd cmd = createCmd();
        when(dnsProviderManager.deleteDnsRecord(cmd)).thenReturn(false);
        cmd.execute();
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteThrowsException() throws Exception {
        DeleteDnsRecordCmd cmd = createCmd();
        when(dnsProviderManager.deleteDnsRecord(cmd)).thenThrow(new RuntimeException("Error"));
        cmd.execute();
    }
}
