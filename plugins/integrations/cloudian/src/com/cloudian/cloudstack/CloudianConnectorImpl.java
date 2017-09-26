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

package com.cloudian.cloudstack;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.log4j.Logger;

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
import com.cloudian.client.CloudianClient;
import com.cloudian.client.CloudianGroup;
import com.cloudian.client.CloudianUser;
import com.cloudian.cloudstack.api.CloudianIsEnabledCmd;
import com.cloudian.cloudstack.api.CloudianSsoLoginCmd;

public class CloudianConnectorImpl extends ComponentLifecycleBase implements CloudianConnector, Configurable {
    private static final Logger LOG = Logger.getLogger(CloudianConnectorImpl.class);

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
            return new CloudianClient(getAdminUrl(),
                    CloudianAdminUser.value(), CloudianAdminPassword.value(),
                    CloudianValidateSSLSecurity.value());
        } catch (final KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            LOG.error("Failed to create Cloudian client due to: ", e);
        }
        throw new CloudRuntimeException("Failed to create and return Cloudian client instance");
    }

    private boolean addOrUpdateGroup(final Domain domain) {
        if (domain == null || isConnectorDisabled()) {
            return false;
        }
        final CloudianClient client = getClient();
        final CloudianGroup existingGroup = client.listGroup(domain.getUuid());
        if (existingGroup != null) {
            if (!existingGroup.getActive() || !existingGroup.getGroupName().equals(domain.getPath())) {
                LOG.debug("Updating Cloudian group for domain uuid=" + domain.getUuid() + " name=" + domain.getName() + " path=" + domain.getPath());
                existingGroup.setActive(true);
                existingGroup.setGroupName(domain.getPath());
                return client.updateGroup(existingGroup);
            }
            return true;
        }

        LOG.debug("Adding Cloudian group for domain uuid=" + domain.getUuid() + " name=" + domain.getName() + " path=" + domain.getPath());
        final CloudianGroup group = new CloudianGroup();
        group.setGroupId(domain.getUuid());
        group.setGroupName(domain.getPath());
        group.setActive(true);
        return client.addGroup(group);
    }

    private boolean removeGroup(final Domain domain) {
        if (domain == null || isConnectorDisabled()) {
            return false;
        }
        final CloudianClient client = getClient();
        LOG.debug("Removing Cloudian group for domain uuid=" + domain.getUuid() + " name=" + domain.getName() + " path=" + domain.getPath());
        for (final CloudianUser user: client.listUsers(domain.getUuid())) {
            if (client.removeUser(user.getUserId(), domain.getUuid())) {
                LOG.error(String.format("Failed to remove Cloudian user id=%s, while removing Cloudian group id=%s", user.getUserId(), domain.getUuid()));
            }
        }
        return client.removeGroup(domain.getUuid());
    }

    private boolean addOrUpdateUserAccount(final Account account, final Domain domain) {
        if (account == null || domain == null || isConnectorDisabled()) {
            return false;
        }
        final User accountUser = userDao.listByAccount(account.getId()).get(0);
        final String fullName = String.format("%s %s (%s)", accountUser.getFirstname(), accountUser.getLastname(), account.getAccountName());
        final CloudianClient client = getClient();
        final CloudianUser existingUser = client.listUser(account.getUuid(), domain.getUuid());
        if (existingUser != null) {
            if (!existingUser.getActive() || !existingUser.getFullName().equals(fullName)) {
                LOG.debug("Updating Cloudian user for account with uuid=" + account.getUuid() + " name=" + account.getAccountName());
                existingUser.setActive(true);
                existingUser.setEmailAddr(accountUser.getEmail());
                existingUser.setFullName(fullName);
                return client.updateUser(existingUser);
            }
            return true;
        }

        LOG.debug("Adding Cloudian user for account with uuid=" + account.getUuid() + " name=" + account.getAccountName());
        final CloudianUser user = new CloudianUser();
        user.setUserId(account.getUuid());
        user.setGroupId(domain.getUuid());
        user.setFullName(fullName);
        user.setEmailAddr(accountUser.getEmail());
        user.setUserType(CloudianUser.USER);
        user.setActive(true);
        return client.addUser(user);
    }

    private boolean removeUserAccount(final Account account) {
        if (account == null || isConnectorDisabled()) {
            return false;
        }
        final CloudianClient client = getClient();
        final Domain domain = domainDao.findById(account.getDomainId());
        LOG.debug("Removing Cloudian user for account with uuid=" + account.getUuid() + " name=" + account.getAccountName());
        return client.removeUser(account.getUuid(), domain.getUuid());
    }

    //////////////////////////////////////////////////
    //////////////// Plugin APIs /////////////////////
    //////////////////////////////////////////////////

    @Override
    public String getAdminUrl() {
        return String.format("%s://%s:%s", CloudianAdminProtocol.value(),
                CloudianAdminHost.value(), CloudianAdminPort.value());
    }

    @Override
    public String getCmcUrl() {
        return String.format("%s://%s:%s/Cloudian/", CloudianCmcProtocol.value(),
                CloudianCmcHost.value(), CloudianCmcPort.value());
    }

    @Override
    public boolean isConnectorDisabled() {
        return !CloudianConnectorEnabled.value();
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
        } else {
            addOrUpdateGroup(domain);
            addOrUpdateUserAccount(caller, domain);
        }

        return CloudianUtils.generateSSOUrl(getCmcUrl(), user, group, CloudianSsoKey.value());
    }

    ///////////////////////////////////////////////////////////
    //////////////// Plugin Configuration /////////////////////
    ///////////////////////////////////////////////////////////

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        if (isConnectorDisabled()) {
            return true;
        }

        messageBus.subscribe(AccountManager.MESSAGE_ADD_ACCOUNT_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final Map<Long, Long> accountGroupMap = (Map<Long, Long>) args;
                    final Long accountId = accountGroupMap.keySet().iterator().next();
                    final Account account = accountDao.findById(accountId);
                    final Domain domain = domainDao.findById(account.getDomainId());

                    if (!addOrUpdateUserAccount(account, domain)) {
                        LOG.warn(String.format("Failed to add account in Cloudian while adding CloudStack account=%s in domain=%s", account.getAccountName(), domain.getPath()));
                    }
                } catch (final Exception e) {
                    LOG.error("Caught exception while adding account in Cloudian: ", e);
                }
            }
        });

        messageBus.subscribe(AccountManager.MESSAGE_REMOVE_ACCOUNT_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final Account account = accountDao.findByIdIncludingRemoved((Long) args);
                    if(!removeUserAccount(account))    {
                        LOG.warn(String.format("Failed to remove account to Cloudian while removing CloudStack account=%s, id=%s", account.getAccountName(), account.getId()));
                    }
                } catch (final Exception e) {
                    LOG.error("Caught exception while removing account in Cloudian: ", e);
                }
            }
        });

        messageBus.subscribe(DomainManager.MESSAGE_ADD_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final Domain domain = domainDao.findById((Long) args);
                    if (!addOrUpdateGroup(domain)) {
                        LOG.warn(String.format("Failed to add group in Cloudian while adding CloudStack domain=%s id=%s", domain.getPath(), domain.getId()));
                    }
                } catch (final Exception e) {
                    LOG.error("Caught exception adding domain/group in Cloudian: ", e);
                }
            }
        });

        messageBus.subscribe(DomainManager.MESSAGE_REMOVE_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final DomainVO domain = (DomainVO) args;
                    if (!removeGroup(domain)) {
                        LOG.warn(String.format("Failed to remove group in Cloudian while removing CloudStack domain=%s id=%s", domain.getPath(), domain.getId()));
                    }
                } catch (final Exception e) {
                    LOG.error("Caught exception while removing domain/group in Cloudian: ", e);
                }
            }
        });

        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CloudianIsEnabledCmd.class);
        if (isConnectorDisabled()) {
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
                CloudianValidateSSLSecurity,
                CloudianCmcAdminUser,
                CloudianCmcHost,
                CloudianCmcPort,
                CloudianCmcProtocol,
                CloudianSsoKey
        };
    }
}
