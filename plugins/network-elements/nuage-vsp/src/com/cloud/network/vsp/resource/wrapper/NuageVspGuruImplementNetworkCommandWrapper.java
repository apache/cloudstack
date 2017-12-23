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

package com.cloud.network.vsp.resource.wrapper;

import javax.naming.ConfigurationException;

import net.nuage.vsp.acs.client.api.NuageVspGuruClient;
import net.nuage.vsp.acs.client.api.model.NetworkRelatedVsdIds;
import net.nuage.vsp.acs.client.exception.NuageVspException;

import com.cloud.agent.api.guru.ImplementNetworkVspCommand;
import com.cloud.agent.api.manager.ImplementNetworkVspAnswer;
import com.cloud.network.resource.NuageVspResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  ImplementNetworkVspCommand.class)
public final class NuageVspGuruImplementNetworkCommandWrapper extends CommandWrapper<ImplementNetworkVspCommand, ImplementNetworkVspAnswer, NuageVspResource> {

    @Override
    public ImplementNetworkVspAnswer execute(ImplementNetworkVspCommand command, NuageVspResource serverResource) {
        try {
            NuageVspGuruClient client = serverResource.getNuageVspGuruClient();
            NetworkRelatedVsdIds networkRelatedVsdIds = client.implement(command.getNetwork(), command.getDhcpOption());
            return new ImplementNetworkVspAnswer(command, networkRelatedVsdIds);
        } catch (ConfigurationException | NuageVspException e) {
            return new ImplementNetworkVspAnswer(command, e);
        }
    }
}