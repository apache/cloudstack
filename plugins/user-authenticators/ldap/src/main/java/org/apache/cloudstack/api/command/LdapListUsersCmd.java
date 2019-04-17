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
package org.apache.cloudstack.api.command;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.cloud.domain.Domain;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.user.ListUsersCmd;
import org.apache.cloudstack.api.response.LdapUserResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.apache.cloudstack.ldap.NoLdapUserMatchingQueryException;
import org.apache.cloudstack.query.QueryService;

import com.cloud.user.Account;

/**
 * @startuml
 * start
 * :list ldap users request;
 * :get ldap binding;
 * if (domain == null) then (true)
 *   :get global trust domain;
 * else (false)
 *   :get trustdomain for domain;
 * endif
 * :get ldap users\n using trust domain;
 * if (filter == 'NoFilter') then (pass as is)
 * elseif (filter == 'AnyDomain') then (anydomain)
 *   :filterList = all\n\t\tcloudstack\n\t\tusers;
 * elseif (filter == 'LocalDomain')
 *   :filterList = local users\n\t\tfor domain;
 * elseif (filter == 'PotentialImport') then (address account\nsynchronisation\nconfigurations)
 *   :query\n the account\n bindings;
 *   :check and markup\n ldap users\n for bound OUs\n with usersource;
 * else ( unknown value for filter )
 *   :throw invalid parameter;
 *   stop
 * endif
 *   :remove users in filterList\nfrom ldap users list;
 * :return remaining;
 * stop
 * @enduml
 */
@APICommand(name = "listLdapUsers", responseObject = LdapUserResponse.class, description = "Lists all LDAP Users", since = "4.2.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class LdapListUsersCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(LdapListUsersCmd.class.getName());
    private static final String s_name = "ldapuserresponse";
    @Inject
    private LdapManager _ldapManager;

    // TODO queryservice is already injected oin basecmd remove it here
    @Inject
    private QueryService _queryService;

    @Parameter(name = "listtype",
            type = CommandType.STRING,
            required = false,
            description = "Determines whether all ldap users are returned or just non-cloudstack users. This option is deprecated in favour for the more option rich 'userfilter' parameter")
    @Deprecated
    private String listType;

    @Parameter(name = ApiConstants.USER_FILTER,
            type = CommandType.STRING,
            required = false,
            since = "4.13",
            description = "Determines what type of filter is applied on the list of users returned from LDAP.\n"
                    + "\tvalid values are\n"
                    + "\t'NoFilter'\t no filtering is done,\n"
                    + "\t'LocalDomain'\tusers already in the current or requested domain will be filtered out of the result list,\n"
                    + "\t'AnyDomain'\tusers that already exist anywhere in cloudstack will be filtered out, and\n"
                    + "\t'PotentialImport'\tall users that would be automatically imported from the listing will be shown,"
                    + " including those that are already in cloudstack, the later will be annotated with their userSource")
    private String userFilter;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, required = false, entityType = DomainResponse.class, description = "linked domain")
    private Long domainId;

    public LdapListUsersCmd() {
        super();
    }

    public LdapListUsersCmd(final LdapManager ldapManager, final QueryService queryService) {
        super();
        _ldapManager = ldapManager;
        _queryService = queryService;
    }

    /**
     * (as a check for isACloudstackUser is done) only non cloudstack users should be shown
     * @param users a list of {@code LdapUser}s
     * @return a (filtered?) list of user response objects
     */
    private List<LdapUserResponse> createLdapUserResponse(final List<LdapUser> users) {
        final List<LdapUserResponse> ldapResponses = new ArrayList<LdapUserResponse>();
        for (final LdapUser user : users) {
            final LdapUserResponse ldapResponse = _ldapManager.createLdapUserResponse(user);
            ldapResponse.setObjectName("LdapUser");
            ldapResponses.add(ldapResponse);
        }
        return ldapResponses;
    }

    private List<UserResponse> cloudstackUsers = null;

    @Override
    public void execute() throws ServerApiException {
        List<LdapUserResponse> ldapResponses = new ArrayList<LdapUserResponse>();
        final ListResponse<LdapUserResponse> response = new ListResponse<LdapUserResponse>();
        try {
            final List<LdapUser> users = _ldapManager.getUsers(domainId);
            ldapResponses = createLdapUserResponse(users);
//            now filter and annotate
            ldapResponses = applyUserFilter(ldapResponses);
        } catch (final NoLdapUserMatchingQueryException ex) {
            // ok, we'll make do with the empty list ldapResponses = new ArrayList<LdapUserResponse>();
        } finally {
            response.setResponses(ldapResponses);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        }
    }

    private List<UserResponse> getCloudstackUsers() {
        if (cloudstackUsers == null) {
            final ListResponse<UserResponse> cloudstackUsersresponse = _queryService.searchForUsers(new ListUsersCmd());
            cloudstackUsers = cloudstackUsersresponse.getResponses();
        }
        return cloudstackUsers;
    }

    private List<LdapUserResponse> applyUserFilter(List<LdapUserResponse> ldapResponses) {
        Method filterMethod = getFilterMethod();
        List<LdapUserResponse> responseList = ldapResponses;
        if (filterMethod != null) {
            try {
                responseList = (List<LdapUserResponse>)filterMethod.invoke(this, ldapResponses);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new CloudRuntimeException("we're damned, the earth is screwed. we will now return to our maker",e);
            }
        }
        return responseList;
    }

    Method getFilterMethod() {
        Method method = null;
        try {
            method = this.getClass().getMethod("filter" + getUserFilter().toString(), java.util.List.class);
            Type returnType = method.getGenericReturnType();
            checkFilterMethodType(returnType);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format("filter method filter%s not found; not filtering ldap users", getUserFilter()));
        }
        return method;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    String getListTypeString() {
        return listType == null ? "all" : listType;
    }

    String getUserFilterString() {
        return userFilter == null ? getListTypeString() == null ? "NoFilter" : getListTypeString().equals("any") ? "NoFilter" : "AnyDomain" : userFilter;
    }

    UserFilter getUserFilter() {
        return UserFilter.fromString(getUserFilterString());
    }

    boolean isACloudstackUser(final LdapUser ldapUser) {
        boolean rc = false;
        final List<UserResponse> cloudstackUsers = getCloudstackUsers();
        if (cloudstackUsers != null) {
            for (final UserResponse cloudstackUser : cloudstackUsers) {
                if (ldapUser.getUsername().equals(cloudstackUser.getUsername())) {
                    rc = true;
                }
            }
        }
        return rc;
    }

    boolean isACloudstackUser(final LdapUserResponse ldapUser) {
        final List<UserResponse> cloudstackUsers = getCloudstackUsers();
        if (cloudstackUsers != null && cloudstackUsers.size() != 0) {
            for (final UserResponse cloudstackUser : cloudstackUsers) {
                if (ldapUser.getUsername().equals(cloudstackUser.getUsername())) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * typecheck for userfilter values
     */
    enum UserFilter {
        NO_FILTER("NoFilter"),
        LOCAL_DOMAIN("LocalDomain"),
        ANY_DOMAIN("AnyDomain"),
        POTENTIAL_IMPORT("PotentialImport");

        private final String value;

        UserFilter(String val) {
            this.value = val;
        }

        static UserFilter fromString(String val) {
            if(NO_FILTER.toString().equalsIgnoreCase(val)) {
                return NO_FILTER;
            } else if (LOCAL_DOMAIN.toString().equalsIgnoreCase(val)) {
                return LOCAL_DOMAIN;
            } else if(ANY_DOMAIN.toString().equalsIgnoreCase(val)) {
                return ANY_DOMAIN;
            } else if(POTENTIAL_IMPORT.toString().equalsIgnoreCase(val)) {
                return POTENTIAL_IMPORT;
            } else {
                throw new IllegalArgumentException(String.format("%s is not a legal 'UserFilter' value", val));
            }
        }

        @Override public String toString() {
            return value;
        }
    }

    /**
     * no filtering but improve with annotation of source for existing ACS users
     * @param input ldap response list of users
     * @return unfiltered list of the input list of ldap users
     */
    public List<LdapUserResponse> filterNoFilter(List<LdapUserResponse> input) {
        for (final LdapUserResponse user : input) {
            annotateCloudstackSource(user);
        }
        return input;
    }

    /**
     * filter the list of ldap users. no users visible to the caller should be in the returned list
     * @param input ldap response list of users
     * @return a list of ldap users not already in ACS
     */
    public List<LdapUserResponse> filterAnyDomain(List<LdapUserResponse> input) {
        final List<LdapUserResponse> ldapResponses = new ArrayList<LdapUserResponse>();
        for (final LdapUserResponse user : input) {
            if (!isACloudstackUser(user)) {
                ldapResponses.add(user);
            }
        }
        return ldapResponses;
    }

    /**
     * filter the list of ldap users. no users visible to the caller already in the domain specified should be in the returned list
     * @param input ldap response list of users
     * @return a list of ldap users not already in ACS
     */
    public List<LdapUserResponse> filterLocalDomain(List<LdapUserResponse> input) {
        final List<LdapUserResponse> ldapResponses = new ArrayList<LdapUserResponse>();
        String domainId = getCurrentDomainId();
        for (final LdapUserResponse user : input) {
            UserResponse cloudstackUser = getCloudstackUser(user);
            if (cloudstackUser == null || !domainId.equals(cloudstackUser.getDomainId())) {
                ldapResponses.add(user);
            }
        }
        return ldapResponses;
    }

    private String getCurrentDomainId() {
        String domainId = null;
        if (this.domainId != null) {
            Domain domain = _domainService.getDomain(this.domainId);
            domainId = domain.getUuid();
        } else {
            final CallContext callContext = CallContext.current();
            domainId = _domainService.getDomain(callContext.getCallingAccount().getDomainId()).getUuid();
        }
        return domainId;
    }

    /**
     *
     * @param input a list of ldap users
     * @return annotated list of the users of the input list, that will be automatically imported or synchronised
     */
    public List<LdapUserResponse> filterPotentialImport(List<LdapUserResponse> input) {
        for (final LdapUserResponse user : input) {
            annotateCloudstackSource(user);
        }
        return input;
    }

    private void annotateCloudstackSource(LdapUserResponse user) {
        final UserResponse cloudstackUser = getCloudstackUser(user);
        if (cloudstackUser != null) {
            user.setUserSource(cloudstackUser.getUserSource());
        }
    }

    private UserResponse getCloudstackUser(LdapUserResponse user) {
        UserResponse returnObject = null;
        final List<UserResponse> cloudstackUsers = getCloudstackUsers();
        if (cloudstackUsers != null) {
            for (final UserResponse cloudstackUser : cloudstackUsers) {
                if (user.getUsername().equals(cloudstackUser.getUsername())) {
                    returnObject = cloudstackUser;
                }
            }
        }
        return returnObject;
    }

    private void checkFilterMethodType(Type returnType) {
        String msg = null;
        if (returnType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) returnType;
            if(type.getRawType().equals(List.class)) {
                Type[] typeArguments = type.getActualTypeArguments();
                if (typeArguments.length == 1) {
                    if (typeArguments[0].equals(LdapUserResponse.class)) {
                        // we're good'
                    } else {
                        msg = new String("list of return type contains " + typeArguments[0].getTypeName());
                    }
                } else {
                    msg = String.format("type %s has to the wrong number of arguments", type.getRawType());
                }
            } else {
                msg = String.format("type %s is not a List<>", type.getTypeName());
            }
        } else {
            msg = new String("can't even begin to explain; review your method signature");
        }
        if(msg != null) {
            throw new IllegalArgumentException(msg);
        }
    }

}