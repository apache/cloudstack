package com.cloud.async.executor;

import java.util.ArrayList;
import java.util.List;

import com.cloud.network.security.NetworkGroupRulesVO;
import com.cloud.serializer.Param;

public class NetworkGroupResultObject {
    @Param(name="id")
    private Long id;

    @Param(name="name")
    private String name;

    @Param(name="description")
    private String description;

    @Param(name="domainid")
    private Long domainId;

    @Param(name="accountid")
    private Long accountId;
    
    @Param(name="accountname")
    private String accountName = null;

    @Param(name="ingressrules")
    private List<IngressRuleResultObject> ingressRules = null;

    public NetworkGroupResultObject() {}

    public NetworkGroupResultObject(Long id, String name, String description, Long domainId, Long accountId, String accountName, List<IngressRuleResultObject> ingressRules) {
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

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Long getAccountId() {
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

    public static List<NetworkGroupResultObject> transposeNetworkGroups(List<NetworkGroupRulesVO> groups) {
        List<NetworkGroupResultObject> resultObjects = new ArrayList<NetworkGroupResultObject>();

        if ((groups != null) && !groups.isEmpty()) {
            List<IngressRuleResultObject> ingressDataList = new ArrayList<IngressRuleResultObject>();
            NetworkGroupResultObject currentGroup = null;

            List<Long> processedGroups = new ArrayList<Long>();
            for (NetworkGroupRulesVO netGroupRule : groups) {
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
                    NetworkGroupResultObject groupResult = new NetworkGroupResultObject();
                    groupResult.setId(netGroupRule.getId());
                    groupResult.setName(netGroupRule.getName());
                    groupResult.setDescription(netGroupRule.getDescription());
                    groupResult.setDomainId(netGroupRule.getDomainId());
                    groupResult.setAccountId(netGroupRule.getAccountId());
                    groupResult.setAccountName(netGroupRule.getAccountName());

                    currentGroup = groupResult;
                }

                if (netGroupRule.getRuleId() != null) {
                    // there's at least one ingress rule for this network group, add the ingress rule data
                    IngressRuleResultObject ingressData = new IngressRuleResultObject();
                    ingressData.setEndPort(netGroupRule.getEndPort());
                    ingressData.setStartPort(netGroupRule.getStartPort());
                    ingressData.setId(netGroupRule.getRuleId());
                    ingressData.setProtocol(netGroupRule.getProtocol());

                    if (netGroupRule.getAllowedNetworkGroup() != null) {
                        ingressData.setAllowedNetworkGroup(netGroupRule.getAllowedNetworkGroup());
                        ingressData.setAllowedNetGroupAcct(netGroupRule.getAllowedNetGrpAcct());
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
