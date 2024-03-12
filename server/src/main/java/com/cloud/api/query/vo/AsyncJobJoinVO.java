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

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.framework.jobs.AsyncJob;

import com.cloud.user.Account;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "async_job_view")
public class AsyncJobJoinVO extends BaseViewVO implements ControlledViewEntity { //InternalIdentity, Identity {
    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "account_uuid")
    private String accountUuid;

    @Column(name = "account_name")
    private String accountName = null;

    @Column(name = "account_type")
    @Enumerated(value = EnumType.ORDINAL)
    private Account.Type accountType;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName = null;

    @Column(name = "domain_path")
    private String domainPath = null;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "user_uuid")
    private String userUuid;

    @Column(name = "job_cmd")
    private String cmd;

    @Column(name = "job_executing_msid")
    private Long executingMsid;

    @Column(name = "job_status")
    private int status;

    @Column(name = "job_process_status")
    private int processStatus;

    @Column(name = "job_result_code")
    private int resultCode;

    @Column(name = "job_result", length = 65535)
    private String result;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "instance_type", length = 64)
    private ApiCommandResourceType instanceType;

    @Column(name = "instance_id", length = 64)
    private Long instanceId;

    @Column(name = "instance_uuid")
    private String instanceUuid;

    public AsyncJobJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
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
    public Account.Type getAccountType() {
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

    public long getUserId() {
        return userId;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public String getCmd() {
        return cmd;
    }

    public int getStatus() {
        return status;
    }

    public int getProcessStatus() {
        return processStatus;
    }

    public int getResultCode() {
        return resultCode;
    }

    public String getResult() {
        return result;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public ApiCommandResourceType getInstanceType() {
        return instanceType;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public String getInstanceUuid() {
        return instanceUuid;
    }

    @Override
    public Class<?> getEntityType() {
        return AsyncJob.class;
    }

    @Override
    public String getName() {
        return null;
    }

    public Long getExecutingMsid() {
        return executingMsid;
    }

    @Override
    public String getProjectUuid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProjectName() {
        // TODO Auto-generated method stub
        return null;
    }

}
