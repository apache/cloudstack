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

import com.cloud.host.HostVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.user.AccountVO;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.VMInstanceVO;

public interface VirtualNetworkAllocator extends Adapter {

    /**
     * Allocate an virtual network tag
     * 
     * @param account account that this network belongs to.
     * @param host host being deployed to.
     * @param dc datacenter being deployed to.
     * @param vm vm that is being deployed.
     * @param so service offering for that vm.
     * @return tag
     */
    String allocateTag(AccountVO account, HostVO host, VMInstanceVO vm, ServiceOfferingVO so);
    
    
    /**
     * Release the virtual network tag.
     * 
     * @param tag tag retrieved in allocateTag
     * @param host host to release this tag in.
     * @param account account to release this tag in.
     * @param vm vm that is releasing this tag.
     */
    void releaseTag(String tag, HostVO host, AccountVO account, VMInstanceVO vm);
    
}
