package com.cloud.agent.api.routing;

public class Site2SiteVpnCfgCommand extends NetworkElementCommand {

    private boolean create;
    private String gatewayIp;
    private String guestIp;
    private String guestCidr;
    private String ipsecPsk;
    
	@Override
    public boolean executeInSequence() {
        return true;
    }
    
    public Site2SiteVpnCfgCommand () {
        this.create = false;
    }
    
    public Site2SiteVpnCfgCommand (boolean create, String gatewayIp, String guestIp, String guestCidr, String ipsecPsk) {
        this.create = create;
        this.gatewayIp = gatewayIp;
        this.guestIp = guestIp;
        this.guestCidr = guestCidr;
        this.ipsecPsk = ipsecPsk;
    }
    
    public boolean isCreate() {
        return create;
    }
    
    public void setCreate(boolean create) {
        this.create = create;
    }

    public String getGatewayIp() {
        return gatewayIp;
    }

    public void setGatewayIp(String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }

    public String getGuestIp() {
        return guestIp;
    }

    public void setGuestIp(String guestIp) {
        this.guestIp = guestIp;
    }

    public String getGuestCidr() {
        return guestCidr;
    }

    public void setGuestCidr(String guestCidr) {
        this.guestCidr = guestCidr;
    }

    public String getIpsecPsk() {
        return ipsecPsk;
    }

    public void setIpsecPsk(String ipsecPsk) {
        this.ipsecPsk = ipsecPsk;
    }
    
    
}
