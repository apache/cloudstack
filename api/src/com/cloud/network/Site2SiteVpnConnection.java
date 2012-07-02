package com.cloud.network;

import java.util.Date;

public interface Site2SiteVpnConnection {
    enum State {
        Pending,
        Connected,
        Disconnected,
        Error,
    }
    public long getId();
    public long getVpnGatewayId();
    public long getCustomerGatewayId();
    public State getState();
    public Date getCreated();
    public Date getRemoved();
}
