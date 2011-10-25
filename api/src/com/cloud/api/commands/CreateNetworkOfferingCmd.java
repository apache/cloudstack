/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.NetworkOfferingResponse;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.user.Account;

@Implementation(description="Creates a network offering.", responseObject=NetworkOfferingResponse.class)
public class CreateNetworkOfferingCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateNetworkOfferingCmd.class.getName());
    private static final String _name = "createnetworkofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the network offering")
    private String networkOfferingName;
    
    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, required=true, description="the display text of the network offering")
    private String displayText;
    
    @Parameter(name=ApiConstants.TRAFFIC_TYPE, type=CommandType.STRING, required=true, description="the traffic type for the network offering, supported types are Public, Management, Control, Guest, Vlan or Storage.")
    private String traffictype;
    
    @Parameter(name=ApiConstants.MAX_CONNECTIONS, type=CommandType.INTEGER, description="maximum number of concurrent connections supported by the network offering")
    private Integer maxConnections;
    
    @Parameter(name=ApiConstants.TAGS, type=CommandType.STRING, description="the tags for the network offering.")
    private String tags; 
    
    @Parameter(name=ApiConstants.SPECIFY_VLAN, type=CommandType.BOOLEAN, description="true is network offering supports vlans")
    private Boolean specifyVlan; 
    
    @Parameter(name=ApiConstants.AVAILABILITY, type=CommandType.STRING, description="the availability of network offering. Default value is Required")
    private String availability; 
    
    @Parameter(name=ApiConstants.NETWORKRATE, type=CommandType.INTEGER, description="data transfer rate in megabits per second allowed.")
    private Integer networkRate;
    
    @Parameter(name=ApiConstants.DHCP_SERVICE, type=CommandType.BOOLEAN, description="true is network offering supports dhcp service")
    private Boolean dhcpService; 
    
    @Parameter(name=ApiConstants.DNS_SERVICE, type=CommandType.BOOLEAN, description="true is network offering supports dns service")
    private Boolean dnsService; 
    
    @Parameter(name=ApiConstants.GATEWAY_SERVICE, type=CommandType.BOOLEAN, description="true is network offering supports gateway service")
    private Boolean gatewayService; 
    
    @Parameter(name=ApiConstants.FIREWALL_SERVICE, type=CommandType.BOOLEAN, description="true is network offering supports firewall service")
    private Boolean firewallService; 
    
    @Parameter(name=ApiConstants.LB_SERVICE, type=CommandType.BOOLEAN, description="true is network offering supports lb service")
    private Boolean lbService; 
    
    @Parameter(name=ApiConstants.USERDATA_SERVICE, type=CommandType.BOOLEAN, description="true is network offering supports user data service")
    private Boolean userdataService;
    
    @Parameter(name=ApiConstants.SOURCE_NAT_SERVICE, type=CommandType.BOOLEAN, description="true is network offering supports source nat service")
    private Boolean sourceNatService;
    
    @Parameter(name=ApiConstants.VPN_SERVICE, type=CommandType.BOOLEAN, description="true is network offering supports vpn service")
    private Boolean vpnService;
    
    @Parameter(name = ApiConstants.SERVICE_PROVIDER_LIST, type = CommandType.MAP, description = "provider to service mapping. If not specified, the provider for the service will be mapped to the default provider on the physical network")
    private Map serviceProviderList;
    
    @Parameter(name=ApiConstants.SECURITY_GROUP_EANBLED, type=CommandType.BOOLEAN, description="true is security group is enabled for the network offering")
    private Boolean securityGroupEnabled;
    
    @Parameter(name=ApiConstants.TYPE, type=CommandType.STRING, required=true, description="type of the network offering: Shared or Isolated")
    private String type;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public String getNetworkOfferingName() {
        return networkOfferingName;
    }
    
    public String getDisplayText() {
        return displayText;
    }

    public String getTags() {
        return tags;
    }

    public String getTraffictype() {
        return traffictype;
    }

    public Integer getMaxconnections() {
        return maxConnections;
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

    public static String getName() {
        return _name;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public Boolean getDhcpService() {
        return dhcpService == null ? false : dhcpService;
    }

    public Boolean getDnsService() {
        return dnsService == null ? false : dnsService;
    }

    public Boolean getGatewayService() {
        return gatewayService == null ? false : gatewayService;
    }

    public Boolean getFirewallService() {
        return firewallService == null ? false : firewallService;
    }

    public Boolean getLbService() {
        return lbService == null ? false : lbService;
    }

    public Boolean getUserdataService() {
        return userdataService == null ? false : userdataService;
    }

    public Boolean getSourceNatService() {
        return sourceNatService == null ? false : sourceNatService;
    }

    public Boolean getVpnService() {
        return vpnService == null ? false : vpnService;
    }

    public String getType() {
        return type;
    }

    public Map<String, List<String>> getServiceProviders() {
        Map<String, List<String>> serviceProviderMap = null;
        if (serviceProviderList != null && !serviceProviderList.isEmpty()) {
            serviceProviderMap = new HashMap<String, List<String>>();
            Collection servicesCollection = serviceProviderList.values();
            Iterator iter = servicesCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> services = (HashMap<String, String>) iter.next();
                String service = (String)services.get("service");
                String provider = (String) services.get("provider");
                List<String> providerList = null;
                if (serviceProviderMap.containsKey(service)) {
                    providerList = serviceProviderMap.get(service);
                } else {
                    providerList = new ArrayList<String>();
                }
                providerList.add(provider);
                serviceProviderMap.put(service, providerList);
            }
        }
        
        return serviceProviderMap;
    }

    public Boolean getSecurityGroupEnabled() {
        return securityGroupEnabled == null ? false : securityGroupEnabled;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return _name;
    }
    
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        NetworkOffering result = _configService.createNetworkOffering(this);
        if (result != null) {
            NetworkOfferingResponse response = _responseGenerator.createNetworkOfferingResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create network offering");
        }
    }
}
