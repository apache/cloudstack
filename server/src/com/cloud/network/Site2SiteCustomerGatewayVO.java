package com.cloud.network;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name=("s2s_customer_gateway"))
public class Site2SiteCustomerGatewayVO implements Site2SiteCustomerGateway {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name="gateway_ip")
    private String gatewayIp;

    @Column(name="guest_cidr_list")
    private String guestCidrList;

    @Column(name="ipsec_psk")
    private String ipsecPsk;

    @Column(name="ike_policy")
    private String ikePolicy;

    @Column(name="esp_policy")
    private String espPolicy;

    @Column(name="lifetime")
    private long lifetime;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    public Site2SiteCustomerGatewayVO() { }

    public Site2SiteCustomerGatewayVO(String gatewayIp, String guestCidrList, String ipsecPsk, String ikePolicy, String espPolicy, long lifetime) {
        this.gatewayIp = gatewayIp;
        this.guestCidrList = guestCidrList;
        this.ipsecPsk = ipsecPsk;
        this.ikePolicy = ikePolicy;
        this.espPolicy = espPolicy;
        this.lifetime = lifetime;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getGatewayIp() {
        return gatewayIp;
    }

    public void setGatewayIp(String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }

    @Override
    public String getGuestCidrList() {
        return guestCidrList;
    }

    public void setGuestCidrList(String guestCidrList) {
        this.guestCidrList = guestCidrList;
    }

    @Override
    public String getIpsecPsk() {
        return ipsecPsk;
    }

    public void setIpsecPsk(String ipsecPsk) {
        this.ipsecPsk = ipsecPsk;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public long getLifetime() {
        return lifetime;
    }

    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
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

    public String getUuid() {
        return uuid;
    }
}
