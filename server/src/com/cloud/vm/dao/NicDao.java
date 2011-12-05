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
package com.cloud.vm.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;

public interface NicDao extends GenericDao<NicVO, Long> {
    List<NicVO> listByVmId(long instanceId);
    
    List<String> listIpAddressInNetwork(long networkConfigId);
    List<NicVO> listByVmIdIncludingRemoved(long instanceId);
    
    List<NicVO> listByNetworkId(long networkId);
    
    NicVO findByInstanceIdAndNetworkId(long networkId, long instanceId);
    
    NicVO findByInstanceIdAndNetworkIdIncludingRemoved(long networkId, long instanceId);

    void removeNicsForInstance(long instanceId);
    
    NicVO findByNetworkIdAndType(long networkId, VirtualMachine.Type vmType);
    
    NicVO findByIp4Address(String ip4Address);
    
    NicVO findDefaultNicForVM(long instanceId);
}
