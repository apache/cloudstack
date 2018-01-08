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

package com.cloud.agent.api.element;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.cloud.agent.api.Command;
import com.cloud.network.manager.NuageVspManager;

import net.nuage.vsp.acs.client.api.model.NetworkRelatedVsdIds;

public class ShutDownVpcVspCommand extends Command {

    private final String _domainUuid;
    private final String _vpcUuid;
    private final String _domainTemplateName;
    private final List<String> _domainRouterUuids;
    private final NetworkRelatedVsdIds _relatedVsdIds;

    public ShutDownVpcVspCommand(String domainUuid, String vpcUuid, String domainTemplateName, List<String> domainRouterUuids, Map<String, String> details) {
        super();
        this._domainUuid = domainUuid;
        this._vpcUuid = vpcUuid;
        this._domainTemplateName = domainTemplateName;
        this._domainRouterUuids = domainRouterUuids;
        this._relatedVsdIds = new NetworkRelatedVsdIds.Builder()
                .vsdDomainId(details.get(NuageVspManager.NETWORK_METADATA_VSD_DOMAIN_ID))
                .vsdZoneId(details.get(NuageVspManager.NETWORK_METADATA_VSD_ZONE_ID))
                .withVsdManaged(details.get(NuageVspManager.NETWORK_METADATA_VSD_MANAGED) != null && details.get(NuageVspManager.NETWORK_METADATA_VSD_MANAGED).equals("true"))
                .build();
    }

    public String getDomainUuid() {
        return _domainUuid;
    }

    public String getVpcUuid() {
        return _vpcUuid;
    }

    public String getDomainTemplateName() {
        return _domainTemplateName;
    }

    public List<String> getDomainRouterUuids() {
        return _domainRouterUuids;
    }

    public NetworkRelatedVsdIds getRelatedVsdIds() {
        return _relatedVsdIds;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShutDownVpcVspCommand)) {
            return false;
        }
        if (!super.equals(o)) return false;

        ShutDownVpcVspCommand that = (ShutDownVpcVspCommand) o;

        return super.equals(that)
            && Objects.equals(_domainUuid, that._domainUuid)
            && Objects.equals(_vpcUuid, that._vpcUuid)
            && Objects.equals(_domainTemplateName, that._domainTemplateName)
            && Objects.equals(_domainRouterUuids, that._domainRouterUuids);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(_domainUuid)
                .append(_vpcUuid)
                .append(_domainTemplateName)
                .toHashCode();
    }
}
