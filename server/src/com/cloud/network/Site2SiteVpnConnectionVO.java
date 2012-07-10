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
@Table(name=("s2s_vpn_connection"))
public class Site2SiteVpnConnectionVO implements Site2SiteVpnConnection {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
	@Column(name="uuid")
	private String uuid;    
    
    @Column(name="vpn_gateway_id")
    private long vpnGatewayId;
    
    @Column(name="customer_gateway_id")
    private long customerGatewayId;

    @Column(name="state")
    private State state;
    
    @Column(name="domain_id")
    private Long domainId;
    
    @Column(name="account_id")
    private Long accountId;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;
    
    public Site2SiteVpnConnectionVO() { }

    public Site2SiteVpnConnectionVO(long accountId, long domainId, long vpnGatewayId, long customerGatewayId) {
        this.uuid = UUID.randomUUID().toString();
        this.setVpnGatewayId(vpnGatewayId);
        this.setCustomerGatewayId(customerGatewayId);
        this.setState(State.Pending);
        this.accountId = accountId;
        this.domainId = domainId;
    }
    
    @Override
    public long getId() {
        return id;
    }
    
    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public long getVpnGatewayId() {
        return vpnGatewayId;
    }

    public void setVpnGatewayId(long vpnGatewayId) {
        this.vpnGatewayId = vpnGatewayId;
    }

    @Override
    public long getCustomerGatewayId() {
        return customerGatewayId;
    }

    public void setCustomerGatewayId(long customerGatewayId) {
        this.customerGatewayId = customerGatewayId;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }
    
    public String getUuid() {
        return uuid;
    }
    
    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }
}
