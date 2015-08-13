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
package org.apache.cloudstack.quota.constant;

import org.apache.cloudstack.usage.UsageTypes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class QuotaTypes extends UsageTypes {
    public static final int CPU_CLOCK_RATE = 15;
    public static final int CPU_NUMBER = 16;
    public static final int MEMORY = 17;

    private final Integer quotaType;
    private final String quotaName;
    private final String quotaUnit;
    private final String description;
    private final String discriminator;
    private final static Map<Integer, QuotaTypes> quotaTypeMap;

    static {
        final HashMap<Integer, QuotaTypes> quotaTypeList = new HashMap<Integer, QuotaTypes>();
        quotaTypeList.put(RUNNING_VM, new QuotaTypes(RUNNING_VM, "RUNNING_VM", "Compute-Month", "Running Vm Usage"));
        quotaTypeList.put(ALLOCATED_VM, new QuotaTypes(ALLOCATED_VM, "ALLOCATED_VM", "Compute-Month", "Allocated Vm Usage"));
        quotaTypeList.put(IP_ADDRESS, new QuotaTypes(IP_ADDRESS, "IP_ADDRESS", "IP-Month", "IP Address Usage"));
        quotaTypeList.put(NETWORK_BYTES_SENT, new QuotaTypes(NETWORK_BYTES_SENT, "NETWORK_BYTES_SENT", "GB", "Network Usage (Bytes Sent)"));
        quotaTypeList.put(NETWORK_BYTES_RECEIVED, new QuotaTypes(NETWORK_BYTES_RECEIVED, "NETWORK_BYTES_RECEIVED", "GB", "Network Usage (Bytes Received)"));
        quotaTypeList.put(VOLUME, new QuotaTypes(VOLUME, "VOLUME", "GB-Month", "Volume Usage"));
        quotaTypeList.put(TEMPLATE, new QuotaTypes(TEMPLATE, "TEMPLATE", "GB-Month", "Template Usage"));
        quotaTypeList.put(ISO, new QuotaTypes(ISO, "ISO", "GB-Month", "ISO Usage"));
        quotaTypeList.put(SNAPSHOT, new QuotaTypes(SNAPSHOT, "SNAPSHOT", "GB-Month", "Snapshot Usage"));
        quotaTypeList.put(SECURITY_GROUP, new QuotaTypes(SECURITY_GROUP, "SECURITY_GROUP", "Policy-Month", "Security Group Usage"));
        quotaTypeList.put(LOAD_BALANCER_POLICY, new QuotaTypes(LOAD_BALANCER_POLICY, "LOAD_BALANCER_POLICY", "Policy-Month", "Load Balancer Usage"));
        quotaTypeList.put(PORT_FORWARDING_RULE, new QuotaTypes(PORT_FORWARDING_RULE, "PORT_FORWARDING_RULE", "Policy-Month", "Port Forwarding Usage"));
        quotaTypeList.put(NETWORK_OFFERING, new QuotaTypes(NETWORK_OFFERING, "NETWORK_OFFERING", "Policy-Month", "Network Offering Usage"));
        quotaTypeList.put(VPN_USERS, new QuotaTypes(VPN_USERS, "VPN_USERS", "Policy-Month", "VPN users usage"));
        quotaTypeList.put(VM_DISK_IO_READ, new QuotaTypes(VM_DISK_IO_READ, "VM_DISK_IO_READ", "GB", "VM Disk usage(I/O Read)"));
        quotaTypeList.put(VM_DISK_IO_WRITE, new QuotaTypes(VM_DISK_IO_WRITE, "VM_DISK_IO_WRITE", "GB", "VM Disk usage(I/O Write)"));
        quotaTypeList.put(VM_DISK_BYTES_READ, new QuotaTypes(VM_DISK_BYTES_READ, "VM_DISK_BYTES_READ", "GB", "VM Disk usage(Bytes Read)"));
        quotaTypeList.put(VM_DISK_BYTES_WRITE, new QuotaTypes(VM_DISK_BYTES_WRITE, "VPN_USERS", "GB", "VM Disk usage(Bytes Write)"));
        quotaTypeList.put(VM_SNAPSHOT, new QuotaTypes(VM_SNAPSHOT, "VM_SNAPSHOT", "GB-Month", "VM Snapshot storage usage"));
        quotaTypeList.put(CPU_CLOCK_RATE, new QuotaTypes(CPU_CLOCK_RATE, "CPU_CLOCK_RATE", "Compute-Month", "Quota tariff for using 1 CPU of clock rate 100MHz"));
        quotaTypeList.put(CPU_NUMBER, new QuotaTypes(CPU_NUMBER, "CPU_NUMBER", "Compute-Month", "Quota tariff for running VM that has 1vCPU"));
        quotaTypeList.put(MEMORY, new QuotaTypes(MEMORY, "MEMORY", "Compute-Month", "Quota tariff for using 1MB or RAM for 1 hour"));
        quotaTypeMap = Collections.unmodifiableMap(quotaTypeList);
    }

    private QuotaTypes(Integer quotaType, String name, String unit, String description) {
        this.quotaType = quotaType;
        this.description = description;
        this.quotaName = name;
        this.quotaUnit = unit;
        this.discriminator = "None";
    }

    public static Map<Integer, QuotaTypes> listQuotaTypes() {
        return quotaTypeMap;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public String getQuotaName() {
        return quotaName;
    }

    public String getQuotaUnit() {
        return quotaUnit;
    }

    public String getDescription() {
        return description;
    }

    public Integer getQuotaType() {
        return quotaType;
    }

    static public String getDescription(int quotaType) {
        QuotaTypes t = quotaTypeMap.get(quotaType);
        if (t != null) {
            return t.getDescription();
        }
        return null;
    }
}
