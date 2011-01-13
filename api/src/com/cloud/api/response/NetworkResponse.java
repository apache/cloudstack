package com.cloud.api.response;

import java.util.List;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class NetworkResponse extends BaseResponse{
    
    @SerializedName("id") @Param(description="the id of the network")
    private Long id;
    
    @SerializedName("name") @Param(description="the name of the network")
    private String name;
    
    @SerializedName("displaytext") @Param(description="the displaytext of the network")
    private String displaytext;
    
    @SerializedName("broadcastdomaintype") @Param(description="Broadcast domain type of the network")
    private String broadcastDomainType;
    
    @SerializedName("traffictype") @Param(description="the traffic type of the network")
    private String trafficType;
    
    @SerializedName("gateway") @Param(description="the network's gateway")
    private String gateway;
    
    @SerializedName("netmask") @Param(description="the network's netmask")
    private String netmask;
    
    @SerializedName("startip") @Param(description="the start ip of the network")
    private String startIp;

    @SerializedName("endip") @Param(description="the end ip of the network")
    private String endIp;
    
    @SerializedName("zoneid") @Param(description="zone id of the network")
    private Long zoneId;
    
    @SerializedName("networkofferingid") @Param(description="network offering id the network is created from")
    private Long networkOfferingId;
    
    @SerializedName("networkofferingname") @Param(description="name of the network offering the network is created from")
    private String networkOfferingName;
    
    @SerializedName("networkofferingdisplaytext") @Param(description="display text of the network offering the network is created from")
    private String networkOfferingDisplayText;
    
    @SerializedName("networkofferingavailability") @Param(description="availability of the network offering the network is created from")
    private String networkOfferingAvailability;
    
    @SerializedName("isshared") @Param(description="true if network is shared, false otherwise")
    private Boolean isShared;
    
    @SerializedName("issystem") @Param(description="true if network is system, false otherwise")
    private Boolean isSystem;
    
    @SerializedName("state") @Param(description="state of the network")
    private String state;

    @SerializedName("related") @Param(description="related to what other network configuration")
    private Long related;
    
    @SerializedName("broadcasturi") @Param(description="broadcast uri of the network")
    private String broadcastUri;
    
    @SerializedName("dns1") @Param(description="the first dns for the network")
    private String dns1;
    
    @SerializedName("dns2") @Param(description="the second dns for the network")
    private String dns2;
    
    @SerializedName("type") @Param(description="the type of the network")
    private String type;
    
    @SerializedName("vlan") @Param(description="the vlan of the network")
    private String vlan;
    
    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account associated with the network")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain id associated with the network")
    private Long domainId;
    
    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain associated with the network")
    private String domain;
    
    @SerializedName("isdefault") @Param(description="true if network is default, false otherwise")
    private Boolean isDefault;
    
    @SerializedName("service") @Param(description="the list of services", responseObject = ServiceResponse.class)
    private List<ServiceResponse> services;
    
    @SerializedName("networkdomain") @Param(description="the network domain")
    private String networkDomain;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBroadcastDomainType() {
        return broadcastDomainType;
    }

    public void setBroadcastDomainType(String broadcastDomainType) {
        this.broadcastDomainType = broadcastDomainType;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public Long getNetworkOfferingId() {
        return networkOfferingId;
    }

    public void setNetworkOfferingId(Long networkOfferingId) {
        this.networkOfferingId = networkOfferingId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getRelated() {
        return related;
    }

    public void setRelated(Long related) {
        this.related = related;
    }

    public String getBroadcastUri() {
        return broadcastUri;
    }

    public void setBroadcastUri(String broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    public String getDns1() {
        return dns1;
    }

    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getNetworkOfferingName() {
        return networkOfferingName;
    }

    public void setNetworkOfferingName(String networkOfferingName) {
        this.networkOfferingName = networkOfferingName;
    }

    public String getNetworkOfferingDisplayText() {
        return networkOfferingDisplayText;
    }

    public void setNetworkOfferingDisplayText(String networkOfferingDisplayText) {
        this.networkOfferingDisplayText = networkOfferingDisplayText;
    }

    public String getDisplaytext() {
        return displaytext;
    }

    public void setDisplaytext(String displaytext) {
        this.displaytext = displaytext;
    }

    public Boolean getIsShared() {
        return isShared;
    }

    public void setIsShared(Boolean isShared) {
        this.isShared = isShared;
    }

    public String getStartIp() {
        return startIp;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public String getEndIp() {
        return endIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public String getVlan() {
        return vlan;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

    public String getNetworkOfferingAvailability() {
        return networkOfferingAvailability;
    }

    public void setNetworkOfferingAvailability(String networkOfferingAvailability) {
        this.networkOfferingAvailability = networkOfferingAvailability;
    }

    public List<ServiceResponse> getServices() {
        return services;
    }

    public void setServices(List<ServiceResponse> services) {
        this.services = services;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getNetworkDomain() {
        return networkDomain;
    }

    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }
    
}
