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

import net.nuage.vsp.acs.client.common.model.NuageVspEntity;
import net.nuage.vsp.acs.client.exception.NuageVspException;

import com.cloud.agent.api.manager.EntityExistsCommand;
import com.cloud.dc.Vlan;
import com.cloud.network.resource.NuageVspResource;
import com.cloud.network.vpc.VpcVO;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  EntityExistsCommand.class)
public final class NuageVspEntityExistsCommandWrapper extends NuageVspCommandWrapper<EntityExistsCommand> {

    @Override public boolean executeNuageVspCommand(EntityExistsCommand cmd, NuageVspResource nuageVspResource) throws ConfigurationException, NuageVspException {
        NuageVspEntity entityType = getNuageVspEntity(cmd.getType());

        return nuageVspResource.getNuageVspApiClient().entityExists(entityType, cmd.getUuid());
    }

    private NuageVspEntity getNuageVspEntity(Class clazz) {
        NuageVspEntity entityType = null;

        if (Vlan.class.isAssignableFrom(clazz)) {
            entityType = NuageVspEntity.SHARED_NETWORK;
        }
        else if(VpcVO.class.isAssignableFrom(clazz)){
            entityType = NuageVspEntity.ZONE;
        }

        return entityType;
    }

    @Override public StringBuilder fillDetail(StringBuilder stringBuilder, EntityExistsCommand cmd) {
        return stringBuilder.append("Check if entity with UUID " + cmd.getUuid() + " of type " + getNuageVspEntity(cmd.getType()) + " exists");
    }

}