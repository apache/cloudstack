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
package org.apache.cloudstack.api.command.admin.vpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.offering.NetworkOffering;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.VpcOfferingResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.user.Account;

import static com.cloud.network.Network.Service.Dhcp;
import static com.cloud.network.Network.Service.Dns;
import static com.cloud.network.Network.Service.Lb;
import static com.cloud.network.Network.Service.StaticNat;
import static com.cloud.network.Network.Service.SourceNat;
import static com.cloud.network.Network.Service.PortForwarding;
import static com.cloud.network.Network.Service.NetworkACL;
import static com.cloud.network.Network.Service.UserData;
import static com.cloud.network.Network.Service.Gateway;

import static org.apache.cloudstack.api.command.utils.OfferingUtils.isNetrisNatted;
import static org.apache.cloudstack.api.command.utils.OfferingUtils.isNetrisRouted;
import static org.apache.cloudstack.api.command.utils.OfferingUtils.isNsxWithoutLb;

@APICommand(name = "createVPCOffering", description = "Creates VPC offering", responseObject = VpcOfferingResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateVPCOfferingCmd extends BaseAsyncCreateCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the vpc offering")
    private String vpcOfferingName;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "the display text of the vpc offering, defaults to the 'name'")
    private String displayText;

    @Parameter(name = ApiConstants.SUPPORTED_SERVICES,
               type = CommandType.LIST,
               collectionType = CommandType.STRING,
               description = "services supported by the vpc offering")
    private List<String> supportedServices;

    @Parameter(name = ApiConstants.SERVICE_PROVIDER_LIST, type = CommandType.MAP, description = "provider to service mapping. "
        + "If not specified, the provider for the service will be mapped to the default provider on the physical network")
    private Map<String, ? extends Map<String, String>> serviceProviderList;

    @Parameter(name = ApiConstants.SERVICE_CAPABILITY_LIST, type = CommandType.MAP, description = "desired service capabilities as part of vpc offering", since = "4.4")
    private Map<String, List<String>> serviceCapabilityList;

    @Parameter(name = ApiConstants.INTERNET_PROTOCOL,
            type = CommandType.STRING,
            description = "The internet protocol of the offering. Options are ipv4 and dualstack. Default is ipv4. dualstack will create an offering that supports both IPv4 and IPv6",
            since = "4.17.0")
    private String internetProtocol;

    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID,
               type = CommandType.UUID,
               entityType = ServiceOfferingResponse.class,
               description = "the ID of the service offering for the VPC router appliance")
    private Long serviceOfferingId;

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

    @Deprecated
    @Parameter(name = ApiConstants.FOR_NSX,
            type = CommandType.BOOLEAN,
            description = "true if network offering is meant to be used for NSX, false otherwise.",
            since = "4.20.0")
    private Boolean forNsx;

    @Parameter(name = ApiConstants.PROVIDER,
            type = CommandType.STRING,
            description = "Name of the provider providing the service",
            since = "4.21.0")
    private String provider;

    @Parameter(name = ApiConstants.NSX_SUPPORT_LB,
            type = CommandType.BOOLEAN,
            description = "true if network offering for NSX VPC offering supports Load balancer service.",
            since = "4.20.0")
    private Boolean nsxSupportsLbService;

    @Parameter(name = ApiConstants.ENABLE,
            type = CommandType.BOOLEAN,
            description = "set to true if the offering is to be enabled during creation. Default is false",
            since = "4.16")
    private Boolean enable;

    @Parameter(name = ApiConstants.NETWORK_MODE,
            type = CommandType.STRING,
            description = "Indicates the mode with which the network will operate. Valid option: NATTED or ROUTED",
            since = "4.20.0")
    private String networkMode;

    @Parameter(name = ApiConstants.SPECIFY_AS_NUMBER, type = CommandType.BOOLEAN, since = "4.20.0",
            description = "true if the VPC offering supports choosing AS number")
    private Boolean specifyAsNumber;

    @Parameter(name = ApiConstants.ROUTING_MODE,
            type = CommandType.STRING,
            since = "4.20.0",
            description = "the routing mode for the VPC offering. Supported types are: Static or Dynamic.")
    private String routingMode;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getVpcOfferingName() {
        return vpcOfferingName;
    }

    public String getDisplayText() {
        return StringUtils.isEmpty(displayText) ? vpcOfferingName : displayText;
    }

    public boolean isExternalNetworkProvider() {
        return Arrays.asList("NSX", "Netris").stream()
                .anyMatch(s -> provider != null && s.equalsIgnoreCase(provider));
    }

    public List<String> getSupportedServices() {
        if (!isExternalNetworkProvider() && CollectionUtils.isEmpty(supportedServices)) {
            throw new InvalidParameterValueException("Supported services needs to be provided");
        }
        if (isExternalNetworkProvider()) {
            supportedServices = new ArrayList<>(List.of(
                    Dhcp.getName(),
                    Dns.getName(),
                    NetworkACL.getName(),
                    UserData.getName()
                    ));
            if (NetworkOffering.NetworkMode.NATTED.name().equalsIgnoreCase(getNetworkMode())) {
                supportedServices.addAll(Arrays.asList(
                        StaticNat.getName(),
                        SourceNat.getName(),
                        PortForwarding.getName()));
            }
            if (NetworkOffering.NetworkMode.ROUTED.name().equalsIgnoreCase(getNetworkMode())) {
                supportedServices.add(Gateway.getName());
            }
            if (getNsxSupportsLbService() || isNetrisNatted(getProvider(), getNetworkMode())) {
                supportedServices.add(Lb.getName());
            }
        }
        return supportedServices;
    }

    public String getProvider() {
        return provider;
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public boolean getNsxSupportsLbService() {
        return org.apache.commons.lang3.BooleanUtils.isTrue(nsxSupportsLbService);
    }

    public Map<String, List<String>> getServiceProviders() {
        Map<String, List<String>> serviceProviderMap = new HashMap<>();
        if (serviceProviderList != null && !serviceProviderList.isEmpty() && !isExternalNetworkProvider()) {
            Collection<? extends Map<String, String>> servicesCollection = serviceProviderList.values();
            Iterator<? extends Map<String, String>> iter = servicesCollection.iterator();
            while (iter.hasNext()) {
                Map<String, String> obj = iter.next();
                if (logger.isTraceEnabled()) {
                    logger.trace("service provider entry specified: " + obj);
                }
                HashMap<String, String> services = (HashMap<String, String>) obj;
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
        } else if (isExternalNetworkProvider()) {
            getServiceProviderMapForExternalProvider(serviceProviderMap, Network.Provider.getProvider(provider).getName());
        }

        return serviceProviderMap;
    }

    private void getServiceProviderMapForExternalProvider(Map<String, List<String>> serviceProviderMap, String provider) {
        List<String> unsupportedServices = new ArrayList<>(List.of("Vpn", "BaremetalPxeService", "SecurityGroup", "Connectivity", "Firewall"));
        if (NetworkOffering.NetworkMode.NATTED.name().equalsIgnoreCase(getNetworkMode())) {
            unsupportedServices.add("Gateway");
        }
        List<String> routerSupported = List.of("Dhcp", "Dns", "UserData");
        List<String> allServices = Network.Service.listAllServices().stream().map(Network.Service::getName).collect(Collectors.toList());
        for (String service : allServices) {
            if (unsupportedServices.contains(service))
                continue;
            if (routerSupported.contains(service))
                serviceProviderMap.put(service, List.of(VirtualRouterProvider.Type.VPCVirtualRouter.name()));
            else if (NetworkOffering.NetworkMode.NATTED.name().equalsIgnoreCase(getNetworkMode()) ||
                    Stream.of(NetworkACL.getName(), Gateway.getName()).anyMatch(s -> s.equalsIgnoreCase(service))) {
                serviceProviderMap.put(service, List.of(provider));
            }
        }
        if ((isNsxWithoutLb(getProvider(), getNsxSupportsLbService())) || isNetrisRouted(getProvider(), getNetworkMode())) {
            serviceProviderMap.remove(Lb.getName());
        }
    }

    public Map<String, List<String>> getServiceCapabilityList() {
        return serviceCapabilityList;
    }

    public String getInternetProtocol() {
        return internetProtocol;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
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

    public Boolean getSpecifyAsNumber() {
        return BooleanUtils.toBoolean(specifyAsNumber);
    }

    public String getRoutingMode() {
        return routingMode;
    }

    @Override
    public void create() throws ResourceAllocationException {
        VpcOffering vpcOff = _vpcProvSvc.createVpcOffering(this);
        if (vpcOff != null) {
            setEntityId(vpcOff.getId());
            setEntityUuid(vpcOff.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create a VPC offering");
        }
    }

    @Override
    public void execute() {
        VpcOffering vpc = _vpcProvSvc.getVpcOffering(getEntityId());
        if (vpc != null) {
            VpcOfferingResponse response = _responseGenerator.createVpcOfferingResponse(vpc);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create VPC offering");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VPC_OFFERING_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating VPC offering. Id: " + getEntityId();
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
