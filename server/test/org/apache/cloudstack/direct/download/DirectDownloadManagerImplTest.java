//
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
//
package org.apache.cloudstack.direct.download;

import com.cloud.agent.AgentManager;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.agent.directdownload.DirectDownloadCommand.DownloadProtocol;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class DirectDownloadManagerImplTest {

    @Mock
    HostDao hostDao;
    @Mock
    AgentManager agentManager;

    @Spy
    @InjectMocks
    private DirectDownloadManagerImpl manager = new DirectDownloadManagerImpl();

    private static final String HTTP_HEADER_1 = "Content-Type";
    private static final String HTTP_VALUE_1 = "application/x-www-form-urlencoded";
    private static final String HTTP_HEADER_2 = "Accept-Encoding";
    private static final String HTTP_VALUE_2 = "gzip";

    private static final String VALID_CERTIFICATE =
            "MIIGIDCCBQigAwIBAgISBBNfK9DprG2M0UhfMJI4EqQKMA0GCSqGSIb3DQEBCwUA" +
            "MEoxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MSMwIQYDVQQD" +
            "ExpMZXQncyBFbmNyeXB0IEF1dGhvcml0eSBYMzAeFw0xODEwMTEwNTU3MDVaFw0x" +
            "OTAxMDkwNTU3MDVaMBsxGTAXBgNVBAMTEGNsb3VkLmNlbnRvcy5vcmcwggEiMA0G" +
            "CSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDPPr8MWWyfIF7lXZ1tkazvDUBKK6Vs" +
            "znDCndeuMqWAOc/vf7RN32bH5v+To9eHB/MPC0cCAMt2K38jC+c4ITgQlja75lDV" +
            "G3sHzORQlD2GNmwiIL5TK6pc0NpmSrNBlMkhSlkaGeRPhNKDj7pIIDwRMbDxiaNE" +
            "lZD/A/K/ugv/FkZT6M1gU17G9oo+ztdKrv6Ps8zCBIh/TlREXcridGdUR1CY5+Pl" +
            "ernPEzDmWf8jKrWgw20n5uf6tbBcR50yX3PM9KjNjZRppUEtIsFfCMYlenrkR5x8" +
            "LAcFd9oPcuJ9PRnhMUKRs9kSUErI7OjEn/KUU0glK6hlUt23UxJ0MhhrAgMBAAGj" +
            "ggMtMIIDKTAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsG" +
            "AQUFBwMCMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFOBxEtfv1fdgy0C27Ir6Ftsq" +
            "wSz9MB8GA1UdIwQYMBaAFKhKamMEfd265tE5t6ZFZe/zqOyhMG8GCCsGAQUFBwEB" +
            "BGMwYTAuBggrBgEFBQcwAYYiaHR0cDovL29jc3AuaW50LXgzLmxldHNlbmNyeXB0" +
            "Lm9yZzAvBggrBgEFBQcwAoYjaHR0cDovL2NlcnQuaW50LXgzLmxldHNlbmNyeXB0" +
            "Lm9yZy8wMQYDVR0RBCowKIIUYnVpbGRsb2dzLmNlbnRvcy5vcmeCEGNsb3VkLmNl" +
            "bnRvcy5vcmcwgf4GA1UdIASB9jCB8zAIBgZngQwBAgEwgeYGCysGAQQBgt8TAQEB" +
            "MIHWMCYGCCsGAQUFBwIBFhpodHRwOi8vY3BzLmxldHNlbmNyeXB0Lm9yZzCBqwYI" +
            "KwYBBQUHAgIwgZ4MgZtUaGlzIENlcnRpZmljYXRlIG1heSBvbmx5IGJlIHJlbGll" +
            "ZCB1cG9uIGJ5IFJlbHlpbmcgUGFydGllcyBhbmQgb25seSBpbiBhY2NvcmRhbmNl" +
            "IHdpdGggdGhlIENlcnRpZmljYXRlIFBvbGljeSBmb3VuZCBhdCBodHRwczovL2xl" +
            "dHNlbmNyeXB0Lm9yZy9yZXBvc2l0b3J5LzCCAQMGCisGAQQB1nkCBAIEgfQEgfEA" +
            "7wB1AG9Tdqwx8DEZ2JkApFEV/3cVHBHZAsEAKQaNsgiaN9kTAAABZmHqH74AAAQD" +
            "AEYwRAIgB0Dx4lsNHUz3gB2j4qCT2NSvo0AZSE485HiX/S5eKRcCIGZE+R+dRlLG" +
            "Q83tXVx3DawCTTEu1T0+cVy6oZ0dcbfUAHYAY/Lbzeg7zCzPC3KEJ1drM6SNYXeP" +
            "vXWmOLHHaFRL2I0AAAFmYeofbAAABAMARzBFAiEAg0zxDNDTHhWgK8Hp3TQwUC2q" +
            "eeFQuDdsQSokgBdbfIwCIC1cC9nq2/HOnxPXgv9J8GlVkfxunzpVnxSNftay9sVf" +
            "MA0GCSqGSIb3DQEBCwUAA4IBAQBGfnEP6dEN9t9NprhGL8A/8IdjBDonFLW0Z1Se" +
            "jsT73GqNuLY1ixqZhswNKO4oKIsFBfOFB5Qt4BYLKkbIqdm/ktAAlpPIjndBC2Tg" +
            "zn0SGXsceQOxkEmzMVYm6SpQ12JJUmmItRI7W3A1wmBmYi4pcpgNsHCkfmdzdZf6" +
            "olpzTsc3AjZGue+LAJA0B/UOwh33uNZrRlQQxig/Do1WDLz5awvhTj/4KlJucFTD" +
            "S2jt4OR6GZGSXYLwEiyG/aJv2CJVtQBCbqqeSNrwj9PkKJmAT3GswnCV+PDteiy5" +
            "Oid9eucJAY9VhVQPoQXEKi7XdhC2PtNvQlLT6alx82mneywq";

    @Before
    public void setUp() {
    }

    @Test
    public void testGetProtocolMetalink() {
        String url = "http://192.168.1.2/tmpl.metalink";
        DownloadProtocol protocol = DirectDownloadManagerImpl.getProtocolFromUrl(url);
        Assert.assertEquals(DownloadProtocol.METALINK, protocol);
    }

    @Test
    public void testGetProtocolHttp() {
        String url = "http://192.168.1.2/tmpl.qcow2";
        DownloadProtocol protocol = DirectDownloadManagerImpl.getProtocolFromUrl(url);
        Assert.assertEquals(DownloadProtocol.HTTP, protocol);
    }

    @Test
    public void testGetProtocolHttps() {
        String url = "https://192.168.1.2/tmpl.qcow2";
        DownloadProtocol protocol = DirectDownloadManagerImpl.getProtocolFromUrl(url);
        Assert.assertEquals(DownloadProtocol.HTTPS, protocol);
    }

    @Test
    public void testGetProtocolNfs() {
        String url = "nfs://192.168.1.2/tmpl.qcow2";
        DownloadProtocol protocol = DirectDownloadManagerImpl.getProtocolFromUrl(url);
        Assert.assertEquals(DownloadProtocol.NFS, protocol);
    }

    @Test
    public void testGetHeadersFromDetailsHttpHeaders() {
        Map<String, String> details = new HashMap<>();
        details.put("Message.ReservedCapacityFreed.Flag", "false");
        details.put(DirectDownloadManagerImpl.httpHeaderDetailKey + ":" + HTTP_HEADER_1, HTTP_VALUE_1);
        details.put(DirectDownloadManagerImpl.httpHeaderDetailKey + ":" + HTTP_HEADER_2, HTTP_VALUE_2);
        Map<String, String> headers = manager.getHeadersFromDetails(details);
        Assert.assertEquals(2, headers.keySet().size());
        Assert.assertTrue(headers.containsKey(HTTP_HEADER_1));
        Assert.assertTrue(headers.containsKey(HTTP_HEADER_2));
        Assert.assertEquals(HTTP_VALUE_1, headers.get(HTTP_HEADER_1));
        Assert.assertEquals(HTTP_VALUE_2, headers.get(HTTP_HEADER_2));
    }

    @Test
    public void testGetHeadersFromDetailsNonHttpHeaders() {
        Map<String, String> details = new HashMap<>();
        details.put("Message.ReservedCapacityFreed.Flag", "false");
        Map<String, String> headers = manager.getHeadersFromDetails(details);
        Assert.assertTrue(headers.isEmpty());
    }

    @Test
    public void testCertificateSanityValidCertificate() {
        String pretifiedCertificate = manager.getPretifiedCertificate(VALID_CERTIFICATE);
        manager.certificateSanity(pretifiedCertificate);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCertificateSanityInvalidCertificate() {
        String pretifiedCertificate = manager.getPretifiedCertificate(VALID_CERTIFICATE + "xxx");
        manager.certificateSanity(pretifiedCertificate);
    }
}
