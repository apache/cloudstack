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
package org.apache.cloudstack.api.command.user.address;

import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.RegionResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.vpc.Vpc;
import com.cloud.offering.NetworkOffering;
import com.cloud.projects.Project;
import com.cloud.user.Account;

@APICommand(name = "associateIpAddress",
        description = "Acquires and associates a public IP to an account. Either of the parameters are required, i.e. either zoneId, or networkId, or vpcId  ",
        responseObject = IPAddressResponse.class,
        responseView = ResponseView.Restricted,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false)
public class AssociateIPAddrCmd extends BaseAsyncCreateCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(AssociateIPAddrCmd.class.getName());
    private static final String s_name = "associateipaddressresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT,
            type = CommandType.STRING,
            description = "the account to associate with this IP address")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "the ID of the domain to associate with this IP address")
    private Long domainId;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "the ID of the availability zone you want to acquire an public IP address from")
    private Long zoneId;

    @Parameter(name = ApiConstants.NETWORK_ID,
            type = CommandType.UUID,
            entityType = NetworkResponse.class,
            description = "The network this IP address should be associated to.")
    private Long networkId;

    @Parameter(name = ApiConstants.PROJECT_ID,
            type = CommandType.UUID,
            entityType = ProjectResponse.class,
            description = "Deploy VM for the project")
    private Long projectId;

    @Parameter(name = ApiConstants.VPC_ID,
            type = CommandType.UUID,
            entityType = VpcResponse.class,
            description = "the VPC you want the IP address to be associated with")
    private Long vpcId;

    @Parameter(name = ApiConstants.IS_PORTABLE,
            type = BaseCmd.CommandType.BOOLEAN,
            description = "should be set to true if public IP is required to be transferable across zones, if not specified defaults to false")
    private Boolean isPortable;

    @Parameter(name = ApiConstants.REGION_ID,
            type = CommandType.INTEGER,
            entityType = RegionResponse.class,
            required = false,
            description = "region ID from where portable IP is to be associated.")
    private Integer regionId;

    @Parameter(name = ApiConstants.FOR_DISPLAY,
            type = CommandType.BOOLEAN,
            description = "an optional field, whether to the display the IP to the end user or not", since = "4.4",
            authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        if (accountName != null) {
            return accountName;
        }
        return CallContext.current().getCallingAccount().getAccountName();
    }

    public long getDomainId() {
        if (domainId != null) {
            return domainId;
        }
        return CallContext.current().getCallingAccount().getDomainId();
    }

    private long getZoneId() {
        if (zoneId != null) {
            return zoneId;
        } else if (vpcId != null) {
            Vpc vpc = _entityMgr.findById(Vpc.class, vpcId);
            if (vpc != null) {
                return vpc.getZoneId();
            }
        } else if (networkId != null) {
            Network ntwk = _entityMgr.findById(Network.class, networkId);
            if (ntwk != null) {
                return ntwk.getDataCenterId();
            }
        }

        throw new InvalidParameterValueException("Unable to figure out zone to assign IP to."
                + " Please specify either zoneId, or networkId, or vpcId in the call");
    }

    public Long getVpcId() {
        return vpcId;
    }

    public boolean isPortable() {
        if (isPortable == null) {
            return false;
        } else {
            return isPortable;
        }
    }

    public Integer getRegionId() {
        return regionId;
    }

    public Long getNetworkId() {
        if (vpcId != null) {
            return null;
        }

        if (networkId != null) {
            return networkId;
        }
        Long zoneId = getZoneId();

        DataCenter zone = _entityMgr.findById(DataCenter.class, zoneId);
        if (zone.getNetworkType() == NetworkType.Advanced) {
            List<? extends Network> networks = _networkService.getIsolatedNetworksOwnedByAccountInZone(getZoneId(), _accountService.getAccount(getEntityOwnerId()));
            if (networks.size() == 0) {
                String domain = _domainService.getDomain(getDomainId()).getName();
                throw new InvalidParameterValueException("Account name=" + getAccountName() + " domain=" + domain + " doesn't have virtual networks in zone=" +
                        zone.getName());
            }

            if (networks.size() < 1) {
                throw new InvalidParameterValueException("Account doesn't have any isolated networks in the zone");
            } else if (networks.size() > 1) {
                throw new InvalidParameterValueException("Account has more than one isolated network in the zone");
            }

            return networks.get(0).getId();
        } else {
            Network defaultGuestNetwork = _networkService.getExclusiveGuestNetwork(zoneId);
            if (defaultGuestNetwork == null) {
                throw new InvalidParameterValueException("Unable to find a default guest network for account " + getAccountName() + " in domain ID=" + getDomainId());
            } else {
                return defaultGuestNetwork.getId();
            }
        }
    }

    @Deprecated
    public Boolean getDisplayIp() {
        return display;
    }

    @Override
    public boolean isDisplay() {
        if (display == null)
            return true;
        else
            return display;
    }

    @Override
    public long getEntityOwnerId() {
        Account caller = CallContext.current().getCallingAccount();
        if (accountName != null && domainId != null) {
            Account account = _accountService.finalizeOwner(caller, accountName, domainId, projectId);
            return account.getId();
        } else if (projectId != null) {
            Project project = _projectService.getProject(projectId);
            if (project != null) {
                if (project.getState() == Project.State.Active) {
                    return project.getProjectAccountId();
                } else {
                    throw new PermissionDeniedException("Can't add resources to the project with specified projectId in state=" + project.getState() +
                            " as it's no longer active");
                }
            } else {
                throw new InvalidParameterValueException("Unable to find project by ID");
            }
        } else if (networkId != null) {
            Network network = _networkService.getNetwork(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Unable to find network by network id specified");
            }

            NetworkOffering offering = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());

            DataCenter zone = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
            if (zone.getNetworkType() == NetworkType.Basic && offering.isElasticIp() && offering.isElasticLb()) {
                // Since the basic zone network is owned by 'Root' domain, domain access checkers will fail for the
                // accounts in non-root domains while acquiring public IP. So add an exception for the 'Basic' zone
                // shared network with EIP/ELB service.
                return caller.getAccountId();
            }

            return network.getAccountId();
        } else if (vpcId != null) {
            Vpc vpc = _entityMgr.findById(Vpc.class, getVpcId());
            if (vpc == null) {
                throw new InvalidParameterValueException("Can't find enabled VPC by ID specified");
            }
            return vpc.getAccountId();
        }

        return caller.getAccountId();
    }

    @Override
    public String getEventType() {
        if (isPortable()) {
            return EventTypes.EVENT_PORTABLE_IP_ASSIGN;
        } else {
            return EventTypes.EVENT_NET_IP_ASSIGN;
        }
    }

    @Override
    public String getEventDescription() {
        return "associating IP to network ID: " + getNetworkId() + " in zone " + getZoneId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "addressinfo";
    }

    @Override
    public void create() throws ResourceAllocationException {
        try {
            IpAddress ip = null;

            if (!isPortable()) {
                ip = _networkService.allocateIP(_accountService.getAccount(getEntityOwnerId()), getZoneId(), getNetworkId(), getDisplayIp());
            } else {
                ip = _networkService.allocatePortableIP(_accountService.getAccount(getEntityOwnerId()), 1, getZoneId(), getNetworkId(), getVpcId());
            }

            if (ip != null) {
                setEntityId(ip.getId());
                setEntityUuid(ip.getUuid());
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to allocate IP address");
            }
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (InsufficientAddressCapacityException ex) {
            s_logger.info(ex);
            s_logger.trace(ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, ex.getMessage());
        }
    }

    @Override
    public void execute() throws ResourceUnavailableException, ResourceAllocationException, ConcurrentOperationException, InsufficientCapacityException {
        CallContext.current().setEventDetails("IP ID: " + getEntityId());

        IpAddress result = null;

        if (getVpcId() != null) {
            result = _vpcService.associateIPToVpc(getEntityId(), getVpcId());
        } else if (getNetworkId() != null) {
            result = _networkService.associateIPToNetwork(getEntityId(), getNetworkId());
        }

        if (result != null) {
            IPAddressResponse ipResponse = _responseGenerator.createIPAddressResponse(getResponseView(), result);
            ipResponse.setResponseName(getCommandName());
            setResponseObject(ipResponse);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to assign IP address");
        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return getNetworkId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.IpAddress;
    }

}
