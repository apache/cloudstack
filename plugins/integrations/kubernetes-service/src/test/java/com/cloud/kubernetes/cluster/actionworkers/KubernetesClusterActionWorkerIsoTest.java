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
package com.cloud.kubernetes.cluster.actionworkers;

import java.util.List;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.context.CallContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.TemplateApiService;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.uservm.UserVm;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmIsoMapVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VmIsoMapDao;

/**
 * Attaching the binaries ISO to the nodes of a cluster, some of which may still have an ISO attached.
 */
@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterActionWorkerIsoTest {

    private static final long BINARIES_ISO_ID = 10L;
    private static final long OTHER_ISO_ID = 11L;
    private static final long VM_ID = 2L;

    @Mock
    KubernetesClusterManagerImpl kubernetesClusterManager;

    @Mock
    KubernetesCluster kubernetesCluster;

    @Mock
    TemplateApiService templateService;

    @Mock
    VMTemplateDao templateDao;

    @Mock
    UserVmDao userVmDao;

    @Mock
    VmIsoMapDao vmIsoMapDao;

    @Mock
    KubernetesSupportedVersion version;

    @Mock
    UserVm vm;

    KubernetesClusterActionWorker actionWorker;

    @Before
    public void setUp() {
        kubernetesClusterManager.kubernetesClusterDao = Mockito.mock(KubernetesClusterDao.class);
        actionWorker = new KubernetesClusterActionWorker(kubernetesCluster, kubernetesClusterManager);
        actionWorker.templateService = templateService;
        actionWorker.templateDao = templateDao;
        actionWorker.userVmDao = userVmDao;
        actionWorker.vmIsoMapDao = vmIsoMapDao;
        Mockito.when(version.getIsoId()).thenReturn(BINARIES_ISO_ID);
        VMTemplateVO iso = Mockito.mock(VMTemplateVO.class);
        Mockito.when(iso.getId()).thenReturn(BINARIES_ISO_ID);
        Mockito.when(iso.getFormat()).thenReturn(Storage.ImageFormat.ISO);
        Mockito.when(iso.getState()).thenReturn(VirtualMachineTemplate.State.Active);
        Mockito.when(templateDao.findById(BINARIES_ISO_ID)).thenReturn(iso);
        Mockito.when(vm.getId()).thenReturn(VM_ID);
    }

    private void attachBinariesIso(Long attachedIsoId, boolean binariesIsoInOtherCdrom) {
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        Mockito.when(userVm.getIsoId()).thenReturn(attachedIsoId);
        Mockito.when(userVmDao.findById(VM_ID)).thenReturn(userVm);
        Mockito.lenient().when(vmIsoMapDao.findByVmIdIsoId(VM_ID, BINARIES_ISO_ID)).thenReturn(binariesIsoInOtherCdrom ? Mockito.mock(VmIsoMapVO.class) : null);
        try (MockedStatic<CallContext> callContext = Mockito.mockStatic(CallContext.class)) {
            callContext.when(() -> CallContext.register(Mockito.any(), Mockito.any(ApiCommandResourceType.class))).thenReturn(Mockito.mock(CallContext.class));
            actionWorker.attachIsoKubernetesVMs(List.of(vm), version);
        }
    }

    @Test
    public void testAttachIsoKubernetesVMsNoIsoAttached() {
        attachBinariesIso(null, false);
        Mockito.verify(templateService, Mockito.never()).detachIso(Mockito.anyLong(), Mockito.any(), Mockito.any());
        Mockito.verify(templateService).attachIso(BINARIES_ISO_ID, VM_ID, true);
    }

    @Test
    public void testAttachIsoKubernetesVMsBinariesIsoAlreadyAttached() {
        attachBinariesIso(BINARIES_ISO_ID, false);
        Mockito.verifyNoInteractions(templateService);
    }

    @Test
    public void testAttachIsoKubernetesVMsBinariesIsoAttachedToOtherCdrom() {
        attachBinariesIso(OTHER_ISO_ID, true);
        Mockito.verifyNoInteractions(templateService);
    }

    @Test
    public void testAttachIsoKubernetesVMsOtherIsoAttached() {
        attachBinariesIso(OTHER_ISO_ID, false);
        Mockito.verify(templateService).detachIso(VM_ID, OTHER_ISO_ID, true);
        Mockito.verify(templateService).attachIso(BINARIES_ISO_ID, VM_ID, true);
    }
}
