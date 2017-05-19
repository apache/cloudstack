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
//
package com.cloud.baremetal.manager;

import com.cloud.baremetal.networkservice.BaremetalRctResponse;
import com.cloud.baremetal.networkservice.BaremetalSwitchBackend;
import com.cloud.deploy.DeployDestination;
import com.cloud.network.Network;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.api.AddBaremetalRctCmd;
import org.apache.cloudstack.api.DeleteBaremetalRctCmd;
import org.apache.cloudstack.framework.config.ConfigKey;

public interface BaremetalVlanManager extends Manager, PluggableService {

    String BaremetalSwitchBaseUrlCK = "baremetal.switch.base.url";
    ConfigKey<String> BaremetalSwitchBaseUrl = new ConfigKey<String>("Advanced", String.class, BaremetalSwitchBaseUrlCK,
            "/api/running/ftos/interface/", "Base URL of Switch API used in Advance Baremetal Solution.", true, ConfigKey.Scope.Zone);

    BaremetalRctResponse addRct(AddBaremetalRctCmd cmd);

    void prepareVlan(Network nw, DeployDestination destHost, String mac);

    void releaseVlan(Network nw, VirtualMachineProfile vm, String mac);

    void registerSwitchBackend(BaremetalSwitchBackend backend);

    void deleteRct(DeleteBaremetalRctCmd cmd);

    BaremetalRctResponse listRct();
}
