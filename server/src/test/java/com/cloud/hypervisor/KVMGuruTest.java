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
package com.cloud.hypervisor;

import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.configuration.ConfigurationManagerImpl;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.kvm.dpdk.DpdkHelper;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.api.ApiConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.junit.Assert;

@RunWith(MockitoJUnitRunner.class)
public class KVMGuruTest {

    @Mock
    HostDao hostDao;
    @Mock
    ServiceOfferingDetailsDao serviceOfferingDetailsDao;

    @Spy
    @InjectMocks
    private KVMGuru guru = new KVMGuru();

    @Mock
    VirtualMachineTO vmTO;
    @Mock
    VirtualMachineProfile vmProfile;
    @Mock
    VirtualMachine vm;
    @Mock
    HostVO host;
    @Mock
    ServiceOffering serviceOffering;
    @Mock
    ServiceOfferingDetailsVO detail1;
    @Mock
    ServiceOfferingDetailsVO detail2;

    @Mock
    ServiceOfferingVO serviceOfferingVoMock;

    @Mock
    VirtualMachine virtualMachineMock;

    @Mock
    ServiceOfferingDao serviceOfferingDaoMock;

    @Mock
    DpdkHelper dpdkHelperMock;

    @Mock
    GuestOSVO guestOsVoMock;

    @Mock
    GuestOSHypervisorVO guestOsMappingMock;

    @Mock
    GuestOSHypervisorDao guestOSHypervisorDaoMock;

    @Mock
    GuestOSDao guestOsDaoMock;

    private static final long hostId = 1L;
    private static final Long offeringId = 1L;

    private static final String detail1Key = ApiConstants.EXTRA_CONFIG + "-config-1";
    private static final String detail1Value = "value1";
    private static final String detail2Key = "detail2";
    private static final String detail2Value = "value2";

    @Before
    public void setup() throws UnsupportedEncodingException {
        Mockito.when(vmTO.getLimitCpuUse()).thenReturn(true);
        Mockito.when(vmProfile.getVirtualMachine()).thenReturn(vm);
        Mockito.when(vm.getHostId()).thenReturn(hostId);
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);
        Mockito.lenient().when(host.getCpus()).thenReturn(3);
        Mockito.when(host.getSpeed()).thenReturn(1995L);
        Mockito.when(vmTO.getMaxSpeed()).thenReturn(500);
        Mockito.lenient().when(serviceOffering.getId()).thenReturn(offeringId);
        Mockito.lenient().when(vmProfile.getServiceOffering()).thenReturn(serviceOffering);

        Mockito.lenient().when(detail1.getName()).thenReturn(detail1Key);
        Mockito.lenient().when(detail1.getValue()).thenReturn(detail1Value);
        Mockito.lenient().when(detail1.getResourceId()).thenReturn(offeringId);
        Mockito.lenient().when(detail2.getName()).thenReturn(detail2Key);
        Mockito.lenient().when(detail2.getResourceId()).thenReturn(offeringId);
        Mockito.lenient().when(detail2.getValue()).thenReturn(detail2Value);

        Mockito.lenient().when(serviceOfferingDetailsDao.listDetails(offeringId)).thenReturn(
                Arrays.asList(detail1, detail2));
    }

    @Test
    public void testSetVmQuotaPercentage() {
        guru.setVmQuotaPercentage(vmTO, vmProfile);
        Mockito.verify(vmTO).setCpuQuotaPercentage(Mockito.anyDouble());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testSetVmQuotaPercentageNullHost() {
        Mockito.when(hostDao.findById(hostId)).thenReturn(null);
        guru.setVmQuotaPercentage(vmTO, vmProfile);
    }

    @Test
    public void testSetVmQuotaPercentageZeroDivision() {
        Mockito.when(host.getSpeed()).thenReturn(0l);
        guru.setVmQuotaPercentage(vmTO, vmProfile);
        Mockito.verify(vmTO, Mockito.never()).setCpuQuotaPercentage(Mockito.anyDouble());
    }

    @Test
    public void testSetVmQuotaPercentageNotCPULimit() {
        Mockito.when(vmTO.getLimitCpuUse()).thenReturn(false);
        guru.setVmQuotaPercentage(vmTO, vmProfile);
        Mockito.verify(vmProfile, Mockito.never()).getVirtualMachine();
        Mockito.verify(vmTO, Mockito.never()).setCpuQuotaPercentage(Mockito.anyDouble());
    }

    @Test
    public void testSetVmQuotaPercentageOverProvision() {
        Mockito.when(vmTO.getMaxSpeed()).thenReturn(3000);
        guru.setVmQuotaPercentage(vmTO, vmProfile);
        Mockito.verify(vmTO).setCpuQuotaPercentage(1d);
    }

    @Test
    public void validateGetVmMaxMemoryReturnCustomOfferingMaxMemory(){
        int maxCustomOfferingMemory = 64;
        Mockito.when(serviceOfferingVoMock.getDetail(ApiConstants.MAX_MEMORY)).thenReturn(String.valueOf(maxCustomOfferingMemory));

        long result = guru.getVmMaxMemory(serviceOfferingVoMock, "Vm description", 1l);

        Assert.assertEquals(ByteScaleUtils.mebibytesToBytes(maxCustomOfferingMemory), result);
    }

    @Test
    public void validateGetVmMaxMemoryReturnVmServiceOfferingMaxRAMSize(){
        int maxMemoryConfig = 64;
        Mockito.when(serviceOfferingVoMock.getDetail(ApiConstants.MAX_MEMORY)).thenReturn(null);

        ConfigKey<Integer> vmServiceOfferingMaxRAMSize = Mockito.mock(ConfigKey.class);
        ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_RAM_SIZE = vmServiceOfferingMaxRAMSize;

        Mockito.when(vmServiceOfferingMaxRAMSize.value()).thenReturn(maxMemoryConfig);
        long result = guru.getVmMaxMemory(serviceOfferingVoMock, "Vm description", 1l);

        Assert.assertEquals(ByteScaleUtils.mebibytesToBytes(maxMemoryConfig), result);
    }

    @Test
    public void validateGetVmMaxMemoryReturnMaxHostMemory(){
        long maxHostMemory = ByteScaleUtils.mebibytesToBytes(2000);
        Mockito.when(serviceOfferingVoMock.getDetail(ApiConstants.MAX_MEMORY)).thenReturn(null);

        ConfigKey<Integer> vmServiceOfferingMaxRAMSize = Mockito.mock(ConfigKey.class);
        ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_RAM_SIZE = vmServiceOfferingMaxRAMSize;

        Mockito.when(vmServiceOfferingMaxRAMSize.value()).thenReturn(0);

        long result = guru.getVmMaxMemory(serviceOfferingVoMock, "Vm description", maxHostMemory);

        Assert.assertEquals(maxHostMemory, result);
    }

    @Test
    public void validateGetVmMaxCpuCoresReturnCustomOfferingMaxCpuCores(){
        int maxCustomOfferingCpuCores = 16;
        Mockito.when(serviceOfferingVoMock.getDetail(ApiConstants.MAX_CPU_NUMBER)).thenReturn(String.valueOf(maxCustomOfferingCpuCores));

        long result = guru.getVmMaxCpuCores(serviceOfferingVoMock, "Vm description", 1);

        Assert.assertEquals(maxCustomOfferingCpuCores, result);
    }

    @Test
    public void validateGetVmMaxCpuCoresVmServiceOfferingMaxCPUCores(){
        int maxCpuCoresConfig = 16;
        Mockito.when(serviceOfferingVoMock.getDetail(ApiConstants.MAX_CPU_NUMBER)).thenReturn(null);

        ConfigKey<Integer> vmServiceOfferingMaxCPUCores = Mockito.mock(ConfigKey.class);
        ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_CPU_CORES = vmServiceOfferingMaxCPUCores;

        Mockito.when(vmServiceOfferingMaxCPUCores.value()).thenReturn(maxCpuCoresConfig);
        long result = guru.getVmMaxCpuCores(serviceOfferingVoMock, "Vm description", 1);

        Assert.assertEquals(maxCpuCoresConfig, result);
    }

    @Test
    public void validateGetVmMaxCpuCoresReturnMaxHostMemory(){
        int maxHostCpuCores = 64;
        Mockito.when(serviceOfferingVoMock.getDetail(ApiConstants.MAX_CPU_NUMBER)).thenReturn(null);

        ConfigKey<Integer> vmServiceOfferingMaxCPUCores = Mockito.mock(ConfigKey.class);
        ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_CPU_CORES = vmServiceOfferingMaxCPUCores;

        Mockito.when(vmServiceOfferingMaxCPUCores.value()).thenReturn(0);

        long result = guru.getVmMaxCpuCores(serviceOfferingVoMock, "Vm description", maxHostCpuCores);

        Assert.assertEquals(maxHostCpuCores, result);
    }

    @Test
    public void validateGetHostMaxMemoryAndCpuCoresHostNotNull(){
        Long maxMemory = 2048l;
        Integer maxCpuCores = 16;

        Mockito.when(host.getTotalMemory()).thenReturn(maxMemory);
        Mockito.when(host.getCpus()).thenReturn(maxCpuCores);

        Pair<Long, Integer> result = guru.getHostMaxMemoryAndCpuCores(host, virtualMachineMock, "Vm description");

        Assert.assertEquals(new Pair<>(maxMemory, maxCpuCores), result);
    }

    @Test
    public void validateGetHostMaxMemoryAndCpuCoresHostNullAndLastHostIdNull(){
        Long maxMemory = Long.MAX_VALUE;
        Integer maxCpuCores = Integer.MAX_VALUE;

        Pair<Long, Integer> result = guru.getHostMaxMemoryAndCpuCores(null, virtualMachineMock, "Vm description");

        Assert.assertEquals(new Pair<>(maxMemory, maxCpuCores), result);
    }

    @Test
    public void validateGetHostMaxMemoryAndCpuCoresHostNullAndLastHostIdNotNullAndLastHostNull(){
        Long maxMemory = Long.MAX_VALUE;
        Integer maxCpuCores = Integer.MAX_VALUE;
        guru.hostDao = hostDao;

        Mockito.when(virtualMachineMock.getLastHostId()).thenReturn(1l);
        Mockito.doReturn(null).when(hostDao).findById(Mockito.any());

        Pair<Long, Integer> result = guru.getHostMaxMemoryAndCpuCores(null, virtualMachineMock, "Vm description");

        Assert.assertEquals(new Pair<>(maxMemory, maxCpuCores), result);
    }

    @Test
    public void validateGetHostMaxMemoryAndCpuCoresHostNullAndLastHostIdNotNullAndLastHostNotNull(){
        Long maxMemory = 2048l;
        Integer maxCpuCores = 16;
        guru.hostDao = hostDao;

        Mockito.when(virtualMachineMock.getLastHostId()).thenReturn(1l);
        Mockito.doReturn(host).when(hostDao).findById(Mockito.any());
        Mockito.when(host.getTotalMemory()).thenReturn(maxMemory);
        Mockito.when(host.getCpus()).thenReturn(maxCpuCores);

        Pair<Long, Integer> result = guru.getHostMaxMemoryAndCpuCores(null, virtualMachineMock, "Vm description");

        Assert.assertEquals(new Pair<>(maxMemory, maxCpuCores), result);
    }

    @Test
    public void validateConfigureVmMemoryAndCpuCoresServiceOfferingIsDynamicAndVmIsDynamicCallGetMethods(){
        guru.serviceOfferingDao = serviceOfferingDaoMock;

        Mockito.doReturn(serviceOfferingVoMock).when(serviceOfferingDaoMock).findById(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(true).when(guru).isVmDynamicScalable(Mockito.any(), Mockito.any(), Mockito.any());

        guru.configureVmMemoryAndCpuCores(vmTO, host, virtualMachineMock, vmProfile);

        Mockito.verify(guru).getVmMaxMemory(Mockito.any(ServiceOfferingVO.class), Mockito.anyString(), Mockito.anyLong());
        Mockito.verify(guru).getVmMaxCpuCores(Mockito.any(ServiceOfferingVO.class), Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    public void validateConfigureVmMemoryAndCpuCoresServiceOfferingIsNotDynamicAndVmIsDynamicDoNotCallGetMethods(){
        guru.serviceOfferingDao = serviceOfferingDaoMock;

        Mockito.doReturn(serviceOfferingVoMock).when(serviceOfferingDaoMock).findById(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(false).when(guru).isVmDynamicScalable(Mockito.any(), Mockito.any(), Mockito.any());

        guru.configureVmMemoryAndCpuCores(vmTO, host, virtualMachineMock, vmProfile);

        Mockito.verify(guru, Mockito.never()).getVmMaxMemory(Mockito.any(ServiceOfferingVO.class), Mockito.anyString(), Mockito.anyLong());
        Mockito.verify(guru, Mockito.never()).getVmMaxCpuCores(Mockito.any(ServiceOfferingVO.class), Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    public void validateConfigureVmMemoryAndCpuCoresServiceOfferingIsDynamicAndVmIsNotDynamicDoNotCallGetMethods(){
        guru.serviceOfferingDao = serviceOfferingDaoMock;

        Mockito.doReturn(serviceOfferingVoMock).when(serviceOfferingDaoMock).findById(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(true).when(serviceOfferingVoMock).isDynamic();
        Mockito.doReturn(false).when(vmTO).isEnableDynamicallyScaleVm();

        guru.configureVmMemoryAndCpuCores(vmTO, host, virtualMachineMock, vmProfile);

        Mockito.verify(guru, Mockito.never()).getVmMaxMemory(Mockito.any(ServiceOfferingVO.class), Mockito.anyString(), Mockito.anyLong());
        Mockito.verify(guru, Mockito.never()).getVmMaxCpuCores(Mockito.any(ServiceOfferingVO.class), Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    public void validateEnableDpdkIfNeededCallDpdkHelperSetDpdkVhostUserMode() {
        Mockito.when(dpdkHelperMock.isDpdkvHostUserModeSettingOnServiceOffering(vmProfile)).thenReturn(Boolean.TRUE);
        guru.enableDpdkIfNeeded(vmProfile, vmTO);
        Mockito.verify(dpdkHelperMock).setDpdkVhostUserMode(vmTO, vmProfile);
    }

    @Test
    public void validateEnableDpdkIfNeededDoNotCallDpdkHelperSetDpdkVhostUserMode() {
        Mockito.when(dpdkHelperMock.isDpdkvHostUserModeSettingOnServiceOffering(vmProfile)).thenReturn(Boolean.FALSE);
        guru.enableDpdkIfNeeded(vmProfile, vmTO);
        Mockito.verify(dpdkHelperMock, Mockito.times(0)).setDpdkVhostUserMode(vmTO, vmProfile);
    }

    @Test
    public void validateEnableDpdkIfNeededNicSetDpdkEnabledTrue() {
        Map<String, String> map = new HashMap<>();
        map.put(DpdkHelper.DPDK_NUMA, "test1");
        map.put(DpdkHelper.DPDK_HUGE_PAGES, "test2");

        NicTO nicTo1 = Mockito.mock(NicTO.class);
        NicTO nicTo2 = Mockito.mock(NicTO.class);
        NicTO nicTo3 = Mockito.mock(NicTO.class);

        NicTO[] nics = {nicTo1, nicTo2, nicTo3};

        Mockito.when(vmTO.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vmTO.getExtraConfig()).thenReturn(map);
        Mockito.when(vmTO.getNics()).thenReturn(nics);

        guru.enableDpdkIfNeeded(vmProfile, vmTO);

        for (NicTO nic : nics) {
            Mockito.verify(nic).setDpdkEnabled(true);
        }
    }

    @Test
    public void validateConfigureVmOsDescriptionHostNotNullAndGuestOsMappingNotNullAndGuestOsDisplayNameNotNull(){
        guru._guestOsDao = guestOsDaoMock;
        guru._guestOsHypervisorDao = guestOSHypervisorDaoMock;

        VirtualMachineTO virtualMachineTo = new VirtualMachineTO() {};
        String platformEmulator = "Ubuntu";

        Mockito.doReturn(guestOsVoMock).when(guestOsDaoMock).findByIdIncludingRemoved(Mockito.any());
        Mockito.doReturn(guestOsMappingMock).when(guestOSHypervisorDaoMock).findByOsIdAndHypervisor(Mockito.anyLong(), Mockito.anyString(), Mockito.any());
        Mockito.doReturn(platformEmulator).when(guestOsMappingMock).getGuestOsName();

        guru.configureVmOsDescription(virtualMachineMock, virtualMachineTo, host);

        Assert.assertEquals(platformEmulator, virtualMachineTo.getPlatformEmulator());
    }

    @Test
    public void validateConfigureVmOsDescriptionHostNotNullAndGuestOsMappingNullAndGuestOsDisplayNameNull(){
        guru._guestOsDao = guestOsDaoMock;
        guru._guestOsHypervisorDao = guestOSHypervisorDaoMock;

        VirtualMachineTO virtualMachineTo = new VirtualMachineTO() {};

        Mockito.doReturn(guestOsVoMock).when(guestOsDaoMock).findByIdIncludingRemoved(Mockito.any());
        Mockito.doReturn(null).when(guestOSHypervisorDaoMock).findByOsIdAndHypervisor(Mockito.anyLong(), Mockito.anyString(), Mockito.any());

        guru.configureVmOsDescription(virtualMachineMock, virtualMachineTo, host);

        Assert.assertEquals("Other", virtualMachineTo.getPlatformEmulator());
    }

    @Test
    public void validateConfigureVmOsDescriptionHostNotNullAndGuestOsMappingNullAndGuestOsDisplayNameNotNull(){
        guru._guestOsDao = guestOsDaoMock;
        guru._guestOsHypervisorDao = guestOSHypervisorDaoMock;

        VirtualMachineTO virtualMachineTo = new VirtualMachineTO() {};
        String platformEmulator = "Ubuntu";

        Mockito.doReturn(guestOsVoMock).when(guestOsDaoMock).findByIdIncludingRemoved(Mockito.any());
        Mockito.doReturn(null).when(guestOSHypervisorDaoMock).findByOsIdAndHypervisor(Mockito.anyLong(), Mockito.anyString(), Mockito.any());
        Mockito.doReturn(platformEmulator).when(guestOsVoMock).getDisplayName();

        guru.configureVmOsDescription(virtualMachineMock, virtualMachineTo, host);

        Assert.assertEquals(platformEmulator, virtualMachineTo.getPlatformEmulator());
    }

    @Test
    public void validateConfigureVmOsDescriptionHostNullAndGuestOsMappingNullAndGuestOsDisplayNameNull(){
        guru._guestOsDao = guestOsDaoMock;
        VirtualMachineTO virtualMachineTo = new VirtualMachineTO() {};

        Mockito.doReturn(guestOsVoMock).when(guestOsDaoMock).findByIdIncludingRemoved(Mockito.any());
        guru.configureVmOsDescription(virtualMachineMock, virtualMachineTo, host);

        Assert.assertEquals("Other", virtualMachineTo.getPlatformEmulator());
    }

    @Test
    public void validateConfigureVmOsDescriptionHostNullAndGuestOsMappingNullAndGuestOsDisplayNameNotNull(){
        guru._guestOsDao = guestOsDaoMock;

        VirtualMachineTO virtualMachineTo = new VirtualMachineTO() {};
        String platformEmulator = "Ubuntu";

        Mockito.doReturn(guestOsVoMock).when(guestOsDaoMock).findByIdIncludingRemoved(Mockito.any());
        Mockito.doReturn(platformEmulator).when(guestOsVoMock).getDisplayName();

        guru.configureVmOsDescription(virtualMachineMock, virtualMachineTo, host);

        Assert.assertEquals(platformEmulator, virtualMachineTo.getPlatformEmulator());
    }

    @Test
    public void testGetClusterIdFromVMHost() {
        Mockito.when(vm.getHostId()).thenReturn(123l);
        HostVO vo = new HostVO("");
        Long expected = 5l;
        vo.setClusterId(expected);

        Mockito.when(hostDao.findById(123l)).thenReturn(vo);

        Long clusterId = guru.findClusterOfVm(vm);

        Assert.assertEquals(expected, clusterId);
    }

    @Test
    public void testGetClusterIdFromLastVMHost() {
        Mockito.when(vm.getHostId()).thenReturn(null);
        Mockito.when(vm.getLastHostId()).thenReturn(321l);
        HostVO vo = new HostVO("");
        Long expected = 7l;
        vo.setClusterId(expected);

        Mockito.when(hostDao.findById(321l)).thenReturn(vo);

        Long clusterId = guru.findClusterOfVm(vm);

        Assert.assertEquals(expected, clusterId);
    }

    @Test
    public void testGetNullWhenVMThereIsNoInformationOfUsedHosts() {
        Mockito.when(vm.getHostId()).thenReturn(null);
        Mockito.when(vm.getLastHostId()).thenReturn(null);

        Long clusterId = guru.findClusterOfVm(vm);

        Assert.assertNull(clusterId);
    }
}
