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
import org.mockito.junit.MockitoJUnitRunner;

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

    private static final String HTTP_HEADER_1 = "content-type";
    private static final String HTTP_VALUE_1 = "application/x-www-form-urlencoded";
    private static final String HTTP_HEADER_2 = "accept-encoding";
    private static final String HTTP_VALUE_2 = "gzip";

    private static final String VALID_CERTIFICATE =
            "MIIDSzCCAjMCFDa0LoW+1O8/cEwCI0nIqfl8c1TLMA0GCSqGSIb3DQEBCwUAMGEx\n" +
            "CzAJBgNVBAYTAkNTMQswCQYDVQQIDAJDUzELMAkGA1UEBwwCQ1MxCzAJBgNVBAoM\n" +
            "AkNTMQswCQYDVQQLDAJDUzELMAkGA1UEAwwCQ1MxETAPBgkqhkiG9w0BCQEWAkNT\n" +
            "MCAXDTE5MDQyNDE1NTIzNVoYDzIwOTgwOTE1MTU1MjM1WjBhMQswCQYDVQQGEwJD\n" +
            "UzELMAkGA1UECAwCQ1MxCzAJBgNVBAcMAkNTMQswCQYDVQQKDAJDUzELMAkGA1UE\n" +
            "CwwCQ1MxCzAJBgNVBAMMAkNTMREwDwYJKoZIhvcNAQkBFgJDUzCCASIwDQYJKoZI\n" +
            "hvcNAQEBBQADggEPADCCAQoCggEBAKstLRcMGCo6+2hojRMjEuuimnWp27yfYhDU\n" +
            "w/Cj03MJe/KCOhwsDqX82QNIr/bNtLdFf2ZJEUQd08sLLlHeUy9y5aOcxt9SGx2j\n" +
            "xolqO4MBL7BW3dklO0IvjaEfBeFP6udz8ajeVur/iPPZb2Edd0zlXuHvDozfQisv\n" +
            "bpuJImnTUVx0ReCXP075PBGvlqQXW2uEht+E/w3H8/2rra3JFV6J5xc77KyQSq2t\n" +
            "1+2ZU7PJiy/rppXf5rjTvNm6ydfag8/av7lcgs2ntdkK4koAmkmROhAwNonlL7cD\n" +
            "xIC83cKOqOFiQXSwr1IgoLf7zBNafKoTlSb/ev6Zt18BXEMLGpkCAwEAATANBgkq\n" +
            "hkiG9w0BAQsFAAOCAQEAVS5uWZRz2m3yx7EUQm47RTMW5WMXU4pI8D+N5WZ9xubY\n" +
            "OqtU3r2OAYpfL/QO8iT7jcqNYGoDqe8ZjEaNvfxiTG8cOI6TSXhKBG6hjSaSFQSH\n" +
            "OZ5mfstM36y/3ENFh6JCJ2ao1rgWSbfDRyAaHuvt6aCkaV6zRq2OMEgoJqZSgwxL\n" +
            "QO230xa2hYgKXOePMVZyHFA2oKJtSOc3jCke9Y8zDUwm0McGdMRBD8tVB0rcaOqQ\n" +
            "0PlDLjB9sQuhhLu8vjdgbznmPbUmMG7JN0yhT1eJbIX5ImXyh0DoTwiaGcYwW6Sq\n" +
            "YodjXACsC37xaQXAPYBiaAs4iI80TJSx1DVFO1LV0g==";

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
