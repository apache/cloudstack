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
package com.cloud.storage.secondary;

import java.util.List;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.HostVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.vm.SecondaryStorageVmVO;

public interface SecondaryStorageVmManager extends Manager {

    public static final int DEFAULT_SS_VM_RAMSIZE = 512;            // 512M
    public static final int DEFAULT_SS_VM_CPUMHZ = 500;                // 500 MHz
    public static final int DEFAULT_SS_VM_MTUSIZE = 1500;
    public static final int DEFAULT_SS_VM_CAPACITY = 50;            // max command execution session per SSVM
    public static final int DEFAULT_STANDBY_CAPACITY = 10;            // standy capacity to reserve per zone

    public static final String ALERT_SUBJECT = "secondarystoragevm-alert";

    public SecondaryStorageVmVO startSecStorageVm(long ssVmVmId);

    public boolean stopSecStorageVm(long ssVmVmId);

    public boolean rebootSecStorageVm(long ssVmVmId);

    public boolean destroySecStorageVm(long ssVmVmId);

    public void onAgentConnect(Long dcId, StartupCommand cmd);

    public boolean generateFirewallConfiguration(Long agentId);

    public boolean generateVMSetupCommand(Long hostId);

    public Pair<HostVO, SecondaryStorageVmVO> assignSecStorageVm(long zoneId, Command cmd);

    boolean generateSetupCommand(Long hostId);

    public List<HostVO> listUpAndConnectingSecondaryStorageVmHost(Long dcId);

    public HostVO pickSsvmHost(HostVO ssHost);
}
