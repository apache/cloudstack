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
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class LdapUserManager {

	@Inject
	private LdapConfiguration _ldapConfiguration;

	public LdapUserManager() {
	}

	public LdapUserManager(final LdapConfiguration ldapConfiguration) {
		_ldapConfiguration = ldapConfiguration;
	}

	private LdapUser createUser(final SearchResult result)
			throws NamingException {
		final Attributes attributes = result.getAttributes();

		final String username = LdapUtils.getAttributeValue(attributes,
				_ldapConfiguration.getUsernameAttribute());
		final String email = LdapUtils.getAttributeValue(attributes,
				_ldapConfiguration.getEmailAttribute());
		final String firstname = LdapUtils.getAttributeValue(attributes,
				_ldapConfiguration.getFirstnameAttribute());
		final String lastname = LdapUtils.getAttributeValue(attributes,
				_ldapConfiguration.getLastnameAttribute());
		final String principal = result.getName() + ","
				+ _ldapConfiguration.getBaseDn();

		return new LdapUser(username, email, firstname, lastname, principal);
	}

	private String generateSearchFilter(final String username) {
		final StringBuilder userObjectFilter = new StringBuilder();
		userObjectFilter.append("(objectClass=");
		userObjectFilter.append(_ldapConfiguration.getUserObject());
		userObjectFilter.append(")");

		final StringBuilder usernameFilter = new StringBuilder();
		usernameFilter.append("(");
		usernameFilter.append(_ldapConfiguration.getUsernameAttribute());
		usernameFilter.append("=");
		usernameFilter.append((username == null ? "*" : username));
		usernameFilter.append(")");

		final StringBuilder memberOfFilter = new StringBuilder();
		if (_ldapConfiguration.getSearchGroupPrinciple() != null) {
			memberOfFilter.append("(memberof=");
			memberOfFilter.append(_ldapConfiguration.getSearchGroupPrinciple());
			memberOfFilter.append(")");
		}

		final StringBuilder result = new StringBuilder();
		result.append("(&");
		result.append(userObjectFilter);
		result.append(usernameFilter);
		result.append(memberOfFilter);
		result.append(")");

		return result.toString();
	}

	public LdapUser getUser(final String username, final DirContext context)
			throws NamingException {
		final NamingEnumeration<SearchResult> result = searchUsers(username,
				context);
		if (result.hasMoreElements()) {
			return createUser(result.nextElement());
		} else {
			throw new NamingException("No user found for username " + username);
		}
	}

	public List<LdapUser> getUsers(final DirContext context)
			throws NamingException {
		return getUsers(null, context);
	}

	public List<LdapUser> getUsers(final String username,
			final DirContext context) throws NamingException {
		final NamingEnumeration<SearchResult> results = searchUsers(username,
				context);

		final List<LdapUser> users = new ArrayList<LdapUser>();

		while (results.hasMoreElements()) {
			final SearchResult result = results.nextElement();
			users.add(createUser(result));
		}

		Collections.sort(users);

		return users;
	}

	public NamingEnumeration<SearchResult> searchUsers(final DirContext context)
			throws NamingException {
		return searchUsers(null, context);
	}

	public NamingEnumeration<SearchResult> searchUsers(final String username,
			final DirContext context) throws NamingException {
		final SearchControls controls = new SearchControls();

		controls.setSearchScope(_ldapConfiguration.getScope());
		controls.setReturningAttributes(_ldapConfiguration
				.getReturnAttributes());

		return context.search(_ldapConfiguration.getBaseDn(),
				generateSearchFilter(username), controls);
	}
}