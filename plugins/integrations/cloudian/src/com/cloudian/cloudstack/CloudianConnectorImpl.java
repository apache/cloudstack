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
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloudian.client.GroupInfo;
import com.cloudian.client.UserInfo;
import com.cloudian.cloudstack.api.CloudianIsEnabledCmd;
import com.cloudian.cloudstack.api.CloudianSsoLoginCmd;

public class CloudianConnectorImpl extends ComponentLifecycleBase implements CloudianConnector, Configurable {
    private static final Logger LOG = Logger.getLogger(CloudianConnectorImpl.class);

    private CloudianClient client;

    @Inject
    private AccountDao accountDao;

    @Inject
    private DomainDao domainDao;

    @Inject
    private MessageBus messageBus;

    @Override
    public boolean isConnectorDisabled() {
        return !CloudianConnectorEnabled.value();
    }

    private String getAdminBaseUrl() {
        return String.format("%s://%s:%s", CloudianAdminProtocol.value(),
                CloudianAdminPort.value(), CloudianCmcPort.value());
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
            if (client.listGroup(group) == null) {
                addGroup(domain);
            }

            if (client.listUser(user, group) == null) {
                addUserAccount(caller);
            }
        }

        final String ssoParams = CloudianUtils.generateSSOUrlParams(user, group, CloudianSsoKey.value());
        if (ssoParams == null) {
            return null;
        }

        return String.format("%s://%s:%s/Cloudian/ssosecurelogin.htm?%s", CloudianCmcProtocol.value(),
                CloudianCmcHost.value(), CloudianCmcPort.value(), ssoParams);
    }

    @Override
    public boolean addGroup(final Domain domain) {
        if (domain == null || isConnectorDisabled()) {
            return false;
        }
        LOG.debug("Adding Cloudian group against domain uuid=" + domain.getUuid() + " name=" + domain.getName() + " path=" + domain.getPath());
        GroupInfo group = new GroupInfo();
        group.setActive(true);
        group.setGroupId(domain.getUuid());
        group.setGroupName(domain.getPath());
        GroupInfo createdGroup = client.addGroup(group);
        return createdGroup != null && createdGroup.getGroupId().equals(domain.getUuid());
    }

    @Override
    public boolean removeGroup(final Domain domain) {
        if (domain == null || isConnectorDisabled()) {
            return false;
        }
        LOG.debug("Removing Cloudian group against domain uuid=" + domain.getUuid() + " name=" + domain.getName() + " path=" + domain.getPath());
        for (UserInfo user: client.listUsers(domain.getUuid())) {
            client.removeUser(user.getUserId(), domain.getUuid());
        }
        return client.removeGroup(domain.getUuid());
    }

    @Override
    public boolean addUserAccount(final Account account) {
        if (account == null || isConnectorDisabled()) {
            return false;
        }
        LOG.debug("Adding Cloudian user account with uuid=" + account.getUuid() + " name=" + account.getAccountName());
        final Domain domain = domainDao.findById(account.getDomainId());
        UserInfo user = new UserInfo();
        user.setActive(true);
        user.setUserId(account.getUuid());
        user.setGroupId(domain.getUuid());
        user.setFullName(account.getAccountName());
        UserInfo createdUser = client.addUser(user);
        return createdUser != null && createdUser.getUserId().equals(account.getUuid());
    }

    @Override
    public boolean removeUserAccount(final Account account) {
        if (account == null || isConnectorDisabled()) {
            return false;
        }
        LOG.debug("Removing Cloudian user account with uuid=" + account.getUuid() + " name=" + account.getAccountName());
        final Domain domain = domainDao.findById(account.getDomainId());
        return client.removeUser(account.getUuid(), domain.getUuid());
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        try {
            client = new CloudianClient(getAdminBaseUrl(),
                    CloudianAdminUser.value(), CloudianAdminPassword.value(),
                    CloudianValidateSSLSecurity.value());
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        messageBus.subscribe(AccountManager.MESSAGE_ADD_ACCOUNT_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                final Map<Long, Long> accountGroupMap = (Map<Long, Long>) args;
                final Long accountId = accountGroupMap.keySet().iterator().next();
                final Account account = accountDao.findById(accountId);
                addUserAccount(account);
            }
        });

        messageBus.subscribe(AccountManager.MESSAGE_REMOVE_ACCOUNT_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                final Account account = accountDao.findByIdIncludingRemoved((Long) args);
                removeUserAccount(account);
            }
        });

        messageBus.subscribe(DomainManager.MESSAGE_ADD_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                final Domain domain = domainDao.findById((Long) args);
                addGroup(domain);
            }
        });

        messageBus.subscribe(DomainManager.MESSAGE_REMOVE_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                final DomainVO domain = (DomainVO) args;
                removeGroup(domain);
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
