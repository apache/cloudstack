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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

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

public class CloudianConnectorImpl extends ComponentLifecycleBase implements CloudianConnector, Configurable {
    public static final Logger LOG = Logger.getLogger(CloudianConnectorImpl.class);

    @Inject
    private AccountDao accountDao;

    @Inject
    private DomainDao domainDao;

    @Inject
    private MessageBus messageBus;


    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        messageBus.subscribe(AccountManager.MESSAGE_ADD_ACCOUNT_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                Map<Long, Long> accountGroupMap = (Map<Long, Long>) args;
                Long accountId = accountGroupMap.keySet().iterator().next();
                // TODO: check and create user in CMC
                final Account account = accountDao.findById(accountId);
                LOG.info("Creating account id=" + accountId + " with uuid=" + account.getUuid());
            }
        });

        messageBus.subscribe(AccountManager.MESSAGE_REMOVE_ACCOUNT_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                Long accountId = (Long) args;
                // TODO: remove/disable user in CMC
                final Account account = accountDao.findById(accountId);
                LOG.info("Removing account id=" + accountId + " with uuid=" + account.getUuid());

            }
        });

        messageBus.subscribe(DomainManager.MESSAGE_ADD_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                Long domainId = (Long) args;
                Domain domain = domainDao.findById(domainId);
                // TODO: check and create group in CMC
                LOG.info("Adding domain id=" + domainId + " with uuid=" + domain.getUuid());
            }
        });

        messageBus.subscribe(DomainManager.MESSAGE_REMOVE_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                DomainVO domain = (DomainVO) args;
                // TODO: remove/disable group in CMC
                LOG.info("Removing domain id=" + domain.getId() + " with uuid=" + domain.getUuid());
            }
        });

        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        if (!CloudianConnectorEnabled.value()) {
            return cmdList;
        }
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
                CloudianCmcHost,
                CloudianCmcPort,
                CloudianCmcProtocol,
                CloudianSsoKey
        };
    }
}
