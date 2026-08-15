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
import com.cloud.hypervisor.Hypervisor.HypervisorType;
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

    ConfigKey<Boolean> ApiLimitEnabled = new ConfigKey<>("Advanced", Boolean.class, "api.throttling.enabled", "false", "Enable/disable Api rate limit", true);

    ConfigKey<Integer> ApiLimitInterval = new ConfigKey<>("Advanced", Integer.class, "api.throttling.interval", "1", "Time interval (in seconds) to reset API count", true);

    ConfigKey<Integer> ApiLimitMax = new ConfigKey<>("Advanced", Integer.class, "api.throttling.max", "25", "Max allowed number of APIs within fixed interval", true);

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

    ConfigKey<Boolean> PublishActionEvent = new ConfigKey<>("Advanced", Boolean.class, "publish.action.events", "true",
            "enable or disable publishing of action events on the event bus", true);

    ConfigKey<Boolean> PublishAlertEvent = new ConfigKey<>("Advanced", Boolean.class, "publish.alert.events", "true",
            "enable or disable publishing of alert events on the event bus", true);

    ConfigKey<Boolean> PublishResourceStateEvent = new ConfigKey<>("Advanced", Boolean.class, "publish.resource.state.events", "true",
            "enable or disable publishing of alert events on the event bus", true);

    ConfigKey<Boolean> PublishUsageEvent = new ConfigKey<>("Advanced", Boolean.class, "publish.usage.events", "true",
            "enable or disable publishing of usage events on the event bus", true);

    ConfigKey<Integer> EventPurgeInterval = new ConfigKey<>("Advanced", Integer.class, "event.purge.interval", "86400",
            "The interval (in seconds) to wait before running the event purge thread", true);

    ConfigKey<Integer> LinkLocalIpNums = new ConfigKey<>("Advanced", Integer.class, "linkLocalIp.nums", "10",
            "The number of link local ip that needed by domR(in power of 2)", true);

    ConfigKey<String> HypervisorList = new ConfigKey<>("Advanced", String.class, "hypervisor.list",
            HypervisorType.KVM + "," + HypervisorType.VMware + "," + HypervisorType.XenServer + "," + HypervisorType.Hyperv + "," +
                    HypervisorType.BareMetal + "," + HypervisorType.Ovm + "," + HypervisorType.LXC + "," + HypervisorType.Ovm3 + "," + HypervisorType.External,
            "The list of hypervisors that this deployment will use.", true, ConfigKey.Kind.CSV, null);

    ConfigKey<String> ManagementNetwork = new ConfigKey<>("Advanced", String.class, "management.network.cidr", null,
            "The cidr of management server network", true);

    ConfigKey<Integer> EventPurgeDelay = new ConfigKey<>("Advanced", Integer.class, "event.purge.delay", "15",
            "Events older than specified number days will be purged. Set this value to 0 to never delete events", true);

    ConfigKey<Integer> AlertPurgeInterval = new ConfigKey<>("Advanced", Integer.class, "alert.purge.interval", "86400",
            "The interval (in seconds) to wait before running the alert purge thread", true);

    ConfigKey<Integer> AlertPurgeDelay = new ConfigKey<>("Advanced", Integer.class, "alert.purge.delay", "0",
            "Alerts older than specified number days will be purged. Set this value to 0 to never delete alerts", true);

    ConfigKey<String> ControlCidr = new ConfigKey<>("Advanced", String.class, "control.cidr", "169.254.0.0/16",
            "Changes the cidr for the control network traffic.  Defaults to using link local.  Must be unique within pods", true);

    ConfigKey<String> ControlGateway = new ConfigKey<>("Advanced", String.class, "control.gateway", "169.254.0.1",
            "gateway for the control network traffic", true);

    ConfigKey<Integer> DetailBatchQuerySize = new ConfigKey<>("Advanced", Integer.class, "detail.batch.query.size", "2000",
            "Default entity detail batch query size for listing", true);

    ConfigKey<Boolean> S3EnableRRS = new ConfigKey<>("Advanced", Boolean.class, "s3.rrs.enabled", "false",
            "enable s3 reduced redundancy storage", true);

    ConfigKey<Integer> S3MaxSingleUploadSize = new ConfigKey<>("Advanced", Integer.class, "s3.singleupload.max.size", "5",
            "The maximum size limit for S3 single part upload API(in GB). If it is set to 0, then it means always use multi-part upload to upload object to S3. "
                    + "If it is set to -1, then it means always use single-part upload to upload object to S3. ", true);

    ConfigKey<String> CloudDnsName = new ConfigKey<>("Advanced", String.class, "cloud.dns.name", null,
            "DNS name of the cloud for the GSLB service", true);

    ConfigKey<String> InternalLbVmServiceOfferingId = new ConfigKey<>("Advanced", String.class, "internallbvm.service.offering", null,
            "Uuid of the service offering used by internal lb vm; if NULL - default system internal lb offering will be used", true);

    ConfigKey<Integer> RouterAggregationCommandEachTimeout = new ConfigKey<>("Advanced", Integer.class, "router.aggregation.command.each.timeout", "600",
            "timeout in seconds for each Virtual Router command being aggregated. The final aggregation command timeout would be determined by this timeout * commands counts ", true);

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
