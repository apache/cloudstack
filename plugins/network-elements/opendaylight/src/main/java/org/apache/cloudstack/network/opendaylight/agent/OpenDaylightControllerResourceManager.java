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

package org.apache.cloudstack.network.opendaylight.agent;

import java.util.List;

import org.apache.cloudstack.network.opendaylight.api.commands.AddOpenDaylightControllerCmd;
import org.apache.cloudstack.network.opendaylight.api.commands.DeleteOpenDaylightControllerCmd;
import org.apache.cloudstack.network.opendaylight.api.commands.ListOpenDaylightControllersCmd;
import org.apache.cloudstack.network.opendaylight.api.responses.OpenDaylightControllerResponse;
import org.apache.cloudstack.network.opendaylight.dao.OpenDaylightControllerVO;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.PluggableService;

public interface OpenDaylightControllerResourceManager extends PluggableService {

    public OpenDaylightControllerVO addController(AddOpenDaylightControllerCmd cmd);

    public void deleteController(DeleteOpenDaylightControllerCmd cmd) throws InvalidParameterValueException;

    public List<OpenDaylightControllerVO> listControllers(ListOpenDaylightControllersCmd cmd);

    public OpenDaylightControllerResponse createResponseFromVO(OpenDaylightControllerVO controller);
}
