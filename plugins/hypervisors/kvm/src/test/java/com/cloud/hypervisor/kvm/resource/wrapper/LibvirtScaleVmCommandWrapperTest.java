/*
 * Copyright 2021 The Apache Software Foundation.
 *
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
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtScaleVmCommandWrapperTest extends TestCase {

    @Mock
    LibvirtComputingResource computingResource;

    @Mock
    ScaleVmCommand command;

    @Mock
    LibvirtUtilitiesHelper libvirtUtilitiesHelper;

    @Mock
    Domain domain;

    @Mock
    Connect connect;

    @Mock
    LibvirtException libvirtException;

    @Mock
    Exception exception;

    LibvirtRequestWrapper wrapper;
    VirtualMachineTO vmTo;

    String scalingDetails;

    int countCpus = 2;

    @Before
    public void init() {
        wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        vmTo = new VirtualMachineTO(1, "Test 1", VirtualMachine.Type.User, countCpus, 1000, 67108864, 67108864, VirtualMachineTemplate.BootloaderType.External, "Other Linux (64x)", true, true, "test123");

        long memory = ByteScaleUtils.bytesToKib(vmTo.getMaxRam());
        int vcpus = vmTo.getCpus();
        scalingDetails = String.format("%s memory to [%s KiB] and cpu cores to [%s]", vmTo.toString(), memory, vcpus);
    }

    @Test
    public void validateExecuteSuccessfully() throws LibvirtException {
        Mockito.when(command.getVirtualMachine()).thenReturn(vmTo);
        Mockito.when(computingResource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        Mockito.when(libvirtUtilitiesHelper.getConnectionByVmName(Mockito.anyString())).thenReturn(connect);
        Mockito.when(connect.domainLookupByName(Mockito.anyString())).thenReturn(domain);
        Mockito.doReturn((long) countCpus).when(computingResource).countDomainRunningVcpus(domain);
        Mockito.doNothing().when(domain).attachDevice(Mockito.anyString());

        Answer answer = wrapper.execute(command, computingResource);

        String details = String.format("Successfully scaled %s.", scalingDetails);
        assertTrue(answer.getResult());
        assertEquals(details, answer.getDetails());
    }

    @Test
    public void validateExecuteHandleLibvirtException() throws LibvirtException {
        Mockito.when(command.getVirtualMachine()).thenReturn(vmTo);
        Mockito.when(computingResource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        Mockito.doThrow(libvirtException).when(libvirtUtilitiesHelper).getConnectionByVmName(Mockito.anyString());
        String errorMessage = "";
        Mockito.when(libvirtException.getMessage()).thenReturn(errorMessage);

        Answer answer = wrapper.execute(command, computingResource);

        String details = String.format("Unable to scale %s due to [%s].", scalingDetails, errorMessage);
        assertFalse(answer.getResult());
        assertEquals(details, answer.getDetails());
    }

    @Test(expected = Exception.class)
    public void validateExecuteThrowAnyOtherException() {
        Mockito.doThrow(exception).when(computingResource).getLibvirtUtilitiesHelper();

        wrapper.execute(command, computingResource);
    }
}