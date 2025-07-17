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
package org.apache.cloudstack.gui.theme;

import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.user.gui.theme.ListGuiThemesCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.gui.theme.dao.GuiThemeJoinDaoImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;


@RunWith(MockitoJUnitRunner.class)
public class GuiThemeServiceImplTest {

    @Mock
    GuiThemeJoinDaoImpl guiThemeJoinDaoMock;

    @Mock
    GuiThemeJoinVO guiThemeJoinVOMock;

    @Mock
    EntityManager entityManagerMock;

    @Mock
    Object objectMock;

    @Mock
    ListGuiThemesCmd listGuiThemesCmdMock;

    @Spy
    @InjectMocks
    GuiThemeServiceImpl guiThemeServiceSpy = new GuiThemeServiceImpl();

    private static final String COMMON_NAME = "*acme.com,acm2.com";

    private static final String DOMAIN_IDS = "1,2,3";

    private static final String ACCOUNT_IDS = "4,5,6";

    private static final String BLANK_STRING = "";

    @Test
    public void listGuiThemesTestShouldIgnoreParametersWhenItIsCalledUnauthenticated() {
        Pair<List<GuiThemeJoinVO>, Integer> emptyPair = new Pair<>(new ArrayList<>(), 0);

        Long accountId = Account.ACCOUNT_ID_SYSTEM;

        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);
            Mockito.doReturn(accountId).when(callContextMock).getCallingAccountId();
            Mockito.doReturn(emptyPair).when(guiThemeServiceSpy).listGuiThemesWithNoAuthentication(Mockito.nullable(ListGuiThemesCmd.class));
            Mockito.when(listGuiThemesCmdMock.getListOnlyDefaultTheme()).thenReturn(false);
            guiThemeServiceSpy.listGuiThemes(listGuiThemesCmdMock);
            Mockito.verify(guiThemeServiceSpy, Mockito.times(1)).listGuiThemesWithNoAuthentication(Mockito.nullable(ListGuiThemesCmd.class));
        }
    }

    @Test
    public void listGuiThemesTestShouldCallNormalFlowWhenAuthenticated() {
        Pair<List<GuiThemeVO>, Integer> emptyPair = new Pair<>(new ArrayList<>(), 0);

        Long accountId = 3L;

        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);
            Mockito.doReturn(accountId).when(callContextMock).getCallingAccountId();
            Mockito.doReturn(emptyPair).when(guiThemeServiceSpy).listGuiThemesInternal(Mockito.nullable(ListGuiThemesCmd.class));
            Mockito.when(listGuiThemesCmdMock.getListOnlyDefaultTheme()).thenReturn(false);
            guiThemeServiceSpy.listGuiThemes(listGuiThemesCmdMock);
            Mockito.verify(guiThemeServiceSpy, Mockito.times(1)).listGuiThemesInternal(Mockito.nullable(ListGuiThemesCmd.class));
        }
    }

    @Test
    public void listGuiThemesTestListOnlyDefaultThemesShouldCallFindDefaultTheme() {
        Mockito.when(listGuiThemesCmdMock.getListOnlyDefaultTheme()).thenReturn(true);
        guiThemeServiceSpy.listGuiThemes(listGuiThemesCmdMock);
        Mockito.verify(guiThemeJoinDaoMock, Mockito.times(1)).findDefaultTheme();
    }

    @Test
    public void shouldSetGuiThemeToPrivateTestDomainIdsAndAccountIdsAreBlankShouldReturnFalse() {
        boolean result = guiThemeServiceSpy.shouldSetGuiThemeToPrivate(BLANK_STRING, BLANK_STRING);

        Assert.assertFalse(result);
    }

    @Test
    public void shouldSetGuiThemeToPrivateTestDomainIdsNotBlankAndAccountIdsIsBlankShouldReturnTrue() {
        boolean result = guiThemeServiceSpy.shouldSetGuiThemeToPrivate(DOMAIN_IDS, BLANK_STRING);

        Assert.assertTrue(result);
    }

    @Test
    public void shouldSetGuiThemeToPrivateTestDomainIdsIsBlankAndAccountIdsIsNotBlankShouldReturnTrue() {
        boolean result = guiThemeServiceSpy.shouldSetGuiThemeToPrivate(BLANK_STRING, ACCOUNT_IDS);

        Assert.assertTrue(result);
    }

    @Test
    public void shouldSetGuiThemeToPrivateTestDomainIdsAndAccountIdsAreNotBlankShouldReturnTrue() {
        boolean result = guiThemeServiceSpy.shouldSetGuiThemeToPrivate(DOMAIN_IDS, ACCOUNT_IDS);

        Assert.assertTrue(result);
    }

    @Test
    public void validateObjectUuidsTestProvidedUuidsIsNullShouldNotThrowCloudRuntimeException() {
        guiThemeServiceSpy.validateObjectUuids(null, null);
    }

    @Test
    public void validateObjectUuidsTestProvidedUuidsIsBlankShouldNotThrowCloudRuntimeException() {
        guiThemeServiceSpy.validateObjectUuids(BLANK_STRING, null);
    }

    @Test
    public void validateObjectUuidsTestProvidedUuidsIsBlankAndCommaSeparatedShouldNotThrowCloudRuntimeException() {
        String blankCommaSeparatedString = ",,,";
        guiThemeServiceSpy.validateObjectUuids(blankCommaSeparatedString, null);
    }

    @Test
    public void validateObjectUuidsTestProvidedUuidIsValidShouldNotThrowCloudRuntimeException() {
        String validUuid = "4";

        Mockito.when(entityManagerMock.findByUuid(Mockito.any(Class.class), Mockito.anyString())).thenReturn(objectMock);
        guiThemeServiceSpy.validateObjectUuids(validUuid, Account.class);
    }

    @Test
    public void validateObjectUuidsTestProvidedUuidsAreValidShouldNotThrowCloudRuntimeException() {
        Mockito.when(entityManagerMock.findByUuid(Mockito.any(Class.class), Mockito.anyString())).thenReturn(objectMock);
        guiThemeServiceSpy.validateObjectUuids(ACCOUNT_IDS, Account.class);
    }

    @Test(expected = CloudRuntimeException.class)
    public void validateObjectUuidsTestProvidedUuidsAreNotValidShouldThrowCloudRuntimeException() {
        Mockito.when(entityManagerMock.findByUuid(Mockito.any(Class.class), Mockito.anyString())).thenReturn(null);
        guiThemeServiceSpy.validateObjectUuids(ACCOUNT_IDS, Account.class);
    }

    @Test
    public void checkIfDefaultThemeIsAllowedTestThemeIsNotConsideredDefault() {
        Mockito.when(guiThemeServiceSpy.isConsideredDefaultTheme(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(false);

        guiThemeServiceSpy.checkIfDefaultThemeIsAllowed(COMMON_NAME, BLANK_STRING, BLANK_STRING, null);
    }

    @Test
    public void checkIfDefaultThemeIsAllowedTestThemeIsConsideredDefaultAndThereIsNotADefaultThemeRegistered() {
        Mockito.when(guiThemeServiceSpy.isConsideredDefaultTheme(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Mockito.when(guiThemeJoinDaoMock.findDefaultTheme()).thenReturn(null);

        guiThemeServiceSpy.checkIfDefaultThemeIsAllowed(BLANK_STRING, BLANK_STRING, BLANK_STRING, null);
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfDefaultThemeIsAllowedTestThemeIsConsideredDefaultAndThereIsADefaultThemeRegisteredShouldThrowCloudRuntimeException() {
        Mockito.when(guiThemeServiceSpy.isConsideredDefaultTheme(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Mockito.when(guiThemeJoinDaoMock.findDefaultTheme()).thenReturn(guiThemeJoinVOMock);

        guiThemeServiceSpy.checkIfDefaultThemeIsAllowed(BLANK_STRING, BLANK_STRING, BLANK_STRING, null);
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfDefaultThemeIsAllowedTestThemeIsConsideredDefaultAndWillBeUpdatedAndIsDifferentIdShouldThrowCloudRuntimeException() {
        Mockito.when(guiThemeServiceSpy.isConsideredDefaultTheme(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Mockito.when(guiThemeJoinDaoMock.findDefaultTheme()).thenReturn(guiThemeJoinVOMock);
        Mockito.when(guiThemeJoinVOMock.getId()).thenReturn(1L);

        guiThemeServiceSpy.checkIfDefaultThemeIsAllowed(BLANK_STRING, BLANK_STRING, BLANK_STRING, 2L);
    }

    @Test
    public void checkIfDefaultThemeIsAllowedTestThemeIsConsideredDefaultAndWillBeUpdatedAndIsTheSameShouldAllowUpdate() {
        Mockito.when(guiThemeServiceSpy.isConsideredDefaultTheme(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Mockito.when(guiThemeJoinDaoMock.findDefaultTheme()).thenReturn(guiThemeJoinVOMock);
        Mockito.when(guiThemeJoinVOMock.getId()).thenReturn(1L);

        guiThemeServiceSpy.checkIfDefaultThemeIsAllowed(BLANK_STRING, BLANK_STRING, BLANK_STRING, 1L);
    }
}
