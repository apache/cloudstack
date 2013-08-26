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
package com.cloud.alert;

import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.capacity.CapacityVO;
import com.cloud.utils.component.Manager;

public interface AlertManager extends Manager {
    public static final short ALERT_TYPE_MEMORY = CapacityVO.CAPACITY_TYPE_MEMORY;
    public static final short ALERT_TYPE_CPU = CapacityVO.CAPACITY_TYPE_CPU;
    public static final short ALERT_TYPE_STORAGE = CapacityVO.CAPACITY_TYPE_STORAGE;
    public static final short ALERT_TYPE_STORAGE_ALLOCATED = CapacityVO.CAPACITY_TYPE_STORAGE_ALLOCATED;
    public static final short ALERT_TYPE_VIRTUAL_NETWORK_PUBLIC_IP = CapacityVO.CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP;
    public static final short ALERT_TYPE_PRIVATE_IP = CapacityVO.CAPACITY_TYPE_PRIVATE_IP;
    public static final short ALERT_TYPE_SECONDARY_STORAGE = CapacityVO.CAPACITY_TYPE_SECONDARY_STORAGE;
    public static final short ALERT_TYPE_HOST = 7;
    public static final short ALERT_TYPE_USERVM = 8;
    public static final short ALERT_TYPE_DOMAIN_ROUTER = 9;
    public static final short ALERT_TYPE_CONSOLE_PROXY = 10;
    public static final short ALERT_TYPE_ROUTING = 11; // lost connection to default route (to the gateway)
    public static final short ALERT_TYPE_STORAGE_MISC = 12; // lost connection to default route (to the gateway)
    public static final short ALERT_TYPE_USAGE_SERVER = 13; // lost connection to default route (to the gateway)
    public static final short ALERT_TYPE_MANAGMENT_NODE = 14; // lost connection to default route (to the gateway)
    public static final short ALERT_TYPE_DOMAIN_ROUTER_MIGRATE = 15;
    public static final short ALERT_TYPE_CONSOLE_PROXY_MIGRATE = 16;
    public static final short ALERT_TYPE_USERVM_MIGRATE = 17;
    public static final short ALERT_TYPE_VLAN = 18;
    public static final short ALERT_TYPE_SSVM = 19;
    public static final short ALERT_TYPE_USAGE_SERVER_RESULT = 20; // Usage job result
    public static final short ALERT_TYPE_STORAGE_DELETE = 21;
    public static final short ALERT_TYPE_UPDATE_RESOURCE_COUNT = 22; // Generated when we fail to update the resource
    // count
    public static final short ALERT_TYPE_USAGE_SANITY_RESULT = 23;
    public static final short ALERT_TYPE_DIRECT_ATTACHED_PUBLIC_IP = 24;
    public static final short ALERT_TYPE_LOCAL_STORAGE = 25;
    public static final short ALERT_TYPE_RESOURCE_LIMIT_EXCEEDED = 26; // Generated when the resource limit exceeds the limit. Currently used for recurring snapshots only
    
    static final ConfigKey<Double> StorageCapacityThreshold = new ConfigKey<Double>(Double.class, "cluster.storage.capacity.notificationthreshold", "Alert", "0.75",
        "Percentage (as a value between 0 and 1) of storage utilization above which alerts will be sent about low storage available.", true, ConfigKey.Scope.Cluster, null);
    static final ConfigKey<Double> CPUCapacityThreshold = new ConfigKey<Double>(Double.class, "cluster.cpu.allocated.capacity.notificationthreshold", "Alert", "0.75",
        "Percentage (as a value between 0 and 1) of cpu utilization above which alerts will be sent about low cpu available.", true, ConfigKey.Scope.Cluster, null);
    static final ConfigKey<Double> MemoryCapacityThreshold = new ConfigKey<Double>(Double.class, "cluster.memory.allocated.capacity.notificationthreshold", "Alert", "0.75",
        "Percentage (as a value between 0 and 1) of memory utilization above which alerts will be sent about low memory available.", true, ConfigKey.Scope.Cluster, null);
    static final ConfigKey<Double> StorageAllocatedCapacityThreshold = new ConfigKey<Double>(Double.class, "cluster.storage.allocated.capacity.notificationthreshold", "Alert",
        "0.75", "Percentage (as a value between 0 and 1) of allocated storage utilization above which alerts will be sent about low storage available.", true,
        ConfigKey.Scope.Cluster, null);
    
    void clearAlert(short alertType, long dataCenterId, long podId);

    void sendAlert(short alertType, long dataCenterId, Long podId, String subject, String body);

    void recalculateCapacity();
}
