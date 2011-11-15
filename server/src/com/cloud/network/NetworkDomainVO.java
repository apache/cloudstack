/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

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
    
    @Column(name="subdomain_access")
    Boolean subdomainAccess;

    protected NetworkDomainVO() {
    }
    
    public NetworkDomainVO(long networkId, long domainId, Boolean subdomainAccess) {
        this.networkId = networkId;
        this.domainId = domainId;
        this.subdomainAccess = subdomainAccess;
    }
    
    @Override
    public long getDomainId() {
        return domainId;
    }
    
    public long getNetworkId() {
        return networkId;
    }

	public Boolean isSubdomainAccess() {
		return subdomainAccess;
	}
}
