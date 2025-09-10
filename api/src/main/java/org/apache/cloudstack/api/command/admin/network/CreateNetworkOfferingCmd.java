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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.cloud.network.Network;
import com.cloud.network.VirtualRouterProvider;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.user.Account;

import static com.cloud.network.Network.Service.Dhcp;
import static com.cloud.network.Network.Service.Dns;
import static com.cloud.network.Network.Service.Lb;
import static com.cloud.network.Network.Service.StaticNat;
import static com.cloud.network.Network.Service.SourceNat;
import static com.cloud.network.Network.Service.PortForwarding;
import static com.cloud.network.Network.Service.NetworkACL;
import static com.cloud.network.Network.Service.UserData;
import static com.cloud.network.Network.Service.Firewall;

@APICommand(name = "createNetworkOffering", description = "Creates a network offering.", responseObject = NetworkOfferingResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateNetworkOfferingCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the network offering")
    private String networkOfferingName;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "the display text of the network offering, defaults to the value of 'name'.")
    private String displayText;

    @Parameter(name = ApiConstants.TRAFFIC_TYPE,
            type = CommandType.STRING,
            required = true,
            description = "the traffic type for the network offering. Supported type in current release is GUEST only")
    private String traffictype;

    @Parameter(name = ApiConstants.TAGS, type = CommandType.STRING, description = "the tags for the network offering.", length = 4096)
    private String tags;

    @Parameter(name = ApiConstants.SPECIFY_VLAN, type = CommandType.BOOLEAN, description = "true if network offering supports vlans")
    private Boolean specifyVlan;

    @Parameter(name = ApiConstants.AVAILABILITY, type = CommandType.STRING, description = "the availability of network offering. The default value is Optional. "
            + " Another value is Required, which will make it as the default network offering for new networks ")
    private String availability;

    @Parameter(name = ApiConstants.NETWORKRATE, type = CommandType.INTEGER, description = "data transfer rate in megabits per second allowed")
    private Integer networkRate;

    @Parameter(name = ApiConstants.CONSERVE_MODE, type = CommandType.BOOLEAN, description = "true if the network offering is IP conserve mode enabled")
    private Boolean conserveMode;

    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID,
            type = CommandType.UUID,
            entityType = ServiceOfferingResponse.class,
            description = "the service offering ID used by virtual router provider")
    private Long serviceOfferingId;

    @Parameter(name = ApiConstants.GUEST_IP_TYPE, type = CommandType.STRING, required = true, description = "guest type of the network offering: Shared or Isolated")
    private String guestIptype;

    @Parameter(name = ApiConstants.INTERNET_PROTOCOL,
            type = CommandType.STRING,
            description = "The internet protocol of network offering. Options are ipv4 and dualstack. Default is ipv4. dualstack will create a network offering that supports both IPv4 and IPv6",
            since = "4.17.0")
    private String internetProtocol;

    @Parameter(name = ApiConstants.SUPPORTED_SERVICES,
            type = CommandType.LIST,
            collectionType = CommandType.STRING,
            description = "services supported by the network offering")
    private List<String> supportedServices;

    @Parameter(name = ApiConstants.SERVICE_PROVIDER_LIST,
            type = CommandType.MAP,
            description = "provider to service mapping. If not specified, the provider for the service will be mapped to the default provider on the physical network")
    private Map serviceProviderList;

    @Parameter(name = ApiConstants.SERVICE_CAPABILITY_LIST, type = CommandType.MAP, description = "desired service capabilities as part of network offering")
    private Map serviceCapabilitystList;

    @Parameter(name = ApiConstants.SPECIFY_IP_RANGES,
            type = CommandType.BOOLEAN,
            description = "true if network offering supports specifying ip ranges; defaulted to false if not specified")
    private Boolean specifyIpRanges;

    @Parameter(name = ApiConstants.IS_PERSISTENT,
            type = CommandType.BOOLEAN,
            description = "true if network offering supports persistent networks; defaulted to false if not specified")
    private Boolean isPersistent;

    @Parameter(name = ApiConstants.FOR_VPC,
            type = CommandType.BOOLEAN,
            description = "true if network offering is meant to be used for VPC, false otherwise.")
    private Boolean forVpc;

    @Parameter(name = ApiConstants.FOR_NSX,
            type = CommandType.BOOLEAN,
            description = "true if network offering is meant to be used for NSX, false otherwise.",
            since = "4.20.0")
    private Boolean forNsx;

    @Parameter(name = ApiConstants.NSX_SUPPORT_LB,
            type = CommandType.BOOLEAN,
            description = "true if network offering for NSX network offering supports Load balancer service.",
            since = "4.20.0")
    private Boolean nsxSupportsLbService;

    @Parameter(name = ApiConstants.NSX_SUPPORTS_INTERNAL_LB,
            type = CommandType.BOOLEAN,
            description = "true if network offering for NSX network offering supports Internal Load balancer service.",
            since = "4.20.0")
    private Boolean nsxSupportsInternalLbService;

    @Parameter(name = ApiConstants.NETWORK_MODE,
            type = CommandType.STRING,
            description = "Indicates the mode with which the network will operate. Valid option: NATTED or ROUTED",
            since = "4.20.0")
    private String networkMode;

    @Parameter(name = ApiConstants.FOR_TUNGSTEN,
            type = CommandType.BOOLEAN,
            description = "true if network offering is meant to be used for Tungsten-Fabric, false otherwise.")
    private Boolean forTungsten;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, since = "4.2.0", description = "Network offering details in key/value pairs."
            + " Supported keys are internallbprovider/publiclbprovider with service provider as a value, and"
            + " promiscuousmode/macaddresschanges/forgedtransmits with true/false as value to accept/reject the security settings if available for a nic/portgroup")
    protected Map details;

    @Parameter(name = ApiConstants.EGRESS_DEFAULT_POLICY,
            type = CommandType.BOOLEAN,
            description = "true if guest network default egress policy is allow; false if default egress policy is deny")
    private Boolean egressDefaultPolicy;

    @Parameter(name = ApiConstants.KEEPALIVE_ENABLED,
            type = CommandType.BOOLEAN,
            required = false,
            description = "if true keepalive will be turned on in the loadbalancer. At the time of writing this has only an effect on haproxy; the mode http and httpclose options are unset in the haproxy conf file.")
    private Boolean keepAliveEnabled;

    @Parameter(name = ApiConstants.MAX_CONNECTIONS,
            type = CommandType.INTEGER,
            description = "maximum number of concurrent connections supported by the network offering")
    private Integer maxConnections;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "the ID of the containing domain(s), null for public offerings")
    private List<Long> domainIds;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "the ID of the containing zone(s), null for public offerings",
            since = "4.13")
    private List<Long> zoneIds;

    @Parameter(name = ApiConstants.ENABLE,
            type = CommandType.BOOLEAN,
            description = "set to true if the offering is to be enabled during creation. Default is false",
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

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getNetworkOfferingName() {
        return networkOfferingName;
    }

    public String getDisplayText() {
        return StringUtils.isEmpty(displayText) ? networkOfferingName : displayText;
    }

    public String getTags() {
        return tags;
    }

    public String getTraffictype() {
        return traffictype;
    }

    public Boolean getSpecifyVlan() {
        return specifyVlan == null ? false : specifyVlan;
    }

    public String getAvailability() {
        return availability == null ? Availability.Optional.toString() : availability;
    }

    public Integer getNetworkRate() {
        return networkRate;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public List<String> getSupportedServices() {
        if (!isForNsx()) {
            return supportedServices == null ? new ArrayList<String>() : supportedServices;
        } else {
            List<String> services = new ArrayList<>(List.of(
                    Dhcp.getName(),
                    Dns.getName(),
                    StaticNat.getName(),
                    SourceNat.getName(),
                    PortForwarding.getName(),
                    UserData.getName()
            ));
            if (getNsxSupportsLbService()) {
                services.add(Lb.getName());
            }
            if (Boolean.TRUE.equals(forVpc)) {
                services.add(NetworkACL.getName());
            } else {
                services.add(Firewall.getName());
            }
            return services;
        }
    }

    public String getGuestIpType() {
        return guestIptype;
    }

    public String getInternetProtocol() {
        return internetProtocol;
    }

    public Boolean getSpecifyIpRanges() {
        return specifyIpRanges == null ? false : specifyIpRanges;
    }

    public Boolean getConserveMode() {
        if (conserveMode == null) {
            return true;
        }
        return conserveMode;
    }

    public Boolean getIsPersistent() {
        return isPersistent == null ? false : isPersistent;
    }

    public Boolean getForVpc() {
        return forVpc;
    }

    public boolean isForNsx() {
        return BooleanUtils.isTrue(forNsx);
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public boolean getNsxSupportsLbService() {
        return BooleanUtils.isTrue(nsxSupportsLbService);
    }

    public boolean getNsxSupportsInternalLbService() {
        return BooleanUtils.isTrue(nsxSupportsInternalLbService);
    }

    public Boolean getForTungsten() {
        return forTungsten;
    }

    public Boolean getEgressDefaultPolicy() {
        if (egressDefaultPolicy == null) {
            return true;
        }
        return egressDefaultPolicy;
    }

    public Boolean getKeepAliveEnabled() {
        return keepAliveEnabled;
    }

    public Integer getMaxconnections() {
        return maxConnections;
    }

    public Map<String, List<String>> getServiceProviders() {
        Map<String, List<String>> serviceProviderMap = new HashMap<>();
        if (serviceProviderList != null && !serviceProviderList.isEmpty() && !isForNsx()) {
            Collection servicesCollection = serviceProviderList.values();
            Iterator iter = servicesCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> services = (HashMap<String, String>) iter.next();
                String service = services.get("service");
                String provider = services.get("provider");
                List<String> providerList = null;
                if (serviceProviderMap.containsKey(service)) {
                    providerList = serviceProviderMap.get(service);
                } else {
                    providerList = new ArrayList<String>();
                }
                providerList.add(provider);
                serviceProviderMap.put(service, providerList);
            }
        } else if (Boolean.TRUE.equals(forNsx)) {
            getServiceProviderMapForNsx(serviceProviderMap);
        }
        return serviceProviderMap;
    }

    private void getServiceProviderMapForNsx(Map<String, List<String>> serviceProviderMap) {
        String routerProvider = Boolean.TRUE.equals(getForVpc()) ? VirtualRouterProvider.Type.VPCVirtualRouter.name() :
                VirtualRouterProvider.Type.VirtualRouter.name();
        List<String> unsupportedServices = new ArrayList<>(List.of("Vpn", "SecurityGroup", "Connectivity",
                "Gateway", "BaremetalPxeService"));
        List<String> routerSupported = List.of("Dhcp", "Dns", "UserData");
        List<String> allServices = Service.listAllServices().stream().map(Service::getName).collect(Collectors.toList());
        if (routerProvider.equals(VirtualRouterProvider.Type.VPCVirtualRouter.name())) {
            unsupportedServices.add("Firewall");
        } else {
            unsupportedServices.add("NetworkACL");
        }
        for (String service : allServices) {
            if (unsupportedServices.contains(service))
                continue;
            if (routerSupported.contains(service))
                serviceProviderMap.put(service, List.of(routerProvider));
            else
                serviceProviderMap.put(service, List.of(Network.Provider.Nsx.getName()));
            if (!getNsxSupportsLbService()) {
                serviceProviderMap.remove(Lb.getName());
            }
        }
    }

    public Map<Capability, String> getServiceCapabilities(Service service) {
        Map<Capability, String> capabilityMap = null;

        if (serviceCapabilitystList != null && !serviceCapabilitystList.isEmpty()) {
            capabilityMap = new HashMap<Capability, String>();
            Collection serviceCapabilityCollection = serviceCapabilitystList.values();
            Iterator iter = serviceCapabilityCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> svcCapabilityMap = (HashMap<String, String>) iter.next();
                Capability capability = null;
                String svc = svcCapabilityMap.get("service");
                String capabilityName = svcCapabilityMap.get("capabilitytype");
                String capabilityValue = svcCapabilityMap.get("capabilityvalue");

                if (capabilityName != null) {
                    capability = Capability.getCapability(capabilityName);
                }

                if ((capability == null) || (capabilityName == null) || (capabilityValue == null)) {
                    throw new InvalidParameterValueException("Invalid capability:" + capabilityName + " capability value:" + capabilityValue);
                }

                if (svc.equalsIgnoreCase(service.getName())) {
                    capabilityMap.put(capability, capabilityValue);
                } else {
                    //throw new InvalidParameterValueException("Service is not equal ")
                }
            }
        }

        return capabilityMap;
    }

    public Map<String, String> getDetails() {
        if (details == null || details.isEmpty()) {
            return null;
        }

        Collection paramsCollection = details.values();
        Object objlist[] = paramsCollection.toArray();
        Map<String, String> params = (Map<String, String>) (objlist[0]);
        for (int i = 1; i < objlist.length; i++) {
            params.putAll((Map<String, String>) (objlist[i]));
        }

        return params;
    }

    public String getServicePackageId() {
        Map<String, String> data = getDetails();
        if (data == null)
            return null;
        return data.get(NetworkOffering.Detail.servicepackageuuid + "");
    }

    public List<Long> getDomainIds() {
        if (CollectionUtils.isNotEmpty(domainIds)) {
            Set<Long> set = new LinkedHashSet<>(domainIds);
            domainIds.clear();
            domainIds.addAll(set);
        }
        return domainIds;
    }

    public List<Long> getZoneIds() {
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            Set<Long> set = new LinkedHashSet<>(zoneIds);
            zoneIds.clear();
            zoneIds.addAll(set);
        }
        return zoneIds;
    }

    public Boolean getEnable() {
        if (enable != null) {
            return enable;
        }
        return false;
    }

    public boolean getSpecifyAsNumber() {
        return BooleanUtils.toBoolean(specifyAsNumber);
    }

    public String getRoutingMode() {
        return routingMode;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

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
