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
package com.cloud.hypervisor.kvm.resource;

import com.cloud.agent.api.VmStatsEntry;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import java.util.Calendar;

public class LibvirtExtendedVmStatsEntry extends VmStatsEntry {
    private long cpuTime;
    private Calendar timestamp;

    public LibvirtExtendedVmStatsEntry() {
    }

    public long getCpuTime() {
        return cpuTime;
    }

    public void setCpuTime(long cpuTime) {
        this.cpuTime = cpuTime;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Calendar timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "cpuTime", "networkWriteKBs", "networkReadKBs", "diskReadIOs", "diskWriteIOs", "diskReadKBs", "diskWriteKBs");
    }
}
