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
package com.cloud.vm.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupReadinessRuleDetailsVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class InstanceBootGroupReadinessRuleDetailsDaoImplTest {

    @Spy
    InstanceBootGroupReadinessRuleDetailsDaoImpl instanceBootGroupReadinessRuleDetailsDaoImplSpy;

    private static final long RULE_ID = 15L;

    @SuppressWarnings("unchecked")
    @Test
    public void testAddDetailNonScriptKeyStoredAsIs() {
        Mockito.doReturn(null).when(instanceBootGroupReadinessRuleDetailsDaoImplSpy).findDetail(RULE_ID, "port");
        Mockito.doReturn(null).when(instanceBootGroupReadinessRuleDetailsDaoImplSpy).persist(Mockito.any(InstanceBootGroupReadinessRuleDetailsVO.class));

        instanceBootGroupReadinessRuleDetailsDaoImplSpy.addDetail(RULE_ID, "port", "8080", true);

        ArgumentCaptor<InstanceBootGroupReadinessRuleDetailsVO> voCaptor = ArgumentCaptor.forClass(InstanceBootGroupReadinessRuleDetailsVO.class);
        Mockito.verify(instanceBootGroupReadinessRuleDetailsDaoImplSpy).persist(voCaptor.capture());
        Assert.assertEquals(RULE_ID, voCaptor.getValue().getResourceId());
        Assert.assertEquals("port", voCaptor.getValue().getName());
        Assert.assertEquals("8080", voCaptor.getValue().getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddDetailScriptKeyStoredEncrypted() {
        Mockito.doReturn(null).when(instanceBootGroupReadinessRuleDetailsDaoImplSpy).findDetail(RULE_ID, "script");
        Mockito.doReturn(null).when(instanceBootGroupReadinessRuleDetailsDaoImplSpy).persist(Mockito.any(InstanceBootGroupReadinessRuleDetailsVO.class));

        String plainScript = "#!/bin/bash\necho ok";
        instanceBootGroupReadinessRuleDetailsDaoImplSpy.addDetail(RULE_ID, "script", plainScript, true);

        ArgumentCaptor<InstanceBootGroupReadinessRuleDetailsVO> voCaptor = ArgumentCaptor.forClass(InstanceBootGroupReadinessRuleDetailsVO.class);
        Mockito.verify(instanceBootGroupReadinessRuleDetailsDaoImplSpy).persist(voCaptor.capture());
        Assert.assertEquals(RULE_ID, voCaptor.getValue().getResourceId());
        Assert.assertEquals("script", voCaptor.getValue().getName());
        Assert.assertEquals(DBEncryptionUtil.encrypt(plainScript), voCaptor.getValue().getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddDetailReplacesExistingDetail() {
        InstanceBootGroupReadinessRuleDetailsVO existing = new InstanceBootGroupReadinessRuleDetailsVO(RULE_ID, "port", "9090");
        Mockito.doReturn(existing).when(instanceBootGroupReadinessRuleDetailsDaoImplSpy).findDetail(RULE_ID, "port");
        Mockito.doReturn(true).when(instanceBootGroupReadinessRuleDetailsDaoImplSpy).remove(existing.getId());
        Mockito.doReturn(null).when(instanceBootGroupReadinessRuleDetailsDaoImplSpy).persist(Mockito.any(InstanceBootGroupReadinessRuleDetailsVO.class));

        instanceBootGroupReadinessRuleDetailsDaoImplSpy.addDetail(RULE_ID, "port", "8080", true);

        Mockito.verify(instanceBootGroupReadinessRuleDetailsDaoImplSpy).remove(existing.getId());
        ArgumentCaptor<InstanceBootGroupReadinessRuleDetailsVO> voCaptor = ArgumentCaptor.forClass(InstanceBootGroupReadinessRuleDetailsVO.class);
        Mockito.verify(instanceBootGroupReadinessRuleDetailsDaoImplSpy).persist(voCaptor.capture());
        Assert.assertEquals("8080", voCaptor.getValue().getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetDetailsDecryptsScriptKeyAndLeavesOthersAsIs() {
        String encryptedScript = DBEncryptionUtil.encrypt("plain-script-body");
        List<InstanceBootGroupReadinessRuleDetailsVO> details = new ArrayList<>();
        details.add(new InstanceBootGroupReadinessRuleDetailsVO(RULE_ID, "script", encryptedScript));
        details.add(new InstanceBootGroupReadinessRuleDetailsVO(RULE_ID, "port", "8080"));
        Mockito.doReturn(details).when(instanceBootGroupReadinessRuleDetailsDaoImplSpy).listBy(Mockito.any(SearchCriteria.class));

        Map<String, String> result = instanceBootGroupReadinessRuleDetailsDaoImplSpy.getDetails(RULE_ID);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals(DBEncryptionUtil.decrypt(encryptedScript), result.get("script"));
        Assert.assertEquals("8080", result.get("port"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetDetailsEmpty() {
        Mockito.doReturn(new ArrayList<>()).when(instanceBootGroupReadinessRuleDetailsDaoImplSpy).listBy(Mockito.any(SearchCriteria.class));

        Map<String, String> result = instanceBootGroupReadinessRuleDetailsDaoImplSpy.getDetails(RULE_ID);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }
}
