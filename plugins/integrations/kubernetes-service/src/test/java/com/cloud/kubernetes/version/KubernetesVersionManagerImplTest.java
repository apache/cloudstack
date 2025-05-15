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
package com.cloud.kubernetes.version;

import java.util.UUID;

import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import org.apache.cloudstack.api.response.KubernetesSupportedVersionResponse;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.cpu.CPU;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesVersionManagerImplTest {

    @Mock
    TemplateJoinDao templateJoinDao;

    @Mock
    AccountManager accountManager;

    @InjectMocks
    KubernetesVersionManagerImpl kubernetesVersionManager = new KubernetesVersionManagerImpl();

    @Test
    public void testUpdateTemplateDetailsInKubernetesSupportedVersionResponseNullTemplate() {
        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);
            Account accountMock = Mockito.mock(Account.class);
            Mockito.when(callContextMock.getCallingAccount()).thenReturn(accountMock);
            Mockito.when(accountMock.getId()).thenReturn(2L);

            KubernetesSupportedVersion kubernetesSupportedVersion = Mockito.mock(KubernetesSupportedVersion.class);
            Mockito.when(kubernetesSupportedVersion.getIsoId()).thenReturn(1L);
            KubernetesSupportedVersionResponse response = new KubernetesSupportedVersionResponse();
            kubernetesVersionManager.updateTemplateDetailsInKubernetesSupportedVersionResponse(kubernetesSupportedVersion,
                    response);
            Assert.assertNull(ReflectionTestUtils.getField(response, "isoId"));
        }
    }

    @Test
    public void testUpdateTemplateDetailsInKubernetesSupportedVersionResponseValidTemplate() {
        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);
            Account accountMock = Mockito.mock(Account.class);
            Mockito.when(callContextMock.getCallingAccount()).thenReturn(accountMock);
            Mockito.when(accountMock.getId()).thenReturn(2L);
            KubernetesSupportedVersion kubernetesSupportedVersion = Mockito.mock(KubernetesSupportedVersion.class);
            Mockito.when(kubernetesSupportedVersion.getIsoId()).thenReturn(1L);
            KubernetesSupportedVersionResponse response = new KubernetesSupportedVersionResponse();
            TemplateJoinVO templateJoinVO = Mockito.mock(TemplateJoinVO.class);
            Mockito.when(templateJoinVO.getArch()).thenReturn(CPU.CPUArch.getDefault());
            String uuid = UUID.randomUUID().toString();
            Mockito.when(templateJoinVO.getUuid()).thenReturn(uuid);
            Mockito.when(templateJoinDao.findById(1L)).thenReturn(templateJoinVO);
            kubernetesVersionManager.updateTemplateDetailsInKubernetesSupportedVersionResponse(kubernetesSupportedVersion,
                    response);
            Assert.assertEquals(uuid, ReflectionTestUtils.getField(response, "isoId"));
            Assert.assertNull(ReflectionTestUtils.getField(response, "isoState"));
            ObjectInDataStoreStateMachine.State state = ObjectInDataStoreStateMachine.State.Ready;
            Mockito.when(templateJoinVO.getState()).thenReturn(state);
            kubernetesVersionManager.updateTemplateDetailsInKubernetesSupportedVersionResponse(kubernetesSupportedVersion,
                    response);
            Assert.assertEquals(state.toString(), ReflectionTestUtils.getField(response, "isoState"));
        }
    }
}
