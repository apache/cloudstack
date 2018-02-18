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

import com.cloud.api.commands.AddBigSwitchBcfDeviceCmd;
import com.cloud.api.commands.DeleteBigSwitchBcfDeviceCmd;
import com.cloud.api.commands.ListBigSwitchBcfDevicesCmd;
import com.cloud.api.response.BigSwitchBcfDeviceResponse;
import com.cloud.network.BigSwitchBcfDeviceVO;
import com.cloud.utils.component.PluggableService;

public interface BigSwitchBcfElementService extends PluggableService {

    public BigSwitchBcfDeviceVO addBigSwitchBcfDevice(AddBigSwitchBcfDeviceCmd cmd);

    public BigSwitchBcfDeviceResponse createBigSwitchBcfDeviceResponse(BigSwitchBcfDeviceVO bigswitchDeviceVO);

    public boolean deleteBigSwitchBcfDevice(DeleteBigSwitchBcfDeviceCmd cmd);

    public List<BigSwitchBcfDeviceVO> listBigSwitchBcfDevices(ListBigSwitchBcfDevicesCmd cmd);

}
