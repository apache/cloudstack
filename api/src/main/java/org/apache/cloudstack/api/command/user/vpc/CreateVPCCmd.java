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
package org.apache.cloudstack.api.command.user.vpc;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.VpcOfferingResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.Vpc;

@APICommand(name = "createVPC", description = "Creates a VPC", responseObject = VpcResponse.class, responseView = ResponseView.Restricted, entityType = {Vpc.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateVPCCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateVPCCmd.class.getName());
    private static final String s_name = "createvpcresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "the account associated with the VPC. " +
            "Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class,
               description = "the domain ID associated with the VPC. " +
            "If used with the account parameter returns the VPC associated with the account for the specified domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class,
               description = "create VPC for the project")
    private Long projectId;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class,
            required = true, description = "the ID of the availability zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the VPC")
    private String vpcName;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, required = true, description = "the display text of " +
            "the VPC")
    private String displayText;

    @Parameter(name = ApiConstants.CIDR, type = CommandType.STRING, required = true, description = "the cidr of the VPC. All VPC " +
            "guest networks' cidrs should be within this CIDR")
    private String cidr;

    @Parameter(name = ApiConstants.VPC_OFF_ID, type = CommandType.UUID, entityType = VpcOfferingResponse.class,
               required = true, description = "the ID of the VPC offering")
    private Long vpcOffering;

    @Parameter(name = ApiConstants.NETWORK_DOMAIN, type = CommandType.STRING,
               description = "VPC network domain. All networks inside the VPC will belong to this domain")
    private String networkDomain;

    @Parameter(name = ApiConstants.START, type = CommandType.BOOLEAN,
               description = "If set to false, the VPC won't start (VPC VR will not get allocated) until its first network gets implemented. " +
                   "True by default.", since = "4.3")
    private Boolean start;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the vpc to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getVpcName() {
        return vpcName;
    }

    public String getCidr() {
        return cidr;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Long getVpcOffering() {
        return vpcOffering;
    }

    public String getNetworkDomain() {
        return networkDomain;
    }

    public boolean isStart() {
        if (start != null) {
            return start;
        }
        return true;
    }

    public Boolean getDisplayVpc() {
        return display;
    }

    @Override
    public void create() throws ResourceAllocationException {
        Vpc vpc = _vpcService.createVpc(getZoneId(), getVpcOffering(), getEntityOwnerId(), getVpcName(), getDisplayText(), getCidr(), getNetworkDomain(), getDisplayVpc());
        if (vpc != null) {
            setEntityId(vpc.getId());
            setEntityUuid(vpc.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create a VPC");
        }
    }

    @Override
    public void execute() {
        Vpc vpc = null;
        try {
            if (isStart()) {
                _vpcService.startVpc(getEntityId(), true);
            } else {
                s_logger.debug("Not starting VPC as " + ApiConstants.START + "=false was passed to the API");
             }
            vpc = _entityMgr.findById(Vpc.class, getEntityId());
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (InsufficientCapacityException ex) {
            s_logger.info(ex);
            s_logger.trace(ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, ex.getMessage());
        }

        if (vpc != null) {
            VpcResponse response = _responseGenerator.createVpcResponse(ResponseView.Restricted, vpc);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create VPC");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VPC_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "creating VPC. Id: " + getEntityId();
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }
}
