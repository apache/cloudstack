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
package org.apache.cloudstack.storage.template;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.VNF.VnfNic;
import com.cloud.storage.Storage;
import com.cloud.template.VirtualMachineTemplate;
import org.apache.cloudstack.api.command.user.template.RegisterVnfTemplateCmd;
import org.apache.cloudstack.api.command.user.template.UpdateVnfTemplateCmd;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VnfTemplateManagerTest {

    @Test
    public void testGetVnfNicsListAllGood() {
        final Map<String, Object> vnfNics = new HashMap<>();
        vnfNics.put("0", new HashMap<>(Map.ofEntries(
                Map.entry("deviceid", "1"),
                Map.entry("name", "eth1"),
                Map.entry("required", "true"),
                Map.entry("description", "The second NIC of VNF appliance")
        )));
        vnfNics.put("1", new HashMap<>(Map.ofEntries(
                Map.entry("deviceid", "2"),
                Map.entry("name", "eth2"),
                Map.entry("required", "false"),
                Map.entry("description", "The third NIC of VNF appliance")
        )));
        vnfNics.put("2", new HashMap<>(Map.ofEntries(
                Map.entry("deviceid", "0"),
                Map.entry("name", "eth0"),
                Map.entry("description", "The first NIC of VNF appliance")
        )));
        List<VnfNic> nicsList = VnfTemplateManager.getVnfNicsList(vnfNics);
        Assert.assertEquals(3, nicsList.size());
        Assert.assertEquals(0, nicsList.get(0).getDeviceId());
        Assert.assertEquals("eth0", nicsList.get(0).getName());
        Assert.assertTrue(nicsList.get(0).isRequired());
        Assert.assertEquals(1, nicsList.get(1).getDeviceId());
        Assert.assertEquals("eth1", nicsList.get(1).getName());
        Assert.assertTrue(nicsList.get(1).isRequired());
        Assert.assertEquals(2, nicsList.get(2).getDeviceId());
        Assert.assertEquals("eth2", nicsList.get(2).getName());
        Assert.assertFalse(nicsList.get(2).isRequired());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetVnfNicsListWithEmptyName() {
        final Map<String, Object> vnfNics = new HashMap<>();
        vnfNics.put("0", new HashMap<>(Map.ofEntries(
                Map.entry("deviceid", "1"),
                Map.entry("required", "true"),
                Map.entry("description", "The second NIC of VNF appliance")
        )));

        List<VnfNic> nicsList = VnfTemplateManager.getVnfNicsList(vnfNics);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetVnfNicsListWithEmptyDeviceId() {
        final Map<String, Object> vnfNics = new HashMap<>();
        vnfNics.put("0", new HashMap<>(Map.ofEntries(
                Map.entry("name", "eth1"),
                Map.entry("required", "true"),
                Map.entry("description", "The second NIC of VNF appliance")
        )));

        List<VnfNic> nicsList = VnfTemplateManager.getVnfNicsList(vnfNics);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetVnfNicsListWithInvalidDeviceId() {
        final Map<String, Object> vnfNics = new HashMap<>();
        vnfNics.put("0", new HashMap<>(Map.ofEntries(
                Map.entry("deviceid", "invalid"),
                Map.entry("name", "eth1"),
                Map.entry("required", "true"),
                Map.entry("description", "The second NIC of VNF appliance")
        )));

        List<VnfNic> nicsList = VnfTemplateManager.getVnfNicsList(vnfNics);
    }

    @Test
    public void testValidateVnfNicsAllGood() {
        List<VnfNic> nicsList = new ArrayList<>();
        nicsList.add(new VnfNic(0, "eth0", true, true, "first NIC"));
        nicsList.add(new VnfNic(1, "eth1", true, true, "second NIC"));
        nicsList.add(new VnfNic(2, "eth2", false, true, "third NIC"));
        VnfTemplateManager.validateVnfNics(nicsList);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVnfNicsStartWithNonzero() {
        List<VnfNic> nicsList = new ArrayList<>();
        nicsList.add(new VnfNic(1, "eth0", true, true, "first NIC"));
        nicsList.add(new VnfNic(2, "eth1", true, true, "second NIC"));
        nicsList.add(new VnfNic(3, "eth2", false, true, "third NIC"));
        VnfTemplateManager.validateVnfNics(nicsList);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVnfNicsWithNonConstantDeviceIds() {
        List<VnfNic> nicsList = new ArrayList<>();
        nicsList.add(new VnfNic(0, "eth0", true, true, "first NIC"));
        nicsList.add(new VnfNic(2, "eth1", true, true, "second NIC"));
        nicsList.add(new VnfNic(4, "eth2", false, true, "third NIC"));
        VnfTemplateManager.validateVnfNics(nicsList);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVnfNicsWithInvalidRequired() {
        List<VnfNic> nicsList = new ArrayList<>();
        nicsList.add(new VnfNic(0, "eth0", true, true, "first NIC"));
        nicsList.add(new VnfNic(1, "eth1", false, true, "second NIC"));
        nicsList.add(new VnfNic(2, "eth2", true, true, "third NIC"));
        VnfTemplateManager.validateVnfNics(nicsList);
    }

    @Test
    public void testValidateApiCommandParamsAllGood() {
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(template.getTemplateType()).thenReturn(Storage.TemplateType.VNF);
        RegisterVnfTemplateCmd cmd = new RegisterVnfTemplateCmd();

        VnfTemplateManager.validateApiCommandParams(cmd, template);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateApiCommandParamsInvalidAccessMethods() {
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(template.getTemplateType()).thenReturn(Storage.TemplateType.VNF);
        UpdateVnfTemplateCmd cmd = new UpdateVnfTemplateCmd();
        Map<String, Object> vnfDetails = new HashMap<>();
        vnfDetails.put("0", new HashMap<>(Map.ofEntries(
                Map.entry("accessMethods", "invalid")
        )));
        ReflectionTestUtils.setField(cmd,"vnfDetails", vnfDetails);

        VnfTemplateManager.validateApiCommandParams(cmd, template);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateApiCommandParamsInvalidAccessDetails() {
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(template.getTemplateType()).thenReturn(Storage.TemplateType.VNF);
        UpdateVnfTemplateCmd cmd = new UpdateVnfTemplateCmd();
        Map<String, Object> accessDetails = new HashMap<>();
        accessDetails.put("0", new HashMap<>(Map.ofEntries(
                Map.entry("username", "admin"),
                Map.entry("password", "password"),
                Map.entry("invalid", "value")
        )));
        ReflectionTestUtils.setField(cmd,"vnfDetails", accessDetails);

        VnfTemplateManager.validateApiCommandParams(cmd, template);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateApiCommandParamsInvalidVnfDetails() {
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(template.getTemplateType()).thenReturn(Storage.TemplateType.VNF);
        UpdateVnfTemplateCmd cmd = new UpdateVnfTemplateCmd();
        Map<String, Object> vnfDetails = new HashMap<>();
        vnfDetails.put("0", new HashMap<>(Map.ofEntries(
                Map.entry("accessMethods", "console"),
                Map.entry("username", "admin"),
                Map.entry("password", "password"),
                Map.entry("version", "4.19.0"),
                Map.entry("vendor", "cloudstack"),
                Map.entry("invalid", "value")
        )));
        ReflectionTestUtils.setField(cmd,"vnfDetails", vnfDetails);

        VnfTemplateManager.validateApiCommandParams(cmd, template);
    }
}
