package org.apache.cloudstack.veeam.api.dto;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OvfXmlUtilTest {

    String configuration = "<ovf:Envelope xmlns:ovf=\"http://schemas.dmtf.org/ovf/envelope/1/\" xmlns:rasd=\"http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_ResourceAllocationSettingData\" xmlns:vssd=\"http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_VirtualSystemSettingData\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ovf:version=\"4.4.0.0\">" +
            "<Content ovf:id=\"out\" xsi:type=\"ovf:VirtualSystem_Type\"><Name>adm-v9</Name><Description>adm-v9</Description>"+
            "<Section xsi:type=\"ovf:VirtualHardwareSection_Type\"><Info>1 CPU, 512 Memory</Info><System><vssd:VirtualSystemType>ENGINE 4.4.0.0</vssd:VirtualSystemType></System><Item><rasd:Caption>1 virtual cpu</rasd:Caption><rasd:Description>Number of virtual CPU</rasd:Description><rasd:InstanceId>1</rasd:InstanceId><rasd:ResourceType>3</rasd:ResourceType><rasd:num_of_sockets>1</rasd:num_of_sockets><rasd:cpu_per_socket>1</rasd:cpu_per_socket><rasd:threads_per_cpu>1</rasd:threads_per_cpu><rasd:max_num_of_vcpus>1</rasd:max_num_of_vcpus><rasd:VirtualQuantity>1</rasd:VirtualQuantity></Item>" +
            "<Item><rasd:Caption>512 MB of memory</rasd:Caption><rasd:Description>Memory Size</rasd:Description><rasd:InstanceId>2</rasd:InstanceId><rasd:ResourceType>4</rasd:ResourceType><rasd:AllocationUnits>MegaBytes</rasd:AllocationUnits><rasd:VirtualQuantity>512</rasd:VirtualQuantity></Item>" +
            "</Section></Content></ovf:Envelope>";

    @Test
    public void updateFromXml_parsesDetails() {
        Vm vm = new Vm();
        OvfXmlUtil.updateFromXml(vm, configuration);

        assertEquals(String.valueOf(512L), vm.getMemory());
        assertEquals("1", vm.getCpu().getTopology().getSockets());
        assertEquals("1", vm.getCpu().getTopology().getCores());
        assertEquals("1", vm.getCpu().getTopology().getThreads());
    }
}
