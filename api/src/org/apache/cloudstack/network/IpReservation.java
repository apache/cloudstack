package org.apache.cloudstack.network;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Date;
import java.util.List;

public interface IpReservation extends Identity, InternalIdentity {
    List<String> getIpList();

    String getStartIp();

    void setStartIp(String startIp);

    String getEndIp();

    void setEndIp(String endIp);

    long getNetworkId();

    void setNetworkId(long networkId);

    Date getCreated();

    void setCreated(Date created);

    Date getRemoved();

    void setRemoved(Date removed);
}
