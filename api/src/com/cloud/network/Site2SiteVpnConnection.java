package com.cloud.network;

import java.util.Date;

import com.cloud.acl.ControlledEntity;

public interface Site2SiteVpnConnection extends ControlledEntity {
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
