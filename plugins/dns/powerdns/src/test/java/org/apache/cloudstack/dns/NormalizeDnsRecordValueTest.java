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

import static org.apache.cloudstack.dns.DnsRecord.RecordType.A;
import static org.apache.cloudstack.dns.DnsRecord.RecordType.AAAA;
import static org.apache.cloudstack.dns.DnsRecord.RecordType.CNAME;
import static org.apache.cloudstack.dns.DnsRecord.RecordType.MX;
import static org.apache.cloudstack.dns.DnsRecord.RecordType.NS;
import static org.apache.cloudstack.dns.DnsRecord.RecordType.PTR;
import static org.apache.cloudstack.dns.DnsRecord.RecordType.SRV;
import static org.apache.cloudstack.dns.DnsRecord.RecordType.TXT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class NormalizeDnsRecordValueTest {

    private final String description;
    private final String input;
    private final DnsRecord.RecordType recordType;
    private final String expected;
    private final boolean expectException;

    public NormalizeDnsRecordValueTest(String description, String input,
                                       DnsRecord.RecordType recordType,
                                       String expected, boolean expectException) {
        this.description = description;
        this.input = input;
        this.recordType = recordType;
        this.expected = expected;
        this.expectException = expectException;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {

                // ----------------------------------------------------------------
                // Guard: blank/null value — all record types should throw
                // ----------------------------------------------------------------
                {"null value, A record",     null,  A,     null, true},
                {"empty value, A record",    "",    A,     null, true},
                {"blank value, A record",    "   ", A,     null, true},

                {"null value, AAAA record",   null,  AAAA,  null, true},
                {"null value, CNAME record",  null,  CNAME, null, true},
                {"null value, MX record",     null,  MX,    null, true},
                {"null value, TXT record",    null,  TXT,   null, true},
                {"null value, SRV record",    null,  SRV,   null, true},
                {"null value, NS record",     null,  NS,    null, true},
                {"null value, PTR record",    null,  PTR,   null, true},

                // ----------------------------------------------------------------
                // A record
                // ----------------------------------------------------------------
                {"A: valid IPv4",                    "93.184.216.34", A, "93.184.216.34", false},
                {"A: valid IPv4 with whitespace",    "  93.184.216.34  ", A, "93.184.216.34", false},
                {"A: loopback",                      "127.0.0.1", A, "127.0.0.1", false},
                {"A: all-zeros",                     "0.0.0.0", A, "0.0.0.0", false},
                {"A: broadcast",                     "255.255.255.255", A, "255.255.255.255", false},
                {"A: private 10.x",                  "10.0.0.1", A, "10.0.0.1", false},
                {"A: private 192.168.x",             "192.168.1.1", A, "192.168.1.1", false},

                {"A: IPv6 rejected",                 "2001:db8::1", A, null, true},
                {"A: domain rejected",               "example.com", A, null, true},
                {"A: partial IP rejected",           "192.168.1", A, null, true},
                {"A: trailing dot rejected",         "93.184.216.34.", A, null, true},
                {"A: octet out of range rejected",   "256.0.0.1", A, null, true},

                // ----------------------------------------------------------------
                // AAAA record
                // ----------------------------------------------------------------
                {"AAAA: full IPv6",                  "2001:0db8:0000:0000:0000:0000:0000:0001", AAAA,
                        "2001:0db8:0000:0000:0000:0000:0000:0001", false},

                {"AAAA: compressed IPv6",            "2001:db8::1", AAAA, "2001:db8::1", false},
                {"AAAA: loopback",                   "::1", AAAA, "::1", false},
                {"AAAA: all zeros",                  "::", AAAA, "::", false},
                {"AAAA: whitespace",                 "  2001:db8::1  ", AAAA, "2001:db8::1", false},

                {"AAAA: IPv4 rejected",              "93.184.216.34", AAAA, null, true},
                {"AAAA: domain rejected",            "example.com", AAAA, null, true},
                {"AAAA: invalid hex rejected",       "2001:db8::xyz", AAAA, null, true},

                // ----------------------------------------------------------------
                // CNAME record
                // ----------------------------------------------------------------
                {"CNAME: basic",                     "target.example.com", CNAME, "target.example.com.", false},
                {"CNAME: uppercase",                 "TARGET.EXAMPLE.COM", CNAME, "target.example.com.", false},
                {"CNAME: trailing dot",              "target.example.com.", CNAME, "target.example.com.", false},
                {"CNAME: whitespace",                "  target.example.com  ", CNAME, "target.example.com.", false},
                {"CNAME: subdomain",                 "sub.target.example.com", CNAME, "sub.target.example.com.", false},

                {"CNAME: IP rejected",               "192.168.1.1", CNAME, null, true},
                {"CNAME: invalid label",             "-bad.example.com", CNAME, null, true},

                // ----------------------------------------------------------------
                // NS record
                // ----------------------------------------------------------------
                {"NS: basic",                        "ns1.example.com", NS, "ns1.example.com.", false},
                {"NS: uppercase",                    "NS1.EXAMPLE.COM", NS, "ns1.example.com.", false},
                {"NS: trailing dot",                 "ns1.example.com.", NS, "ns1.example.com.", false},
                {"NS: subdomain",                    "ns1.sub.example.com", NS, "ns1.sub.example.com.", false},

                {"NS: IP rejected",                  "8.8.8.8", NS, null, true},
                {"NS: invalid label",                "ns1-.example.com", NS, null, true},

                // ----------------------------------------------------------------
                // PTR record
                // ----------------------------------------------------------------
                {"PTR: basic",                       "host.example.com", PTR, "host.example.com.", false},
                {"PTR: in-addr.arpa",                "1.168.192.in-addr.arpa", PTR, "1.168.192.in-addr.arpa.", false},
                {"PTR: uppercase",                   "HOST.EXAMPLE.COM", PTR, "host.example.com.", false},
                {"PTR: trailing dot",                "host.example.com.", PTR, "host.example.com.", false},

                {"PTR: IP rejected",                 "192.168.1.1", PTR, null, true},
                {"PTR: invalid label",               "-host.example.com", PTR, null, true},

                // ----------------------------------------------------------------
                // MX record
                // ----------------------------------------------------------------
                {"MX: standard",                     "10 mail.example.com", MX, "10 mail.example.com.", false},
                {"MX: zero priority",                "0 mail.example.com", MX, "0 mail.example.com.", false},
                {"MX: max priority",                 "65535 mail.example.com", MX, "65535 mail.example.com.", false},
                {"MX: uppercase",                    "10 MAIL.EXAMPLE.COM", MX, "10 mail.example.com.", false},
                {"MX: trailing dot",                 "10 mail.example.com.", MX, "10 mail.example.com.", false},
                {"MX: extra whitespace",             "10   mail.example.com", MX, "10 mail.example.com.", false},

                {"MX: missing domain",               "10", MX, null, true},
                {"MX: priority out of range",        "65536 mail.example.com", MX, null, true},
                {"MX: non-numeric priority",         "abc mail.example.com", MX, null, true},
                {"MX: IP rejected",                  "10 192.168.1.1", MX, null, true},

                // ----------------------------------------------------------------
                // SRV record
                // ----------------------------------------------------------------
                {"SRV: standard",         "10 20 443 target.example.com", SRV, "10 20 443 target.example.com.", false},
                {"SRV: zeros",            "0 0 1 target.example.com", SRV, "0 0 1 target.example.com.", false},
                {"SRV: max values",       "65535 65535 65535 target.example.com", SRV, "65535 65535 65535 target.example.com.", false},
                {"SRV: uppercase",        "10 20 443 TARGET.EXAMPLE.COM", SRV, "10 20 443 target.example.com.", false},
                {"SRV: trailing dot",     "10 20 443 target.example.com.", SRV, "10 20 443 target.example.com.", false},

                {"SRV: missing target",           "10 20 443", SRV, null, true},
                {"SRV: port 0",                   "10 20 0 target.example.com", SRV, null, true},
                {"SRV: priority out of range",    "65536 20 443 target.example.com", SRV, null, true},
                {"SRV: IP rejected",              "10 20 443 192.168.1.1", SRV, null, true},

                // ----------------------------------------------------------------
                // TXT record
                // ----------------------------------------------------------------
                {"TXT: trim",                    "  hello world  ", TXT, "hello world", false},
                {"TXT: already clean",           "v=spf1 include:example.com ~all", TXT, "v=spf1 include:example.com ~all", false},
                {"TXT: special chars",           "v=DKIM1; k=rsa; p=MIGf", TXT, "v=DKIM1; k=rsa; p=MIGf", false},
                {"TXT: unicode",                 "héllo wörld", TXT, "héllo wörld", false},
                {"TXT: multiple spaces",         "key=value  with  spaces", TXT, "key=value  with  spaces", false},
                {"TXT: quoted",                  "\"quoted value\"", TXT, "\"quoted value\"", false},
                {"TXT: blank",                   "   ", TXT, null, true},
                {"TXT: newline",                 "\n", TXT, null, true},
        });
    }

    @Test
    public void testNormalizeDnsRecordValue() {
        if (expectException) {
            try {
                DnsProviderUtil.normalizeDnsRecordValue(input, recordType);
                fail("Expected IllegalArgumentException for [" + description + "] input='" + input + "'");
            } catch (IllegalArgumentException ignored) {}
        } else {
            String result = DnsProviderUtil.normalizeDnsRecordValue(input, recordType);
            assertEquals(description, expected, result);
        }
    }
}
