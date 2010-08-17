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

import java.util.List;

import javax.ejb.Local;

import com.cloud.dc.DataCenterVO;
import com.cloud.network.NetworkManager;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StorageManager;
import com.cloud.user.AccountVO;
import com.cloud.utils.component.Inject;

@Local(value=VmManager.class)
public class MauriceMoss implements VmManager {
    @Inject private StorageManager _storageMgr;
    @Inject private NetworkManager _networkMgr;

    @Override
    public VMInstanceVO allocate(VMInstanceVO vm, 
            ServiceOfferingVO serviceOffering, 
            NetworkOfferingVO[] networkOfferings, 
            DiskOfferingVO[] diskOffering,
            DataCenterVO dc,
            AccountVO account) {
    	
    	return null;
    }
    
    @Override
    public void create(VmCharacteristics vm, List<DiskCharacteristics> disks, List<NetworkCharacteristics> networks) {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
    }
    
    protected MauriceMoss() {
    }
}
