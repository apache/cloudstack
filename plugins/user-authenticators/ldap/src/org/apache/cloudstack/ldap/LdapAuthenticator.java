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

import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.server.auth.DefaultUserAuthenticator;
import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;

public class LdapAuthenticator extends DefaultUserAuthenticator {
	private static final Logger s_logger = Logger
			.getLogger(LdapAuthenticator.class.getName());

	@Inject
	private LdapManager _ldapManager;
	@Inject
	private UserAccountDao _userAccountDao;

	public LdapAuthenticator() {
		super();
	}

	public LdapAuthenticator(final LdapManager ldapManager,
			final UserAccountDao userAccountDao) {
		super();
		_ldapManager = ldapManager;
		_userAccountDao = userAccountDao;
	}

	@Override
	public boolean authenticate(final String username, final String password,
			final Long domainId, final Map<String, Object[]> requestParameters) {

		final UserAccount user = _userAccountDao.getUserAccount(username,
				domainId);

		if (user == null) {
			s_logger.debug("Unable to find user with " + username
					+ " in domain " + domainId);
			return false;
		} else if (_ldapManager.isLdapEnabled()) {
			return _ldapManager.canAuthenticate(username, password);
		} else {
			return false;
		}
	}

	@Override
	public String encode(final String password) {
		return password;
	}
}
