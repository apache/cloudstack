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

import com.cloud.network.Network.GuestType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VirtualMachine.State;

@Entity
@Table(name="domain_router_view")
public class DomainRouterJoinVO extends BaseViewVO implements ControlledViewEntity {

    @Id
    @Column(name="id", updatable=false, nullable = false)
    private long id;

    @Column(name="name", updatable=false, nullable=false, length=255)
    private String name = null;


    @Column(name="account_id")
    private long accountId;

    @Column(name="account_uuid")
    private String accountUuid;

    @Column(name="account_name")
    private String accountName = null;

    @Column(name="account_type")
    private short accountType;

    @Column(name="domain_id")
    private long domainId;

    @Column(name="domain_uuid")
    private String domainUuid;

    @Column(name="domain_name")
    private String domainName = null;

    @Column(name="domain_path")
    private String domainPath = null;

    /**
     * Note that state is intentionally missing the setter.  Any updates to
     * the state machine needs to go through the DAO object because someone
     * else could be updating it as well.
     */
    @Enumerated(value=EnumType.STRING)
    @Column(name="state", updatable=true, nullable=false, length=32)
    private State state = null;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name="instance_name", updatable=true, nullable=false)
    private String instanceName;

    @Column(name="pod_id", updatable=true, nullable=false)
    private Long podId;

    @Column(name="pod_uuid")
    private String podUuid;

    @Column(name="data_center_id")
    private long dataCenterId;

    @Column(name="data_center_uuid")
    private String dataCenterUuid;

    @Column(name="data_center_name")
    private String dataCenterName = null;

    @Column(name="dns1")
    private String dns1 = null;

    @Column(name="dns2")
    private String dns2 = null;


    @Column(name="host_id", updatable=true, nullable=true)
    private long hostId;

    @Column(name="host_uuid")
    private String hostUuid;

    @Column(name="host_name", nullable=false)
    private String hostName;

    @Column(name="template_id", updatable=true, nullable=true, length=17)
    private long templateId;

    @Column(name="template_uuid")
    private String templateUuid;

    @Column(name="service_offering_id")
    private long serviceOfferingId;

    @Column(name="service_offering_uuid")
    private String serviceOfferingUuid;

    @Column(name="service_offering_name")
    private String serviceOfferingName;


    @Column(name = "vpc_id")
    private long vpcId;

    @Column(name = "vpc_uuid")
    private String vpcUuid;

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

    @Column(name = "mac_address")
    private String macAddress;

    @Column(name = "broadcast_uri")
    private URI broadcastUri;

    @Column(name = "isolation_uri")
    private URI isolationUri;

    @Column(name="network_id")
    private long networkId;

    @Column(name="network_uuid")
    private String networkUuid;

    @Column(name="network_name")
    private String networkName;

    @Column(name="network_domain")
    private String networkDomain;

    @Column(name="traffic_type")
    @Enumerated(value=EnumType.STRING)
    private TrafficType trafficType;


    @Column(name="project_id")
    private long projectId;

    @Column(name="project_uuid")
    private String projectUuid;

    @Column(name="project_name")
    private String projectName;

    @Column(name="job_id")
    private long jobId;

    @Column(name="job_uuid")
    private String jobUuid;

    @Column(name="job_status")
    private int jobStatus;


    @Column(name="uuid")
    private String uuid;

    @Column(name="template_version")
    private String templateVersion;

    @Column(name="scripts_version")
    private String scriptsVersion;

    @Column(name="redundant_state")
    @Enumerated(EnumType.STRING)
    private RedundantState redundantState;

    @Column(name="is_redundant_router")
    boolean isRedundantRouter;

    @Column(name="guest_type")
    @Enumerated(value=EnumType.STRING)
    private GuestType guestType;


    public DomainRouterJoinVO() {
    }




    @Override
    public long getId() {
        return id;
    }


    @Override
    public void setId(long id) {
        this.id = id;

    }




    @Override
    public String getUuid() {
        return uuid;
    }




    public void setUuid(String uuid) {
        this.uuid = uuid;
    }



    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }




    @Override
    public long getAccountId() {
        return accountId;
    }


    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }


    @Override
    public String getAccountUuid() {
        return accountUuid;
    }




    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }




    @Override
    public String getAccountName() {
        return accountName;
    }


    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }


    @Override
    public short getAccountType() {
        return accountType;
    }


    public void setAccountType(short accountType) {
        this.accountType = accountType;
    }


    @Override
    public long getDomainId() {
        return domainId;
    }


    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    @Override
    public String getDomainUuid() {
        return domainUuid;
    }




    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }




    @Override
    public String getDomainName() {
        return domainName;
    }


    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public String getDomainPath() {
        return domainPath;
    }


    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }


    public State getState() {
        return state;
    }


    public void setState(State state) {
        this.state = state;
    }


    public Date getCreated() {
        return created;
    }


    public void setCreated(Date created) {
        this.created = created;
    }


    public Date getRemoved() {
        return removed;
    }


    public void setRemoved(Date removed) {
        this.removed = removed;
    }


    public String getInstanceName() {
        return instanceName;
    }


    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }


    public String getPodUuid() {
        return podUuid;
    }




    public void setPodUuid(String podUuid) {
        this.podUuid = podUuid;
    }


    public String getDataCenterUuid() {
        return dataCenterUuid;
    }

    public void setDataCenterUuid(String zoneUuid) {
        this.dataCenterUuid = zoneUuid;
    }

    public String getDataCenterName() {
        return dataCenterName;
    }


    public void setDataCenterName(String zoneName) {
        this.dataCenterName = zoneName;
    }


    public Long getHostId() {
        return hostId;
    }


    public void setHostId(long hostId) {
        this.hostId = hostId;
    }


    public String getHostUuid() {
        return hostUuid;
    }




    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }




    public String getHostName() {
        return hostName;
    }


    public void setHostName(String hostName) {
        this.hostName = hostName;
    }


    public long getTemplateId() {
        return templateId;
    }


    public void setTemplateId(long templateId) {
        this.templateId = templateId;
    }



    public String getTemplateUuid() {
        return templateUuid;
    }




    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }





    public String getServiceOfferingUuid() {
        return serviceOfferingUuid;
    }


    public void setServiceOfferingUuid(String serviceOfferingUuid) {
        this.serviceOfferingUuid = serviceOfferingUuid;
    }




    public String getServiceOfferingName() {
        return serviceOfferingName;
    }


    public void setServiceOfferingName(String serviceOfferingName) {
        this.serviceOfferingName = serviceOfferingName;
    }

    public long getVpcId() {
        return vpcId;
    }

    public void setVpcId(long vpcId) {
        this.vpcId = vpcId;
    }




    public long getNicId() {
        return nicId;
    }


    public void setNicId(long nicId) {
        this.nicId = nicId;
    }


    public boolean isDefaultNic() {
        return isDefaultNic;
    }


    public void setDefaultNic(boolean isDefaultNic) {
        this.isDefaultNic = isDefaultNic;
    }


    public String getIpAddress() {
        return ipAddress;
    }


    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }


    public String getGateway() {
        return gateway;
    }


    public void setGateway(String gateway) {
        this.gateway = gateway;
    }


    public String getNetmask() {
        return netmask;
    }


    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }


    public String getMacAddress() {
        return macAddress;
    }


    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }


    public URI getBroadcastUri() {
        return broadcastUri;
    }


    public void setBroadcastUri(URI broadcastUri) {
        this.broadcastUri = broadcastUri;
    }


    public URI getIsolationUri() {
        return isolationUri;
    }


    public void setIsolationUri(URI isolationUri) {
        this.isolationUri = isolationUri;
    }


    public long getNetworkId() {
        return networkId;
    }


    public void setNetworkId(long networkId) {
        this.networkId = networkId;
    }


    public String getNetworkName() {
        return networkName;
    }




    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }




    public String getNetworkDomain() {
        return networkDomain;
    }




    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }




    public TrafficType getTrafficType() {
        return trafficType;
    }


    public void setTrafficType(TrafficType trafficType) {
        this.trafficType = trafficType;
    }



    public long getServiceOfferingId() {
        return serviceOfferingId;
    }




    public void setServiceOfferingId(long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }




    public long getProjectId() {
        return projectId;
    }




    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }




    @Override
    public String getProjectUuid() {
        return projectUuid;
    }




    public void setProjectUuid(String projectUuid) {
        this.projectUuid = projectUuid;
    }




    @Override
    public String getProjectName() {
        return projectName;
    }




    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }






    public String getVpcUuid() {
        return vpcUuid;
    }




    public void setVpcUuid(String vpcUuid) {
        this.vpcUuid = vpcUuid;
    }




    public String getNicUuid() {
        return nicUuid;
    }




    public void setNicUuid(String nicUuid) {
        this.nicUuid = nicUuid;
    }




    public String getNetworkUuid() {
        return networkUuid;
    }




    public void setNetworkUuid(String networkUuid) {
        this.networkUuid = networkUuid;
    }


    public long getJobId() {
        return jobId;
    }




    public void setJobId(long jobId) {
        this.jobId = jobId;
    }




    public String getJobUuid() {
        return jobUuid;
    }




    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }




    public int getJobStatus() {
        return jobStatus;
    }




    public void setJobStatus(int jobStatus) {
        this.jobStatus = jobStatus;
    }



    public Long getPodId() {
        return podId;
    }




    public void setPodId(Long podId) {
        this.podId = podId;
    }




    public long getDataCenterId() {
        return dataCenterId;
    }




    public void setDataCenterId(long zoneId) {
        this.dataCenterId = zoneId;
    }




    public String getDns1() {
        return dns1;
    }




    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }




    public String getDns2() {
        return dns2;
    }




    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }




    public String getTemplateVersion() {
        return templateVersion;
    }




    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }




    public String getScriptsVersion() {
        return scriptsVersion;
    }




    public void setScriptsVersion(String scriptsVersion) {
        this.scriptsVersion = scriptsVersion;
    }




    public RedundantState getRedundantState() {
        return redundantState;
    }




    public void setRedundantState(RedundantState redundantState) {
        this.redundantState = redundantState;
    }




    public boolean isRedundantRouter() {
        return isRedundantRouter;
    }




    public void setRedundantRouter(boolean isRedundantRouter) {
        this.isRedundantRouter = isRedundantRouter;
    }




    public GuestType getGuestType() {
        return guestType;
    }




    public void setGuestType(GuestType guestType) {
        this.guestType = guestType;
    }


}
