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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.cloud.utils.exception.CloudRuntimeException;

public class DnsRecordTest {

    @Test
    public void testDefaultConstructor() {
        DnsRecord record = new DnsRecord();
        assertNull(record.getName());
        assertNull(record.getType());
        assertNull(record.getContents());
        assertEquals(0, record.getTtl());
    }

    @Test
    public void testParameterizedConstructor() {
        List<String> contents = Arrays.asList("192.168.1.1");
        DnsRecord record = new DnsRecord("www", DnsRecord.RecordType.A, contents, 3600);

        assertEquals("www", record.getName());
        assertEquals(DnsRecord.RecordType.A, record.getType());
        assertEquals(contents, record.getContents());
        assertEquals(3600, record.getTtl());
    }

    @Test
    public void testSettersAndGetters() {
        DnsRecord record = new DnsRecord();
        List<String> contents = Arrays.asList("10.0.0.1", "10.0.0.2");

        record.setName("mail");
        record.setType(DnsRecord.RecordType.AAAA);
        record.setContents(contents);
        record.setTtl(7200);

        assertEquals("mail", record.getName());
        assertEquals(DnsRecord.RecordType.AAAA, record.getType());
        assertEquals(contents, record.getContents());
        assertEquals(7200, record.getTtl());
    }

    // RecordType.fromString tests

    @Test
    public void testFromStringValid() {
        assertEquals(DnsRecord.RecordType.A, DnsRecord.RecordType.fromString("A"));
        assertEquals(DnsRecord.RecordType.AAAA, DnsRecord.RecordType.fromString("AAAA"));
        assertEquals(DnsRecord.RecordType.CNAME, DnsRecord.RecordType.fromString("CNAME"));
        assertEquals(DnsRecord.RecordType.MX, DnsRecord.RecordType.fromString("MX"));
        assertEquals(DnsRecord.RecordType.TXT, DnsRecord.RecordType.fromString("TXT"));
        assertEquals(DnsRecord.RecordType.SRV, DnsRecord.RecordType.fromString("SRV"));
        assertEquals(DnsRecord.RecordType.PTR, DnsRecord.RecordType.fromString("PTR"));
        assertEquals(DnsRecord.RecordType.NS, DnsRecord.RecordType.fromString("NS"));
    }

    @Test
    public void testFromStringCaseInsensitive() {
        assertEquals(DnsRecord.RecordType.A, DnsRecord.RecordType.fromString("a"));
        assertEquals(DnsRecord.RecordType.CNAME, DnsRecord.RecordType.fromString("cname"));
        assertEquals(DnsRecord.RecordType.MX, DnsRecord.RecordType.fromString("mx"));
    }

    @Test
    public void testFromStringNull() {
        assertNull(DnsRecord.RecordType.fromString(null));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testFromStringInvalid() {
        DnsRecord.RecordType.fromString("INVALID");
    }

    @Test
    public void testRecordTypeValues() {
        DnsRecord.RecordType[] values = DnsRecord.RecordType.values();
        assertNotNull(values);
        assertEquals(8, values.length);
    }
}
