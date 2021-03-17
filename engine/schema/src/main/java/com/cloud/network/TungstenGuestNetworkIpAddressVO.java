package com.cloud.network;

import com.cloud.utils.net.Ip;
import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = ("tungsten_guest_network_ip_address"))
public class TungstenGuestNetworkIpAddressVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "network_id")
    private long networkId;

    @Column(name = "public_ip_address")
    @Enumerated(value = EnumType.STRING)
    private Ip publicIpAddress = null;

    @Column(name = "guest_ip_address")
    @Enumerated(value = EnumType.STRING)
    private Ip guestIpAddress = null;

    public TungstenGuestNetworkIpAddressVO() {
    }

    public TungstenGuestNetworkIpAddressVO(long networkId, Ip guestIpAddress) {
        this.networkId = networkId;
        this.guestIpAddress = guestIpAddress;
    }


    public TungstenGuestNetworkIpAddressVO(long networkId, Ip publicIpAddress, Ip guestIpAddress) {
        this.networkId = networkId;
        this.publicIpAddress = publicIpAddress;
        this.guestIpAddress = guestIpAddress;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(final long networkId) {
        this.networkId = networkId;
    }

    public Ip getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(final Ip publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public Ip getGuestIpAddress() {
        return guestIpAddress;
    }

    public void setGuestIpAddress(final Ip guestIpAddress) {
        this.guestIpAddress = guestIpAddress;
    }
}
