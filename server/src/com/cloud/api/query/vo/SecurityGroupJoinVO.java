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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.network.security.SecurityRule.SecurityRuleType;
import com.cloud.server.ResourceTag.TaggedResourceType;

@Entity
@Table(name="security_group_view")
public class SecurityGroupJoinVO extends BaseViewVO implements ControlledViewEntity{

    @Id
    @Column(name="id", updatable=false, nullable = false)
    private long id;

    @Column(name="name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "uuid")
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

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "rule_uuid")
    private String ruleUuid;

    @Column(name = "rule_start_port")
    private int ruleStartPort;

    @Column(name = "rule_end_port")
    private int ruleEndPort;

    @Column(name = "rule_protocol")
    private String ruleProtocol;

    @Column(name = "rule_type")
    private String ruleType;

    @Column(name = "rule_allowed_network_id")
    private Long ruleAllowedNetworkId = null;

    @Column(name = "rule_allowed_ip_cidr")
    private String ruleAllowedSourceIpCidr = null;

    @Column(name="tag_id")
    private long tagId;

    @Column(name="tag_uuid")
    private String tagUuid;

    @Column(name="tag_key")
    private String tagKey;

    @Column(name="tag_value")
    private String tagValue;

    @Column(name="tag_domain_id")
    private long tagDomainId;

    @Column(name="tag_account_id")
    private long tagAccountId;

    @Column(name="tag_resource_id")
    private long tagResourceId;

    @Column(name="tag_resource_uuid")
    private String tagResourceUuid;

    @Column(name="tag_resource_type")
    @Enumerated(value=EnumType.STRING)
    private TaggedResourceType tagResourceType;

    @Column(name="tag_customer")
    private String tagCustomer;

    public SecurityGroupJoinVO() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleUuid() {
        return ruleUuid;
    }

    public void setRuleUuid(String ruleUuid) {
        this.ruleUuid = ruleUuid;
    }

    public int getRuleStartPort() {
        return ruleStartPort;
    }

    public void setRuleStartPort(int ruleStartPort) {
        this.ruleStartPort = ruleStartPort;
    }

    public int getRuleEndPort() {
        return ruleEndPort;
    }

    public void setRuleEndPort(int ruleEndPort) {
        this.ruleEndPort = ruleEndPort;
    }

    public String getRuleProtocol() {
        return ruleProtocol;
    }

    public void setRuleProtocol(String ruleProtocol) {
        this.ruleProtocol = ruleProtocol;
    }

    public SecurityRuleType getRuleType() {
        if ("ingress".equalsIgnoreCase(ruleType)) {
            return SecurityRuleType.IngressRule;
        } else {
            return SecurityRuleType.EgressRule;
        }
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public Long getRuleAllowedNetworkId() {
        return ruleAllowedNetworkId;
    }

    public void setRuleAllowedNetworkId(Long ruleAllowedNetworkId) {
        this.ruleAllowedNetworkId = ruleAllowedNetworkId;
    }

    public String getRuleAllowedSourceIpCidr() {
        return ruleAllowedSourceIpCidr;
    }

    public void setRuleAllowedSourceIpCidr(String ruleAllowedSourceIpCidr) {
        this.ruleAllowedSourceIpCidr = ruleAllowedSourceIpCidr;
    }

    public long getTagId() {
        return tagId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }

    public String getTagUuid() {
        return tagUuid;
    }

    public void setTagUuid(String tagUuid) {
        this.tagUuid = tagUuid;
    }

    public String getTagKey() {
        return tagKey;
    }

    public void setTagKey(String tagKey) {
        this.tagKey = tagKey;
    }

    public String getTagValue() {
        return tagValue;
    }

    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }

    public long getTagDomainId() {
        return tagDomainId;
    }

    public void setTagDomainId(long tagDomainId) {
        this.tagDomainId = tagDomainId;
    }

    public long getTagAccountId() {
        return tagAccountId;
    }

    public void setTagAccountId(long tagAccountId) {
        this.tagAccountId = tagAccountId;
    }

    public long getTagResourceId() {
        return tagResourceId;
    }

    public void setTagResourceId(long tagResourceId) {
        this.tagResourceId = tagResourceId;
    }

    public String getTagResourceUuid() {
        return tagResourceUuid;
    }

    public void setTagResourceUuid(String tagResourceUuid) {
        this.tagResourceUuid = tagResourceUuid;
    }

    public TaggedResourceType getTagResourceType() {
        return tagResourceType;
    }

    public void setTagResourceType(TaggedResourceType tagResourceType) {
        this.tagResourceType = tagResourceType;
    }

    public String getTagCustomer() {
        return tagCustomer;
    }

    public void setTagCustomer(String tagCustomer) {
        this.tagCustomer = tagCustomer;
    }

}
