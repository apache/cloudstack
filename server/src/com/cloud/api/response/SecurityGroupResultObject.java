/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.acl.ControlledEntity;
import com.cloud.api.ApiDBUtils;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupRules;
import com.cloud.serializer.Param;
import com.cloud.user.Account;

public class SecurityGroupResultObject implements ControlledEntity{
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

    @Param(name = "ingressrules")
    private List<IngressRuleResultObject> ingressRules = null;

    public SecurityGroupResultObject() {
    }

    public SecurityGroupResultObject(Long id, String name, String description, long domainId, long accountId, String accountName, List<IngressRuleResultObject> ingressRules) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.domainId = domainId;
        this.accountId = accountId;
        this.accountName = accountName;
        this.ingressRules = ingressRules;
    }

    public Long getId() {
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

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

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

    public List<IngressRuleResultObject> getIngressRules() {
        return ingressRules;
    }

    public void setIngressRules(List<IngressRuleResultObject> ingressRules) {
        this.ingressRules = ingressRules;
    }

    public static List<SecurityGroupResultObject> transposeNetworkGroups(List<? extends SecurityGroupRules> groups) {
        List<SecurityGroupResultObject> resultObjects = new ArrayList<SecurityGroupResultObject>();
        Map<Long, SecurityGroup> allowedSecurityGroups = new HashMap<Long, SecurityGroup>();
        Map<Long, Account> accounts = new HashMap<Long, Account>();

        if ((groups != null) && !groups.isEmpty()) {
            List<IngressRuleResultObject> ingressDataList = new ArrayList<IngressRuleResultObject>();
            SecurityGroupResultObject currentGroup = null;

            List<Long> processedGroups = new ArrayList<Long>();
            for (SecurityGroupRules netGroupRule : groups) {
                Long groupId = netGroupRule.getId();
                if (!processedGroups.contains(groupId)) {
                    processedGroups.add(groupId);

                    if (currentGroup != null) {
                        if (!ingressDataList.isEmpty()) {
                            currentGroup.setIngressRules(ingressDataList);
                            ingressDataList = new ArrayList<IngressRuleResultObject>();
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
                    // there's at least one ingress rule for this network group, add the ingress rule data
                    IngressRuleResultObject ingressData = new IngressRuleResultObject();
                    ingressData.setEndPort(netGroupRule.getEndPort());
                    ingressData.setStartPort(netGroupRule.getStartPort());
                    ingressData.setId(netGroupRule.getRuleId());
                    ingressData.setProtocol(netGroupRule.getProtocol());

                    Long allowedSecurityGroupId = netGroupRule.getAllowedNetworkId();
                    if (allowedSecurityGroupId != null) {
                        SecurityGroup allowedSecurityGroup = allowedSecurityGroups.get(allowedSecurityGroupId);
                        if (allowedSecurityGroup == null) {
                            allowedSecurityGroup = ApiDBUtils.findSecurityGroupById(allowedSecurityGroupId);
                            allowedSecurityGroups.put(allowedSecurityGroupId, allowedSecurityGroup);
                        }

                        ingressData.setAllowedSecurityGroup(allowedSecurityGroup.getName());

                        Account allowedAccount = accounts.get(allowedSecurityGroup.getAccountId());
                        if (allowedAccount == null) {
                            allowedAccount = ApiDBUtils.findAccountById(allowedSecurityGroup.getAccountId());
                            accounts.put(allowedAccount.getId(), allowedAccount);
                        }

                        ingressData.setAllowedSecGroupAcct(allowedAccount.getAccountName());
                    } else if (netGroupRule.getAllowedSourceIpCidr() != null) {
                        ingressData.setAllowedSourceIpCidr(netGroupRule.getAllowedSourceIpCidr());
                    }
                    ingressDataList.add(ingressData);
                }
            }

            // all rules have been processed, add the final data into the list
            if (currentGroup != null) {
                if (!ingressDataList.isEmpty()) {
                    currentGroup.setIngressRules(ingressDataList);
                }
                resultObjects.add(currentGroup);
            }
        }
        return resultObjects;
    }
}
