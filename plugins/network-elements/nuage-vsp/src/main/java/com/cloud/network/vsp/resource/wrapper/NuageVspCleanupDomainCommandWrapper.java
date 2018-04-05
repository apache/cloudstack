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

import net.nuage.vsp.acs.client.exception.NuageVspException;

import com.cloud.agent.api.manager.CleanUpDomainCommand;
import com.cloud.network.resource.NuageVspResource;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  CleanUpDomainCommand.class)
public final class NuageVspCleanupDomainCommandWrapper extends NuageVspCommandWrapper<CleanUpDomainCommand> {

    @Override public boolean executeNuageVspCommand(CleanUpDomainCommand cmd, NuageVspResource nuageVspResource) throws ConfigurationException, NuageVspException {
        return nuageVspResource.getNuageVspManagerClient().cleanUpDomain(cmd.getDomainCleanUp());
    }

    @Override public StringBuilder fillDetail(StringBuilder stringBuilder, CleanUpDomainCommand cmd) {
        return stringBuilder.append("Clean Domain ").append(cmd.getDomainCleanUp().getUuid());
    }

}