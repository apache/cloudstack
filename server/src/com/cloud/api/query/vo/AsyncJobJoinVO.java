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

import com.cloud.async.AsyncJob.Type;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name="async_job_view")
public class AsyncJobJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name="id")
    private long id;

    @Column(name="uuid")
    private String uuid;

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


    @Column(name="user_id")
    private long userId;

    @Column(name="user_uuid")
    private String userUuid;

    @Column(name="job_cmd")
    private String cmd;

    @Column(name="job_status")
    private int status;

    @Column(name="job_process_status")
    private int processStatus;

    @Column(name="job_result_code")
    private int resultCode;

    @Column(name="job_result", length=65535)
    private String result;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Enumerated(value=EnumType.STRING)
    @Column(name="instance_type", length=64)
    private Type instanceType;

    @Column(name="instance_id", length=64)
    private Long instanceId;

    @Column(name="instance_uuid")
    private String instanceUuid;


    public AsyncJobJoinVO() {
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


    public long getAccountId() {
        return accountId;
    }


    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }


    public String getAccountUuid() {
        return accountUuid;
    }


    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }


    public String getAccountName() {
        return accountName;
    }


    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }


    public short getAccountType() {
        return accountType;
    }


    public void setAccountType(short accountType) {
        this.accountType = accountType;
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


    public long getUserId() {
        return userId;
    }


    public void setUserId(long userId) {
        this.userId = userId;
    }


    public String getUserUuid() {
        return userUuid;
    }


    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }


    public String getCmd() {
        return cmd;
    }


    public void setCmd(String cmd) {
        this.cmd = cmd;
    }


    public int getStatus() {
        return status;
    }


    public void setStatus(int status) {
        this.status = status;
    }


    public int getProcessStatus() {
        return processStatus;
    }


    public void setProcessStatus(int processStatus) {
        this.processStatus = processStatus;
    }


    public int getResultCode() {
        return resultCode;
    }


    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }


    public String getResult() {
        return result;
    }


    public void setResult(String result) {
        this.result = result;
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


    public Type getInstanceType() {
        return instanceType;
    }


    public void setInstanceType(Type instanceType) {
        this.instanceType = instanceType;
    }


    public Long getInstanceId() {
        return instanceId;
    }


    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }


    public String getInstanceUuid() {
        return instanceUuid;
    }


    public void setInstanceUuid(String instanceUuid) {
        this.instanceUuid = instanceUuid;
    }

}
