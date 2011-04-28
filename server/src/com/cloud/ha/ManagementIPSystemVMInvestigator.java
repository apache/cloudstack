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

package com.cloud.ha;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.network.NetworkManager;
import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.component.Inject;
import com.cloud.vm.Nic;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

@Local(value={Investigator.class})
public class ManagementIPSystemVMInvestigator extends AbstractInvestigatorImpl {
    private static final Logger s_logger = Logger.getLogger(ManagementIPSystemVMInvestigator.class);

    private String _name = null;
    @Inject private HostDao _hostDao = null;
    @Inject private NetworkManager _networkMgr = null;


    @Override
    public Boolean isVmAlive(VMInstanceVO vm, HostVO host) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("testing if vm (" + vm.getId() + ") is alive");
        }
        if (VirtualMachine.Type.isSystemVM(vm.getType())) {
        	
            Nic nic = _networkMgr.getNicForTraffic(vm.getId(), TrafficType.Management);
            if (nic == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find a management nic, cannot ping this system VM, unable to determine state of vm (" + vm.getId() + "), returning null");
                }
                return null;
            }
            
            // get the data center IP address, find a host on the pod, use that host to ping the data center IP address
            HostVO vmHost = _hostDao.findById(vm.getHostId());
            List<Long> otherHosts = findHostByPod(vm.getPodId(), vm.getHostId());
            for (Long otherHost : otherHosts) {

                Status vmState = testIpAddress(otherHost, vm.getPrivateIpAddress());
                if (vmState == null) {
                    // can't get information from that host, try the next one
                    continue;
                }
                if (vmState == Status.Up) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("successfully pinged vm's private IP (" + vm.getPrivateIpAddress() + "), returning that the VM is up");
                    }
                    return Boolean.TRUE;
                } else if (vmState == Status.Down) {
                    // We can't ping the VM directly...if we can ping the host, then report the VM down.
                    // If we can't ping the host, then we don't have enough information.
                    Status vmHostState = testIpAddress(otherHost, vmHost.getPrivateIpAddress());
                    if ((vmHostState != null) && (vmHostState == Status.Up)) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("successfully pinged vm's host IP (" + vmHost.getPrivateIpAddress() + "), but could not ping VM, returning that the VM is down");
                        }
                        return Boolean.FALSE;
                    }
                }
            }
        }else{
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Not a System Vm, unable to determine state of vm (" + vm.getId() + "), returning null");
            }
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("unable to determine state of vm (" + vm.getId() + "), returning null");
        }
        return null;
    }

    @Override
    public Status isAgentAlive(HostVO agent) {
    	return null;
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

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }  

}
