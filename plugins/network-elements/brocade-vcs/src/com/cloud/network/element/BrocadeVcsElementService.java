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

package com.cloud.network.element;

import java.util.List;

import com.cloud.api.commands.AddBrocadeVcsDeviceCmd;
import com.cloud.api.commands.DeleteBrocadeVcsDeviceCmd;
import com.cloud.api.commands.ListBrocadeVcsDeviceNetworksCmd;
import com.cloud.api.commands.ListBrocadeVcsDevicesCmd;
import com.cloud.api.response.BrocadeVcsDeviceResponse;
import com.cloud.network.Network;
import com.cloud.network.BrocadeVcsDeviceVO;
import com.cloud.utils.component.PluggableService;

public interface BrocadeVcsElementService extends PluggableService {

    public BrocadeVcsDeviceVO addBrocadeVcsDevice(AddBrocadeVcsDeviceCmd cmd);

    public BrocadeVcsDeviceResponse createBrocadeVcsDeviceResponse(BrocadeVcsDeviceVO brocadeDeviceVO);

    boolean deleteBrocadeVcsDevice(DeleteBrocadeVcsDeviceCmd cmd);

    List<? extends Network> listBrocadeVcsDeviceNetworks(ListBrocadeVcsDeviceNetworksCmd cmd);

    List<BrocadeVcsDeviceVO> listBrocadeVcsDevices(ListBrocadeVcsDevicesCmd cmd);

}
