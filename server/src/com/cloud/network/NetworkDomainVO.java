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

import com.cloud.domain.PartOf;

@Entity
@Table(name="domain_network_ref")
public class NetworkDomainVO implements PartOf {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    long id;
    
    @Column(name="domain_id")
    long domainId;
    
    @Column(name="network_id")
    long networkId;

    protected NetworkDomainVO() {
    }
    
    public NetworkDomainVO(long networkId, long domainId) {
        this.networkId = networkId;
        this.domainId = domainId;
    }
    
    @Override
    public long getDomainId() {
        return domainId;
    }
    
    public long getNetworkId() {
        return networkId;
    }
    
}
