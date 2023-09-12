//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package com.cloud.network.vm;
import java.util.Map;

import com.cloud.api.commands.DeployNetscalerVpxCmd;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.router.VirtualRouter;
import com.cloud.user.Account;

public interface NetScalerVMManager {
    //RAM/CPU for the system offering used by Internal LB VMs
    int DEFAULT_NETSCALER_VM_RAMSIZE = 2048; // 2048 MB
    int DEFAULT_NETSCALER_VM_CPU_MHZ = 1024; // 1024 MHz

    Map<String, Object> deployNetscalerServiceVm(DeployNetscalerVpxCmd cmd);

    VirtualRouter stopNetscalerServiceVm(Long id, boolean forced, Account callingAccount, long callingUserId) throws ConcurrentOperationException, ResourceUnavailableException;

    Map<String, Object> deployNsVpx(Account owner, DeployDestination dest, DeploymentPlan plan, long svcOffId, long templateId) throws InsufficientCapacityException;

    VirtualRouter stopNetScalerVm(Long id, boolean forced, Account callingAccount, long callingUserId);
}
