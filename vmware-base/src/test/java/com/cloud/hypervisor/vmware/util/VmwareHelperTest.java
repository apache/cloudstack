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

package com.cloud.hypervisor.vmware.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.cloud.agent.api.to.DiskTO;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.storage.Volume;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.vim25.DatastoreInfo;
import com.vmware.vim25.Description;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ParaVirtualSCSIController;
import com.vmware.vim25.VirtualAHCIController;
import com.vmware.vim25.VirtualController;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualNVMEController;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VirtualSCSISharing;
import org.apache.cloudstack.storage.DiskControllerMappingVO;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.vmware.vim25.VirtualDisk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class VmwareHelperTest {
    @Mock
    private VirtualMachineMO virtualMachineMO;
    @Mock
    private VmwareHypervisorHost vmwareHypervisorHostMock;
    @Mock
    private VirtualMachineConfigSpec virtualMachineConfigSpecMock;

    private static final String diskLabel = "disk1";
    private static final String diskFileBaseName = "xyz.vmdk";
    private static final String dataStoreName = "Datastore";
    private static final String vmName = "VM1";

    @Before
    public void setUp() throws Exception {
        VirtualDiskFlatVer2BackingInfo backingInfo = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        Mockito.when(backingInfo.getFileName()).thenReturn("abc");
        Mockito.when(backingInfo.getDatastore()).thenReturn(Mockito.mock(ManagedObjectReference.class));
        VirtualDisk disk = Mockito.mock(VirtualDisk.class);
        VirtualDisk[] disks = new VirtualDisk[1];
        disks[0] = disk;
        Description description = Mockito.mock(Description.class);
        Mockito.when(description.getLabel()).thenReturn(diskLabel);
        Mockito.when(description.getSummary()).thenReturn("");
        Mockito.when(disk.getBacking()).thenReturn(backingInfo);
        Mockito.when(disk.getDeviceInfo()).thenReturn(description);
        Mockito.when(virtualMachineMO.getAllDiskDevice()).thenReturn(disks);
        Mockito.when(virtualMachineMO.getVmdkFileBaseName(disk)).thenReturn(diskFileBaseName);

        DatastoreInfo datastoreInfo = Mockito.mock(DatastoreInfo.class);
        Mockito.when(datastoreInfo.getName()).thenReturn(dataStoreName);
        VmwareClient client = Mockito.mock(VmwareClient.class);
        Mockito.when(client.getDynamicProperty(Mockito.any(ManagedObjectReference.class), Mockito.anyString()))
                .thenReturn(datastoreInfo);
        VmwareContext context = Mockito.mock(VmwareContext.class);
        Mockito.when(context.getVimClient()).thenReturn(client);
        Mockito.when(virtualMachineMO.getContext()).thenReturn(context);
        Mockito.when(virtualMachineMO.getName()).thenReturn(vmName);

        DiskControllerMappingVO osdefaultMapping = new DiskControllerMappingVO();
        osdefaultMapping.setName("osdefault");
        osdefaultMapping.setControllerReference("osdefault");
        DiskControllerMappingVO ideMapping = new DiskControllerMappingVO();
        ideMapping.setName("ide");
        ideMapping.setControllerReference(VirtualIDEController.class.getName());
        ideMapping.setBusName("ide");
        ideMapping.setMaxDeviceCount(2);
        ideMapping.setMaxControllerCount(2);
        DiskControllerMappingVO lsilogicMapping = new DiskControllerMappingVO();
        lsilogicMapping.setName("lsilogic");
        lsilogicMapping.setControllerReference(VirtualLsiLogicController.class.getName());
        lsilogicMapping.setBusName("scsi");
        lsilogicMapping.setMaxDeviceCount(16);
        lsilogicMapping.setMaxControllerCount(4);
        DiskControllerMappingVO pvscsiMapping = new DiskControllerMappingVO();
        pvscsiMapping.setName("pvscsi");
        pvscsiMapping.setControllerReference(ParaVirtualSCSIController.class.getName());
        pvscsiMapping.setBusName("scsi");
        pvscsiMapping.setMaxDeviceCount(16);
        pvscsiMapping.setMaxControllerCount(4);
        DiskControllerMappingVO sataMapping = new DiskControllerMappingVO();
        sataMapping.setName("sata");
        sataMapping.setControllerReference(VirtualAHCIController.class.getName());
        sataMapping.setBusName("sata");
        sataMapping.setMaxDeviceCount(30);
        sataMapping.setMaxControllerCount(4);
        DiskControllerMappingVO nvmeMapping = new DiskControllerMappingVO();
        nvmeMapping.setName("nvme");
        nvmeMapping.setControllerReference(VirtualNVMEController.class.getName());
        nvmeMapping.setBusName("nvme");
        nvmeMapping.setMaxDeviceCount(15);
        nvmeMapping.setMaxControllerCount(4);
        VmwareHelper.setSupportedDiskControllers(List.of(osdefaultMapping, ideMapping, lsilogicMapping, pvscsiMapping, sataMapping, nvmeMapping));

        Mockito.when(virtualMachineConfigSpecMock.getDeviceChange()).thenReturn(new ArrayList<>());
    }

    @Test
    public void prepareDiskDeviceTestNotLimitingIOPS() throws Exception {
        Mockito.when(virtualMachineMO.getIDEDeviceControllerKey()).thenReturn(1);
        VirtualDisk virtualDisk = (VirtualDisk) VmwareHelper.prepareDiskDevice(virtualMachineMO, null, -1, new String[1], null, 0, 0, null);
        assertNull(virtualDisk.getStorageIOAllocation());
    }

    @Test
    public void prepareDiskDeviceTestLimitingIOPS() throws Exception {
        Mockito.when(virtualMachineMO.getIDEDeviceControllerKey()).thenReturn(1);
        VirtualDisk virtualDisk = (VirtualDisk) VmwareHelper.prepareDiskDevice(virtualMachineMO, null, -1, new String[1], null, 0, 0, Long.valueOf(1000));
        assertEquals(Long.valueOf(1000), virtualDisk.getStorageIOAllocation().getLimit());
    }

    @Test
    public void prepareDiskDeviceTestLimitingIOPSToZero() throws Exception {
        Mockito.when(virtualMachineMO.getIDEDeviceControllerKey()).thenReturn(1);
        VirtualDisk virtualDisk = (VirtualDisk) VmwareHelper.prepareDiskDevice(virtualMachineMO, null, -1, new String[1], null, 0, 0, Long.valueOf(0));
        assertNull(virtualDisk.getStorageIOAllocation());
    }

    @Test
    public void testGetUnmanageInstanceDisks() {
        List<UnmanagedInstanceTO.Disk> disks = VmwareHelper.getUnmanageInstanceDisks(virtualMachineMO);
        Assert.assertEquals(1, disks.size());
        UnmanagedInstanceTO.Disk disk = disks.get(0);
        Assert.assertEquals(diskLabel, disk.getLabel());
        Assert.assertEquals(diskFileBaseName, disk.getFileBaseName());
        Assert.assertEquals(dataStoreName, disk.getDatastoreName());
    }



    @Test
    public void isControllerOsRecommendedTestControllerIsOsRecommendedReturnsTrue() {
        DiskControllerMappingVO mapping = new DiskControllerMappingVO();
        mapping.setName("osdefault");

        boolean result = VmwareHelper.isControllerOsRecommended(mapping);

        Assert.assertTrue(result);
    }

    @Test
    public void isControllerOsRecommendedTestControllerIsNotOsRecommendedReturnsFalse() {
        DiskControllerMappingVO mapping = new DiskControllerMappingVO();
        mapping.setName("lsilogic");

        boolean result = VmwareHelper.isControllerOsRecommended(mapping);

        Assert.assertFalse(result);
    }

    @Test
    public void isControllerScsiTestControllerIsScsiReturnsTrue() {
        DiskControllerMappingVO mapping = new DiskControllerMappingVO();
        mapping.setBusName("scsi");

        boolean result = VmwareHelper.isControllerScsi(mapping);

        Assert.assertTrue(result);
    }

    @Test
    public void isControllerScsiTestControllerIsNotScsiReturnsFalse() {
        DiskControllerMappingVO mapping = new DiskControllerMappingVO();
        mapping.setBusName("nvme");

        boolean result = VmwareHelper.isControllerScsi(mapping);

        Assert.assertFalse(result);
    }

    @Test
    public void getDiskControllerMappingTestSearchByExistingNameReturnsObject() {
        String name = "lsilogic";

        DiskControllerMappingVO mapping = VmwareHelper.getDiskControllerMapping(name, null);

        Assert.assertEquals(name, mapping.getName());
    }

    @Test
    public void getDiskControllerMappingTestSearchByExistingControllerReferenceReturnsObject() {
        String classpath = VirtualLsiLogicController.class.getName();

        DiskControllerMappingVO mapping = VmwareHelper.getDiskControllerMapping(null, classpath);

        Assert.assertEquals(classpath, mapping.getControllerReference());
    }

    @Test(expected = CloudRuntimeException.class)
    public void getDiskControllerMappingTestThrowsExceptionWhenNoMatches() {
        VmwareHelper.getDiskControllerMapping("invalid", "invalid");
    }

    @Test
    public void getAllDiskControllerMappingsExceptOsDefaultTestReturnDoesNotContainOsDefaultMapping() {
        List<DiskControllerMappingVO> result = VmwareHelper.getAllSupportedDiskControllerMappingsExceptOsDefault();

        DiskControllerMappingVO osdefaultMapping = VmwareHelper.getDiskControllerMapping("osdefault", null);
        Assert.assertFalse(result.contains(osdefaultMapping));
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void getRequiredDiskControllersTestRequiresAllControllersWhenInstanceIsNotSystemVm() {
        Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo = new Pair<>(new DiskControllerMappingVO(), new DiskControllerMappingVO());

        Set<DiskControllerMappingVO> result = VmwareHelper.getRequiredDiskControllers(controllerInfo, false);

        Assert.assertTrue(result.contains(controllerInfo.first()));
        Assert.assertTrue(result.contains(controllerInfo.second()));
    }

    @Test
    public void getRequiredDiskControllersTestRequiresOnlyRootDiskControllerWhenInstanceIsSystemVm() {
        Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo = new Pair<>(new DiskControllerMappingVO(), new DiskControllerMappingVO());

        Set<DiskControllerMappingVO> result = VmwareHelper.getRequiredDiskControllers(controllerInfo, true);

        Assert.assertTrue(result.contains(controllerInfo.first()));
        Assert.assertFalse(result.contains(controllerInfo.second()));
    }

    @Test
    public void chooseDiskControllersDiskControllersTestControllersAreNotOsRecommendedReturnsProvidedControllers() throws Exception {
        DiskControllerMappingVO nvmeMapping = VmwareHelper.getDiskControllerMapping("nvme", null);
        DiskControllerMappingVO sataMapping = VmwareHelper.getDiskControllerMapping("sata", null);
        Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo = new Pair<>(nvmeMapping, sataMapping);
        Mockito.doReturn("VirtualLsiLogicController").when(virtualMachineMO).getRecommendedDiskController(null);

        Pair<DiskControllerMappingVO, DiskControllerMappingVO> result = VmwareHelper.chooseDiskControllers(controllerInfo, virtualMachineMO, null, null);

        Assert.assertEquals(nvmeMapping, result.first());
        Assert.assertEquals(sataMapping, result.second());
    }

    @Test
    public void chooseDiskControllersTestControllersAreOsRecommendedAndVmMoIsProvidedReturnsConvertedControllersBasedOnVmMo() throws Exception {
        DiskControllerMappingVO osdefaultMapping = VmwareHelper.getDiskControllerMapping("osdefault", null);
        Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo = new Pair<>(osdefaultMapping, osdefaultMapping);
        Mockito.doReturn("VirtualLsiLogicController").when(virtualMachineMO).getRecommendedDiskController(null);

        Pair<DiskControllerMappingVO, DiskControllerMappingVO> result = VmwareHelper.chooseDiskControllers(controllerInfo, virtualMachineMO, null, null);

        DiskControllerMappingVO lsilogicMapping = VmwareHelper.getDiskControllerMapping("lsilogic", null);
        Assert.assertEquals(lsilogicMapping, result.first());
        Assert.assertEquals(lsilogicMapping, result.second());
    }

    @Test
    public void chooseDiskControllersTestControllersAreOsRecommendedAndHostIsProvidedReturnsConvertedControllersBasedOnHost() throws Exception {
        DiskControllerMappingVO osdefaultMapping = VmwareHelper.getDiskControllerMapping("osdefault", null);
        Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo = new Pair<>(osdefaultMapping, osdefaultMapping);
        String guestOsId = "guestOsId";
        Mockito.doReturn("VirtualLsiLogicController").when(vmwareHypervisorHostMock).getRecommendedDiskController(guestOsId);

        Pair<DiskControllerMappingVO, DiskControllerMappingVO> result = VmwareHelper.chooseDiskControllers(controllerInfo, null, vmwareHypervisorHostMock, guestOsId);

        DiskControllerMappingVO lsilogicMapping = VmwareHelper.getDiskControllerMapping("lsilogic", null);
        Assert.assertEquals(lsilogicMapping, result.first());
        Assert.assertEquals(lsilogicMapping, result.second());
    }

    @Test
    public void chooseDiskControllersTestControllersShareTheSameBusTypeReturnsRootDiskController() throws Exception {
        DiskControllerMappingVO osdefaultMapping = VmwareHelper.getDiskControllerMapping("osdefault", null);
        DiskControllerMappingVO pvscsiMapping = VmwareHelper.getDiskControllerMapping("pvscsi", null);
        Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo = new Pair<>(osdefaultMapping, pvscsiMapping);
        Mockito.doReturn("VirtualLsiLogicController").when(virtualMachineMO).getRecommendedDiskController(null);

        Pair<DiskControllerMappingVO, DiskControllerMappingVO> result = VmwareHelper.chooseDiskControllers(controllerInfo, virtualMachineMO, null, null);

        DiskControllerMappingVO lsilogicMapping = VmwareHelper.getDiskControllerMapping("lsilogic", null);
        Assert.assertEquals(lsilogicMapping, result.first());
        Assert.assertEquals(lsilogicMapping, result.second());
    }

    @Test
    public void addDiskControllersToVmConfigSpecTestDoesNotAddIdeControllers() throws Exception {
        DiskControllerMappingVO ideMapping = VmwareHelper.getDiskControllerMapping("ide", null);
        Set<DiskControllerMappingVO> requiredControllers = new HashSet<>();
        requiredControllers.add(ideMapping);

        VmwareHelper.addDiskControllersToVmConfigSpec(virtualMachineConfigSpecMock, requiredControllers, false);

        Assert.assertEquals(0, virtualMachineConfigSpecMock.getDeviceChange().size());
    }

    @Test
    public void addDiskControllersToVmConfigSpecTestMaximumAmmountOfControllersIsAdded() throws Exception {
        DiskControllerMappingVO nvmeMapping = VmwareHelper.getDiskControllerMapping("nvme", null);
        DiskControllerMappingVO sataMapping = VmwareHelper.getDiskControllerMapping("sata", null);
        Set<DiskControllerMappingVO> requiredControllers = new HashSet<>();
        requiredControllers.add(nvmeMapping);
        requiredControllers.add(sataMapping);

        VmwareHelper.addDiskControllersToVmConfigSpec(virtualMachineConfigSpecMock, requiredControllers, false);

        int expectedControllerAmmount = nvmeMapping.getMaxControllerCount() + sataMapping.getMaxControllerCount();
        Assert.assertEquals(expectedControllerAmmount, virtualMachineConfigSpecMock.getDeviceChange().size());

        Set<Integer> usedKeys = new HashSet<>();
        Map<String, Set<Integer>> usedBusNumbers = new HashMap<>();
        usedBusNumbers.put(nvmeMapping.getControllerReference(), new HashSet<>());
        usedBusNumbers.put(sataMapping.getControllerReference(), new HashSet<>());
        for (VirtualDeviceConfigSpec virtualDeviceConfigSpec : virtualMachineConfigSpecMock.getDeviceChange()) {
            Assert.assertEquals(VirtualDeviceConfigSpecOperation.ADD, virtualDeviceConfigSpec.getOperation());
            VirtualController controller = (VirtualController) virtualDeviceConfigSpec.getDevice();
            usedKeys.add(controller.getKey());
            usedBusNumbers.get(controller.getClass().getName()).add(controller.getBusNumber());
        }
        Assert.assertEquals(expectedControllerAmmount, usedKeys.size());
        Assert.assertEquals((int) nvmeMapping.getMaxControllerCount(), usedBusNumbers.get(nvmeMapping.getControllerReference()).size());
        Assert.assertEquals((int) sataMapping.getMaxControllerCount(), usedBusNumbers.get(sataMapping.getControllerReference()).size());
    }

    @Test
    public void addDiskControllersToVmConfigSpecTestAddedScsiControllersDoNotShareBus() throws Exception {
        DiskControllerMappingVO lsilogicMapping = VmwareHelper.getDiskControllerMapping("lsilogic", null);
        Set<DiskControllerMappingVO> requiredControllers = new HashSet<>();
        requiredControllers.add(lsilogicMapping);

        VmwareHelper.addDiskControllersToVmConfigSpec(virtualMachineConfigSpecMock, requiredControllers, false);

        for (VirtualDeviceConfigSpec virtualDeviceConfigSpec : virtualMachineConfigSpecMock.getDeviceChange()) {
            Assert.assertEquals(VirtualSCSISharing.NO_SHARING, ((VirtualSCSIController) virtualDeviceConfigSpec.getDevice()).getSharedBus());
        }
    }

    @Test
    public void addDiskControllersToVmConfigSpecTestInstanceIsSystemVmAddsOneController() throws Exception {
        DiskControllerMappingVO lsilogicMapping = VmwareHelper.getDiskControllerMapping("lsilogic", null);
        Set<DiskControllerMappingVO> requiredControllers = new HashSet<>();
        requiredControllers.add(lsilogicMapping);

        VmwareHelper.addDiskControllersToVmConfigSpec(virtualMachineConfigSpecMock, requiredControllers, true);

        Assert.assertEquals(1, virtualMachineConfigSpecMock.getDeviceChange().size());
    }

    @Test
    public void getDiskControllersFromVmSettingsTestReturnsSpecifiedControllersWhenInstanceIsNotSystemVm() {
        Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo = VmwareHelper.getDiskControllersFromVmSettings("nvme", "sata", false);

        Assert.assertEquals("nvme", controllerInfo.first().getName());
        Assert.assertEquals("sata", controllerInfo.second().getName());
    }

    @Test
    public void getDiskControllersFromVmSettingsTestReturnsLsiLogicForRootDiskWhenInstanceIsSystemVm() {
        Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo = VmwareHelper.getDiskControllersFromVmSettings("nvme", "sata", true);

        Assert.assertEquals("lsilogic", controllerInfo.first().getName());
        Assert.assertEquals("sata", controllerInfo.second().getName());
    }

    @Test
    public void getControllerBasedOnDiskTypeTestReturnsRootDiskControllerWhenVolumeTypeIsRoot() {
        DiskControllerMappingVO rootDiskController = new DiskControllerMappingVO();
        rootDiskController.setControllerReference("root");
        Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo = new Pair<>(rootDiskController, new DiskControllerMappingVO());
        DiskTO disk = new DiskTO();
        disk.setType(Volume.Type.ROOT);

        DiskControllerMappingVO result = VmwareHelper.getControllerBasedOnDiskType(controllerInfo, disk);

        Assert.assertEquals(rootDiskController, result);
    }

    @Test
    public void getControllerBasedOnDiskTypeTestReturnsRootDiskControllerWhenVolumeDiskSeqIsZero() {
        DiskControllerMappingVO rootDiskController = new DiskControllerMappingVO();
        rootDiskController.setControllerReference("root");
        Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo = new Pair<>(rootDiskController, new DiskControllerMappingVO());
        DiskTO disk = new DiskTO();
        disk.setDiskSeq(0L);

        DiskControllerMappingVO result = VmwareHelper.getControllerBasedOnDiskType(controllerInfo, disk);

        Assert.assertEquals(rootDiskController, result);
    }

    @Test
    public void getControllerBasedOnDiskTypeTestReturnsDataDiskControllerWhenVolumeTypeIsNotRootAndDiskSeqIsNotZero() {
        DiskControllerMappingVO dataDiskController = new DiskControllerMappingVO();
        dataDiskController.setControllerReference("data");
        Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo = new Pair<>(new DiskControllerMappingVO(), dataDiskController);
        DiskTO disk = new DiskTO();
        disk.setType(Volume.Type.DATADISK);
        disk.setDiskSeq(1L);

        DiskControllerMappingVO result = VmwareHelper.getControllerBasedOnDiskType(controllerInfo, disk);

        Assert.assertEquals(dataDiskController, result);
    }

    @Test
    public void configureDiskControllerMappingsInVmwareBaseModuleTestOsdefaultIsConfigured() {
        DiskControllerMappingVO osdefaultMappingVo = new DiskControllerMappingVO();
        osdefaultMappingVo.setName("osdefault");
        osdefaultMappingVo.setControllerReference("osdefault");

        VmwareHelper.configureDiskControllerMappingsInVmwareBaseModule(List.of(osdefaultMappingVo));

        Assert.assertEquals(List.of(osdefaultMappingVo), VmwareHelper.getAllSupportedDiskControllerMappings());
    }

    @Test
    public void configureDiskControllerMappingsInVmwareBaseModuleTestLsiLogicIsConfigured() {
        DiskControllerMappingVO lsilogicMappingVo = new DiskControllerMappingVO();
        lsilogicMappingVo.setName("lsilogic");
        lsilogicMappingVo.setControllerReference(VirtualLsiLogicController.class.getName());

        VmwareHelper.configureDiskControllerMappingsInVmwareBaseModule(List.of(lsilogicMappingVo));

        Assert.assertEquals(List.of(lsilogicMappingVo), VmwareHelper.getAllSupportedDiskControllerMappings());
    }

    @Test
    public void configureDiskControllerMappingsInVmwareBaseModuleTestInvalidMappingIsNotConfigured() {
        DiskControllerMappingVO invalidMappingVo = new DiskControllerMappingVO();
        invalidMappingVo.setName("invalid");
        invalidMappingVo.setControllerReference("invalid");

        VmwareHelper.configureDiskControllerMappingsInVmwareBaseModule(List.of(invalidMappingVo));

        Assert.assertEquals(0, VmwareHelper.getAllSupportedDiskControllerMappings().size());
    }
}
