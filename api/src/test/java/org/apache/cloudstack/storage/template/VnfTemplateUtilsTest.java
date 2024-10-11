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
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class VnfTemplateUtilsTest {

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

        Map<String, Object> vnfNicsMock = Mockito.mock(Map.class);
        Mockito.when(vnfNicsMock.values()).thenReturn(vnfNics.values());

        List<VnfNic> nicsList = VnfTemplateUtils.getVnfNicsList(vnfNicsMock);
        Mockito.verify(vnfNicsMock).values();

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

        Map<String, Object> vnfNicsMock = Mockito.mock(Map.class);
        Mockito.when(vnfNicsMock.values()).thenReturn(vnfNics.values());

        List<VnfNic> nicsList = VnfTemplateUtils.getVnfNicsList(vnfNicsMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetVnfNicsListWithEmptyDeviceId() {
        final Map<String, Object> vnfNics = new HashMap<>();
        vnfNics.put("0", new HashMap<>(Map.ofEntries(
                Map.entry("name", "eth1"),
                Map.entry("required", "true"),
                Map.entry("description", "The second NIC of VNF appliance")
        )));

        Map<String, Object> vnfNicsMock = Mockito.mock(Map.class);
        Mockito.when(vnfNicsMock.values()).thenReturn(vnfNics.values());

        List<VnfNic> nicsList = VnfTemplateUtils.getVnfNicsList(vnfNicsMock);
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

        Map<String, Object> vnfNicsMock = Mockito.mock(Map.class);
        Mockito.when(vnfNicsMock.values()).thenReturn(vnfNics.values());

        List<VnfNic> nicsList = VnfTemplateUtils.getVnfNicsList(vnfNicsMock);
    }

    @Test
    public void testValidateVnfNicsAllGood() {
        VnfNic nic1 = Mockito.mock(VnfNic.class);
        Mockito.when(nic1.getDeviceId()).thenReturn(0L);
        Mockito.when(nic1.isRequired()).thenReturn(true);

        VnfNic nic2 = Mockito.mock(VnfNic.class);
        Mockito.when(nic2.getDeviceId()).thenReturn(1L);
        Mockito.when(nic2.isRequired()).thenReturn(true);

        VnfNic nic3 = Mockito.mock(VnfNic.class);
        Mockito.when(nic3.getDeviceId()).thenReturn(2L);
        Mockito.when(nic3.isRequired()).thenReturn(false);

        List<VnfNic> nicsList = Arrays.asList(nic1, nic2, nic3);

        VnfTemplateUtils.validateVnfNics(nicsList);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVnfNicsStartWithNonzero() {
        VnfNic nic1 = Mockito.mock(VnfNic.class);
        Mockito.when(nic1.getDeviceId()).thenReturn(1L);

        VnfNic nic2 = Mockito.mock(VnfNic.class);

        VnfNic nic3 = Mockito.mock(VnfNic.class);

        List<VnfNic> nicsList = Arrays.asList(nic1, nic2, nic3);

        VnfTemplateUtils.validateVnfNics(nicsList);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVnfNicsWithNonConstantDeviceIds() {
        VnfNic nic1 = Mockito.mock(VnfNic.class);
        Mockito.when(nic1.getDeviceId()).thenReturn(0L);
        Mockito.when(nic1.isRequired()).thenReturn(true);

        VnfNic nic2 = Mockito.mock(VnfNic.class);
        Mockito.when(nic2.getDeviceId()).thenReturn(2L);

        VnfNic nic3 = Mockito.mock(VnfNic.class);

        List<VnfNic> nicsList = Arrays.asList(nic1, nic2, nic3);

        VnfTemplateUtils.validateVnfNics(nicsList);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVnfNicsWithInvalidRequired() {
        VnfNic nic1 = Mockito.mock(VnfNic.class);
        Mockito.when(nic1.getDeviceId()).thenReturn(0L);
        Mockito.when(nic1.isRequired()).thenReturn(true);

        VnfNic nic2 = Mockito.mock(VnfNic.class);
        Mockito.when(nic2.getDeviceId()).thenReturn(1L);
        Mockito.when(nic2.isRequired()).thenReturn(false);

        VnfNic nic3 = Mockito.mock(VnfNic.class);
        Mockito.when(nic3.getDeviceId()).thenReturn(2L);
        Mockito.when(nic3.isRequired()).thenReturn(true);

        List<VnfNic> nicsList = Arrays.asList(nic1, nic2, nic3);

        VnfTemplateUtils.validateVnfNics(nicsList);
    }

    @Test
    public void testValidateApiCommandParamsAllGood() {
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        RegisterVnfTemplateCmd cmd = Mockito.mock(RegisterVnfTemplateCmd.class);
        Map<String, String> vnfDetails = Mockito.spy(new HashMap<>());
        vnfDetails.put("username", "admin");
        vnfDetails.put("password", "password");
        vnfDetails.put("version", "4.19.0");
        vnfDetails.put("vendor", "cloudstack");
        Mockito.when(cmd.getVnfDetails()).thenReturn(vnfDetails);

        VnfTemplateUtils.validateApiCommandParams(cmd, template);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateApiCommandParamsInvalidAccessMethods() {
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(template.getTemplateType()).thenReturn(Storage.TemplateType.VNF);
        UpdateVnfTemplateCmd cmd = Mockito.mock(UpdateVnfTemplateCmd.class);
        Map<String, String> vnfDetails = Mockito.spy(new HashMap<>());
        vnfDetails.put("access_methods", "invalid");
        Mockito.when(cmd.getVnfDetails()).thenReturn(vnfDetails);

        VnfTemplateUtils.validateApiCommandParams(cmd, template);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateApiCommandParamsInvalidAccessDetails() {
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(template.getTemplateType()).thenReturn(Storage.TemplateType.VNF);
        UpdateVnfTemplateCmd cmd = Mockito.mock(UpdateVnfTemplateCmd.class);
        Map<String, String> vnfDetails = Mockito.spy(new HashMap<>());
        vnfDetails.put("invalid", "value");
        Mockito.when(cmd.getVnfDetails()).thenReturn(vnfDetails);

        VnfTemplateUtils.validateApiCommandParams(cmd, template);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateApiCommandParamsInvalidTemplateType() {
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(template.getTemplateType()).thenReturn(Storage.TemplateType.USER);
        UpdateVnfTemplateCmd cmd = Mockito.mock(UpdateVnfTemplateCmd.class);

        VnfTemplateUtils.validateApiCommandParams(cmd, template);
    }

    @Test
    public void testValidateVnfCidrList() {
        List<String> cidrList = new ArrayList<>();
        cidrList.add("10.10.10.0/24");
        VnfTemplateUtils.validateVnfCidrList(cidrList);
    }

    @Test
    public void testValidateVnfCidrListWithEmptyList() {
        List<String> cidrList = new ArrayList<>();
        VnfTemplateUtils.validateVnfCidrList(cidrList);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVnfCidrListwithInvalidList() {
        List<String> cidrList = new ArrayList<>();
        cidrList.add("10.10.10.0/24");
        cidrList.add("10.10.10.0/33");
        VnfTemplateUtils.validateVnfCidrList(cidrList);
    }
}
