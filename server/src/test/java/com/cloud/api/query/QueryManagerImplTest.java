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

package com.cloud.api.query;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.cloud.projects.ProjectManager;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.command.user.event.ListEventsCmd;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.event.dao.EventJoinDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkVO;
import com.cloud.projects.Project;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VirtualMachine;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ComponentContext.class, ViewResponseHelper.class})
public class QueryManagerImplTest {
    public static final long USER_ID = 1;
    public static final long ACCOUNT_ID = 1;
    private long projId = 1l;
    private String accountName = "name";
    private long domId = 1l;

    @Spy
    @InjectMocks
    private QueryManagerImpl queryManagerImplSpy = new QueryManagerImpl();
    @Mock
    EntityManager entityManager;
    @Mock
    AccountManager accountManagerMock;
    @Mock
    ProjectManager projectManagerMock;
    @Mock
    EventJoinDao eventJoinDao;
    @Mock
    Account accountMock;
    @Mock
    Project projectMock;
    private AccountVO account;
    private UserVO user;

    @Before
    public void setUp() throws Exception {
        setupCommonMocks();
    }

    @InjectMocks
    private QueryManagerImpl queryManager = new QueryManagerImpl();

    private void setupCommonMocks() {
        account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(ACCOUNT_ID);
        user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        Mockito.when(accountManagerMock.isRootAdmin(account.getId())).thenReturn(false);
        Mockito.doNothing().when(accountManagerMock).buildACLSearchParameters(Mockito.any(Account.class), Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyList(),
                Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.doNothing().when(accountManagerMock).buildACLSearchBuilder(Mockito.any(SearchBuilder.class), Mockito.anyLong(), Mockito.anyBoolean(), Mockito.anyList(),
                Mockito.any(Project.ListProjectResourcesCriteria.class));
        Mockito.doNothing().when(accountManagerMock).buildACLViewSearchCriteria(Mockito.any(), Mockito.anyLong(), Mockito.anyBoolean(), Mockito.anyList(),
                Mockito.any(Project.ListProjectResourcesCriteria.class));
        final SearchBuilder<EventJoinVO> searchBuilder = Mockito.mock(SearchBuilder.class);
        final SearchCriteria<EventJoinVO> searchCriteria = Mockito.mock(SearchCriteria.class);
        final EventJoinVO eventJoinVO = Mockito.mock(EventJoinVO.class);
        when(searchBuilder.entity()).thenReturn(eventJoinVO);
        when(searchBuilder.create()).thenReturn(searchCriteria);
        Mockito.when(eventJoinDao.createSearchBuilder()).thenReturn(searchBuilder);
        Mockito.when(eventJoinDao.createSearchCriteria()).thenReturn(searchCriteria);
    }

    private ListEventsCmd setupMockListEventsCmd() {
        ListEventsCmd cmd = Mockito.mock(ListEventsCmd.class);
        Mockito.when(cmd.getEntryTime()).thenReturn(null);
        Mockito.when(cmd.listAll()).thenReturn(true);
        return cmd;
    }

    @Test
    public void searchForEventsSuccess() {
        ListEventsCmd cmd = setupMockListEventsCmd();
        String uuid = UUID.randomUUID().toString();
        Mockito.when(cmd.getResourceId()).thenReturn(uuid);
        Mockito.when(cmd.getResourceType()).thenReturn(ApiCommandResourceType.Network.toString());
        List<EventJoinVO> events = new ArrayList<>();
        events.add(Mockito.mock(EventJoinVO.class));
        events.add(Mockito.mock(EventJoinVO.class));
        events.add(Mockito.mock(EventJoinVO.class));
        Pair<List<EventJoinVO>, Integer> pair = new Pair<>(events, events.size());
        NetworkVO network = Mockito.mock(NetworkVO.class);
        Mockito.when(network.getId()).thenReturn(1L);
        Mockito.when(network.getAccountId()).thenReturn(account.getId());
        Mockito.when(entityManager.findByUuidIncludingRemoved(Network.class, uuid)).thenReturn(network);
        Mockito.doNothing().when(accountManagerMock).checkAccess(account, SecurityChecker.AccessType.ListEntry, true, network);
        Mockito.when(eventJoinDao.searchAndCount(Mockito.any(), Mockito.any(Filter.class))).thenReturn(pair);
        List<EventResponse> respList = new ArrayList<EventResponse>();
        for (EventJoinVO vt : events) {
            respList.add(eventJoinDao.newEventResponse(vt));
        }
        PowerMockito.mockStatic(ViewResponseHelper.class);
        Mockito.when(ViewResponseHelper.createEventResponse(Mockito.any())).thenReturn(respList);
        ListResponse<EventResponse> result = queryManager.searchForEvents(cmd);
        assertEquals((int) result.getCount(), events.size());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void searchForEventsFailResourceTypeNull() {
        ListEventsCmd cmd  = setupMockListEventsCmd();
        String uuid = UUID.randomUUID().toString();
        Mockito.when(cmd.getResourceId()).thenReturn(uuid);
        Mockito.when(cmd.getResourceType()).thenReturn(null);
        queryManager.searchForEvents(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void searchForEventsFailResourceTypeInvalid() {
        ListEventsCmd cmd  = setupMockListEventsCmd();
        Mockito.when(cmd.getResourceType()).thenReturn("Some");
        queryManager.searchForEvents(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void searchForEventsFailResourceIdInvalid() {
        ListEventsCmd cmd  = setupMockListEventsCmd();
        Mockito.when(cmd.getResourceId()).thenReturn("random");
        Mockito.when(cmd.getResourceType()).thenReturn(ApiCommandResourceType.VirtualMachine.toString());
        queryManager.searchForEvents(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void searchForEventsFailResourceNotFound() {
        ListEventsCmd cmd  = setupMockListEventsCmd();
        String uuid = UUID.randomUUID().toString();
        Mockito.when(cmd.getResourceId()).thenReturn(uuid);
        Mockito.when(cmd.getResourceType()).thenReturn(ApiCommandResourceType.VirtualMachine.toString());
        Mockito.when(entityManager.findByUuidIncludingRemoved(VirtualMachine.class, uuid)).thenReturn(null);
        queryManager.searchForEvents(cmd);
    }

    @Test(expected = PermissionDeniedException.class)
    public void searchForEventsFailPermissionDenied() {
        ListEventsCmd cmd  = setupMockListEventsCmd();
        String uuid = UUID.randomUUID().toString();
        Mockito.when(cmd.getResourceId()).thenReturn(uuid);
        Mockito.when(cmd.getResourceType()).thenReturn(ApiCommandResourceType.Network.toString());
        NetworkVO network = Mockito.mock(NetworkVO.class);
        Mockito.when(network.getId()).thenReturn(1L);
        Mockito.when(network.getAccountId()).thenReturn(2L);
        Mockito.when(entityManager.findByUuidIncludingRemoved(Network.class, uuid)).thenReturn(network);
        Mockito.doThrow(new PermissionDeniedException("Denied")).when(accountManagerMock).checkAccess(account, SecurityChecker.AccessType.ListEntry, false, network);
        queryManager.searchForEvents(cmd);
    }

    @Test
    public void getCallerAccordingToProjectIdAndAccountNameAndDomainIdTestReturnOriginalCaller() {
        Account result = queryManagerImplSpy.getCallerAccordingToProjectIdAndAccountNameAndDomainId(accountMock, null, null, null);
        Mockito.verify(queryManagerImplSpy, Mockito.times(0)).getCallerAccordingToAccountNameAndDomainId(Mockito.any(Account.class), Mockito.anyString(), Mockito.anyLong());
        Mockito.verify(queryManagerImplSpy, Mockito.times(0)).getCallerAccordingToProjectId(Mockito.any(Account.class), Mockito.anyLong());
        assertEquals(result, accountMock);
    }

    @Test
    public void getCallerAccordingToProjectIdAndAccountNameAndDomainIdTestProjectIdNull() {
        Account anotherAccount = Mockito.mock(Account.class);
        Mockito.doReturn(anotherAccount).when(queryManagerImplSpy).getCallerAccordingToAccountNameAndDomainId(Mockito.any(Account.class), Mockito.anyString(), Mockito.anyLong());
        Account result = queryManagerImplSpy.getCallerAccordingToProjectIdAndAccountNameAndDomainId(accountMock, null, accountName, domId);
        Mockito.verify(queryManagerImplSpy, Mockito.times(1)).getCallerAccordingToAccountNameAndDomainId(Mockito.any(Account.class), Mockito.anyString(), Mockito.anyLong());
        Mockito.verify(queryManagerImplSpy, Mockito.times(0)).getCallerAccordingToProjectId(Mockito.any(Account.class), Mockito.anyLong());
        assertEquals(result, anotherAccount);
    }

    @Test
    public void getCallerAccordingToProjectIdAndAccountNameAndDomainIdTestAccountNameAndDomainIdNull() {
        Account projectAccount = Mockito.mock(Account.class);
        Mockito.doReturn(projectAccount).when(queryManagerImplSpy).getCallerAccordingToProjectId(Mockito.any(Account.class), Mockito.anyLong());
        Account result = queryManagerImplSpy.getCallerAccordingToProjectIdAndAccountNameAndDomainId(accountMock, projId, null, null);
        Mockito.verify(queryManagerImplSpy, Mockito.times(0)).getCallerAccordingToAccountNameAndDomainId(Mockito.any(Account.class), Mockito.anyString(), Mockito.anyLong());
        Mockito.verify(queryManagerImplSpy, Mockito.times(1)).getCallerAccordingToProjectId(Mockito.any(Account.class), Mockito.anyLong());
        assertEquals(result, projectAccount);
    }

    @Test
    public void getCallerAccordingToProjectIdAndAccountNameAndDomainIdTestAccountNameNull() {
        Account projectAccount = Mockito.mock(Account.class);
        Mockito.doReturn(projectAccount).when(queryManagerImplSpy).getCallerAccordingToProjectId(Mockito.any(Account.class), Mockito.anyLong());
        Account result = queryManagerImplSpy.getCallerAccordingToProjectIdAndAccountNameAndDomainId(accountMock, projId, null, domId);
        Mockito.verify(queryManagerImplSpy, Mockito.times(0)).getCallerAccordingToAccountNameAndDomainId(Mockito.any(Account.class), Mockito.anyString(), Mockito.anyLong());
        Mockito.verify(queryManagerImplSpy, Mockito.times(1)).getCallerAccordingToProjectId(Mockito.any(Account.class), Mockito.anyLong());
        assertEquals(result, projectAccount);
    }

    @Test
    public void getCallerAccordingToProjectIdAndAccountNameAndDomainIdTestDomainIdNull() {
        Account projectAccount = Mockito.mock(Account.class);
        Mockito.doReturn(projectAccount).when(queryManagerImplSpy).getCallerAccordingToProjectId(Mockito.any(Account.class), Mockito.anyLong());
        Account result = queryManagerImplSpy.getCallerAccordingToProjectIdAndAccountNameAndDomainId(accountMock, projId, accountName, null);
        Mockito.verify(queryManagerImplSpy, Mockito.times(0)).getCallerAccordingToAccountNameAndDomainId(Mockito.any(Account.class), Mockito.anyString(), Mockito.anyLong());
        Mockito.verify(queryManagerImplSpy, Mockito.times(1)).getCallerAccordingToProjectId(Mockito.any(Account.class), Mockito.anyLong());
        assertEquals(result, projectAccount);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getCallerAccordingToAccountNameAndDomainIdTestThrowInvalidParameterValueException() {
        Mockito.doReturn(null).when(accountManagerMock).getActiveAccountByName(Mockito.anyString(), Mockito.anyLong());
        queryManagerImplSpy.getCallerAccordingToAccountNameAndDomainId(accountMock, accountName, domId);
    }

    @Test(expected = PermissionDeniedException.class)
    public void getCallerAccordingToAccountNameAndDomainIdTestThrowPermissionDeniedException() {
        Account anotherAccount = Mockito.mock(Account.class);
        Mockito.doReturn(anotherAccount).when(accountManagerMock).getActiveAccountByName(Mockito.anyString(), Mockito.anyLong());
        Mockito.doThrow(PermissionDeniedException.class).when(accountManagerMock).checkAccess(accountMock, null, true, anotherAccount);
        queryManagerImplSpy.getCallerAccordingToAccountNameAndDomainId(accountMock, accountName, domId);
    }

    public void getCallerAccordingToAccountNameAndDomainIdTestReturnAccount() {
        Account anotherAccount = Mockito.mock(Account.class);
        Mockito.doReturn(anotherAccount).when(accountManagerMock).getActiveAccountByName(Mockito.anyString(), Mockito.anyLong());
        Account result = queryManagerImplSpy.getCallerAccordingToAccountNameAndDomainId(accountMock, accountName, domId);
        assertEquals(result, anotherAccount);
    }

    @Test
    public void getCallerAccordingToProjectIdTestReturnProjectAccount() {
        Account anotherAccount = Mockito.mock(Account.class);
        Mockito.doReturn(projectMock).when(projectManagerMock).getProject(projId);
        Mockito.doReturn(anotherAccount).when(accountManagerMock).getActiveAccountById(Mockito.anyLong());
        Mockito.doReturn(true).when(projectManagerMock).canAccessProjectAccount(Mockito.any(Account.class), Mockito.anyLong());
        Account result = queryManagerImplSpy.getCallerAccordingToProjectId(accountMock, projId);
        assertEquals(result, anotherAccount);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getCallerAccordingToProjectIdTestProjectNotFound() {
        Mockito.doReturn(null).when(projectManagerMock).getProject(projId);
        queryManagerImplSpy.getCallerAccordingToProjectId(accountMock, projId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getCallerAccordingToProjectIdTestAccountNotFound() {
        Mockito.doReturn(projectMock).when(projectManagerMock).getProject(projId);
        Mockito.doReturn(null).when(accountManagerMock).getActiveAccountById(Mockito.anyLong());
        queryManagerImplSpy.getCallerAccordingToProjectId(accountMock, projId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getCallerAccordingToProjectIdTestAccountCanAccessProject() {
        Account anotherAccount = Mockito.mock(Account.class);
        Mockito.doReturn(projectMock).when(projectManagerMock).getProject(projId);
        Mockito.doReturn(anotherAccount).when(accountManagerMock).getActiveAccountById(Mockito.anyLong());
        Mockito.doReturn(false).when(projectManagerMock).canAccessProjectAccount(Mockito.any(Account.class), Mockito.anyLong());
        queryManagerImplSpy.getCallerAccordingToProjectId(accountMock, projId);
    }

    @Test(expected = PermissionDeniedException.class)
    public void getCallerAccordingToProjectIdTestCheckAccess() {
        Account anotherAccount = Mockito.mock(Account.class);
        Mockito.doReturn(projectMock).when(projectManagerMock).getProject(projId);
        Mockito.doReturn(anotherAccount).when(accountManagerMock).getActiveAccountById(Mockito.anyLong());
        Mockito.doReturn(true).when(projectManagerMock).canAccessProjectAccount(Mockito.any(Account.class), Mockito.anyLong());
        Mockito.doThrow(PermissionDeniedException.class).when(accountManagerMock).checkAccess(accountMock, null, true, anotherAccount);
        queryManagerImplSpy.getCallerAccordingToProjectId(accountMock, projId);
    }
}