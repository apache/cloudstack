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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ResourceAlertMetricTest {

    @Test
    public void testCpuAppliesToVmAndHost() {
        assertTrue(ResourceAlertMetric.CPU_UTILIZATION.appliesTo(ResourceAlertRule.ResourceType.VirtualMachine));
        assertTrue(ResourceAlertMetric.CPU_UTILIZATION.appliesTo(ResourceAlertRule.ResourceType.Host));
        assertFalse(ResourceAlertMetric.CPU_UTILIZATION.appliesTo(ResourceAlertRule.ResourceType.StoragePool));
        assertFalse(ResourceAlertMetric.CPU_UTILIZATION.appliesTo(ResourceAlertRule.ResourceType.Volume));
    }

    @Test
    public void testMemoryAppliesToVmAndHost() {
        assertTrue(ResourceAlertMetric.MEMORY_UTILIZATION.appliesTo(ResourceAlertRule.ResourceType.VirtualMachine));
        assertTrue(ResourceAlertMetric.MEMORY_UTILIZATION.appliesTo(ResourceAlertRule.ResourceType.Host));
        assertFalse(ResourceAlertMetric.MEMORY_UTILIZATION.appliesTo(ResourceAlertRule.ResourceType.StoragePool));
        assertFalse(ResourceAlertMetric.MEMORY_UTILIZATION.appliesTo(ResourceAlertRule.ResourceType.Volume));
    }

    @Test
    public void testDiskMetricsApplyToVmAndVolume() {
        for (ResourceAlertMetric m : new ResourceAlertMetric[]{
                ResourceAlertMetric.DISK_READ_IOPS, ResourceAlertMetric.DISK_WRITE_IOPS,
                ResourceAlertMetric.DISK_READ_KBPS, ResourceAlertMetric.DISK_WRITE_KBPS}) {
            assertTrue(m.name(), m.appliesTo(ResourceAlertRule.ResourceType.VirtualMachine));
            assertTrue(m.name(), m.appliesTo(ResourceAlertRule.ResourceType.Volume));
            assertFalse(m.name(), m.appliesTo(ResourceAlertRule.ResourceType.Host));
            assertFalse(m.name(), m.appliesTo(ResourceAlertRule.ResourceType.StoragePool));
        }
    }

    @Test
    public void testNetworkMetricsApplyToVmAndHost() {
        for (ResourceAlertMetric m : new ResourceAlertMetric[]{
                ResourceAlertMetric.NETWORK_READ_KBPS, ResourceAlertMetric.NETWORK_WRITE_KBPS}) {
            assertTrue(m.name(), m.appliesTo(ResourceAlertRule.ResourceType.VirtualMachine));
            assertTrue(m.name(), m.appliesTo(ResourceAlertRule.ResourceType.Host));
            assertFalse(m.name(), m.appliesTo(ResourceAlertRule.ResourceType.StoragePool));
            assertFalse(m.name(), m.appliesTo(ResourceAlertRule.ResourceType.Volume));
        }
    }

    @Test
    public void testStorageUtilizationAppliesToStoragePoolOnly() {
        assertTrue(ResourceAlertMetric.STORAGE_UTILIZATION.appliesTo(ResourceAlertRule.ResourceType.StoragePool));
        assertFalse(ResourceAlertMetric.STORAGE_UTILIZATION.appliesTo(ResourceAlertRule.ResourceType.VirtualMachine));
        assertFalse(ResourceAlertMetric.STORAGE_UTILIZATION.appliesTo(ResourceAlertRule.ResourceType.Host));
        assertFalse(ResourceAlertMetric.STORAGE_UTILIZATION.appliesTo(ResourceAlertRule.ResourceType.Volume));
    }

    @Test
    public void testLoadAverageAppliesToHostOnly() {
        assertTrue(ResourceAlertMetric.LOAD_AVERAGE.appliesTo(ResourceAlertRule.ResourceType.Host));
        assertFalse(ResourceAlertMetric.LOAD_AVERAGE.appliesTo(ResourceAlertRule.ResourceType.VirtualMachine));
        assertFalse(ResourceAlertMetric.LOAD_AVERAGE.appliesTo(ResourceAlertRule.ResourceType.Volume));
        assertFalse(ResourceAlertMetric.LOAD_AVERAGE.appliesTo(ResourceAlertRule.ResourceType.StoragePool));
    }

    @Test
    public void testVolumeSizeAppliesToVolumeOnly() {
        assertTrue(ResourceAlertMetric.VOLUME_SIZE_GB.appliesTo(ResourceAlertRule.ResourceType.Volume));
        assertFalse(ResourceAlertMetric.VOLUME_SIZE_GB.appliesTo(ResourceAlertRule.ResourceType.VirtualMachine));
        assertFalse(ResourceAlertMetric.VOLUME_SIZE_GB.appliesTo(ResourceAlertRule.ResourceType.Host));
        assertFalse(ResourceAlertMetric.VOLUME_SIZE_GB.appliesTo(ResourceAlertRule.ResourceType.StoragePool));
    }

    @Test
    public void testStorageUsedIopsAppliesToStoragePoolOnly() {
        assertTrue(ResourceAlertMetric.STORAGE_USED_IOPS.appliesTo(ResourceAlertRule.ResourceType.StoragePool));
        assertFalse(ResourceAlertMetric.STORAGE_USED_IOPS.appliesTo(ResourceAlertRule.ResourceType.Volume));
        assertFalse(ResourceAlertMetric.STORAGE_USED_IOPS.appliesTo(ResourceAlertRule.ResourceType.Host));
        assertFalse(ResourceAlertMetric.STORAGE_USED_IOPS.appliesTo(ResourceAlertRule.ResourceType.VirtualMachine));
    }

    @Test
    public void testPercentageMetrics() {
        assertTrue(ResourceAlertMetric.CPU_UTILIZATION.isPercentage());
        assertTrue(ResourceAlertMetric.MEMORY_UTILIZATION.isPercentage());
        assertTrue(ResourceAlertMetric.STORAGE_UTILIZATION.isPercentage());
        assertFalse(ResourceAlertMetric.DISK_READ_IOPS.isPercentage());
        assertFalse(ResourceAlertMetric.VOLUME_SIZE_GB.isPercentage());
    }
}
