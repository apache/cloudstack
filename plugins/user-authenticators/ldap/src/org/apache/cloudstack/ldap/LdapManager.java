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

import java.util.List;

import org.apache.cloudstack.api.command.LdapListConfigurationCmd;
import org.apache.cloudstack.api.response.LdapConfigurationResponse;
import org.apache.cloudstack.api.response.LdapUserResponse;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.Pair;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.response.LinkDomainToLdapResponse;

public interface LdapManager extends PluggableService {

    enum LinkType { GROUP, OU;}

    LdapConfigurationResponse addConfiguration(String hostname, int port) throws InvalidParameterValueException;

    boolean canAuthenticate(String principal, String password);

    LdapConfigurationResponse createLdapConfigurationResponse(LdapConfigurationVO configuration);

    LdapUserResponse createLdapUserResponse(LdapUser user);

    LdapConfigurationResponse deleteConfiguration(String hostname) throws InvalidParameterValueException;

    LdapUser getUser(final String username) throws NoLdapUserMatchingQueryException;

    LdapUser getUser(String username, String type, String name) throws NoLdapUserMatchingQueryException;

    List<LdapUser> getUsers() throws NoLdapUserMatchingQueryException;

    List<LdapUser> getUsersInGroup(String groupName) throws NoLdapUserMatchingQueryException;

    boolean isLdapEnabled();

    Pair<List<? extends LdapConfigurationVO>, Integer> listConfigurations(LdapListConfigurationCmd cmd);

    List<LdapUser> searchUsers(String query) throws NoLdapUserMatchingQueryException;

    LinkDomainToLdapResponse linkDomainToLdap(Long domainId, String type, String name, short accountType);

    public LdapTrustMapVO getDomainLinkedToLdap(long domainId);
}