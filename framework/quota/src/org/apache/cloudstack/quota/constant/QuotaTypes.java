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

import java.util.HashMap;

public class QuotaTypes extends UsageTypes {
    public static final int CPU_CLOCK_RATE = 15;
    public static final int CPU_NUMBER = 16;
    public static final int MEMORY = 17;

    private final Integer quotaType;
    private final String quotaName;
    private final String quotaUnit;
    private final String description;
    private final String discriminator;
    private static final HashMap<Integer, QuotaTypes> quotaTypeList = new HashMap<Integer, QuotaTypes>();

    static {
        quotaTypeList.put(Integer.valueOf(RUNNING_VM), new QuotaTypes(Integer.valueOf(RUNNING_VM), "RUNNING_VM", "Compute-Month", "Running Vm Usage"));
        quotaTypeList.put(Integer.valueOf(ALLOCATED_VM), new QuotaTypes(Integer.valueOf(ALLOCATED_VM), "ALLOCATED_VM", "Compute-Month", "Allocated Vm Usage"));
        quotaTypeList.put(Integer.valueOf(IP_ADDRESS), new QuotaTypes(Integer.valueOf(IP_ADDRESS), "IP_ADDRESS", "IP-Month", "IP Address Usage"));
        quotaTypeList.put(Integer.valueOf(NETWORK_BYTES_SENT), new QuotaTypes(Integer.valueOf(NETWORK_BYTES_SENT), "NETWORK_BYTES_SENT", "GB", "Network Usage (Bytes Sent)"));
        quotaTypeList.put(Integer.valueOf(NETWORK_BYTES_RECEIVED), new QuotaTypes(Integer.valueOf(NETWORK_BYTES_RECEIVED), "NETWORK_BYTES_RECEIVED", "GB", "Network Usage (Bytes Received)"));
        quotaTypeList.put(Integer.valueOf(VOLUME), new QuotaTypes(Integer.valueOf(VOLUME), "VOLUME", "GB-Month", "Volume Usage"));
        quotaTypeList.put(Integer.valueOf(TEMPLATE), new QuotaTypes(Integer.valueOf(TEMPLATE), "TEMPLATE", "GB-Month", "Template Usage"));
        quotaTypeList.put(Integer.valueOf(ISO), new QuotaTypes(Integer.valueOf(ISO), "ISO", "GB-Month", "ISO Usage"));
        quotaTypeList.put(Integer.valueOf(SNAPSHOT), new QuotaTypes(Integer.valueOf(SNAPSHOT), "SNAPSHOT", "GB-Month", "Snapshot Usage"));
        quotaTypeList.put(Integer.valueOf(SECURITY_GROUP), new QuotaTypes(Integer.valueOf(SECURITY_GROUP), "SECURITY_GROUP", "Policy-Month", "Security Group Usage"));
        quotaTypeList.put(Integer.valueOf(LOAD_BALANCER_POLICY), new QuotaTypes(Integer.valueOf(LOAD_BALANCER_POLICY), "LOAD_BALANCER_POLICY", "Policy-Month", "Load Balancer Usage"));
        quotaTypeList.put(Integer.valueOf(PORT_FORWARDING_RULE), new QuotaTypes(Integer.valueOf(PORT_FORWARDING_RULE), "PORT_FORWARDING_RULE", "Policy-Month", "Port Forwarding Usage"));
        quotaTypeList.put(Integer.valueOf(NETWORK_OFFERING), new QuotaTypes(Integer.valueOf(NETWORK_OFFERING), "NETWORK_OFFERING", "Policy-Month", "Network Offering Usage"));
        quotaTypeList.put(Integer.valueOf(VPN_USERS), new QuotaTypes(Integer.valueOf(VPN_USERS), "VPN_USERS", "Policy-Month", "VPN users usage"));
        quotaTypeList.put(Integer.valueOf(VM_DISK_IO_READ), new QuotaTypes(Integer.valueOf(VM_DISK_IO_READ), "VM_DISK_IO_READ", "GB", "VM Disk usage(I/O Read)"));
        quotaTypeList.put(Integer.valueOf(VM_DISK_IO_WRITE), new QuotaTypes(Integer.valueOf(VM_DISK_IO_WRITE), "VM_DISK_IO_WRITE", "GB", "VM Disk usage(I/O Write)"));
        quotaTypeList.put(Integer.valueOf(VM_DISK_BYTES_READ), new QuotaTypes(Integer.valueOf(VM_DISK_BYTES_READ), "VM_DISK_BYTES_READ", "GB", "VM Disk usage(Bytes Read)"));
        quotaTypeList.put(Integer.valueOf(VM_DISK_BYTES_WRITE), new QuotaTypes(Integer.valueOf(VM_DISK_BYTES_WRITE), "VPN_USERS", "GB", "VM Disk usage(Bytes Write)"));
        quotaTypeList.put(Integer.valueOf(VM_SNAPSHOT), new QuotaTypes(Integer.valueOf(VM_SNAPSHOT), "VM_SNAPSHOT", "GB-Month", "VM Snapshot storage usage"));
        quotaTypeList.put(Integer.valueOf(CPU_CLOCK_RATE), new QuotaTypes(Integer.valueOf(CPU_CLOCK_RATE), "CPU_CLOCK_RATE", "Compute-Month", "Quota tariff for using 1 CPU of clock rate 100MHz"));
        quotaTypeList.put(Integer.valueOf(CPU_NUMBER), new QuotaTypes(Integer.valueOf(CPU_NUMBER), "CPU_NUMBER", "Compute-Month", "Quota tariff for running VM that has 1vCPU"));
        quotaTypeList.put(Integer.valueOf(MEMORY), new QuotaTypes(Integer.valueOf(MEMORY), "MEMORY", "Compute-Month", "Quota tariff for using 1MB or RAM for 1 hour"));
    }

    public QuotaTypes(Integer quotaType, String name, String unit, String description) {
        this.quotaType = quotaType;
        this.description = description;
        this.quotaName = name;
        this.quotaUnit = unit;
        this.discriminator = "None";
    }

    public static HashMap<Integer, QuotaTypes> listQuotaTypes() {
        return quotaTypeList;
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
        return listQuotaTypes().get(quotaType).getDescription();
    }
}
