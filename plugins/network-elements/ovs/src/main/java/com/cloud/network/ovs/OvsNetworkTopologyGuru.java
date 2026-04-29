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
package com.cloud.network.ovs;

import com.cloud.utils.component.Manager;

import java.util.List;

public interface OvsNetworkTopologyGuru extends Manager {

    /**
     * get the list of hypervisor hosts id's on which VM's belonging to the network currently spans
     */
    public  List<Long> getNetworkSpanedHosts(long networkId);

    /**
     * get the list of hypervisor hosts id's on which VM's belonging to a VPC spans
     */
    public  List<Long> getVpcSpannedHosts(long vpId);

    /**
     * get the list of VPC id's of the vpc's for which one or more VM's from the VPC are running on the host
     */
    public  List<Long> getVpcOnHost(long hostId);

    /**
     * get the list of all active Vm id's in a network
     */
    public List<Long> getAllActiveVmsInNetwork(long networkId);

    /**
     * get the list of all active Vm id's in the VPC for all ther tiers
     */
    public List<Long> getAllActiveVmsInVpc(long vpcId);

    /**
     * get the list of all Vm id's in the VPC for all the tiers that are running on the host
     */
    public List<Long> getActiveVmsInVpcOnHost(long vpcId, long hostId);

    /**
     * get the list of all Vm id's in the network that are running on the host
     */
    public List<Long> getActiveVmsInNetworkOnHost(long vpcId, long hostId, boolean includeVr);

    /**
     * get the list of all Vpc id's in which, a VM has a nic in the network that is part of VPC
     */
    public List<Long> getVpcIdsVmIsPartOf(long vmId);
}
