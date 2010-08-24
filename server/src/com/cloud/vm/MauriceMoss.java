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
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

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
    String _name;
    @Inject private StorageManager _storageMgr;
    @Inject private NetworkManager _networkMgr;

    @Override
    public VMInstanceVO allocate(VMInstanceVO vm, 
            ServiceOfferingVO serviceOffering,
            Long rootSize,
            List<NetworkOfferingVO> networkOfferings, 
            Map<DiskOfferingVO, Long> diskOfferings,
            DataCenterVO dc,
            AccountVO owner) {
        return null;
    }

    @Override
    public VMInstanceVO allocate(VMInstanceVO vm,
    ServiceOfferingVO serviceOffering,
    Long rootSize,
    List<NetworkOfferingVO> networkOfferings,
    DiskOfferingVO dataOffering,
    Long dataSize,
    DataCenterVO dc,
    AccountVO owner) {
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
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        return true;
    }
    
    @Override
    public String getName() {
        return _name;
    }
    
    protected MauriceMoss() {
    }
    
}
