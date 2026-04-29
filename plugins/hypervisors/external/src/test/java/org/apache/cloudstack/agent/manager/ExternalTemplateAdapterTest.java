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

package org.apache.cloudstack.agent.manager;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.api.command.user.template.RegisterTemplateCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.cpu.CPU;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class ExternalTemplateAdapterTest {
    @Mock
    ImageStoreDao _imgStoreDao;
    @Mock
    AccountManager _accountMgr;
    @Mock
    TemplateManager templateMgr;
    @Mock
    ResourceLimitService _resourceLimitMgr;
    @Mock
    UserDao _userDao;
    @Mock
    VMTemplateDao _tmpltDao;

    @Spy
    @InjectMocks
    ExternalTemplateAdapter adapter;

    private RegisterTemplateCmd cmd;
    private TemplateProfile profile;
    private DataStore dataStore;

    @Before
    public void setUp() {
        cmd = mock(RegisterTemplateCmd.class);
        profile = mock(TemplateProfile.class);
        dataStore = mock(DataStore.class);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void prepare_WhenHypervisorTypeIsNone_ThrowsInvalidParameterValueException() throws ResourceAllocationException {
        Account adminAccount =  new AccountVO("system", 1L, "", Account.Type.ADMIN, "uuid");
        ReflectionTestUtils.setField(adminAccount, "id", 1L);
        CallContext callContext = Mockito.mock(CallContext.class);
        when(callContext.getCallingAccount()).thenReturn(adminAccount);
        try (MockedStatic<CallContext> mockedCallContext = Mockito.mockStatic(CallContext.class)) {
            mockedCallContext.when(CallContext::current).thenReturn(callContext);
            when(cmd.getHypervisor()).thenReturn("None");
            when(cmd.getZoneIds()).thenReturn(List.of(1L));
            when(_imgStoreDao.findRegionImageStores()).thenReturn(Collections.emptyList());

            adapter.prepare(cmd);
        }
    }

    @Test
    public void prepare_WhenRegionImageStoresExist_ZoneIdsAreIgnored() throws ResourceAllocationException {
        Account adminAccount =  new AccountVO("system", 1L, "", Account.Type.ADMIN, "uuid");
        ReflectionTestUtils.setField(adminAccount, "id", 1L);
        CallContext callContext = Mockito.mock(CallContext.class);
        when(callContext.getCallingAccount()).thenReturn(adminAccount);
        try (MockedStatic<CallContext> mockedCallContext = Mockito.mockStatic(CallContext.class)) {
            mockedCallContext.when(CallContext::current).thenReturn(callContext);
            when(cmd.getZoneIds()).thenReturn(List.of(1L, 2L));
            when(_imgStoreDao.findRegionImageStores()).thenReturn(List.of(mock(ImageStoreVO.class)));
            when(cmd.getHypervisor()).thenReturn("KVM");
            when(cmd.getDetails()).thenReturn(null);
            when(cmd.getExternalDetails()).thenReturn(Collections.emptyMap());
            when(_accountMgr.getAccount(anyLong())).thenReturn(mock(com.cloud.user.Account.class));
            when(_accountMgr.isAdmin(anyLong())).thenReturn(true);
            when(templateMgr.validateTemplateType(any(), anyBoolean(), anyBoolean(), eq(Hypervisor.HypervisorType.External))).thenReturn(com.cloud.storage.Storage.TemplateType.USER);
            when(_userDao.findById(any())).thenReturn(mock(UserVO.class));
            when(cmd.getEntityOwnerId()).thenReturn(1L);
            when(cmd.getTemplateName()).thenReturn("t");
            when(cmd.getDisplayText()).thenReturn("d");
            when(cmd.getArch()).thenReturn(CPU.CPUArch.amd64);
            when(cmd.getBits()).thenReturn(64);
            when(cmd.isPasswordEnabled()).thenReturn(false);
            when(cmd.getRequiresHvm()).thenReturn(false);
            when(cmd.getUrl()).thenReturn("http://example.com");
            when(cmd.isPublic()).thenReturn(false);
            when(cmd.isFeatured()).thenReturn(false);
            when(cmd.isExtractable()).thenReturn(false);
            when(cmd.getFormat()).thenReturn("QCOW2");
            when(cmd.getOsTypeId()).thenReturn(1L);
            when(cmd.getChecksum()).thenReturn("abc");
            when(cmd.getTemplateTag()).thenReturn(null);
            when(cmd.isSshKeyEnabled()).thenReturn(false);
            when(cmd.isDynamicallyScalable()).thenReturn(false);
            when(cmd.isDirectDownload()).thenReturn(false);
            when(cmd.isDeployAsIs()).thenReturn(false);
            when(cmd.isForCks()).thenReturn(false);
            when(cmd.getExtensionId()).thenReturn(null);

            TemplateProfile result = adapter.prepare(cmd);

            assertNull(result.getZoneIdList());
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void createTemplateForPostUpload_WhenZoneIdListIsNull_ThrowsCloudRuntimeException() {
        when(profile.getFormat()).thenReturn(com.cloud.storage.Storage.ImageFormat.QCOW2);
        when(profile.getZoneIdList()).thenReturn(null);

        adapter.createTemplateForPostUpload(profile);
    }

    @Test(expected = CloudRuntimeException.class)
    public void createTemplateForPostUpload_WhenMultipleZoneIds_ThrowsCloudRuntimeException() {
        when(profile.getFormat()).thenReturn(com.cloud.storage.Storage.ImageFormat.QCOW2);
        when(profile.getZoneIdList()).thenReturn(List.of(1L, 2L));

        adapter.createTemplateForPostUpload(profile);
    }

    @Test(expected = CloudRuntimeException.class)
    public void prepareDelete_AlwaysThrowsCloudRuntimeException() {
        adapter.prepareDelete(mock(org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd.class));
    }
}
