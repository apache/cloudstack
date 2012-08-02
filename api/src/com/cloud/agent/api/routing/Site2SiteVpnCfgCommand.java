package com.cloud.agent.api.routing;

public class Site2SiteVpnCfgCommand extends NetworkElementCommand {

    private boolean create;
    private String localPublicIp;
    private String localGuestCidr;
    private String localPublicGateway;
    private String peerGatewayIp;
    private String peerGuestCidrList;
    private String ipsecPsk;
    private String ikePolicy;
    private String espPolicy;
    private long ikeLifetime;
    private long espLifetime;
    private boolean dpd;
    
	@Override
    public boolean executeInSequence() {
        return true;
    }
    
    public Site2SiteVpnCfgCommand () {
        this.create = false;
    }
    
    public Site2SiteVpnCfgCommand (boolean create, String localPublicIp, String localPublicGateway, String localGuestCidr, String peerGatewayIp,
            String peerGuestCidrList, String ikePolicy, String espPolicy, String ipsecPsk, Long ikeLifetime, Long espLifetime, Boolean dpd) {
        this.create = create;
        this.setLocalPublicIp(localPublicIp);
        this.setLocalPublicGateway(localPublicGateway);
        this.setLocalGuestCidr(localGuestCidr);
        this.setPeerGatewayIp(peerGatewayIp);
        this.setPeerGuestCidrList(peerGuestCidrList);
        this.ipsecPsk = ipsecPsk;
        this.ikePolicy = ikePolicy;
        this.espPolicy = espPolicy;
        this.ikeLifetime = ikeLifetime;
        this.espLifetime = espLifetime;
        this.dpd = dpd;
    }
    
    public boolean isCreate() {
        return create;
    }
    
    public void setCreate(boolean create) {
        this.create = create;
    }

    public String getIpsecPsk() {
        return ipsecPsk;
    }

    public void setIpsecPsk(String ipsecPsk) {
        this.ipsecPsk = ipsecPsk;
    }

    public String getIkePolicy() {
        return ikePolicy;
    }

    public void setIkePolicy(String ikePolicy) {
        this.ikePolicy = ikePolicy;
    }

    public String getEspPolicy() {
        return espPolicy;
    }

    public void setEspPolicy(String espPolicy) {
        this.espPolicy = espPolicy;
    }

    public long getIkeLifetime() {
        return ikeLifetime;
    }

    public void setikeLifetime(long ikeLifetime) {
        this.ikeLifetime = ikeLifetime;
    }

    public long getEspLifetime() {
        return espLifetime;
    }

    public void setEspLifetime(long espLifetime) {
        this.espLifetime = espLifetime;
    }

    public Boolean getDpd() {
        return dpd;
    }

    public void setDpd(Boolean dpd) {
        this.dpd = dpd;
    }

    public String getLocalPublicIp() {
        return localPublicIp;
    }

    public void setLocalPublicIp(String localPublicIp) {
        this.localPublicIp = localPublicIp;
    }

    public String getLocalGuestCidr() {
        return localGuestCidr;
    }

    public void setLocalGuestCidr(String localGuestCidr) {
        this.localGuestCidr = localGuestCidr;
    }

    public String getLocalPublicGateway() {
        return localPublicGateway;
    }

    public void setLocalPublicGateway(String localPublicGateway) {
        this.localPublicGateway = localPublicGateway;
    }

    public String getPeerGatewayIp() {
        return peerGatewayIp;
    }

    public void setPeerGatewayIp(String peerGatewayIp) {
        this.peerGatewayIp = peerGatewayIp;
    }

    public String getPeerGuestCidrList() {
        return peerGuestCidrList;
    }

    public void setPeerGuestCidrList(String peerGuestCidrList) {
        this.peerGuestCidrList = peerGuestCidrList;
    }
}
