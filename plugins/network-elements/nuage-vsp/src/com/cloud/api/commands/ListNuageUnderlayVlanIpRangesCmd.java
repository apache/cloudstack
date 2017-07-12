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

package com.cloud.api.commands;

import com.cloud.api.response.NuageVlanIpRangeResponse;
import com.cloud.dc.Vlan;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.utils.Pair;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.admin.vlan.ListVlanIpRangesCmd;
import org.apache.cloudstack.api.response.ListResponse;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = "listNuageUnderlayVlanIpRanges", description = "enable Nuage underlay on vlan ip range", responseObject = NuageVlanIpRangeResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.10",
        authorized = {RoleType.Admin})
public class ListNuageUnderlayVlanIpRangesCmd extends ListVlanIpRangesCmd {
    public static final String APINAME = "listNuageUnderlayVlanIpRanges";

    @Inject
    NuageVspManager _nuageVspManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = VspConstants.NUAGE_VSP_API_UNDERLAY, type = CommandType.BOOLEAN, description = "true to list only underlay enabled, false if not, empty for all")
    private Boolean underlay;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Boolean getUnderlay() {
        return underlay;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends Vlan>, Integer> vlans = _mgr.searchForVlans(this);
        ListResponse<NuageVlanIpRangeResponse> response = new ListResponse<NuageVlanIpRangeResponse>();
        List<NuageVlanIpRangeResponse> vlanIpRanges = _nuageVspManager.filterNuageVlanIpRanges(vlans.first(), underlay);
        response.setResponses(vlanIpRanges);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }
}
