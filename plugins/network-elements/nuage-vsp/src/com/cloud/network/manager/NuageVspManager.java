//
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

package com.cloud.network.manager;

import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.ConfigKey.Scope;

import com.cloud.api.commands.AddNuageVspDeviceCmd;
import com.cloud.api.commands.DeleteNuageVspDeviceCmd;
import com.cloud.api.commands.ListNuageVspDevicesCmd;
import com.cloud.api.response.NuageVspDeviceResponse;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.utils.component.PluggableService;

public interface NuageVspManager extends PluggableService {

    static final String NUAGE_VPC_OFFERING_NAME = "Default VPC offering with NuageVsp";

    static final String NUAGE_VPC_OFFERING_DISPLAY_TEXT = "Default VPC offering with NuageVsp";

    static final ConfigKey<Integer> NuageVspSyncInterval = new ConfigKey<Integer>(Integer.class, "nuagevsp.sync.interval", "Advanced", "480",
            "The interval (in minutes) to wait before running the next synchronization worker to synchronize the information between CloudStack and NuageVsp", false, Scope.Global,
            1);

    static final ConfigKey<Integer> NuageVspSyncWorkers = new ConfigKey<Integer>(Integer.class, "nuagevsp.sync.workers", "Advanced", "1",
            "Number of workers to synchronize the information between CloudStack and NuageVsp", false, Scope.Global, 1);

    NuageVspDeviceVO addNuageVspDevice(AddNuageVspDeviceCmd cmd);

    NuageVspDeviceResponse createNuageVspDeviceResponse(NuageVspDeviceVO nuageVspDeviceVO);

    boolean deleteNuageVspDevice(DeleteNuageVspDeviceCmd cmd);

    List<NuageVspDeviceVO> listNuageVspDevices(ListNuageVspDevicesCmd cmd);

}
