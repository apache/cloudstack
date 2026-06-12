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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public enum ResourceAlertMetric {
    CPU_UTILIZATION(ResourceAlertRule.ResourceType.VirtualMachine, ResourceAlertRule.ResourceType.Host),
    MEMORY_UTILIZATION(ResourceAlertRule.ResourceType.VirtualMachine, ResourceAlertRule.ResourceType.Host),
    DISK_READ_IOPS(ResourceAlertRule.ResourceType.VirtualMachine, ResourceAlertRule.ResourceType.Volume),
    DISK_WRITE_IOPS(ResourceAlertRule.ResourceType.VirtualMachine, ResourceAlertRule.ResourceType.Volume),
    DISK_READ_KBPS(ResourceAlertRule.ResourceType.VirtualMachine, ResourceAlertRule.ResourceType.Volume),
    DISK_WRITE_KBPS(ResourceAlertRule.ResourceType.VirtualMachine, ResourceAlertRule.ResourceType.Volume),
    STORAGE_UTILIZATION(ResourceAlertRule.ResourceType.StoragePool),
    NETWORK_READ_KBPS(ResourceAlertRule.ResourceType.VirtualMachine),
    NETWORK_WRITE_KBPS(ResourceAlertRule.ResourceType.VirtualMachine);

    private final Set<ResourceAlertRule.ResourceType> applicableTypes;

    ResourceAlertMetric(ResourceAlertRule.ResourceType... types) {
        this.applicableTypes = EnumSet.copyOf(Arrays.asList(types));
    }

    public boolean appliesTo(ResourceAlertRule.ResourceType type) {
        return applicableTypes.contains(type);
    }
}
