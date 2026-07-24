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

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.AutoScaleVmGroupResponse;
import org.apache.cloudstack.api.response.AutoScaleVmProfileResponse;
import org.apache.cloudstack.api.response.ConsoleSessionResponse;
import org.apache.cloudstack.api.response.DirectDownloadCertificateResponse;
import org.apache.cloudstack.api.response.GuestOSCategoryResponse;
import org.apache.cloudstack.api.response.IpQuarantineResponse;
import org.apache.cloudstack.api.response.NicSecondaryIpResponse;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UnmanagedInstanceResponse;
import org.apache.cloudstack.api.response.UsageRecordResponse;
import org.apache.cloudstack.api.response.TrafficTypeResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.usage.Usage;
import org.apache.cloudstack.usage.UsageService;
import org.apache.cloudstack.usage.UsageTypes;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;

import com.cloud.capacity.Capacity;
import com.cloud.configuration.Resource;
import com.cloud.domain.DomainVO;
import com.cloud.host.HostVO;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.network.PublicIpQuarantine;
import com.cloud.network.VpnUserVO;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmGroupVO;
import com.cloud.network.as.AutoScaleVmProfileVO;
import com.cloud.network.as.dao.AutoScaleVmGroupVmMapDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.resource.icon.ResourceIconVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceIconManager;
import com.cloud.server.ResourceTag;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOsCategory;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.usage.UsageVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserData;
import com.cloud.user.UserDataVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDataDao;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.net.Ip;
import com.cloud.vm.ConsoleSessionVO;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.VMInstanceVO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock
    ResourceIconManager resourceIconManager;

    @Mock
    EntityManager entityManagerMock;

    @Mock
    GuestOSDao guestOSDaoMock;

    @Mock
    GuestOSCategoryDao guestOSCategoryDaoMock;

    @Mock
    private ConsoleSessionVO consoleSessionMock;
    @Mock
    private DomainVO domainVOMock;
    @Mock
    private UserVO userVOMock;
    @Mock
    private AccountVO accountVOMock;
    @Mock
    private HostVO hostVOMock;
    @Mock
    private VMInstanceVO vmInstanceVOMock;

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

    @BeforeEach
    public void injectMocks() {
        helper = new ApiResponseHelper();
        ReflectionTestUtils.setField(helper, "_usageSvc", usageService);
        ReflectionTestUtils.setField(helper, "_entityMgr", entityManagerMock);
        ReflectionTestUtils.setField(helper, "_guestOsDao", guestOSDaoMock);
        ReflectionTestUtils.setField(helper, "_guestOsCategoryDao", guestOSCategoryDaoMock);
    }

    @BeforeEach
    public void setup() {
        AccountVO account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(1);
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);

        CallContext.register(user, account);
    }

    @AfterEach
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

    @Test
    public void testCreateTrafficTypeResponse() {
        PhysicalNetworkVO pnet = new PhysicalNetworkVO();
        pnet.addIsolationMethod("VXLAN");
        pnet.addIsolationMethod("STT");

        try (MockedStatic<ApiDBUtils> ignored = Mockito.mockStatic(ApiDBUtils.class)) {
            when(ApiDBUtils.findPhysicalNetworkById(anyLong())).thenReturn(pnet);
            String xenLabel = "xen";
            String kvmLabel = "kvm";
            String vmwareLabel = "vmware";
            String simulatorLabel = "simulator";
            String hypervLabel = "hyperv";
            String ovmLabel = "ovm";
            String vlan = "vlan";
            String trafficType = "Public";
            PhysicalNetworkTrafficType pnetTrafficType = new PhysicalNetworkTrafficTypeVO(pnet.getId(), Networks.TrafficType.getTrafficType(trafficType), xenLabel, kvmLabel, vmwareLabel, simulatorLabel, vlan, hypervLabel, ovmLabel);

            TrafficTypeResponse response = apiResponseHelper.createTrafficTypeResponse(pnetTrafficType);
            assertFalse(UUID.fromString(response.getId()).toString().isEmpty());
            assertEquals(response.getphysicalNetworkId(), pnet.getUuid());
            assertEquals(response.getTrafficType(), trafficType);
            assertEquals(response.getXenLabel(), xenLabel);
            assertEquals(response.getKvmLabel(), kvmLabel);
            assertEquals(response.getVmwareLabel(), vmwareLabel);
            assertEquals(response.getHypervLabel(), hypervLabel);
            assertEquals(response.getOvm3Label(), ovmLabel);
            assertEquals(response.getVlan(), vlan);
            assertEquals(response.getIsolationMethods(), "VXLAN,STT");

        }
    }

    private UnmanagedInstanceTO getUnmanagedInstanceForTests() {
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
        UnmanagedInstanceTO instance = getUnmanagedInstanceForTests();
        UnmanagedInstanceResponse response = apiResponseHelper.createUnmanagedInstanceResponse(instance, null, null);
        Assertions.assertEquals(1, response.getDisks().size());
        Assertions.assertEquals(1, response.getNics().size());
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

        Assertions.assertEquals(quarantinedIpUuid, result.getId());
        Assertions.assertEquals(publicIpAddress, result.getPublicIpAddress());
        Assertions.assertEquals(previousOwnerUuid, result.getPreviousOwnerId());
        Assertions.assertEquals(previousOwnerName, result.getPreviousOwnerName());
        Assertions.assertEquals(created, result.getCreated());
        Assertions.assertEquals(removed, result.getRemoved());
        Assertions.assertEquals(endDate, result.getEndDate());
        Assertions.assertEquals(removalReason, result.getRemovalReason());
        Assertions.assertEquals(removerAccountUuid, result.getRemoverAccountId());
        Assertions.assertEquals("quarantinedip", result.getResponseName());
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
        Assertions.assertTrue(apiResponseHelper.capacityListingForSingleTag(List.of(c1, c2)));
        Assertions.assertFalse(apiResponseHelper.capacityListingForSingleTag(List.of(c1, c2, c3)));
        Assertions.assertFalse(apiResponseHelper.capacityListingForSingleTag(List.of(c4, c2, c3)));
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
        Assertions.assertTrue(apiResponseHelper.capacityListingForSingleNonGpuType(List.of(c1, c2)));
        Assertions.assertFalse(apiResponseHelper.capacityListingForSingleNonGpuType(List.of(c1, c2, c3)));
    }

    @Test
    public void testCreateGuestOSCategoryResponse_WithResourceIcon() {
        GuestOsCategory guestOsCategory = Mockito.mock(GuestOsCategory.class);
        ResourceIconVO resourceIconVO = Mockito.mock(ResourceIconVO.class);
        String uuid = UUID.randomUUID().toString();
        String name = "Ubuntu";
        boolean featured = true;
        Mockito.when(guestOsCategory.getUuid()).thenReturn(uuid);
        Mockito.when(guestOsCategory.getName()).thenReturn(name);
        Mockito.when(guestOsCategory.isFeatured()).thenReturn(featured);
        ResourceIconResponse mockIconResponse = Mockito.mock(ResourceIconResponse.class);
        try (MockedStatic<ApiDBUtils> ignored = Mockito.mockStatic(ApiDBUtils.class)) {
            Mockito.when(ApiDBUtils.getResourceIconByResourceUUID(uuid, ResourceTag.ResourceObjectType.GuestOsCategory)).thenReturn(resourceIconVO);
            Mockito.when(ApiDBUtils.newResourceIconResponse(resourceIconVO)).thenReturn(mockIconResponse);
            GuestOSCategoryResponse response = apiResponseHelper.createGuestOSCategoryResponse(guestOsCategory);
            Assertions.assertNotNull(response);
            Assertions.assertEquals(uuid, response.getId());
            Assertions.assertEquals(name, response.getName());
            Object obj = ReflectionTestUtils.getField(response, "featured");
            if (obj == null) {
                Assertions.fail("Invalid featured value");
            }
            Assertions.assertTrue((Boolean)obj);
            obj = ReflectionTestUtils.getField(response, "resourceIconResponse");
            Assertions.assertNotNull(obj);
            Assertions.assertEquals("oscategory", response.getObjectName());
        }
    }

    @Test
    public void testCreateGuestOSCategoryResponse_WithoutResourceIcon() {
        GuestOsCategory guestOsCategory = Mockito.mock(GuestOsCategory.class);
        String uuid = "1234";
        String name = "Ubuntu";
        boolean featured = false;
        Mockito.when(guestOsCategory.getUuid()).thenReturn(uuid);
        Mockito.when(guestOsCategory.getName()).thenReturn(name);
        Mockito.when(guestOsCategory.isFeatured()).thenReturn(featured);
        try (MockedStatic<ApiDBUtils> ignored = Mockito.mockStatic(ApiDBUtils.class)) {
            when(ApiDBUtils.getResourceIconByResourceUUID(uuid, ResourceTag.ResourceObjectType.GuestOsCategory)).thenReturn(null);
            GuestOSCategoryResponse response = apiResponseHelper.createGuestOSCategoryResponse(guestOsCategory);
            Assertions.assertNotNull(response);
            Assertions.assertEquals(uuid, response.getId());
            Assertions.assertEquals(name, response.getName());
            Object obj = ReflectionTestUtils.getField(response, "featured");
            if (obj == null) {
                Assertions.fail("Invalid featured value");
            }
            Assertions.assertFalse((Boolean)obj);
            obj = ReflectionTestUtils.getField(response, "resourceIconResponse");
            Assertions.assertNull(obj);
            Assertions.assertEquals("oscategory", response.getObjectName());
        }
    }

    @Test
    public void testCreateGuestOSCategoryResponse_WithShowIconFalse() {
        GuestOsCategory guestOsCategory = Mockito.mock(GuestOsCategory.class);
        Mockito.when(guestOsCategory.getUuid()).thenReturn(UUID.randomUUID().toString());
        try (MockedStatic<ApiDBUtils> mockedStatic = Mockito.mockStatic(ApiDBUtils.class)) {
            GuestOSCategoryResponse response = apiResponseHelper.createGuestOSCategoryResponse(guestOsCategory, false);
            Assertions.assertNotNull(response);
            mockedStatic.verify(() -> ApiDBUtils.getResourceIconByResourceUUID(Mockito.any(), Mockito.any()),
                    Mockito.never());
        }
    }

    @Test
    public void testGetResourceIconsUsingOsCategory_withValidData() {
        TemplateResponse template1 = Mockito.mock(TemplateResponse.class);
        when(template1.getId()).thenReturn("t1");
        when(template1.getOsTypeCategoryId()).thenReturn(100L);
        TemplateResponse template2 = Mockito.mock(TemplateResponse.class);
        when(template2.getId()).thenReturn("t2");
        when(template2.getOsTypeCategoryId()).thenReturn(200L);
        List<TemplateResponse> responses = Arrays.asList(template1, template2);
        Map<Long, ResourceIcon> icons = new HashMap<>();
        ResourceIcon icon1 = Mockito.mock(ResourceIcon.class);
        ResourceIcon icon2 = Mockito.mock(ResourceIcon.class);
        icons.put(100L, icon1);
        icons.put(200L, icon2);
        when(resourceIconManager.getByResourceTypeAndIds(Mockito.eq(ResourceTag.ResourceObjectType.GuestOsCategory), Mockito.anySet()))
                .thenReturn(icons);
        Map<String, ResourceIcon> result = apiResponseHelper.getResourceIconsUsingOsCategory(responses);
        assertEquals(2, result.size());
        assertEquals(icon1, result.get("t1"));
        assertEquals(icon2, result.get("t2"));
    }

    @Test
    public void testGetResourceIconsUsingOsCategory_missingIcons() {
        TemplateResponse template1 = Mockito.mock(TemplateResponse.class);
        when(template1.getId()).thenReturn("t1");
        when(template1.getOsTypeCategoryId()).thenReturn(100L);
        List<TemplateResponse> responses = List.of(template1);
        when(resourceIconManager.getByResourceTypeAndIds(Mockito.eq(ResourceTag.ResourceObjectType.GuestOsCategory), Mockito.anySet())).thenReturn(Collections.emptyMap());
        Map<String, ResourceIcon> result = apiResponseHelper.getResourceIconsUsingOsCategory(responses);
        assertTrue(result.containsKey("t1"));
        assertNull(result.get("t1"));
    }

    @Test
    public void testUpdateTemplateIsoResponsesForIcons_withMixedIcons() {
        TemplateResponse template1 = Mockito.mock(TemplateResponse.class);
        when(template1.getId()).thenReturn("t1");
        TemplateResponse template2 = Mockito.mock(TemplateResponse.class);
        when(template2.getId()).thenReturn("t2");
        List<TemplateResponse> responses = Arrays.asList(template1, template2);
        Map<String, ResourceIcon> isoIcons = new HashMap<>();
        isoIcons.put("t1", Mockito.mock(ResourceIcon.class));
        when(resourceIconManager.getByResourceTypeAndUuids(ResourceTag.ResourceObjectType.ISO, Set.of("t1", "t2")))
                .thenReturn(isoIcons);
        Map<String, ResourceIcon> fallbackIcons = Map.of("t2", Mockito.mock(ResourceIcon.class));
        Mockito.doReturn(fallbackIcons).when(apiResponseHelper).getResourceIconsUsingOsCategory(Mockito.anyList());
        ResourceIconResponse iconResponse1 = new ResourceIconResponse();
        ResourceIconResponse iconResponse2 = new ResourceIconResponse();
        Mockito.doReturn(iconResponse1).when(apiResponseHelper).createResourceIconResponse(isoIcons.get("t1"));
        Mockito.doReturn(iconResponse2).when(apiResponseHelper).createResourceIconResponse(fallbackIcons.get("t2"));
        apiResponseHelper.updateTemplateIsoResponsesForIcons(responses, ResourceTag.ResourceObjectType.ISO);
        verify(template1).setResourceIconResponse(iconResponse1);
        verify(template2).setResourceIconResponse(iconResponse2);
    }

    @Test
    public void testUpdateTemplateIsoResponsesForIcons_emptyInput() {
        apiResponseHelper.updateTemplateIsoResponsesForIcons(Collections.emptyList(),
                ResourceTag.ResourceObjectType.Template);
        Mockito.verify(resourceIconManager, Mockito.never()).getByResourceTypeAndUuids(Mockito.any(),
                Mockito.anyCollection());
    }

    private ConsoleSessionResponse getExpectedConsoleSessionResponseForTests(boolean fullView) {
        ConsoleSessionResponse expected = new ConsoleSessionResponse();
        expected.setId("uuid");
        expected.setCreated(new Date());
        expected.setAcquired(new Date());
        expected.setRemoved(new Date());
        expected.setConsoleEndpointCreatorAddress("127.0.0.1");
        expected.setClientAddress("127.0.0.1");

        if (fullView) {
            expected.setDomain("domain");
            expected.setDomainPath("domainPath");
            expected.setDomainId("domainUuid");
            expected.setUser("user");
            expected.setUserId("userUuid");
            expected.setAccount("account");
            expected.setAccountId("accountUuid");
            expected.setHostName("host");
            expected.setHostId("hostUuid");
            expected.setVmId("vmUuid");
            expected.setVmName("vmName");
        }

        return expected;
    }

    @Test
    public void createConsoleSessionResponseTestShouldReturnRestrictedResponse() {
        ConsoleSessionResponse expected = getExpectedConsoleSessionResponseForTests(false);

        try (MockedStatic<ApiDBUtils> apiDBUtilsStaticMock = Mockito.mockStatic(ApiDBUtils.class)) {
            Mockito.when(consoleSessionMock.getUuid()).thenReturn(expected.getId());
            Mockito.when(consoleSessionMock.getDomainId()).thenReturn(2L);
            Mockito.when(consoleSessionMock.getCreated()).thenReturn(expected.getCreated());
            Mockito.when(consoleSessionMock.getAcquired()).thenReturn(expected.getAcquired());
            Mockito.when(consoleSessionMock.getRemoved()).thenReturn(expected.getRemoved());
            Mockito.when(consoleSessionMock.getConsoleEndpointCreatorAddress()).thenReturn(expected.getConsoleEndpointCreatorAddress());
            Mockito.when(consoleSessionMock.getClientAddress()).thenReturn(expected.getClientAddress());

            ConsoleSessionResponse response = apiResponseHelper.createConsoleSessionResponse(consoleSessionMock, ResponseObject.ResponseView.Restricted);

            Assertions.assertEquals(expected.getId(), response.getId());
            Assertions.assertEquals(expected.getCreated(), response.getCreated());
            Assertions.assertEquals(expected.getAcquired(), response.getAcquired());
            Assertions.assertEquals(expected.getRemoved(), response.getRemoved());
            Assertions.assertEquals(expected.getConsoleEndpointCreatorAddress(), response.getConsoleEndpointCreatorAddress());
            Assertions.assertEquals(expected.getClientAddress(), response.getClientAddress());
        }
    }

    @Test
    public void createConsoleSessionResponseTestShouldReturnFullResponse() {
        ConsoleSessionResponse expected = getExpectedConsoleSessionResponseForTests(true);

        try (MockedStatic<ApiDBUtils> apiDBUtilsStaticMock = Mockito.mockStatic(ApiDBUtils.class)) {
            Mockito.when(consoleSessionMock.getUuid()).thenReturn(expected.getId());
            Mockito.when(consoleSessionMock.getDomainId()).thenReturn(2L);
            Mockito.when(consoleSessionMock.getAccountId()).thenReturn(2L);
            Mockito.when(consoleSessionMock.getUserId()).thenReturn(2L);
            Mockito.when(consoleSessionMock.getHostId()).thenReturn(2L);
            Mockito.when(consoleSessionMock.getInstanceId()).thenReturn(2L);
            Mockito.when(consoleSessionMock.getCreated()).thenReturn(expected.getCreated());
            Mockito.when(consoleSessionMock.getAcquired()).thenReturn(expected.getAcquired());
            Mockito.when(consoleSessionMock.getRemoved()).thenReturn(expected.getRemoved());
            Mockito.when(consoleSessionMock.getConsoleEndpointCreatorAddress()).thenReturn(expected.getConsoleEndpointCreatorAddress());
            Mockito.when(consoleSessionMock.getClientAddress()).thenReturn(expected.getClientAddress());

            apiDBUtilsStaticMock.when(() -> ApiDBUtils.findDomainById(2L)).thenReturn(domainVOMock);
            Mockito.when(domainVOMock.getName()).thenReturn(expected.getDomain());
            Mockito.when(domainVOMock.getPath()).thenReturn(expected.getDomainPath());
            Mockito.when(domainVOMock.getUuid()).thenReturn(expected.getDomainId());

            Mockito.when(apiResponseHelper.findUserById(2L)).thenReturn(userVOMock);
            Mockito.when(userVOMock.getUsername()).thenReturn(expected.getUser());
            Mockito.when(userVOMock.getUuid()).thenReturn(expected.getUserId());

            Mockito.when(ApiDBUtils.findAccountById(2L)).thenReturn(accountVOMock);
            Mockito.when(accountVOMock.getAccountName()).thenReturn(expected.getAccount());
            Mockito.when(accountVOMock.getUuid()).thenReturn(expected.getAccountId());

            Mockito.when(apiResponseHelper.findHostById(2L)).thenReturn(hostVOMock);
            Mockito.when(hostVOMock.getUuid()).thenReturn(expected.getHostId());
            Mockito.when(hostVOMock.getName()).thenReturn(expected.getHostName());

            apiDBUtilsStaticMock.when(() -> ApiDBUtils.findVMInstanceById(2L)).thenReturn(vmInstanceVOMock);
            Mockito.when(vmInstanceVOMock.getUuid()).thenReturn(expected.getVmId());
            Mockito.when(vmInstanceVOMock.getInstanceName()).thenReturn(expected.getVmName());

            ConsoleSessionResponse response = apiResponseHelper.createConsoleSessionResponse(consoleSessionMock, ResponseObject.ResponseView.Full);

            Assertions.assertEquals(expected.getId(), response.getId());
            Assertions.assertEquals(expected.getCreated(), response.getCreated());
            Assertions.assertEquals(expected.getAcquired(), response.getAcquired());
            Assertions.assertEquals(expected.getRemoved(), response.getRemoved());
            Assertions.assertEquals(expected.getConsoleEndpointCreatorAddress(), response.getConsoleEndpointCreatorAddress());
            Assertions.assertEquals(expected.getClientAddress(), response.getClientAddress());
            Assertions.assertEquals(expected.getDomain(), response.getDomain());
            Assertions.assertEquals(expected.getDomainPath(), response.getDomainPath());
            Assertions.assertEquals(expected.getDomainId(), response.getDomainId());
            Assertions.assertEquals(expected.getUser(), response.getUser());
            Assertions.assertEquals(expected.getUserId(), response.getUserId());
            Assertions.assertEquals(expected.getAccount(), response.getAccount());
            Assertions.assertEquals(expected.getAccountId(), response.getAccountId());
            Assertions.assertEquals(expected.getHostId(), response.getHostId());
            Assertions.assertEquals(expected.getHostName(), response.getHostName());
            Assertions.assertEquals(expected.getVmId(), response.getVmId());
            Assertions.assertEquals(expected.getVmName(), response.getVmName());
        }
    }

    @Test
    @DisplayName("RUNNING_VM usage populates service offering, VM and OS details")
    public void populateRunningVmUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        ServiceOfferingVO serviceOffering = mock(ServiceOfferingVO.class);
        VMInstanceVO vmInstance = mock(VMInstanceVO.class);
        VMTemplateVO template = mock(VMTemplateVO.class);
        GuestOSVO guestOS = mock(GuestOSVO.class);
        GuestOSCategoryVO guestOSCategory = mock(GuestOSCategoryVO.class);
        Long usageId = 11L;
        Long offeringId = 21L;
        Long guestOSId = 31L;
        Long guestOSCategoryId = 41L;

        when(usageRecord.getUsageType()).thenReturn(UsageTypes.RUNNING_VM);
        when(usageRecord.getOfferingId()).thenReturn(offeringId);
        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(usageRecord.getVmInstanceId()).thenReturn(usageId);
        when(usageRecord.getType()).thenReturn("KVM");
        when(usageRecord.getCpuCores()).thenReturn(null);
        when(usageRecord.getCpuSpeed()).thenReturn(2400L);
        when(usageRecord.getMemory()).thenReturn(8192L);
        when(entityManagerMock.findByIdIncludingRemoved(ServiceOfferingVO.class, offeringId.toString())).thenReturn(serviceOffering);
        when(serviceOffering.getUuid()).thenReturn("service-offering-uuid");
        when(serviceOffering.getName()).thenReturn("Small Instance");
        when(serviceOffering.getCpu()).thenReturn(4);
        when(vmInstance.getUuid()).thenReturn("vm-uuid");
        when(vmInstance.getId()).thenReturn(usageId);
        when(vmInstance.getHostName()).thenReturn("vm-host");
        when(vmInstance.getInstanceName()).thenReturn("i-2-11-VM");
        when(vmInstance.getGuestOSId()).thenReturn(guestOSId);
        when(guestOSDaoMock.findById(guestOSId)).thenReturn(guestOS);
        when(guestOS.getUuid()).thenReturn("guest-os-uuid");
        when(guestOS.getDisplayName()).thenReturn("Ubuntu 22.04");
        when(guestOS.getCategoryId()).thenReturn(guestOSCategoryId);
        when(guestOSCategoryDaoMock.findById(guestOSCategoryId)).thenReturn(guestOSCategory);
        when(guestOSCategory.getUuid()).thenReturn("guest-os-category-uuid");
        when(guestOSCategory.getName()).thenReturn("Linux");
        when(template.getUuid()).thenReturn("template-uuid");
        when(template.getName()).thenReturn("Ubuntu Template");

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateRunningOrAllocatedVmUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class, VMInstanceVO.class, VMTemplateVO.class},
                usageRecord, response, false, vmInstance, template);

        // Assert
        assertResponseField(response, "offeringId", "service-offering-uuid");
        assertResponseField(response, "usageId", "vm-uuid");
        assertResponseField(response, "type", "KVM");
        assertResponseField(response, "cpuNumber", 4L);
        assertResponseField(response, "cpuSpeed", 2400L);
        assertResponseField(response, "memory", 8192L);
        assertResponseField(response, "osTypeId", "guest-os-uuid");
        assertResponseField(response, "osDisplayName", "Ubuntu 22.04");
        assertResponseField(response, "osCategoryId", "guest-os-category-uuid");
        assertResponseField(response, "osCategoryName", "Linux");
        assertDescriptionContains(response, "Running VM usage for vm-host (i-2-11-VM) (vm-uuid)");
        assertDescriptionContains(response, "using service offering Small Instance (service-offering-uuid)");
        assertDescriptionContains(response, "and template Ubuntu Template (template-uuid)");
        assertUsageResourceDetails(resourceDetails, ResourceTag.ResourceObjectType.UserVm, usageId);
        verify(entityManagerMock).findByIdIncludingRemoved(ServiceOfferingVO.class, offeringId.toString());
        verify(guestOSDaoMock).findById(guestOSId);
    }

    @Test
    @DisplayName("ALLOCATED_VM usage falls back to service offering compute details")
    public void populateAllocatedVmUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        ServiceOfferingVO serviceOffering = mock(ServiceOfferingVO.class);
        VMInstanceVO vmInstance = mock(VMInstanceVO.class);
        Long usageId = 12L;
        Long vmInstanceId = 22L;
        Long offeringId = 32L;

        when(usageRecord.getUsageType()).thenReturn(UsageTypes.ALLOCATED_VM);
        when(usageRecord.getOfferingId()).thenReturn(offeringId);
        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(usageRecord.getVmInstanceId()).thenReturn(vmInstanceId);
        when(usageRecord.getType()).thenReturn("KVM");
        when(usageRecord.getCpuCores()).thenReturn(null);
        when(usageRecord.getCpuSpeed()).thenReturn(null);
        when(usageRecord.getMemory()).thenReturn(null);
        when(entityManagerMock.findByIdIncludingRemoved(ServiceOfferingVO.class, offeringId.toString())).thenReturn(serviceOffering);
        when(entityManagerMock.findByIdIncludingRemoved(VMInstanceVO.class, usageId.toString())).thenReturn(vmInstance);
        when(serviceOffering.getUuid()).thenReturn("allocated-service-offering-uuid");
        when(serviceOffering.getName()).thenReturn("Medium Instance");
        when(serviceOffering.getCpu()).thenReturn(2);
        when(serviceOffering.getSpeed()).thenReturn(1800);
        when(serviceOffering.getRamSize()).thenReturn(4096);
        when(vmInstance.getUuid()).thenReturn("allocated-vm-uuid");
        when(vmInstance.getId()).thenReturn(usageId);
        when(vmInstance.getHostName()).thenReturn("allocated-vm-host");
        when(vmInstance.getInstanceName()).thenReturn("i-2-12-VM");

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateRunningOrAllocatedVmUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class, VMInstanceVO.class, VMTemplateVO.class},
                usageRecord, response, false, null, null);

        // Assert
        assertResponseField(response, "offeringId", "allocated-service-offering-uuid");
        assertResponseField(response, "usageId", "allocated-vm-uuid");
        assertResponseField(response, "type", "KVM");
        assertResponseField(response, "cpuNumber", 2L);
        assertResponseField(response, "cpuSpeed", 1800L);
        assertResponseField(response, "memory", 4096L);
        assertDescriptionContains(response, "Allocated VM usage for allocated-vm-host (i-2-12-VM) (allocated-vm-uuid)");
        assertDescriptionContains(response, "using service offering Medium Instance (allocated-service-offering-uuid)");
        assertUsageResourceDetails(resourceDetails, ResourceTag.ResourceObjectType.UserVm, usageId);
        verify(entityManagerMock).findByIdIncludingRemoved(ServiceOfferingVO.class, offeringId.toString());
        verify(entityManagerMock).findByIdIncludingRemoved(VMInstanceVO.class, usageId.toString());
    }

    @Test
    @DisplayName("IP_ADDRESS usage populates public IP flags")
    public void populateIpAddressUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        IPAddressVO ipAddress = mock(IPAddressVO.class);
        Long usageId = 13L;

        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(usageRecord.getType()).thenReturn("SourceNat");
        when(usageRecord.getSize()).thenReturn(1L);
        when(entityManagerMock.findByIdIncludingRemoved(IPAddressVO.class, usageId.toString())).thenReturn(ipAddress);
        when(ipAddress.getUuid()).thenReturn("ip-address-uuid");
        when(ipAddress.getId()).thenReturn(usageId);

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateIpAddressUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class}, usageRecord, response);

       // Assert
        assertResponseField(response, "usageId", "ip-address-uuid");
        assertResponseField(response, "isSourceNat", Boolean.TRUE);
        assertResponseField(response, "isSystem", Boolean.TRUE);
        assertUsageResourceDetails(resourceDetails, ResourceTag.ResourceObjectType.PublicIpAddress, usageId);
        verify(entityManagerMock).findByIdIncludingRemoved(IPAddressVO.class, usageId.toString());
    }

    @Test
    @DisplayName("NETWORK_BYTES_SENT usage populates VM and network details")
    public void populateNetworkBytesSentUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        VMInstanceVO vmInstance = mock(VMInstanceVO.class);
        NetworkVO network = mock(NetworkVO.class);
        Long usageId = 14L;
        Long networkId = 24L;

        when(usageRecord.getUsageType()).thenReturn(UsageTypes.NETWORK_BYTES_SENT);
        when(usageRecord.getType()).thenReturn("UserVm");
        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(usageRecord.getNetworkId()).thenReturn(networkId);
        when(usageRecord.getRawUsage()).thenReturn(1024D);
        when(entityManagerMock.findByIdIncludingRemoved(VMInstanceVO.class, usageId.toString())).thenReturn(vmInstance);
        when(entityManagerMock.findByIdIncludingRemoved(NetworkVO.class, networkId.toString())).thenReturn(network);
        when(vmInstance.getUuid()).thenReturn("network-vm-uuid");
        when(vmInstance.getId()).thenReturn(usageId);
        when(vmInstance.getInstanceName()).thenReturn("r-14-VM");
        when(network.getUuid()).thenReturn("network-uuid");
        when(network.getId()).thenReturn(networkId);
        when(network.getName()).thenReturn("guest-network");
        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Guest);

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateNetworkBytesUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class}, usageRecord, response, false);

        // Assert
        assertResponseField(response, "type", "UserVm");
        assertResponseField(response, "usageId", "network-vm-uuid");
        assertResponseField(response, "networkId", "network-uuid");
        assertResponseField(response, "resourceName", "guest-network");
        assertDescriptionContains(response, "Bytes sent by network guest-network (network-uuid)");
        assertDescriptionContains(response, "using router r-14-VM (network-vm-uuid)");
        assertUsageResourceDetails(resourceDetails, ResourceTag.ResourceObjectType.Network, networkId);
        verify(entityManagerMock).findByIdIncludingRemoved(VMInstanceVO.class, usageId.toString());
        verify(entityManagerMock).findByIdIncludingRemoved(NetworkVO.class, networkId.toString());
    }

    @Test
    @DisplayName("NETWORK_BYTES_RECEIVED usage populates VM and network details")
    public void populateNetworkBytesReceivedUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        VMInstanceVO vmInstance = mock(VMInstanceVO.class);
        NetworkVO network = mock(NetworkVO.class);
        Long usageId = 15L;
        Long networkId = 25L;

        when(usageRecord.getUsageType()).thenReturn(UsageTypes.NETWORK_BYTES_RECEIVED);
        when(usageRecord.getType()).thenReturn("DomainRouter");
        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(usageRecord.getNetworkId()).thenReturn(networkId);
        when(usageRecord.getRawUsage()).thenReturn(2048D);
        when(entityManagerMock.findByIdIncludingRemoved(VMInstanceVO.class, usageId.toString())).thenReturn(vmInstance);
        when(entityManagerMock.findByIdIncludingRemoved(NetworkVO.class, networkId.toString())).thenReturn(network);
        when(vmInstance.getUuid()).thenReturn("network-router-uuid");
        when(vmInstance.getId()).thenReturn(usageId);
        when(vmInstance.getInstanceName()).thenReturn("r-15-VM");
        when(network.getUuid()).thenReturn("received-network-uuid");
        when(network.getId()).thenReturn(networkId);
        when(network.getName()).thenReturn("received-network");
        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Guest);

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateNetworkBytesUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class}, usageRecord, response, false);

        // Assert
        assertResponseField(response, "type", "DomainRouter");
        assertResponseField(response, "usageId", "network-router-uuid");
        assertResponseField(response, "networkId", "received-network-uuid");
        assertResponseField(response, "resourceName", "received-network");
        assertDescriptionContains(response, "Bytes received by network received-network (received-network-uuid)");
        assertDescriptionContains(response, "using router r-15-VM (network-router-uuid)");
        assertUsageResourceDetails(resourceDetails, ResourceTag.ResourceObjectType.Network, networkId);
        verify(entityManagerMock).findByIdIncludingRemoved(VMInstanceVO.class, usageId.toString());
        verify(entityManagerMock).findByIdIncludingRemoved(NetworkVO.class, networkId.toString());
    }

    @Test
    @DisplayName("VOLUME usage populates volume size and offering details")
    public void populateVolumeUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        VMInstanceVO vmInstance = mock(VMInstanceVO.class);
        VMTemplateVO template = mock(VMTemplateVO.class);
        VolumeVO volume = mock(VolumeVO.class);
        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        Long usageId = 16L;
        Long offeringId = 26L;

        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(usageRecord.getOfferingId()).thenReturn(offeringId);
        when(usageRecord.getSize()).thenReturn(4096L);
        when(entityManagerMock.findByIdIncludingRemoved(VolumeVO.class, usageId.toString())).thenReturn(volume);
        when(entityManagerMock.findByIdIncludingRemoved(DiskOfferingVO.class, offeringId.toString())).thenReturn(diskOffering);
        when(volume.getUuid()).thenReturn("volume-uuid");
        when(volume.getId()).thenReturn(usageId);
        when(volume.getName()).thenReturn("data-volume");
        when(diskOffering.getUuid()).thenReturn("disk-offering-uuid");
        when(diskOffering.getName()).thenReturn("Small Disk");
        when(vmInstance.getUuid()).thenReturn("volume-vm-uuid");
        when(vmInstance.getHostName()).thenReturn("volume-vm");
        when(template.getUuid()).thenReturn("volume-template-uuid");
        when(template.getName()).thenReturn("Volume Template");

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateVolumeUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class, VMInstanceVO.class, VMTemplateVO.class},
                usageRecord, response, false, vmInstance, template);

        // Assert
        assertResponseField(response, "usageId", "volume-uuid");
        assertResponseField(response, "size", 4096L);
        assertResponseField(response, "offeringId", "disk-offering-uuid");
        assertDescriptionContains(response, "Volume usage for data-volume (volume-uuid)");
        assertDescriptionContains(response, "attached to VM volume-vm (volume-vm-uuid)");
        assertDescriptionContains(response, "with disk offering Small Disk (disk-offering-uuid)");
        assertUsageResourceDetails(resourceDetails, ResourceTag.ResourceObjectType.Volume, usageId);
        verify(entityManagerMock).findByIdIncludingRemoved(VolumeVO.class, usageId.toString());
        verify(entityManagerMock).findByIdIncludingRemoved(DiskOfferingVO.class, offeringId.toString());
    }

    @Test
    @DisplayName("TEMPLATE usage populates template size details")
    public void populateTemplateUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        VMTemplateVO template = mock(VMTemplateVO.class);
        Long usageId = 17L;

        when(usageRecord.getUsageType()).thenReturn(UsageTypes.TEMPLATE);
        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(usageRecord.getSize()).thenReturn(8192L);
        when(usageRecord.getVirtualSize()).thenReturn(16384L);
        when(entityManagerMock.findByIdIncludingRemoved(VMTemplateVO.class, usageId.toString())).thenReturn(template);
        when(template.getUuid()).thenReturn("template-usage-uuid");
        when(template.getId()).thenReturn(usageId);
        when(template.getName()).thenReturn("CentOS Template");

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateTemplateOrIsoUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class}, usageRecord, response, false);

        // Assert
        assertResponseField(response, "usageId", "template-usage-uuid");
        assertResponseField(response, "size", 8192L);
        assertResponseField(response, "virtualSize", 16384L);
        assertDescriptionContains(response, "Template usage for CentOS Template (template-usage-uuid)");
        assertUsageResourceDetails(resourceDetails, ResourceTag.ResourceObjectType.Template, usageId);
        verify(entityManagerMock).findByIdIncludingRemoved(VMTemplateVO.class, usageId.toString());
    }

    @Test
    @DisplayName("ISO usage populates ISO size details")
    public void populateIsoUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        VMTemplateVO iso = mock(VMTemplateVO.class);
        Long usageId = 18L;

        when(usageRecord.getUsageType()).thenReturn(UsageTypes.ISO);
        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(usageRecord.getSize()).thenReturn(2048L);
        when(usageRecord.getVirtualSize()).thenReturn(4096L);
        when(entityManagerMock.findByIdIncludingRemoved(VMTemplateVO.class, usageId.toString())).thenReturn(iso);
        when(iso.getUuid()).thenReturn("iso-usage-uuid");
        when(iso.getId()).thenReturn(usageId);
        when(iso.getName()).thenReturn("Installer ISO");

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateTemplateOrIsoUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class}, usageRecord, response, false);

        // Assert
        assertResponseField(response, "usageId", "iso-usage-uuid");
        assertResponseField(response, "size", 2048L);
        assertResponseField(response, "virtualSize", 2048L);
        assertDescriptionContains(response, "ISO usage for Installer ISO (iso-usage-uuid)");
        assertUsageResourceDetails(resourceDetails, ResourceTag.ResourceObjectType.ISO, usageId);
        verify(entityManagerMock).findByIdIncludingRemoved(VMTemplateVO.class, usageId.toString());
    }

    @Test
    @DisplayName("SNAPSHOT usage populates snapshot size details")
    public void populateSnapshotUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        SnapshotVO snapshot = mock(SnapshotVO.class);
        Long usageId = 19L;

        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(usageRecord.getSize()).thenReturn(1024L);
        when(entityManagerMock.findByIdIncludingRemoved(SnapshotVO.class, usageId.toString())).thenReturn(snapshot);
        when(snapshot.getUuid()).thenReturn("snapshot-uuid");
        when(snapshot.getId()).thenReturn(usageId);
        when(snapshot.getName()).thenReturn("daily-snapshot");

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateSnapshotUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class}, usageRecord, response, false);

        // Assert
        assertResponseField(response, "usageId", "snapshot-uuid");
        assertResponseField(response, "size", 1024L);
        assertDescriptionContains(response, "Snapshot usage for daily-snapshot (snapshot-uuid)");
        assertUsageResourceDetails(resourceDetails, ResourceTag.ResourceObjectType.Snapshot, usageId);
        verify(entityManagerMock).findByIdIncludingRemoved(SnapshotVO.class, usageId.toString());
    }

    @Test
    @DisplayName("SECURITY_GROUP usage populates security group details")
    public void populateSecurityGroupUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        SecurityGroupVO securityGroup = mock(SecurityGroupVO.class);
        VMInstanceVO vmInstance = mock(VMInstanceVO.class);
        Long usageId = 20L;

        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(entityManagerMock.findByIdIncludingRemoved(SecurityGroupVO.class, usageId.toString())).thenReturn(securityGroup);
        when(securityGroup.getUuid()).thenReturn("security-group-uuid");
        when(securityGroup.getId()).thenReturn(usageId);
        when(securityGroup.getName()).thenReturn("web-tier");
        when(vmInstance.getUuid()).thenReturn("security-group-vm-uuid");
        when(vmInstance.getHostName()).thenReturn("security-group-vm");

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateSecurityGroupUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class, VMInstanceVO.class},
                usageRecord, response, false, vmInstance);

        // Assert
        assertResponseField(response, "usageId", "security-group-uuid");
        assertDescriptionContains(response, "Security group web-tier (security-group-uuid) usage");
        assertDescriptionContains(response, "for VM security-group-vm (security-group-vm-uuid)");
        assertUsageResourceDetails(resourceDetails, ResourceTag.ResourceObjectType.SecurityGroup, usageId);
        verify(entityManagerMock).findByIdIncludingRemoved(SecurityGroupVO.class, usageId.toString());
    }

    @Test
    @DisplayName("LOAD_BALANCER_POLICY usage populates load balancer details")
    public void populateLoadBalancerPolicyUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        LoadBalancerVO loadBalancer = mock(LoadBalancerVO.class);
        Long usageId = 21L;

        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(entityManagerMock.findByIdIncludingRemoved(LoadBalancerVO.class, usageId.toString())).thenReturn(loadBalancer);
        when(loadBalancer.getUuid()).thenReturn("load-balancer-uuid");
        when(loadBalancer.getId()).thenReturn(usageId);
        when(loadBalancer.getName()).thenReturn("public-lb");

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateLoadBalancerPolicyUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class}, usageRecord, response, false);

        // Assert
        assertResponseField(response, "usageId", "load-balancer-uuid");
        assertDescriptionContains(response, "Loadbalancer policy usage public-lb (load-balancer-uuid)");
        assertUsageResourceDetails(resourceDetails, ResourceTag.ResourceObjectType.LoadBalancer, usageId);
        verify(entityManagerMock).findByIdIncludingRemoved(LoadBalancerVO.class, usageId.toString());
    }

    @Test
    @DisplayName("PORT_FORWARDING_RULE usage populates port forwarding rule details")
    public void populatePortForwardingRuleUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        PortForwardingRuleVO portForwardingRule = mock(PortForwardingRuleVO.class);
        Long usageId = 22L;

        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(entityManagerMock.findByIdIncludingRemoved(PortForwardingRuleVO.class, usageId.toString())).thenReturn(portForwardingRule);
        when(portForwardingRule.getUuid()).thenReturn("port-forwarding-rule-uuid");
        when(portForwardingRule.getId()).thenReturn(usageId);

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populatePortForwardingRuleUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class}, usageRecord, response, false);

        // Assert
        assertResponseField(response, "usageId", "port-forwarding-rule-uuid");
        assertDescriptionContains(response, "Port forwarding rule usage (port-forwarding-rule-uuid)");
        assertUsageResourceDetails(resourceDetails, ResourceTag.ResourceObjectType.PortForwardingRule, usageId);
        verify(entityManagerMock).findByIdIncludingRemoved(PortForwardingRuleVO.class, usageId.toString());
    }

    @Test
    @DisplayName("NETWORK_OFFERING usage populates offering and default flag")
    public void populateNetworkOfferingUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        NetworkOfferingVO networkOffering = mock(NetworkOfferingVO.class);
        VMInstanceVO vmInstance = mock(VMInstanceVO.class);
        Long offeringId = 23L;

        when(usageRecord.getOfferingId()).thenReturn(offeringId);
        when(usageRecord.getUsageId()).thenReturn(1L);
        when(entityManagerMock.findByIdIncludingRemoved(NetworkOfferingVO.class, offeringId.toString())).thenReturn(networkOffering);
        when(networkOffering.getUuid()).thenReturn("network-offering-uuid");
        when(networkOffering.getName()).thenReturn("Default Isolated Network");
        when(vmInstance.getUuid()).thenReturn("network-offering-vm-uuid");
        when(vmInstance.getHostName()).thenReturn("network-offering-vm");

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateNetworkOfferingUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class, VMInstanceVO.class},
                usageRecord, response, false, vmInstance);

        // Assert
        assertResponseField(response, "offeringId", "network-offering-uuid");
        assertResponseField(response, "isDefault", Boolean.TRUE);
        assertDescriptionContains(response, "Network offering Default Isolated Network (network-offering-uuid) usage");
        assertDescriptionContains(response, "for VM network-offering-vm (network-offering-vm-uuid)");
        assertUsageResourceDetails(resourceDetails, null, null);
        verify(entityManagerMock).findByIdIncludingRemoved(NetworkOfferingVO.class, offeringId.toString());
    }

    @Test
    @DisplayName("VPN_USERS usage populates VPN user details")
    public void populateVpnUsersUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mock(UsageVO.class);
        UsageRecordResponse response = new UsageRecordResponse();
        VpnUserVO vpnUser = mock(VpnUserVO.class);
        Long usageId = 24L;

        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(entityManagerMock.findByIdIncludingRemoved(VpnUserVO.class, usageId.toString())).thenReturn(vpnUser);
        when(vpnUser.getUuid()).thenReturn("vpn-user-uuid");
        when(vpnUser.getUsername()).thenReturn("vpn-user");

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateVpnUsersUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class}, usageRecord, response, false);

        // Assert
        assertResponseField(response, "usageId", "vpn-user-uuid");
        assertDescriptionContains(response, "VPN usage for user vpn-user (vpn-user-uuid)");
        assertUsageResourceDetails(resourceDetails, null, null);
        verify(entityManagerMock).findByIdIncludingRemoved(VpnUserVO.class, usageId.toString());
    }

    @Test
    @DisplayName("VM_DISK_IO_READ usage populates disk read request details")
    public void populateVmDiskIoReadUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mockVmDiskUsageRecord(UsageTypes.VM_DISK_IO_READ, 25L, 512D);
        UsageRecordResponse response = new UsageRecordResponse();
        VMInstanceVO vmInstance = mockVmInstance("vm-disk-read-vm", "vm-disk-read-vm-uuid");
        VolumeVO volume = mockVolume(25L, "vm-disk-read-volume-uuid", "vm-disk-read-volume");
        when(entityManagerMock.findByIdIncludingRemoved(VolumeVO.class, "25")).thenReturn(volume);

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateVmDiskUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class, VMInstanceVO.class},
                usageRecord, response, false, vmInstance);

        // Assert
        assertVmDiskUsageResponse(response, resourceDetails, "Disk I/O read requests", "vm-disk-read-volume-uuid", "vm-disk-read-volume", 25L);
        verify(entityManagerMock).findByIdIncludingRemoved(VolumeVO.class, "25");
    }

    @Test
    @DisplayName("VM_DISK_IO_WRITE usage populates disk write request details")
    public void populateVmDiskIoWriteUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mockVmDiskUsageRecord(UsageTypes.VM_DISK_IO_WRITE, 26L, 1024D);
        UsageRecordResponse response = new UsageRecordResponse();
        VMInstanceVO vmInstance = mockVmInstance("vm-disk-write-vm", "vm-disk-write-vm-uuid");
        VolumeVO volume = mockVolume(26L, "vm-disk-write-volume-uuid", "vm-disk-write-volume");
        when(entityManagerMock.findByIdIncludingRemoved(VolumeVO.class, "26")).thenReturn(volume);

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateVmDiskUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class, VMInstanceVO.class},
                usageRecord, response, false, vmInstance);

        // Assert
        assertVmDiskUsageResponse(response, resourceDetails, "Disk I/O write requests", "vm-disk-write-volume-uuid", "vm-disk-write-volume", 26L);
        verify(entityManagerMock).findByIdIncludingRemoved(VolumeVO.class, "26");
    }

    @Test
    @DisplayName("VM_DISK_BYTES_READ usage populates disk read byte details")
    public void populateVmDiskBytesReadUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mockVmDiskUsageRecord(UsageTypes.VM_DISK_BYTES_READ, 27L, 2048D);
        UsageRecordResponse response = new UsageRecordResponse();
        VMInstanceVO vmInstance = mockVmInstance("vm-disk-bytes-read-vm", "vm-disk-bytes-read-vm-uuid");
        VolumeVO volume = mockVolume(27L, "vm-disk-bytes-read-volume-uuid", "vm-disk-bytes-read-volume");
        when(entityManagerMock.findByIdIncludingRemoved(VolumeVO.class, "27")).thenReturn(volume);

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateVmDiskUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class, VMInstanceVO.class},
                usageRecord, response, false, vmInstance);

        // Assert
        assertVmDiskUsageResponse(response, resourceDetails, "Disk I/O read bytes", "vm-disk-bytes-read-volume-uuid", "vm-disk-bytes-read-volume", 27L);
        verify(entityManagerMock).findByIdIncludingRemoved(VolumeVO.class, "27");
    }

    @Test
    @DisplayName("VM_DISK_BYTES_WRITE usage populates disk write byte details")
    public void populateVmDiskBytesWriteUsageResponseTest() throws Exception {
        // Arrange
        UsageVO usageRecord = mockVmDiskUsageRecord(UsageTypes.VM_DISK_BYTES_WRITE, 28L, 4096D);
        UsageRecordResponse response = new UsageRecordResponse();
        VMInstanceVO vmInstance = mockVmInstance("vm-disk-bytes-write-vm", "vm-disk-bytes-write-vm-uuid");
        VolumeVO volume = mockVolume(28L, "vm-disk-bytes-write-volume-uuid", "vm-disk-bytes-write-volume");
        when(entityManagerMock.findByIdIncludingRemoved(VolumeVO.class, "28")).thenReturn(volume);

        // Act
        Object resourceDetails = invokeUsageDetailsHelper("populateVmDiskUsageResponse",
                new Class<?>[] {Usage.class, UsageRecordResponse.class, boolean.class, VMInstanceVO.class},
                usageRecord, response, false, vmInstance);

        // Assert
        assertVmDiskUsageResponse(response, resourceDetails, "Disk I/O write bytes", "vm-disk-bytes-write-volume-uuid", "vm-disk-bytes-write-volume", 28L);
        verify(entityManagerMock).findByIdIncludingRemoved(VolumeVO.class, "28");
    }

    private Object invokeUsageDetailsHelper(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = ApiResponseHelper.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(helper, args);
    }

    private void assertResponseField(UsageRecordResponse response, String fieldName, Object expectedValue) {
        assertEquals(expectedValue, ReflectionTestUtils.getField(response, fieldName));
    }

    private void assertDescriptionContains(UsageRecordResponse response, String expectedText) {
        Object description = ReflectionTestUtils.getField(response, "description");
        Assertions.assertNotNull(description);
        assertTrue(description.toString().contains(expectedText),
                String.format("Expected description [%s] to contain [%s]", description, expectedText));
    }

    private void assertUsageResourceDetails(Object resourceDetails, ResourceTag.ResourceObjectType expectedResourceType, Long expectedResourceId) {
        assertEquals(expectedResourceType, ReflectionTestUtils.getField(resourceDetails, "resourceType"));
        assertEquals(expectedResourceId, ReflectionTestUtils.getField(resourceDetails, "resourceId"));
    }

    private UsageVO mockVmDiskUsageRecord(int usageType, Long usageId, Double rawUsage) {
        UsageVO usageRecord = mock(UsageVO.class);
        when(usageRecord.getUsageType()).thenReturn(usageType);
        when(usageRecord.getUsageId()).thenReturn(usageId);
        when(usageRecord.getType()).thenReturn("UserVm");
        when(usageRecord.getRawUsage()).thenReturn(rawUsage);
        return usageRecord;
    }

    private VMInstanceVO mockVmInstance(String hostName, String uuid) {
        VMInstanceVO vmInstance = mock(VMInstanceVO.class);
        when(vmInstance.getHostName()).thenReturn(hostName);
        when(vmInstance.getUuid()).thenReturn(uuid);
        return vmInstance;
    }

    private VolumeVO mockVolume(Long id, String uuid, String name) {
        VolumeVO volume = mock(VolumeVO.class);
        when(volume.getId()).thenReturn(id);
        when(volume.getUuid()).thenReturn(uuid);
        when(volume.getName()).thenReturn(name);
        return volume;
    }

    private void assertVmDiskUsageResponse(UsageRecordResponse response, Object resourceDetails, String descriptionPrefix, String volumeUuid, String volumeName, Long volumeId) {
        assertResponseField(response, "type", "UserVm");
        assertResponseField(response, "usageId", volumeUuid);
        assertDescriptionContains(response, descriptionPrefix);
        assertDescriptionContains(response, "volume " + volumeName + " (" + volumeUuid + ")");
        assertUsageResourceDetails(resourceDetails, ResourceTag.ResourceObjectType.Volume, volumeId);
    }
}
