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

import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.utils.component.Manager;

public interface AlertManager extends Manager, AlertService {

    static final ConfigKey<Double> StorageCapacityThreshold = new ConfigKey<Double>(Double.class, "cluster.storage.capacity.notificationthreshold", "Alert", "0.75",
        "Percentage (as a value between 0 and 1) of storage utilization above which alerts will be sent about low storage available.", true, ConfigKey.Scope.Cluster,
        null);
    static final ConfigKey<Double> CPUCapacityThreshold = new ConfigKey<Double>(Double.class, "cluster.cpu.allocated.capacity.notificationthreshold", "Alert", "0.75",
        "Percentage (as a value between 0 and 1) of cpu utilization above which alerts will be sent about low cpu available.", true, ConfigKey.Scope.Cluster, null);
    static final ConfigKey<Double> MemoryCapacityThreshold = new ConfigKey<Double>(Double.class, "cluster.memory.allocated.capacity.notificationthreshold", "Alert",
        "0.75", "Percentage (as a value between 0 and 1) of memory utilization above which alerts will be sent about low memory available.", true,
        ConfigKey.Scope.Cluster, null);
    static final ConfigKey<Double> StorageAllocatedCapacityThreshold = new ConfigKey<Double>(Double.class, "cluster.storage.allocated.capacity.notificationthreshold",
        "Alert", "0.75", "Percentage (as a value between 0 and 1) of allocated storage utilization above which alerts will be sent about low storage available.", true,
        ConfigKey.Scope.Cluster, null);

    public static final ConfigKey<Boolean> AlertSmtpUseStartTLS = new ConfigKey<Boolean>("Advanced", Boolean.class, "alert.smtp.useStartTLS", "false",
            "If set to true and if we enable security via alert.smtp.useAuth, this will enable StartTLS to secure the connection.", true);

    public static final ConfigKey<String> AlertSmtpEnabledSecurityProtocols = new ConfigKey<String>("Advanced", String.class, "alert.smtp.enabledSecurityProtocols", "",
            "White-space separated security protocols; ex: \"TLSv1 TLSv1.1\". Supported protocols: SSLv2Hello, SSLv3, TLSv1, TLSv1.1 and TLSv1.2", true);

    public static final ConfigKey<Double> Ipv6SubnetCapacityThreshold = new ConfigKey<Double>("Advanced", Double.class,
            "zone.virtualnetwork.ipv6subnet.capacity.notificationthreshold",
            "0.75",
            "Percentage (as a value between 0 and 1) of guest network IPv6 subnet utilization above which alerts will be sent.",
            true);

    void clearAlert(AlertType alertType, long dataCenterId, long podId);

    void recalculateCapacity();

    void sendAlert(AlertType alertType, long dataCenterId, Long podId, String subject, String body);

}
