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
package org.apache.cloudstack.engine.orchestration;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.DiskOfferingInfo;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntityImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudOrchestratorTest {

    private static final long VM_ID = 1L;
    private static final long SERVICE_OFFERING_ID = 2L;
    private static final long ROOT_DISK_OFFERING_ID = 3L;
    private static final String TEMPLATE_ID = "4";

    @InjectMocks
    private CloudOrchestrator cloudOrchestrator = new CloudOrchestrator();

    @Mock
    private VirtualMachineManager virtualMachineManager;
    @Mock
    private VMTemplateDao templateDao;
    @Mock
    private VMInstanceDao vmDao;
    @Mock
    private UserVmDetailsDao userVmDetailsDao;
    @Mock
    private ServiceOfferingDao serviceOfferingDao;
    @Mock
    private DiskOfferingDao diskOfferingDao;

    @Test
    public void createVirtualMachineSetsCustomIopsFromVmDetails() throws Exception {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        DiskOfferingVO rootDiskOffering = Mockito.mock(DiskOfferingVO.class);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        VirtualMachineEntityImpl vmEntity = Mockito.mock(VirtualMachineEntityImpl.class);

        Mockito.when(vmDao.findByUuid("vm-uuid")).thenReturn(vm);
        Mockito.when(vm.getId()).thenReturn(VM_ID);
        Mockito.when(vm.getServiceOfferingId()).thenReturn(SERVICE_OFFERING_ID);
        Mockito.when(vm.getInstanceName()).thenReturn("i-1-1-VM");
        Mockito.when(serviceOfferingDao.findById(VM_ID, SERVICE_OFFERING_ID)).thenReturn(serviceOffering);
        Mockito.when(diskOfferingDao.findById(ROOT_DISK_OFFERING_ID)).thenReturn(rootDiskOffering);
        Mockito.when(rootDiskOffering.isCustomizedIops()).thenReturn(true);
        Mockito.when(templateDao.findById(Long.valueOf(TEMPLATE_ID))).thenReturn(template);

        Map<String, String> details = new HashMap<>();
        details.put(VmDetailConstants.MIN_IOPS, "100");
        details.put(VmDetailConstants.MAX_IOPS, "1000");
        Mockito.when(userVmDetailsDao.listDetailsKeyPairs(VM_ID)).thenReturn(details);

        try (MockedStatic<ComponentContext> componentContext = Mockito.mockStatic(ComponentContext.class)) {
            componentContext.when(() -> ComponentContext.inject(VirtualMachineEntityImpl.class)).thenReturn(vmEntity);

            cloudOrchestrator.createVirtualMachine("vm-uuid", "owner", TEMPLATE_ID, "host", "display", HypervisorType.KVM.name(),
                    1, 1000, 1024, null, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), null,
                    null, null, null, null, ROOT_DISK_OFFERING_ID);
        }

        ArgumentCaptor<DiskOfferingInfo> rootDiskOfferingInfo = ArgumentCaptor.forClass(DiskOfferingInfo.class);
        Mockito.verify(virtualMachineManager).allocate(Mockito.eq("i-1-1-VM"), Mockito.eq(template), Mockito.eq(serviceOffering), rootDiskOfferingInfo.capture(),
                Mockito.anyList(), Mockito.any(LinkedHashMap.class), Mockito.isNull(), Mockito.eq(HypervisorType.KVM), Mockito.isNull(), Mockito.isNull());

        Assert.assertEquals(Long.valueOf(100), rootDiskOfferingInfo.getValue().getMinIops());
        Assert.assertEquals(Long.valueOf(1000), rootDiskOfferingInfo.getValue().getMaxIops());
    }
}
