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

package org.apache.cloudstack.veeam.api.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class OvfXmlUtilTest {

    String configuration = "<ovf:Envelope xmlns:ovf=\"http://schemas.dmtf.org/ovf/envelope/1/\" xmlns:rasd=\"http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_ResourceAllocationSettingData\" xmlns:vssd=\"http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_VirtualSystemSettingData\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ovf:version=\"4.4.0.0\">" +
            "<Content ovf:id=\"out\" xsi:type=\"ovf:VirtualSystem_Type\"><Name>adm-v9</Name><Description>adm-v9</Description>" +
            "<Section xsi:type=\"ovf:VirtualHardwareSection_Type\"><Info>1 CPU, 512 Memory</Info><System><vssd:VirtualSystemType>ENGINE 4.4.0.0</vssd:VirtualSystemType></System><Item><rasd:Caption>1 virtual cpu</rasd:Caption><rasd:Description>Number of virtual CPU</rasd:Description><rasd:InstanceId>1</rasd:InstanceId><rasd:ResourceType>3</rasd:ResourceType><rasd:num_of_sockets>1</rasd:num_of_sockets><rasd:cpu_per_socket>1</rasd:cpu_per_socket><rasd:threads_per_cpu>1</rasd:threads_per_cpu><rasd:max_num_of_vcpus>1</rasd:max_num_of_vcpus><rasd:VirtualQuantity>1</rasd:VirtualQuantity></Item>" +
            "<Item><rasd:Caption>512 MB of memory</rasd:Caption><rasd:Description>Memory Size</rasd:Description><rasd:InstanceId>2</rasd:InstanceId><rasd:ResourceType>4</rasd:ResourceType><rasd:AllocationUnits>MegaBytes</rasd:AllocationUnits><rasd:VirtualQuantity>512</rasd:VirtualQuantity></Item>" +
            "</Section></Content></ovf:Envelope>";

    @Test
    public void updateFromXml_parsesDetails() {
        Vm vm = new Vm();
        OvfXmlUtil.updateFromXml(vm, configuration);

        assertEquals(String.valueOf(512 * OvfXmlUtil.MemoryAllocationUnit.Megabytes.getBytesMultiplier()), vm.getMemory());
        assertEquals("1", vm.getCpu().getTopology().getSockets());
        assertEquals("1", vm.getCpu().getTopology().getCores());
        assertEquals("1", vm.getCpu().getTopology().getThreads());
    }

    @Test
    public void test_restoreConfig_parse() throws Exception {
        Vm vm = mock(Vm.class);
        Vm.Initialization initialization = mock(Vm.Initialization.class);
        Vm.Initialization.Configuration configMock = mock(Vm.Initialization.Configuration.class);
        when(initialization.getConfiguration()).thenReturn(configMock);
        when(vm.getInitialization()).thenReturn(initialization);
        String ovfXml;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-ovf.xml")) {
            assertNotNull(is);
            ovfXml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        when(configMock.getData()).thenReturn(ovfXml);

        String instanceConfig = OvfXmlUtil.getConfigMetadataXml(vm, mock(Logger.class));
        assertNotNull(instanceConfig);
        assertTrue(instanceConfig.contains("ovf:CloudStackMetadata_Type"));
        assertTrue(instanceConfig.contains("<NetworkId>6aff2178-a323-4148-a592-edbd47b93229</NetworkId>"));

        Pair<String, String> result = OvfXmlUtil.getVmNicDetailFromStoredConfig(instanceConfig, "6aff2178-a323-4148-a592-edbd47b93229", mock(Logger.class));
        assertNotNull(result);
        assertEquals("02:01:00:cf:00:05", result.first());
        assertEquals("10.1.1.40", result.second());
    }
}
