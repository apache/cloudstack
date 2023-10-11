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
import com.cloud.storage.VnfTemplateNicVO;
import com.cloud.storage.dao.VnfTemplateDetailsDao;
import com.cloud.storage.dao.VnfTemplateNicDao;
import com.cloud.template.VirtualMachineTemplate;
import org.apache.cloudstack.api.command.user.template.RegisterVnfTemplateCmd;
import org.apache.cloudstack.api.command.user.template.UpdateVnfTemplateCmd;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class VnfTemplateManagerImplTest {

    @Spy
    @InjectMocks
    VnfTemplateManagerImpl vnfTemplateManagerImpl;

    @Mock
    VnfTemplateDetailsDao vnfTemplateDetailsDao;
    @Mock
    VnfTemplateNicDao vnfTemplateNicDao;

    @Mock
    VirtualMachineTemplate template;

    static long templateId = 100L;
    final Map<String, Object> vnfNics = new HashMap<>();
    final Map<String, Object> vnfDetails = new HashMap<>();

    @Before
    public void setUp() {
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

        vnfDetails.put("0", new HashMap<>(Map.ofEntries(
                Map.entry("accessMethods", "console,http,https"),
                Map.entry("username", "admin"),
                Map.entry("password", "password"),
                Map.entry("version", "4.19.0"),
                Map.entry("vendor", "cloudstack")
        )));

        VnfTemplateNicVO vnfNic1 = new VnfTemplateNicVO(templateId, 0L, "eth0", true, true, "first");
        VnfTemplateNicVO vnfNic2 = new VnfTemplateNicVO(templateId, 1L, "eth1", true, true, "second");
        VnfTemplateNicVO vnfNic3 = new VnfTemplateNicVO(templateId, 2L, "eth2", false, true, "second");
        Mockito.doReturn(Arrays.asList(vnfNic1, vnfNic2, vnfNic3)).when(vnfTemplateNicDao).listByTemplateId(templateId);

        Mockito.when(template.getId()).thenReturn(templateId);
    }

    @Test
    public void testPersistVnfTemplateRegister() {
        RegisterVnfTemplateCmd cmd = new RegisterVnfTemplateCmd();
        ReflectionTestUtils.setField(cmd,"vnfNics", vnfNics);
        ReflectionTestUtils.setField(cmd,"vnfDetails", vnfDetails);

        vnfTemplateManagerImpl.persistVnfTemplate(templateId, cmd);

        Mockito.verify(vnfTemplateNicDao, Mockito.times(vnfNics.size())).persist(any(VnfTemplateNicVO.class));
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(0)).removeDetails(templateId);
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(5)).addDetail(eq(templateId), anyString(), anyString(), eq(true));
    }

    @Test
    public void testPersistVnfTemplateUpdate() {
        UpdateVnfTemplateCmd cmd = new UpdateVnfTemplateCmd();
        ReflectionTestUtils.setField(cmd,"vnfNics", vnfNics);
        ReflectionTestUtils.setField(cmd,"vnfDetails", vnfDetails);

        vnfTemplateManagerImpl.updateVnfTemplate(templateId, cmd);

        Mockito.verify(vnfTemplateNicDao, Mockito.times(vnfNics.size())).persist(any(VnfTemplateNicVO.class));
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(1)).removeDetails(templateId);
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(5)).addDetail(eq(templateId), anyString(), anyString(), eq(true));
    }

    @Test
    public void testPersistVnfTemplateUpdateWithoutNics() {
        UpdateVnfTemplateCmd cmd = new UpdateVnfTemplateCmd();
        ReflectionTestUtils.setField(cmd,"vnfDetails", vnfDetails);
        ReflectionTestUtils.setField(cmd,"cleanupVnfNics", true);

        vnfTemplateManagerImpl.updateVnfTemplate(templateId, cmd);

        Mockito.verify(vnfTemplateNicDao, Mockito.times(1)).deleteByTemplateId(templateId);
        Mockito.verify(vnfTemplateNicDao, Mockito.times(0)).persist(any(VnfTemplateNicVO.class));
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(1)).removeDetails(templateId);
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(5)).addDetail(eq(templateId), anyString(), anyString(), eq(true));
    }

    @Test
    public void testPersistVnfTemplateUpdateWithoutDetails() {
        UpdateVnfTemplateCmd cmd = new UpdateVnfTemplateCmd();
        ReflectionTestUtils.setField(cmd,"vnfNics", vnfNics);
        ReflectionTestUtils.setField(cmd,"cleanupVnfDetails", true);

        vnfTemplateManagerImpl.updateVnfTemplate(templateId, cmd);

        Mockito.verify(vnfTemplateNicDao, Mockito.times(vnfNics.size())).persist(any(VnfTemplateNicVO.class));
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(1)).removeDetails(templateId);
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(0)).addDetail(eq(templateId), anyString(), anyString(), eq(true));
    }

    @Test
    public void testValidateVnfApplianceNicsWithRequiredNics() {
        List<Long> networkIds = Arrays.asList(200L, 201L);
        vnfTemplateManagerImpl.validateVnfApplianceNics(template, networkIds);
    }

    @Test
    public void testValidateVnfApplianceNicsWithAllNics() {
        List<Long> networkIds = Arrays.asList(200L, 201L, 202L);
        vnfTemplateManagerImpl.validateVnfApplianceNics(template, networkIds);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVnfApplianceNicsWithEmptyList() {
        List<Long> networkIds = new ArrayList<>();
        vnfTemplateManagerImpl.validateVnfApplianceNics(template, networkIds);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVnfApplianceNicsWithMissingNetworkId() {
        List<Long> networkIds = Arrays.asList(200L);
        vnfTemplateManagerImpl.validateVnfApplianceNics(template, networkIds);
    }
}
