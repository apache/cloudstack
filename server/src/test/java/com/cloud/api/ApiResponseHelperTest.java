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

import com.cloud.capacity.Capacity;
import com.cloud.configuration.Resource;
import com.cloud.domain.DomainVO;
import com.cloud.network.PublicIpQuarantine;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmGroupVO;
import com.cloud.network.as.AutoScaleVmProfileVO;
import com.cloud.network.as.dao.AutoScaleVmGroupVmMapDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.usage.UsageVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserData;
import com.cloud.user.UserDataVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDataDao;
import com.cloud.utils.net.Ip;
import com.cloud.vm.NicSecondaryIp;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.response.AutoScaleVmGroupResponse;
import org.apache.cloudstack.api.response.AutoScaleVmProfileResponse;
import org.apache.cloudstack.api.response.DirectDownloadCertificateResponse;
import org.apache.cloudstack.api.response.IpQuarantineResponse;
import org.apache.cloudstack.api.response.NicSecondaryIpResponse;
import org.apache.cloudstack.api.response.UnmanagedInstanceResponse;
import org.apache.cloudstack.api.response.UsageRecordResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.usage.UsageService;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiResponseHelperTest {

    @Mock
    UsageService usageService;

    ApiResponseHelper helper;

    @Mock
    AccountManager accountManagerMock;

    @Mock
    AnnotationDao annotationDaoMock;

    @Mock
    NetworkServiceMapDao ntwkSrvcDaoMock;

    @Mock
    AutoScaleVmGroupVmMapDao autoScaleVmGroupVmMapDaoMock;

    @Mock
    UserDataDao userDataDaoMock;

    @Mock
    IPAddressDao ipAddressDaoMock;

    @Spy
    @InjectMocks
    ApiResponseHelper apiResponseHelper = new ApiResponseHelper();

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss ZZZ");

    static long zoneId = 1L;
    static long domainId = 2L;
    static long accountId = 3L;
    static long serviceOfferingId = 4L;
    static long templateId  = 5L;
    static String userdata = "userdata";
    static long userdataId = 6L;
    static String userdataDetails = "userdataDetails";
    static String userdataNew = "userdataNew";

    static long autoScaleUserId = 7L;

    @Before
    public void injectMocks() throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field usageSvcField = ApiResponseHelper.class
                .getDeclaredField("_usageSvc");
        usageSvcField.setAccessible(true);
        helper = new ApiResponseHelper();
        usageSvcField.set(helper, usageService);
    }

    @Before
    public void setup() {
        AccountVO account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(1);
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);

        CallContext.register(user, account);
    }

    @After
    public void cleanup() {
        CallContext.unregister();
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

        try (MockedStatic<ApiDBUtils> ignored = Mockito.mockStatic(ApiDBUtils.class)) {
            when(ApiDBUtils.findAccountById(anyLong())).thenReturn(account);
            when(ApiDBUtils.findDomainById(anyLong())).thenReturn(domain);

            UsageRecordResponse MockResponse = helper.createUsageResponse(usage);
            assertEquals("DomainName", MockResponse.getDomainName());
        }
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

    @Test
    public void testAutoScaleVmGroupResponse() {
        AutoScaleVmGroupVO vmGroup = new AutoScaleVmGroupVO(1L, 2L, 3L, 4L, "test", 5, 6, 7, 8, new Date(), 9L, AutoScaleVmGroup.State.ENABLED);

        try (MockedStatic<ApiDBUtils> ignored = Mockito.mockStatic(ApiDBUtils.class)) {
            when(ApiDBUtils.findAutoScaleVmProfileById(anyLong())).thenReturn(null);
            when(ApiDBUtils.findLoadBalancerById(anyLong())).thenReturn(null);
            when(ApiDBUtils.findAccountById(anyLong())).thenReturn(new AccountVO());
            when(ApiDBUtils.findDomainById(anyLong())).thenReturn(new DomainVO());
            when(ApiDBUtils.countAvailableVmsByGroupId(anyLong())).thenReturn(9);

            AutoScaleVmGroupResponse response = apiResponseHelper.createAutoScaleVmGroupResponse(vmGroup);
            assertEquals("test", response.getName());
            assertEquals(5, response.getMinMembers());
            assertEquals(6, response.getMaxMembers());
            assertEquals(8, response.getInterval());
            assertEquals(9, response.getAvailableVirtualMachineCount());
            assertEquals(AutoScaleVmGroup.State.ENABLED.toString(), response.getState());

            assertNull(response.getNetworkName());
            assertNull(response.getLbProvider());
            assertNull(response.getPublicIp());
            assertNull(response.getPublicPort());
            assertNull(response.getPrivatePort());
        }
    }

    @Test
    public void testAutoScaleVmGroupResponseWithNetwork() {
        AutoScaleVmGroupVO vmGroup = new AutoScaleVmGroupVO(1L, 2L, 3L, 4L, "test", 5, 6, 7, 8, new Date(), 9L, AutoScaleVmGroup.State.ENABLED);

        LoadBalancerVO lb = new LoadBalancerVO(null, null, null, 0L, 8080, 8081, null, 0L, 0L, 1L, null, null);
        NetworkVO network = new NetworkVO(1L, null, null, null, 2L, 1L, 2L, 3L,
                "testnetwork", "displaytext", "networkdomain", null, 1L, null, null, false, null, false);
        IPAddressVO ipAddressVO = new IPAddressVO(new Ip("10.10.10.10"), 1L, 1L, 1L,false);

        try (MockedStatic<ApiDBUtils> ignored = Mockito.mockStatic(ApiDBUtils.class)) {
            when(ApiDBUtils.findAutoScaleVmProfileById(anyLong())).thenReturn(null);
            when(ApiDBUtils.findAccountById(anyLong())).thenReturn(new AccountVO());
            when(ApiDBUtils.findDomainById(anyLong())).thenReturn(new DomainVO());
            when(ApiDBUtils.findLoadBalancerById(anyLong())).thenReturn(lb);

            when(ApiDBUtils.findNetworkById(anyLong())).thenReturn(network);
            when(ntwkSrvcDaoMock.getProviderForServiceInNetwork(anyLong(), any())).thenReturn("VirtualRouter");
            when(ApiDBUtils.findIpAddressById(anyLong())).thenReturn(ipAddressVO);

            AutoScaleVmGroupResponse response = apiResponseHelper.createAutoScaleVmGroupResponse(vmGroup);
            assertEquals("test", response.getName());
            assertEquals(5, response.getMinMembers());
            assertEquals(6, response.getMaxMembers());
            assertEquals(8, response.getInterval());
            assertEquals(AutoScaleVmGroup.State.ENABLED.toString(), response.getState());

            assertEquals("testnetwork", response.getNetworkName());
            assertEquals("VirtualRouter", response.getLbProvider());
            assertEquals("10.10.10.10", response.getPublicIp());
            assertEquals("8080", response.getPublicPort());
            assertEquals("8081", response.getPrivatePort());
        }
    }

    @Test
    public void testAutoScaleVmProfileResponse() {
        AutoScaleVmProfileVO vmProfile = new AutoScaleVmProfileVO(zoneId, domainId, accountId, serviceOfferingId, templateId, null, null, userdata, null, autoScaleUserId);
        vmProfile.setUserDataId(userdataId);
        vmProfile.setUserDataDetails(userdataDetails);

        try (MockedStatic<ApiDBUtils> ignored = Mockito.mockStatic(ApiDBUtils.class)) {
            when(ApiDBUtils.findAccountById(anyLong())).thenReturn(new AccountVO());
            when(ApiDBUtils.findDomainById(anyLong())).thenReturn(new DomainVO());

            UserData.UserDataOverridePolicy templatePolicy = UserData.UserDataOverridePolicy.APPEND;
            VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
            when(ApiDBUtils.findTemplateById(anyLong())).thenReturn(templateVO);
            when(templateVO.getUserDataOverridePolicy()).thenReturn(templatePolicy);

            UserDataVO userDataVO = Mockito.mock(UserDataVO.class);
            String userDataUuid = "userDataUuid";
            String userDataName = "userDataName";
            when(userDataDaoMock.findById(anyLong())).thenReturn(userDataVO);
            when(userDataVO.getUuid()).thenReturn(userDataUuid);
            when(userDataVO.getName()).thenReturn(userDataName);

            AutoScaleVmProfileResponse response = apiResponseHelper.createAutoScaleVmProfileResponse(vmProfile);
            assertEquals(templatePolicy.toString(), response.getUserDataPolicy());
            assertEquals(userdata, response.getUserData());
            assertEquals(userDataUuid, response.getUserDataId());
            assertEquals(userDataName, response.getUserDataName());
            assertEquals(userdataDetails, response.getUserDataDetails());
        }
    }

    @Test
    public void testAutoScaleVmProfileResponseWithoutUserData() {
        AutoScaleVmProfileVO vmProfile = new AutoScaleVmProfileVO(zoneId, domainId, accountId, serviceOfferingId, templateId, null, null, null, null, autoScaleUserId);

        try (MockedStatic<ApiDBUtils> ignored = Mockito.mockStatic(ApiDBUtils.class)) {
            when(ApiDBUtils.findAccountById(anyLong())).thenReturn(new AccountVO());
            when(ApiDBUtils.findDomainById(anyLong())).thenReturn(new DomainVO());

            VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
            when(ApiDBUtils.findTemplateById(anyLong())).thenReturn(templateVO);

            AutoScaleVmProfileResponse response = apiResponseHelper.createAutoScaleVmProfileResponse(vmProfile);
            assertNull(response.getUserDataPolicy());
            assertNull(response.getUserData());
            assertNull(response.getUserDataId());
            assertNull(response.getUserDataName());
            assertNull(response.getUserDataDetails());
        }
    }

    private UnmanagedInstanceTO getUnmanagedInstaceForTests() {
        UnmanagedInstanceTO instance = Mockito.mock(UnmanagedInstanceTO.class);
        Mockito.when(instance.getPowerState()).thenReturn(UnmanagedInstanceTO.PowerState.PowerOff);
        Mockito.when(instance.getClusterName()).thenReturn("CL1");
        UnmanagedInstanceTO.Disk disk = Mockito.mock(UnmanagedInstanceTO.Disk.class);
        Mockito.when(disk.getDiskId()).thenReturn("0");
        Mockito.when(disk.getLabel()).thenReturn("Hard disk 1");
        Mockito.when(disk.getCapacity()).thenReturn(17179869184L);
        Mockito.when(disk.getPosition()).thenReturn(0);
        Mockito.when(instance.getDisks()).thenReturn(List.of(disk));
        UnmanagedInstanceTO.Nic nic = Mockito.mock(UnmanagedInstanceTO.Nic.class);
        Mockito.when(nic.getNicId()).thenReturn("Network adapter 1");
        Mockito.when(nic.getMacAddress()).thenReturn("aa:bb:cc:dd:ee:ff");
        Mockito.when(instance.getNics()).thenReturn(List.of(nic));
        return instance;
    }

    @Test
    public void testCreateUnmanagedInstanceResponseVmwareDcVms() {
        UnmanagedInstanceTO instance = getUnmanagedInstaceForTests();
        UnmanagedInstanceResponse response = apiResponseHelper.createUnmanagedInstanceResponse(instance, null, null);
        Assert.assertEquals(1, response.getDisks().size());
        Assert.assertEquals(1, response.getNics().size());
    }

    @Test
    public void createQuarantinedIpsResponseTestReturnsObject() {
        String quarantinedIpUuid = "quarantined_ip_uuid";
        Long previousOwnerId = 300L;
        String previousOwnerUuid = "previous_owner_uuid";
        String previousOwnerName = "previous_owner_name";
        Long removerAccountId = 400L;
        String removerAccountUuid = "remover_account_uuid";
        Long publicIpAddressId = 500L;
        String publicIpAddress = "1.2.3.4";
        Date created = new Date(599L);
        Date removed = new Date(600L);
        Date endDate = new Date(601L);
        String removalReason = "removalReason";

        PublicIpQuarantine quarantinedIpMock = Mockito.mock(PublicIpQuarantine.class);
        IPAddressVO ipAddressVoMock = Mockito.mock(IPAddressVO.class);
        Account previousOwner = Mockito.mock(Account.class);
        Account removerAccount = Mockito.mock(Account.class);

        Mockito.when(quarantinedIpMock.getUuid()).thenReturn(quarantinedIpUuid);
        Mockito.when(quarantinedIpMock.getPreviousOwnerId()).thenReturn(previousOwnerId);
        Mockito.when(quarantinedIpMock.getPublicIpAddressId()).thenReturn(publicIpAddressId);
        Mockito.doReturn(ipAddressVoMock).when(ipAddressDaoMock).findById(publicIpAddressId);
        Mockito.when(ipAddressVoMock.getAddress()).thenReturn(new Ip(publicIpAddress));
        Mockito.doReturn(previousOwner).when(accountManagerMock).getAccount(previousOwnerId);
        Mockito.when(previousOwner.getUuid()).thenReturn(previousOwnerUuid);
        Mockito.when(previousOwner.getName()).thenReturn(previousOwnerName);
        Mockito.when(quarantinedIpMock.getCreated()).thenReturn(created);
        Mockito.when(quarantinedIpMock.getRemoved()).thenReturn(removed);
        Mockito.when(quarantinedIpMock.getEndDate()).thenReturn(endDate);
        Mockito.when(quarantinedIpMock.getRemovalReason()).thenReturn(removalReason);
        Mockito.when(quarantinedIpMock.getRemoverAccountId()).thenReturn(removerAccountId);
        Mockito.when(removerAccount.getUuid()).thenReturn(removerAccountUuid);
        Mockito.doReturn(removerAccount).when(accountManagerMock).getAccount(removerAccountId);

        IpQuarantineResponse result = apiResponseHelper.createQuarantinedIpsResponse(quarantinedIpMock);

        Assert.assertEquals(quarantinedIpUuid, result.getId());
        Assert.assertEquals(publicIpAddress, result.getPublicIpAddress());
        Assert.assertEquals(previousOwnerUuid, result.getPreviousOwnerId());
        Assert.assertEquals(previousOwnerName, result.getPreviousOwnerName());
        Assert.assertEquals(created, result.getCreated());
        Assert.assertEquals(removed, result.getRemoved());
        Assert.assertEquals(endDate, result.getEndDate());
        Assert.assertEquals(removalReason, result.getRemovalReason());
        Assert.assertEquals(removerAccountUuid, result.getRemoverAccountId());
        Assert.assertEquals("quarantinedip", result.getResponseName());
    }

    @Test
    public void testCapacityListingForSingleTag() {
        Capacity c1 = Mockito.mock(Capacity.class);
        Mockito.when(c1.getTag()).thenReturn("tag1");
        Capacity c2 = Mockito.mock(Capacity.class);
        Mockito.when(c2.getTag()).thenReturn("tag1");
        Capacity c3 = Mockito.mock(Capacity.class);
        Mockito.when(c3.getTag()).thenReturn("tag2");
        Capacity c4 = Mockito.mock(Capacity.class);
        Assert.assertTrue(apiResponseHelper.capacityListingForSingleTag(List.of(c1, c2)));
        Assert.assertFalse(apiResponseHelper.capacityListingForSingleTag(List.of(c1, c2, c3)));
        Assert.assertFalse(apiResponseHelper.capacityListingForSingleTag(List.of(c4, c2, c3)));
    }

    @Test
    public void testCapacityListingForSingleNonGpuType() {
        Capacity c1 = Mockito.mock(Capacity.class);
        Mockito.when(c1.getCapacityType()).thenReturn((short)Resource.ResourceType.user_vm.getOrdinal());
        Capacity c2 = Mockito.mock(Capacity.class);
        Mockito.when(c2.getCapacityType()).thenReturn((short)Resource.ResourceType.user_vm.getOrdinal());
        Capacity c3 = Mockito.mock(Capacity.class);
        Mockito.when(c3.getCapacityType()).thenReturn((short)Resource.ResourceType.volume.getOrdinal());
        Capacity c4 = Mockito.mock(Capacity.class);
        Assert.assertTrue(apiResponseHelper.capacityListingForSingleNonGpuType(List.of(c1, c2)));
        Assert.assertFalse(apiResponseHelper.capacityListingForSingleNonGpuType(List.of(c1, c2, c3)));
    }
}
