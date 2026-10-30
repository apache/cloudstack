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
package com.cloud.api.query.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.junit.Assert;
import org.apache.cloudstack.context.CallContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;

@RunWith(MockitoJUnitRunner.class)
public class VolumeJoinDaoImplTest extends GenericDaoBaseWithTagInformationBaseTest<VolumeJoinVO, VolumeResponse> {

    private static final long DISK_OFFERING_ID = 10L;

    @InjectMocks
    private VolumeJoinDaoImpl _volumeJoinDaoImpl;

    @Mock
    private AnnotationDao annotationDao;

    @Mock
    private AccountManager _accountMgr;

    private VolumeJoinVO volume = new VolumeJoinVO();
    private VolumeResponse volumeResponse = new VolumeResponse();

    @Before
    public void setup() {
        lenient().when(annotationDao.hasAnnotations(anyString(), anyString(), anyBoolean())).thenReturn(false);
        lenient().when(_accountMgr.isRootAdmin(eq(1L))).thenReturn(true);
    }

    @Test
    public void testUpdateVolumeTagInfo(){
        prepareSetup();
        testUpdateTagInformation(_volumeJoinDaoImpl, volume, volumeResponse);
    }

    @Test
    public void testSetThrottleRatesMapsReadAndWriteDistinctly() {
        VolumeJoinVO vol = Mockito.mock(VolumeJoinVO.class);
        Mockito.when(vol.getBytesReadRate()).thenReturn(100L);
        Mockito.when(vol.getBytesWriteRate()).thenReturn(200L);
        Mockito.when(vol.getIopsReadRate()).thenReturn(300L);
        Mockito.when(vol.getIopsWriteRate()).thenReturn(400L);

        VolumeResponse response = new VolumeResponse();
        _volumeJoinDaoImpl.setThrottleRates(response, vol);

        Assert.assertEquals(Long.valueOf(100L), response.getBytesReadRate());
        Assert.assertEquals(Long.valueOf(200L), response.getBytesWriteRate());
        Assert.assertEquals(Long.valueOf(300L), response.getIopsReadRate());
        Assert.assertEquals(Long.valueOf(400L), response.getIopsWriteRate());
    }

    @Test
    public void testNewVolumeResponseSetsCustomizedIopsForComputeOfferingRoot() {
        VolumeJoinVO volumeJoin = mockVolumeWithDiskOffering();
        DiskOfferingVO computeOnlyOffering = mock(DiskOfferingVO.class);
        when(computeOnlyOffering.isCustomizedIops()).thenReturn(true);
        ServiceOfferingVO serviceOffering = mock(ServiceOfferingVO.class);
        when(serviceOffering.getId()).thenReturn(5L);
        when(serviceOffering.getName()).thenReturn("FullCustom_Compute");
        when(serviceOffering.getDisplayText()).thenReturn("FullCustom_Compute");

        VolumeResponse response = newVolumeResponse(volumeJoin, apiDBUtils -> {
            apiDBUtils.when(() -> ApiDBUtils.findComputeOnlyDiskOfferingById(DISK_OFFERING_ID))
                    .thenReturn(computeOnlyOffering);
            apiDBUtils.when(() -> ApiDBUtils.findServiceOfferingByComputeOnlyDiskOffering(DISK_OFFERING_ID, false))
                    .thenReturn(serviceOffering);
        });

        assertTrue(response.isCustomizedIops());
        assertEquals("5", response.getServiceOfferingId());
        assertEquals("FullCustom_Compute", response.getServiceOfferingName());
        assertNull(response.getDiskOfferingId());
    }

    @Test
    public void testNewVolumeResponseSetsCustomizedIopsForDiskOffering() {
        VolumeJoinVO volumeJoin = mockVolumeWithDiskOffering();
        when(volumeJoin.getDiskOfferingUuid()).thenReturn("disk-offering-uuid");
        when(volumeJoin.getDiskOfferingName()).thenReturn("NFS_Disk_Custom75");
        when(volumeJoin.getDiskOfferingDisplayText()).thenReturn("NFS_Disk_Custom75");
        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOffering.isCustomizedIops()).thenReturn(true);

        VolumeResponse response = newVolumeResponse(volumeJoin, apiDBUtils -> {
            apiDBUtils.when(() -> ApiDBUtils.findComputeOnlyDiskOfferingById(DISK_OFFERING_ID)).thenReturn(null);
            apiDBUtils.when(() -> ApiDBUtils.findDiskOfferingById(DISK_OFFERING_ID)).thenReturn(diskOffering);
        });

        assertTrue(response.isCustomizedIops());
        assertEquals("disk-offering-uuid", response.getDiskOfferingId());
        assertNull(response.getServiceOfferingId());
    }

    @Test
    public void testNewVolumeResponseSetsCustomizedIopsFalseWhenOfferingIsFixed() {
        VolumeJoinVO volumeJoin = mockVolumeWithDiskOffering();
        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOffering.isCustomizedIops()).thenReturn(false);

        VolumeResponse response = newVolumeResponse(volumeJoin, apiDBUtils -> {
            apiDBUtils.when(() -> ApiDBUtils.findComputeOnlyDiskOfferingById(DISK_OFFERING_ID)).thenReturn(null);
            apiDBUtils.when(() -> ApiDBUtils.findDiskOfferingById(DISK_OFFERING_ID)).thenReturn(diskOffering);
        });

        assertFalse(response.isCustomizedIops());
    }

    @Test
    public void testNewVolumeResponseSetsCustomizedIopsFalseForFixedComputeOffering() {
        VolumeJoinVO volumeJoin = mockVolumeWithDiskOffering();
        DiskOfferingVO computeOnlyOffering = mock(DiskOfferingVO.class);
        when(computeOnlyOffering.isCustomizedIops()).thenReturn(false);
        ServiceOfferingVO serviceOffering = mock(ServiceOfferingVO.class);
        when(serviceOffering.getId()).thenReturn(5L);
        when(serviceOffering.getName()).thenReturn("Fixed_Compute");
        when(serviceOffering.getDisplayText()).thenReturn("Fixed_Compute");

        VolumeResponse response = newVolumeResponse(volumeJoin, apiDBUtils -> {
            apiDBUtils.when(() -> ApiDBUtils.findComputeOnlyDiskOfferingById(DISK_OFFERING_ID))
                    .thenReturn(computeOnlyOffering);
            apiDBUtils.when(() -> ApiDBUtils.findServiceOfferingByComputeOnlyDiskOffering(DISK_OFFERING_ID, false))
                    .thenReturn(serviceOffering);
        });

        assertFalse(response.isCustomizedIops());
        assertEquals("5", response.getServiceOfferingId());
        assertEquals("Fixed_Compute", response.getServiceOfferingName());
        assertNull(response.getDiskOfferingId());
    }

    @Test
    public void testNewVolumeResponseLeavesCustomizedIopsNullWhenOfferingValueIsNull() {
        VolumeJoinVO volumeJoin = mockVolumeWithDiskOffering();
        when(volumeJoin.getDiskOfferingUuid()).thenReturn("disk-offering-uuid");
        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOffering.isCustomizedIops()).thenReturn(null);

        VolumeResponse response = newVolumeResponse(volumeJoin, apiDBUtils -> {
            apiDBUtils.when(() -> ApiDBUtils.findComputeOnlyDiskOfferingById(DISK_OFFERING_ID)).thenReturn(null);
            apiDBUtils.when(() -> ApiDBUtils.findDiskOfferingById(DISK_OFFERING_ID)).thenReturn(diskOffering);
        });

        assertNull(response.isCustomizedIops());
        assertNull(response.getCustomizedIops());
    }

    private VolumeJoinVO mockVolumeWithDiskOffering() {
        VolumeJoinVO volumeJoin = mock(VolumeJoinVO.class);
        when(volumeJoin.getUuid()).thenReturn("volume-uuid");
        when(volumeJoin.getName()).thenReturn("ROOT-67");
        when(volumeJoin.getDiskOfferingId()).thenReturn(DISK_OFFERING_ID);
        when(volumeJoin.getVmId()).thenReturn(0L);
        when(volumeJoin.getState()).thenReturn(Volume.State.Ready);
        when(volumeJoin.getVolumeType()).thenReturn(Volume.Type.ROOT);
        when(volumeJoin.getTemplateId()).thenReturn(0L);
        when(volumeJoin.getTagId()).thenReturn(0L);
        when(volumeJoin.getMinIops()).thenReturn(0L);
        when(volumeJoin.getMaxIops()).thenReturn(1000L);
        when(volumeJoin.getDeleteProtection()).thenReturn(false);
        when(volumeJoin.isDisplayVolume()).thenReturn(true);
        when(volumeJoin.getKmsWrappedKeyId()).thenReturn(null);
        when(volumeJoin.getJobId()).thenReturn(null);
        return volumeJoin;
    }

    private VolumeResponse newVolumeResponse(VolumeJoinVO volumeJoin, Consumer<MockedStatic<ApiDBUtils>> apiStubs) {
        Account callingAccount = mock(Account.class);
        when(callingAccount.getId()).thenReturn(1L);
        CallContext callContext = mock(CallContext.class);
        when(callContext.getCallingAccount()).thenReturn(callingAccount);

        try (MockedStatic<ApiDBUtils> apiDBUtils = mockStatic(ApiDBUtils.class);
             MockedStatic<CallContext> callContextMocked = mockStatic(CallContext.class);
             MockedStatic<ApiResponseHelper> apiResponseHelperMocked = mockStatic(ApiResponseHelper.class)) {
            apiStubs.accept(apiDBUtils);
            callContextMocked.when(CallContext::current).thenReturn(callContext);
            apiResponseHelperMocked.when(() -> ApiResponseHelper.populateOwner(any(), any()))
                    .thenAnswer(invocation -> null);
            return _volumeJoinDaoImpl.newVolumeResponse(ResponseView.Restricted, volumeJoin);
        }
    }

}
