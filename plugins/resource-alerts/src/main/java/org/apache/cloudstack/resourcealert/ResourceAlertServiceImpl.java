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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
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
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleWebhookDao;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertRuleJoinVO;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertRuleVO;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.webhook.WebhookHelper;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.apache.commons.lang3.StringUtils;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.dao.HostDao;
import com.cloud.projects.Project;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.dao.UserVmDao;

import org.apache.cloudstack.context.CallContext;

public class ResourceAlertServiceImpl extends ManagerBase implements ResourceAlertService {

    @Inject
    AccountManager accountManager;
    @Inject
    ResourceAlertRuleDao ruleDao;
    @Inject
    ResourceAlertRuleJoinDao ruleJoinDao;
    @Inject
    ResourceAlertDao alertDao;
    @Inject
    ResourceAlertRuleWebhookDao ruleWebhookDao;
    @Inject
    UserVmDao userVmDao;
    @Inject
    VolumeDao volumeDao;
    @Inject
    HostDao hostDao;
    @Inject
    PrimaryDataStoreDao storagePoolDao;

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_RESOURCE_ALERT_RULE_CREATE, eventDescription = "creating resource alert rule")
    public ResourceAlertRuleResponse createResourceAlertRule(CreateResourceAlertRuleCmd cmd) {
        ResourceAlertRule.ResourceType resourceType = parseResourceType(cmd.getResourceType());
        AlertCondition condition = parseCondition(cmd.getCondition());
        AlertSeverity severity = parseSeverity(cmd.getSeverity());
        ResourceAlertMetric metric = parseMetric(cmd.getMetric(), resourceType);

        int resetInterval = cmd.getResetInterval() != null ? cmd.getResetInterval() : ResourceAlertManagerImpl.DEFAULT_RESET_INTERVAL.value();
        boolean email = cmd.getEmail() != null && cmd.getEmail();

        Account caller = CallContext.current().getCallingAccount();
        checkInfrastructureAccess(caller, resourceType);
        checkEmailAccess(caller, email);
        Account owner = accountManager.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), null);

        int limit = ResourceAlertManagerImpl.RULES_PER_ACCOUNT_LIMIT.valueIn(owner.getId());
        if (limit > 0 && ruleDao.countActiveByAccountId(owner.getId()) >= limit) {
            throw new InvalidParameterValueException(
                    "Account has reached the maximum of " + limit + " resource alert rules");
        }
        long domainId = owner.getDomainId();

        InternalIdentity resource = findResourceOrFail(resourceType, cmd.getResourceId());
        if (resource instanceof ControlledEntity) {
            accountManager.checkAccess(owner, null, false, (ControlledEntity) resource);
        }
        Long resourceId = resource != null ? resource.getId() : null;

        ResourceAlertRuleVO rule = new ResourceAlertRuleVO(
                cmd.getName(), resourceType, resourceId,
                owner.getId(), domainId,
                metric.name(), condition, cmd.getThreshold(), severity,
                cmd.getMessage(), email, resetInterval);

        List<Long> webhookIds = resolveWebhookIds(owner, cmd.getWebhookIds());
        ruleDao.persist(rule);
        CallContext.current().setEventResourceId(rule.getId());
        CallContext.current().setEventDetails("Rule: " + rule.getName());
        if (!webhookIds.isEmpty()) {
            ruleWebhookDao.replaceWebhooksForRule(rule.getId(), webhookIds);
        }
        return toRuleResponse(ruleJoinDao.findById(rule.getId()));
    }

    @Override
    public ListResponse<ResourceAlertRuleResponse> listResourceAlertRules(ListResourceAlertRulesCmd cmd) {
        Long resourceId = resolveResourceIdFilter(cmd.getResourceType(), cmd.getResourceId());
        ResourceAlertRule.ResourceType resourceType = StringUtils.isNotBlank(cmd.getResourceType()) ?
                parseResourceType(cmd.getResourceType()) : null;

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<>();
        Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject =
                new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, cmd.getId(), cmd.getAccountName(), null,
                permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        SearchBuilder<ResourceAlertRuleJoinVO> sb = createAclSearchBuilder(domainIdRecursiveListProject, permittedAccounts);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("resourceType", sb.entity().getResourceType(), SearchCriteria.Op.EQ);
        sb.and("resourceId", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
        SearchCriteria<ResourceAlertRuleJoinVO> sc = createAclSearchCriteria(sb, domainIdRecursiveListProject, permittedAccounts);
        if (cmd.getId() != null) sc.setParameters("id", cmd.getId());
        if (StringUtils.isNotBlank(cmd.getRuleName())) sc.setParameters("name", cmd.getRuleName());
        if (StringUtils.isNotBlank(cmd.getKeyword())) sc.setParameters("keyword", "%" + cmd.getKeyword() + "%");
        if (resourceType != null) sc.setParameters("resourceType", resourceType);
        if (resourceId != null) sc.setParameters("resourceId", resourceId);

        Filter filter = new Filter(ResourceAlertRuleJoinVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Pair<List<ResourceAlertRuleJoinVO>, Integer> rules = ruleJoinDao.searchAndCount(sc, filter);

        List<ResourceAlertRuleResponse> responses = rules.first().stream()
                .map(this::toRuleResponse)
                .collect(Collectors.toList());

        ListResponse<ResourceAlertRuleResponse> response = new ListResponse<>();
        response.setResponses(responses, rules.second());
        return response;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_RESOURCE_ALERT_RULE_UPDATE, eventDescription = "updating resource alert rule")
    public ResourceAlertRuleResponse updateResourceAlertRule(UpdateResourceAlertRuleCmd cmd) {
        ResourceAlertRuleVO rule = findRuleForCaller(cmd.getId());
        checkEmailAccess(CallContext.current().getCallingAccount(), Boolean.TRUE.equals(cmd.getEmail()));

        if (StringUtils.isNotBlank(cmd.getName())) rule.setName(cmd.getName());
        if (StringUtils.isNotBlank(cmd.getCondition())) rule.setCondition(parseCondition(cmd.getCondition()));
        if (cmd.getThreshold() != null) rule.setThreshold(cmd.getThreshold());
        if (StringUtils.isNotBlank(cmd.getSeverity())) rule.setSeverity(parseSeverity(cmd.getSeverity()));
        if (cmd.getMessage() != null) rule.setMessage(cmd.getMessage());
        if (cmd.getEmail() != null) rule.setEmail(cmd.getEmail());
        if (cmd.getResetInterval() != null) rule.setResetInterval(cmd.getResetInterval());
        rule.setUpdated(new Date());

        if (cmd.isCleanupWebhooks()) {
            ruleWebhookDao.replaceWebhooksForRule(rule.getId(), new ArrayList<>());
        } else if (cmd.getWebhookIds() != null) {
            Account owner = accountManager.getAccount(rule.getAccountId());
            ruleWebhookDao.replaceWebhooksForRule(rule.getId(), resolveWebhookIds(owner, cmd.getWebhookIds()));
        }
        ruleDao.update(rule.getId(), rule);
        return toRuleResponse(ruleJoinDao.findById(rule.getId()));
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_RESOURCE_ALERT_RULE_DELETE, eventDescription = "deleting resource alert rule")
    public boolean deleteResourceAlertRule(DeleteResourceAlertRuleCmd cmd) {
        findRuleForCaller(cmd.getId());
        return ruleDao.remove(cmd.getId());
    }

    @Override
    public ListResponse<ResourceAlertResponse> listResourceAlerts(ListResourceAlertsCmd cmd) {
        Long resourceId = resolveResourceIdFilter(cmd.getResourceType(), cmd.getResourceId());
        List<Long> alertRuleIds = null;
        if (cmd.getAlertRuleId() != null) {
            ResourceAlertRuleVO rule = ruleDao.findByUuid(cmd.getAlertRuleId());
            if (rule == null) {
                throw new InvalidParameterValueException("Alert rule not found: " + cmd.getAlertRuleId());
            }
            accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, rule);
            alertRuleIds = new ArrayList<>(List.of(rule.getId()));
        }
        List<Long> visibleRuleIds = listVisibleRuleIds(cmd);
        if (visibleRuleIds != null) {
            if (alertRuleIds == null) {
                alertRuleIds = visibleRuleIds;
            } else {
                alertRuleIds.retainAll(visibleRuleIds);
            }
        }
        if (StringUtils.isNotBlank(cmd.getResourceType())) {
            List<Long> typeRuleIds = ruleDao.listIdsByResourceType(parseResourceType(cmd.getResourceType()));
            if (alertRuleIds == null) {
                alertRuleIds = new ArrayList<>(typeRuleIds);
            } else {
                alertRuleIds.retainAll(typeRuleIds);
            }
        }
        if (alertRuleIds != null && alertRuleIds.isEmpty()) {
            ListResponse<ResourceAlertResponse> empty = new ListResponse<>();
            empty.setResponses(new ArrayList<>(), 0);
            return empty;
        }
        Pair<List<ResourceAlertVO>, Integer> alerts = alertDao.searchAndCountByFilters(
                alertRuleIds, resourceId, cmd.getSeverity(), cmd.getStartDate(), cmd.getEndDate(),
                cmd.getStartIndex(), cmd.getPageSizeVal());

        Map<Long, ResourceAlertRuleVO> rules = new HashMap<>();
        List<ResourceAlertResponse> responses = alerts.first().stream()
                .map(alert -> toAlertResponse(alert, rules.computeIfAbsent(alert.getAlertRuleId(), ruleDao::findByIdIncludingRemoved)))
                .collect(Collectors.toList());

        ListResponse<ResourceAlertResponse> response = new ListResponse<>();
        response.setResponses(responses, alerts.second());
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
        r.setResourceId(getResourceUuid(vo.getResourceType(), vo.getResourceId()));
        r.setMetric(vo.getMetric());
        r.setCondition(vo.getCondition() != null ? vo.getCondition().name() : null);
        r.setThreshold(vo.getThreshold());
        r.setSeverity(vo.getSeverity() != null ? vo.getSeverity().name() : null);
        r.setMessage(vo.getMessage());
        r.setEmail(vo.isEmail());
        r.setResetInterval(vo.getResetInterval());
        r.setWebhookIds(getWebhookUuids(vo.getId()));
        r.setAccountName(vo.getAccountName());
        r.setDomainId(vo.getDomainUuid());
        r.setDomainName(vo.getDomainName());
        r.setCreated(vo.getCreated());
        return r;
    }

    private ResourceAlertResponse toAlertResponse(ResourceAlertVO vo, ResourceAlertRuleVO rule) {
        ResourceAlertResponse r = new ResourceAlertResponse();
        r.setObjectName("resourcealert");
        r.setId(vo.getUuid());
        r.setAlertRuleId(rule != null ? rule.getUuid() : null);
        r.setResourceId(rule != null ? getResourceUuid(rule.getResourceType(), vo.getResourceId()) : null);
        r.setMetricType(vo.getMetricType());
        r.setMetricValue(vo.getMetricValue());
        r.setSeverity(vo.getSeverity() != null ? vo.getSeverity().name() : null);
        r.setMessage(vo.getMessage());
        r.setAlertTimestamp(vo.getAlertTimestamp());
        return r;
    }

    private InternalIdentity findResource(ResourceAlertRule.ResourceType type, String uuid) {
        switch (type) {
            case VirtualMachine:
                return userVmDao.findByUuid(uuid);
            case Volume:
                return volumeDao.findByUuid(uuid);
            case Host:
                return hostDao.findByUuid(uuid);
            case StoragePool:
                return storagePoolDao.findByUuid(uuid);
            default:
                return null;
        }
    }

    private InternalIdentity findResourceOrFail(ResourceAlertRule.ResourceType type, String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        InternalIdentity resource = findResource(type, uuid);
        if (resource == null) {
            throw new InvalidParameterValueException("Unable to find " + type.name() + " with ID " + uuid);
        }
        return resource;
    }

    private Long resolveResourceIdFilter(String resourceType, String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        if (StringUtils.isBlank(resourceType)) {
            throw new InvalidParameterValueException("resourcetype is required when resourceid is specified");
        }
        return findResourceOrFail(parseResourceType(resourceType), uuid).getId();
    }

    private String getResourceUuid(ResourceAlertRule.ResourceType type, Long id) {
        if (type == null || id == null) {
            return null;
        }
        Identity resource;
        switch (type) {
            case VirtualMachine:
                resource = userVmDao.findByIdIncludingRemoved(id);
                break;
            case Volume:
                resource = volumeDao.findByIdIncludingRemoved(id);
                break;
            case Host:
                resource = hostDao.findByIdIncludingRemoved(id);
                break;
            case StoragePool:
                resource = storagePoolDao.findByIdIncludingRemoved(id);
                break;
            default:
                resource = null;
        }
        return resource != null ? resource.getUuid() : null;
    }

    protected WebhookHelper getWebhookHelper() {
        try {
            return ComponentContext.getDelegateComponentOfType(WebhookHelper.class);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    private List<Long> resolveWebhookIds(Account owner, List<String> webhookUuids) {
        List<Long> ids = new ArrayList<>();
        if (webhookUuids == null || webhookUuids.isEmpty()) {
            return ids;
        }
        WebhookHelper webhookHelper = getWebhookHelper();
        if (webhookHelper == null) {
            throw new InvalidParameterValueException("Webhooks are not available, the webhook plugin is not enabled");
        }
        for (String uuid : webhookUuids) {
            ControlledEntity webhook = webhookHelper.findWebhookByUuid(uuid);
            if (!(webhook instanceof InternalIdentity)) {
                throw new InvalidParameterValueException("Unable to find webhook with ID " + uuid);
            }
            accountManager.checkAccess(owner, null, false, webhook);
            long id = ((InternalIdentity) webhook).getId();
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private List<String> getWebhookUuids(long ruleId) {
        List<Long> ids = ruleWebhookDao.listWebhookIdsByRule(ruleId);
        WebhookHelper webhookHelper = ids.isEmpty() ? null : getWebhookHelper();
        if (webhookHelper == null) {
            return new ArrayList<>();
        }
        return ids.stream().map(webhookHelper::getWebhookUuid).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void checkInfrastructureAccess(Account caller, ResourceAlertRule.ResourceType resourceType) {
        boolean infra = resourceType == ResourceAlertRule.ResourceType.Host
                || resourceType == ResourceAlertRule.ResourceType.StoragePool;
        if (infra && !accountManager.isRootAdmin(caller.getId())) {
            throw new PermissionDeniedException("Only root admins can create alert rules for " + resourceType.name());
        }
    }

    private void checkEmailAccess(Account caller, boolean email) {
        if (email && !accountManager.isRootAdmin(caller.getId())) {
            throw new PermissionDeniedException("Only root admins can enable email for alert rules");
        }
    }

    private ResourceAlertRuleVO findRuleForCaller(long id) {
        ResourceAlertRuleVO rule = ruleDao.findById(id);
        if (rule == null || rule.getRemoved() != null) {
            throw new InvalidParameterValueException("Alert rule not found");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, true, rule);
        CallContext.current().setEventResourceId(rule.getId());
        CallContext.current().setEventDetails("Rule: " + rule.getName());
        return rule;
    }

    private SearchBuilder<ResourceAlertRuleJoinVO> createAclSearchBuilder(
            Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject, List<Long> permittedAccounts) {
        SearchBuilder<ResourceAlertRuleJoinVO> sb = ruleJoinDao.createSearchBuilder();
        accountManager.buildACLSearchBuilder(sb, domainIdRecursiveListProject.first(), domainIdRecursiveListProject.second(),
                permittedAccounts, domainIdRecursiveListProject.third());
        return sb;
    }

    private SearchCriteria<ResourceAlertRuleJoinVO> createAclSearchCriteria(SearchBuilder<ResourceAlertRuleJoinVO> sb,
            Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject, List<Long> permittedAccounts) {
        SearchCriteria<ResourceAlertRuleJoinVO> sc = sb.create();
        accountManager.buildACLSearchCriteria(sc, domainIdRecursiveListProject.first(), domainIdRecursiveListProject.second(),
                permittedAccounts, domainIdRecursiveListProject.third());
        return sc;
    }

    // Returns null when the caller can see alerts of every rule.
    private List<Long> listVisibleRuleIds(ListResourceAlertsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<>();
        Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject =
                new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, null, cmd.getAccountName(), null,
                permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        if (permittedAccounts.isEmpty() && domainIdRecursiveListProject.first() == null) {
            return null;
        }
        SearchBuilder<ResourceAlertRuleJoinVO> sb = createAclSearchBuilder(domainIdRecursiveListProject, permittedAccounts);
        SearchCriteria<ResourceAlertRuleJoinVO> sc = createAclSearchCriteria(sb, domainIdRecursiveListProject, permittedAccounts);
        return ruleJoinDao.searchIncludingRemoved(sc, null, null, false).stream()
                .map(ResourceAlertRuleJoinVO::getId)
                .collect(Collectors.toList());
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
