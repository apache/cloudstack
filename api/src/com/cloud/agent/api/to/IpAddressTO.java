package com.cloud.agent.api.to;


public class IpAddressTO {
    
    private String publicIp;
    private boolean sourceNat;
    private boolean add;
    private boolean oneToOneNat;
    private boolean firstIP;
    private String vlanId;
    private String vlanGateway;
    private String vlanNetmask;
    private String vifMacAddress;
    private String guestIp;
    private Integer networkRate;
    
    
    public IpAddressTO(String ipAddress, boolean add, boolean firstIP, boolean sourceNat, String vlanId, String vlanGateway, String vlanNetmask, String vifMacAddress, String guestIp, Integer networkRate) {
        this.publicIp = ipAddress;
        this.add = add;
        this.firstIP = firstIP;
        this.sourceNat = sourceNat;
        this.vlanId = vlanId;
        this.vlanGateway = vlanGateway;
        this.vlanNetmask = vlanNetmask;
        this.vifMacAddress = vifMacAddress;
        this.guestIp = guestIp;
        this.networkRate = networkRate;
    }
    
    protected IpAddressTO() {
    }

    public String getGuestIp(){
        return guestIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public boolean isAdd() {
        return add;
    }
    
    public boolean isOneToOneNat(){
        return this.oneToOneNat;
    }
    
    public boolean isFirstIP() {
        return firstIP;
    }

    public void setSourceNat(boolean sourceNat) {
        this.sourceNat = sourceNat;
    }

    public boolean isSourceNat() {
        return sourceNat;
    }
    
    public String getVlanId() {
        return vlanId;
    }
    
    public String getVlanGateway() {
        return vlanGateway;
    }
    
    public String getVlanNetmask() {
        return vlanNetmask;
    }
    
    public String getVifMacAddress() {
        return vifMacAddress;
    }
    
    public Integer getNetworkRate() {
        return networkRate;
    }

}
