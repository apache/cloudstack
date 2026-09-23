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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.resourcealert.api.command.admin.CreateResourceAlertRuleCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.DeleteResourceAlertRuleCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.ListResourceAlertsCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.UpdateResourceAlertRuleCmd;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleJoinDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleWebhookDao;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertRuleVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.webhook.WebhookHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ResourceAlertServiceImplTest {

    @Spy
    @InjectMocks
    ResourceAlertServiceImpl service;

    @Mock AccountManager accountManager;
    @Mock ResourceAlertRuleDao ruleDao;
    @Mock ResourceAlertRuleJoinDao ruleJoinDao;
    @Mock ResourceAlertDao alertDao;
    @Mock ResourceAlertRuleWebhookDao ruleWebhookDao;
    @Mock WebhookHelper webhookHelper;
    @Mock UserVmDao userVmDao;
    @Mock VolumeDao volumeDao;
    @Mock HostDao hostDao;
    @Mock PrimaryDataStoreDao storagePoolDao;

    private MockedStatic<CallContext> callContextMocked;
    private Account caller;
    private Account owner;

    @Before
    public void setUp() {
        caller = mock(Account.class);
        when(caller.getId()).thenReturn(2L);
        CallContext callContext = mock(CallContext.class);
        when(callContext.getCallingAccount()).thenReturn(caller);
        callContextMocked = Mockito.mockStatic(CallContext.class);
        callContextMocked.when(CallContext::current).thenReturn(callContext);

        owner = mock(Account.class);
        when(owner.getId()).thenReturn(42L);
        when(accountManager.finalizeOwner(eq(caller), any(), any(), any())).thenReturn(owner);
    }

    @After
    public void tearDown() {
        callContextMocked.close();
    }

    private CreateResourceAlertRuleCmd validVmCreateCmd() {
        CreateResourceAlertRuleCmd cmd = mock(CreateResourceAlertRuleCmd.class);
        when(cmd.getName()).thenReturn("cpu-high");
        when(cmd.getResourceType()).thenReturn("VirtualMachine");
        when(cmd.getCondition()).thenReturn("GT");
        when(cmd.getSeverity()).thenReturn("HIGH");
        when(cmd.getMetric()).thenReturn("CPU_UTILIZATION");
        when(cmd.getThreshold()).thenReturn(80.0);
        when(cmd.getResetInterval()).thenReturn(null);
        when(cmd.getEmail()).thenReturn(null);
        return cmd;
    }

    private ResourceAlertRuleVO persistedRuleCapture() {
        ArgumentCaptor<ResourceAlertRuleVO> captor = ArgumentCaptor.forClass(ResourceAlertRuleVO.class);
        verify(ruleDao).persist(captor.capture());
        return captor.getValue();
    }

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
        CreateResourceAlertRuleCmd cmd = validVmCreateCmd();
        when(cmd.getMetric()).thenReturn("STORAGE_UTILIZATION");

        service.createResourceAlertRule(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateFailsWhenAccountAtRuleLimit() {
        CreateResourceAlertRuleCmd cmd = validVmCreateCmd();
        // default limit is 20
        when(ruleDao.countActiveByAccountId(42L)).thenReturn(20);

        service.createResourceAlertRule(cmd);
    }

    @Test
    public void testCreateUsesDefaultResetIntervalWhenNotSet() {
        service.createResourceAlertRule(validVmCreateCmd());

        assertEquals(600, persistedRuleCapture().getResetInterval());
    }

    @Test
    public void testCreateAssignsRuleToFinalizedOwner() {
        service.createResourceAlertRule(validVmCreateCmd());

        assertEquals(42L, persistedRuleCapture().getAccountId());
    }

    @Test
    public void testCreateResolvesResourceUuidAndChecksOwnerAccess() {
        CreateResourceAlertRuleCmd cmd = validVmCreateCmd();
        when(cmd.getResourceId()).thenReturn("vm-uuid");
        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getId()).thenReturn(7L);
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(vm);

        service.createResourceAlertRule(cmd);

        verify(accountManager).checkAccess(owner, null, false, (ControlledEntity) vm);
        assertEquals(Long.valueOf(7L), persistedRuleCapture().getResourceId());
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCreateFailsWhenOwnerCannotAccessResource() {
        CreateResourceAlertRuleCmd cmd = validVmCreateCmd();
        when(cmd.getResourceId()).thenReturn("vm-uuid");
        UserVmVO vm = mock(UserVmVO.class);
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(vm);
        doThrow(new PermissionDeniedException("denied"))
                .when(accountManager).checkAccess(owner, null, false, (ControlledEntity) vm);

        service.createResourceAlertRule(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateFailsOnUnknownResourceUuid() {
        CreateResourceAlertRuleCmd cmd = validVmCreateCmd();
        when(cmd.getResourceId()).thenReturn("no-such-vm");

        service.createResourceAlertRule(cmd);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCreateHostRuleFailsForNonRootAdmin() {
        CreateResourceAlertRuleCmd cmd = validVmCreateCmd();
        when(cmd.getResourceType()).thenReturn("Host");
        when(accountManager.isRootAdmin(2L)).thenReturn(false);

        service.createResourceAlertRule(cmd);
    }

    @Test
    public void testCreateHostRuleAllowedForRootAdmin() {
        CreateResourceAlertRuleCmd cmd = validVmCreateCmd();
        when(cmd.getResourceType()).thenReturn("Host");
        when(accountManager.isRootAdmin(2L)).thenReturn(true);

        service.createResourceAlertRule(cmd);

        verify(ruleDao).persist(any(ResourceAlertRuleVO.class));
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCreateWithEmailFailsForNonRootAdmin() {
        CreateResourceAlertRuleCmd cmd = validVmCreateCmd();
        when(cmd.getEmail()).thenReturn(true);
        when(accountManager.isRootAdmin(2L)).thenReturn(false);

        service.createResourceAlertRule(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateFailsWhenRuleNotFound() {
        UpdateResourceAlertRuleCmd cmd = mock(UpdateResourceAlertRuleCmd.class);
        when(cmd.getId()).thenReturn(999L);

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

    @Test(expected = PermissionDeniedException.class)
    public void testUpdateFailsWhenCallerCannotAccessRule() {
        UpdateResourceAlertRuleCmd cmd = mock(UpdateResourceAlertRuleCmd.class);
        when(cmd.getId()).thenReturn(1L);
        ResourceAlertRuleVO rule = mock(ResourceAlertRuleVO.class);
        when(ruleDao.findById(1L)).thenReturn(rule);
        doThrow(new PermissionDeniedException("denied")).when(accountManager).checkAccess(caller, null, true, rule);

        service.updateResourceAlertRule(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteFailsWhenRuleNotFound() {
        DeleteResourceAlertRuleCmd cmd = mock(DeleteResourceAlertRuleCmd.class);
        when(cmd.getId()).thenReturn(999L);

        service.deleteResourceAlertRule(cmd);
    }

    @Test
    public void testDeleteDoesNotRemoveWhenCallerCannotAccessRule() {
        DeleteResourceAlertRuleCmd cmd = mock(DeleteResourceAlertRuleCmd.class);
        when(cmd.getId()).thenReturn(1L);
        ResourceAlertRuleVO rule = mock(ResourceAlertRuleVO.class);
        when(ruleDao.findById(1L)).thenReturn(rule);
        doThrow(new PermissionDeniedException("denied")).when(accountManager).checkAccess(caller, null, true, rule);

        try {
            service.deleteResourceAlertRule(cmd);
        } catch (PermissionDeniedException expected) {
        }
        verify(ruleDao, never()).remove(1L);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListAlertsFailsWithUnknownRuleUuid() {
        ListResourceAlertsCmd cmd = mock(ListResourceAlertsCmd.class);
        when(cmd.getAlertRuleId()).thenReturn("no-such-uuid");

        service.listResourceAlerts(cmd);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testListAlertsFailsWhenCallerCannotAccessRule() {
        ListResourceAlertsCmd cmd = mock(ListResourceAlertsCmd.class);
        when(cmd.getAlertRuleId()).thenReturn("rule-uuid");
        ResourceAlertRuleVO rule = mock(ResourceAlertRuleVO.class);
        when(ruleDao.findByUuid("rule-uuid")).thenReturn(rule);
        doThrow(new PermissionDeniedException("denied")).when(accountManager).checkAccess(caller, null, true, rule);

        service.listResourceAlerts(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListAlertsFailsWhenResourceIdWithoutType() {
        ListResourceAlertsCmd cmd = mock(ListResourceAlertsCmd.class);
        when(cmd.getResourceId()).thenReturn("vm-uuid");

        service.listResourceAlerts(cmd);
    }

    private ControlledEntity mockWebhook(String uuid, long id) {
        ControlledEntity webhook = mock(ControlledEntity.class, Mockito.withSettings().extraInterfaces(InternalIdentity.class));
        when(((InternalIdentity) webhook).getId()).thenReturn(id);
        when(webhookHelper.findWebhookByUuid(uuid)).thenReturn(webhook);
        return webhook;
    }

    @Test
    public void testCreateMapsWebhooksAfterCheckingOwnerAccess() {
        doReturn(webhookHelper).when(service).getWebhookHelper();
        CreateResourceAlertRuleCmd cmd = validVmCreateCmd();
        when(cmd.getWebhookIds()).thenReturn(List.of("wh-1", "wh-1"));
        ControlledEntity webhook = mockWebhook("wh-1", 11L);

        service.createResourceAlertRule(cmd);

        verify(accountManager, Mockito.times(2)).checkAccess(owner, null, false, webhook);
        verify(ruleWebhookDao).replaceWebhooksForRule(Mockito.anyLong(), eq(List.of(11L)));
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCreateFailsWhenOwnerCannotAccessWebhook() {
        doReturn(webhookHelper).when(service).getWebhookHelper();
        CreateResourceAlertRuleCmd cmd = validVmCreateCmd();
        when(cmd.getWebhookIds()).thenReturn(List.of("wh-1"));
        ControlledEntity webhook = mockWebhook("wh-1", 11L);
        doThrow(new PermissionDeniedException("denied")).when(accountManager).checkAccess(owner, null, false, webhook);

        service.createResourceAlertRule(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateFailsOnUnknownWebhook() {
        doReturn(webhookHelper).when(service).getWebhookHelper();
        CreateResourceAlertRuleCmd cmd = validVmCreateCmd();
        when(cmd.getWebhookIds()).thenReturn(List.of("no-such-webhook"));

        service.createResourceAlertRule(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateWithWebhooksFailsWhenWebhookPluginMissing() {
        doReturn(null).when(service).getWebhookHelper();
        CreateResourceAlertRuleCmd cmd = validVmCreateCmd();
        when(cmd.getWebhookIds()).thenReturn(List.of("wh-1"));

        service.createResourceAlertRule(cmd);
    }

    @Test
    public void testCreateWithoutWebhooksDoesNotTouchMapping() {
        service.createResourceAlertRule(validVmCreateCmd());

        verify(ruleWebhookDao, never()).replaceWebhooksForRule(Mockito.anyLong(), any());
    }

    @Test
    public void testUpdateCleanupWebhooksClearsMapping() {
        UpdateResourceAlertRuleCmd cmd = mock(UpdateResourceAlertRuleCmd.class);
        when(cmd.getId()).thenReturn(1L);
        when(cmd.isCleanupWebhooks()).thenReturn(true);
        ResourceAlertRuleVO rule = mock(ResourceAlertRuleVO.class);
        when(rule.getId()).thenReturn(1L);
        when(ruleDao.findById(1L)).thenReturn(rule);

        service.updateResourceAlertRule(cmd);

        verify(ruleWebhookDao).replaceWebhooksForRule(1L, new ArrayList<>());
    }
}
