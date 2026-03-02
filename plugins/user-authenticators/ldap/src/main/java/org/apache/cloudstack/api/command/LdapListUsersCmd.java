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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import com.cloud.domain.Domain;
import com.cloud.user.User;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.LdapUserResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.apache.cloudstack.ldap.NoLdapUserMatchingQueryException;
import org.apache.cloudstack.query.QueryService;

import com.cloud.user.Account;
import org.apache.commons.collections.CollectionUtils;

/**
 * a short flow, use plantuml to view (see <a href="http://plantuml.com">the plantuml site</a>)
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
@APICommand(name = "listLdapUsers", responseObject = LdapUserResponse.class, description = "Lists LDAP Users according to the specifications from the user request.", since = "4.2.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin,RoleType.DomainAdmin})
public class LdapListUsersCmd extends BaseListCmd {

    private static final String s_name = "ldapuserresponse";
    @Inject
    private LdapManager _ldapManager;

    @Parameter(name = "listtype",
            type = CommandType.STRING,
            description = "Determines whether all ldap users are returned or just non-cloudstack users. This option is deprecated in favour for the more option rich 'userfilter' parameter")
    @Deprecated
    private String listType;

    @Parameter(name = ApiConstants.USER_FILTER,
            type = CommandType.STRING,
            since = "4.13",
            description = "Determines what type of filter is applied on the list of users returned from LDAP.\n"
                    + "\tvalid values are\n"
                    + "\t'NoFilter'\t no filtering is done,\n"
                    + "\t'LocalDomain'\tusers already in the current or requested domain will be filtered out of the result list,\n"
                    + "\t'AnyDomain'\tusers that already exist anywhere in cloudstack will be filtered out, and\n"
                    + "\t'PotentialImport'\tall users that would be automatically imported from the listing will be shown,"
                    + " including those that are already in cloudstack, the later will be annotated with their userSource")
    private String userFilter;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "Linked Domain")
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
        final List<LdapUserResponse> ldapResponses = new ArrayList<>();
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
        cloudstackUsers = null;
        List<LdapUserResponse> ldapResponses = new ArrayList<>();
        final ListResponse<LdapUserResponse> response = new ListResponse<>();
        try {
            final List<LdapUser> users = _ldapManager.getUsers(domainId);
            ldapResponses = createLdapUserResponse(users);
            // now filter and annotate
            ldapResponses = applyUserFilter(ldapResponses);
        } catch (final NoLdapUserMatchingQueryException ex) {
            logger.debug(ex.getMessage());
            // ok, we'll make do with the empty list
        } finally {
            response.setResponses(ldapResponses);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        }
    }

    /**
     * get a list of relevant cloudstack users, depending on the userFilter
     */
    private List<UserResponse> getCloudstackUsers() {
        if (cloudstackUsers == null) {
            try {
                cloudstackUsers = getUserFilter().getCloudstackUserList(this).getResponses();
            } catch (IllegalArgumentException e) {
                throw new CloudRuntimeException("error in program login; we are not filtering but still querying users to filter???", e);
            }
            traceUserList();
        }
        return cloudstackUsers;
    }

    private void traceUserList() {
        if(logger.isTraceEnabled()) {
            StringBuilder users = new StringBuilder();
            for (UserResponse user : cloudstackUsers) {
                if (users.length()> 0) {
                    users.append(", ");
                }
                users.append(user.getUsername());
            }

            logger.trace("checking against {} cloudstackusers: {}.", this.cloudstackUsers.size(), users);
        }
    }

    private List<LdapUserResponse> applyUserFilter(List<LdapUserResponse> ldapResponses) {
        logger.trace("applying filter: {} or {}.", this.getListTypeString(), this.getUserFilter());
        return getUserFilter().filter(this,ldapResponses);
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
        return userFilter == null ? getListTypeString() == null ? "NoFilter" : getListTypeString().equals("all") ? "NoFilter" : "AnyDomain" : userFilter;
    }

    UserFilter getUserFilter() {
        return UserFilter.fromString(getUserFilterString());
    }

    boolean isACloudStackUser(final LdapUser ldapUser) {
        String username = ldapUser.getUsername();
        return isACloudStackUser(username);
    }

    boolean isACloudStackUser(final LdapUserResponse ldapUser) {
        logger.trace("checking response : {}", ldapUser.toString());
        String username = ldapUser.getUsername();
        return isACloudStackUser(username);
    }

    private boolean isACloudStackUser(String username) {
        boolean rc = false;
        final List<UserResponse> cloudstackUsers = getCloudstackUsers();
        if (CollectionUtils.isNotEmpty(cloudstackUsers)) {
            for (final UserResponse cloudstackUser : cloudstackUsers) {
                if (username.equals(cloudstackUser.getUsername())) {
                    logger.trace("Found user {} in CloudStack", cloudstackUser.getUsername());
                    rc = true;
                    break;
                } else {
                    logger.trace("ldap user {} does not match cloudstack user {}", username, cloudstackUser.getUsername());
                }
            }
        }
        return rc;
    }

    /**
     * typecheck for userfilter values and filter type dependend functionalities.
     * This could have been in two switch statements elsewhere in the code.
     * Arguably this is a cleaner solution.
     */
    enum UserFilter {
        NO_FILTER("NoFilter"){
            @Override public List<LdapUserResponse> filter(LdapListUsersCmd cmd, List<LdapUserResponse> input) {
                return cmd.filterNoFilter(input);
            }

            /**
             * in case of no filter we should find all users in the current domain for annotation.
             */
            @Override public ListResponse<UserResponse> getCloudstackUserList(LdapListUsersCmd cmd) {
                return cmd._queryService.searchForUsers(cmd.domainId,true);

            }
        },
        LOCAL_DOMAIN("LocalDomain"){
            @Override public List<LdapUserResponse> filter(LdapListUsersCmd cmd, List<LdapUserResponse> input) {
                return cmd.filterLocalDomain(input);
            }

            /**
             * if we are filtering for local domain, only get users for the current domain
             */
            @Override public ListResponse<UserResponse> getCloudstackUserList(LdapListUsersCmd cmd) {
                return cmd._queryService.searchForUsers(cmd.domainId,false);
            }
        },
        ANY_DOMAIN("AnyDomain"){
            @Override public List<LdapUserResponse> filter(LdapListUsersCmd cmd, List<LdapUserResponse> input) {
                return cmd.filterAnyDomain(input);
            }

            /*
             * if we are filtering for any domain, get recursive all users for the root domain
             */
            @Override public ListResponse<UserResponse> getCloudstackUserList(LdapListUsersCmd cmd) {
                return cmd._queryService.searchForUsers(CallContext.current().getCallingAccount().getDomainId(), true);
            }
        },
        POTENTIAL_IMPORT("PotentialImport"){
            @Override public List<LdapUserResponse> filter(LdapListUsersCmd cmd, List<LdapUserResponse> input) {
                return cmd.filterPotentialImport(input);
            }

            /**
             * if we are filtering for potential imports,
             *    we are only looking for users in the linked domains/accounts,
             *    which is only relevant if we ask ldap users for this domain.
             *    So we are asking for all users in the current domain as well
             */
            @Override public ListResponse<UserResponse> getCloudstackUserList(LdapListUsersCmd cmd) {
                return cmd._queryService.searchForUsers(cmd.domainId,false);
            }
        };

        private final String value;

        UserFilter(String val) {
            this.value = val;
        }

        public abstract List<LdapUserResponse> filter(LdapListUsersCmd cmd, List<LdapUserResponse> input);

        public abstract ListResponse<UserResponse> getCloudstackUserList(LdapListUsersCmd cmd);

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
        if(logger.isTraceEnabled()) {
            logger.trace("returning unfiltered list of ldap users");
        }
        annotateUserListWithSources(input);
        return input;
    }

    /**
     * filter the list of ldap users. no users visible to the caller should be in the returned list
     * @param input ldap response list of users
     * @return a list of ldap users not already in ACS
     */
    public List<LdapUserResponse> filterAnyDomain(List<LdapUserResponse> input) {
        if(logger.isTraceEnabled()) {
            logger.trace("filtering existing users");
        }
        final List<LdapUserResponse> ldapResponses = new ArrayList<>();
        for (final LdapUserResponse user : input) {
            if (isNotAlreadyImportedInTheCurrentDomain(user)) {
                ldapResponses.add(user);
            }
        }
        annotateUserListWithSources(ldapResponses);

        return ldapResponses;
    }

    /**
     * @return true unless the user is imported in the specified cloudstack domain from LDAP
     */
    private boolean isNotAlreadyImportedInTheCurrentDomain(LdapUserResponse user) {
        UserResponse cloudstackUser = getCloudstackUser(user);
        String domainId = getCurrentDomainId();

        return cloudstackUser == null /*doesn't exist in cloudstack*/
                || ! (
                        cloudstackUser.getUserSource().equalsIgnoreCase(User.Source.LDAP.toString())
                                && domainId.equals(cloudstackUser.getDomainId())); /* is from another source */
    }

    /**
     * filter the list of ldap users. no users visible to the caller already in the domain specified should be in the returned list
     * @param input ldap response list of users
     * @return a list of ldap users not already in ACS
     */
    public List<LdapUserResponse> filterLocalDomain(List<LdapUserResponse> input) {
        if(logger.isTraceEnabled()) {
            logger.trace("filtering local domain users");
        }
        final List<LdapUserResponse> ldapResponses = new ArrayList<>();
        String domainId = getCurrentDomainId();
        for (final LdapUserResponse user : input) {
            UserResponse cloudstackUser = getCloudstackUser(user);
            if (cloudstackUser == null /*doesn't exist in cloudstack*/
                    || !domainId.equals(cloudstackUser.getDomainId()) /* doesn't exist in this domain */
                    || !cloudstackUser.getUserSource().equalsIgnoreCase(User.Source.LDAP.toString()) /* is from another source */
            ) {
                ldapResponses.add(user);
            }
        }
        annotateUserListWithSources(ldapResponses);
        return ldapResponses;
    }

    private String getCurrentDomainId() {
        String domainId;
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
        if(logger.isTraceEnabled()) {
            logger.trace("should be filtering potential imports!!!");
        }
        // functional possibility do not add only users not yet in cloudstack but include users that would be moved if they are so in ldap?
        // This means if they are part of an Account linked to an LDAP Group/OU
        input.removeIf(ldapUser ->
                (
                        (isACloudStackUser(ldapUser))
                        && (getCloudstackUser(ldapUser).getUserSource().equalsIgnoreCase(User.Source.LDAP.toString()))
                )
        );
        annotateUserListWithSources(input);
        return input;
    }

    private void annotateUserListWithSources(List<LdapUserResponse> input) {
        for (final LdapUserResponse user : input) {
            annotateCloudstackSource(user);
        }
    }

    private void annotateCloudstackSource(LdapUserResponse user) {
        final UserResponse cloudstackUser = getCloudstackUser(user);
        if (cloudstackUser != null) {
            user.setUserSource(cloudstackUser.getUserSource());
        } else {
            user.setUserSource("");
        }
    }

    private UserResponse getCloudstackUser(LdapUserResponse user) {
        UserResponse returnObject = null;
        final List<UserResponse> cloudstackUsers = getCloudstackUsers();
        if (cloudstackUsers != null) {
            for (final UserResponse cloudstackUser : cloudstackUsers) {
                if (user.getUsername().equals(cloudstackUser.getUsername())) {
                    returnObject = cloudstackUser;
                    if (Objects.equals(returnObject.getDomainId(), this.getCurrentDomainId())) {
                        break;
                    }
                }
            }
        }
        return returnObject;
    }
}
