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
package org.apache.cloudstack.ldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import java.util.UUID;

import org.apache.cloudstack.api.LdapValidator;
import org.apache.cloudstack.api.command.LDAPConfigCmd;
import org.apache.cloudstack.api.command.LDAPRemoveCmd;
import org.apache.cloudstack.api.command.LdapAddConfigurationCmd;
import org.apache.cloudstack.api.command.LdapCreateAccountCmd;
import org.apache.cloudstack.api.command.LdapDeleteConfigurationCmd;
import org.apache.cloudstack.api.command.LdapImportUsersCmd;
import org.apache.cloudstack.api.command.LdapListConfigurationCmd;
import org.apache.cloudstack.api.command.LdapListUsersCmd;
import org.apache.cloudstack.api.command.LdapUserSearchCmd;
import org.apache.cloudstack.api.command.LinkAccountToLdapCmd;
import org.apache.cloudstack.api.command.LinkDomainToLdapCmd;
import org.apache.cloudstack.api.response.LdapConfigurationResponse;
import org.apache.cloudstack.api.response.LdapUserResponse;
import org.apache.cloudstack.api.response.LinkAccountToLdapResponse;
import org.apache.cloudstack.api.response.LinkDomainToLdapResponse;
import org.apache.cloudstack.ldap.dao.LdapConfigurationDao;
import org.apache.cloudstack.ldap.dao.LdapTrustMapDao;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;

@Component
public class LdapManagerImpl implements LdapManager, LdapValidator {
    private static final Logger s_logger = Logger.getLogger(LdapManagerImpl.class.getName());

    @Inject
    private LdapConfigurationDao _ldapConfigurationDao;

    @Inject
    private DomainDao domainDao;

    @Inject
    private AccountDao accountDao;

    @Inject
    private LdapContextFactory _ldapContextFactory;

    @Inject
    private LdapConfiguration _ldapConfiguration;

    @Inject LdapUserManagerFactory _ldapUserManagerFactory;

    @Inject
    LdapTrustMapDao _ldapTrustMapDao;


    public LdapManagerImpl() {
        super();
    }

    public LdapManagerImpl(final LdapConfigurationDao ldapConfigurationDao, final LdapContextFactory ldapContextFactory, final LdapUserManagerFactory ldapUserManagerFactory,
                           final LdapConfiguration ldapConfiguration) {
        super();
        _ldapConfigurationDao = ldapConfigurationDao;
        _ldapContextFactory = ldapContextFactory;
        _ldapUserManagerFactory = ldapUserManagerFactory;
        _ldapConfiguration = ldapConfiguration;
    }

    @Override
    public LdapConfigurationResponse addConfiguration(final LdapAddConfigurationCmd cmd) throws InvalidParameterValueException {
        return addConfigurationInternal(cmd.getHostname(),cmd.getPort(),cmd.getDomainId());
    }

    @Override // TODO make private
    public LdapConfigurationResponse addConfiguration(final String hostname, int port, final Long domainId) throws InvalidParameterValueException {
        return addConfigurationInternal(hostname,port,domainId);
    }

    private LdapConfigurationResponse addConfigurationInternal(final String hostname, int port, final Long domainId) throws InvalidParameterValueException {
        // TODO evaluate what the right default should be
        if(port <= 0) {
            port = 389;
        }

        // hostname:port is unique for domain binding
        LdapConfigurationVO configuration = _ldapConfigurationDao.find(hostname, port, domainId);
        if (configuration == null) {
            LdapContext context = null;
            try {
                final String providerUrl = "ldap://" + hostname + ":" + port;
                context = _ldapContextFactory.createBindContext(providerUrl,domainId);
                configuration = new LdapConfigurationVO(hostname, port, domainId);
                _ldapConfigurationDao.persist(configuration);
                s_logger.info("Added new ldap server with url: " + providerUrl + (domainId == null ? "": " for domain " + domainId));
                return createLdapConfigurationResponse(configuration);
            } catch (NamingException | IOException e) {
                s_logger.debug("NamingException while doing an LDAP bind", e);
                throw new InvalidParameterValueException("Unable to bind to the given LDAP server");
            } finally {
                closeContext(context);
            }
        } else {
            throw new InvalidParameterValueException("Duplicate configuration");
        }
    }

    /**
     * TODO decide if the principal is good enough to get the domain id or we need to add it as parameter
     * @param principal
     * @param password
     * @param domainId
     * @return
     */
    @Override
    public boolean canAuthenticate(final String principal, final String password, final Long domainId) {
        try {
            // TODO return the right account for this user
            final LdapContext context = _ldapContextFactory.createUserContext(principal, password,domainId);
            closeContext(context);
            return true;
        } catch (NamingException | IOException e) {
            s_logger.debug("Exception while doing an LDAP bind for user "+" "+principal, e);
            s_logger.info("Failed to authenticate user: " + principal + ". incorrect password.");
            return false;
        }
    }

    private void closeContext(final LdapContext context) {
        try {
            if (context != null) {
                context.close();
            }
        } catch (final NamingException e) {
            s_logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public LdapConfigurationResponse createLdapConfigurationResponse(final LdapConfigurationVO configuration) {
        String domainUuid = null;
        if(configuration.getDomainId() != null) {
            domainUuid = domainDao.findById(configuration.getDomainId()).getUuid();
        }
        return new LdapConfigurationResponse(configuration.getHostname(), configuration.getPort(), domainUuid);
    }

    @Override
    public LdapUserResponse createLdapUserResponse(final LdapUser user) {
        final LdapUserResponse response = new LdapUserResponse();
        response.setUsername(user.getUsername());
        response.setFirstname(user.getFirstname());
        response.setLastname(user.getLastname());
        response.setEmail(user.getEmail());
        response.setPrincipal(user.getPrincipal());
        response.setDomain(user.getDomain());
        return response;
    }

    @Override
    public LdapConfigurationResponse deleteConfiguration(final LdapDeleteConfigurationCmd cmd) throws InvalidParameterValueException {
        return deleteConfigurationInternal(cmd.getHostname(), cmd.getPort(), cmd.getDomainId());
    }

    @Override
    public LdapConfigurationResponse deleteConfiguration(final String hostname, int port, Long domainId) throws InvalidParameterValueException {
        return deleteConfigurationInternal(hostname, port, domainId);
    }

    private LdapConfigurationResponse deleteConfigurationInternal(final String hostname, int port, Long domainId) throws InvalidParameterValueException {
        final LdapConfigurationVO configuration = _ldapConfigurationDao.find(hostname,port,domainId);
        if (configuration == null) {
            throw new InvalidParameterValueException("Cannot find configuration with hostname " + hostname);
        } else {
            _ldapConfigurationDao.remove(configuration.getId());
            s_logger.info("Removed ldap server with url: " + hostname + ':' + port + (domainId == null ? "" : " for domain id " + domainId));
            return createLdapConfigurationResponse(configuration);
        }
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(LdapUserSearchCmd.class);
        cmdList.add(LdapListUsersCmd.class);
        cmdList.add(LdapAddConfigurationCmd.class);
        cmdList.add(LdapDeleteConfigurationCmd.class);
        cmdList.add(LdapListConfigurationCmd.class);
        cmdList.add(LdapCreateAccountCmd.class);
        cmdList.add(LdapImportUsersCmd.class);
        cmdList.add(LDAPConfigCmd.class);
        cmdList.add(LDAPRemoveCmd.class);
        cmdList.add(LinkDomainToLdapCmd.class);
        cmdList.add(LinkAccountToLdapCmd.class);
        return cmdList;
    }

    @Override
    public LdapUser getUser(final String username, Long domainId) throws NoLdapUserMatchingQueryException {
        LdapContext context = null;
        try {
            context = _ldapContextFactory.createBindContext(domainId);

            final String escapedUsername = LdapUtils.escapeLDAPSearchFilter(username);
            return _ldapUserManagerFactory.getInstance(_ldapConfiguration.getLdapProvider(null)).getUser(escapedUsername, context, domainId);

        } catch (NamingException | IOException e) {
            s_logger.debug("ldap Exception: ",e);
            throw new NoLdapUserMatchingQueryException("No Ldap User found for username: "+username);
        } finally {
            closeContext(context);
        }
    }

    @Override
    public LdapUser getUser(final String username, final String type, final String name, Long domainId) throws NoLdapUserMatchingQueryException {
        LdapContext context = null;
        try {
            context = _ldapContextFactory.createBindContext(domainId);
            final String escapedUsername = LdapUtils.escapeLDAPSearchFilter(username);
            return _ldapUserManagerFactory.getInstance(_ldapConfiguration.getLdapProvider(null)).getUser(escapedUsername, type, name, context, domainId);
        } catch (NamingException | IOException e) {
            s_logger.debug("ldap Exception: ",e);
            throw new NoLdapUserMatchingQueryException("No Ldap User found for username: "+username + " in group: " + name + " of type: " + type);
        } finally {
            closeContext(context);
        }
    }

    @Override
    public List<LdapUser> getUsers(Long domainId) throws NoLdapUserMatchingQueryException {
        LdapContext context = null;
        try {
            context = _ldapContextFactory.createBindContext(domainId);
            return _ldapUserManagerFactory.getInstance(_ldapConfiguration.getLdapProvider(domainId)).getUsers(context, domainId);
        } catch (NamingException | IOException e) {
            s_logger.debug("ldap Exception: ",e);
            throw new NoLdapUserMatchingQueryException("*");
        } finally {
            closeContext(context);
        }
    }

    @Override
    public List<LdapUser> getUsersInGroup(String groupName, Long domainId) throws NoLdapUserMatchingQueryException {
        LdapContext context = null;
        try {
            context = _ldapContextFactory.createBindContext(domainId);
            return _ldapUserManagerFactory.getInstance(_ldapConfiguration.getLdapProvider(domainId)).getUsersInGroup(groupName, context, domainId);
        } catch (NamingException | IOException e) {
            s_logger.debug("ldap NamingException: ",e);
            throw new NoLdapUserMatchingQueryException("groupName=" + groupName);
        } finally {
            closeContext(context);
        }
    }

    @Override
    public boolean isLdapEnabled() {
        return listConfigurations(new LdapListConfigurationCmd(this)).second() > 0;
    }

    @Override
    public Pair<List<? extends LdapConfigurationVO>, Integer> listConfigurations(final LdapListConfigurationCmd cmd) {
        final String hostname = cmd.getHostname();
        final int port = cmd.getPort();
        final Long domainId = cmd.getDomainId();
        final Pair<List<LdapConfigurationVO>, Integer> result = _ldapConfigurationDao.searchConfigurations(hostname, port, domainId);
        return new Pair<List<? extends LdapConfigurationVO>, Integer>(result.first(), result.second());
    }

    @Override
    public List<LdapUser> searchUsers(final String username) throws NoLdapUserMatchingQueryException {
        LdapContext context = null;
        try {
            // TODO search users per domain (only?)
            context = _ldapContextFactory.createBindContext(null);
            final String escapedUsername = LdapUtils.escapeLDAPSearchFilter(username);
            return _ldapUserManagerFactory.getInstance(_ldapConfiguration.getLdapProvider(null)).getUsers("*" + escapedUsername + "*", context, null);
        } catch (NamingException | IOException e) {
            s_logger.debug("ldap Exception: ",e);
            throw new NoLdapUserMatchingQueryException(username);
        } finally {
            closeContext(context);
        }
    }

    @Override
    public LinkDomainToLdapResponse linkDomainToLdap(LinkDomainToLdapCmd cmd) {
        Validate.isTrue(_ldapConfiguration.getBaseDn(cmd.getDomainId()) == null, "can not configure an ldap server and an ldap group/ou to a domain");
        Validate.notEmpty(cmd.getLdapDomain(), "ldapDomain cannot be empty, please supply a GROUP or OU name");
        return linkDomainToLdap(cmd.getDomainId(),cmd.getType(),cmd.getLdapDomain(),cmd.getAccountType());
    }

    private LinkDomainToLdapResponse linkDomainToLdap(Long domainId, String type, String name, short accountType) {
        Validate.notNull(type, "type cannot be null. It should either be GROUP or OU");
        Validate.notNull(domainId, "domainId cannot be null.");
        Validate.notEmpty(name, "GROUP or OU name cannot be empty");
        //Account type should be 0 or 2. check the constants in com.cloud.user.Account
        Validate.isTrue(accountType==0 || accountType==2, "accountype should be either 0(normal user) or 2(domain admin)");
        LinkType linkType = LdapManager.LinkType.valueOf(type.toUpperCase());
        LdapTrustMapVO vo = _ldapTrustMapDao.persist(new LdapTrustMapVO(domainId, linkType, name, accountType, 0));
        DomainVO domain = domainDao.findById(vo.getDomainId());
        String domainUuid = "<unknown>";
        if (domain == null) {
            s_logger.error("no domain in database for id " + vo.getDomainId());
        } else {
            domainUuid = domain.getUuid();
        }
        LinkDomainToLdapResponse response = new LinkDomainToLdapResponse(domainUuid, vo.getType().toString(), vo.getName(), vo.getAccountType());
        return response;
    }

    @Override
    public LdapTrustMapVO getDomainLinkedToLdap(long domainId){
        return _ldapTrustMapDao.findByDomainId(domainId);
    }

    @Override
    public List<LdapTrustMapVO> getDomainLinkage(long domainId){
        return _ldapTrustMapDao.searchByDomainId(domainId);
    }

    public LdapTrustMapVO getAccountLinkedToLdap(long domainId, long accountId){
        return _ldapTrustMapDao.findByAccount(domainId, accountId);
    }

    @Override
    public LdapTrustMapVO getLinkedLdapGroup(long domainId, String group) {
        return _ldapTrustMapDao.findGroupInDomain(domainId, group);
    }

    @Override public LinkAccountToLdapResponse linkAccountToLdap(LinkAccountToLdapCmd cmd) {
        Validate.notNull(_ldapConfiguration.getBaseDn(cmd.getDomainId()), "can not configure an ldap server and an ldap group/ou to a domain");
        Validate.notNull(cmd.getDomainId(), "domainId cannot be null.");
        Validate.notEmpty(cmd.getAccountName(), "accountName cannot be empty.");
        Validate.notEmpty(cmd.getLdapDomain(), "ldapDomain cannot be empty, please supply a GROUP or OU name");
        Validate.notNull(cmd.getType(), "type cannot be null. It should either be GROUP or OU");
        Validate.notEmpty(cmd.getLdapDomain(), "GROUP or OU name cannot be empty");

        LinkType linkType = LdapManager.LinkType.valueOf(cmd.getType().toUpperCase());
        Account account = accountDao.findActiveAccount(cmd.getAccountName(),cmd.getDomainId());
        if (account == null) {
            account = new AccountVO(cmd.getAccountName(), cmd.getDomainId(), null, cmd.getAccountType(), UUID.randomUUID().toString());
            accountDao.persist((AccountVO)account);
        }
        Long accountId = account.getAccountId();
        LdapTrustMapVO vo = _ldapTrustMapDao.persist(new LdapTrustMapVO(cmd.getDomainId(), linkType, cmd.getLdapDomain(), cmd.getAccountType(), accountId));
        DomainVO domain = domainDao.findById(vo.getDomainId());
        String domainUuid = "<unknown>";
        if (domain == null) {
            s_logger.error("no domain in database for id " + vo.getDomainId());
        } else {
            domainUuid = domain.getUuid();
        }

        LinkAccountToLdapResponse response = new LinkAccountToLdapResponse(domainUuid, vo.getType().toString(), vo.getName(), vo.getAccountType(), account.getUuid(), cmd.getAccountName());
        return response;
    }
}
