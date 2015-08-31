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

import com.cloud.server.auth.DefaultUserAuthenticator;
import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.Map;

public class LdapAuthenticator extends DefaultUserAuthenticator {
    private static final Logger s_logger = Logger.getLogger(LdapAuthenticator.class.getName());

    @Inject
    private LdapManager _ldapManager;
    @Inject
    private UserAccountDao _userAccountDao;

    public LdapAuthenticator() {
        super();
    }

    public LdapAuthenticator(final LdapManager ldapManager, final UserAccountDao userAccountDao) {
        super();
        _ldapManager = ldapManager;
        _userAccountDao = userAccountDao;
    }

    @Override
    public Pair<Boolean, ActionOnFailedAuthentication> authenticate(final String username, final String password, final Long domainId, final Map<String, Object[]> requestParameters) {

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            s_logger.debug("Username or Password cannot be empty");
            return new Pair<Boolean, ActionOnFailedAuthentication>(false, null);
        }

        final UserAccount user = _userAccountDao.getUserAccount(username, domainId);

        if (user == null) {
            s_logger.debug("Unable to find user with " + username + " in domain " + domainId);
            return new Pair<Boolean, ActionOnFailedAuthentication>(false, null);
        } else if (_ldapManager.isLdapEnabled()) {
            boolean result = _ldapManager.canAuthenticate(username, password);
            ActionOnFailedAuthentication action = null;
            if (result == false) {
                action = ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT;
            }
            return new Pair<Boolean, ActionOnFailedAuthentication>(result, action);

        } else {
            return new Pair<Boolean, ActionOnFailedAuthentication>(false, ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT);
        }
    }

    @Override
    public String encode(final String password) {
        return password;
    }
}
