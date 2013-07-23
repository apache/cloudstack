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
package com.cloud.ha;

import javax.ejb.Local;

import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;

@Local(value=Investigator.class)
public class VmwareInvestigator extends AdapterBase implements Investigator {
    protected VmwareInvestigator() {
    }
    
    @Override
    public Status isAgentAlive(Host agent) {
    	if(agent.getHypervisorType() == HypervisorType.VMware)
    		return Status.Disconnected;
    	
    	return null;
    }
    
    @Override
    public Boolean isVmAlive(VirtualMachine vm, Host host) {
    	if(vm.getHypervisorType() == HypervisorType.VMware)
    		return true;
    	
    	return null;
    }
}

