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
package com.cloud.network.element;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.NetworkConfiguration;
import com.cloud.network.NetworkConfiguration.State;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.network.NetworkManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;


@Local(value=NetworkElement.class)
public class DomainRouterElement extends AdapterBase implements NetworkElement {
    private static final Logger s_logger = Logger.getLogger(DomainRouterElement.class);
    
    @Inject NetworkManager _networkMgr;

    @Override
    public Boolean implement(NetworkConfiguration config, NetworkOffering offering, Account user) {
        if (offering.getGuestIpType() != GuestIpType.Virtualized) {
            s_logger.trace("Not handling guest ip type = " + offering.getGuestIpType());
            return null;
        }
        
        List<NetworkConfigurationVO> configs = _networkMgr.getNetworkConfigurationsforOffering(offering.getId(), config.getDataCenterId(), user.getId());
        for (NetworkConfigurationVO c : configs) {
            if (c.getState() != State.Implemented && c.getState() != State.Setup) {
                s_logger.debug("Not all network is ready to be implemented yet.");
                return true;
            }
        }
        
        
        return true;
    }

    @Override
    public Boolean prepare(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile vm, NetworkOffering offering, Account user) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Boolean release(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile vm, NetworkOffering offering, Account user) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Boolean shutdown(NetworkConfiguration config, NetworkOffering offering, Account user) {
        // TODO Auto-generated method stub
        return false;
    }

    protected DomainRouterElement() {
        super();
    }
}
