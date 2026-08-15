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
package com.cloud.server;

import java.util.UUID;

import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.agent.api.Answer;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.PluggableService;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

/**
 */
public interface ManagementServer extends ManagementService, PluggableService {

    ConfigKey<String> customCsIdentifier = new ConfigKey<>("Advanced", String.class, "custom.cs.identifier",
            UUID.randomUUID().toString().split("-")[0].substring(4), "Custom identifier for the cloudstack installation", true, ConfigKey.Scope.Global);

    ConfigKey<Boolean> exposeCloudStackVersionInApiXmlResponse = new ConfigKey<>("Advanced", Boolean.class, "expose.cloudstack.version.api.xml.response", "true",
            "Indicates whether ACS version should appear in the root element of an API XML response.", true, ConfigKey.Scope.Global);

    ConfigKey<String> ElasticLoadBalancerEnabled = new ConfigKey<>("Advanced", String.class, "network.loadbalancer.basiczone.elb.enabled", "false",
            "Whether the load balancing service is enabled for basic zones", true);

    ConfigKey<String> ElasticLoadBalancerNetwork = new ConfigKey<>("Advanced", String.class, "network.loadbalancer.basiczone.elb.network", "guest",
            "Whether the elastic load balancing service public ips are taken from the public or guest network", true);

    ConfigKey<String> OvmPublicNetwork = new ConfigKey<>("Hidden", String.class,
            "ovm.public.network.device", null,
            "Specify the public bridge on host for public network", true);

    ConfigKey<String> OvmPrivateNetwork = new ConfigKey<>("Hidden", String.class,
            "ovm.private.network.device", null,
            "Specify the private bridge on host for private network", true);

    ConfigKey<String> OvmGuestNetwork = new ConfigKey<>("Hidden", String.class,
            "ovm.guest.network.device", null,
            "Specify the private bridge on host for private network", true);

    ConfigKey<String> Ovm3PublicNetwork = new ConfigKey<>("Hidden", String.class,
            "ovm3.public.network.device", null,
            "Specify the public bridge on host for public network", true);

    ConfigKey<String> Ovm3PrivateNetwork = new ConfigKey<>("Hidden", String.class,
            "ovm3.private.network.device", null,
            "Specify the private bridge on host for private network", true);

    ConfigKey<String> Ovm3GuestNetwork = new ConfigKey<>("Hidden", String.class,
            "ovm3.guest.network.device", null,
            "Specify the guest bridge on host for guest network", true);

    ConfigKey<String> Ovm3StorageNetwork = new ConfigKey<>("Hidden", String.class,
            "ovm3.storage.network.device", null,
            "Specify the storage bridge on host for storage network", true);

    ConfigKey<String> KvmPublicNetwork = new ConfigKey<>("Hidden", String.class,
            "kvm.public.network.device", null,
            "Specify the public bridge on host for public network", true);

    ConfigKey<String> KvmPrivateNetwork = new ConfigKey<>("Hidden", String.class,
            "kvm.private.network.device", null,
            "Specify the private bridge on host for private network", true);

    ConfigKey<String> KvmGuestNetwork = new ConfigKey<>("Hidden", String.class,
            "kvm.guest.network.device", null,
            "Specify the private bridge on host for private network", true);

    /**
     * returns the instance id of this management server.
     *
     * @return id of the management server
     */
    long getId();

    /**
     * Fetches the version of cloud stack
    */
    @Override
    String getVersion();

    /**
     * Retrieves a host by id
     *
     * @param hostId
     * @return Host
     */
    HostVO getHostBy(long hostId);

    DetailVO findDetail(long hostId, String name);

    Pair<Boolean, String> setConsoleAccessForVm(long vmId, String sessionUuid);

    String getConsoleAccessUrlRoot(long vmId);

    String getConsoleAccessAddress(long vmId);

    GuestOSVO getGuestOs(Long guestOsId);

    GuestOSHypervisorVO getGuestOsHypervisor(Long guestOsHypervisorId);

    /**
     * Returns the vnc port of the vm.
     *
     * @param VirtualMachine vm
     * @return the vnc port if found; -1 if unable to find.
     */
    Pair<String, Integer> getVncPort(VirtualMachine vm);

    public long getMemoryOrCpuCapacityByHost(Long hostId, short capacityType);

    Pair<Boolean, String> updateSystemVM(VMInstanceVO systemVM, boolean forced);

    Answer getExternalVmConsole(VirtualMachine vm, Host host);

}
