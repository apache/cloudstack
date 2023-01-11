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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.usage.UsageTypes;
import org.apache.cloudstack.usage.UsageUnitTypes;

public class QuotaTypes extends UsageTypes {
    private final Integer quotaType;
    private final String quotaName;
    private final String quotaUnit;
    private final String description;
    private final String discriminator;
    private final static Map<Integer, QuotaTypes> quotaTypeMap;

    static {
        final HashMap<Integer, QuotaTypes> quotaTypeList = new HashMap<Integer, QuotaTypes>();
        quotaTypeList.put(RUNNING_VM, new QuotaTypes(RUNNING_VM, "RUNNING_VM", UsageUnitTypes.COMPUTE_MONTH.toString(), "Running Vm Usage"));
        quotaTypeList.put(ALLOCATED_VM, new QuotaTypes(ALLOCATED_VM, "ALLOCATED_VM", UsageUnitTypes.COMPUTE_MONTH.toString(), "Allocated Vm Usage"));
        quotaTypeList.put(IP_ADDRESS, new QuotaTypes(IP_ADDRESS, "IP_ADDRESS", UsageUnitTypes.IP_MONTH.toString(), "IP Address Usage"));
        quotaTypeList.put(NETWORK_BYTES_SENT, new QuotaTypes(NETWORK_BYTES_SENT, "NETWORK_BYTES_SENT", UsageUnitTypes.GB.toString(), "Network Usage (Bytes Sent)"));
        quotaTypeList.put(NETWORK_BYTES_RECEIVED, new QuotaTypes(NETWORK_BYTES_RECEIVED, "NETWORK_BYTES_RECEIVED", UsageUnitTypes.GB.toString(), "Network Usage (Bytes Received)"));
        quotaTypeList.put(VOLUME, new QuotaTypes(VOLUME, "VOLUME", UsageUnitTypes.GB_MONTH.toString(), "Volume Usage"));
        quotaTypeList.put(TEMPLATE, new QuotaTypes(TEMPLATE, "TEMPLATE", UsageUnitTypes.GB_MONTH.toString(), "Template Usage"));
        quotaTypeList.put(ISO, new QuotaTypes(ISO, "ISO", UsageUnitTypes.GB_MONTH.toString(), "ISO Usage"));
        quotaTypeList.put(SNAPSHOT, new QuotaTypes(SNAPSHOT, "SNAPSHOT", UsageUnitTypes.GB_MONTH.toString(), "Snapshot Usage"));
        quotaTypeList.put(SECURITY_GROUP, new QuotaTypes(SECURITY_GROUP, "SECURITY_GROUP", UsageUnitTypes.POLICY_MONTH.toString(), "Security Group Usage"));
        quotaTypeList.put(LOAD_BALANCER_POLICY, new QuotaTypes(LOAD_BALANCER_POLICY, "LOAD_BALANCER_POLICY", UsageUnitTypes.POLICY_MONTH.toString(), "Load Balancer Usage"));
        quotaTypeList.put(PORT_FORWARDING_RULE, new QuotaTypes(PORT_FORWARDING_RULE, "PORT_FORWARDING_RULE", UsageUnitTypes.POLICY_MONTH.toString(), "Port Forwarding Usage"));
        quotaTypeList.put(NETWORK_OFFERING, new QuotaTypes(NETWORK_OFFERING, "NETWORK_OFFERING", UsageUnitTypes.POLICY_MONTH.toString(), "Network Offering Usage"));
        quotaTypeList.put(VPN_USERS, new QuotaTypes(VPN_USERS, "VPN_USERS", UsageUnitTypes.POLICY_MONTH.toString(), "VPN users usage"));
        quotaTypeList.put(VM_DISK_IO_READ, new QuotaTypes(VM_DISK_IO_READ, "VM_DISK_IO_READ", UsageUnitTypes.GB.toString(), "VM Disk usage(I/O Read)"));
        quotaTypeList.put(VM_DISK_IO_WRITE, new QuotaTypes(VM_DISK_IO_WRITE, "VM_DISK_IO_WRITE", UsageUnitTypes.GB.toString(), "VM Disk usage(I/O Write)"));
        quotaTypeList.put(VM_DISK_BYTES_READ, new QuotaTypes(VM_DISK_BYTES_READ, "VM_DISK_BYTES_READ", UsageUnitTypes.GB.toString(), "VM Disk usage(Bytes Read)"));
        quotaTypeList.put(VM_DISK_BYTES_WRITE, new QuotaTypes(VM_DISK_BYTES_WRITE, "VM_DISK_BYTES_WRITE", UsageUnitTypes.GB.toString(), "VM Disk usage(Bytes Write)"));
        quotaTypeList.put(VM_SNAPSHOT, new QuotaTypes(VM_SNAPSHOT, "VM_SNAPSHOT", UsageUnitTypes.GB_MONTH.toString(), "VM Snapshot storage usage"));
        quotaTypeList.put(VOLUME_SECONDARY, new QuotaTypes(VOLUME_SECONDARY, "VOLUME_SECONDARY", UsageUnitTypes.GB_MONTH.toString(), "Volume secondary storage usage"));
        quotaTypeList.put(VM_SNAPSHOT_ON_PRIMARY, new QuotaTypes(VM_SNAPSHOT_ON_PRIMARY, "VM_SNAPSHOT_ON_PRIMARY", UsageUnitTypes.GB_MONTH.toString(), "VM Snapshot primary storage usage"));
        quotaTypeList.put(BACKUP, new QuotaTypes(BACKUP, "BACKUP", UsageUnitTypes.GB_MONTH.toString(), "Backup storage usage"));
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
