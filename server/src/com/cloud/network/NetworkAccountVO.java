/**
 * 
 */
package com.cloud.network;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.user.OwnedBy;

@Entity
@Table(name="account_network_ref")
public class NetworkAccountVO implements OwnedBy {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    long id;
    
    @Column(name="account_id")
    long accountId;
    
    @Column(name="network_configuration_id")
    long networkConfigurationId;
    
    @Column(name="is_owner")
    boolean owner;

    protected NetworkAccountVO() {
    }
    
    public NetworkAccountVO(long networkConfigurationId, long accountId, boolean owner) {
        this.networkConfigurationId = networkConfigurationId;
        this.accountId = accountId;
        this.owner = owner;
    }
    
    @Override
    public long getAccountId() {
        return accountId;
    }
    
    public long getNetworkConfigurationId() {
        return networkConfigurationId;
    }
    
    public boolean isOwner() {
        return owner;
    }

}
