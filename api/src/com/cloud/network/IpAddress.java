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
package com.cloud.network;

import java.util.Date;

import com.cloud.acl.ControlledEntity;
import com.cloud.utils.net.Ip;

/**
 * IpAddress represents the public ip address to be allocated in the CloudStack.
 * 
 * When it is not allocated, it should have
 *   - State = Free
 *   - Allocated = null
 *   - AccountId = null
 *   - DomainId = null
 *   
 * When it is allocated, it should have
 *   - State = Allocated
 *   - AccountId = account owner.
 *   - DomainId = domain of the account owner.
 *   - Allocated = time it was allocated.
 */
public interface IpAddress extends ControlledEntity {
    enum State {
        Allocating,  // The IP Address is being propagated to other network elements and is not ready for use yet.
        Allocated,   // The IP address is in used.
        Releasing,   // The IP address is being released for other network elements and is not ready for allocation.
        Free         // The IP address is ready to be allocated. 
    }
    
    long getDataCenterId();

    Ip getAddress();
    
    Long getAllocatedToAccountId();
    
    Long getAllocatedInDomainId();
    
    Date getAllocatedTime();
    
    boolean isSourceNat();

    long getVlanId();

    boolean isOneToOneNat();
    
    State getState();
    
    boolean readyToUse();
    
    Long getAssociatedWithNetworkId();
    
    Long getAssociatedWithVmId();
    
    public Long getPhysicalNetworkId();
    
    /**
     * @return database id.
     */
    long getId();
    
    void setState(IpAddress.State state);
}
