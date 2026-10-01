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
package com.cloud.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cloudstack.context.CallContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.dc.DataCenter;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.UUIDManager;
import com.cloud.vm.dao.UserVmDao;

public class UserVmImportTemplateTest {
    private final UserVmManagerImpl manager = new UserVmManagerImpl();
    private final VMTemplateDao templateDao = mock(VMTemplateDao.class);
    private final UserVmDao vmDao = mock(UserVmDao.class);
    private final DataCenter zone = mock(DataCenter.class);
    private final Account owner = mock(Account.class);
    private final ServiceOffering offering = mock(ServiceOffering.class);

    @Before
    public void setUp() {
        UUIDManager uuidManager = mock(UUIDManager.class);
        GuestOSDao guestOSDao = mock(GuestOSDao.class);
        GuestOSCategoryDao categoryDao = mock(GuestOSCategoryDao.class);
        GuestOSVO guestOS = mock(GuestOSVO.class);
        ReflectionTestUtils.setField(manager, "_templateDao", templateDao);
        ReflectionTestUtils.setField(manager, "_vmDao", vmDao);
        ReflectionTestUtils.setField(manager, "_uuidMgr", uuidManager);
        ReflectionTestUtils.setField(manager, "_itMgr", mock(VirtualMachineManager.class));
        ReflectionTestUtils.setField(manager, "_guestOSDao", guestOSDao);
        ReflectionTestUtils.setField(manager, "_guestOSCategoryDao", categoryDao);
        when(zone.getId()).thenReturn(1L);
        when(owner.getId()).thenReturn(10L);
        when(owner.getDomainId()).thenReturn(11L);
        when(offering.getId()).thenReturn(12L);
        when(vmDao.getNextInSequence(Long.class, "id")).thenReturn(42L);
        when(uuidManager.generateUuid(UserVm.class, null)).thenReturn("b90b3f01-bbbd-4129-af59-d628c92d935f");
        when(guestOSDao.findById(5L)).thenReturn(guestOS);
        when(guestOS.getCategoryId()).thenReturn(6L);
        when(categoryDao.findById(6L)).thenReturn(mock(GuestOSCategoryVO.class));
    }

    @Test
    public void importsSoftDeletedDummyTemplateWithoutReloadingIt() throws InsufficientCapacityException {
        VMTemplateVO template = createTemplate();
        template.setRemoved(new Date());
        template.setState(VirtualMachineTemplate.State.Inactive);
        doAnswer(invocation -> {
            template.setDetails(new HashMap<>());
            return null;
        }).when(templateDao).loadDetails(template);

        UserVmVO vm = importTemplate(template);

        assertNotNull(vm);
        assertEquals(203L, vm.getTemplateId());
        assertNotNull(template.getRemoved());
        verify(templateDao).loadDetails(template);
        verifyNoMoreInteractions(templateDao);
        verify(vmDao).persist(vm);
    }

    @Test
    public void copiesLoadedCpuModeFromActiveTemplateIntoImportedVm() throws InsufficientCapacityException {
        VMTemplateVO template = createTemplate();
        template.setState(VirtualMachineTemplate.State.Active);
        doAnswer(invocation -> {
            template.setDetails(Collections.singletonMap("guest.cpu.mode", "host-passthrough"));
            return null;
        }).when(templateDao).loadDetails(template);

        UserVmVO vm = importTemplate(template);

        assertEquals("host-passthrough", vm.getDetail("guest.cpu.mode"));
        verify(templateDao).loadDetails(template);
        verifyNoMoreInteractions(templateDao);
        verify(vmDao).saveDetails(vm, Collections.emptyList());
    }

    @Test
    public void importsNonVoTemplateUsingItsExistingDetails() throws InsufficientCapacityException {
        VirtualMachineTemplate template = mock(VirtualMachineTemplate.class);
        when(template.getId()).thenReturn(204L);
        when(template.getFormat()).thenReturn(ImageFormat.QCOW2);
        when(template.getDetails()).thenReturn(Collections.singletonMap("guest.cpu.mode", "host-model"));

        UserVmVO vm = importTemplate(template);

        assertEquals(204L, vm.getTemplateId());
        assertEquals("host-model", vm.getDetail("guest.cpu.mode"));
        verifyNoInteractions(templateDao);
    }

    @Test
    public void explicitVmCpuDetailsOverrideTemplateDefaults() throws InsufficientCapacityException {
        VMTemplateVO template = createTemplate();
        doAnswer(invocation -> {
            template.setDetails(Map.of(VmDetailConstants.GUEST_CPU_MODE, "custom",
                    VmDetailConstants.GUEST_CPU_MODEL, "Skylake-Client"));
            return null;
        }).when(templateDao).loadDetails(template);
        Map<String, String> vmDetails = Map.of(VmDetailConstants.GUEST_CPU_MODE, "custom",
                VmDetailConstants.GUEST_CPU_MODEL, "EPYC");

        UserVmVO vm = importTemplate(template, vmDetails);

        assertEquals("custom", vm.getDetail(VmDetailConstants.GUEST_CPU_MODE));
        assertEquals("EPYC", vm.getDetail(VmDetailConstants.GUEST_CPU_MODEL));
        assertEquals("Skylake-Client", template.getDetails().get(VmDetailConstants.GUEST_CPU_MODEL));
    }

    @Test
    public void explicitVmCpuModeOverridesTemplateMode() throws InsufficientCapacityException {
        VMTemplateVO template = createTemplate();
        doAnswer(invocation -> {
            template.setDetails(Collections.singletonMap(VmDetailConstants.GUEST_CPU_MODE, "host-passthrough"));
            return null;
        }).when(templateDao).loadDetails(template);

        UserVmVO vm = importTemplate(template, Collections.singletonMap(VmDetailConstants.GUEST_CPU_MODE, "host-model"));

        assertEquals("host-model", vm.getDetail(VmDetailConstants.GUEST_CPU_MODE));
    }

    @Test
    public void stillRejectsMissingTemplate() {
        InvalidParameterValueException exception = assertThrows(InvalidParameterValueException.class, () -> importTemplate(null));

        assertEquals("Unable to import virtual machine without a template", exception.getMessage());
        verifyNoInteractions(templateDao, vmDao);
    }

    private VMTemplateVO createTemplate() {
        return VMTemplateVO.createSystemIso(203L, "import-test-template", "import-test-template", false,
                "", true, 64, Account.ACCOUNT_ID_SYSTEM, "", "Import test template", false, 5L);
    }

    private UserVmVO importTemplate(VirtualMachineTemplate template) throws InsufficientCapacityException {
        return importTemplate(template, new HashMap<>());
    }

    private UserVmVO importTemplate(VirtualMachineTemplate template, Map<String, String> vmDetails) throws InsufficientCapacityException {
        try (MockedStatic<Transaction> transaction = mockStatic(Transaction.class);
                MockedStatic<CallContext> context = mockStatic(CallContext.class)) {
            transaction.when(() -> Transaction.execute(any(TransactionCallbackWithException.class))).thenAnswer(invocation -> {
                TransactionCallbackWithException<UserVm, InsufficientCapacityException> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            context.when(CallContext::current).thenReturn(mock(CallContext.class));
            return (UserVmVO) manager.importVM(zone, null, template, "import-test-vm", "Import test VM", owner,
                    null, owner, true, null, 10L, 20L, offering, null, 5L, null, HypervisorType.KVM,
                    vmDetails, VirtualMachine.PowerState.PowerOff, new LinkedHashMap<>());
        }
    }
}
