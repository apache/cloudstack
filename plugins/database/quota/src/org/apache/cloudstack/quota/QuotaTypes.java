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
package org.apache.cloudstack.quota;

import org.apache.cloudstack.api.response.QuotaTypeResponse;
import org.apache.cloudstack.usage.UsageTypes;

import java.util.ArrayList;
import java.util.List;

public class QuotaTypes extends UsageTypes {
    public static final int CPU_CLOCK_RATE = 15;
    public static final int CPU_NUMBER = 16;
    public static final int MEMORY = 17;

    public static List<QuotaTypeResponse> responseList = new ArrayList<QuotaTypeResponse>();

    public static List<QuotaTypeResponse> listQuotaUsageTypes() {
        responseList.add(new QuotaTypeResponse(RUNNING_VM, "Running Vm Usage"));
        responseList.add(new QuotaTypeResponse(ALLOCATED_VM, "Allocated Vm Usage"));
        responseList.add(new QuotaTypeResponse(IP_ADDRESS, "IP Address Usage"));
        responseList.add(new QuotaTypeResponse(NETWORK_BYTES_SENT, "Network Usage (Bytes Sent)"));
        responseList.add(new QuotaTypeResponse(NETWORK_BYTES_RECEIVED, "Network Usage (Bytes Received)"));
        responseList.add(new QuotaTypeResponse(VOLUME, "Volume Usage"));
        responseList.add(new QuotaTypeResponse(TEMPLATE, "Template Usage"));
        responseList.add(new QuotaTypeResponse(ISO, "ISO Usage"));
        responseList.add(new QuotaTypeResponse(SNAPSHOT, "Snapshot Usage"));
        responseList.add(new QuotaTypeResponse(SECURITY_GROUP, "Security Group Usage"));
        responseList.add(new QuotaTypeResponse(LOAD_BALANCER_POLICY, "Load Balancer Usage"));
        responseList.add(new QuotaTypeResponse(PORT_FORWARDING_RULE, "Port Forwarding Usage"));
        responseList.add(new QuotaTypeResponse(NETWORK_OFFERING, "Network Offering Usage"));
        responseList.add(new QuotaTypeResponse(VPN_USERS, "VPN users usage"));
        responseList.add(new QuotaTypeResponse(VM_DISK_IO_READ, "VM Disk usage(I/O Read)"));
        responseList.add(new QuotaTypeResponse(VM_DISK_IO_WRITE, "VM Disk usage(I/O Write)"));
        responseList.add(new QuotaTypeResponse(VM_DISK_BYTES_READ, "VM Disk usage(Bytes Read)"));
        responseList.add(new QuotaTypeResponse(VM_DISK_BYTES_WRITE, "VM Disk usage(Bytes Write)"));
        responseList.add(new QuotaTypeResponse(VM_SNAPSHOT, "VM Snapshot storage usage"));
        responseList.add(new QuotaTypeResponse(CPU_CLOCK_RATE, "Quota tariff for using 1 CPU of clock rate 100MHz"));
        responseList.add(new QuotaTypeResponse(CPU_NUMBER, "Quota tariff for running VM that has 1vCPU"));
        responseList.add(new QuotaTypeResponse(MEMORY, "Quota tariff for using 1MB or RAM for 1 hour"));
        return responseList;
    }

}
