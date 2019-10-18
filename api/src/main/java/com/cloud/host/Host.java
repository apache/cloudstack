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
package com.cloud.host;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceState;
import com.cloud.utils.fsm.StateObject;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.ha.HAResource;
import org.apache.cloudstack.kernel.Partition;

import java.util.Date;

/**
 *  Host represents one particular host server.
 */
public interface Host extends StateObject<Status>, Identity, Partition, HAResource {
    public enum Type {
        Storage(false), Routing(false), SecondaryStorage(false), SecondaryStorageCmdExecutor(false), ConsoleProxy(true), ExternalFirewall(false), ExternalLoadBalancer(
                false), ExternalVirtualSwitchSupervisor(false), PxeServer(false), BaremetalPxe(false), BaremetalDhcp(false), TrafficMonitor(false), NetScalerControlCenter(false),

        ExternalDhcp(false), SecondaryStorageVM(true), LocalSecondaryStorage(false), L2Networking(false);
        boolean _virtual;

        private Type(boolean virtual) {
            _virtual = virtual;
        }

        public boolean isVirtual() {
            return _virtual;
        }

        public static String[] toStrings(Host.Type... types) {
            String[] strs = new String[types.length];
            for (int i = 0; i < types.length; i++) {
                strs[i] = types[i].toString();
            }
            return strs;
        }
    }
    public static final String HOST_UEFI_ENABLE = "Host.Uefi.Enable";

    /**
     * @return name of the machine.
     */
    String getName();

    /**
     * @return the type of host.
     */
    Type getType();

    /**
     * @return the date the host first registered
     */
    Date getCreated();

    /**
     * @return current state of this machine.
     */
    Status getStatus();

    /**
     * @return the ip address of the host.
     */
    String getPrivateIpAddress();

    /**
     * @return the ip address of the host.
     */
    String getStorageUrl();

    /**
     * @return the ip address of the host attached to the storage network.
     */
    String getStorageIpAddress();

    /**
     * @return the mac address of the host.
     */
    String getGuid();

    /**
     * @return total amount of memory.
     */
    Long getTotalMemory();

    /**
     * @return # of cpu sockets in a machine.
     */
    Integer getCpuSockets();

    /**
     * @return # of cores in a machine.  Note two cpus with two cores each returns 4.
     */
    Integer getCpus();

    /**
     * @return speed of each cpu in mhz.
     */
    Long getSpeed();

    /**
     * @return the proxy port that is being listened at the agent host
     */
    Integer getProxyPort();

    /**
     * @return the pod.
     */
    Long getPodId();

    /**
     * @return availability zone.
     */
    long getDataCenterId();

    /**
     * @return parent path.  only used for storage server.
     */
    String getParent();

    /**
     * @return storage ip address.
     */
    String getStorageIpAddressDeux();

    /**
     * @return type of hypervisor
     */
    HypervisorType getHypervisorType();

    /**
     * @return disconnection date
     */
    Date getDisconnectedOn();

    /**
     * @return version
     */
    String getVersion();

    /*
     * @return total size
     */
    long getTotalSize();

    /*
     * @return capabilities
     */
    String getCapabilities();

    /*
     * @return last pinged time
     */
    long getLastPinged();

    /*
     * @return management server id
     */
    Long getManagementServerId();

    /*
     *@return removal date
     */
    Date getRemoved();

    Long getClusterId();

    String getPublicIpAddress();

    String getPublicNetmask();

    String getPrivateNetmask();

    String getStorageNetmask();

    String getStorageMacAddress();

    String getPublicMacAddress();

    String getPrivateMacAddress();

    String getStorageNetmaskDeux();

    String getStorageMacAddressDeux();

    String getHypervisorVersion();

    boolean isInMaintenanceStates();

    boolean isDisabled();

    ResourceState getResourceState();
}
