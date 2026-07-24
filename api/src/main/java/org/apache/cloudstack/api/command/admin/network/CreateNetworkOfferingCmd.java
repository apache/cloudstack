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
package org.apache.cloudstack.api.command.admin.network;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;

import com.cloud.offering.NetworkOffering;

@APICommand(name = "createNetworkOffering", description = "Creates a network offering.", responseObject = NetworkOfferingResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateNetworkOfferingCmd extends NetworkOfferingBaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.TRAFFIC_TYPE,
            type = CommandType.STRING,
            required = true,
            description = "The traffic type for the network offering. Supported type in current release is GUEST only")
    private String traffictype;

    @Parameter(name = ApiConstants.GUEST_IP_TYPE, type = CommandType.STRING, required = true, description = "Guest type of the network offering: Shared or Isolated")
    private String guestIptype;

<<<<<<< Updated upstream
=======
    @Parameter(name = ApiConstants.INTERNET_PROTOCOL,
            type = CommandType.STRING,
            description = "The internet protocol of network offering. Options are IPv4 and dualstack. Default is IPv4. dualstack will create a network offering that supports both IPv4 and IPv6",
            since = "4.17.0")
    private String internetProtocol;

    @Parameter(name = ApiConstants.SUPPORTED_SERVICES,
            type = CommandType.LIST,
            collectionType = CommandType.STRING,
            description = "Services supported by the network offering")
    private List<String> supportedServices;

    @Parameter(name = ApiConstants.SERVICE_PROVIDER_LIST,
            type = CommandType.MAP,
            description = "Provider to service mapping. If not specified, the provider for the service will be mapped to the default provider on the physical network")
    private Map serviceProviderList;

    @Parameter(name = ApiConstants.SERVICE_CAPABILITY_LIST, type = CommandType.MAP, description = "Desired service capabilities as part of network offering")
    private Map serviceCapabilitystList;

    @Parameter(name = ApiConstants.SPECIFY_IP_RANGES,
            type = CommandType.BOOLEAN,
            description = "True if network offering supports specifying ip ranges; defaulted to false if not specified")
    private Boolean specifyIpRanges;

    @Parameter(name = ApiConstants.IS_PERSISTENT,
            type = CommandType.BOOLEAN,
            description = "True if network offering supports persistent networks; defaulted to false if not specified")
    private Boolean isPersistent;

    @Parameter(name = ApiConstants.FOR_VPC,
            type = CommandType.BOOLEAN,
            description = "True if network offering is meant to be used for VPC, false otherwise.")
    private Boolean forVpc;

    @Parameter(name = ApiConstants.FOR_NSX,
            type = CommandType.BOOLEAN,
            description = "true if network offering is meant to be used for NSX, false otherwise.",
            since = "4.20.0")
    private Boolean forNsx;

    @Parameter(name = ApiConstants.NSX_SUPPORT_LB,
            type = CommandType.BOOLEAN,
            description = "True if network offering for NSX network offering supports Load balancer service.",
            since = "4.20.0")
    private Boolean nsxSupportsLbService;

    @Parameter(name = ApiConstants.NSX_SUPPORTS_INTERNAL_LB,
            type = CommandType.BOOLEAN,
            description = "True if network offering for NSX network offering supports Internal Load balancer service.",
            since = "4.20.0")
    private Boolean nsxSupportsInternalLbService;

    @Parameter(
    name = ApiConstants.NETWORK_MODE,
    type = CommandType.STRING,
    required = true,
    description = "the network mode of the network offering, possible values are NATTED and ROUTED"
    )
    private String networkMode;

    @Parameter(name = ApiConstants.FOR_TUNGSTEN,
            type = CommandType.BOOLEAN,
            description = "True if network offering is meant to be used for Tungsten-Fabric, false otherwise.")
    private Boolean forTungsten;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, since = "4.2.0", description = "Network offering details in key/value pairs."
            + " Supported keys are internallbprovider/publiclbprovider with service provider as a value, and"
            + " promiscuousmode/macaddresschanges/forgedtransmits with true/false as value to accept/reject the security settings if available for a nic/portgroup")
    protected Map details;

    @Parameter(name = ApiConstants.EGRESS_DEFAULT_POLICY,
            type = CommandType.BOOLEAN,
            description = "True if guest network default egress policy is allow; false if default egress policy is deny")
    private Boolean egressDefaultPolicy;

    @Parameter(name = ApiConstants.KEEPALIVE_ENABLED,
            type = CommandType.BOOLEAN,
            required = false,
            description = "If true keepalive will be turned on in the loadbalancer. At the time of writing this has only an effect on haproxy; the mode http and httpclose options are unset in the haproxy conf file.")
    private Boolean keepAliveEnabled;

    @Parameter(name = ApiConstants.MAX_CONNECTIONS,
            type = CommandType.INTEGER,
            description = "Maximum number of concurrent connections supported by the Network offering")
    private Integer maxConnections;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "The ID of the containing domain(s), null for public offerings")
    private List<Long> domainIds;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "The ID of the containing zone(s), null for public offerings",
            since = "4.13")
    private List<Long> zoneIds;

    @Parameter(name = ApiConstants.ENABLE,
            type = CommandType.BOOLEAN,
            description = "Set to true if the offering is to be enabled during creation. Default is false",
            since = "4.16")
    private Boolean enable;

    @Parameter(name = ApiConstants.SPECIFY_AS_NUMBER, type = CommandType.BOOLEAN, since = "4.20.0",
            description = "true if network offering supports choosing AS number")
    private Boolean specifyAsNumber;

    @Parameter(name = ApiConstants.ROUTING_MODE,
            type = CommandType.STRING,
            since = "4.20.0",
            description = "the routing mode for the network offering. Supported types are: Static or Dynamic.")
    private String routingMode;

>>>>>>> Stashed changes
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getTraffictype() {
        return traffictype;
    }

    public String getGuestIpType() {
        return guestIptype;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        NetworkOffering result = _configService.createNetworkOffering(this);
        if (result != null) {
            NetworkOfferingResponse response = _responseGenerator.createNetworkOfferingResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create network offering");
        }
    }
}
