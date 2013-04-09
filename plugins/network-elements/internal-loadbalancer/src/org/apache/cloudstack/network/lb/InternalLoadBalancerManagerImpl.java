// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
// 
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.network.lb;

import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.Commands;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;

@Component
@Local(value = { InternalLoadBalancerManager.class })
public class InternalLoadBalancerManagerImpl extends ManagerBase implements
InternalLoadBalancerManager, VirtualMachineGuru<DomainRouterVO> {
    private static final Logger s_logger = Logger
            .getLogger(InternalLoadBalancerManagerImpl.class);
    
    @Inject VirtualMachineManager _itMgr;

    @Override
    public DomainRouterVO findByName(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DomainRouterVO findById(long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DomainRouterVO persist(DomainRouterVO vm) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<DomainRouterVO> profile, DeployDestination dest, ReservationContext context) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<DomainRouterVO> profile, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile<DomainRouterVO> profile, long hostId, Commands cmds, ReservationContext context) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile<DomainRouterVO> profile) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void finalizeStop(VirtualMachineProfile<DomainRouterVO> profile, StopAnswer answer) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void finalizeExpunge(DomainRouterVO vm) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Long convertToId(String vmName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean plugNic(Network network, NicTO nic, VirtualMachineTO vm, ReservationContext context, DeployDestination dest) throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean unplugNic(Network network, NicTO nic, VirtualMachineTO vm, ReservationContext context, DeployDestination dest) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void prepareStop(VirtualMachineProfile<DomainRouterVO> profile) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _itMgr.registerGuru(VirtualMachine.Type.InternalLoadBalancerVm, this);

        if (s_logger.isInfoEnabled()) {
            s_logger.info(getName()  +  " has been configured");
        }
        return true;
    }
    
    @Override
    public String getName() {
        return _name;
    }

}
