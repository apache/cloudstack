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
package com.cloud.keystore;

import org.apache.cloudstack.api.response.AlertResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import com.cloud.api.ApiSerializerHelper;

import junit.framework.TestCase;

public class KeystoreTest extends TestCase {
    private final static Logger s_logger = Logger.getLogger(KeystoreTest.class);

    private final String keyContent = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALV5vGlkiWwoZX4hTRplPXP8qtST\n"
        + "hwZhko8noeY5vf8ECwmd+vrCTw/JvnOtkx/8oYNbg/SeUt1EfOsk6gqJdBblGFBZRMcUJlIpqE9z\n"
        + "uv68U9G8Gfi/qvRSY336hibw0J5bZ4vn1QqmyHDB+Czea9AjFUV7AEVG15+vED7why+/AgMBAAEC\n"
        + "gYBmFBPnNKYYMKDmUdUNA+WNWJK/ADzzWe8WlzR6TACTcbLDthl289WFC/YVG42mcHRpbxDKiEQU\n"
        + "MnIR0rHTO34Qb/2HcuyweStU2gqR6omxBvMnFpJr90nD1HcOMJzeLHsphau0/EmKKey+gk4PyieD\n"
        + "KqTM7LTjjHv8xPM4n+WAAQJBAOMNCeFKlJ4kMokWhU74B5/w/NGyT1BHUN0VmilHSiJC8JqS4BiI\n"
        + "ZpAeET3VmilO6QTGh2XVhEDGteu3uZR6ipUCQQDMnRzMgQ/50LFeIQo4IBtwlEouczMlPQF4c21R\n"
        + "1d720moxILVPT0NJZTQUDDmmgbL+B7CgtcCR2NlP5sKPZVADAkEAh4Xq1cy8dMBKYcVNgNtPQcqI\n"
        + "PWpfKR3ISI5yXB0vRNAL6Vet5zbTcUZhKDVtNSbis3UEsGYH8NorEC2z2cpjGQJANhJi9Ow6c5Mh\n"
        + "/DURBUn+1l5pyCKrZnDbvaALSLATLvjmFTuGjoHszy2OeKnOZmEqExWnKKE/VYuPyhy6V7i3TwJA\n"
        + "f8skDgtPK0OsBCa6IljPaHoWBjPc4kFkSTSS1d56hUcWSikTmiuKdLyBb85AADSZYsvHWrte4opN\n" + "dhNukMJuRA==\n";

    private final String certContent = "-----BEGIN CERTIFICATE-----\n" + "MIIE3jCCA8agAwIBAgIFAqv56tIwDQYJKoZIhvcNAQEFBQAwgcoxCzAJBgNVBAYT\n"
        + "AlVTMRAwDgYDVQQIEwdBcml6b25hMRMwEQYDVQQHEwpTY290dHNkYWxlMRowGAYD\n" + "VQQKExFHb0RhZGR5LmNvbSwgSW5jLjEzMDEGA1UECxMqaHR0cDovL2NlcnRpZmlj\n"
        + "YXRlcy5nb2RhZGR5LmNvbS9yZXBvc2l0b3J5MTAwLgYDVQQDEydHbyBEYWRkeSBT\n" + "ZWN1cmUgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkxETAPBgNVBAUTCDA3OTY5Mjg3\n"
        + "MB4XDTA5MDIxMTA0NTc1NloXDTEyMDIwNzA1MTEyM1owWTEZMBcGA1UECgwQKi5y\n" + "ZWFsaG9zdGlwLmNvbTEhMB8GA1UECwwYRG9tYWluIENvbnRyb2wgVmFsaWRhdGVk\n"
        + "MRkwFwYDVQQDDBAqLnJlYWxob3N0aXAuY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GN\n" + "ADCBiQKBgQC1ebxpZIlsKGV+IU0aZT1z/KrUk4cGYZKPJ6HmOb3/BAsJnfr6wk8P\n"
        + "yb5zrZMf/KGDW4P0nlLdRHzrJOoKiXQW5RhQWUTHFCZSKahPc7r+vFPRvBn4v6r0\n" + "UmN9+oYm8NCeW2eL59UKpshwwfgs3mvQIxVFewBFRtefrxA+8IcvvwIDAQABo4IB\n"
        + "vTCCAbkwDwYDVR0TAQH/BAUwAwEBADAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYB\n" + "BQUHAwIwDgYDVR0PAQH/BAQDAgWgMDIGA1UdHwQrMCkwJ6AloCOGIWh0dHA6Ly9j\n"
        + "cmwuZ29kYWRkeS5jb20vZ2RzMS0yLmNybDBTBgNVHSAETDBKMEgGC2CGSAGG/W0B\n" + "BxcBMDkwNwYIKwYBBQUHAgEWK2h0dHA6Ly9jZXJ0aWZpY2F0ZXMuZ29kYWRkeS5j\n"
        + "b20vcmVwb3NpdG9yeS8wgYAGCCsGAQUFBwEBBHQwcjAkBggrBgEFBQcwAYYYaHR0\n" + "cDovL29jc3AuZ29kYWRkeS5jb20vMEoGCCsGAQUFBzAChj5odHRwOi8vY2VydGlm\n"
        + "aWNhdGVzLmdvZGFkZHkuY29tL3JlcG9zaXRvcnkvZ2RfaW50ZXJtZWRpYXRlLmNy\n" + "dDAfBgNVHSMEGDAWgBT9rGEyk2xF1uLuhV+auud2mWjM5zArBgNVHREEJDAighAq\n"
        + "LnJlYWxob3N0aXAuY29tgg5yZWFsaG9zdGlwLmNvbTAdBgNVHQ4EFgQUHxwmdK5w\n" + "9/YVeZ/3fHyi6nQfzoYwDQYJKoZIhvcNAQEFBQADggEBABv/XinvId6oWXJtmku+\n"
        + "7m90JhSVH0ycoIGjgdaIkcExQGP08MCilbUsPcbhLheSFdgn/cR4e1MP083lacoj\n" + "OGauY7b8f/cuquGkT49Ns14awPlEzRjjycQEjjLxFEuL5CFWa2t2gKRE1dSfhDQ+\n"
        + "fJ6GBCs1XgZLuhkKS8fPf+YmG2ZjHzYDjYoSx7paDXgEm+kbYIZdCK51lA0BUAjP\n" + "9ZMGhsu/PpAbh5U/DtcIqxY0xeqD4TeGsBzXg6uLhv+jKHDtXg5fYPe+z0n5DCEL\n"
        + "k0fLF4+i/pt9hVCz0QrZ28RUhXf825+EOL0Gw+Uzt+7RV2cCaJrlu4cDrDom2FRy\n" + "E8I=\n" + "-----END CERTIFICATE-----\n";

    @Override
    @Before
    public void setUp() {
        /*
                MockComponentLocator locator = new MockComponentLocator("management-server");
                locator.addDao("keystoreDao", KeystoreDaoImpl.class);
                locator.addManager("KeystoreManager", KeystoreManagerImpl.class);
                locator.makeActive(new DefaultInterceptorLibrary());
        */
    }

    @Override
    @After
    public void tearDown() throws Exception {
    }

    /*
        public void testKeystoreSave() throws Exception {
            KeystoreVO ksVo;

            ComponentLocator locator = ComponentLocator.getCurrentLocator();

            KeystoreDao ksDao = locator.getDao(KeystoreDao.class);
            ksDao.save("CPVMCertificate", "CPVMCertificate", "KeyForCertificate", "realhostip.com");
            ksVo = ksDao.findByName("CPVMCertificate");
            assertTrue(ksVo != null);
            assertTrue(ksVo.getCertificate().equals("CPVMCertificate"));
            assertTrue(ksVo.getKey().equals("KeyForCertificate"));
            assertTrue(ksVo.getDomainSuffix().equals("realhostip.com"));

            ksDao.save("CPVMCertificate", "CPVMCertificate Again", "KeyForCertificate Again", "again.realhostip.com");

            ksVo = ksDao.findByName("CPVMCertificate");
            assertTrue(ksVo != null);
            assertTrue(ksVo.getCertificate().equals("CPVMCertificate Again"));
            assertTrue(ksVo.getKey().equals("KeyForCertificate Again"));
            assertTrue(ksVo.getDomainSuffix().equals("again.realhostip.com"));

            ksDao.expunge(ksVo.getId());
        }

        public void testStripeKey() throws Exception {
            Pattern regex = Pattern.compile("(^[\\-]+[^\\-]+[\\-]+[\\n]?)([^\\-]+)([\\-]+[^\\-]+[\\-]+$)");
            Matcher m = regex.matcher("-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEAm4bLUORp9oM65GV9XrPrbs+K563DjUR1M8mP1HaE+Y4lX5pk\nvQjC/xoEqSs5pxDDWXAkoexvxij8A4AWcsKU1Q+ep2E+GcytBoz8XINGvgb8cQNn\n/4PlVWKp7j5SDDNCfleYvmiRn8k6P4mxVJOHKzwb/IwQcKghyqAF1w==\n-----END RSA PRIVATE KEY-----");
            if(m.find()) {
                String content = m.group(2);
                assertTrue(content.startsWith("MIIEpAIBAAKCAQE"));
                assertTrue(content.endsWith("KghyqAF1w==\n"));
            } else {
                assertTrue(false);
            }
        }

        public void testKeystoreManager() throws Exception {
            ComponentLocator locator = ComponentLocator.getCurrentLocator();

            KeystoreManagerImpl ksMgr = ComponentLocator.inject(KeystoreManagerImpl.class);
            assertTrue(ksMgr.configure("TaskManager", new HashMap<String, Object>()));
            assertTrue(ksMgr.start());

            ksMgr.saveCertificate("CPVMCertificate", certContent, keyContent, "realhostip.com");

            byte[] ksBits = ksMgr.getKeystoreBits("CPVMCertificate", "realhostip", "vmops.com");
            assertTrue(ksBits != null);

            try {
                KeyStore ks = CertificateHelper.loadKeystore(ksBits, "vmops.com");
                assertTrue(ks != null);
            } catch(Exception e) {
                assertTrue(false);
            }

            KeystoreDao ksDao = locator.getDao(KeystoreDao.class);
            KeystoreVO ksVo = ksDao.findByName("CPVMCertificate");
            ksDao.expunge(ksVo.getId());
        }
    */
    public void testUuid() {
        UserVmResponse vm = new UserVmResponse();
        vm.setId(Long.toString(3L));
        /*
                vm.setAccountName("admin");
                vm.setName("i-2-3-KY");
                vm.setDisplayName("i-2-3-KY");
                vm.setDomainId(1L);
                vm.setDomainName("ROOT");
                vm.setCreated(new Date());
                vm.setState("Running");
                vm.setZoneId(1L);
                vm.setZoneName("KY");
                vm.setHostId(1L);

                vm.setObjectName("virtualmachine");
        */
        String result = ApiSerializerHelper.toSerializedString(vm);
        System.out.println(result);
        //Object obj = ApiSerializerHelper.fromSerializedString(result);

        AlertResponse alert = new AlertResponse();
        alert.setId("100");
        alert.setDescription("Hello");

        result = ApiSerializerHelper.toSerializedString(alert);
        System.out.println(result);
        ApiSerializerHelper.fromSerializedString(result);
    }
}
