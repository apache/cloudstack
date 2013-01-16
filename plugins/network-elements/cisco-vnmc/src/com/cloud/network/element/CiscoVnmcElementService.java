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
package com.cloud.network.element;

import java.util.List;

import com.cloud.api.commands.AddCiscoVnmcDeviceCmd;
import com.cloud.api.commands.DeleteCiscoVnmcDeviceCmd;
import com.cloud.api.commands.ListCiscoVnmcDevicesCmd;
import com.cloud.api.response.CiscoVnmcDeviceResponse;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.cisco.CiscoVnmcDeviceVO;
import com.cloud.utils.component.PluggableService;

public interface CiscoVnmcElementService extends PluggableService {

    public static final Provider CiscoVnmc = new Provider("CiscoVnmc", true);

	public CiscoVnmcDeviceVO addCiscoVnmcDevice(AddCiscoVnmcDeviceCmd cmd);

    public CiscoVnmcDeviceResponse createCiscoVnmcDeviceResponse(
            CiscoVnmcDeviceVO ciscoVnmcDeviceVO);

    boolean deleteCiscoVnmcDevice(DeleteCiscoVnmcDeviceCmd cmd);

    List<CiscoVnmcDeviceVO> listCiscoVnmcDevices(ListCiscoVnmcDevicesCmd cmd);
    
    void assignAsa1000vToNetwork(Network network);

}
