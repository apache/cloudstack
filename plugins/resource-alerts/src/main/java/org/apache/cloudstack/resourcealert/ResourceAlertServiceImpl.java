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

package org.apache.cloudstack.resourcealert;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.resourcealert.api.command.admin.CreateResourceAlertRuleCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.DeleteResourceAlertRuleCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.ListResourceAlertRulesCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.ListResourceAlertsCmd;
import org.apache.cloudstack.resourcealert.api.command.admin.UpdateResourceAlertRuleCmd;
import org.apache.cloudstack.resourcealert.api.response.ResourceAlertResponse;
import org.apache.cloudstack.resourcealert.api.response.ResourceAlertRuleResponse;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleJoinDao;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertRuleJoinVO;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertRuleVO;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertVO;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ManagerBase;

import org.apache.cloudstack.context.CallContext;

public class ResourceAlertServiceImpl extends ManagerBase implements ResourceAlertService {

    @Inject
    AccountManager accountManager;
    @Inject
    DomainDao domainDao;
    @Inject
    ResourceAlertRuleDao ruleDao;
    @Inject
    ResourceAlertRuleJoinDao ruleJoinDao;
    @Inject
    ResourceAlertDao alertDao;

    @Override
    public ResourceAlertRuleResponse createResourceAlertRule(CreateResourceAlertRuleCmd cmd) {
        ResourceAlertRule.ResourceType resourceType = parseResourceType(cmd.getResourceType());
        AlertCondition condition = parseCondition(cmd.getCondition());
        AlertSeverity severity = parseSeverity(cmd.getSeverity());
        ResourceAlertMetric metric = parseMetric(cmd.getMetric(), resourceType);

        int resetInterval = cmd.getResetInterval() != null ? cmd.getResetInterval() : 600;
        boolean email = cmd.getEmail() != null && cmd.getEmail();

        Account owner = resolveOwner(cmd.getAccountName(), cmd.getDomainId());
        long domainId = owner.getDomainId();

        ResourceAlertRuleVO rule = new ResourceAlertRuleVO(
                cmd.getName(), resourceType, cmd.getResourceId(),
                owner.getId(), domainId,
                metric.name(), condition, cmd.getThreshold(), severity,
                cmd.getMessage(), email, resetInterval);

        ruleDao.persist(rule);
        return toRuleResponse(ruleJoinDao.findById(rule.getId()));
    }

    @Override
    public ListResponse<ResourceAlertRuleResponse> listResourceAlertRules(ListResourceAlertRulesCmd cmd) {
        Long offset = cmd.getStartIndex();
        Long limit = cmd.getPageSizeVal();

        List<ResourceAlertRuleJoinVO> rules = ruleJoinDao.searchByFilters(
                cmd.getId(), cmd.getRuleName(), cmd.getResourceType(),
                cmd.getResourceId(), cmd.getAccountName(), cmd.getDomainId(),
                offset, limit);

        int count = ruleJoinDao.countByFilters(
                cmd.getId(), cmd.getRuleName(), cmd.getResourceType(),
                cmd.getResourceId(), cmd.getAccountName(), cmd.getDomainId());

        List<ResourceAlertRuleResponse> responses = rules.stream()
                .map(this::toRuleResponse)
                .collect(Collectors.toList());

        ListResponse<ResourceAlertRuleResponse> response = new ListResponse<>();
        response.setResponses(responses, count);
        return response;
    }

    @Override
    public ResourceAlertRuleResponse updateResourceAlertRule(UpdateResourceAlertRuleCmd cmd) {
        ResourceAlertRuleVO rule = ruleDao.findById(cmd.getId());
        if (rule == null || rule.getRemoved() != null) {
            throw new InvalidParameterValueException("Alert rule not found");
        }

        if (StringUtils.isNotBlank(cmd.getName())) rule.setName(cmd.getName());
        if (StringUtils.isNotBlank(cmd.getCondition())) rule.setCondition(parseCondition(cmd.getCondition()));
        if (cmd.getThreshold() != null) rule.setThreshold(cmd.getThreshold());
        if (StringUtils.isNotBlank(cmd.getSeverity())) rule.setSeverity(parseSeverity(cmd.getSeverity()));
        if (cmd.getMessage() != null) rule.setMessage(cmd.getMessage());
        if (cmd.getEmail() != null) rule.setEmail(cmd.getEmail());
        if (cmd.getResetInterval() != null) rule.setResetInterval(cmd.getResetInterval());
        rule.setUpdated(new Date());

        ruleDao.update(rule.getId(), rule);
        return toRuleResponse(ruleJoinDao.findById(rule.getId()));
    }

    @Override
    public boolean deleteResourceAlertRule(DeleteResourceAlertRuleCmd cmd) {
        ResourceAlertRuleVO rule = ruleDao.findById(cmd.getId());
        if (rule == null || rule.getRemoved() != null) {
            throw new InvalidParameterValueException("Alert rule not found");
        }
        return ruleDao.remove(cmd.getId());
    }

    @Override
    public ListResponse<ResourceAlertResponse> listResourceAlerts(ListResourceAlertsCmd cmd) {
        Long alertRuleInternalId = null;
        if (cmd.getAlertRuleId() != null) {
            ResourceAlertRuleVO rule = ruleDao.findByUuid(cmd.getAlertRuleId());
            if (rule == null) {
                throw new InvalidParameterValueException("Alert rule not found: " + cmd.getAlertRuleId());
            }
            alertRuleInternalId = rule.getId();
        }
        List<ResourceAlertVO> alerts = alertDao.listByFilters(
                alertRuleInternalId, cmd.getResourceId(),
                cmd.getSeverity(), cmd.getStartDate(), cmd.getEndDate());

        List<ResourceAlertResponse> responses = alerts.stream()
                .map(this::toAlertResponse)
                .collect(Collectors.toList());

        ListResponse<ResourceAlertResponse> response = new ListResponse<>();
        response.setResponses(responses, responses.size());
        return response;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmds = new ArrayList<>();
        cmds.add(CreateResourceAlertRuleCmd.class);
        cmds.add(ListResourceAlertRulesCmd.class);
        cmds.add(UpdateResourceAlertRuleCmd.class);
        cmds.add(DeleteResourceAlertRuleCmd.class);
        cmds.add(ListResourceAlertsCmd.class);
        return cmds;
    }

    private ResourceAlertRuleResponse toRuleResponse(ResourceAlertRuleJoinVO vo) {
        if (vo == null) return null;
        ResourceAlertRuleResponse r = new ResourceAlertRuleResponse();
        r.setObjectName("resourcealertrule");
        r.setId(vo.getUuid());
        r.setName(vo.getName());
        r.setResourceType(vo.getResourceType() != null ? vo.getResourceType().name() : null);
        r.setResourceId(vo.getResourceId() != null ? String.valueOf(vo.getResourceId()) : null);
        r.setMetric(vo.getMetric());
        r.setCondition(vo.getCondition() != null ? vo.getCondition().name() : null);
        r.setThreshold(vo.getThreshold());
        r.setSeverity(vo.getSeverity() != null ? vo.getSeverity().name() : null);
        r.setMessage(vo.getMessage());
        r.setEmail(vo.isEmail());
        r.setResetInterval(vo.getResetInterval());
        r.setAccountName(vo.getAccountName());
        r.setDomainId(vo.getDomainUuid());
        r.setDomainName(vo.getDomainName());
        r.setCreated(vo.getCreated());
        return r;
    }

    private ResourceAlertResponse toAlertResponse(ResourceAlertVO vo) {
        ResourceAlertResponse r = new ResourceAlertResponse();
        r.setObjectName("resourcealert");
        r.setId(vo.getUuid());
        ResourceAlertRuleVO rule = ruleDao.findById(vo.getAlertRuleId());
        r.setAlertRuleId(rule != null ? rule.getUuid() : null);
        r.setResourceId(vo.getResourceId() != null ? String.valueOf(vo.getResourceId()) : null);
        r.setMetricType(vo.getMetricType());
        r.setMetricValue(vo.getMetricValue());
        r.setSeverity(vo.getSeverity() != null ? vo.getSeverity().name() : null);
        r.setMessage(vo.getMessage());
        r.setAlertTimestamp(vo.getAlertTimestamp());
        return r;
    }

    private Account resolveOwner(String accountName, Long domainId) {
        if (StringUtils.isNotBlank(accountName) && domainId != null) {
            Domain domain = domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Domain not found");
            }
            Account account = accountManager.getActiveAccountByName(accountName, domainId);
            if (account == null) {
                throw new InvalidParameterValueException("Account not found in the specified domain");
            }
            return account;
        }
        return accountManager.getActiveAccountById(
                CallContext.current().getCallingAccountId());
    }

    private ResourceAlertRule.ResourceType parseResourceType(String value) {
        ResourceAlertRule.ResourceType type = EnumUtils.getEnum(ResourceAlertRule.ResourceType.class, value);
        if (type == null) {
            throw new InvalidParameterValueException("Invalid resourcetype: " + value);
        }
        return type;
    }

    private AlertCondition parseCondition(String value) {
        AlertCondition cond = EnumUtils.getEnum(AlertCondition.class, value != null ? value.toUpperCase() : null);
        if (cond == null) {
            throw new InvalidParameterValueException("Invalid condition: " + value + ". Valid values: GT, GTE, LT, LTE, EQ");
        }
        return cond;
    }

    private AlertSeverity parseSeverity(String value) {
        AlertSeverity sev = EnumUtils.getEnum(AlertSeverity.class, value != null ? value.toUpperCase() : null);
        if (sev == null) {
            throw new InvalidParameterValueException("Invalid severity: " + value + ". Valid values: CRITICAL, HIGH, MEDIUM, LOW");
        }
        return sev;
    }

    private ResourceAlertMetric parseMetric(String value, ResourceAlertRule.ResourceType resourceType) {
        ResourceAlertMetric metric = EnumUtils.getEnum(ResourceAlertMetric.class, value != null ? value.toUpperCase() : null);
        if (metric == null) {
            throw new InvalidParameterValueException("Invalid metric: " + value);
        }
        if (!metric.appliesTo(resourceType)) {
            throw new InvalidParameterValueException(
                    "Metric " + metric.name() + " does not apply to resource type " + resourceType.name());
        }
        return metric;
    }
}
