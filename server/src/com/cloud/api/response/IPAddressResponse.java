package com.cloud.api.response;

import java.util.Date;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class IPAddressResponse implements ResponseObject {
    @Param(name="ipaddress")
    private String ipAddress;

    @Param(name="allocated")
    private Date allocated;

    @Param(name="zoneid")
    private Long zoneId;

    @Param(name="zonename")
    private String zoneName;

    @Param(name="issourcenat")
    private Boolean sourceNat;

    @Param(name="account")
    private String accountName;

    @Param(name="domainid")
    private Long domainId;

    @Param(name="domain")
    private String domainName;

    @Param(name="forvirtualnetwork")
    private Boolean forVirtualNetwork;

    @Param(name="vlanid")
    private Long vlanId;

    @Param(name="vlanname")
    private String vlanName;

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Date getAllocated() {
        return allocated;
    }

    public void setAllocated(Date allocated) {
        this.allocated = allocated;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public Boolean getSourceNat() {
        return sourceNat;
    }

    public void setSourceNat(Boolean sourceNat) {
        this.sourceNat = sourceNat;
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

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Boolean getForVirtualNetwork() {
        return forVirtualNetwork;
    }

    public void setForVirtualNetwork(Boolean forVirtualNetwork) {
        this.forVirtualNetwork = forVirtualNetwork;
    }

    public Long getVlanId() {
        return vlanId;
    }

    public void setVlanId(Long vlanId) {
        this.vlanId = vlanId;
    }

    public String getVlanName() {
        return vlanName;
    }

    public void setVlanName(String vlanName) {
        this.vlanName = vlanName;
    }
}
