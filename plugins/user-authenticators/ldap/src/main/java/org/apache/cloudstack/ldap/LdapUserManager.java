/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.ldap;

import java.io.IOException;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

public interface LdapUserManager {

    public enum Provider {
        MICROSOFTAD, OPENLDAP;
    }

    public LdapUser getUser(final String username, final LdapContext context, Long domainId) throws NamingException, IOException;

    public LdapUser getUser(final String username, final String type, final String name, final LdapContext context, Long domainId) throws NamingException, IOException;

    public List<LdapUser> getUsers(final LdapContext context, Long domainId) throws NamingException, IOException;

    public List<LdapUser> getUsers(final String username, final LdapContext context, Long domainId) throws NamingException, IOException;

    public List<LdapUser> getUsersInGroup(String groupName, LdapContext context, Long domainId) throws NamingException;

    public List<LdapUser> searchUsers(final LdapContext context, Long domainId) throws NamingException, IOException;

    public List<LdapUser> searchUsers(final String username, final LdapContext context, Long domainId) throws NamingException, IOException;
}
