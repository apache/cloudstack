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

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.LdapValidator;
import org.apache.cloudstack.api.command.LdapAddConfigurationCmd;
import org.apache.cloudstack.api.command.LdapCreateAccountCmd;
import org.apache.cloudstack.api.command.LdapDeleteConfigurationCmd;
import org.apache.cloudstack.api.command.LdapImportUsersCmd;
import org.apache.cloudstack.api.command.LdapListConfigurationCmd;
import org.apache.cloudstack.api.command.LdapListUsersCmd;
import org.apache.cloudstack.api.command.LdapUserSearchCmd;
import org.apache.cloudstack.api.response.LdapConfigurationResponse;
import org.apache.cloudstack.api.response.LdapUserResponse;
import org.apache.cloudstack.ldap.dao.LdapConfigurationDao;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.Pair;

@Component
@Local(value = LdapManager.class)
public class LdapManagerImpl implements LdapManager, LdapValidator {
    private static final Logger s_logger = Logger.getLogger(LdapManagerImpl.class.getName());

    @Inject
    private LdapConfigurationDao _ldapConfigurationDao;

    @Inject
    private LdapContextFactory _ldapContextFactory;

    @Inject
    private LdapUserManager _ldapUserManager;

    public LdapManagerImpl() {
        super();
    }

    public LdapManagerImpl(final LdapConfigurationDao ldapConfigurationDao, final LdapContextFactory ldapContextFactory, final LdapUserManager ldapUserManager) {
        super();
        _ldapConfigurationDao = ldapConfigurationDao;
        _ldapContextFactory = ldapContextFactory;
        _ldapUserManager = ldapUserManager;
    }

    @Override
    public LdapConfigurationResponse addConfiguration(final String hostname, final int port) throws InvalidParameterValueException {
        LdapConfigurationVO configuration = _ldapConfigurationDao.findByHostname(hostname);
        if (configuration == null) {
            try {
                final String providerUrl = "ldap://" + hostname + ":" + port;
                _ldapContextFactory.createBindContext(providerUrl);
                configuration = new LdapConfigurationVO(hostname, port);
                _ldapConfigurationDao.persist(configuration);
                s_logger.info("Added new ldap server with hostname: " + hostname);
                return new LdapConfigurationResponse(hostname, port);
            } catch (final NamingException e) {
                throw new InvalidParameterValueException("Unable to bind to the given LDAP server");
            }
        } else {
            throw new InvalidParameterValueException("Duplicate configuration");
        }
    }

    @Override
    public boolean canAuthenticate(final String username, final String password) {
        final String escapedUsername = LdapUtils.escapeLDAPSearchFilter(username);
        try {
            final LdapUser user = getUser(escapedUsername);
            final String principal = user.getPrincipal();
            final DirContext context = _ldapContextFactory.createUserContext(principal, password);
            closeContext(context);
            return true;
        } catch (final NamingException e) {
            s_logger.info("Failed to authenticate user: " + username + ". incorrect password.");
            return false;
        }
    }

    private void closeContext(final DirContext context) {
        try {
            if (context != null) {
                context.close();
            }
        } catch (final NamingException e) {
            s_logger.warn(e.getMessage());
        }
    }

    @Override
    public LdapConfigurationResponse createLdapConfigurationResponse(final LdapConfigurationVO configuration) {
        final LdapConfigurationResponse response = new LdapConfigurationResponse();
        response.setHostname(configuration.getHostname());
        response.setPort(configuration.getPort());
        return response;
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
    public LdapConfigurationResponse deleteConfiguration(final String hostname) throws InvalidParameterValueException {
        final LdapConfigurationVO configuration = _ldapConfigurationDao.findByHostname(hostname);
        if (configuration == null) {
            throw new InvalidParameterValueException("Cannot find configuration with hostname " + hostname);
        } else {
            _ldapConfigurationDao.remove(configuration.getId());
            s_logger.info("Removed ldap server with hostname: " + hostname);
            return new LdapConfigurationResponse(configuration.getHostname(), configuration.getPort());
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
        return cmdList;
    }

    @Override
    public LdapUser getUser(final String username) throws NamingException {
        DirContext context = null;
        try {
            context = _ldapContextFactory.createBindContext();

            final String escapedUsername = LdapUtils.escapeLDAPSearchFilter(username);
            return _ldapUserManager.getUser(escapedUsername, context);

        } catch (final NamingException e) {
            throw e;
        } finally {
            closeContext(context);
        }
    }

    @Override
    public List<LdapUser> getUsers() throws NoLdapUserMatchingQueryException {
        DirContext context = null;
        try {
            context = _ldapContextFactory.createBindContext();
            return _ldapUserManager.getUsers(context);
        } catch (final NamingException e) {
            throw new NoLdapUserMatchingQueryException("*");
        } finally {
            closeContext(context);
        }
    }

    @Override
    public List<LdapUser> getUsersInGroup(String groupName) throws NoLdapUserMatchingQueryException {
        DirContext context = null;
        try {
            context = _ldapContextFactory.createBindContext();
            return _ldapUserManager.getUsersInGroup(groupName, context);
        } catch (final NamingException e) {
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
        final Pair<List<LdapConfigurationVO>, Integer> result = _ldapConfigurationDao.searchConfigurations(hostname, port);
        return new Pair<List<? extends LdapConfigurationVO>, Integer>(result.first(), result.second());
    }

    @Override
    public List<LdapUser> searchUsers(final String username) throws NoLdapUserMatchingQueryException {
        DirContext context = null;
        try {
            context = _ldapContextFactory.createBindContext();
            final String escapedUsername = LdapUtils.escapeLDAPSearchFilter(username);
            return _ldapUserManager.getUsers("*" + escapedUsername + "*", context);
        } catch (final NamingException e) {
            throw new NoLdapUserMatchingQueryException(username);
        } finally {
            closeContext(context);
        }
    }
}