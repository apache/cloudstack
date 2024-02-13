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

package org.apache.cloudstack.mom.webhook;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mom.webhook.api.command.user.CreateWebhookRuleCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.DeleteWebhookRuleCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.ListWebhookRulesCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.UpdateWebhookRuleCmd;
import org.apache.cloudstack.mom.webhook.api.response.WebhookRuleResponse;
import org.apache.cloudstack.mom.webhook.dao.WebhookRuleDao;
import org.apache.cloudstack.mom.webhook.vo.WebhookRuleVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.domain.Domain;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.projects.Project;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Ternary;
import com.cloud.utils.UriUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.rest.HttpConstants;

public class WebhookApiServiceImpl extends ManagerBase implements WebhookApiService {
    public static final Logger LOGGER = Logger.getLogger(WebhookApiServiceImpl.class.getName());

    @Inject
    AccountManager accountManager;
    @Inject
    WebhookRuleDao webhookRuleDao;

    protected WebhookRuleResponse createWebhookRuleResponse(WebhookRuleVO webhookRuleVO) {
        WebhookRuleResponse response = new WebhookRuleResponse();
        response.setObjectName("webhook");
        response.setId(webhookRuleVO.getUuid());
        response.setName(webhookRuleVO.getName());
        response.setDescription(webhookRuleVO.getDescription());
        Account account = ApiDBUtils.findAccountById(webhookRuleVO.getAccountId());
        if (account.getType() == Account.Type.PROJECT) {
            Project project = ApiDBUtils.findProjectByProjectAccountId(account.getId());
            response.setProjectId(project.getUuid());
            response.setProjectName(project.getName());
        } else {
            response.setAccountName(account.getAccountName());
        }
        Domain domain = ApiDBUtils.findDomainById(webhookRuleVO.getDomainId());
        response.setDomainId(domain.getUuid());
        response.setDomainName(domain.getName());
        response.setState(webhookRuleVO.getState().toString());
        response.setPayloadUrl(webhookRuleVO.getPayloadUrl());
        response.setSecretKey(webhookRuleVO.getSecretKey());
        response.setSslVerification(webhookRuleVO.isSslVerification());
        response.setScope(webhookRuleVO.getScope().toString());
        response.setCreated(webhookRuleVO.getCreated());
        return response;
    }

    /**
     * @param cmd
     * @return Account
     */
    protected Account getOwner(final CreateWebhookRuleCmd cmd) {
        final Account caller = CallContext.current().getCallingAccount();
        return  accountManager.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
    }

    @Override
    public ListResponse<WebhookRuleResponse> listWebhookRules(ListWebhookRulesCmd cmd) {
        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();
        final Long clusterId = cmd.getId();
        final String state = cmd.getState();
        final String name = cmd.getName();
        final String keyword = cmd.getKeyword();
        List<WebhookRuleResponse> responsesList = new ArrayList<>();
        List<Long> permittedAccounts = new ArrayList<>();
        Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, clusterId, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        Project.ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();


        Filter searchFilter = new Filter(WebhookRuleVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<WebhookRuleVO> sb = webhookRuleDao.createSearchBuilder();
        accountManager.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.IN);
        SearchCriteria<WebhookRuleVO> sc = sb.create();
        accountManager.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        if (state != null) {
            sc.setParameters("state", state);
        }
        if(keyword != null){
            sc.setParameters("keyword", "%" + keyword + "%");
        }
        if (clusterId != null) {
            sc.setParameters("id", clusterId);
        }
        if (name != null) {
            sc.setParameters("name", name);
        }
        List<WebhookRuleVO> rules = webhookRuleDao.search(sc, searchFilter);
        for (WebhookRuleVO rule : rules) {
            WebhookRuleResponse response = createWebhookRuleResponse(rule);
            responsesList.add(response);
        }
        ListResponse<WebhookRuleResponse> response = new ListResponse<>();
        response.setResponses(responsesList);
        return response;
    }

    @Override
    public WebhookRuleResponse createWebhookRule(CreateWebhookRuleCmd cmd) throws CloudRuntimeException {
        final Account owner = getOwner(cmd);
        final String name  = cmd.getName();
        final String description = cmd.getDescription();
        final String payloadUrl = cmd.getPayloadUrl();
        final String secretKey = cmd.getSecretKey();
        final boolean sslVerification = cmd.isSslVerification();
        final String scopeStr = cmd.getScope();
        final boolean isAdmin = accountManager.isAdmin(owner.getId());
        final String stateStr = cmd.getState();
        WebhookRule.Scope scope = isAdmin ? WebhookRule.Scope.Global : WebhookRule.Scope.Local;
        if (StringUtils.isNotEmpty(scopeStr)) {
            try {
                scope = WebhookRule.Scope.valueOf(scopeStr);
            } catch (IllegalArgumentException iae) {
                throw new InvalidParameterValueException("Invalid scope specified");
            }
        }
        if ((WebhookRule.Scope.Global.equals(scope) && !Account.Type.ADMIN.equals(owner.getType())) ||
                (WebhookRule.Scope.Domain.equals(scope) &&
                        !List.of(Account.Type.ADMIN, Account.Type.DOMAIN_ADMIN).contains(owner.getType()))) {
            throw new InvalidParameterValueException(String.format("Scope %s can not be specified for owner %s", scope, owner.getName()));
        }
        WebhookRule.State state = WebhookRule.State.Enabled;
        if (StringUtils.isNotEmpty(stateStr)) {
            try {
                state = WebhookRule.State.valueOf(stateStr);
            } catch (IllegalArgumentException iae) {
                throw new InvalidParameterValueException("Invalid state specified");
            }
        }
        UriUtils.validateUrl(payloadUrl);
        URI uri = URI.create(payloadUrl);
        if (sslVerification && !HttpConstants.HTTPS.equalsIgnoreCase(uri.getScheme())) {
            throw new InvalidParameterValueException(String.format("SSL verification can be specified only for HTTPS URLs, %s", payloadUrl));
        }
        WebhookRuleVO rule = new WebhookRuleVO(name, description, state, owner.getDomainId(),
                owner.getId(), payloadUrl, secretKey, sslVerification, scope);
        rule = webhookRuleDao.persist(rule);
        return createWebhookRuleResponse(rule);
    }

    @Override
    public boolean deleteWebhookRule(DeleteWebhookRuleCmd cmd) throws CloudRuntimeException {
        final Account caller = CallContext.current().getCallingAccount();
        final long id = cmd.getId();
        WebhookRule rule = webhookRuleDao.findById(id);
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find the webhook rule with the specified ID");
        }
        accountManager.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, rule);
        return webhookRuleDao.remove(id);
    }

    @Override
    public WebhookRuleResponse updateWebhookRule(UpdateWebhookRuleCmd cmd) throws CloudRuntimeException {
        final Account caller = CallContext.current().getCallingAccount();
        final long id = cmd.getId();
        final String name  = cmd.getName();
        final String description = cmd.getDescription();
        final String payloadUrl = cmd.getPayloadUrl();
        final String secretKey = cmd.getSecretKey();
        final Boolean sslVerification = cmd.isSslVerification();
        final String scopeStr = cmd.getScope();
        final String stateStr = cmd.getState();
        WebhookRuleVO rule = webhookRuleDao.findById(id);
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find the webhook rule with the specified ID");
        }
        accountManager.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, rule);
        boolean updateNeeded = false;
        if (StringUtils.isNotBlank(name)) {
            rule.setName(name);
            updateNeeded = true;
        }
        if (description != null) {
            rule.setDescription(description);
            updateNeeded = true;
        }
        if (StringUtils.isNotEmpty(stateStr)) {
            try {
                WebhookRule.State state = WebhookRule.State.valueOf(stateStr);
                rule.setState(state);
                updateNeeded = true;
            } catch (IllegalArgumentException iae) {
                throw new InvalidParameterValueException("Invalid state specified");
            }
        }
        if (StringUtils.isNotEmpty(scopeStr)) {
            try {
                WebhookRule.Scope scope = WebhookRule.Scope.valueOf(scopeStr);
                Account owner = accountManager.getAccount(rule.getAccountId());
                if ((WebhookRule.Scope.Global.equals(scope) && !Account.Type.ADMIN.equals(owner.getType())) ||
                        (WebhookRule.Scope.Domain.equals(scope) &&
                                !List.of(Account.Type.ADMIN, Account.Type.DOMAIN_ADMIN).contains(owner.getType()))) {
                    throw new InvalidParameterValueException(String.format("Scope %s can not be specified for owner %s", scope, owner.getName()));
                }
                rule.setScope(scope);
                updateNeeded = true;
            } catch (IllegalArgumentException iae) {
                throw new InvalidParameterValueException("Invalid scope specified");
            }
        }
        URI uri = URI.create(rule.getPayloadUrl());
        if (StringUtils.isNotEmpty(payloadUrl)) {
            UriUtils.validateUrl(payloadUrl);
            uri = URI.create(payloadUrl);
            rule.setPayloadUrl(payloadUrl);
            updateNeeded = true;
        }
        if (sslVerification != null) {
            if (Boolean.TRUE.equals(sslVerification) && !HttpConstants.HTTPS.equalsIgnoreCase(uri.getScheme())) {
                throw new InvalidParameterValueException(String.format("SSL verification can be specified only for HTTPS URLs, %s", payloadUrl));
            }
            updateNeeded = true;
        }
        if (secretKey != null) {
            rule.setSecretKey(secretKey);
            updateNeeded = true;
        }
        if (updateNeeded && !webhookRuleDao.update(id, rule)) {
            return null;
        }
        return createWebhookRuleResponse(rule);
    }

    @Override
    public WebhookRuleResponse createWebhookRuleResponse(long webhookRuleId) {
        WebhookRuleVO webhookRuleVO = webhookRuleDao.findById(webhookRuleId);
        return createWebhookRuleResponse(webhookRuleVO);
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(CreateWebhookRuleCmd.class);
        cmdList.add(ListWebhookRulesCmd.class);
        cmdList.add(UpdateWebhookRuleCmd.class);
        cmdList.add(DeleteWebhookRuleCmd.class);
        return cmdList;
    }
}
