package com.cloud.network;

import java.util.Date;

import com.cloud.acl.ControlledEntity;

public interface Site2SiteVpnGateway extends ControlledEntity {
    public long getId();
    public long getAddrId();
    public Date getRemoved();
}
