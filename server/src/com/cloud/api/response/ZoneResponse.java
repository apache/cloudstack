package com.cloud.api.response;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class ZoneResponse implements ResponseObject {
    @Param(name="id")
    private Long id;

    @Param(name="name")
    private String name;

    @Param(name="dns1")
    private String dns1;

    @Param(name="dns2")
    private String dns2;

    @Param(name="internaldns1")
    private String internalDns1;

    @Param(name="internaldns2")
    private String internalDns2;

    @Param(name="vlan")
    private String vlan;

    @Param(name="guestcidraddress")
    private String guestCidrAddress;

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

    public String getInternalDns1() {
        return internalDns1;
    }

    public void setInternalDns1(String internalDns1) {
        this.internalDns1 = internalDns1;
    }

    public String getInternalDns2() {
        return internalDns2;
    }

    public void setInternalDns2(String internalDns2) {
        this.internalDns2 = internalDns2;
    }

    public String getVlan() {
        return vlan;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public String getGuestCidrAddress() {
        return guestCidrAddress;
    }

    public void setGuestCidrAddress(String guestCidrAddress) {
        this.guestCidrAddress = guestCidrAddress;
    }
}
