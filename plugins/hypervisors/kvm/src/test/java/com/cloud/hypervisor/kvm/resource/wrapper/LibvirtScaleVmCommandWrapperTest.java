/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import junit.framework.TestCase;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LibvirtComputingResource.class)
public class LibvirtScaleVmCommandWrapperTest extends TestCase {

    @Spy
    LibvirtScaleVmCommandWrapper libvirtScaleVmCommandWrapperSpy = Mockito.spy(LibvirtScaleVmCommandWrapper.class);

    @Mock
    LibvirtComputingResource libvirtComputingResourceMock;

    @Mock
    ScaleVmCommand scaleVmCommandMock;

    @Mock
    LibvirtUtilitiesHelper libvirtUtilitiesHelperMock;

    @Mock
    Domain domainMock;

    @Mock
    Connect connectMock;

    @Mock
    LibvirtException libvirtException;

    @Mock
    Exception exception;

    LibvirtRequestWrapper wrapper;
    VirtualMachineTO vmTo;

    String scalingDetails;

    @Before
    public void init() {
        wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        vmTo = new VirtualMachineTO(1, "Test 1", VirtualMachine.Type.User, 2, 1000, 67108864, 67108864, VirtualMachineTemplate.BootloaderType.External, "Other Linux (64x)", true, true, "test123");

        long memory = ByteScaleUtils.bytesToKibibytes(vmTo.getMaxRam());
        int vcpus = vmTo.getCpus();
        int cpuShares = vcpus * vmTo.getSpeed();
        scalingDetails = String.format("%s memory to [%s KiB], CPU cores to [%s] and cpu_shares to [%s]", vmTo.toString(), memory, vcpus, cpuShares);

        PowerMockito.mockStatic(LibvirtComputingResource.class);
    }

    @Test
    public void validateScaleVcpusRunningVcpusLessThanNewVcpusSetNewVcpu() throws LibvirtException{
        long runningVcpus = 1;
        int newVcpus = 2;

        PowerMockito.when(LibvirtComputingResource.countDomainRunningVcpus(Mockito.any())).thenReturn(runningVcpus);
        Mockito.doNothing().when(domainMock).setVcpus(Mockito.anyInt());

        libvirtScaleVmCommandWrapperSpy.scaleVcpus(domainMock, newVcpus, scalingDetails);

        Mockito.verify(domainMock).setVcpus(Mockito.anyInt());
    }

    @Test
    public void validateScaleVcpusRunningVcpusEqualThanNewVcpusDoNothing() throws LibvirtException{
        long runningVcpus = 2;
        int newVcpus = 2;

        PowerMockito.when(LibvirtComputingResource.countDomainRunningVcpus(Mockito.any())).thenReturn(runningVcpus);

        libvirtScaleVmCommandWrapperSpy.scaleVcpus(domainMock, newVcpus, scalingDetails);

        Mockito.verify(domainMock, Mockito.never()).setVcpus(Mockito.anyInt());
    }

    @Test
    public void validateScaleVcpusRunningVcpusHigherThanNewVcpusDoNothing() throws LibvirtException{
        long runningVcpus = 2;
        int newVcpus = 1;

        PowerMockito.when(LibvirtComputingResource.countDomainRunningVcpus(Mockito.any())).thenReturn(runningVcpus);

        libvirtScaleVmCommandWrapperSpy.scaleVcpus(domainMock, newVcpus, scalingDetails);

        Mockito.verify(domainMock, Mockito.never()).setVcpus(Mockito.anyInt());
    }

    @Test (expected = LibvirtException.class)
    public void validateScaleVcpusSetVcpusThrowLibvirtException() throws LibvirtException{
        long runningVcpus = 1;
        int newVcpus = 2;

        PowerMockito.when(LibvirtComputingResource.countDomainRunningVcpus(Mockito.any())).thenReturn(runningVcpus);
        Mockito.doThrow(LibvirtException.class).when(domainMock).setVcpus(Mockito.anyInt());

        libvirtScaleVmCommandWrapperSpy.scaleVcpus(domainMock, newVcpus, scalingDetails);

        Mockito.verify(domainMock, Mockito.never()).setVcpus(Mockito.anyInt());
    }

    @Test
    public void validateScaleMemoryMemoryLessThanZeroDoNothing() throws LibvirtException {
        long currentMemory = 1l;
        long newMemory = 0l;

        PowerMockito.when(LibvirtComputingResource.getDomainMemory(Mockito.any())).thenReturn(currentMemory);

        libvirtScaleVmCommandWrapperSpy.scaleMemory(domainMock, newMemory, scalingDetails);

        Mockito.verify(domainMock, Mockito.never()).getXMLDesc(Mockito.anyInt());
        Mockito.verify(domainMock, Mockito.never()).attachDevice(Mockito.anyString());
    }

    @Test
    public void validateScaleMemoryMemoryEqualToZeroDoNothing() throws LibvirtException {
        long currentMemory = 1l;
        long newMemory = 1l;

        PowerMockito.when(LibvirtComputingResource.getDomainMemory(Mockito.any())).thenReturn(currentMemory);

        libvirtScaleVmCommandWrapperSpy.scaleMemory(domainMock, newMemory, scalingDetails);

        Mockito.verify(domainMock, Mockito.never()).getXMLDesc(Mockito.anyInt());
        Mockito.verify(domainMock, Mockito.never()).attachDevice(Mockito.anyString());
    }

    @Test (expected = CloudRuntimeException.class)
    public void validateScaleMemoryDomainXmlDoesNotContainsMaxMemory() throws LibvirtException {
        long currentMemory = 1l;
        long newMemory = 2l;

        PowerMockito.when(LibvirtComputingResource.getDomainMemory(Mockito.any())).thenReturn(currentMemory);
        Mockito.doReturn("").when(domainMock).getXMLDesc(Mockito.anyInt());

        libvirtScaleVmCommandWrapperSpy.scaleMemory(domainMock, newMemory, scalingDetails);

        Mockito.verify(domainMock).getXMLDesc(Mockito.anyInt());
        Mockito.verify(domainMock, Mockito.never()).attachDevice(Mockito.anyString());
    }

    @Test (expected = LibvirtException.class)
    public void validateScaleMemoryAttachDeviceThrowsLibvirtException() throws LibvirtException {
        long currentMemory = 1l;
        long newMemory = 2l;

        PowerMockito.when(LibvirtComputingResource.getDomainMemory(Mockito.any())).thenReturn(currentMemory);
        Mockito.doReturn("<maxMemory slots='16' unit='KiB'>").when(domainMock).getXMLDesc(Mockito.anyInt());
        Mockito.doThrow(LibvirtException.class).when(domainMock).attachDevice(Mockito.anyString());

        libvirtScaleVmCommandWrapperSpy.scaleMemory(domainMock, newMemory, scalingDetails);

        Mockito.verify(domainMock).getXMLDesc(Mockito.anyInt());
        Mockito.verify(domainMock).attachDevice(Mockito.anyString());
    }

    @Test
    public void validateScaleMemory() throws LibvirtException {
        long currentMemory = 1l;
        long newMemory = 2l;

        PowerMockito.when(LibvirtComputingResource.getDomainMemory(Mockito.any())).thenReturn(currentMemory);
        Mockito.doReturn("<maxMemory slots='16' unit='KiB'>").when(domainMock).getXMLDesc(Mockito.anyInt());
        Mockito.doNothing().when(domainMock).attachDevice(Mockito.anyString());

        libvirtScaleVmCommandWrapperSpy.scaleMemory(domainMock, newMemory, scalingDetails);

        Mockito.verify(domainMock).getXMLDesc(Mockito.anyInt());
        Mockito.verify(domainMock).attachDevice(Mockito.anyString());
    }

    @Test
    public void validateExecuteHandleLibvirtException() throws LibvirtException {
        String errorMessage = "";

        Mockito.doReturn(vmTo).when(scaleVmCommandMock).getVirtualMachine();
        Mockito.doReturn(libvirtUtilitiesHelperMock).when(libvirtComputingResourceMock).getLibvirtUtilitiesHelper();
        Mockito.doThrow(libvirtException).when(libvirtUtilitiesHelperMock).getConnectionByVmName(Mockito.anyString());
        Mockito.doReturn(errorMessage).when(libvirtException).getMessage();

        Answer answer = libvirtScaleVmCommandWrapperSpy.execute(scaleVmCommandMock, libvirtComputingResourceMock);

        String details = String.format("Unable to scale %s due to [%s].", scalingDetails, errorMessage);
        assertFalse(answer.getResult());
        assertEquals(details, answer.getDetails());
    }

    @Test
    public void validateExecuteSuccessfully() throws LibvirtException {
        Mockito.doReturn(vmTo).when(scaleVmCommandMock).getVirtualMachine();
        Mockito.doReturn(libvirtUtilitiesHelperMock).when(libvirtComputingResourceMock).getLibvirtUtilitiesHelper();
        Mockito.doReturn(connectMock).when(libvirtUtilitiesHelperMock).getConnectionByVmName(Mockito.anyString());
        Mockito.doReturn(domainMock).when(connectMock).domainLookupByName(Mockito.anyString());
        Mockito.doNothing().when(libvirtScaleVmCommandWrapperSpy).scaleMemory(Mockito.any(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doNothing().when(libvirtScaleVmCommandWrapperSpy).scaleVcpus(Mockito.any(), Mockito.anyInt(), Mockito.anyString());

        Answer answer = libvirtScaleVmCommandWrapperSpy.execute(scaleVmCommandMock, libvirtComputingResourceMock);

        String details = String.format("Successfully scaled %s.", scalingDetails);
        assertTrue(answer.getResult());
        assertEquals(details, answer.getDetails());
    }

    @Test(expected = Exception.class)
    public void validateExecuteThrowAnyOtherException() {
        Mockito.doThrow(Exception.class).when(libvirtComputingResourceMock).getLibvirtUtilitiesHelper();

        libvirtScaleVmCommandWrapperSpy.execute(scaleVmCommandMock, libvirtComputingResourceMock);
    }

    @Test
    public void updateCpuSharesTestOldSharesLessThanNewSharesUpdateShares() throws LibvirtException {
        int oldShares = 2000;
        int newShares = 3000;

        PowerMockito.when(LibvirtComputingResource.getCpuShares(Mockito.any())).thenReturn(oldShares);
        libvirtScaleVmCommandWrapperSpy.updateCpuShares(domainMock, newShares);

        PowerMockito.verifyStatic(LibvirtComputingResource.class, Mockito.times(1));
        libvirtComputingResourceMock.setCpuShares(domainMock, newShares);
    }

    @Test
    public void updateCpuSharesTestOldSharesHigherThanNewSharesDoNothing() throws LibvirtException {
        int oldShares = 3000;
        int newShares = 2000;

        PowerMockito.when(LibvirtComputingResource.getCpuShares(Mockito.any())).thenReturn(oldShares);
        libvirtScaleVmCommandWrapperSpy.updateCpuShares(domainMock, newShares);

        PowerMockito.verifyStatic(LibvirtComputingResource.class, Mockito.times(0));
        libvirtComputingResourceMock.setCpuShares(domainMock, newShares);
    }

    @Test
    public void updateCpuSharesTestOldSharesEqualsNewSharesDoNothing() throws LibvirtException {
        int oldShares = 2000;
        int newShares = 2000;

        PowerMockito.when(LibvirtComputingResource.getCpuShares(Mockito.any())).thenReturn(oldShares);
        libvirtScaleVmCommandWrapperSpy.updateCpuShares(domainMock, newShares);

        PowerMockito.verifyStatic(LibvirtComputingResource.class, Mockito.times(0));
        libvirtComputingResourceMock.setCpuShares(domainMock, newShares);
    }
}
