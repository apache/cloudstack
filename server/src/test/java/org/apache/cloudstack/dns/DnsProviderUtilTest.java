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

import org.junit.Test;

public class DnsProviderUtilTest {

    @Test
    public void testNormalizeDnsRecordValueA() {
        String result = DnsProviderUtil.normalizeDnsRecordValue("  1.2.3.4  ", DnsRecord.RecordType.A);
        assertEquals("1.2.3.4", result);
    }

    @Test
    public void testNormalizeDnsRecordValueAAAA() {
        String result = DnsProviderUtil.normalizeDnsRecordValue("  2001:db8::1  ", DnsRecord.RecordType.AAAA);
        assertEquals("2001:db8::1", result);
    }

    @Test
    public void testNormalizeDnsRecordValueCNAME() {
        // Appends dot in the process? No, normalizeDomain trims, lowercases, removes trailing dot, and checks validity.
        String result = DnsProviderUtil.normalizeDnsRecordValue("  Host.Example.Com.  ", DnsRecord.RecordType.CNAME);
        assertEquals("host.example.com", result);
    }

    @Test
    public void testNormalizeDnsRecordValueNS() {
        String result = DnsProviderUtil.normalizeDnsRecordValue("NS1.EXAMPLE.COM", DnsRecord.RecordType.NS);
        assertEquals("ns1.example.com", result);
    }

    @Test
    public void testNormalizeDnsRecordValuePTR() {
        String result = DnsProviderUtil.normalizeDnsRecordValue("ptr.valid.zone.", DnsRecord.RecordType.PTR);
        assertEquals("ptr.valid.zone", result);
    }

    @Test
    public void testNormalizeDnsRecordValueSRV() {
        String result = DnsProviderUtil.normalizeDnsRecordValue("srv.example.com", DnsRecord.RecordType.SRV);
        assertEquals("srv.example.com", result);
    }

    @Test
    public void testNormalizeDnsRecordValueMX() {
        // MX records just get trimmed and lowercased
        String result = DnsProviderUtil.normalizeDnsRecordValue(" 10 MAIL.EXAMPLE.COM ", DnsRecord.RecordType.MX);
        assertEquals("10 mail.example.com", result);
    }

    @Test
    public void testNormalizeDnsRecordValueTXT() {
        // TXT records are preserved exactly
        String result = DnsProviderUtil.normalizeDnsRecordValue("  Exact text value.  ", DnsRecord.RecordType.TXT);
        assertEquals("  Exact text value.  ", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizeDnsRecordValueEmpty() {
        DnsProviderUtil.normalizeDnsRecordValue("   ", DnsRecord.RecordType.A);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizeDnsRecordValueNull() {
        DnsProviderUtil.normalizeDnsRecordValue(null, DnsRecord.RecordType.A);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizeDnsRecordValueInvalidDomain() {
        DnsProviderUtil.normalizeDnsRecordValue("invalid!domain", DnsRecord.RecordType.CNAME);
    }
}
