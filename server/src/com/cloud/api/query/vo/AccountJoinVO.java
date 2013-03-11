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

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import com.cloud.user.Account.State;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name="account_view")
public class AccountJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name="id")
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name="account_name")
    private String accountName = null;

    @Column(name="type")
    private short type;


    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    private State state;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name="cleanup_needed")
    private boolean needsCleanup = false;

    @Column(name="network_domain")
    private String networkDomain;


    @Column(name="domain_id")
    private long domainId;

    @Column(name="domain_uuid")
    private String domainUuid;

    @Column(name="domain_name")
    private String domainName = null;

    @Column(name="domain_path")
    private String domainPath = null;


    @Column(name="data_center_id")
    private long dataCenterId;

    @Column(name="data_center_uuid")
    private String dataCenterUuid;

    @Column(name="data_center_name")
    private String dataCenterName;

    @Column(name="bytesReceived")
    private Long bytesReceived;

    @Column(name="bytesSent")
    private Long bytesSent;

    @Column(name="vmLimit")
    private Long vmLimit;

    @Column(name="vmTotal")
    private Long vmTotal;


    @Column(name="ipLimit")
    private Long ipLimit;

    @Column(name="ipTotal")
    private Long ipTotal;

    @Column(name="ipFree")
    private Long ipFree;

    @Column(name="volumeLimit")
    private Long volumeLimit;

    @Column(name="volumeTotal")
    private Long volumeTotal;

    @Column(name="snapshotLimit")
    private Long snapshotLimit;

    @Column(name="snapshotTotal")
    private Long snapshotTotal;

    @Column(name="templateLimit")
    private Long templateLimit;

    @Column(name="templateTotal")
    private Long templateTotal;

    @Column(name="stoppedVms")
    private Integer vmStopped;

    @Column(name="runningVms")
    private Integer vmRunning;

    @Column(name="projectLimit")
    private Long projectLimit;

    @Column(name="projectTotal")
    private Long projectTotal;


    @Column(name="networkLimit")
    private Long networkLimit;

    @Column(name="networkTotal")
    private Long networkTotal;


    @Column(name="vpcLimit")
    private Long vpcLimit;

    @Column(name="vpcTotal")
    private Long vpcTotal;


    @Column(name="cpuLimit")
    private Long cpuLimit;

    @Column(name="cpuTotal")
    private Long cpuTotal;


    @Column(name="memoryLimit")
    private Long memoryLimit;

    @Column(name="memoryTotal")
    private Long memoryTotal;

    @Column(name="job_id")
    private long jobId;

    @Column(name="job_uuid")
    private String jobUuid;

    @Column(name="job_status")
    private int jobStatus;

    public AccountJoinVO() {
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


    public String getAccountName() {
        return accountName;
    }


    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }


    public short getType() {
        return type;
    }


    public void setType(short type) {
        this.type = type;
    }


    public State getState() {
        return state;
    }


    public void setState(State state) {
        this.state = state;
    }


    public Date getRemoved() {
        return removed;
    }


    public void setRemoved(Date removed) {
        this.removed = removed;
    }


    public boolean isNeedsCleanup() {
        return needsCleanup;
    }


    public void setNeedsCleanup(boolean needsCleanup) {
        this.needsCleanup = needsCleanup;
    }


    public String getNetworkDomain() {
        return networkDomain;
    }


    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }


    public long getDomainId() {
        return domainId;
    }


    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }


    public String getDomainUuid() {
        return domainUuid;
    }


    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }


    public String getDomainName() {
        return domainName;
    }


    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }


    public String getDomainPath() {
        return domainPath;
    }


    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }


    public long getDataCenterId() {
        return dataCenterId;
    }


    public void setDataCenterId(long dataCenterId) {
        this.dataCenterId = dataCenterId;
    }


    public String getDataCenterUuid() {
        return dataCenterUuid;
    }


    public void setDataCenterUuid(String dataCenterUuid) {
        this.dataCenterUuid = dataCenterUuid;
    }


    public String getDataCenterName() {
        return dataCenterName;
    }


    public void setDataCenterName(String dataCenterName) {
        this.dataCenterName = dataCenterName;
    }


    public Long getBytesReceived() {
        return bytesReceived;
    }


    public void setBytesReceived(Long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }


    public Long getBytesSent() {
        return bytesSent;
    }


    public void setBytesSent(Long bytesSent) {
        this.bytesSent = bytesSent;
    }




    public Long getVmTotal() {
        return vmTotal;
    }


    public void setVmTotal(Long vmTotal) {
        this.vmTotal = vmTotal;
    }





    public Long getIpTotal() {
        return ipTotal;
    }


    public void setIpTotal(Long ipTotal) {
        this.ipTotal = ipTotal;
    }


    public Long getIpFree() {
        return ipFree;
    }


    public void setIpFree(Long ipFree) {
        this.ipFree = ipFree;
    }



    public Long getVolumeTotal() {
        return volumeTotal;
    }


    public void setVolumeTotal(Long volumeTotal) {
        this.volumeTotal = volumeTotal;
    }



    public Long getSnapshotTotal() {
        return snapshotTotal;
    }


    public void setSnapshotTotal(Long snapshotTotal) {
        this.snapshotTotal = snapshotTotal;
    }




    public Long getTemplateTotal() {
        return templateTotal;
    }


    public void setTemplateTotal(Long templateTotal) {
        this.templateTotal = templateTotal;
    }


    public Integer getVmStopped() {
        return vmStopped;
    }


    public void setVmStopped(Integer vmStopped) {
        this.vmStopped = vmStopped;
    }


    public Integer getVmRunning() {
        return vmRunning;
    }


    public void setVmRunning(Integer vmRunning) {
        this.vmRunning = vmRunning;
    }



    public Long getProjectTotal() {
        return projectTotal;
    }


    public void setProjectTotal(Long projectTotal) {
        this.projectTotal = projectTotal;
    }



    public Long getNetworkTotal() {
        return networkTotal;
    }


    public void setNetworkTotal(Long networkTotal) {
        this.networkTotal = networkTotal;
    }


    public Long getVpcTotal() {
        return vpcTotal;
    }


    public void setVpcTotal(Long vpcTotal) {
        this.vpcTotal = vpcTotal;
    }


    public Long getCpuTotal() {
        return cpuTotal;
    }


    public void setCpuTotal(Long cpuTotal) {
        this.cpuTotal = cpuTotal;
    }

    public Long getMemoryTotal() {
        return memoryTotal;
    }


    public void setMemoryTotal(Long memoryTotal) {
        this.memoryTotal = memoryTotal;
    }


    public Long getVmLimit() {
        return vmLimit;
    }


    public void setVmLimit(Long vmLimit) {
        this.vmLimit = vmLimit;
    }


    public Long getIpLimit() {
        return ipLimit;
    }


    public void setIpLimit(Long ipLimit) {
        this.ipLimit = ipLimit;
    }


    public Long getVolumeLimit() {
        return volumeLimit;
    }


    public void setVolumeLimit(Long volumeLimit) {
        this.volumeLimit = volumeLimit;
    }


    public Long getSnapshotLimit() {
        return snapshotLimit;
    }


    public void setSnapshotLimit(Long snapshotLimit) {
        this.snapshotLimit = snapshotLimit;
    }


    public Long getTemplateLimit() {
        return templateLimit;
    }


    public void setTemplateLimit(Long templateLimit) {
        this.templateLimit = templateLimit;
    }


    public Long getProjectLimit() {
        return projectLimit;
    }


    public void setProjectLimit(Long projectLimit) {
        this.projectLimit = projectLimit;
    }


    public Long getNetworkLimit() {
        return networkLimit;
    }


    public void setNetworkLimit(Long networkLimit) {
        this.networkLimit = networkLimit;
    }


    public Long getVpcLimit() {
        return vpcLimit;
    }


    public void setVpcLimit(Long vpcLimit) {
        this.vpcLimit = vpcLimit;
    }


    public Long getCpuLimit() {
        return cpuLimit;
    }


    public void setCpuLimit(Long cpuLimit) {
        this.cpuLimit = cpuLimit;
    }


    public Long getMemoryLimit() {
        return memoryLimit;
    }


    public void setMemoryLimit(Long memoryLimit) {
        this.memoryLimit = memoryLimit;
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




}
