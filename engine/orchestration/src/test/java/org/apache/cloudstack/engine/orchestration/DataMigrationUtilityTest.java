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
package org.apache.cloudstack.engine.orchestration;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.VMTemplateVO;
import junit.framework.TestCase;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DataMigrationUtilityTest extends TestCase {

    @Spy
    private DataMigrationUtility dataMigrationUtility;

    @Mock
    private VMTemplateVO templateVoMock;

    @Mock
    private TemplateDataStoreVO templateDataStoreVoMock;

    private void prepareForShouldMigrateTemplateTests() {
        Mockito.when(templateDataStoreVoMock.getState()).thenReturn(ObjectInDataStoreStateMachine.State.Ready);
        Mockito.when(templateVoMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(templateVoMock.getParentTemplateId()).thenReturn(null);
    }

    @Test
    public void shouldMigrateTemplateTestReturnsFalseWhenTemplateIsNotReady() {
        prepareForShouldMigrateTemplateTests();
        Mockito.when(templateDataStoreVoMock.getState()).thenReturn(ObjectInDataStoreStateMachine.State.Migrating);

        boolean result = dataMigrationUtility.shouldMigrateTemplate(templateDataStoreVoMock, templateVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void shouldMigrateTemplateTestReturnsFalseWhenHypervisorTypeIsSimulator() {
        prepareForShouldMigrateTemplateTests();
        Mockito.when(templateVoMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.Simulator);

        boolean result = dataMigrationUtility.shouldMigrateTemplate(templateDataStoreVoMock, templateVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void shouldMigrateTemplateTestReturnsFalseWhenTemplateHasParentTemplate() {
        prepareForShouldMigrateTemplateTests();
        Mockito.when(templateVoMock.getParentTemplateId()).thenReturn(1L);

        boolean result = dataMigrationUtility.shouldMigrateTemplate(templateDataStoreVoMock, templateVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void shouldMigrateTemplateTestReturnsTrueWhenTemplateIsReadyAndDoesNotHaveParentTemplateAndHypervisorTypeIsNotSimulator() {
        prepareForShouldMigrateTemplateTests();

        boolean result = dataMigrationUtility.shouldMigrateTemplate(templateDataStoreVoMock, templateVoMock);

        Assert.assertTrue(result);
    }
}
