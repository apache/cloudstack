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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import com.cloud.async.AsyncJob;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@APICommand(name = "associateIpAddress", description="Acquires and associates a public IP to an account.", responseObject=IPAddressResponse.class)
public class AssociateIPAddrCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(AssociateIPAddrCmd.class.getName());
    private static final String s_name = "associateipaddressresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING,
            description="the account to associate with this IP address")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType = DomainResponse.class,
        description="the ID of the domain to associate with this IP address")
    private Long domainId;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType = ZoneResponse.class,
        description="the ID of the availability zone you want to acquire an public IP address from")
    private Long zoneId;

    @Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.UUID, entityType = NetworkResponse.class,
        description="The network this ip address should be associated to.")
    private Long networkId;

    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.UUID, entityType = ProjectResponse.class,
        description="Deploy vm for the project")
    private Long projectId;

    @Parameter(name=ApiConstants.VPC_ID, type=CommandType.UUID, entityType = VpcResponse.class,
            description="the VPC you want the ip address to " +
            "be associated with")
    private Long vpcId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public String getAccountName() {
        if (accountName != null) {
            return accountName;
        }
        return UserContext.current().getCaller().getAccountName();
    }

    public long getDomainId() {
        if (domainId != null) {
            return domainId;
        }
        return UserContext.current().getCaller().getDomainId();
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

        throw new InvalidParameterValueException("Unable to figure out zone to assign ip to");
    }

    public Long getVpcId() {
        return vpcId;
    }

    public Long getNetworkId() {
        if (vpcId != null) {
            return null;
        }

        if (networkId != null) {
            return networkId;
        }
        Long zoneId = getZoneId();

        if (zoneId == null) {
            return null;
        }

        DataCenter zone = _configService.getZone(zoneId);
        if (zone.getNetworkType() == NetworkType.Advanced) {
            List<? extends Network> networks = _networkService.getIsolatedNetworksOwnedByAccountInZone(getZoneId(),
                    _accountService.getAccount(getEntityOwnerId()));
            if (networks.size() == 0) {
                String domain = _domainService.getDomain(getDomainId()).getName();
                throw new InvalidParameterValueException("Account name=" + getAccountName() + " domain=" + domain +
                        " doesn't have virtual networks in zone=" + zone.getName());
            }

            if (networks.size() < 1) {
                throw new InvalidParameterValueException("Account doesn't have any Isolated networks in the zone");
            } else if (networks.size() > 1) {
                throw new InvalidParameterValueException("Account has more than one Isolated network in the zone");
            }

            return networks.get(0).getId();
        } else {
            Network defaultGuestNetwork = _networkService.getExclusiveGuestNetwork(zoneId);
            if (defaultGuestNetwork == null) {
                throw new InvalidParameterValueException("Unable to find a default Guest network for account " +
                        getAccountName() + " in domain id=" + getDomainId());
            } else {
                return defaultGuestNetwork.getId();
            }
        }
    }

    @Override
    public long getEntityOwnerId() {
        Account caller = UserContext.current().getCaller();
        if (accountName != null && domainId != null) {
            Account account = _accountService.finalizeOwner(caller, accountName, domainId, projectId);
            return account.getId();
        } else if (networkId != null){
            Network network = _networkService.getNetwork(networkId);
            return network.getAccountId();
        } else if (vpcId != null) {
            Vpc vpc = _vpcService.getVpc(getVpcId());
            if (vpc == null) {
                throw new InvalidParameterValueException("Can't find Enabled vpc by id specified");
            }
            return vpc.getAccountId();
        }

        return caller.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_IP_ASSIGN;
    }

    @Override
    public String getEventDescription() {
        return  "associating ip to network id: " + getNetworkId() + " in zone " + getZoneId();
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
    public void create() throws ResourceAllocationException{
        try {
            IpAddress ip =  _networkService.allocateIP(_accountService.getAccount(getEntityOwnerId()), false, getZoneId());

            if (ip != null) {
                this.setEntityId(ip.getId());
                this.setEntityUuid(ip.getUuid());
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to allocate ip address");
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
    public void execute() throws ResourceUnavailableException, ResourceAllocationException,
                                    ConcurrentOperationException, InsufficientCapacityException {
        UserContext.current().setEventDetails("Ip Id: " + getEntityId());

        IpAddress result = null;

        if (getVpcId() != null) {
            result = _vpcService.associateIPToVpc(getEntityId(), getVpcId());
        } else if (getNetworkId() != null) {
            result = _networkService.associateIPToNetwork(getEntityId(), getNetworkId());
        }

        if (result != null) {
            IPAddressResponse ipResponse = _responseGenerator.createIPAddressResponse(result);
            ipResponse.setResponseName(getCommandName());
            this.setResponseObject(ipResponse);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to assign ip address");
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
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.IpAddress;
    }

}
