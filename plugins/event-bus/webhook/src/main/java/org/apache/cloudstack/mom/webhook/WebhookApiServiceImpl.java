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
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mom.webhook.api.command.user.CreateWebhookCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.DeleteWebhookCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.DeleteWebhookDeliveryCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.ExecuteWebhookDeliveryCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.ListWebhookDeliveriesCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.ListWebhooksCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.UpdateWebhookCmd;
import org.apache.cloudstack.mom.webhook.api.response.WebhookDeliveryResponse;
import org.apache.cloudstack.mom.webhook.api.response.WebhookResponse;
import org.apache.cloudstack.mom.webhook.dao.WebhookDao;
import org.apache.cloudstack.mom.webhook.dao.WebhookDeliveryDao;
import org.apache.cloudstack.mom.webhook.dao.WebhookDeliveryJoinDao;
import org.apache.cloudstack.mom.webhook.dao.WebhookJoinDao;
import org.apache.cloudstack.mom.webhook.vo.WebhookDeliveryJoinVO;
import org.apache.cloudstack.mom.webhook.vo.WebhookDeliveryVO;
import org.apache.cloudstack.mom.webhook.vo.WebhookJoinVO;
import org.apache.cloudstack.mom.webhook.vo.WebhookVO;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.api.ApiResponseHelper;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.Project;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.UriUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.rest.HttpConstants;

public class WebhookApiServiceImpl extends ManagerBase implements WebhookApiService {

    @Inject
    AccountManager accountManager;
    @Inject
    DomainDao domainDao;
    @Inject
    WebhookDao webhookDao;
    @Inject
    WebhookJoinDao webhookJoinDao;
    @Inject
    WebhookDeliveryDao webhookDeliveryDao;
    @Inject
    WebhookDeliveryJoinDao webhookDeliveryJoinDao;
    @Inject
    ManagementServerHostDao managementServerHostDao;
    @Inject
    WebhookService webhookService;

    protected WebhookResponse createWebhookResponse(WebhookJoinVO webhookVO) {
        WebhookResponse response = new WebhookResponse();
        response.setObjectName("webhook");
        response.setId(webhookVO.getUuid());
        response.setName(webhookVO.getName());
        response.setDescription(webhookVO.getDescription());
        ApiResponseHelper.populateOwner(response, webhookVO);
        response.setState(webhookVO.getState().toString());
        response.setPayloadUrl(webhookVO.getPayloadUrl());
        response.setSecretKey(webhookVO.getSecretKey());
        response.setSslVerification(webhookVO.isSslVerification());
        response.setScope(webhookVO.getScope().toString());
        response.setCreated(webhookVO.getCreated());
        return response;
    }

    protected List<Long> getIdsOfAccessibleWebhooks(Account caller) {
        if (Account.Type.ADMIN.equals(caller.getType())) {
            return new ArrayList<>();
        }
        String domainPath = null;
        if (Account.Type.DOMAIN_ADMIN.equals(caller.getType())) {
            Domain domain = domainDao.findById(caller.getDomainId());
            domainPath = domain.getPath();
        }
        List<WebhookJoinVO> webhooks = webhookJoinDao.listByAccountOrDomain(caller.getId(), domainPath);
        return webhooks.stream().map(WebhookJoinVO::getId).collect(Collectors.toList());
    }

    protected ManagementServerHostVO basicWebhookDeliveryApiCheck(Account caller, final Long id, final Long webhookId,
                final Long managementServerId, final Date startDate, final Date endDate) {
        if (id != null) {
            WebhookDeliveryVO webhookDeliveryVO = webhookDeliveryDao.findById(id);
            if (webhookDeliveryVO == null) {
                throw new InvalidParameterValueException("Invalid ID specified");
            }
            WebhookVO webhookVO = webhookDao.findById(webhookDeliveryVO.getWebhookId());
            if (webhookVO != null) {
                accountManager.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, webhookVO);
            }
        }
        if (webhookId != null) {
            WebhookVO webhookVO = webhookDao.findById(webhookId);
            if (webhookVO == null) {
                throw new InvalidParameterValueException("Invalid Webhook specified");
            }
            accountManager.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, webhookVO);
        }
        if (endDate != null && startDate != null && endDate.before(startDate)) {
            throw new InvalidParameterValueException(String.format("Invalid %s specified", ApiConstants.END_DATE));
        }
        ManagementServerHostVO managementServerHostVO = null;
        if (managementServerId != null) {
            if (!Account.Type.ADMIN.equals(caller.getType())) {
                throw new PermissionDeniedException("Invalid parameter specified");
            }
            managementServerHostVO = managementServerHostDao.findById(managementServerId);
            if (managementServerHostVO == null) {
                throw new InvalidParameterValueException("Invalid management server specified");
            }
        }
        return managementServerHostVO;
    }

    protected WebhookDeliveryResponse createWebhookDeliveryResponse(WebhookDeliveryJoinVO webhookDeliveryVO) {
        WebhookDeliveryResponse response = new WebhookDeliveryResponse();
        response.setObjectName(WebhookDelivery.class.getSimpleName().toLowerCase());
        response.setId(webhookDeliveryVO.getUuid());
        response.setEventId(webhookDeliveryVO.getEventUuid());
        response.setEventType(webhookDeliveryVO.getEventType());
        response.setWebhookId(webhookDeliveryVO.getWebhookUuId());
        response.setWebhookName(webhookDeliveryVO.getWebhookName());
        response.setManagementServerId(webhookDeliveryVO.getManagementServerUuId());
        response.setManagementServerName(webhookDeliveryVO.getManagementServerName());
        response.setHeaders(webhookDeliveryVO.getHeaders());
        response.setPayload(webhookDeliveryVO.getPayload());
        response.setSuccess(webhookDeliveryVO.isSuccess());
        response.setResponse(webhookDeliveryVO.getResponse());
        response.setStartTime(webhookDeliveryVO.getStartTime());
        response.setEndTime(webhookDeliveryVO.getEndTime());
        return response;
    }

    protected WebhookDeliveryResponse createTestWebhookDeliveryResponse(WebhookDelivery webhookDelivery,
            Webhook webhook) {
        WebhookDeliveryResponse response = new WebhookDeliveryResponse();
        response.setObjectName(WebhookDelivery.class.getSimpleName().toLowerCase());
        response.setId(webhookDelivery.getUuid());
        response.setEventType(WebhookDelivery.TEST_EVENT_TYPE);
        if (webhook != null) {
            response.setWebhookId(webhook.getUuid());
            response.setWebhookName(webhook.getName());
        }
        ManagementServerHostVO msHost =
                managementServerHostDao.findByMsid(webhookDelivery.getManagementServerId());
        if (msHost != null) {
            response.setManagementServerId(msHost.getUuid());
            response.setManagementServerName(msHost.getName());
        }
        response.setHeaders(webhookDelivery.getHeaders());
        response.setPayload(webhookDelivery.getPayload());
        response.setSuccess(webhookDelivery.isSuccess());
        response.setResponse(webhookDelivery.getResponse());
        response.setStartTime(webhookDelivery.getStartTime());
        response.setEndTime(webhookDelivery.getEndTime());
        return response;
    }

    /**
     * @param cmd
     * @return Account
     */
    protected Account getOwner(final CreateWebhookCmd cmd) {
        final Account caller = CallContext.current().getCallingAccount();
        return  accountManager.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
    }

    protected String getNormalizedPayloadUrl(String payloadUrl) {
        if (StringUtils.isBlank(payloadUrl) || payloadUrl.startsWith("http://") || payloadUrl.startsWith("https://")) {
            return payloadUrl;
        }
        return String.format("http://%s", payloadUrl);
    }

    protected void validateWebhookOwnerPayloadUrl(Account owner, String payloadUrl, Webhook currentWebhook) {
        WebhookVO webhookVO = webhookDao.findByAccountAndPayloadUrl(owner.getId(), payloadUrl);
        if (webhookVO == null) {
            return;
        }
        if (currentWebhook != null && webhookVO.getId() == currentWebhook.getId()) {
            return;
        }
        String error = String.format("Payload URL: %s is already in use by another webhook", payloadUrl);
        logger.error(String.format("%s: %s for Account [%s]", error, webhookVO, owner));
        throw new InvalidParameterValueException(error);
    }

    @Override
    public ListResponse<WebhookResponse> listWebhooks(ListWebhooksCmd cmd) {
        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();
        final Long clusterId = cmd.getId();
        final String stateStr = cmd.getState();
        final String name = cmd.getName();
        final String keyword = cmd.getKeyword();
        final String scopeStr = cmd.getScope();
        List<WebhookResponse> responsesList = new ArrayList<>();
        List<Long> permittedAccounts = new ArrayList<>();
        Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject =
                new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, clusterId, cmd.getAccountName(), cmd.getProjectId(),
                permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        Project.ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();


        Filter searchFilter = new Filter(WebhookJoinVO.class, "id", true, cmd.getStartIndex(),
                cmd.getPageSizeVal());
        SearchBuilder<WebhookJoinVO> sb = webhookJoinDao.createSearchBuilder();
        accountManager.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("scope", sb.entity().getScope(), SearchCriteria.Op.EQ);
        SearchCriteria<WebhookJoinVO> sc = sb.create();
        accountManager.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);
        Webhook.Scope scope = null;
        if (StringUtils.isNotEmpty(scopeStr)) {
            try {
                scope = Webhook.Scope.valueOf(scopeStr);
            } catch (IllegalArgumentException iae) {
                throw new InvalidParameterValueException("Invalid scope specified");
            }
        }
        if ((Webhook.Scope.Global.equals(scope) && !Account.Type.ADMIN.equals(caller.getType())) ||
                (Webhook.Scope.Domain.equals(scope) &&
                        !List.of(Account.Type.ADMIN, Account.Type.DOMAIN_ADMIN).contains(caller.getType()))) {
            throw new InvalidParameterValueException(String.format("Scope %s can not be specified", scope));
        }
        Webhook.State state = null;
        if (StringUtils.isNotEmpty(stateStr)) {
            try {
                state = Webhook.State.valueOf(stateStr);
            } catch (IllegalArgumentException iae) {
                throw new InvalidParameterValueException("Invalid state specified");
            }
        }
        if (scope != null) {
            sc.setParameters("scope", scope.name());
        }
        if (state != null) {
            sc.setParameters("state", state.name());
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
        Pair<List<WebhookJoinVO>, Integer> webhooksAndCount = webhookJoinDao.searchAndCount(sc, searchFilter);
        for (WebhookJoinVO webhook : webhooksAndCount.first()) {
            WebhookResponse response = createWebhookResponse(webhook);
            responsesList.add(response);
        }
        ListResponse<WebhookResponse> response = new ListResponse<>();
        response.setResponses(responsesList, webhooksAndCount.second());
        return response;
    }

    @Override
    public WebhookResponse createWebhook(CreateWebhookCmd cmd) throws CloudRuntimeException {
        final Account owner = getOwner(cmd);
        final String name  = cmd.getName();
        final String description = cmd.getDescription();
        final String payloadUrl = getNormalizedPayloadUrl(cmd.getPayloadUrl());
        final String secretKey = cmd.getSecretKey();
        final boolean sslVerification = cmd.isSslVerification();
        final String scopeStr = cmd.getScope();
        final String stateStr = cmd.getState();
        Webhook.Scope scope = Webhook.Scope.Local;
        if (StringUtils.isNotEmpty(scopeStr)) {
            try {
                scope = Webhook.Scope.valueOf(scopeStr);
            } catch (IllegalArgumentException iae) {
                throw new InvalidParameterValueException("Invalid scope specified");
            }
        }
        if ((Webhook.Scope.Global.equals(scope) && !Account.Type.ADMIN.equals(owner.getType())) ||
                (Webhook.Scope.Domain.equals(scope) &&
                        !List.of(Account.Type.ADMIN, Account.Type.DOMAIN_ADMIN).contains(owner.getType()))) {
            throw new InvalidParameterValueException(
                    String.format("Scope %s can not be specified for owner %s", scope, owner.getName()));
        }
        Webhook.State state = Webhook.State.Enabled;
        if (StringUtils.isNotEmpty(stateStr)) {
            try {
                state = Webhook.State.valueOf(stateStr);
            } catch (IllegalArgumentException iae) {
                throw new InvalidParameterValueException("Invalid state specified");
            }
        }
        UriUtils.validateUrl(payloadUrl);
        validateWebhookOwnerPayloadUrl(owner, payloadUrl, null);
        URI uri = URI.create(payloadUrl);
        if (sslVerification && !HttpConstants.HTTPS.equalsIgnoreCase(uri.getScheme())) {
            throw new InvalidParameterValueException(
                    String.format("SSL verification can be specified only for HTTPS URLs, %s", payloadUrl));
        }
        long domainId = owner.getDomainId();
        Long cmdDomainId = cmd.getDomainId();
        if (cmdDomainId != null &&
                List.of(Account.Type.ADMIN, Account.Type.DOMAIN_ADMIN).contains(owner.getType()) &&
                Webhook.Scope.Domain.equals(scope)) {
            domainId = cmdDomainId;
        }
        WebhookVO webhook = new WebhookVO(name, description, state, domainId, owner.getId(), payloadUrl, secretKey,
                sslVerification, scope);
        webhook = webhookDao.persist(webhook);
        return createWebhookResponse(webhook.getId());
    }

    @Override
    public boolean deleteWebhook(DeleteWebhookCmd cmd) throws CloudRuntimeException {
        final Account caller = CallContext.current().getCallingAccount();
        final long id = cmd.getId();
        Webhook webhook = webhookDao.findById(id);
        if (webhook == null) {
            throw new InvalidParameterValueException("Unable to find the webhook with the specified ID");
        }
        accountManager.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, webhook);
        return webhookDao.remove(id);
    }

    @Override
    public WebhookResponse updateWebhook(UpdateWebhookCmd cmd) throws CloudRuntimeException {
        final Account caller = CallContext.current().getCallingAccount();
        final long id = cmd.getId();
        final String name  = cmd.getName();
        final String description = cmd.getDescription();
        final String payloadUrl = getNormalizedPayloadUrl(cmd.getPayloadUrl());
        String secretKey = cmd.getSecretKey();
        final Boolean sslVerification = cmd.isSslVerification();
        final String scopeStr = cmd.getScope();
        final String stateStr = cmd.getState();
        WebhookVO webhook = webhookDao.findById(id);
        if (webhook == null) {
            throw new InvalidParameterValueException("Unable to find the webhook with the specified ID");
        }
        accountManager.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, webhook);
        boolean updateNeeded = false;
        if (StringUtils.isNotBlank(name)) {
            webhook.setName(name);
            updateNeeded = true;
        }
        if (description != null) {
            webhook.setDescription(description);
            updateNeeded = true;
        }
        if (StringUtils.isNotEmpty(stateStr)) {
            try {
                Webhook.State state = Webhook.State.valueOf(stateStr);
                webhook.setState(state);
                updateNeeded = true;
            } catch (IllegalArgumentException iae) {
                throw new InvalidParameterValueException("Invalid state specified");
            }
        }
        Account owner = accountManager.getAccount(webhook.getAccountId());
        if (StringUtils.isNotEmpty(scopeStr)) {
            try {
                Webhook.Scope scope = Webhook.Scope.valueOf(scopeStr);
                if ((Webhook.Scope.Global.equals(scope) && !Account.Type.ADMIN.equals(owner.getType())) ||
                        (Webhook.Scope.Domain.equals(scope) &&
                                !List.of(Account.Type.ADMIN, Account.Type.DOMAIN_ADMIN).contains(owner.getType()))) {
                    throw new InvalidParameterValueException(
                            String.format("Scope %s can not be specified for owner %s", scope, owner.getName()));
                }
                webhook.setScope(scope);
                updateNeeded = true;
            } catch (IllegalArgumentException iae) {
                throw new InvalidParameterValueException("Invalid scope specified");
            }
        }
        URI uri = URI.create(webhook.getPayloadUrl());
        if (StringUtils.isNotEmpty(payloadUrl)) {
            UriUtils.validateUrl(payloadUrl);
            validateWebhookOwnerPayloadUrl(owner, payloadUrl, webhook);
            uri = URI.create(payloadUrl);
            webhook.setPayloadUrl(payloadUrl);
            updateNeeded = true;
        }
        if (sslVerification != null) {
            if (Boolean.TRUE.equals(sslVerification) && !HttpConstants.HTTPS.equalsIgnoreCase(uri.getScheme())) {
                throw new InvalidParameterValueException(
                        String.format("SSL verification can be specified only for HTTPS URLs, %s", payloadUrl));
            }
            webhook.setSslVerification(sslVerification);
            updateNeeded = true;
        }
        if (secretKey != null) {
            if (StringUtils.isBlank(secretKey)) {
                secretKey = null;
            }
            webhook.setSecretKey(secretKey);
            updateNeeded = true;
        }
        if (updateNeeded && !webhookDao.update(id, webhook)) {
            return null;
        }
        return createWebhookResponse(webhook.getId());
    }

    @Override
    public WebhookResponse createWebhookResponse(long webhookId) {
        WebhookJoinVO webhookVO = webhookJoinDao.findById(webhookId);
        return createWebhookResponse(webhookVO);
    }

    @Override
    public ListResponse<WebhookDeliveryResponse> listWebhookDeliveries(ListWebhookDeliveriesCmd cmd) {
        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();
        final Long id = cmd.getId();
        final Long webhookId = cmd.getWebhookId();
        final Long managementServerId = cmd.getManagementServerId();
        final String keyword = cmd.getKeyword();
        final Date startDate = cmd.getStartDate();
        final Date endDate = cmd.getEndDate();
        final String eventType = cmd.getEventType();
        List<WebhookDeliveryResponse> responsesList = new ArrayList<>();
        ManagementServerHostVO host = basicWebhookDeliveryApiCheck(caller, id, webhookId, managementServerId,
                startDate, endDate);

        Filter searchFilter = new Filter(WebhookDeliveryJoinVO.class, "id", false, cmd.getStartIndex(),
                cmd.getPageSizeVal());
        List<Long> webhookIds = new ArrayList<>();
        if (webhookId != null) {
            webhookIds.add(webhookId);
        } else {
            webhookIds.addAll(getIdsOfAccessibleWebhooks(caller));
        }
        Pair<List<WebhookDeliveryJoinVO>, Integer> deliveriesAndCount =
                webhookDeliveryJoinDao.searchAndCountByListApiParameters(id, webhookIds,
                        (host != null ? host.getMsid() : null), keyword, startDate, endDate, eventType, searchFilter);
        for (WebhookDeliveryJoinVO delivery : deliveriesAndCount.first()) {
            WebhookDeliveryResponse response = createWebhookDeliveryResponse(delivery);
            responsesList.add(response);
        }
        ListResponse<WebhookDeliveryResponse> response = new ListResponse<>();
        response.setResponses(responsesList, deliveriesAndCount.second());
        return response;
    }

    @Override
    public int deleteWebhookDelivery(DeleteWebhookDeliveryCmd cmd) throws CloudRuntimeException {
        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();
        final Long id = cmd.getId();
        final Long webhookId = cmd.getWebhookId();
        final Long managementServerId = cmd.getManagementServerId();
        final Date startDate = cmd.getStartDate();
        final Date endDate = cmd.getEndDate();
        ManagementServerHostVO host = basicWebhookDeliveryApiCheck(caller, id, webhookId, managementServerId,
                startDate, endDate);
        int removed = webhookDeliveryDao.deleteByDeleteApiParams(id, webhookId,
                (host != null ? host.getMsid() : null), startDate, endDate);
        logger.info("{} webhook deliveries removed", removed);
        return removed;
    }

    @Override
    public WebhookDeliveryResponse executeWebhookDelivery(ExecuteWebhookDeliveryCmd cmd) throws CloudRuntimeException {
        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();
        final Long deliveryId = cmd.getId();
        final Long webhookId = cmd.getWebhookId();
        final String payloadUrl = getNormalizedPayloadUrl(cmd.getPayloadUrl());
        final String secretKey = cmd.getSecretKey();
        final Boolean sslVerification = cmd.isSslVerification();
        final String payload = cmd.getPayload();
        final Account owner = accountManager.finalizeOwner(caller, null, null, null);

        if (ObjectUtils.allNull(deliveryId, webhookId) && StringUtils.isBlank(payloadUrl)) {
            throw new InvalidParameterValueException(String.format("One of the %s, %s or %s must be specified",
                    ApiConstants.ID, ApiConstants.WEBHOOK_ID, ApiConstants.PAYLOAD_URL));
        }
        WebhookDeliveryVO existingDelivery = null;
        WebhookVO webhook = null;
        if (deliveryId != null) {
            existingDelivery = webhookDeliveryDao.findById(deliveryId);
            if (existingDelivery == null) {
                throw new InvalidParameterValueException("Invalid webhook delivery specified");
            }
            webhook = webhookDao.findById(existingDelivery.getWebhookId());
        }
        if (StringUtils.isNotBlank(payloadUrl)) {
            UriUtils.validateUrl(payloadUrl);
        }
        if (webhookId != null) {
            webhook = webhookDao.findById(webhookId);
            if (webhook == null) {
                throw new InvalidParameterValueException("Invalid webhook specified");
            }
            if (StringUtils.isNotBlank(payloadUrl)) {
                webhook.setPayloadUrl(payloadUrl);
            }
            if (StringUtils.isNotBlank(secretKey)) {
                webhook.setSecretKey(secretKey);
            }
            if (sslVerification != null) {
                webhook.setSslVerification(Boolean.TRUE.equals(sslVerification));
            }
        }
        if (ObjectUtils.allNull(deliveryId, webhookId)) {
            webhook = new WebhookVO(owner.getDomainId(), owner.getId(), payloadUrl, secretKey,
                    Boolean.TRUE.equals(sslVerification));
        }
        WebhookDelivery webhookDelivery = webhookService.executeWebhookDelivery(existingDelivery, webhook, payload);
        if (webhookDelivery.getId() != WebhookDelivery.ID_DUMMY) {
            return createWebhookDeliveryResponse(webhookDeliveryJoinDao.findById(webhookDelivery.getId()));
        }
        return createTestWebhookDeliveryResponse(webhookDelivery, webhook);
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(CreateWebhookCmd.class);
        cmdList.add(ListWebhooksCmd.class);
        cmdList.add(UpdateWebhookCmd.class);
        cmdList.add(DeleteWebhookCmd.class);
        cmdList.add(ListWebhookDeliveriesCmd.class);
        cmdList.add(DeleteWebhookDeliveryCmd.class);
        cmdList.add(ExecuteWebhookDeliveryCmd.class);
        return cmdList;
    }
}
