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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

public class DtoSerializationTest {

    @Test
    public void diskAttachmentJson_UsesInterfaceProperty() throws Exception {
        Mapper mapper = new Mapper();
        DiskAttachment diskAttachment = new DiskAttachment();
        diskAttachment.setIface("virtio_scsi");

        String json = mapper.toJson(diskAttachment);

        assertTrue(json.contains("\"interface\":\"virtio_scsi\""));
        assertFalse(json.contains("\"iface\""));
    }

    @Test
    public void nicJson_UsesInterfacePropertyAndRoundTrips() throws Exception {
        Mapper mapper = new Mapper();
        Nic nic = new Nic();
        nic.setInterfaceType("virtio");

        String json = mapper.toJson(nic);
        Nic read = mapper.jsonMapper().readValue(json, Nic.class);

        assertTrue(json.contains("\"interface\":\"virtio\""));
        assertFalse(json.contains("interface_type"));
        assertEquals("virtio", read.getInterfaceType());
    }

    @Test
    public void vmJson_IgnoresCloudStackSpecificFields() throws Exception {
        Mapper mapper = new Mapper();
        Vm vm = new Vm();
        vm.setName("vm-1");
        vm.setAccountId("account-uuid");
        vm.setAffinityGroupId("affinity-uuid");
        vm.setUserDataId("userdata-uuid");

        String json = mapper.toJson(vm);

        assertTrue(json.contains("\"name\":\"vm-1\""));
        assertFalse(json.contains("account_id"));
        assertFalse(json.contains("affinity_group_id"));
        assertFalse(json.contains("user_data_id"));
    }

    @Test
    public void vmXml_WritesEmptyElements() throws Exception {
        Mapper mapper = new Mapper();
        Vm vm = new Vm();

        String xml = mapper.toXml(vm);

        assertTrue(xml.contains("<io/>"));
        assertTrue(xml.contains("<migration/>"));
    }
}

