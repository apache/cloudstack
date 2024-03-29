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

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainService;
import com.cloud.user.User;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.LdapUserResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.apache.cloudstack.ldap.NoLdapUserMatchingQueryException;
import org.apache.cloudstack.query.QueryService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LdapListUsersCmdTest implements LdapConfigurationChanger {

    public static final String LOCAL_DOMAIN_ID = "12345678-90ab-cdef-fedc-ba0987654321";
    public static final String LOCAL_DOMAIN_NAME = "engineering";
    @Mock
    LdapManager ldapManager;
    @Mock
    QueryService queryService;
    @Mock
    DomainService domainService;

    LdapListUsersCmd ldapListUsersCmd;
    LdapListUsersCmd cmdSpy;

    Domain localDomain;

    MockedStatic<CallContext> callContextMocked;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        ldapListUsersCmd = new LdapListUsersCmd(ldapManager, queryService);
        cmdSpy = spy(ldapListUsersCmd);

        callContextMocked = Mockito.mockStatic(CallContext.class);
        CallContext callContextMock = Mockito.mock(CallContext.class);
        callContextMocked.when(CallContext::current).thenReturn(callContextMock);
        Account accountMock = Mockito.mock(Account.class);
        when(accountMock.getDomainId()).thenReturn(1l);
        when(callContextMock.getCallingAccount()).thenReturn(accountMock);

        ldapListUsersCmd._domainService = domainService;

// no need to        setHiddenField(ldapListUsersCmd, .... );
    }

    @After
    public void tearDown() throws Exception {
        callContextMocked.close();
    }

    /**
     * given: "We have an LdapManager, QueryService and LdapListUsersCmd"
     *  when: "Get entity owner id is called"
     *  then: "a 1 should be returned"
     *
     */
    @Test
    public void getEntityOwnerIdisOne() {
        long ownerId = ldapListUsersCmd.getEntityOwnerId();
        assertEquals(ownerId, 1);
    }

    /**
     * given: "We have an LdapManager with no users, QueryService and a LdapListUsersCmd"
     *  when: "LdapListUsersCmd is executed"
     *  then: "An array of size 0 is returned"
     *
     * @throws NoLdapUserMatchingQueryException
     */
    @Test
    public void successfulEmptyResponseFromExecute() throws NoLdapUserMatchingQueryException {
        Mockito.doThrow(new NoLdapUserMatchingQueryException("")).when(ldapManager).getUsers(null);
        ldapListUsersCmd.execute();
        assertEquals(0, ((ListResponse)ldapListUsersCmd.getResponseObject()).getResponses().size());
    }

    /**
     * given: "We have an LdapManager, one user, QueryService and a LdapListUsersCmd"
     *  when: "LdapListUsersCmd is executed"
     *  then: "a list of size not 0 is returned"
     */
    @Test
    public void successfulResponseFromExecute() throws NoLdapUserMatchingQueryException {
        mockACSUserSearch();

        mockResponseCreation();

        useSubdomain();

        ldapListUsersCmd.execute();

        verify(queryService, times(1)).searchForUsers(nullable(Long.class), nullable(Boolean.class));
        assertNotEquals(0, ((ListResponse)ldapListUsersCmd.getResponseObject()).getResponses().size());
    }

    /**
     * given: "We have an LdapManager, QueryService and a LdapListUsersCmd"
     *  when: "Get command name is called"
     *  then: "ldapuserresponse is returned"
     */
    @Test
    public void successfulReturnOfCommandName() {
        String commandName = ldapListUsersCmd.getCommandName();

        assertEquals("ldapuserresponse", commandName);
    }

    /**
     * given: "We have an LdapUser and a CloudStack user whose username match"
     *  when: "isACloudstackUser is executed"
     *  then: "The result is true"
     *
     * TODO: is this really the valid behaviour? shouldn't the user also be linked to ldap and not accidentally match?
     */
    @Test
    public void isACloudstackUser() {
        mockACSUserSearch();

        LdapUser ldapUser = new LdapUser("rmurphy", "rmurphy@cloudstack.org", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org", null, false, null);

        boolean result = ldapListUsersCmd.isACloudstackUser(ldapUser);

        assertTrue(result);
    }

    /**
     * given: "We have an LdapUser and not a matching CloudstackUser"
     *  when: "isACloudstackUser is executed"
     *  then: "The result is false"
     */
    @Test
    public void isNotACloudstackUser() {
        doReturn(new ListResponse<UserResponse>()).when(queryService).searchForUsers(nullable(Long.class), nullable(Boolean.class));

        LdapUser ldapUser = new LdapUser("rmurphy", "rmurphy@cloudstack.org", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org", null, false, null);

        boolean result = ldapListUsersCmd.isACloudstackUser(ldapUser);

        assertFalse(result);
    }

    /**
     * test whether a value other than 'any' for 'listtype' leads to a good 'userfilter' value
     */
    @Test
    public void getListtypeOther() {
        when(cmdSpy.getListTypeString()).thenReturn("otHer", "anY");

        String userfilter = cmdSpy.getUserFilterString();
        assertEquals("AnyDomain", userfilter);

        userfilter = cmdSpy.getUserFilterString();
        assertEquals("AnyDomain", userfilter);
    }

    /**
     * test whether a value of 'any' for 'listtype' leads to a good 'userfilter' value
     */
    @Test
    public void getListtypeAny() {
        when(cmdSpy.getListTypeString()).thenReturn("all");
        String userfilter = cmdSpy.getUserFilterString();
        assertEquals("NoFilter", userfilter);
    }

    /**
     * test whether values for 'userfilter' yield the right filter
     */
    @Test
    public void getUserFilter() throws NoSuchFieldException, IllegalAccessException {
        when(cmdSpy.getListTypeString()).thenReturn("otHer");
        LdapListUsersCmd.UserFilter userfilter = cmdSpy.getUserFilter();

        assertEquals(LdapListUsersCmd.UserFilter.ANY_DOMAIN, userfilter);

        when(cmdSpy.getListTypeString()).thenReturn("anY");
        userfilter = cmdSpy.getUserFilter();
        assertEquals(LdapListUsersCmd.UserFilter.ANY_DOMAIN, userfilter);
    }

    /**
     * test if the right exception is thrown on invalid input.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getInvalidUserFilterValues() throws NoSuchFieldException, IllegalAccessException {
        setHiddenField(ldapListUsersCmd, "userFilter", "false");
// unused output:       LdapListUsersCmd.UserFilter userfilter =
                ldapListUsersCmd.getUserFilter();
    }

    @Test
    public void getUserFilterValues() {
        assertEquals("PotentialImport", LdapListUsersCmd.UserFilter.POTENTIAL_IMPORT.toString());
        assertEquals(LdapListUsersCmd.UserFilter.POTENTIAL_IMPORT, LdapListUsersCmd.UserFilter.fromString("PotentialImport"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getInvalidUserFilterStringValue() {
        LdapListUsersCmd.UserFilter.fromString("PotentImport");
    }

    /**
     * apply no filter
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void applyNoFilter() throws NoSuchFieldException, IllegalAccessException, NoLdapUserMatchingQueryException {
        mockACSUserSearch();
        mockResponseCreation();

        useSubdomain();

        setHiddenField(ldapListUsersCmd, "userFilter", "NoFilter");
        ldapListUsersCmd.execute();

        assertEquals(3, ((ListResponse)ldapListUsersCmd.getResponseObject()).getResponses().size());
    }

    /**
     * filter all acs users
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void applyAnyDomain() throws NoSuchFieldException, IllegalAccessException, NoLdapUserMatchingQueryException {
        mockACSUserSearch();
        mockResponseCreation();

        useSubdomain();

        setHiddenField(ldapListUsersCmd, "userFilter", "AnyDomain");
        setHiddenField(ldapListUsersCmd, "domainId", 2l /* not root */);
        ldapListUsersCmd.execute();

        // 'rmurphy' annotated with native
        // 'bob' still in
        // 'abhi' is filtered out
        List<ResponseObject> responses = ((ListResponse)ldapListUsersCmd.getResponseObject()).getResponses();
        assertEquals(2, responses.size());
        for(ResponseObject response : responses) {
            if(!(response instanceof LdapUserResponse)) {
                fail("unexpected return-type from API backend method");
            } else {
                LdapUserResponse userResponse = (LdapUserResponse)response;
                // further validate this user
                if ("rmurphy".equals(userResponse.getUsername()) &&
                        ! User.Source.NATIVE.toString().equalsIgnoreCase(userResponse.getUserSource())) {
                    fail("expected murphy from ldap");
                }
                if ("bob".equals(userResponse.getUsername()) &&
                        ! "".equals(userResponse.getUserSource())) {
                    fail("expected bob from without usersource");
                }
            }
        }
    }

    /**
     * filter out acs users for the requested domain
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void applyLocalDomainForASubDomain() throws NoSuchFieldException, IllegalAccessException, NoLdapUserMatchingQueryException {
        mockACSUserSearch();
        mockResponseCreation();

        setHiddenField(ldapListUsersCmd, "userFilter", "LocalDomain");
        setHiddenField(ldapListUsersCmd, "domainId", 2l /* not root */);

        localDomain = useSubdomain();

        ldapListUsersCmd.execute();

        // 'rmurphy' filtered out 'bob' still in
        assertEquals(2, ((ListResponse)ldapListUsersCmd.getResponseObject()).getResponses().size());
        // todo: assert user sources
    }

    /**
     * filter out acs users for the default domain
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void applyLocalDomainForTheCallersDomain() throws NoSuchFieldException, IllegalAccessException, NoLdapUserMatchingQueryException {
        mockACSUserSearch();
        mockResponseCreation();

        setHiddenField(ldapListUsersCmd, "userFilter", "LocalDomain");

        AccountVO account = new AccountVO();
        setHiddenField(account, "accountName", "admin");
        setHiddenField(account, "domainId", 1l);
        final CallContext callContext = CallContext.current();
        setHiddenField(callContext, "account", account);
        DomainVO domainVO = useDomain("ROOT", 1l);
        localDomain = domainVO;

        ldapListUsersCmd.execute();

        // 'rmurphy' filtered out 'bob' still in
        assertEquals(2, ((ListResponse)ldapListUsersCmd.getResponseObject()).getResponses().size());
        // todo: assert usersources
    }

    /**
     * todo generate an extensive configuration and check with an extensive user list
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void applyPotentialImport() throws NoSuchFieldException, IllegalAccessException, NoLdapUserMatchingQueryException {
        mockACSUserSearch();
        mockResponseCreation();

        useSubdomain();

        setHiddenField(ldapListUsersCmd, "userFilter", "PotentialImport");
        ldapListUsersCmd.execute();

        assertEquals(2, ((ListResponse)ldapListUsersCmd.getResponseObject()).getResponses().size());
    }

    /**
     * unknown filter
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test(expected = IllegalArgumentException.class)
    public void applyUnknownFilter() throws NoSuchFieldException, IllegalAccessException {
        setHiddenField(ldapListUsersCmd, "userFilter", "UnknownFilter");
        ldapListUsersCmd.execute();
    }

    /**
     * make sure there are no unimplemented filters
     *
     * This was created to deal with the possible {code}NoSuchMethodException{code} that won't be dealt with in regular coverage
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void applyUnimplementedFilter() throws NoSuchFieldException, IllegalAccessException {
        useSubdomain();
        for (LdapListUsersCmd.UserFilter UNIMPLEMENTED_FILTER : LdapListUsersCmd.UserFilter.values()) {
            setHiddenField(ldapListUsersCmd, "userFilter", UNIMPLEMENTED_FILTER.toString());
            ldapListUsersCmd.getUserFilter().filter(ldapListUsersCmd,new ArrayList<LdapUserResponse>());
        }
    }

    // helper methods //
    ////////////////////
    private DomainVO useSubdomain() {
        DomainVO domainVO = useDomain(LOCAL_DOMAIN_NAME, 2l);
        return domainVO;
    }

    private DomainVO useDomain(String domainName, long domainId) {
        DomainVO domainVO = new DomainVO();
        domainVO.setName(domainName);
        domainVO.setId(domainId);
        domainVO.setUuid(LOCAL_DOMAIN_ID);
        when(domainService.getDomain(nullable(Long.class))).thenReturn(domainVO);
        return domainVO;
    }

    private void mockACSUserSearch() {
        UserResponse rmurphy = createMockUserResponse("rmurphy", User.Source.NATIVE);
        UserResponse rohit = createMockUserResponse("rohit", User.Source.SAML2);
        UserResponse abhi = createMockUserResponse("abhi", User.Source.LDAP);

        ArrayList<UserResponse> responses = new ArrayList<>();
        responses.add(rmurphy);
        responses.add(rohit);
        responses.add(abhi);

        ListResponse<UserResponse> queryServiceResponse = new ListResponse<>();
        queryServiceResponse.setResponses(responses);

        doReturn(queryServiceResponse).when(queryService).searchForUsers(nullable(Long.class), nullable(Boolean.class));
    }

    private UserResponse createMockUserResponse(String uid, User.Source source) {
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername(uid);
        userResponse.setUserSource(source);

        // for now:
        userResponse.setDomainId(LOCAL_DOMAIN_ID);
        userResponse.setDomainName(LOCAL_DOMAIN_NAME);

        return userResponse;
    }

    private void mockResponseCreation() throws NoLdapUserMatchingQueryException {
        List<LdapUser> users = new ArrayList();
        LdapUser murphy = new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org", "mythical", false, null);
        LdapUser bob = new LdapUser("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", LOCAL_DOMAIN_NAME, false, null);
        LdapUser abhi = new LdapUser("abhi", "abhi@test.com", "Abhi", "YoungOrOld", "cn=abhi,ou=engineering,dc=cloudstack,dc=org", LOCAL_DOMAIN_NAME, false, null);
        users.add(murphy);
        users.add(bob);
        users.add(abhi);

        doReturn(users).when(ldapManager).getUsers(any());

        LdapUserResponse response = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org", null);
        doReturn(response).when(ldapManager).createLdapUserResponse(murphy);
        LdapUserResponse bobResponse = new LdapUserResponse("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", LOCAL_DOMAIN_NAME);
        doReturn(bobResponse).when(ldapManager).createLdapUserResponse(bob);
        LdapUserResponse abhiResponse = new LdapUserResponse("abhi", "abhi@test.com", "Abhi", "YoungOrOld", "cn=abhi,ou=engineering,dc=cloudstack,dc=org", LOCAL_DOMAIN_NAME);
        doReturn(abhiResponse).when(ldapManager).createLdapUserResponse(abhi);
    }
}
