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

import static com.amazonaws.services.s3.internal.Constants.HMAC_SHA1_ALGORITHM;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloudian.cloudstack.api.CloudianIsEnabledCmd;
import com.cloudian.cloudstack.api.CloudianSsoLoginCmd;
import com.google.common.base.Strings;

public class CloudianConnectorImpl extends ComponentLifecycleBase implements CloudianConnector, Configurable {
    private static final Logger LOG = Logger.getLogger(CloudianConnectorImpl.class);

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

    /**
     * Computes RFC 2104-compliant HMAC signature.
     *
     * @param data  The data to be signed.
     * @param key   The signing key.
     * @return The Base64-encoded RFC 2104-compliant HMAC signature.
     */
    public static String calculateRFC2104HMAC(String data, String key) {
        try {
            // get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);

            // get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());

            // return the base64-encode the hmac
            return Base64.encodeBase64String(rawHmac);
        } catch (Exception e) {
            // LOG?
        }
        return null;
    }

    public static String genSSOParams(String userId, String groupId, String key) {
        StringBuilder sts = new StringBuilder();
        sts.append("user=");
        sts.append(userId);
        sts.append("&group=");
        sts.append(groupId);
        sts.append("&timestamp=");
        sts.append(System.currentTimeMillis());

        String signature = calculateRFC2104HMAC(sts.toString(), key);
        if (Strings.isNullOrEmpty(signature)) {
            return null;
        }
        sts.append("&signature=");
        try {
            sts.append(URLEncoder.encode(signature, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return null;
        }

        sts.append("&redirect=");
        if (groupId.equals("0")) {
            sts.append("admin.htm");
        } else {
            sts.append("explorer.htm");
        }

        return sts.toString();
    }

    @Override
    public String generateSsoUrl() {
        // add user/group in CMC if not available
        // return generated login url using sso shared key

        String ssoparams = genSSOParams("admin", "0", CloudianSsoKey.value());
        if (ssoparams == null) {
            return null;
        }

        return "https://cmc.hs.yadav.xyz:8443/Cloudian/ssosecurelogin.htm?" + ssoparams;
    }

    @Override
    public boolean addGroup(final Domain domain) {
        if (domain == null || isConnectorDisabled()) {
            return false;
        }
        LOG.debug("Adding Cloudian group against domain uuid=" + domain.getUuid() + " name=" + domain.getName() + " path=" + domain.getPath());

        return false;
    }

    @Override
    public boolean removeGroup(final Domain domain) {
        if (domain == null || isConnectorDisabled()) {
            return false;
        }
        LOG.debug("Removing Cloudian group against domain uuid=" + domain.getUuid() + " name=" + domain.getName() + " path=" + domain.getPath());

        return false;
    }

    @Override
    public boolean addUserAccount(final Account account) {
        if (account == null || isConnectorDisabled()) {
            return false;
        }
        LOG.debug("Adding Cloudian user account with uuid=" + account.getUuid() + " name=" + account.getAccountName());
        final Domain domain = domainDao.findById(account.getId());

        return false;
    }

    @Override
    public boolean removeUserAccount(final Account account) {
        if (account == null || isConnectorDisabled()) {
            return false;
        }
        LOG.debug("Removing Cloudian user account with uuid=" + account.getUuid() + " name=" + account.getAccountName());
        final Domain domain = domainDao.findById(account.getId());

        return false;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

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
                CloudianCmcHost,
                CloudianCmcPort,
                CloudianCmcProtocol,
                CloudianSsoKey
        };
    }
}
