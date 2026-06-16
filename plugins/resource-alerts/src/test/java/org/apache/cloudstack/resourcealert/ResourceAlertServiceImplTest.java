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

package org.apache.cloudstack.resourcealert;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.resourcealert.api.command.admin.CreateResourceAlertRuleCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.DeleteResourceAlertRuleCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.ListResourceAlertsCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.UpdateResourceAlertRuleCmd;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleJoinDao;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertRuleVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.AccountManager;

@RunWith(MockitoJUnitRunner.class)
public class ResourceAlertServiceImplTest {

    @InjectMocks
    ResourceAlertServiceImpl service;

    @Mock AccountManager accountManager;
    @Mock DomainDao domainDao;
    @Mock ResourceAlertRuleDao ruleDao;
    @Mock ResourceAlertRuleJoinDao ruleJoinDao;
    @Mock ResourceAlertDao alertDao;

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateFailsOnInvalidCondition() {
        CreateResourceAlertRuleCmd cmd = mock(CreateResourceAlertRuleCmd.class);
        when(cmd.getResourceType()).thenReturn("VirtualMachine");
        when(cmd.getCondition()).thenReturn("GREATER_THAN");

        service.createResourceAlertRule(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateFailsOnInvalidSeverity() {
        CreateResourceAlertRuleCmd cmd = mock(CreateResourceAlertRuleCmd.class);
        when(cmd.getResourceType()).thenReturn("VirtualMachine");
        when(cmd.getCondition()).thenReturn("GT");
        when(cmd.getSeverity()).thenReturn("URGENT");

        service.createResourceAlertRule(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateFailsOnInvalidResourceType() {
        CreateResourceAlertRuleCmd cmd = mock(CreateResourceAlertRuleCmd.class);
        when(cmd.getResourceType()).thenReturn("Database");

        service.createResourceAlertRule(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateFailsWhenMetricDoesNotApplyToResourceType() {
        CreateResourceAlertRuleCmd cmd = mock(CreateResourceAlertRuleCmd.class);
        when(cmd.getResourceType()).thenReturn("VirtualMachine");
        when(cmd.getCondition()).thenReturn("GT");
        when(cmd.getSeverity()).thenReturn("HIGH");
        when(cmd.getMetric()).thenReturn("STORAGE_UTILIZATION"); // only applies to StoragePool

        service.createResourceAlertRule(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateFailsWhenRuleNotFound() {
        UpdateResourceAlertRuleCmd cmd = mock(UpdateResourceAlertRuleCmd.class);
        when(cmd.getId()).thenReturn(999L);
        when(ruleDao.findById(999L)).thenReturn(null);

        service.updateResourceAlertRule(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateFailsWhenRuleAlreadyDeleted() {
        UpdateResourceAlertRuleCmd cmd = mock(UpdateResourceAlertRuleCmd.class);
        when(cmd.getId()).thenReturn(1L);

        ResourceAlertRuleVO deletedRule = mock(ResourceAlertRuleVO.class);
        when(deletedRule.getRemoved()).thenReturn(new java.util.Date());
        when(ruleDao.findById(1L)).thenReturn(deletedRule);

        service.updateResourceAlertRule(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteFailsWhenRuleNotFound() {
        DeleteResourceAlertRuleCmd cmd = mock(DeleteResourceAlertRuleCmd.class);
        when(cmd.getId()).thenReturn(999L);
        when(ruleDao.findById(999L)).thenReturn(null);

        service.deleteResourceAlertRule(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListAlertsFailsWithUnknownRuleUuid() {
        ListResourceAlertsCmd cmd = mock(ListResourceAlertsCmd.class);
        when(cmd.getAlertRuleId()).thenReturn("no-such-uuid");
        when(ruleDao.findByUuid("no-such-uuid")).thenReturn(null);

        service.listResourceAlerts(cmd);
    }
}
