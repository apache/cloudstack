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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.apache.cloudstack.resourcealert.dao.ResourceAlertDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleDao;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertRuleVO;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.server.StatsCollector;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmStats;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.UserVmVO;

@RunWith(MockitoJUnitRunner.class)
public class ResourceAlertManagerImplTest {

    @InjectMocks
    ResourceAlertManagerImpl manager;

    @Mock ResourceAlertRuleDao ruleDao;
    @Mock ResourceAlertDao alertDao;
    @Mock UserVmDao userVmDao;
    @Mock HostDao hostDao;
    @Mock PrimaryDataStoreDao storagePoolDao;
    @Mock VolumeDao volumeDao;
    @Mock StatsCollector statsCollector;

    @Captor ArgumentCaptor<ResourceAlertVO> alertCaptor;

    private static final long VM_ID = 101L;
    private static final long HOST_ID = 201L;
    private static final long POOL_ID = 301L;

    private ResourceAlertRuleVO vmCpuRule(Long resourceId) {
        return new ResourceAlertRuleVO("test", ResourceAlertRule.ResourceType.VirtualMachine,
                resourceId, 1L, 1L, "CPU_UTILIZATION", AlertCondition.GT, 80.0,
                AlertSeverity.HIGH, "CPU high", false, 600);
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

        verify(alertDao, never()).persist(alertCaptor.capture());
    }

    @Test
    public void testRuleDoesNotFireWithinResetInterval() {
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);

        // last alert fired 10 seconds ago; reset interval is 600
        ResourceAlertVO recentAlert = mock(ResourceAlertVO.class);
        when(recentAlert.getAlertTimestamp()).thenReturn(
                new Date(System.currentTimeMillis() - 10_000L));
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(recentAlert);

        manager.evaluateRules();

        verify(alertDao, never()).persist(alertCaptor.capture());
    }

    @Test
    public void testRuleFiresAfterResetIntervalExpires() {
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getCPUUtilization()).thenReturn(85.0);
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);

        // last alert fired 700 seconds ago; reset interval is 600 → should fire again
        ResourceAlertVO oldAlert = mock(ResourceAlertVO.class);
        when(oldAlert.getAlertTimestamp()).thenReturn(
                new Date(System.currentTimeMillis() - 700_000L));
        when(alertDao.findLastFiredForRule(anyLong(), eq(VM_ID))).thenReturn(oldAlert);

        manager.evaluateRules();

        verify(alertDao).persist(alertCaptor.capture());
    }

    @Test
    public void testNullStatsSkipped() {
        ResourceAlertRuleVO rule = vmCpuRule(VM_ID);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(null);

        manager.evaluateRules();

        verify(alertDao, never()).persist(alertCaptor.capture());
    }

    @Test
    public void testVmMemorySkippedWhenNoBalloonDriver() {
        ResourceAlertRuleVO rule = new ResourceAlertRuleVO("test",
                ResourceAlertRule.ResourceType.VirtualMachine, VM_ID, 1L, 1L,
                "MEMORY_UTILIZATION", AlertCondition.GT, 50.0,
                AlertSeverity.MEDIUM, null, false, 600);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        VmStats stats = mock(VmStats.class);
        when(stats.getMemoryKBs()).thenReturn(8192.0);
        when(stats.getIntFreeMemoryKBs()).thenReturn(-1.0); // no balloon driver
        when(statsCollector.getVmStats(VM_ID, false)).thenReturn(stats);

        manager.evaluateRules();

        verify(alertDao, never()).persist(alertCaptor.capture());
    }

    @Test
    public void testVmMemoryUtilizationCalculation() {
        ResourceAlertRuleVO rule = new ResourceAlertRuleVO("test",
                ResourceAlertRule.ResourceType.VirtualMachine, VM_ID, 1L, 1L,
                "MEMORY_UTILIZATION", AlertCondition.GT, 70.0,
                AlertSeverity.HIGH, null, false, 600);
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
                "STORAGE_UTILIZATION", AlertCondition.GT, 65.0,
                AlertSeverity.HIGH, null, false, 600);
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
        // null resourceId → fan-out to all running VMs in the account
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

        verify(alertDao, times(2)).persist(alertCaptor.capture());
    }

    @Test
    public void testGenericVmRuleSkipsStoppedVms() {
        ResourceAlertRuleVO rule = vmCpuRule(null);
        when(ruleDao.listActive()).thenReturn(Collections.singletonList(rule));

        UserVmVO stopped = mock(UserVmVO.class);
        when(stopped.getState()).thenReturn(VirtualMachine.State.Stopped);

        when(userVmDao.listByAccountId(1L)).thenReturn(Collections.singletonList(stopped));

        manager.evaluateRules();

        verify(alertDao, never()).persist(alertCaptor.capture());
    }

    @Test
    public void testHostCpuRuleUsesHostStats() {
        ResourceAlertRuleVO rule = new ResourceAlertRuleVO("test",
                ResourceAlertRule.ResourceType.Host, HOST_ID, 1L, 1L,
                "CPU_UTILIZATION", AlertCondition.GT, 85.0,
                AlertSeverity.CRITICAL, null, false, 600);
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
                "MEMORY_UTILIZATION", AlertCondition.GT, 80.0,
                AlertSeverity.HIGH, null, false, 600);
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
}
