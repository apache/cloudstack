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
import com.cloud.hypervisor.vmware.mo.ClusterMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.storage.Volume;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.vim25.DatastoreInfo;
import com.vmware.vim25.Description;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualController;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VirtualSCSISharing;
import com.cloud.hypervisor.vmware.mo.DiskControllerType;
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
import java.util.Collections;
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
        DiskControllerType controllerType = DiskControllerType.osdefault;

        boolean result = VmwareHelper.isControllerOsRecommended(controllerType);

        Assert.assertTrue(result);
    }

    @Test
    public void isControllerOsRecommendedTestControllerIsNotOsRecommendedReturnsFalse() {
        DiskControllerType controllerType = DiskControllerType.lsilogic;

        boolean result = VmwareHelper.isControllerOsRecommended(controllerType);

        Assert.assertFalse(result);
    }

    @Test
    public void isControllerScsiTestControllerIsScsiReturnsTrue() {
        DiskControllerType controllerType = DiskControllerType.lsilogic;

        boolean result = VmwareHelper.isControllerScsi(controllerType);

        Assert.assertTrue(result);
    }

    @Test
    public void isControllerScsiTestControllerIsNotScsiReturnsFalse() {
        DiskControllerType controllerType = DiskControllerType.nvme;

        boolean result = VmwareHelper.isControllerScsi(controllerType);

        Assert.assertFalse(result);
    }

    @Test
    public void getDiskControllerTypeTestSearchByExistingNameReturnsObject() {
        String name = "lsilogic";

        DiskControllerType controllerType = VmwareHelper.getDiskControllerType(name, null);

        Assert.assertEquals(name, controllerType.getName());
    }

    @Test
    public void getDiskControllerTypeTestSearchByExistingControllerReferenceReturnsObject() {
        String classpath = VirtualLsiLogicController.class.getName();

        DiskControllerType controllerType = VmwareHelper.getDiskControllerType(null, classpath);

        Assert.assertEquals(classpath, controllerType.getControllerReference());
    }

    @Test
    public void getDiskControllerTypeTestSearchIsCaseInsensitiveAndAcceptsAliases() {
        Assert.assertEquals(DiskControllerType.lsisas1068, VmwareHelper.getDiskControllerType("LSILogicSAS", null));
        Assert.assertEquals(DiskControllerType.pvscsi, VmwareHelper.getDiskControllerType("VirtualSCSI", null));
        Assert.assertEquals(DiskControllerType.lsilogic, VmwareHelper.getDiskControllerType("VirtualLsiLogicController", null));
        Assert.assertEquals(DiskControllerType.ide, VmwareHelper.getDiskControllerType("vim.vm.device.VirtualIDEController", null));
    }

    @Test(expected = CloudRuntimeException.class)
    public void getDiskControllerTypeTestThrowsExceptionWhenNoMatches() {
        VmwareHelper.getDiskControllerType("invalid", "invalid");
    }

    @Test
    public void getAllSupportedDiskControllerTypesExceptOsDefaultTestDoesNotContainOsDefault() {
        List<DiskControllerType> result = VmwareHelper.getAllSupportedDiskControllerTypesExceptOsDefault();

        DiskControllerType osdefaultController = VmwareHelper.getDiskControllerType("osdefault", null);
        Assert.assertFalse(result.contains(osdefaultController));
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void getRequiredDiskControllersTestRequiresAllControllersWhenInstanceIsNotSystemVm() {
        Pair<DiskControllerType, DiskControllerType> controllerInfo = new Pair<>(DiskControllerType.lsilogic, DiskControllerType.nvme);

        Set<DiskControllerType> result = VmwareHelper.getRequiredDiskControllers(controllerInfo, false);

        Assert.assertTrue(result.contains(controllerInfo.first()));
        Assert.assertTrue(result.contains(controllerInfo.second()));
    }

    @Test
    public void getRequiredDiskControllersTestRequiresOnlyRootDiskControllerWhenInstanceIsSystemVm() {
        Pair<DiskControllerType, DiskControllerType> controllerInfo = new Pair<>(DiskControllerType.lsilogic, DiskControllerType.nvme);

        Set<DiskControllerType> result = VmwareHelper.getRequiredDiskControllers(controllerInfo, true);

        Assert.assertTrue(result.contains(controllerInfo.first()));
        Assert.assertFalse(result.contains(controllerInfo.second()));
    }

    @Test
    public void chooseDiskControllersDiskControllersTestControllersAreNotOsRecommendedReturnsProvidedControllers() throws Exception {
        DiskControllerType nvmeController = VmwareHelper.getDiskControllerType("nvme", null);
        DiskControllerType sataController = VmwareHelper.getDiskControllerType("sata", null);
        Pair<DiskControllerType, DiskControllerType> controllerInfo = new Pair<>(nvmeController, sataController);

        Pair<DiskControllerType, DiskControllerType> result = VmwareHelper.chooseDiskControllers(controllerInfo, virtualMachineMO, null, null);

        Assert.assertEquals(nvmeController, result.first());
        Assert.assertEquals(sataController, result.second());
        Mockito.verify(virtualMachineMO, Mockito.never()).getRecommendedDiskController(null);
    }

    @Test
    public void chooseDiskControllersTestControllersAreOsRecommendedAndVmMoIsProvidedReturnsConvertedControllersBasedOnVmMo() throws Exception {
        DiskControllerType osdefaultController = VmwareHelper.getDiskControllerType("osdefault", null);
        Pair<DiskControllerType, DiskControllerType> controllerInfo = new Pair<>(osdefaultController, osdefaultController);
        Mockito.doReturn("VirtualLsiLogicController").when(virtualMachineMO).getRecommendedDiskController(null);

        Pair<DiskControllerType, DiskControllerType> result = VmwareHelper.chooseDiskControllers(controllerInfo, virtualMachineMO, null, null);

        DiskControllerType lsilogicController = VmwareHelper.getDiskControllerType("lsilogic", null);
        Assert.assertEquals(lsilogicController, result.first());
        Assert.assertEquals(lsilogicController, result.second());
    }

    @Test
    public void chooseDiskControllersTestResolvesAliasedRecommendedController() throws Exception {
        DiskControllerType osdefaultController = VmwareHelper.getDiskControllerType("osdefault", null);
        Pair<DiskControllerType, DiskControllerType> controllerInfo = new Pair<>(osdefaultController, osdefaultController);
        Mockito.doReturn("lsilogicsas").when(virtualMachineMO).getRecommendedDiskController(null);

        Pair<DiskControllerType, DiskControllerType> result = VmwareHelper.chooseDiskControllers(controllerInfo, virtualMachineMO, null, null);

        Assert.assertEquals(DiskControllerType.lsisas1068, result.first());
        Assert.assertEquals(DiskControllerType.lsisas1068, result.second());
    }

    @Test
    public void chooseDiskControllersTestControllersAreOsRecommendedAndHostIsProvidedReturnsConvertedControllersBasedOnHost() throws Exception {
        DiskControllerType osdefaultController = VmwareHelper.getDiskControllerType("osdefault", null);
        Pair<DiskControllerType, DiskControllerType> controllerInfo = new Pair<>(osdefaultController, osdefaultController);
        String guestOsId = "guestOsId";
        Mockito.doReturn("VirtualLsiLogicController").when(vmwareHypervisorHostMock).getRecommendedDiskController(guestOsId);

        Pair<DiskControllerType, DiskControllerType> result = VmwareHelper.chooseDiskControllers(controllerInfo, null, vmwareHypervisorHostMock, guestOsId);

        DiskControllerType lsilogicController = VmwareHelper.getDiskControllerType("lsilogic", null);
        Assert.assertEquals(lsilogicController, result.first());
        Assert.assertEquals(lsilogicController, result.second());
    }

    @Test
    public void chooseDiskControllersTestControllersShareTheSameBusTypeReturnsRootDiskController() throws Exception {
        DiskControllerType osdefaultController = VmwareHelper.getDiskControllerType("osdefault", null);
        DiskControllerType pvscsiController = VmwareHelper.getDiskControllerType("pvscsi", null);
        Pair<DiskControllerType, DiskControllerType> controllerInfo = new Pair<>(osdefaultController, pvscsiController);
        Mockito.doReturn("VirtualLsiLogicController").when(virtualMachineMO).getRecommendedDiskController(null);

        Pair<DiskControllerType, DiskControllerType> result = VmwareHelper.chooseDiskControllers(controllerInfo, virtualMachineMO, null, null);

        DiskControllerType lsilogicController = VmwareHelper.getDiskControllerType("lsilogic", null);
        Assert.assertEquals(lsilogicController, result.first());
        Assert.assertEquals(lsilogicController, result.second());
    }

    @Test
    public void addDiskControllersToVmConfigSpecTestDoesNotAddIdeControllers() throws Exception {
        DiskControllerType ideController = VmwareHelper.getDiskControllerType("ide", null);
        Set<DiskControllerType> requiredControllers = new HashSet<>();
        requiredControllers.add(ideController);

        VmwareHelper.addDiskControllersToVmConfigSpec(virtualMachineConfigSpecMock, requiredControllers, false);

        Assert.assertEquals(0, virtualMachineConfigSpecMock.getDeviceChange().size());
    }

    @Test
    public void addDiskControllersToVmConfigSpecTestMaximumAmmountOfControllersIsAdded() throws Exception {
        DiskControllerType nvmeController = VmwareHelper.getDiskControllerType("nvme", null);
        DiskControllerType sataController = VmwareHelper.getDiskControllerType("sata", null);
        Set<DiskControllerType> requiredControllers = new HashSet<>();
        requiredControllers.add(nvmeController);
        requiredControllers.add(sataController);

        VmwareHelper.addDiskControllersToVmConfigSpec(virtualMachineConfigSpecMock, requiredControllers, false);

        int expectedControllerAmount = nvmeController.getMaxControllerCount() + sataController.getMaxControllerCount();
        Assert.assertEquals(expectedControllerAmount, virtualMachineConfigSpecMock.getDeviceChange().size());

        Set<Integer> usedKeys = new HashSet<>();
        Map<String, Set<Integer>> usedBusNumbers = new HashMap<>();
        usedBusNumbers.put(nvmeController.getControllerReference(), new HashSet<>());
        usedBusNumbers.put(sataController.getControllerReference(), new HashSet<>());
        for (VirtualDeviceConfigSpec virtualDeviceConfigSpec : virtualMachineConfigSpecMock.getDeviceChange()) {
            Assert.assertEquals(VirtualDeviceConfigSpecOperation.ADD, virtualDeviceConfigSpec.getOperation());
            VirtualController controller = (VirtualController) virtualDeviceConfigSpec.getDevice();
            usedKeys.add(controller.getKey());
            usedBusNumbers.get(controller.getClass().getName()).add(controller.getBusNumber());
        }
        Assert.assertEquals(expectedControllerAmount, usedKeys.size());
        Assert.assertEquals((int) nvmeController.getMaxControllerCount(), usedBusNumbers.get(nvmeController.getControllerReference()).size());
        Assert.assertEquals((int) sataController.getMaxControllerCount(), usedBusNumbers.get(sataController.getControllerReference()).size());
    }

    @Test
    public void addDiskControllersToVmConfigSpecTestAddedScsiControllersDoNotShareBus() throws Exception {
        DiskControllerType lsilogicController = VmwareHelper.getDiskControllerType("lsilogic", null);
        Set<DiskControllerType> requiredControllers = new HashSet<>();
        requiredControllers.add(lsilogicController);

        VmwareHelper.addDiskControllersToVmConfigSpec(virtualMachineConfigSpecMock, requiredControllers, false);

        for (VirtualDeviceConfigSpec virtualDeviceConfigSpec : virtualMachineConfigSpecMock.getDeviceChange()) {
            Assert.assertEquals(VirtualSCSISharing.NO_SHARING, ((VirtualSCSIController) virtualDeviceConfigSpec.getDevice()).getSharedBus());
        }
    }

    @Test
    public void addDiskControllersToVmConfigSpecTestInstanceIsSystemVmAddsOneController() throws Exception {
        DiskControllerType lsilogicController = VmwareHelper.getDiskControllerType("lsilogic", null);
        Set<DiskControllerType> requiredControllers = new HashSet<>();
        requiredControllers.add(lsilogicController);

        VmwareHelper.addDiskControllersToVmConfigSpec(virtualMachineConfigSpecMock, requiredControllers, true);

        Assert.assertEquals(1, virtualMachineConfigSpecMock.getDeviceChange().size());
    }

    @Test
    public void getDiskControllersFromVmSettingsTestReturnsSpecifiedControllersWhenInstanceIsNotSystemVm() {
        Pair<DiskControllerType, DiskControllerType> controllerInfo = VmwareHelper.getDiskControllersFromVmSettings("nvme", "sata", false);

        Assert.assertEquals("nvme", controllerInfo.first().getName());
        Assert.assertEquals("sata", controllerInfo.second().getName());
    }

    @Test
    public void getDiskControllersFromVmSettingsTestReturnsLsiLogicForRootDiskWhenInstanceIsSystemVm() {
        Pair<DiskControllerType, DiskControllerType> controllerInfo = VmwareHelper.getDiskControllersFromVmSettings("nvme", "sata", true);

        Assert.assertEquals("lsilogic", controllerInfo.first().getName());
        Assert.assertEquals("sata", controllerInfo.second().getName());
    }

    @Test
    public void getDiskControllersFromVmSettingsTestReturnsOsDefaultForMissingDataDiskControllerWhenInstanceIsNotSystemVm() {
        Pair<DiskControllerType, DiskControllerType> controllerInfo = VmwareHelper.getDiskControllersFromVmSettings("ide", null, false);

        Assert.assertEquals(DiskControllerType.ide, controllerInfo.first());
        Assert.assertEquals(DiskControllerType.osdefault, controllerInfo.second());
    }

    @Test
    public void getControllerBasedOnDiskTypeTestReturnsRootDiskControllerWhenVolumeTypeIsRoot() {
        Pair<DiskControllerType, DiskControllerType> controllerInfo = new Pair<>(DiskControllerType.lsilogic, DiskControllerType.nvme);
        DiskTO disk = new DiskTO();
        disk.setType(Volume.Type.ROOT);

        DiskControllerType result = VmwareHelper.getControllerBasedOnDiskType(controllerInfo, disk);

        Assert.assertEquals(DiskControllerType.lsilogic, result);
    }

    @Test
    public void getControllerBasedOnDiskTypeTestReturnsRootDiskControllerWhenVolumeDiskSeqIsZero() {
        Pair<DiskControllerType, DiskControllerType> controllerInfo = new Pair<>(DiskControllerType.lsilogic, DiskControllerType.nvme);
        DiskTO disk = new DiskTO();
        disk.setDiskSeq(0L);

        DiskControllerType result = VmwareHelper.getControllerBasedOnDiskType(controllerInfo, disk);

        Assert.assertEquals(DiskControllerType.lsilogic, result);
    }

    @Test
    public void getControllerBasedOnDiskTypeTestReturnsDataDiskControllerWhenVolumeTypeIsNotRootAndDiskSeqIsNotZero() {
        Pair<DiskControllerType, DiskControllerType> controllerInfo = new Pair<>(DiskControllerType.lsilogic, DiskControllerType.nvme);
        DiskTO disk = new DiskTO();
        disk.setType(Volume.Type.DATADISK);
        disk.setDiskSeq(1L);

        DiskControllerType result = VmwareHelper.getControllerBasedOnDiskType(controllerInfo, disk);

        Assert.assertEquals(DiskControllerType.nvme, result);
    }

    @Test
    public void testGetVmwareHostVersionException() throws Exception {
        HostMO hostMO = Mockito.mock(HostMO.class);
        Mockito.when(hostMO.getProductVersion()).thenThrow(new Exception("Error obtaining host version"));
        Assert.assertNull(VmwareHelper.getVmwareHostVersion(hostMO));
    }

    @Test
    public void testGetVmwareHostVersion() throws Exception {
        HostMO hostMO = Mockito.mock(HostMO.class);
        String version = "8.0.3";
        Mockito.when(hostMO.getProductVersion()).thenReturn(version);
        Assert.assertEquals(version, VmwareHelper.getVmwareHostVersion(hostMO));
    }

    @Test
    public void testGetVmwareHostVersionInvalidParameter() {
        ClusterMO clusterMO = Mockito.mock(ClusterMO.class);
        Assert.assertNull(VmwareHelper.getVmwareHostVersion(clusterMO));
    }

    @Test
    public void prepareIsoDeviceTestUsesIdeControllerAndDeviceNumberFromGlobalUnitNumber() throws Exception {
        VirtualController secondIdeController = new VirtualIDEController();
        secondIdeController.setKey(42);
        Mockito.when(virtualMachineMO.getIsoDevice()).thenReturn(null);
        Mockito.when(virtualMachineMO.getNthDevice(DiskControllerType.ide.getControllerReference(), 1)).thenReturn(secondIdeController);

        Pair<VirtualDevice, Boolean> result = VmwareHelper.prepareIsoDevice(virtualMachineMO, null, null, true, true, 2, 1);

        VirtualCdrom cdRom = (VirtualCdrom) result.first();
        Assert.assertTrue(result.second());
        Assert.assertEquals(42, (int) cdRom.getControllerKey());
        Assert.assertEquals(0, (int) cdRom.getUnitNumber());
    }

    @Test
    public void validateDiskControllersHardwareVersionTestDoesNothingWhenNoControllersAreProvided() {
        VmwareHelper.validateDiskControllersHardwareVersion(Collections.emptySet(), 0);
    }

    @Test
    public void validateDiskControllersHardwareVersionTestDoesNothingWhenControllersDoNotRequireMinimumHardwareVersion() {
        VmwareHelper.validateDiskControllersHardwareVersion(Set.of(DiskControllerType.ide, DiskControllerType.lsilogic), 0);
    }

    @Test
    public void validateDiskControllersHardwareVersionTestDoesNothingWhenHardwareVersionEqualsMinimum() {
        VmwareHelper.validateDiskControllersHardwareVersion(Set.of(DiskControllerType.ahci), 10);
    }

    @Test
    public void validateDiskControllersHardwareVersionTestDoesNothingWhenHardwareVersionIsHigherThanMinimum() {
        VmwareHelper.validateDiskControllersHardwareVersion(Set.of(DiskControllerType.nvme), 14);
    }

    @Test(expected = CloudRuntimeException.class)
    public void validateDiskControllersHardwareVersionTestThrowsExceptionWhenHardwareVersionIsTooOld() {
        VmwareHelper.validateDiskControllersHardwareVersion(Set.of(DiskControllerType.nvme), 12);
    }

    @Test
    public void validateDiskControllersHardwareVersionTestThrowsExceptionListingAllUnsupportedControllers() {
        try {
            VmwareHelper.validateDiskControllersHardwareVersion(Set.of(DiskControllerType.pvscsi, DiskControllerType.nvme), 6);
            Assert.fail("Expected CloudRuntimeException");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains(DiskControllerType.pvscsi.getName()));
            Assert.assertTrue(e.getMessage().contains(DiskControllerType.nvme.getName()));
        }
    }
}
