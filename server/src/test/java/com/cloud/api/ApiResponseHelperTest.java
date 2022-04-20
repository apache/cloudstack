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
package com.cloud.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.cloudstack.api.response.DirectDownloadCertificateResponse;
import org.apache.cloudstack.api.response.NicSecondaryIpResponse;
import org.apache.cloudstack.api.response.UsageRecordResponse;
import org.apache.cloudstack.usage.UsageService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.domain.DomainVO;
import com.cloud.usage.UsageVO;
import com.cloud.user.AccountVO;
import com.cloud.vm.NicSecondaryIp;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ApiDBUtils.class)
public class ApiResponseHelperTest {

    @Mock
    UsageService usageService;

    ApiResponseHelper helper;

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss ZZZ");

    @Before
    public void injectMocks() throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field usageSvcField = ApiResponseHelper.class
                .getDeclaredField("_usageSvc");
        usageSvcField.setAccessible(true);
        helper = new ApiResponseHelper();
        usageSvcField.set(helper, usageService);
    }

    @Test
    public void getDateStringInternal() throws ParseException {
        Mockito.when(usageService.getUsageTimezone()).thenReturn(
                TimeZone.getTimeZone("UTC"));
        assertEquals("2014-06-29'T'23:45:00+00:00", helper
                .getDateStringInternal(dateFormat.parse("2014-06-29 23:45:00 UTC")));
        assertEquals("2014-06-29'T'23:45:01+00:00", helper
                .getDateStringInternal(dateFormat.parse("2014-06-29 23:45:01 UTC")));
        assertEquals("2014-06-29'T'23:45:11+00:00", helper
                .getDateStringInternal(dateFormat.parse("2014-06-29 23:45:11 UTC")));
        assertEquals("2014-06-29'T'23:05:11+00:00", helper
                .getDateStringInternal(dateFormat.parse("2014-06-29 23:05:11 UTC")));
        assertEquals("2014-05-29'T'08:45:11+00:00", helper
                .getDateStringInternal(dateFormat.parse("2014-05-29 08:45:11 UTC")));
    }

    @Test
    public void testUsageRecordResponse(){
        //Creating the usageVO object to be passed to the createUsageResponse.
        Long zoneId = null;
        Long accountId = 1L;
        Long domainId = 1L;
        String Description = "Test Object";
        String usageDisplay = " ";
        int usageType = -1;
        Double rawUsage = null;
        Long vmId = null;
        String vmName = " ";
        Long offeringId = null;
        Long templateId = null;
        Long usageId = null;
        Date startDate = null;
        Date endDate = null;
        String type = " ";
        UsageVO usage = new UsageVO(zoneId,accountId,domainId,Description,usageDisplay,usageType,rawUsage,vmId,vmName,offeringId,templateId,usageId,startDate,endDate,type);

        DomainVO domain = new DomainVO();
        domain.setName("DomainName");

        AccountVO account = new AccountVO();

        PowerMockito.mockStatic(ApiDBUtils.class);
        when(ApiDBUtils.findAccountById(anyLong())).thenReturn(account);
        when(ApiDBUtils.findDomainById(anyLong())).thenReturn(domain);

        UsageRecordResponse MockResponse = helper.createUsageResponse(usage);
        assertEquals("DomainName",MockResponse.getDomainName());
    }

    @Test
    public void setResponseIpAddressTestIpv4() {
        NicSecondaryIp result = Mockito.mock(NicSecondaryIp.class);
        NicSecondaryIpResponse response = new NicSecondaryIpResponse();
        setResult(result, "ipv4", "ipv6");

        ApiResponseHelper.setResponseIpAddress(result, response);

        assertTrue(response.getIpAddr().equals("ipv4"));
    }

    private void setResult(NicSecondaryIp result, String ipv4, String ipv6) {
        when(result.getIp4Address()).thenReturn(ipv4);
        when(result.getIp6Address()).thenReturn(ipv6);
    }

    @Test
    public void setResponseIpAddressTestIpv6() {
        NicSecondaryIp result = Mockito.mock(NicSecondaryIp.class);
        NicSecondaryIpResponse response = new NicSecondaryIpResponse();
        setResult(result, null, "ipv6");

        ApiResponseHelper.setResponseIpAddress(result, response);

        assertTrue(response.getIpAddr().equals("ipv6"));
    }

    @Test
    public void testHandleCertificateResponse() {
        String certStr = "-----BEGIN CERTIFICATE-----\n" +
                "MIIGLTCCBRWgAwIBAgIQOHZRhOAYLowYNcopBvxCdjANBgkqhkiG9w0BAQsFADCB\n" +
                "jzELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n" +
                "A1UEBxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTcwNQYDVQQD\n" +
                "Ey5TZWN0aWdvIFJTQSBEb21haW4gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
                "MB4XDTIxMDYxNTAwMDAwMFoXDTIyMDcxNjIzNTk1OVowFzEVMBMGA1UEAwwMKi5h\n" +
                "cGFjaGUub3JnMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4UoHCmK5\n" +
                "XdbyZ++d2BGuX35zZcESvr4K1Hw7ZTbyzMC+uokBKJcng1Hf5ctjUFKCoz7AlWRq\n" +
                "JH5U3vU0y515C0aEE+j0lUHlxMGQD2ut+sJ6BZqcTBl5d8ns1TSckEH31DBDN3Fw\n" +
                "uMLqEWBOjwt1MMT3Z+kR7ekuheJYbYHbJ2VtnKQd4jHmLly+/p+UqaQ6dIvQxq82\n" +
                "ggZIUNWjGKwXS2vKl6O9EDu/QaAX9e059pf3UxAxGtJjeKXWJvt1e96T53+2+kXp\n" +
                "j0/PuyT6F0o+grY08tCJnw7kTB4sE2qfALdwSblvyjBDOYtS4Xj5nycMpd+4Qse4\n" +
                "2+irNBdZ63pqqQIDAQABo4IC+jCCAvYwHwYDVR0jBBgwFoAUjYxexFStiuF36Zv5\n" +
                "mwXhuAGNYeEwHQYDVR0OBBYEFH+9CNXAwWW4+jyizee51r8x4ofHMA4GA1UdDwEB\n" +
                "/wQEAwIFoDAMBgNVHRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEF\n" +
                "BQcDAjBJBgNVHSAEQjBAMDQGCysGAQQBsjEBAgIHMCUwIwYIKwYBBQUHAgEWF2h0\n" +
                "dHBzOi8vc2VjdGlnby5jb20vQ1BTMAgGBmeBDAECATCBhAYIKwYBBQUHAQEEeDB2\n" +
                "ME8GCCsGAQUFBzAChkNodHRwOi8vY3J0LnNlY3RpZ28uY29tL1NlY3RpZ29SU0FE\n" +
                "b21haW5WYWxpZGF0aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdo\n" +
                "dHRwOi8vb2NzcC5zZWN0aWdvLmNvbTAjBgNVHREEHDAaggwqLmFwYWNoZS5vcmeC\n" +
                "CmFwYWNoZS5vcmcwggF+BgorBgEEAdZ5AgQCBIIBbgSCAWoBaAB2AEalVet1+pEg\n" +
                "MLWiiWn0830RLEF0vv1JuIWr8vxw/m1HAAABehHLqfgAAAQDAEcwRQIgINH3CquJ\n" +
                "zTAprwjdo2cEWkMzpaNoP1SOI4xGl68PF2oCIQC77eD7K6Smx4Fv/z/sTKk21Psb\n" +
                "ZhmVq5YoqhwRKuMgVAB2AEHIyrHfIkZKEMahOglCh15OMYsbA+vrS8do8JBilgb2\n" +
                "AAABehHLqcEAAAQDAEcwRQIhANh++zJa9AE4U0DsHIFq6bW40b1OfGfH8uUdmjEZ\n" +
                "s1jzAiBIRtJeFVmobSnbFKlOr8BGfD2L/hg1rkAgJlKY5oFShgB2ACl5vvCeOTkh\n" +
                "8FZzn2Old+W+V32cYAr4+U1dJlwlXceEAAABehHLqZ4AAAQDAEcwRQIhAOZDfvU8\n" +
                "Hz80I6Iyj2rv8+yWBVq1XVixI8bMykdCO6ADAiAWj8cJ9g1zxko4dJu8ouJf+Pwl\n" +
                "0bbhhuJHhy/f5kiaszANBgkqhkiG9w0BAQsFAAOCAQEAlkdB7FZtVQz39TDNKR4u\n" +
                "I8VQsTH5n4Kg+zVc0pptI7HGUWtp5PjBAEsvJ/G/NQXsjVflQaNPRRd7KNZycZL1\n" +
                "jls6GdVoWVno6O5aLS7cCnb0tTlb8srhb9vdLZkSoCVCZLVjik5s2TLfpLsBKrTP\n" +
                "leVY3n9TBZH+vyKLHt4WHR23Z+74xDsuXunoPGXQVV8ymqTtfohaoM19jP99vjY7\n" +
                "DL/289XjMSfyPFqlpU4JDM7lY/kJSKB/C4eQglT8Sgm0h/kj5hdT2uMJBIQZIJVv\n" +
                "241fAVUPgrYAESOMm2TVA9r1OzeoUNlKw+e3+vjTR6sfDDp/iRKcEVQX4u9+CxZp\n" +
                "9g==\n-----END CERTIFICATE-----";
        DirectDownloadCertificateResponse response = new DirectDownloadCertificateResponse();
        helper.handleCertificateResponse(certStr, response);
        assertEquals("3", response.getVersion());
        assertEquals("CN=*.apache.org", response.getSubject());
    }
}
