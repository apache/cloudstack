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

import net.nuage.vsp.acs.client.api.NuageVspManagerClient;
import net.nuage.vsp.acs.client.common.model.Pair;
import net.nuage.vsp.acs.client.exception.NuageVspException;

import com.cloud.agent.api.sync.SyncNuageVspCmsIdAnswer;
import com.cloud.agent.api.sync.SyncNuageVspCmsIdCommand;
import com.cloud.network.resource.NuageVspResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.StringUtils;

@ResourceWrapper(handles =  SyncNuageVspCmsIdCommand.class)
public final class NuageVspSyncCmsIdCommandWrapper extends CommandWrapper<SyncNuageVspCmsIdCommand, SyncNuageVspCmsIdAnswer, NuageVspResource> {

    @Override public SyncNuageVspCmsIdAnswer execute(SyncNuageVspCmsIdCommand cmd, NuageVspResource nuageVspResource) {
        NuageVspManagerClient client = null;
        try {
            client = nuageVspResource.getNuageVspManagerClient();

            Pair<Boolean, String> answer;
            switch (cmd.getSyncType()) {
            case REGISTER:
                String registeredNuageVspCmsId = client.registerNuageVspCmsId();
                answer = Pair.of(StringUtils.isNotBlank(registeredNuageVspCmsId), registeredNuageVspCmsId);
                break;
            case UNREGISTER:
                boolean success = client.unregisterNuageVspCmsId(cmd.getNuageVspCmsId());
                answer = Pair.of(success, cmd.getNuageVspCmsId());
                break;
            default:
                answer = client.auditNuageVspCmsId(cmd.getNuageVspCmsId(), cmd.getSyncType() == SyncNuageVspCmsIdCommand.SyncType.AUDIT_ONLY);
                break;
            }
            return new SyncNuageVspCmsIdAnswer(answer.getLeft(), answer.getRight(), cmd.getSyncType());
        } catch (ConfigurationException|NuageVspException e) {
            return new SyncNuageVspCmsIdAnswer(cmd, e, cmd.getSyncType());
        }
    }
}