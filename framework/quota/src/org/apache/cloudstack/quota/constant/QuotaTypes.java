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

    private Integer quotaType;
    private String quotaName;
    private String quotaUnit;
    private String description;
    private String discriminator;

    public QuotaTypes(Integer quotaType, String name, String unit, String description) {
        this.quotaType = quotaType;
        this.description = description;
        this.quotaName = name;
        this.quotaUnit = unit;
        this.discriminator = "None";
    }

    public static HashMap<Integer, QuotaTypes> listQuotaTypes() {
        final HashMap<Integer, QuotaTypes> quotaTypeList = new HashMap<Integer, QuotaTypes>();
        quotaTypeList.put(new Integer(RUNNING_VM), new QuotaTypes(new Integer(RUNNING_VM), "RUNNING_VM", "Compute-Month", "Running Vm Usage"));
        quotaTypeList.put(new Integer(ALLOCATED_VM), new QuotaTypes(new Integer(ALLOCATED_VM), "ALLOCATED_VM", "Compute-Month", "Allocated Vm Usage"));
        quotaTypeList.put(new Integer(IP_ADDRESS), new QuotaTypes(new Integer(IP_ADDRESS), "IP_ADDRESS", "IP-Month", "IP Address Usage"));
        quotaTypeList.put(new Integer(NETWORK_BYTES_SENT), new QuotaTypes(new Integer(NETWORK_BYTES_SENT), "NETWORK_BYTES_SENT", "GB", "Network Usage (Bytes Sent)"));
        quotaTypeList.put(new Integer(NETWORK_BYTES_RECEIVED), new QuotaTypes(new Integer(NETWORK_BYTES_RECEIVED), "NETWORK_BYTES_RECEIVED", "GB", "Network Usage (Bytes Received)"));
        quotaTypeList.put(new Integer(VOLUME), new QuotaTypes(new Integer(VOLUME), "VOLUME", "GB-Month", "Volume Usage"));
        quotaTypeList.put(new Integer(TEMPLATE), new QuotaTypes(new Integer(TEMPLATE), "TEMPLATE", "GB-Month", "Template Usage"));
        quotaTypeList.put(new Integer(ISO), new QuotaTypes(new Integer(ISO), "ISO", "GB-Month", "ISO Usage"));
        quotaTypeList.put(new Integer(SNAPSHOT), new QuotaTypes(new Integer(SNAPSHOT), "SNAPSHOT", "GB-Month", "Snapshot Usage"));
        quotaTypeList.put(new Integer(SECURITY_GROUP), new QuotaTypes(new Integer(SECURITY_GROUP), "SECURITY_GROUP", "Policy-Month", "Security Group Usage"));
        quotaTypeList.put(new Integer(LOAD_BALANCER_POLICY), new QuotaTypes(new Integer(LOAD_BALANCER_POLICY), "LOAD_BALANCER_POLICY", "Policy-Month", "Load Balancer Usage"));
        quotaTypeList.put(new Integer(PORT_FORWARDING_RULE), new QuotaTypes(new Integer(PORT_FORWARDING_RULE), "PORT_FORWARDING_RULE", "Policy-Month", "Port Forwarding Usage"));
        quotaTypeList.put(new Integer(NETWORK_OFFERING), new QuotaTypes(new Integer(NETWORK_OFFERING), "NETWORK_OFFERING", "Policy-Month", "Network Offering Usage"));
        quotaTypeList.put(new Integer(VPN_USERS), new QuotaTypes(new Integer(VPN_USERS), "VPN_USERS", "Policy-Month", "VPN users usage"));
        quotaTypeList.put(new Integer(VM_DISK_IO_READ), new QuotaTypes(new Integer(VM_DISK_IO_READ), "VM_DISK_IO_READ", "GB", "VM Disk usage(I/O Read)"));
        quotaTypeList.put(new Integer(VM_DISK_IO_WRITE), new QuotaTypes(new Integer(VM_DISK_IO_WRITE), "VM_DISK_IO_WRITE", "GB", "VM Disk usage(I/O Write)"));
        quotaTypeList.put(new Integer(VM_DISK_BYTES_READ), new QuotaTypes(new Integer(VM_DISK_BYTES_READ), "VM_DISK_BYTES_READ", "GB", "VM Disk usage(Bytes Read)"));
        quotaTypeList.put(new Integer(VM_DISK_BYTES_WRITE), new QuotaTypes(new Integer(VM_DISK_BYTES_WRITE), "VPN_USERS", "GB", "VM Disk usage(Bytes Write)"));
        quotaTypeList.put(new Integer(VM_SNAPSHOT), new QuotaTypes(new Integer(VM_SNAPSHOT), "VM_SNAPSHOT", "GB-Month", "VM Snapshot storage usage"));
        quotaTypeList.put(new Integer(CPU_CLOCK_RATE), new QuotaTypes(new Integer(CPU_CLOCK_RATE), "CPU_CLOCK_RATE", "Compute-Month", "Quota tariff for using 1 CPU of clock rate 100MHz"));
        quotaTypeList.put(new Integer(CPU_NUMBER), new QuotaTypes(new Integer(CPU_NUMBER), "CPU_NUMBER", "Compute-Month", "Quota tariff for running VM that has 1vCPU"));
        quotaTypeList.put(new Integer(MEMORY), new QuotaTypes(new Integer(MEMORY), "MEMORY", "Compute-Month", "Quota tariff for using 1MB or RAM for 1 hour"));
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
