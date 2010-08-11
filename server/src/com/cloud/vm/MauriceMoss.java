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
package com.cloud.vm;

import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;

public class MauriceMoss implements VmManager {

    @Override
    public VMInstanceVO allocate(VMInstanceVO vm, ServiceOfferingVO serviceOffering, NetworkOfferingVO[] networkOfferings, DiskOfferingVO[] diskOffering) {
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.vm.VmManager#create(com.cloud.vm.VMInstanceVO)
     */
    @Override
    public void create(VMInstanceVO vm) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.vm.VmManager#destroy()
     */
    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.vm.VmManager#start()
     */
    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.vm.VmManager#stop()
     */
    @Override
    public void stop() {
        // TODO Auto-generated method stub

    }

}
