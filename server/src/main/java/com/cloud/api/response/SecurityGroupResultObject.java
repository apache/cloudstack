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
package com.cloud.api.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.api.ApiDBUtils;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupRules;
import com.cloud.serializer.Param;
import com.cloud.user.Account;

public class SecurityGroupResultObject implements ControlledEntity, InternalIdentity {
    @Param(name = "id")
    private Long id;

    @Param(name = "name")
    private String name;

    @Param(name = "description")
    private String description;

    @Param(name = "domainid")
    private long domainId;

    @Param(name = "accountid")
    private long accountId;

    @Param(name = "accountname")
    private String accountName = null;

    @Param(name = "securitygrouprules")
    private List<SecurityGroupRuleResultObject> securityGroupRules = null;

    public SecurityGroupResultObject() {
    }

    public SecurityGroupResultObject(Long id, String name, String description, long domainId, long accountId, String accountName,
            List<SecurityGroupRuleResultObject> ingressRules) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.domainId = domainId;
        this.accountId = accountId;
        this.accountName = accountName;
        securityGroupRules = ingressRules;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public List<SecurityGroupRuleResultObject> getSecurityGroupRules() {
        return securityGroupRules;
    }

    public void setSecurityGroupRules(List<SecurityGroupRuleResultObject> securityGroupRules) {
        this.securityGroupRules = securityGroupRules;
    }

    public static List<SecurityGroupResultObject> transposeNetworkGroups(List<? extends SecurityGroupRules> groups) {
        List<SecurityGroupResultObject> resultObjects = new ArrayList<SecurityGroupResultObject>();
        Map<Long, SecurityGroup> allowedSecurityGroups = new HashMap<Long, SecurityGroup>();
        Map<Long, Account> accounts = new HashMap<Long, Account>();

        if ((groups != null) && !groups.isEmpty()) {
            List<SecurityGroupRuleResultObject> securityGroupRuleDataList = new ArrayList<SecurityGroupRuleResultObject>();
            SecurityGroupResultObject currentGroup = null;

            List<Long> processedGroups = new ArrayList<Long>();
            for (SecurityGroupRules netGroupRule : groups) {
                Long groupId = netGroupRule.getId();
                if (!processedGroups.contains(groupId)) {
                    processedGroups.add(groupId);

                    if (currentGroup != null) {
                        if (!securityGroupRuleDataList.isEmpty()) {
                            currentGroup.setSecurityGroupRules(securityGroupRuleDataList);
                            securityGroupRuleDataList = new ArrayList<SecurityGroupRuleResultObject>();
                        }
                        resultObjects.add(currentGroup);
                    }

                    // start a new group
                    SecurityGroupResultObject groupResult = new SecurityGroupResultObject();
                    groupResult.setId(netGroupRule.getId());
                    groupResult.setName(netGroupRule.getName());
                    groupResult.setDescription(netGroupRule.getDescription());
                    groupResult.setDomainId(netGroupRule.getDomainId());

                    Account account = accounts.get(netGroupRule.getAccountId());
                    if (account == null) {
                        account = ApiDBUtils.findAccountById(netGroupRule.getAccountId());
                        accounts.put(account.getId(), account);
                    }

                    groupResult.setAccountId(account.getId());
                    groupResult.setAccountName(account.getAccountName());

                    currentGroup = groupResult;
                }

                if (netGroupRule.getRuleId() != null) {
                    // there's at least one securitygroup rule for this network group, add the securitygroup rule data
                    SecurityGroupRuleResultObject securityGroupRuleData = new SecurityGroupRuleResultObject();
                    securityGroupRuleData.setEndPort(netGroupRule.getEndPort());
                    securityGroupRuleData.setStartPort(netGroupRule.getStartPort());
                    securityGroupRuleData.setId(netGroupRule.getRuleId());
                    securityGroupRuleData.setProtocol(netGroupRule.getProtocol());
                    securityGroupRuleData.setRuleType(netGroupRule.getRuleType());
                    Long allowedSecurityGroupId = netGroupRule.getAllowedNetworkId();
                    if (allowedSecurityGroupId != null) {
                        SecurityGroup allowedSecurityGroup = allowedSecurityGroups.get(allowedSecurityGroupId);
                        if (allowedSecurityGroup == null) {
                            allowedSecurityGroup = ApiDBUtils.findSecurityGroupById(allowedSecurityGroupId);
                            allowedSecurityGroups.put(allowedSecurityGroupId, allowedSecurityGroup);
                        }

                        securityGroupRuleData.setAllowedSecurityGroup(allowedSecurityGroup.getName());

                        Account allowedAccount = accounts.get(allowedSecurityGroup.getAccountId());
                        if (allowedAccount == null) {
                            allowedAccount = ApiDBUtils.findAccountById(allowedSecurityGroup.getAccountId());
                            accounts.put(allowedAccount.getId(), allowedAccount);
                        }

                        securityGroupRuleData.setAllowedSecGroupAcct(allowedAccount.getAccountName());
                    } else if (netGroupRule.getAllowedSourceIpCidr() != null) {
                        securityGroupRuleData.setAllowedSourceIpCidr(netGroupRule.getAllowedSourceIpCidr());
                    }
                    securityGroupRuleDataList.add(securityGroupRuleData);
                }
            }

            // all rules have been processed, add the final data into the list
            if (currentGroup != null) {
                if (!securityGroupRuleDataList.isEmpty()) {
                    currentGroup.setSecurityGroupRules(securityGroupRuleDataList);
                }
                resultObjects.add(currentGroup);
            }
        }
        return resultObjects;
    }

    @Override
    public Class<?> getEntityType() {
        return SecurityGroup.class;
    }
}
