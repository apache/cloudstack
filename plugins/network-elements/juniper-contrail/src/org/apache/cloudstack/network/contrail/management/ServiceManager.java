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

package org.apache.cloudstack.network.contrail.management;

import org.apache.cloudstack.network.contrail.api.response.ServiceInstanceResponse;

import com.cloud.dc.DataCenter;
import com.cloud.network.Network;
import com.cloud.offering.ServiceOffering;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

public interface ServiceManager {
    /**
     * Create a virtual machine that executes a network service appliance (e.g. vSRX)
     * @param left Left or inside network (e.g. project network).
     * @param right Right or outside network (e.g. public network).
     * @return
     */
    public ServiceVirtualMachine createServiceInstance(DataCenter zone, Account owner, VirtualMachineTemplate template,
            ServiceOffering serviceOffering, String name, Network left, Network right);

    public void startServiceInstance(long instanceId);
    public ServiceInstanceResponse createServiceInstanceResponse(long instanceId);
}
