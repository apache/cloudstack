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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleWebhookDao;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertRuleVO;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.utils.mailing.SMTPMailProperties;
import org.apache.cloudstack.utils.mailing.SMTPMailSender;
import org.apache.cloudstack.webhook.WebhookHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.host.HostStats;
import com.cloud.host.dao.HostDao;
import com.cloud.server.ResourceTag;
import com.cloud.server.StatsCollector;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VolumeStats;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmStats;
import com.cloud.vm.dao.UserVmDao;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RunWith(MockitoJUnitRunner.class)
public class ResourceAlertManagerImplTest {

    @Spy @InjectMocks
    ResourceAlertManagerImpl manager;

    @Mock ResourceAlertRuleDao ruleDao;
    @Mock ResourceAlertDao alertDao;
    @Mock ResourceAlertRuleWebhookDao ruleWebhookDao;
    @Mock WebhookHelper webhookHelper;
    @Mock UserVmDao userVmDao;
    @Mock HostDao hostDao;
    @Mock PrimaryDataStoreDao storagePoolDao;
    @Mock VolumeDao volumeDao;
    @Mock StatsCollector statsCollector;
    @Mock ConfigurationDao configDao;
    @Mock ResourceTagDao resourceTagDao;
    @Mock SMTPMailSender mailSender;

    @Captor ArgumentCaptor<ResourceAlertVO> alertCaptor;
    @Captor ArgumentCaptor<SMTPMailProperties> mailCaptor;

    private static final long VM_ID = 101L;
    private static final long HOST_ID = 201L;
    private static final long POOL_ID = 301L;

    @Before
    public void setUp() throws Exception {
        // stub out the AlertGenerator static call (needs Spring context in real env)
        doNothing().when(manager).publishAlertEvent(anyLong(), anyString(), anyString());
    }

    private ResourceAlertRuleVO vmCpuRule(Long resourceId) {
        return vmCpuRuleWithEmail(resourceId, false);
    }

    private ResourceAlertRuleVO vmCpuRuleWithEmail(Long resourceId, boolean email) {
        return new ResourceAlertRuleVO("test", ResourceAlertRule.ResourceType.VirtualMachine,
                resourceId, 1L, 1L, "CPU_UTILIZATION", AlertCondition.GT, 80.0,
                AlertSeverity.HIGH, "CPU high", email, 600);
    }

    private void injectMailSender(String... recipients) throws Exception {
        Field f = ResourceAlertManagerImpl.class.getDeclaredField("mailSender");
        f.setAccessible(true);
        f.set(manager, mailSender);

        Field r = ResourceAlertManagerImpl.class.getDeclaredField("emailRecipients");
        r.setAccessible(true);
        r.set(manager, recipients);

        Field s = ResourceAlertManagerImpl.class.getDeclaredField("senderAddress");
        s.setAccessible(true);
        s.set(manager, "alerts@example.com");

        // replace async executor with a synchronous one so verify() works immediately
        manager.emailExecutor = new AbstractExecutorService() {
            @Override public void execute(Runnable command) { command.run(); }
            @Override public void shutdown() {}
            @Override public List<Runnable> shutdownNow() { return Collections.emptyList(); }
            @Override public boolean isShutdown() { return false; }
            @Override public boolean isTerminated() { return false; }
            @Override public boolean awaitTermination(long t, TimeUnit u) { return true; }
        };
    }

    @Test
    public void testVmCpuRuleFiresWhenThresholdBreached() {
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(null);

        manager.evaluateRules();

        verify(alertDao).persist(alertCaptor.capture());
        ResourceAlertVO fired = alertCaptor.getValue();
        assertEquals(VM_ID, (long) fired.getResourceId());
        assertEquals("CPU_UTILIZATION", fired.getMetricType());
        assertEquals(85.0, fired.getMetricValue(), 0.001);
        assertEquals(AlertSeverity.HIGH, fired.getSeverity());
    }

    @Test
    public void testVmCpuRuleDoesNotFireWhenBelowThreshold() {
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(75.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);

        manager.evaluateRules();

        verify(alertDao, never()).persist(any());
    }

    @Test
    public void testRuleDoesNotFireWithinResetInterval() {
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);

        ResourceAlertVO recentAlert = mock(ResourceAlertVO.class);
        when(recentAlert.getAlertTimestamp()).thenReturn(new Date(System.currentTimeMillis() - 10_000L));
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(recentAlert);

        manager.evaluateRules();

        verify(alertDao, never()).persist(any());
    }

    @Test
    public void testRuleFiresAfterResetIntervalExpires() {
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);

        ResourceAlertVO oldAlert = mock(ResourceAlertVO.class);
        when(oldAlert.getAlertTimestamp()).thenReturn(new Date(System.currentTimeMillis() - 700_000L));
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(oldAlert);

        manager.evaluateRules();

        verify(alertDao).persist(any());
    }

    @Test
    public void testNullStatsSkipped() {
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(null);

        manager.evaluateRules();

        verify(alertDao, never()).persist(any());
    }

    @Test
    public void testVmMemorySkippedWhenNoBalloonDriver() {
        ResourceAlertRuleVO rule = new ResourceAlertRuleVO("test",
                ResourceAlertRule.ResourceType.VirtualMachine, VM_ID, 1L, 1L,
                "MEMORY_UTILIZATION", AlertCondition.GT, 50.0, AlertSeverity.MEDIUM, null, false, 600);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getMemoryKBs()).thenReturn(8192.0);
        when(stats.getIntFreeMemoryKBs()).thenReturn(-1.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);

        manager.evaluateRules();

        verify(alertDao, never()).persist(any());
    }

    @Test
    public void testVmMemoryUtilizationCalculation() {
        ResourceAlertRuleVO rule = new ResourceAlertRuleVO("test",
                ResourceAlertRule.ResourceType.VirtualMachine, VM_ID, 1L, 1L,
                "MEMORY_UTILIZATION", AlertCondition.GT, 70.0, AlertSeverity.HIGH, null, false, 600);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getMemoryKBs()).thenReturn(8192.0);
        when(stats.getIntFreeMemoryKBs()).thenReturn(2048.0); // 75% used
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(null);

        manager.evaluateRules();

        verify(alertDao).persist(alertCaptor.capture());
        assertEquals(75.0, alertCaptor.getValue().getMetricValue(), 0.001);
    }

    @Test
    public void testStorageUtilizationCalculation() {
        ResourceAlertRuleVO rule = new ResourceAlertRuleVO("test",
                ResourceAlertRule.ResourceType.StoragePool, POOL_ID, 1L, 1L,
                "STORAGE_UTILIZATION", AlertCondition.GT, 65.0, AlertSeverity.HIGH, null, false, 600);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        StorageStats poolStats = mock(StorageStats.class);
        when(poolStats.getCapacityBytes()).thenReturn(10000L);
        when(poolStats.getByteUsed()).thenReturn(7000L); // 70%
        when(statsCollector.getStoragePoolStats(POOL_ID)).thenReturn(poolStats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(POOL_ID))).thenReturn(null);

        manager.evaluateRules();

        verify(alertDao).persist(alertCaptor.capture());
        assertEquals(70.0, alertCaptor.getValue().getMetricValue(), 0.001);
    }

    @Test
    public void testGenericVmRuleFansOutToAllRunningVms() {
        ResourceAlertRuleVO rule = vmCpuRule(null);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        UserVmVO vm1 = mock(UserVmVO.class);
        when(vm1.getId()).thenReturn(101L);
        when(vm1.getState()).thenReturn(VirtualMachine.State.Running);

        UserVmVO vm2 = mock(UserVmVO.class);
        when(vm2.getId()).thenReturn(102L);
        when(vm2.getState()).thenReturn(VirtualMachine.State.Running);

        when(userVmDao.listByAccountId(1L)).thenReturn(Arrays.asList(vm1, vm2));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(101L, false)).thenReturn(stats);
        when(statsCollector.getVmStats(102L, false)).thenReturn(stats);
        when(alertDao.findLastFiredForRule(anyLong(), anyLong())).thenReturn(null);

        manager.evaluateRules();

        verify(alertDao, times(2)).persist(any());
    }

    @Test
    public void testGenericVmRuleSkipsStoppedVms() {
        ResourceAlertRuleVO rule = vmCpuRule(null);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        UserVmVO stopped = mock(UserVmVO.class);
        when(stopped.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(userVmDao.listByAccountId(1L)).thenReturn(Collections.singletonList(stopped));

        manager.evaluateRules();

        verify(alertDao, never()).persist(any());
    }

    @Test
    public void testHostCpuRuleUsesHostStats() {
        ResourceAlertRuleVO rule = new ResourceAlertRuleVO("test",
                ResourceAlertRule.ResourceType.Host, HOST_ID, 1L, 1L,
                "CPU_UTILIZATION", AlertCondition.GT, 85.0, AlertSeverity.CRITICAL, null, false, 600);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        HostStats hostStats = mock(HostStats.class);
        when(hostStats.getCpuUtilization()).thenReturn(90.0);
        when(statsCollector.getHostStats(HOST_ID)).thenReturn(hostStats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(HOST_ID))).thenReturn(null);

        manager.evaluateRules();

        verify(alertDao).persist(alertCaptor.capture());
        assertEquals(HOST_ID, (long) alertCaptor.getValue().getResourceId());
        assertEquals(90.0, alertCaptor.getValue().getMetricValue(), 0.001);
    }

    @Test
    public void testHostMemoryUtilizationCalculation() {
        ResourceAlertRuleVO rule = new ResourceAlertRuleVO("test",
                ResourceAlertRule.ResourceType.Host, HOST_ID, 1L, 1L,
                "MEMORY_UTILIZATION", AlertCondition.GT, 80.0, AlertSeverity.HIGH, null, false, 600);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        HostStats hostStats = mock(HostStats.class);
        when(hostStats.getTotalMemoryKBs()).thenReturn(16384.0);
        when(hostStats.getFreeMemoryKBs()).thenReturn(1638.4); // ~90% used
        when(statsCollector.getHostStats(HOST_ID)).thenReturn(hostStats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(HOST_ID))).thenReturn(null);

        manager.evaluateRules();

        verify(alertDao).persist(alertCaptor.capture());
        assertEquals(90.0, alertCaptor.getValue().getMetricValue(), 0.01);
    }

    @Test
    public void testEventBusPublishedOnFiring() {
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(null);

        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getDataCenterId()).thenReturn(1L);
        when(userVmDao.findById(VM_ID)).thenReturn(vm);

        manager.evaluateRules();

        verify(manager).publishAlertEvent(eq(1L), anyString(), anyString());
    }

    @Test
    public void testEventBusNotPublishedWhenNoFiring() {
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(75.0); // below threshold
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);

        manager.evaluateRules();

        verify(manager, never()).publishAlertEvent(anyLong(), anyString(), anyString());
    }

    @Test
    public void testEmailSentWhenRuleHasEmailEnabled() throws Exception {
        injectMailSender("admin@example.com");

        ResourceAlertRuleVO rule = vmCpuRuleWithEmail(VM_ID, true);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(null);

        manager.evaluateRules();

        verify(mailSender).sendMail(mailCaptor.capture());
        SMTPMailProperties mail = mailCaptor.getValue();
        assertTrue(mail.getSubject().contains("CPU_UTILIZATION"));
        assertTrue(mail.getSubject().contains("HIGH"));
        assertTrue(mail.getContent().toString().contains("85."));
    }

    @Test
    public void testEmailSkippedWhenRuleHasEmailDisabled() throws Exception {
        injectMailSender("admin@example.com");

        ResourceAlertRuleVO rule = vmCpuRuleWithEmail(VM_ID, false);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(null);

        manager.evaluateRules();

        verify(mailSender, never()).sendMail(any());
    }

    @Test
    public void testEmailSkippedWhenNoRecipientsConfigured() throws Exception {
        // mailSender injected but no recipients → should not attempt to send
        injectMailSender(/* no recipients */);

        ResourceAlertRuleVO rule = vmCpuRuleWithEmail(VM_ID, true);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(null);

        manager.evaluateRules();

        verify(mailSender, never()).sendMail(any());
    }

    @Test
    public void testSubjectContainsKeyAlertFields() throws Exception {
        injectMailSender("admin@example.com");

        ResourceAlertRuleVO rule = vmCpuRuleWithEmail(VM_ID, true);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(null);

        manager.evaluateRules();

        verify(mailSender).sendMail(mailCaptor.capture());
        String subject = mailCaptor.getValue().getSubject();
        assertTrue(subject.contains("HIGH"));
        assertTrue(subject.contains("CPU_UTILIZATION"));
        assertTrue(subject.contains("GT"));
        assertTrue(subject.contains("VirtualMachine"));
    }

    @Test
    public void testGetDataCenterIdUsesVmDao() {
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(null);

        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getDataCenterId()).thenReturn(42L);
        when(userVmDao.findById(VM_ID)).thenReturn(vm);

        manager.evaluateRules();

        verify(manager).publishAlertEvent(eq(42L), anyString(), anyString());
    }

    @Test
    public void testGenericRuleSkipsOptedOutVm() {
        ResourceAlertRuleVO rule = vmCpuRule(null);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getId()).thenReturn(VM_ID);
        when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        when(userVmDao.listByAccountId(1L)).thenReturn(Collections.singletonList(vm));

        ResourceTag optOutTag = mock(ResourceTag.class);
        when(optOutTag.getValue()).thenReturn("true");
        when(resourceTagDao.findByKey(VM_ID, ResourceTag.ResourceObjectType.UserVm, "resource.alert.opt.out"))
                .thenReturn(optOutTag);

        manager.evaluateRules();

        verify(alertDao, never()).persist(any());
    }

    @Test
    public void testGenericRuleDoesNotSkipVmWithOptOutTagValueFalse() {
        ResourceAlertRuleVO rule = vmCpuRule(null);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getId()).thenReturn(VM_ID);
        when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        when(userVmDao.listByAccountId(1L)).thenReturn(Collections.singletonList(vm));

        ResourceTag tag = mock(ResourceTag.class);
        when(tag.getValue()).thenReturn("false");
        when(resourceTagDao.findByKey(VM_ID, ResourceTag.ResourceObjectType.UserVm, "resource.alert.opt.out"))
                .thenReturn(tag);
        when(ruleDao.existsSpecificRule(ResourceAlertRule.ResourceType.VirtualMachine, "CPU_UTILIZATION", VM_ID))
                .thenReturn(false);

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(null);

        manager.evaluateRules();

        verify(alertDao).persist(any());
    }

    @Test
    public void testGenericRuleSkipsVmWithSpecificRuleForSameMetric() {
        ResourceAlertRuleVO rule = vmCpuRule(null);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getId()).thenReturn(VM_ID);
        when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        when(userVmDao.listByAccountId(1L)).thenReturn(Collections.singletonList(vm));

        when(resourceTagDao.findByKey(VM_ID, ResourceTag.ResourceObjectType.UserVm, "resource.alert.opt.out"))
                .thenReturn(null);
        when(ruleDao.existsSpecificRule(ResourceAlertRule.ResourceType.VirtualMachine, "CPU_UTILIZATION", VM_ID))
                .thenReturn(true);

        manager.evaluateRules();

        verify(alertDao, never()).persist(any());
    }

    @Test
    public void testSpecificRuleIgnoresOptOutAndPrecedenceChecks() {
        // specific rule (non-null resourceId) must not check opt-out or precedence
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(null);

        manager.evaluateRules();

        verify(alertDao).persist(any());
        verify(resourceTagDao, never()).findByKey(anyLong(), any(), anyString());
        verify(ruleDao, never()).existsSpecificRule(any(), anyString(), anyLong());
    }

    @Test
    public void testGetDataCenterIdFallsBackToZeroWhenVmNotFound() {
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(null);
        when(userVmDao.findById(VM_ID)).thenReturn(null);

        manager.evaluateRules();

        verify(manager).publishAlertEvent(eq(0L), anyString(), anyString());
    }

    private void stubFiringVmCpuRule(ResourceAlertRuleVO rule) {
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));
        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);
    }

    @Test
    public void testFiredAlertIsDeliveredToMappedWebhooks() {
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        stubFiringVmCpuRule(rule);
        when(ruleWebhookDao.listWebhookIdsByRule(rule.getId())).thenReturn(Arrays.asList(11L, 12L));
        doReturn(webhookHelper).when(manager).getWebhookHelper();
        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getUuid()).thenReturn("vm-uuid");
        when(userVmDao.findByIdIncludingRemoved(VM_ID)).thenReturn(vm);

        manager.evaluateRules();

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(webhookHelper).deliverToWebhooks(eq(Arrays.asList(11L, 12L)), eq(1L),
                eq(ResourceAlertManagerImpl.ALERT_EVENT_TYPE), payloadCaptor.capture());
        JsonObject payload = JsonParser.parseString(payloadCaptor.getValue()).getAsJsonObject();
        assertEquals(rule.getUuid(), payload.get("ruleid").getAsString());
        assertEquals("VirtualMachine", payload.get("resourcetype").getAsString());
        assertEquals("vm-uuid", payload.get("resourceid").getAsString());
        assertEquals("CPU_UTILIZATION", payload.get("metric").getAsString());
        assertEquals(85.0, payload.get("value").getAsDouble(), 0.001);
        assertEquals("HIGH", payload.get("severity").getAsString());
    }

    @Test
    public void testFiredAlertWithoutMappedWebhooksSkipsDelivery() {
        stubFiringVmCpuRule(vmCpuRule(VM_ID));

        manager.evaluateRules();

        verify(alertDao).persist(any());
        verify(manager, never()).getWebhookHelper();
    }

    private ResourceAlertRuleVO hostRule(String metric, double threshold) {
        return new ResourceAlertRuleVO("host", ResourceAlertRule.ResourceType.Host,
                HOST_ID, 1L, 1L, metric, AlertCondition.GT, threshold,
                AlertSeverity.HIGH, null, false, 600);
    }

    @Test
    public void testHostLoadAverageRuleFires() {
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(hostRule("LOAD_AVERAGE", 4.0)));
        HostStats stats = mock(HostStats.class);
        when(stats.getLoadAverage()).thenReturn(6.5);
        when(statsCollector.getHostStats(HOST_ID)).thenReturn(stats);

        manager.evaluateRules();

        verify(alertDao).persist(alertCaptor.capture());
        assertEquals(6.5, alertCaptor.getValue().getMetricValue(), 0.001);
    }

    @Test
    public void testHostNetworkReadRuleUsesHostStats() {
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(hostRule("NETWORK_READ_KBPS", 1000.0)));
        HostStats stats = mock(HostStats.class);
        when(stats.getNetworkReadKBs()).thenReturn(2500.0);
        when(statsCollector.getHostStats(HOST_ID)).thenReturn(stats);

        manager.evaluateRules();

        verify(alertDao).persist(alertCaptor.capture());
        assertEquals(2500.0, alertCaptor.getValue().getMetricValue(), 0.001);
        verify(statsCollector, never()).getVmStats(anyLong(), any(Boolean.class));
    }

    @Test
    public void testHostNetworkWriteRuleDoesNotFireBelowThreshold() {
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(hostRule("NETWORK_WRITE_KBPS", 1000.0)));
        HostStats stats = mock(HostStats.class);
        when(stats.getNetworkWriteKBs()).thenReturn(10.0);
        when(statsCollector.getHostStats(HOST_ID)).thenReturn(stats);

        manager.evaluateRules();

        verify(alertDao, never()).persist(any());
    }

    private static final long VOLUME_ID = 401L;

    private ResourceAlertRuleVO volumeSizeRule(double thresholdGb) {
        return new ResourceAlertRuleVO("vol", ResourceAlertRule.ResourceType.Volume,
                VOLUME_ID, 1L, 1L, "VOLUME_SIZE_GB", AlertCondition.GT, thresholdGb,
                AlertSeverity.MEDIUM, null, false, 600);
    }

    @Test
    public void testVolumeSizeRuleUsesPhysicalSizeByPath() {
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(volumeSizeRule(10.0)));
        VolumeVO vol = mock(VolumeVO.class);
        when(vol.getFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        when(vol.getPath()).thenReturn("vol-path");
        when(volumeDao.findById(VOLUME_ID)).thenReturn(vol);
        VolumeStats stats = mock(VolumeStats.class);
        when(stats.getPhysicalSize()).thenReturn(20L * 1024 * 1024 * 1024);
        when(statsCollector.getVolumeStats("vol-path")).thenReturn(stats);

        manager.evaluateRules();

        verify(alertDao).persist(alertCaptor.capture());
        assertEquals(20.0, alertCaptor.getValue().getMetricValue(), 0.001);
    }

    @Test
    public void testVolumeSizeRuleUsesChainInfoForOva() {
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(volumeSizeRule(10.0)));
        VolumeVO vol = mock(VolumeVO.class);
        when(vol.getFormat()).thenReturn(Storage.ImageFormat.OVA);
        when(vol.getChainInfo()).thenReturn("chain-info");
        when(volumeDao.findById(VOLUME_ID)).thenReturn(vol);

        manager.evaluateRules();

        verify(statsCollector).getVolumeStats("chain-info");
        verify(alertDao, never()).persist(any());
    }

    private ResourceAlertRuleVO poolIopsRule(double threshold) {
        return new ResourceAlertRuleVO("pool-iops", ResourceAlertRule.ResourceType.StoragePool,
                POOL_ID, 1L, 1L, "STORAGE_USED_IOPS", AlertCondition.GT, threshold,
                AlertSeverity.HIGH, null, false, 600);
    }

    @Test
    public void testStoragePoolIopsRuleFires() {
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(poolIopsRule(1000.0)));
        StorageStats stats = mock(StorageStats.class);
        when(stats.getUsedIops()).thenReturn(5000L);
        when(statsCollector.getStoragePoolStats(POOL_ID)).thenReturn(stats);

        manager.evaluateRules();

        verify(alertDao).persist(alertCaptor.capture());
        assertEquals(5000.0, alertCaptor.getValue().getMetricValue(), 0.001);
    }

    @Test
    public void testStoragePoolIopsRuleSkippedWhenDriverDoesNotReportIops() {
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(poolIopsRule(1000.0)));
        StorageStats stats = mock(StorageStats.class);
        when(stats.getUsedIops()).thenReturn(null);
        when(statsCollector.getStoragePoolStats(POOL_ID)).thenReturn(stats);

        manager.evaluateRules();

        verify(alertDao, never()).persist(any());
    }
}
