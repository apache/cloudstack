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
package org.apache.cloudstack.network.tungsten.model;

import net.juniper.tungsten.api.types.ApplicationPolicySet;
import net.juniper.tungsten.api.types.NetworkPolicy;
import net.juniper.tungsten.api.types.Tag;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;

import java.util.List;

public class TungstenTag implements TungstenModel {
    private Tag tag;
    private List<VirtualNetwork> virtualNetworkList;
    private List<VirtualMachine> virtualMachineList;
    private List<VirtualMachineInterface> virtualMachineInterfaceList;
    private List<NetworkPolicy> networkPolicyList;
    private List<ApplicationPolicySet> applicationPolicySetList;

    public TungstenTag() {
    }

    public TungstenTag(final Tag tag, final List<VirtualNetwork> virtualNetworkList,
        final List<VirtualMachine> virtualMachineList, final List<VirtualMachineInterface> virtualMachineInterfaceList,
        final List<NetworkPolicy> networkPolicyList, final List<ApplicationPolicySet> applicationPolicySetList) {
        this.tag = tag;
        this.virtualNetworkList = virtualNetworkList;
        this.virtualMachineList = virtualMachineList;
        this.virtualMachineInterfaceList = virtualMachineInterfaceList;
        this.networkPolicyList = networkPolicyList;
        this.applicationPolicySetList = applicationPolicySetList;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(final Tag tag) {
        this.tag = tag;
    }

    public List<VirtualMachine> getVirtualMachineList() {
        return virtualMachineList;
    }

    public void setVirtualMachineList(final List<VirtualMachine> virtualMachineList) {
        this.virtualMachineList = virtualMachineList;
    }

    public List<VirtualMachineInterface> getVirtualMachineInterfaceList() {
        return virtualMachineInterfaceList;
    }

    public void setVirtualMachineInterfaceList(final List<VirtualMachineInterface> virtualMachineInterfaceList) {
        this.virtualMachineInterfaceList = virtualMachineInterfaceList;
    }

    public List<NetworkPolicy> getNetworkPolicyList() {
        return networkPolicyList;
    }

    public void setNetworkPolicyList(final List<NetworkPolicy> networkPolicyList) {
        this.networkPolicyList = networkPolicyList;
    }

    public List<VirtualNetwork> getVirtualNetworkList() {
        return virtualNetworkList;
    }

    public void setVirtualNetworkList(final List<VirtualNetwork> virtualNetworkList) {
        this.virtualNetworkList = virtualNetworkList;
    }

    public List<ApplicationPolicySet> getApplicationPolicySetList() {
        return applicationPolicySetList;
    }

    public void setApplicationPolicySetList(final List<ApplicationPolicySet> applicationPolicySetList) {
        this.applicationPolicySetList = applicationPolicySetList;
    }
}
