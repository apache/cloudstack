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

    private static final String VALID_HUNDRED_YEARS_CERTIFICATE =
            "MIIDSzCCAjMCFExHvjXI7ffTZqcH4urbc6bqazFaMA0GCSqGSIb3DQEBCwUAMGEx" +
            "CzAJBgNVBAYTAkNTMQswCQYDVQQIDAJDUzELMAkGA1UEBwwCQ1MxCzAJBgNVBAoM" +
            "AkNTMQswCQYDVQQLDAJDUzELMAkGA1UEAwwCQ1MxETAPBgkqhkiG9w0BCQEWAkNT" +
            "MCAXDTE5MDQwMzEzMzgyOFoYDzIxMTkwMzEwMTMzODI4WjBhMQswCQYDVQQGEwJD" +
            "UzELMAkGA1UECAwCQ1MxCzAJBgNVBAcMAkNTMQswCQYDVQQKDAJDUzELMAkGA1UE" +
            "CwwCQ1MxCzAJBgNVBAMMAkNTMREwDwYJKoZIhvcNAQkBFgJDUzCCASIwDQYJKoZI" +
            "hvcNAQEBBQADggEPADCCAQoCggEBANEuKuCOjnRJZtqBeKwZD4XNVDTKmxGLNJ3j" +
            "6q71qlLa8quu115di6IxHkeerB9XnQMHHmqCv1qgpoWDuxA8uAra8T/teCvQjGRl" +
            "lWDlBBajFZZ4Crsj0MxGIbuoHTQ4Ossyv3vJztbm+RZ79nTEA35xzQj7HxeFVyk+" +
            "zqC6e4mMCzhI+UTKd3sOZBt8/y34egCv2UK9Lso9950dHnmlXYREd1j85Kestqjh" +
            "tKntw3DLo5i8RLQ/11iW4Z+xOlL11ubhvJ0S8UAF5BU8pcLMNv+WztaoAAc3N+Yc" +
            "WSTIXjQUtMT9TlHec8+NKlF9e62o4XMNHiaGEOf1idXC2URqqy8CAwEAATANBgkq" +
            "hkiG9w0BAQsFAAOCAQEAFSAjv8dw0Lo8U7CsjWNlW/LBZdP9D54vx0kXOLyWjeYH" +
            "7u4DTikoigjunm1lB5QPL2k5jSQEpTclcN313hMGCEMW9GZEtcSoUxqkiTEtKlyw" +
            "cC/PO/NHHgDrp1Fg9yhUOLKXJyBp9bfjKtm2YqPNyrpTB5cLfjRp69Hx5G5KuKCm" +
            "fAxcVdrfUluu6l+d4Y4FnuvS3rb9rDy/ES36SXczXNQFAzvI8ZuTlgxTRIvM184N" +
            "GA0utaoFPJAzZ01HYlRSYmipHx6NZE7roTAC5wmT3R1jkFlfkw8LSBynsR6U6Vkw" +
            "90kMmEH4NoYTV+mF4A0iY+NkEsuvnSsqheDknO/8OA==";

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
        String pretifiedCertificate = manager.getPretifiedCertificate(VALID_HUNDRED_YEARS_CERTIFICATE);
        manager.certificateSanity(pretifiedCertificate);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCertificateSanityInvalidCertificate() {
        String pretifiedCertificate = manager.getPretifiedCertificate(VALID_HUNDRED_YEARS_CERTIFICATE + "xxx");
        manager.certificateSanity(pretifiedCertificate);
    }
}
