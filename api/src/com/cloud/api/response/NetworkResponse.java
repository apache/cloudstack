package com.cloud.api.response;

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
    
    //TODO - add description
    @SerializedName("broadcastdomaintype")
    private String broadcastDomainType;
    
    //TODO - add description
    @SerializedName("traffictype")
    private String trafficType;
    
    //TODO - add description
    @SerializedName("gateway")
    private String gateway;
    
    //TODO - add description
    @SerializedName("netmask")
    private String netmask;
    
    @SerializedName("startip") @Param(description="the start ip of the network")
    private String startIp;

    @SerializedName("endip") @Param(description="the end ip of the network")
    private String endIp;
    
    //TODO - add description
    @SerializedName("zoneid")
    private Long zoneId;
    
    //TODO - add description
    @SerializedName("networkofferingid")
    private Long networkOfferingId;
    
    //TODO - add description
    @SerializedName("networkofferingname")
    private String networkOfferingName;
    
    //TODO - add description
    @SerializedName("networkofferingdisplaytext")
    private String networkOfferingDisplayText;
    
  //TODO - add description
    @SerializedName("isshared")
    private Boolean isShared;
    
    //TODO - add description
    @SerializedName("issystem")
    private Boolean isSystem;
    
    //TODO - add description
    @SerializedName("state")
    private String state;
    
    //TODO - add description
    @SerializedName("related")
    private Long related;
    
    //TODO - add description
    @SerializedName("broadcasturi")
    private String broadcastUri;
    
    //TODO - add description
    @SerializedName("dns1")
    private String dns1;
    
    //TODO - add description
    @SerializedName("dns2")
    private String dns2;
    
    //TODO - add description
    @SerializedName("type")
    private String type;
    
    //TODO - add description
    @SerializedName("vlan")
    private String vlan;
    
    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account associated with the network")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain id associated with the network")
    private Long domainId;
    
    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain associated with the network")
    private String domain;

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
    
    
}
