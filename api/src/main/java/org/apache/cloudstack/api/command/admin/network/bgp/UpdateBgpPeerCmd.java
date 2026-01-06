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

package org.apache.cloudstack.api.command.admin.network.bgp;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BgpPeerResponse;
import org.apache.cloudstack.network.BgpPeer;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.collections.MapUtils;

import java.util.Collection;
import java.util.Map;

@APICommand(name = "updateBgpPeer",
        description = "Updates an existing Bgp Peer.",
        responseObject = BgpPeerResponse.class,
        since = "4.20.0",
        requestHasSensitiveInfo = true,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class UpdateBgpPeerCmd extends BaseAsyncCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = BgpPeerResponse.class, required = true, description = "Id of the Bgp Peer")
    private Long id;

    @Parameter(name = ApiConstants.IP_ADDRESS,
            type = CommandType.STRING,
            description = "The IPv4 address of the Bgp Peer.")
    private String ip4Address;

    @Parameter(name = ApiConstants.IP6_ADDRESS,
            type = CommandType.STRING,
            description = "The IPv6 address of the Bgp Peer.")
    private String ip6Address;

    @Parameter(name = ApiConstants.AS_NUMBER,
            type = CommandType.LONG,
            description = "The AS number of the Bgp Peer.")
    private Long asNumber;

    @Parameter(name = ApiConstants.PASSWORD,
            type = CommandType.STRING,
            description = "The password of the Bgp Peer.")
    private String password;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP,
            description = "BGP peer details in key/value pairs.")
    protected Map details;

    @Parameter(name = ApiConstants.CLEAN_UP_DETAILS,
            type = CommandType.BOOLEAN,
            description = "optional boolean field, which indicates if details should be cleaned up or not (if set to true, details are removed for this resource; if false or not set, no action)")
    private Boolean cleanupDetails;

    public Long getId() {
        return id;
    }

    public String getIp4Address() {
        return ip4Address;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public Long getAsNumber() {
        return asNumber;
    }

    public String getPassword() {
        return password;
    }

    public Map<String, String> getDetails() {
        if (MapUtils.isEmpty(details)) {
            return null;
        }
        Collection<String> paramsCollection = this.details.values();
        return (Map<String, String>) (paramsCollection.toArray())[0];
    }

    public boolean isCleanupDetails(){
        return cleanupDetails == null ? false : cleanupDetails.booleanValue();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_BGP_PEER_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating Bgp Peer " + getId();
    }

    @Override
    public void execute() {
        try {
            BgpPeer result = routedIpv4Manager.updateBgpPeer(this);
            if (result != null) {
                BgpPeerResponse response = routedIpv4Manager.createBgpPeerResponse(result);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update Bgp Peer:" + getId());
            }
        } catch (InvalidParameterValueException ex) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, ex.getMessage());
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }

    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
