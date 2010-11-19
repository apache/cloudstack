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
    
    @Column(name="network_id")
    long networkId;
    
    @Column(name="is_owner")
    boolean owner;

    protected NetworkAccountVO() {
    }
    
    public NetworkAccountVO(long networkId, long accountId, boolean owner) {
        this.networkId = networkId;
        this.accountId = accountId;
        this.owner = owner;
    }
    
    @Override
    public long getAccountId() {
        return accountId;
    }
    
    public long getNetworkId() {
        return networkId;
    }
    
    public boolean isOwner() {
        return owner;
    }

}
