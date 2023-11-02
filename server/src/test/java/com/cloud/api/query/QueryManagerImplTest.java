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

import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.event.dao.EventJoinDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.Network;
import com.cloud.network.VNF;
import com.cloud.network.dao.NetworkVO;
import com.cloud.server.ResourceTag;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.command.user.event.ListEventsCmd;
import org.apache.cloudstack.api.command.user.resource.ListDetailOptionsCmd;
import org.apache.cloudstack.api.response.DetailOptionsResponse;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QueryManagerImplTest {
    public static final long USER_ID = 1;
    public static final long ACCOUNT_ID = 1;

    @Spy
    @InjectMocks
    private QueryManagerImpl queryManagerImplSpy = new QueryManagerImpl();

    @Mock
    EntityManager entityManager;

    @Mock
    AccountManager accountManager;

    @Mock
    EventJoinDao eventJoinDao;

    @Mock
    Account accountMock;

    @Mock
    TemplateJoinDao templateJoinDaoMock;

    @Mock
    SearchCriteria searchCriteriaMock;

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
        Mockito.when(accountManager.isRootAdmin(account.getId())).thenReturn(false);
        final SearchBuilder<EventJoinVO> searchBuilder = Mockito.mock(SearchBuilder.class);
        final SearchCriteria<EventJoinVO> searchCriteria = Mockito.mock(SearchCriteria.class);
        final EventJoinVO eventJoinVO = Mockito.mock(EventJoinVO.class);
        when(searchBuilder.entity()).thenReturn(eventJoinVO);
        when(searchBuilder.create()).thenReturn(searchCriteria);
        Mockito.when(eventJoinDao.createSearchBuilder()).thenReturn(searchBuilder);
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
        Mockito.doNothing().when(accountManager).checkAccess(account, SecurityChecker.AccessType.ListEntry, true, network);
        Mockito.when(eventJoinDao.searchAndCount(Mockito.any(), Mockito.any(Filter.class))).thenReturn(pair);
        List<EventResponse> respList = new ArrayList<EventResponse>();
        for (EventJoinVO vt : events) {
            respList.add(eventJoinDao.newEventResponse(vt));
        }
        try (MockedStatic<ViewResponseHelper> ignored = Mockito.mockStatic(ViewResponseHelper.class)) {
            Mockito.when(ViewResponseHelper.createEventResponse(Mockito.any())).thenReturn(respList);
            ListResponse<EventResponse> result = queryManager.searchForEvents(cmd);
            Assert.assertEquals((int) result.getCount(), events.size());
        }
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
        Mockito.doThrow(new PermissionDeniedException("Denied")).when(accountManager).checkAccess(account, SecurityChecker.AccessType.ListEntry, false, network);
        queryManager.searchForEvents(cmd);
    }

    @Test
    public void listVnfDetailOptionsCmd() {
        ListDetailOptionsCmd cmd = Mockito.mock(ListDetailOptionsCmd.class);
        when(cmd.getResourceType()).thenReturn(ResourceTag.ResourceObjectType.VnfTemplate);

        DetailOptionsResponse response = queryManager.listDetailOptions(cmd);
        Map<String, List<String>> options = response.getDetails();

        int expectedLength = VNF.AccessDetail.values().length + VNF.VnfDetail.values().length;
        Assert.assertEquals(expectedLength, options.size());
        Set<String> keys = options.keySet();
        for (VNF.AccessDetail detail : VNF.AccessDetail.values()) {
            Assert.assertTrue(keys.contains(detail.name().toLowerCase()));
        }
        for (VNF.VnfDetail detail : VNF.VnfDetail.values()) {
            Assert.assertTrue(keys.contains(detail.name().toLowerCase()));
        }
        List<String> expectedAccessMethods = Arrays.stream(VNF.AccessMethod.values()).map(method -> method.toString()).sorted().collect(Collectors.toList());
        Assert.assertEquals(expectedAccessMethods, options.get(VNF.AccessDetail.ACCESS_METHODS.name().toLowerCase()));

    }

    @Test
    public void applyPublicTemplateRestrictionsTestDoesNotApplyRestrictionsWhenCallerIsRootAdmin() {
        Mockito.when(accountMock.getType()).thenReturn(Account.Type.ADMIN);

        queryManagerImplSpy.applyPublicTemplateSharingRestrictions(searchCriteriaMock, accountMock);

        Mockito.verify(searchCriteriaMock, Mockito.never()).addAnd(Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    @Test
    public void applyPublicTemplateRestrictionsTestAppliesRestrictionsWhenCallerIsNotRootAdmin() {
        long callerDomainId = 1L;
        long sharableDomainId = 2L;
        long unsharableDomainId = 3L;

        Mockito.when(accountMock.getType()).thenReturn(Account.Type.NORMAL);

        Mockito.when(accountMock.getDomainId()).thenReturn(callerDomainId);
        TemplateJoinVO templateMock1 = Mockito.mock(TemplateJoinVO.class);
        Mockito.when(templateMock1.getDomainId()).thenReturn(callerDomainId);
        Mockito.lenient().doReturn(false).when(queryManagerImplSpy).checkIfDomainSharesTemplates(callerDomainId);

        TemplateJoinVO templateMock2 = Mockito.mock(TemplateJoinVO.class);
        Mockito.when(templateMock2.getDomainId()).thenReturn(sharableDomainId);
        Mockito.doReturn(true).when(queryManagerImplSpy).checkIfDomainSharesTemplates(sharableDomainId);

        TemplateJoinVO templateMock3 = Mockito.mock(TemplateJoinVO.class);
        Mockito.when(templateMock3.getDomainId()).thenReturn(unsharableDomainId);
        Mockito.doReturn(false).when(queryManagerImplSpy).checkIfDomainSharesTemplates(unsharableDomainId);

        List<TemplateJoinVO> publicTemplates = List.of(templateMock1, templateMock2, templateMock3);
        Mockito.when(templateJoinDaoMock.listPublicTemplates()).thenReturn(publicTemplates);

        queryManagerImplSpy.applyPublicTemplateSharingRestrictions(searchCriteriaMock, accountMock);

        Mockito.verify(searchCriteriaMock).addAnd("domainId", SearchCriteria.Op.NOTIN, unsharableDomainId);
    }

    @Test
    public void addDomainIdToSetIfDomainDoesNotShareTemplatesTestDoesNotAddWhenCallerBelongsToDomain() {
        long domainId = 1L;
        Set<Long> set = new HashSet<>();

        Mockito.when(accountMock.getDomainId()).thenReturn(domainId);

        queryManagerImplSpy.addDomainIdToSetIfDomainDoesNotShareTemplates(domainId, accountMock, set);

        Assert.assertEquals(0, set.size());
    }

    @Test
    public void addDomainIdToSetIfDomainDoesNotShareTemplatesTestAddsWhenDomainDoesNotShareTemplates() {
        long domainId = 1L;
        Set<Long> set = new HashSet<>();

        Mockito.when(accountMock.getDomainId()).thenReturn(2L);
        Mockito.doReturn(false).when(queryManagerImplSpy).checkIfDomainSharesTemplates(domainId);

        queryManagerImplSpy.addDomainIdToSetIfDomainDoesNotShareTemplates(domainId, accountMock, set);

        Assert.assertTrue(set.contains(domainId));
    }
}
