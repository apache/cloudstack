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

package org.apache.cloudstack.cloudian;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.cloudian.api.CloudianIsEnabledCmd;
import org.apache.cloudstack.cloudian.api.CloudianSsoLoginCmd;
import org.apache.cloudstack.cloudian.client.CloudianClient;
import org.apache.cloudstack.cloudian.client.CloudianGroup;
import org.apache.cloudstack.cloudian.client.CloudianUser;
import org.apache.cloudstack.cloudian.client.CloudianUtils;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.exception.CloudRuntimeException;

public class CloudianConnectorImpl extends ComponentLifecycleBase implements CloudianConnector, Configurable {

    @Inject
    private UserDao userDao;

    @Inject
    private AccountDao accountDao;

    @Inject
    private DomainDao domainDao;

    @Inject
    private MessageBus messageBus;

    /////////////////////////////////////////////////////
    //////////////// Plugin Methods /////////////////////
    /////////////////////////////////////////////////////

    private CloudianClient getClient() {
        try {
            return new CloudianClient(CloudianAdminHost.value(), CloudianAdminPort.value(), CloudianAdminProtocol.value(),
                    CloudianAdminUser.value(), CloudianAdminPassword.value(),
                    CloudianValidateSSLSecurity.value(), CloudianAdminApiRequestTimeout.value());
        } catch (final KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Failed to create Cloudian API client due to: ", e);
        }
        throw new CloudRuntimeException("Failed to create and return Cloudian API client instance");
    }

    private boolean addGroup(final Domain domain) {
        if (domain == null || !isEnabled()) {
            return false;
        }
        final CloudianClient client = getClient();
        final CloudianGroup group = new CloudianGroup();
        group.setGroupId(domain.getUuid());
        group.setGroupName(domain.getPath());
        group.setActive(true);
        return client.addGroup(group);
    }

    private boolean removeGroup(final Domain domain) {
        if (domain == null || !isEnabled()) {
            return false;
        }
        final CloudianClient client = getClient();
        for (final CloudianUser user: client.listUsers(domain.getUuid())) {
            if (client.removeUser(user.getUserId(), domain.getUuid())) {
                logger.error(String.format("Failed to remove Cloudian user id=%s, while removing Cloudian group id=%s", user.getUserId(), domain.getUuid()));
            }
        }
        for (int retry = 0; retry < 3; retry++) {
            if (client.removeGroup(domain.getUuid())) {
                return true;
            } else {
                logger.warn("Failed to remove Cloudian group id=" + domain.getUuid() + ", retrying count=" + retry+1);
            }
        }
        logger.warn("Failed to remove Cloudian group id=" + domain.getUuid() + ", please remove manually");
        return false;
    }

    private boolean addUserAccount(final Account account, final Domain domain) {
        if (account == null || domain == null || !isEnabled()) {
            return false;
        }
        final User accountUser = userDao.listByAccount(account.getId()).get(0);
        final CloudianClient client = getClient();
        final String fullName = String.format("%s %s (%s)", accountUser.getFirstname(), accountUser.getLastname(), account.getAccountName());
        final CloudianUser user = new CloudianUser();
        user.setUserId(account.getUuid());
        user.setGroupId(domain.getUuid());
        user.setFullName(fullName);
        user.setEmailAddr(accountUser.getEmail());
        user.setUserType(CloudianUser.USER);
        user.setActive(true);
        return client.addUser(user);
    }

    private boolean updateUserAccount(final Account account, final Domain domain, final CloudianUser existingUser) {
        if (account == null || domain == null || !isEnabled()) {
            return false;
        }
        final CloudianClient client = getClient();
        if (existingUser != null) {
            final User accountUser = userDao.listByAccount(account.getId()).get(0);
            final String fullName = String.format("%s %s (%s)", accountUser.getFirstname(), accountUser.getLastname(), account.getAccountName());
            if (!existingUser.getActive() || !existingUser.getFullName().equals(fullName) || !existingUser.getEmailAddr().equals(accountUser.getEmail())) {
                existingUser.setActive(true);
                existingUser.setFullName(fullName);
                existingUser.setEmailAddr(accountUser.getEmail());
                return client.updateUser(existingUser);
            }
            return true;
        }
        return false;
    }

    private boolean removeUserAccount(final Account account) {
        if (account == null || !isEnabled()) {
            return false;
        }
        final CloudianClient client = getClient();
        final Domain domain = domainDao.findById(account.getDomainId());
        for (int retry = 0; retry < 3; retry++) {
            if (client.removeUser(account.getUuid(), domain.getUuid())) {
                return true;
            } else {
                logger.warn("Failed to remove Cloudian user id=" + account.getUuid() + " in group id=" + domain.getUuid() + ", retrying count=" + retry+1);
            }
        }
        logger.warn("Failed to remove Cloudian user id=" + account.getUuid() + " in group id=" + domain.getUuid() + ", please remove manually");
        return false;
    }

    //////////////////////////////////////////////////
    //////////////// Plugin APIs /////////////////////
    //////////////////////////////////////////////////

    @Override
    public String getCmcUrl() {
        return String.format("%s://%s:%s/Cloudian/", CloudianCmcProtocol.value(),
                CloudianCmcHost.value(), CloudianCmcPort.value());
    }

    @Override
    public boolean isEnabled() {
        return CloudianConnectorEnabled.value();
    }

    @Override
    public String generateSsoUrl() {
        final Account caller = CallContext.current().getCallingAccount();
        final Domain domain = domainDao.findById(caller.getDomainId());

        String user = caller.getUuid();
        String group = domain.getUuid();

        if (caller.getAccountName().equals("admin") && caller.getRoleId() == RoleType.Admin.getId()) {
            user = CloudianCmcAdminUser.value();
            group = "0";
        }

        logger.debug(String.format("Attempting Cloudian SSO with user id=%s, group id=%s", user, group));

        final CloudianUser ssoUser = getClient().listUser(user, group);
        if (ssoUser == null || !ssoUser.getActive()) {
            logger.debug(String.format("Failed to find existing Cloudian user id=%s in group id=%s", user, group));
            final CloudianGroup ssoGroup = getClient().listGroup(group);
            if (ssoGroup == null) {
                logger.debug(String.format("Failed to find existing Cloudian group id=%s, trying to add it", group));
                if (!addGroup(domain)) {
                    logger.error("Failed to add missing Cloudian group id=" + group);
                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Aborting Cloudian SSO, failed to add group to Cloudian.");
                }
            }
            if (!addUserAccount(caller, domain)) {
                logger.error("Failed to add missing Cloudian group id=" + group);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Aborting Cloudian SSO, failed to add user to Cloudian.");
            }
            final CloudianUser addedSsoUser = getClient().listUser(user, group);
            if (addedSsoUser == null || !addedSsoUser.getActive()) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Aborting Cloudian SSO, failed to find mapped Cloudian user, please fix integration issues.");
            }
        } else if (!group.equals("0")) {
            updateUserAccount(caller, domain, ssoUser);
        }

        logger.debug(String.format("Validated Cloudian SSO for Cloudian user id=%s, group id=%s", user, group));
        return CloudianUtils.generateSSOUrl(getCmcUrl(), user, group, CloudianSsoKey.value());
    }

    ///////////////////////////////////////////////////////////
    //////////////// Plugin Configuration /////////////////////
    ///////////////////////////////////////////////////////////

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        if (!isEnabled()) {
            logger.debug("Cloudian connector is disabled, skipping configuration");
            return true;
        }

        logger.debug(String.format("Cloudian connector is enabled, completed configuration, integration is ready. " +
                        "Cloudian admin host:%s, port:%s, user:%s",
                CloudianAdminHost.value(), CloudianAdminPort.value(), CloudianAdminUser.value()));

        messageBus.subscribe(AccountManager.MESSAGE_ADD_ACCOUNT_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final Map<Long, Long> accountGroupMap = (Map<Long, Long>) args;
                    final Long accountId = accountGroupMap.keySet().iterator().next();
                    final Account account = accountDao.findById(accountId);
                    final Domain domain = domainDao.findById(account.getDomainId());

                    if (!addUserAccount(account, domain)) {
                        logger.warn(String.format("Failed to add account in Cloudian while adding CloudStack account=%s in domain=%s", account.getAccountName(), domain.getPath()));
                    }
                } catch (final Exception e) {
                    logger.error("Caught exception while adding account in Cloudian: ", e);
                }
            }
        });

        messageBus.subscribe(AccountManager.MESSAGE_REMOVE_ACCOUNT_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final Account account = accountDao.findByIdIncludingRemoved((Long) args);
                    if(!removeUserAccount(account))    {
                        logger.warn(String.format("Failed to remove account to Cloudian while removing CloudStack account=%s, id=%s", account.getAccountName(), account.getId()));
                    }
                } catch (final Exception e) {
                    logger.error("Caught exception while removing account in Cloudian: ", e);
                }
            }
        });

        messageBus.subscribe(DomainManager.MESSAGE_ADD_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final Domain domain = domainDao.findById((Long) args);
                    if (!addGroup(domain)) {
                        logger.warn(String.format("Failed to add group in Cloudian while adding CloudStack domain=%s id=%s", domain.getPath(), domain.getId()));
                    }
                } catch (final Exception e) {
                    logger.error("Caught exception adding domain/group in Cloudian: ", e);
                }
            }
        });

        messageBus.subscribe(DomainManager.MESSAGE_REMOVE_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final DomainVO domain = (DomainVO) args;
                    if (!removeGroup(domain)) {
                        logger.warn(String.format("Failed to remove group in Cloudian while removing CloudStack domain=%s id=%s", domain.getPath(), domain.getId()));
                    }
                } catch (final Exception e) {
                    logger.error("Caught exception while removing domain/group in Cloudian: ", e);
                }
            }
        });

        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CloudianIsEnabledCmd.class);
        if (!isEnabled()) {
            return cmdList;
        }
        cmdList.add(CloudianSsoLoginCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return CloudianConnector.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                CloudianConnectorEnabled,
                CloudianAdminHost,
                CloudianAdminPort,
                CloudianAdminUser,
                CloudianAdminPassword,
                CloudianAdminProtocol,
                CloudianAdminApiRequestTimeout,
                CloudianValidateSSLSecurity,
                CloudianCmcAdminUser,
                CloudianCmcHost,
                CloudianCmcPort,
                CloudianCmcProtocol,
                CloudianSsoKey
        };
    }
}
