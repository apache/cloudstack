package com.cloud.network;

import java.util.Date;

public interface Site2SiteCustomerGateway {
    public long getId();
    public String getGatewayIp();
    public String getGuestCidrList();
    public String getIpsecPsk();
    public Date getRemoved();
}
