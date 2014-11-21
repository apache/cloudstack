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

import com.cloud.api.commands.AddCiscoAsa1000vResourceCmd;
import com.cloud.api.commands.DeleteCiscoAsa1000vResourceCmd;
import com.cloud.api.commands.ListCiscoAsa1000vResourcesCmd;
import com.cloud.api.response.CiscoAsa1000vResourceResponse;
import com.cloud.network.Network;
import com.cloud.network.cisco.CiscoAsa1000vDevice;
import com.cloud.network.cisco.CiscoAsa1000vDeviceVO;
import com.cloud.utils.component.PluggableService;

public interface CiscoAsa1000vService extends PluggableService {

    public CiscoAsa1000vDevice addCiscoAsa1000vResource(AddCiscoAsa1000vResourceCmd cmd);

    public CiscoAsa1000vResourceResponse createCiscoAsa1000vResourceResponse(CiscoAsa1000vDevice ciscoAsa1000vDeviceVO);

    boolean deleteCiscoAsa1000vResource(DeleteCiscoAsa1000vResourceCmd cmd);

    List<CiscoAsa1000vDeviceVO> listCiscoAsa1000vResources(ListCiscoAsa1000vResourcesCmd cmd);

    CiscoAsa1000vDevice assignAsa1000vToNetwork(Network network);

}
