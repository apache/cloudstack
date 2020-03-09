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
package com.cloud.api.query.vo;

import java.net.URI;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;

@Entity
@Table(name = "domain_router_view")
public class DomainRouterJoinVO extends BaseViewVO implements ControlledViewEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

    @Column(name = "name", updatable = false, nullable = false, length = 255)
    private String name = null;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "account_uuid")
    private String accountUuid;

    @Column(name = "account_name")
    private String accountName = null;

    @Column(name = "account_type")
    private short accountType;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName = null;

    @Column(name = "domain_path")
    private String domainPath = null;

    /**
     * Note that state is intentionally missing the setter.  Any updates to
     * the state machine needs to go through the DAO object because someone
     * else could be updating it as well.
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "state", updatable = true, nullable = false, length = 32)
    private State state = null;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "instance_name", updatable = true, nullable = false)
    private String instanceName;

    @Column(name = "pod_id", updatable = true, nullable = false)
    private Long podId;

    @Column(name = "pod_uuid")
    private String podUuid;

    @Column(name = "data_center_id")
    private long dataCenterId;

    @Column(name = "data_center_uuid")
    private String dataCenterUuid;

    @Column(name = "data_center_name")
    private String dataCenterName = null;

    @Column(name = "cluster_id")
    private long clusterId;

    @Column(name = "dns1")
    private String dns1 = null;

    @Column(name = "dns2")
    private String dns2 = null;

    @Column(name = "ip6_dns1")
    private String ip6Dns1 = null;

    @Column(name = "ip6_dns2")
    private String ip6Dns2 = null;

    @Column(name = "host_id", updatable = true, nullable = true)
    private Long hostId;

    @Column(name = "host_uuid")
    private String hostUuid;

    @Column(name = "host_name", nullable = false)
    private String hostName;

    @Column(name="hypervisor_type")
    @Enumerated(value=EnumType.STRING)
    private Hypervisor.HypervisorType hypervisorType;

    @Column(name = "template_id", updatable = true, nullable = true, length = 17)
    private long templateId;

    @Column(name = "template_uuid")
    private String templateUuid;

    @Column(name = "service_offering_id")
    private long serviceOfferingId;

    @Column(name = "service_offering_uuid")
    private String serviceOfferingUuid;

    @Column(name = "service_offering_name")
    private String serviceOfferingName;

    @Column(name = "vpc_id")
    private long vpcId;

    @Column(name = "vpc_uuid")
    private String vpcUuid;

    @Column(name = "vpc_name")
    private String vpcName;

    @Column(name = "nic_id")
    private long nicId;

    @Column(name = "nic_uuid")
    private String nicUuid;

    @Column(name = "is_default_nic")
    private boolean isDefaultNic;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "gateway")
    private String gateway;

    @Column(name = "netmask")
    private String netmask;

    @Column(name = "ip6_address")
    private String ip6Address;

    @Column(name = "ip6_gateway")
    private String ip6Gateway;

    @Column(name = "ip6_cidr")
    private String ip6Cidr;

    @Column(name = "mac_address")
    private String macAddress;

    @Column(name = "broadcast_uri")
    private URI broadcastUri;

    @Column(name = "isolation_uri")
    private URI isolationUri;

    @Column(name = "network_id")
    private long networkId;

    @Column(name = "network_uuid")
    private String networkUuid;

    @Column(name = "network_name")
    private String networkName;

    @Column(name = "network_domain")
    private String networkDomain;

    @Column(name = "traffic_type")
    @Enumerated(value = EnumType.STRING)
    private TrafficType trafficType;

    @Column(name = "project_id")
    private long projectId;

    @Column(name = "project_uuid")
    private String projectUuid;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_uuid")
    private String jobUuid;

    @Column(name = "job_status")
    private int jobStatus;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "template_version")
    private String templateVersion;

    @Column(name = "scripts_version")
    private String scriptsVersion;

    @Column(name = "redundant_state")
    @Enumerated(EnumType.STRING)
    private RedundantState redundantState;

    @Column(name = "is_redundant_router")
    boolean isRedundantRouter;

    @Column(name = "guest_type")
    @Enumerated(value = EnumType.STRING)
    private GuestType guestType;

    @Column(name = "role")
    @Enumerated(value = EnumType.STRING)
    private VirtualRouter.Role role;

    public DomainRouterJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public short getAccountType() {
        return accountType;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public String getDomainUuid() {
        return domainUuid;
    }

    @Override
    public String getDomainName() {
        return domainName;
    }

    @Override
    public String getDomainPath() {
        return domainPath;
    }

    public State getState() {
        return state;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getPodUuid() {
        return podUuid;
    }

    public String getDataCenterUuid() {
        return dataCenterUuid;
    }

    public String getDataCenterName() {
        return dataCenterName;
    }

    public Long getHostId() {
        return hostId;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public String getHostName() {
        return hostName;
    }

    public Hypervisor.HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public long getTemplateId() {
        return templateId;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public String getServiceOfferingUuid() {
        return serviceOfferingUuid;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public long getVpcId() {
        return vpcId;
    }

    public String getVpcName() {
        return vpcName;
    }

    public long getNicId() {
        return nicId;
    }

    public boolean isDefaultNic() {
        return isDefaultNic;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getGateway() {
        return gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public URI getBroadcastUri() {
        return broadcastUri;
    }

    public URI getIsolationUri() {
        return isolationUri;
    }

    public long getNetworkId() {
        return networkId;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getNetworkDomain() {
        return networkDomain;
    }

    public TrafficType getTrafficType() {
        return trafficType;
    }

    public long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public long getProjectId() {
        return projectId;
    }

    @Override
    public String getProjectUuid() {
        return projectUuid;
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    public String getVpcUuid() {
        return vpcUuid;
    }

    public String getNicUuid() {
        return nicUuid;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public int getJobStatus() {
        return jobStatus;
    }

    public Long getPodId() {
        return podId;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public String getDns1() {
        return dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public String getScriptsVersion() {
        return scriptsVersion;
    }

    public RedundantState getRedundantState() {
        return redundantState;
    }

    public boolean isRedundantRouter() {
        return isRedundantRouter;
    }

    public GuestType getGuestType() {
        return guestType;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public String getIp6Gateway() {
        return ip6Gateway;
    }

    public String getIp6Cidr() {
        return ip6Cidr;
    }

    public String getIp6Dns1() {
        return ip6Dns1;
    }

    public String getIp6Dns2() {
        return ip6Dns2;
    }

    public VirtualRouter.Role getRole() {
        return role;
    }

    @Override
    public Class<?> getEntityType() {
        return VirtualMachine.class;
    }
}
