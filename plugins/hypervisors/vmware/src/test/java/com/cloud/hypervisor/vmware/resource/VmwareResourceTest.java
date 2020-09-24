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
package com.cloud.hypervisor.vmware.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.ScaleVmAnswer;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.hypervisor.vmware.util.VmwareClient;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.storage.resource.VmwareStorageProcessor;
import com.cloud.storage.resource.VmwareStorageProcessor.VmwareStorageProcessorConfigurableFields;
import com.cloud.storage.resource.VmwareStorageSubsystemCommandHandler;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VmDetailConstants;
import com.vmware.vim25.HostCapability;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineVideoCard;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CopyCommand.class, DatacenterMO.class, VmwareResource.class})
public class VmwareResourceTest {

    private static final String VOLUME_PATH = "XXXXXXXXXXXX";

    @Mock
    VmwareStorageProcessor storageProcessor;
    @Mock
    VmwareStorageSubsystemCommandHandler storageHandler;

    @Spy
    @InjectMocks
    VmwareResource _resource = new VmwareResource() {

        @Override
        public ScaleVmAnswer execute(ScaleVmCommand cmd) {
            return super.execute(cmd);
        }

        @Override
        public VmwareHypervisorHost getHyperHost(VmwareContext context, Command cmd) {
            return hyperHost;
        }

    };

    @Mock
    VmwareContext context;
    @Mock
    ScaleVmCommand cmd;
    @Mock
    VirtualMachineTO vmSpec;
    @Mock
    VmwareHypervisorHost hyperHost;
    @Mock
    VirtualMachineMO vmMo;
    @Mock
    VirtualMachineConfigSpec vmConfigSpec;
    @Mock
    VirtualMachineMO vmMo3dgpu;
    @Mock
    VirtualMachineTO vmSpec3dgpu;
    @Mock
    VirtualMachineVideoCard videoCard;
    @Mock
    VirtualDevice virtualDevice;
    @Mock
    DataTO srcDataTO;
    @Mock
    NfsTO srcDataNfsTO;
    @Mock
    VolumeTO volume;
    @Mock
    ManagedObjectReference mor;
    @Mock
    DatacenterMO datacenter;
    @Mock
    DataTO destDataTO;
    @Mock
    PrimaryDataStoreTO destDataStoreTO;
    @Mock
    HostMO host;
    @Mock
    ManagedObjectReference hostRef;
    @Mock
    ManagedObjectReference computeRef;
    @Mock
    ManagedObjectReference envRef;
    @Mock
    VmwareClient client;
    @Mock
    VimPortType vimService;
    @Mock
    HostCapability hostCapability;

    CopyCommand storageCmd;
    EnumMap<VmwareStorageProcessorConfigurableFields, Object> params = new EnumMap<VmwareStorageProcessorConfigurableFields,Object>(VmwareStorageProcessorConfigurableFields.class);

    private static final String NFS_VERSION = "3";
    private static final String NFS_VERSION_NOT_PRESENT = null;
    private static final long VRAM_MEMORY_SIZE = 131072l;
    private static final long VIDEO_CARD_MEMORY_SIZE = 65536l;
    private static final Boolean FULL_CLONE_FLAG = true;

    private Map<String,String> specsArray = new HashMap<String,String>();

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        storageCmd = PowerMockito.mock(CopyCommand.class);
        doReturn(context).when(_resource).getServiceContext(null);
        when(cmd.getVirtualMachine()).thenReturn(vmSpec);

        when(storageCmd.getSrcTO()).thenReturn(srcDataTO);
        when(srcDataTO.getDataStore()).thenReturn(srcDataNfsTO);
        when(srcDataNfsTO.getNfsVersion()).thenReturn(NFS_VERSION);
        when(videoCard.getVideoRamSizeInKB()).thenReturn(VIDEO_CARD_MEMORY_SIZE);
        when(volume.getPath()).thenReturn(VOLUME_PATH);

        when(storageCmd.getDestTO()).thenReturn(destDataTO);
        when(destDataTO.getHypervisorType()).thenReturn(HypervisorType.VMware);
        when(destDataTO.getDataStore()).thenReturn(destDataStoreTO);
        when(destDataStoreTO.isFullCloneFlag()).thenReturn(FULL_CLONE_FLAG);
        when(volume.getPath()).thenReturn(VOLUME_PATH);
        when(vmSpec.getDetails()).thenReturn(specsArray);

        when(vmMo.getContext()).thenReturn(context);
        when(vmMo.getRunningHost()).thenReturn(host);
        when(host.getMor()).thenReturn(hostRef);
        when(context.getVimClient()).thenReturn(client);
        when(client.getMoRefProp(hostRef, "parent")).thenReturn(computeRef);
        when(client.getMoRefProp(computeRef, "environmentBrowser")).thenReturn(envRef);
        when(context.getService()).thenReturn(vimService);
        when(vimService.queryTargetCapabilities(envRef, hostRef)).thenReturn(hostCapability);
        when(hostCapability.isNestedHVSupported()).thenReturn(true);
    }

    //Test successful scaling up the vm
    @Test
    public void testScaleVMF1() throws Exception {
        when(_resource.getHyperHost(context, null)).thenReturn(hyperHost);
        doReturn("i-2-3-VM").when(cmd).getVmName();
        when(hyperHost.findVmOnHyperHost("i-2-3-VM")).thenReturn(vmMo);
        doReturn(536870912L).when(vmSpec).getMinRam();
        doReturn(1).when(vmSpec).getCpus();
        doReturn(1000).when(vmSpec).getMinSpeed();
        doReturn(1000).when(vmSpec).getMaxSpeed();
        doReturn(536870912L).when(vmSpec).getMaxRam();
        doReturn(false).when(vmSpec).getLimitCpuUse();
        when(vmMo.configureVm(vmConfigSpec)).thenReturn(true);

        _resource.execute(cmd);
        verify(_resource).execute(cmd);
    }

    @Test
    public void testGenerateMacSequence() {
        final NicTO nicTo1 = new NicTO();
        nicTo1.setMac("01:23:45:67:89:AB");
        nicTo1.setDeviceId(1);

        final NicTO nicTo2 = new NicTO();
        nicTo2.setMac("02:00:65:b5:00:03");
        nicTo2.setDeviceId(0);

        //final NicTO [] nicTOs = {nicTO1, nicTO2, nicTO3};
        //final NicTO[] nics = new NicTO[]{nic};
        final NicTO[] nics = new NicTO[] {nicTo1, nicTo2};

        String macSequence = _resource.generateMacSequence(nics);
        assertEquals(macSequence, "02:00:65:b5:00:03|01:23:45:67:89:AB");
    }

    @Test
    public void testReplaceNicsMacSequenceInBootArgs() {
        String bootArgs = "nic_macs=02:00:65:b5:00:03|7C02:00:4f:1b:00:15|7C1e:00:54:00:00:0f|7C02:00:35:fa:00:11|7C02:00:47:40:00:12";
        doReturn(bootArgs).when(vmSpec).getBootArgs();

        String oldMacSequence = "7C02:00:35:fa:00:11|7C02:00:47:40:00:12";
        String newMacSequence = "7C02:00:0c:1d:00:1d|7C02:00:68:0f:00:1e";

        String updatedBootArgs = _resource.replaceNicsMacSequenceInBootArgs(oldMacSequence, newMacSequence, vmSpec);

        String newBootArgs = "nic_macs=02:00:65:b5:00:03|7C02:00:4f:1b:00:15|7C1e:00:54:00:00:0f|7C02:00:0c:1d:00:1d|7C02:00:68:0f:00:1e";
        assertEquals(newBootArgs, updatedBootArgs);
    }

    @Test
    public void testConfigureVideoCardSvgaVramProvided() throws Exception {
        Map<String, String> specDetails = new HashMap<String, String>();
        specDetails.put("svga.vramSize", String.valueOf(VRAM_MEMORY_SIZE));
        when(vmSpec3dgpu.getDetails()).thenReturn(specDetails);

        _resource.configureVideoCard(vmMo3dgpu, vmSpec3dgpu, vmConfigSpec);
        verify(_resource).setNewVRamSizeVmVideoCard(vmMo3dgpu, VRAM_MEMORY_SIZE, vmConfigSpec);
    }

    @Test
    public void testConfigureVideoCardNotSvgaVramProvided() throws Exception {
        _resource.configureVideoCard(vmMo3dgpu, vmSpec3dgpu, vmConfigSpec);
        verify(_resource, never()).setNewVRamSizeVmVideoCard(vmMo3dgpu, VRAM_MEMORY_SIZE, vmConfigSpec);
    }

    @Test
    public void testModifyVmVideoCardVRamSizeDifferentVramSizes() {
        _resource.modifyVmVideoCardVRamSize(videoCard, vmMo3dgpu, VRAM_MEMORY_SIZE, vmConfigSpec);
        verify(_resource).configureSpecVideoCardNewVRamSize(videoCard, VRAM_MEMORY_SIZE, vmConfigSpec);
    }

    @Test
    public void testModifyVmVideoCardVRamSizeEqualSizes() {
        _resource.modifyVmVideoCardVRamSize(videoCard, vmMo3dgpu, VIDEO_CARD_MEMORY_SIZE, vmConfigSpec);
        verify(_resource, never()).configureSpecVideoCardNewVRamSize(videoCard, VIDEO_CARD_MEMORY_SIZE, vmConfigSpec);
    }

    @Test
    public void testSetNewVRamSizeVmVideoCardPresent() throws Exception {
        when(vmMo3dgpu.getAllDeviceList()).thenReturn(Arrays.asList(videoCard, virtualDevice));
        _resource.setNewVRamSizeVmVideoCard(vmMo3dgpu, VRAM_MEMORY_SIZE, vmConfigSpec);
        verify(_resource).modifyVmVideoCardVRamSize(videoCard, vmMo3dgpu, VRAM_MEMORY_SIZE, vmConfigSpec);
    }

    @Test
    public void testSetNewVRamSizeVmVideoCardNotPresent() throws Exception {
        when(vmMo3dgpu.getAllDeviceList()).thenReturn(Arrays.asList(virtualDevice));
        _resource.setNewVRamSizeVmVideoCard(vmMo3dgpu, VRAM_MEMORY_SIZE, vmConfigSpec);
        verify(_resource, never()).modifyVmVideoCardVRamSize(any(VirtualMachineVideoCard.class), eq(vmMo3dgpu), eq(VRAM_MEMORY_SIZE), eq(vmConfigSpec));
    }

    @Test
    public void testConfigureSpecVideoCardNewVRamSize() {
        when(vmConfigSpec.getDeviceChange()).thenReturn(new ArrayList<VirtualDeviceConfigSpec>());
        _resource.configureSpecVideoCardNewVRamSize(videoCard, VRAM_MEMORY_SIZE, vmConfigSpec);

        InOrder inOrder = Mockito.inOrder(videoCard, vmConfigSpec);
        inOrder.verify(videoCard).setVideoRamSizeInKB(VRAM_MEMORY_SIZE);
        inOrder.verify(videoCard).setUseAutoDetect(false);
        inOrder.verify(vmConfigSpec).getDeviceChange();
    }

    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testgetNfsVersionFromNfsTONull(){
        assertFalse(_resource.getStorageNfsVersionFromNfsTO(null));
    }

    @Test
    public void testgetNfsVersionFromNfsTONfsVersionNull(){
        when(srcDataNfsTO.getNfsVersion()).thenReturn(NFS_VERSION_NOT_PRESENT);
        assertFalse(_resource.getStorageNfsVersionFromNfsTO(srcDataNfsTO));
    }

    @Test
    public void testgetNfsVersionFromNfsTONfsVersion(){
        assertTrue(_resource.getStorageNfsVersionFromNfsTO(srcDataNfsTO));
    }

    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testSetCurrentNfsVersionInProcessorAndHandler(){
        params.put(VmwareStorageProcessorConfigurableFields.NFS_VERSION, NFS_VERSION);
        _resource.reconfigureProcessorByHandler(params);
        verify(storageHandler).reconfigureStorageProcessor(params);
    }

    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testExamineStorageSubSystemCommandNfsVersionNotPresent(){
        when(srcDataNfsTO.getNfsVersion()).thenReturn(NFS_VERSION_NOT_PRESENT);
        _resource.examineStorageSubSystemCommandNfsVersion(storageCmd,params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testExamineStorageSubSystemCommandNfsVersion(){
        _resource.examineStorageSubSystemCommandNfsVersion(storageCmd, params);
        assertEquals(1, params.size());
        assertEquals(NFS_VERSION, params.get(VmwareStorageProcessorConfigurableFields.NFS_VERSION));
    }

    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testExamineStorageSubSystemCommandFullCloneFlagForVmwareNullHypervisor(){
        when(destDataTO.getHypervisorType()).thenReturn(null);
        _resource.examineStorageSubSystemCommandFullCloneFlagForVmware(storageCmd, params);
        verify(destDataTO, never()).getDataStore();
    }

    @Test
    public void testExamineStorageSubSystemCommandFullCloneFlagForHypervisorNotVmware(){
        when(destDataTO.getHypervisorType()).thenReturn(HypervisorType.XenServer);
        _resource.examineStorageSubSystemCommandFullCloneFlagForVmware(storageCmd, params);
        verify(destDataTO, never()).getDataStore();
    }

    @Test
    public void testExamineStorageSubSystemCommandFullCloneFlagForVmware(){
        EnumMap<VmwareStorageProcessorConfigurableFields, Object> params2 = _resource.examineStorageSubSystemCommandFullCloneFlagForVmware(storageCmd, params);
        verify(destDataTO).getDataStore();
        verify(destDataStoreTO, times(2)).isFullCloneFlag();
        assertEquals(1, params2.size());
        assertEquals(FULL_CLONE_FLAG, params2.get(VmwareStorageProcessorConfigurableFields.FULL_CLONE_FLAG));
    }

    @Test
    public void testExamineStorageSubSystemCommandFullCloneFlagForVmwareNull(){
        when(destDataStoreTO.isFullCloneFlag()).thenReturn(null);
        _resource.examineStorageSubSystemCommandFullCloneFlagForVmware(storageCmd, params);
        verify(destDataTO).getDataStore();
        verify(destDataStoreTO).isFullCloneFlag();
        assertTrue(params.isEmpty());
    }

    // ---------------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void checkStorageProcessorAndHandlerNfsVersionAttributeVersionNotSet(){
        _resource.checkStorageProcessorAndHandlerNfsVersionAttribute(storageCmd);
        verify(_resource).examineStorageSubSystemCommandNfsVersion(Matchers.eq(storageCmd), any(EnumMap.class));
        verify(_resource).examineStorageSubSystemCommandFullCloneFlagForVmware(Matchers.eq(storageCmd), any(EnumMap.class));
        verify(_resource).reconfigureProcessorByHandler(any(EnumMap.class));
        assertEquals(NFS_VERSION, _resource.storageNfsVersion);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void checkStorageProcessorAndHandlerNfsVersionAttributeVersionSet(){
        _resource.storageNfsVersion = NFS_VERSION;
        _resource.checkStorageProcessorAndHandlerNfsVersionAttribute(storageCmd);
        verify(_resource, never()).examineStorageSubSystemCommandNfsVersion(Matchers.eq(storageCmd), any(EnumMap.class));
    }

    @Test(expected= CloudRuntimeException.class)
    public void testFindVmOnDatacenterNullHyperHostReference() throws Exception {
        when(hyperHost.getMor()).thenReturn(null);
        _resource.findVmOnDatacenter(context, hyperHost, volume);
    }

    @Test
    public void testFindVmOnDatacenter() throws Exception {
        when(hyperHost.getHyperHostDatacenter()).thenReturn(mor);
        when(datacenter.getMor()).thenReturn(mor);
        when(datacenter.findVm(VOLUME_PATH)).thenReturn(vmMo);
        whenNew(DatacenterMO.class).withArguments(context, mor).thenReturn(datacenter);
        VirtualMachineMO result = _resource.findVmOnDatacenter(context, hyperHost, volume);
        assertEquals(vmMo, result);
    }

    @Test
    public void testConfigNestedHVSupportFlagTrue() throws Exception{
        specsArray.put(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG, "true");
        _resource.configNestedHVSupport(vmMo, vmSpec, vmConfigSpec);
        verify(vmMo).getRunningHost();
        verify(host).getMor();
        verify(context, times(2)).getVimClient();
        verify(client).getMoRefProp(hostRef, "parent");
        verify(client).getMoRefProp(computeRef, "environmentBrowser");
        verify(context).getService();
        verify(vimService).queryTargetCapabilities(envRef, hostRef);
        verify(hostCapability).isNestedHVSupported();

        verify(vmConfigSpec).setNestedHVEnabled(true);
    }

    @Test
    public void testConfigNestedHVSupportFlagFalse() throws Exception{
        specsArray.put(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG, "false");
        _resource.configNestedHVSupport(vmMo, vmSpec, vmConfigSpec);
        verify(vmMo, never()).getRunningHost();
    }

}
