package org.apache.cloudstack.ontapsvm;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Date;

public interface OntapSvm extends Identity, InternalIdentity {
    String getName();

    void setName(String name);

    String getiPv4Address();

    void setiPv4Address(String iPv4Address);

    int getVlan();

    void setVlan(int vlan);

    long getNetworkId();

    void setNetworkId(long networkId);

    Date getCreated();

    void setCreated(Date created);

    Date getRemoved();

    void setRemoved(Date removed);
}
