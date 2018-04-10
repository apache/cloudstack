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

package com.cloud.network.vpc;

import java.util.ArrayList;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.network.CreateNetworkACLCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkACLItemCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkACLListCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.NetworkACLItem.Action;
import com.cloud.network.vpc.NetworkACLItem.TrafficType;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CallContext.class)
public class NetworkACLServiceImplTest {

    @Spy
    @InjectMocks
    private NetworkACLServiceImpl networkAclServiceImpl = new NetworkACLServiceImpl();
    @Mock
    private NetworkModel networkModelMock;
    @Mock
    private NetworkACLManager networkAclManager;
    @Mock
    private NetworkACLItemDao networkAclItemDaoMock;
    @Mock
    private EntityManager entityManagerMock;
    @Mock
    private AccountManager accountManagerMock;
    @Mock
    private NetworkACLDao networkAclDaoMock;

    @Mock
    private CreateNetworkACLCmd createNetworkAclCmdMock;
    @Mock
    private UpdateNetworkACLItemCmd updateNetworkACLItemCmdMock;
    @Mock
    private Network networkMock;
    @Mock
    private NetworkACL networkAclMock;
    @Mock
    private NetworkACLItemVO networkAclItemVoMock;
    @Mock
    private NetworkACLVO networkACLVOMock;
    @Mock
    private UpdateNetworkACLListCmd updateNetworkACLListCmdMock;

    private Long networkAclMockId = 1L;
    private Long networkOfferingMockId = 2L;
    private Long networkMockVpcMockId = 3L;
    private long networkAclListId = 1l;

    @Before
    public void befoteTest() {
        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(Mockito.mock(CallContext.class));

        Mockito.when(networkAclDaoMock.findById(networkAclListId)).thenReturn(networkACLVOMock);
        Mockito.when(createNetworkAclCmdMock.getNetworkId()).thenReturn(1L);
        Mockito.when(createNetworkAclCmdMock.getProtocol()).thenReturn("tcp");

        Mockito.when(networkMock.getNetworkOfferingId()).thenReturn(networkOfferingMockId);
        Mockito.when(networkMock.getVpcId()).thenReturn(networkMockVpcMockId);
    }

    @Test
    public void createNetworkACLItemTestAclNumberNull() {
        createNetworkACLItemTestForNumberAndExecuteTest(null);
    }

    @Test
    public void createNetworkACLItemTestAclNumberNotNull() {
        createNetworkACLItemTestForNumberAndExecuteTest(10);
    }

    private void createNetworkACLItemTestForNumberAndExecuteTest(Integer number) {
        Mockito.when(createNetworkAclCmdMock.getNumber()).thenReturn(number);

        Mockito.doReturn(networkAclMockId).when(networkAclServiceImpl).createAclListIfNeeded(createNetworkAclCmdMock);
        Mockito.when(networkAclManager.getNetworkACL(networkAclMockId)).thenReturn(networkAclMock);

        Mockito.doNothing().when(networkAclServiceImpl).validateAclRuleNumber(createNetworkAclCmdMock, networkAclMock);
        Mockito.doNothing().when(networkAclServiceImpl).validateNetworkAcl(networkAclMock);

        Mockito.doReturn(Action.Allow).when(networkAclServiceImpl).validateAndCreateNetworkAclRuleAction(Mockito.anyString());
        Mockito.when(networkAclItemDaoMock.getMaxNumberByACL(networkAclMockId)).thenReturn(5);

        Mockito.doNothing().when(networkAclServiceImpl).validateNetworkACLItem(Mockito.any(NetworkACLItemVO.class));
        Mockito.when(networkAclManager.createNetworkACLItem(Mockito.any(NetworkACLItemVO.class))).thenAnswer(new Answer<NetworkACLItemVO>() {
            @Override
            public NetworkACLItemVO answer(InvocationOnMock invocation) throws Throwable {
                return (NetworkACLItemVO)invocation.getArguments()[0];
            }
        });

        NetworkACLItem netowkrAclRuleCreated = networkAclServiceImpl.createNetworkACLItem(createNetworkAclCmdMock);

        Assert.assertEquals(number == null ? 6 : number, netowkrAclRuleCreated.getNumber());

        InOrder inOrder = Mockito.inOrder(networkAclManager, networkAclServiceImpl, networkAclItemDaoMock);
        inOrder.verify(networkAclServiceImpl).createAclListIfNeeded(createNetworkAclCmdMock);
        inOrder.verify(networkAclManager).getNetworkACL(networkAclMockId);
        inOrder.verify(networkAclServiceImpl).validateNetworkAcl(networkAclMock);
        inOrder.verify(networkAclServiceImpl).validateAclRuleNumber(createNetworkAclCmdMock, networkAclMock);
        inOrder.verify(networkAclServiceImpl).validateAndCreateNetworkAclRuleAction(Mockito.anyString());
        inOrder.verify(networkAclItemDaoMock, Mockito.times(number == null ? 1 : 0)).getMaxNumberByACL(networkAclMockId);
        inOrder.verify(networkAclServiceImpl).validateNetworkACLItem(Mockito.any(NetworkACLItemVO.class));
        inOrder.verify(networkAclManager).createNetworkACLItem(Mockito.any(NetworkACLItemVO.class));
    }

    @Test
    public void createAclListIfNeededTestAclRuleListIdNotNull() {
        Long expectedAclListId = 1L;
        Mockito.when(createNetworkAclCmdMock.getACLId()).thenReturn(expectedAclListId);

        Long returnetAclListId = networkAclServiceImpl.createAclListIfNeeded(createNetworkAclCmdMock);

        Assert.assertEquals(expectedAclListId, returnetAclListId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createAclListIfNeededTestAclRuleListIdNullAndNetworkDoesNotHaveVpc() {
        Mockito.when(createNetworkAclCmdMock.getACLId()).thenReturn(null);

        long networkId = 1L;
        Mockito.when(createNetworkAclCmdMock.getNetworkId()).thenReturn(networkId);

        Network networkMock = Mockito.mock(Network.class);
        Mockito.when(networkMock.getVpcId()).thenReturn(null);

        Mockito.doReturn(networkMock).when(networkModelMock).getNetwork(networkId);

        networkAclServiceImpl.createAclListIfNeeded(createNetworkAclCmdMock);
    }

    @Test
    public void createAclListIfNeededTestAclRuleListIdNullAndNetworkWithVpcAndNotAclListYet() {
        Mockito.when(createNetworkAclCmdMock.getACLId()).thenReturn(null);

        long networkId = 1L;
        Mockito.when(createNetworkAclCmdMock.getNetworkId()).thenReturn(networkId);

        Network networkMock = Mockito.mock(Network.class);
        Mockito.when(networkMock.getVpcId()).thenReturn(12L);
        Mockito.when(networkMock.getNetworkACLId()).thenReturn(null);

        Mockito.doReturn(networkMock).when(networkModelMock).getNetwork(networkId);

        Long expectedAclListId = 15L;
        Mockito.doReturn(expectedAclListId).when(networkAclServiceImpl).createAclListForNetworkAndReturnAclListId(createNetworkAclCmdMock, networkMock);

        Long aclIdReturned = networkAclServiceImpl.createAclListIfNeeded(createNetworkAclCmdMock);

        Assert.assertEquals(expectedAclListId, aclIdReturned);
        Mockito.verify(networkAclServiceImpl).createAclListForNetworkAndReturnAclListId(createNetworkAclCmdMock, networkMock);
    }

    @Test
    public void createAclListIfNeededTestAclRuleListIdNullAndNetworkWithVpcAndAclListAlreadyCreated() {
        Mockito.when(createNetworkAclCmdMock.getACLId()).thenReturn(null);

        long networkId = 1L;
        Mockito.when(createNetworkAclCmdMock.getNetworkId()).thenReturn(networkId);
        Network networkMock = Mockito.mock(Network.class);
        ;
        Mockito.when(networkMock.getVpcId()).thenReturn(12L);
        Long expectedAclListId = 15L;
        Mockito.when(networkMock.getNetworkACLId()).thenReturn(expectedAclListId);

        Mockito.doReturn(networkMock).when(networkModelMock).getNetwork(networkId);

        Mockito.doReturn(16L).when(networkAclServiceImpl).createAclListForNetworkAndReturnAclListId(createNetworkAclCmdMock, networkMock);

        Long aclIdReturned = networkAclServiceImpl.createAclListIfNeeded(createNetworkAclCmdMock);

        Assert.assertEquals(expectedAclListId, aclIdReturned);
        Mockito.verify(networkAclServiceImpl, Mockito.times(0)).createAclListForNetworkAndReturnAclListId(createNetworkAclCmdMock, networkMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createAclListForNetworkAndReturnAclListIdTestServicesNotSupportedByNetworkOffering() {
        Mockito.doReturn(false).when(networkModelMock).areServicesSupportedByNetworkOffering(networkOfferingMockId, Network.Service.NetworkACL);
        networkAclServiceImpl.createAclListForNetworkAndReturnAclListId(createNetworkAclCmdMock, networkMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createAclListForNetworkAndReturnAclListIdTestServicesSupportedByNetworkOfferingButVpcNotFound() {
        Mockito.doReturn(true).when(networkModelMock).areServicesSupportedByNetworkOffering(networkOfferingMockId, Network.Service.NetworkACL);
        Mockito.doReturn(null).when(entityManagerMock).findById(Vpc.class, networkMockVpcMockId);

        networkAclServiceImpl.createAclListForNetworkAndReturnAclListId(createNetworkAclCmdMock, networkMock);
    }

    @Test(expected = CloudRuntimeException.class)
    public void createAclListForNetworkAndReturnAclListIdTestCreateNetworkAclReturnsNull() {
        Mockito.doReturn(true).when(networkModelMock).areServicesSupportedByNetworkOffering(networkOfferingMockId, Network.Service.NetworkACL);
        Mockito.doReturn(Mockito.mock(Vpc.class)).when(entityManagerMock).findById(Vpc.class, networkMockVpcMockId);
        Mockito.doReturn(null).when(networkAclManager).createNetworkACL(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyBoolean());

        networkAclServiceImpl.createAclListForNetworkAndReturnAclListId(createNetworkAclCmdMock, networkMock);
    }

    @Test(expected = CloudRuntimeException.class)
    public void createAclListForNetworkAndReturnAclListIdTestAclNetworkIsCreatedButNotApplied() throws ResourceUnavailableException {
        Mockito.doReturn(true).when(networkModelMock).areServicesSupportedByNetworkOffering(networkOfferingMockId, Network.Service.NetworkACL);
        Mockito.doReturn(Mockito.mock(Vpc.class)).when(entityManagerMock).findById(Vpc.class, networkMockVpcMockId);
        Mockito.doReturn(Mockito.mock(NetworkACL.class)).when(networkAclManager).createNetworkACL(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyBoolean());
        Mockito.doReturn(false).when(networkAclManager).replaceNetworkACL(Mockito.any(NetworkACL.class), Mockito.any(NetworkVO.class));

        NetworkVO networkVoMock = new NetworkVO();
        networkVoMock.setNetworkOfferingId(networkOfferingMockId);
        networkVoMock.setVpcId(networkMockVpcMockId);

        networkAclServiceImpl.createAclListForNetworkAndReturnAclListId(createNetworkAclCmdMock, networkVoMock);
    }

    @Test(expected = CloudRuntimeException.class)
    public void createAclListForNetworkAndReturnAclListIdTestAclNetworkIsCreatedButNotAppliedWithException() throws ResourceUnavailableException {
        Mockito.doReturn(true).when(networkModelMock).areServicesSupportedByNetworkOffering(networkOfferingMockId, Network.Service.NetworkACL);
        Mockito.doReturn(Mockito.mock(Vpc.class)).when(entityManagerMock).findById(Vpc.class, networkMockVpcMockId);
        Mockito.doReturn(Mockito.mock(NetworkACL.class)).when(networkAclManager).createNetworkACL(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyBoolean());

        Mockito.doThrow(ResourceUnavailableException.class).when(networkAclManager).replaceNetworkACL(Mockito.any(NetworkACL.class), Mockito.any(NetworkVO.class));

        NetworkVO networkVoMock = new NetworkVO();
        networkVoMock.setNetworkOfferingId(networkOfferingMockId);
        networkVoMock.setVpcId(networkMockVpcMockId);

        networkAclServiceImpl.createAclListForNetworkAndReturnAclListId(createNetworkAclCmdMock, networkVoMock);
    }

    @Test
    public void createAclListForNetworkAndReturnAclListIdTestAclIsCreatedAndAppliedWithSuccess() throws ResourceUnavailableException {
        Mockito.doReturn(true).when(networkModelMock).areServicesSupportedByNetworkOffering(networkOfferingMockId, Network.Service.NetworkACL);
        Mockito.doReturn(Mockito.mock(Vpc.class)).when(entityManagerMock).findById(Vpc.class, networkMockVpcMockId);

        NetworkACL networkAclMock = Mockito.mock(NetworkACL.class);
        Long expectedNetworkAclId = 5L;
        Mockito.when(networkAclMock.getId()).thenReturn(expectedNetworkAclId);
        Mockito.doReturn(networkAclMock).when(networkAclManager).createNetworkACL(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyBoolean());

        Mockito.doReturn(true).when(networkAclManager).replaceNetworkACL(Mockito.any(NetworkACL.class), Mockito.any(NetworkVO.class));

        NetworkVO networkVoMock = new NetworkVO();
        networkVoMock.setNetworkOfferingId(networkOfferingMockId);
        networkVoMock.setVpcId(networkMockVpcMockId);

        Long networkAclIdReceived = networkAclServiceImpl.createAclListForNetworkAndReturnAclListId(createNetworkAclCmdMock, networkVoMock);

        Assert.assertEquals(expectedNetworkAclId, networkAclIdReceived);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAclRuleNumberTestNumberLessThanOne() {
        Mockito.when(createNetworkAclCmdMock.getNumber()).thenReturn(0);
        networkAclServiceImpl.validateAclRuleNumber(createNetworkAclCmdMock, networkAclMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAclRuleNumberTestNumberNegative() {
        Mockito.when(createNetworkAclCmdMock.getNumber()).thenReturn(-1);
        networkAclServiceImpl.validateAclRuleNumber(createNetworkAclCmdMock, networkAclMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAclRuleNumberTestNumberInOtherAcl() {
        Mockito.when(createNetworkAclCmdMock.getNumber()).thenReturn(1);
        Mockito.doReturn(Mockito.mock(NetworkACLItemVO.class)).when(networkAclItemDaoMock).findByAclAndNumber(Mockito.anyLong(), Mockito.anyInt());

        networkAclServiceImpl.validateAclRuleNumber(createNetworkAclCmdMock, networkAclMock);

        Mockito.verify(networkAclItemDaoMock).findByAclAndNumber(Mockito.anyLong(), Mockito.anyInt());
    }

    @Test
    public void validateAclRuleNumberTestHappyDay() {
        Mockito.when(createNetworkAclCmdMock.getNumber()).thenReturn(1);
        Mockito.doReturn(null).when(networkAclItemDaoMock).findByAclAndNumber(Mockito.anyLong(), Mockito.anyInt());

        networkAclServiceImpl.validateAclRuleNumber(createNetworkAclCmdMock, networkAclMock);
        Mockito.verify(networkAclItemDaoMock).findByAclAndNumber(Mockito.anyLong(), Mockito.anyInt());
    }

    @Test
    public void validateAclRuleNumberTestNumberNull() {
        Mockito.when(createNetworkAclCmdMock.getNumber()).thenReturn(null);
        Mockito.doReturn(null).when(networkAclItemDaoMock).findByAclAndNumber(Mockito.anyLong(), Mockito.anyInt());

        networkAclServiceImpl.validateAclRuleNumber(createNetworkAclCmdMock, networkAclMock);
        Mockito.verify(networkAclItemDaoMock, Mockito.times(0)).findByAclAndNumber(Mockito.anyLong(), Mockito.anyInt());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateNetworkAclTestAclNull() {
        networkAclServiceImpl.validateNetworkAcl(null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateNetworkAclTestAclDefaulAllow() {
        Mockito.when(networkAclMock.getId()).thenReturn(2L);
        networkAclServiceImpl.validateNetworkAcl(networkAclMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateNetworkAclTestAclDefaulDeny() {
        Mockito.when(networkAclMock.getId()).thenReturn(1L);
        networkAclServiceImpl.validateNetworkAcl(networkAclMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateNetworkAclTestAclNotDefaulWithoutVpc() {
        Mockito.when(networkAclMock.getId()).thenReturn(3L);
        Mockito.doReturn(null).when(entityManagerMock).findById(Vpc.class, networkMockVpcMockId);
        ;

        networkAclServiceImpl.validateNetworkAcl(networkAclMock);
    }

    @Test
    @PrepareForTest(CallContext.class)
    public void validateNetworkAclTestAclNotDefaulWithVpc() {
        CallContext callContextMock = Mockito.mock(CallContext.class);
        Mockito.doReturn(Mockito.mock(Account.class)).when(callContextMock).getCallingAccount();

        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.when(networkAclMock.getId()).thenReturn(3L);
        Mockito.when(networkAclMock.getVpcId()).thenReturn(networkMockVpcMockId);

        Mockito.doReturn(Mockito.mock(Vpc.class)).when(entityManagerMock).findById(Vpc.class, networkMockVpcMockId);
        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.isNull(AccessType.class), Mockito.eq(true), Mockito.any(Vpc.class));

        networkAclServiceImpl.validateNetworkAcl(networkAclMock);

        Mockito.verify(entityManagerMock).findById(Vpc.class, networkMockVpcMockId);
        Mockito.verify(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.isNull(AccessType.class), Mockito.eq(true), Mockito.any(Vpc.class));

        PowerMockito.verifyStatic();
        CallContext.current();

    }

    @Test
    public void validateAndCreateNetworkAclRuleActionTestActionNull() {
        NetworkACLItem.Action receivedAction = networkAclServiceImpl.validateAndCreateNetworkAclRuleAction(null);

        Assert.assertEquals(NetworkACLItem.Action.Allow, receivedAction);

        Mockito.verify(networkAclServiceImpl).validateNetworkAclRuleAction(null);
    }

    @Test
    public void validateAndCreateNetworkAclRuleActionTestActionAllow() {
        NetworkACLItem.Action receivedAction = networkAclServiceImpl.validateAndCreateNetworkAclRuleAction("allow");

        Assert.assertEquals(NetworkACLItem.Action.Allow, receivedAction);
        Mockito.verify(networkAclServiceImpl).validateNetworkAclRuleAction("allow");
    }

    @Test
    public void validateAndCreateNetworkAclRuleActionTestActionDeny() {
        NetworkACLItem.Action receivedAction = networkAclServiceImpl.validateAndCreateNetworkAclRuleAction("deny");

        Assert.assertEquals(NetworkACLItem.Action.Deny, receivedAction);
        Mockito.verify(networkAclServiceImpl).validateNetworkAclRuleAction("deny");
    }

    @Test
    public void validateNetworkAclRuleActionTestActionNull() {
        networkAclServiceImpl.validateNetworkAclRuleAction(null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateNetworkAclRuleActionTestInvalidAction() {
        networkAclServiceImpl.validateNetworkAclRuleAction("Invalid");
    }

    @Test
    public void validateNetworkAclRuleActionTestValidActions() {
        networkAclServiceImpl.validateNetworkAclRuleAction("deny");
        networkAclServiceImpl.validateNetworkAclRuleAction("allow");
    }

    @Test
    public void validateNetworkACLItemTest() {
        Mockito.doNothing().when(networkAclServiceImpl).validateSourceStartAndEndPorts(networkAclItemVoMock);
        Mockito.doNothing().when(networkAclServiceImpl).validateSourceCidrList(networkAclItemVoMock);
        Mockito.doNothing().when(networkAclServiceImpl).validateProtocol(networkAclItemVoMock);

        networkAclServiceImpl.validateNetworkACLItem(networkAclItemVoMock);

        InOrder inOrder = Mockito.inOrder(networkAclServiceImpl);
        inOrder.verify(networkAclServiceImpl).validateSourceStartAndEndPorts(networkAclItemVoMock);
        inOrder.verify(networkAclServiceImpl).validateSourceCidrList(networkAclItemVoMock);
        inOrder.verify(networkAclServiceImpl).validateProtocol(networkAclItemVoMock);
    }

    @Test
    public void validateSourceStartAndEndPortsTestBothPortsNull() {
        networkAclServiceImpl.validateSourceStartAndEndPorts(networkAclItemVoMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateSourceStartAndEndPortsTestStartPorInvalid() {
        Mockito.when(networkAclItemVoMock.getSourcePortStart()).thenReturn(65536);

        networkAclServiceImpl.validateSourceStartAndEndPorts(networkAclItemVoMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateSourceStartAndEndPortsTestStartPorValidButEndPortInvalid() {
        Mockito.when(networkAclItemVoMock.getSourcePortStart()).thenReturn(65535);
        Mockito.when(networkAclItemVoMock.getSourcePortEnd()).thenReturn(65536);

        networkAclServiceImpl.validateSourceStartAndEndPorts(networkAclItemVoMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateSourceStartAndEndPortsTestStartPortBiggerThanEndPort() {
        Mockito.when(networkAclItemVoMock.getSourcePortStart()).thenReturn(65535);
        Mockito.when(networkAclItemVoMock.getSourcePortEnd()).thenReturn(2);

        networkAclServiceImpl.validateSourceStartAndEndPorts(networkAclItemVoMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateSourceStartAndEndPortsTestPortsWithAllProtocol() {
        Mockito.when(networkAclItemVoMock.getSourcePortStart()).thenReturn(1);
        Mockito.when(networkAclItemVoMock.getSourcePortEnd()).thenReturn(2);
        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("all");

        networkAclServiceImpl.validateSourceStartAndEndPorts(networkAclItemVoMock);
    }

    @Test
    public void validateSourceStartAndEndPortsTestPortsWithTcpProtocol() {
        Mockito.when(networkAclItemVoMock.getSourcePortStart()).thenReturn(1);
        Mockito.when(networkAclItemVoMock.getSourcePortEnd()).thenReturn(2);
        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("tcp");

        networkAclServiceImpl.validateSourceStartAndEndPorts(networkAclItemVoMock);
    }

    @Test
    public void validateSourceCidrListTestEmptySourceCirdList() {
        Mockito.when(networkAclItemVoMock.getSourceCidrList()).thenReturn(new ArrayList<>());
        networkAclServiceImpl.validateSourceCidrList(networkAclItemVoMock);
    }

    @Test(expected = ServerApiException.class)
    public void validateSourceCidrListTestInvalidCidrs() {
        ArrayList<String> cidrsInvalid = new ArrayList<>();
        cidrsInvalid.add("256.0.0.0./32");

        Mockito.when(networkAclItemVoMock.getSourceCidrList()).thenReturn(cidrsInvalid);
        networkAclServiceImpl.validateSourceCidrList(networkAclItemVoMock);
    }

    @Test
    public void validateSourceCidrListTestValidCidrs() {
        ArrayList<String> cidrsInvalid = new ArrayList<>();
        cidrsInvalid.add("192.168.12.0/24");

        Mockito.when(networkAclItemVoMock.getSourceCidrList()).thenReturn(cidrsInvalid);
        networkAclServiceImpl.validateSourceCidrList(networkAclItemVoMock);
    }

    @Test
    public void validateProtocolTestProtocolIsNullOrBlank() {
        Mockito.doNothing().when(networkAclServiceImpl).validateIcmpTypeAndCode(networkAclItemVoMock);

        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn(null);
        networkAclServiceImpl.validateProtocol(networkAclItemVoMock);

        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("");
        networkAclServiceImpl.validateProtocol(networkAclItemVoMock);

        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("    ");
        networkAclServiceImpl.validateProtocol(networkAclItemVoMock);

        Mockito.verify(networkAclServiceImpl, Mockito.times(0)).validateIcmpTypeAndCode(networkAclItemVoMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateProtocolTestProtocolIsNumericValueLessThanZero() {
        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("-1");
        networkAclServiceImpl.validateProtocol(networkAclItemVoMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateProtocolTestProtocolIsNumericValueMoreThan255() {
        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("256");
        networkAclServiceImpl.validateProtocol(networkAclItemVoMock);
    }

    @Test
    public void validateProtocolTestProtocolIsNumericValidValue() {
        Mockito.doNothing().when(networkAclServiceImpl).validateIcmpTypeAndCode(networkAclItemVoMock);

        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("255");
        Mockito.when(networkAclItemVoMock.getIcmpCode()).thenReturn(null);
        Mockito.when(networkAclItemVoMock.getIcmpType()).thenReturn(null);

        networkAclServiceImpl.validateProtocol(networkAclItemVoMock);

        Mockito.verify(networkAclServiceImpl).validateIcmpTypeAndCode(networkAclItemVoMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateProtocolTestProtocolIsStringInvalid() {
        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("invalid");
        networkAclServiceImpl.validateProtocol(networkAclItemVoMock);
    }

    @Test
    public void validateProtocolTestProtocolIsStringValid() {
        Mockito.doNothing().when(networkAclServiceImpl).validateIcmpTypeAndCode(networkAclItemVoMock);

        Mockito.when(networkAclItemVoMock.getIcmpCode()).thenReturn(null);
        Mockito.when(networkAclItemVoMock.getIcmpType()).thenReturn(null);

        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("tcp");
        networkAclServiceImpl.validateProtocol(networkAclItemVoMock);

        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("all");
        networkAclServiceImpl.validateProtocol(networkAclItemVoMock);

        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("udp");
        networkAclServiceImpl.validateProtocol(networkAclItemVoMock);

        Mockito.verify(networkAclServiceImpl, Mockito.times(3)).validateIcmpTypeAndCode(networkAclItemVoMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateProtocolTestProtocolNotIcmpWithIcmpConfigurations() {
        Mockito.when(networkAclItemVoMock.getIcmpCode()).thenReturn(1);
        Mockito.when(networkAclItemVoMock.getIcmpType()).thenReturn(1);

        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("tcp");
        networkAclServiceImpl.validateProtocol(networkAclItemVoMock);
    }

    @Test
    public void validateProtocolTestProtocolNotIcmpWithSourcePorts() {
        Mockito.doNothing().when(networkAclServiceImpl).validateIcmpTypeAndCode(networkAclItemVoMock);

        Mockito.when(networkAclItemVoMock.getIcmpCode()).thenReturn(null);
        Mockito.when(networkAclItemVoMock.getIcmpType()).thenReturn(null);

        Mockito.when(networkAclItemVoMock.getSourcePortStart()).thenReturn(1);
        Mockito.when(networkAclItemVoMock.getSourcePortEnd()).thenReturn(1);

        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("tcp");
        networkAclServiceImpl.validateProtocol(networkAclItemVoMock);

        Mockito.verify(networkAclServiceImpl).validateIcmpTypeAndCode(networkAclItemVoMock);
    }

    @Test
    public void validateProtocolTestProtocolIcmpWithIcmpConfigurations() {
        Mockito.when(networkAclItemVoMock.getIcmpCode()).thenReturn(1);
        Mockito.when(networkAclItemVoMock.getIcmpType()).thenReturn(1);

        Mockito.when(networkAclItemVoMock.getSourcePortStart()).thenReturn(null);
        Mockito.when(networkAclItemVoMock.getSourcePortEnd()).thenReturn(null);

        Mockito.doNothing().when(networkAclServiceImpl).validateIcmpTypeAndCode(networkAclItemVoMock);

        Mockito.when(networkAclItemVoMock.getProtocol()).thenReturn("icmp");
        networkAclServiceImpl.validateProtocol(networkAclItemVoMock);

        Mockito.verify(networkAclServiceImpl).validateIcmpTypeAndCode(networkAclItemVoMock);
    }

    @Test
    public void validateIcmpTypeAndCodeTestIcmpTypeNull() {
        Mockito.when(networkAclItemVoMock.getIcmpType()).thenReturn(null);

        networkAclServiceImpl.validateIcmpTypeAndCode(networkAclItemVoMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateIcmpTypeAndCodeTestIcmpTypeInvalid() {
        Mockito.when(networkAclItemVoMock.getIcmpType()).thenReturn(256);

        networkAclServiceImpl.validateIcmpTypeAndCode(networkAclItemVoMock);
    }

    @Test
    public void validateIcmpTypeAndCodeTestIcmpTypeNegativeOne() {
        Mockito.when(networkAclItemVoMock.getIcmpType()).thenReturn(-1);
        Mockito.when(networkAclItemVoMock.getIcmpCode()).thenReturn(null);

        networkAclServiceImpl.validateIcmpTypeAndCode(networkAclItemVoMock);
    }

    @Test
    public void validateIcmpTypeAndCodeTestIcmpTypeNegativeOneAndIcmpCodeNegativeOne() {
        Mockito.when(networkAclItemVoMock.getIcmpType()).thenReturn(-1);
        Mockito.when(networkAclItemVoMock.getIcmpCode()).thenReturn(-1);

        networkAclServiceImpl.validateIcmpTypeAndCode(networkAclItemVoMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateIcmpTypeAndCodeTestIcmpTypeValidAndIcmpCodeInvalid() {
        Mockito.when(networkAclItemVoMock.getIcmpType()).thenReturn(255);
        Mockito.when(networkAclItemVoMock.getIcmpCode()).thenReturn(16);

        networkAclServiceImpl.validateIcmpTypeAndCode(networkAclItemVoMock);
    }

    @Test
    public void validateIcmpTypeAndCodeTestIcmpTypeValidAndIcmpCodeValid() {
        Mockito.when(networkAclItemVoMock.getIcmpType()).thenReturn(255);
        Mockito.when(networkAclItemVoMock.getIcmpCode()).thenReturn(1);

        networkAclServiceImpl.validateIcmpTypeAndCode(networkAclItemVoMock);
    }

    @Test
    public void updateNetworkACLItemTest() throws ResourceUnavailableException {
        Mockito.when(networkAclItemVoMock.getAclId()).thenReturn(networkAclMockId);
        Mockito.doReturn(networkAclItemVoMock).when(networkAclServiceImpl).validateNetworkAclRuleIdAndRetrieveIt(updateNetworkACLItemCmdMock);
        Mockito.doReturn(networkAclMock).when(networkAclManager).getNetworkACL(networkAclMockId);
        Mockito.doNothing().when(networkAclServiceImpl).validateNetworkAcl(Mockito.eq(networkAclMock));
        Mockito.doNothing().when(networkAclServiceImpl).transferDataToNetworkAclRulePojo(Mockito.eq(updateNetworkACLItemCmdMock), Mockito.eq(networkAclItemVoMock), Mockito.eq(networkAclMock));
        Mockito.doNothing().when(networkAclServiceImpl).validateNetworkACLItem(networkAclItemVoMock);
        Mockito.doReturn(networkAclItemVoMock).when(networkAclManager).updateNetworkACLItem(networkAclItemVoMock);

        networkAclServiceImpl.updateNetworkACLItem(updateNetworkACLItemCmdMock);

        InOrder inOrder = Mockito.inOrder(networkAclServiceImpl, networkAclManager);
        inOrder.verify(networkAclServiceImpl).validateNetworkAclRuleIdAndRetrieveIt(updateNetworkACLItemCmdMock);
        inOrder.verify(networkAclManager).getNetworkACL(networkAclMockId);
        inOrder.verify(networkAclServiceImpl).validateNetworkAcl(networkAclMock);
        inOrder.verify(networkAclServiceImpl).transferDataToNetworkAclRulePojo(Mockito.eq(updateNetworkACLItemCmdMock), Mockito.eq(networkAclItemVoMock), Mockito.eq(networkAclMock));
        inOrder.verify(networkAclServiceImpl).validateNetworkACLItem(networkAclItemVoMock);
        inOrder.verify(networkAclManager).updateNetworkACLItem(networkAclItemVoMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateNetworkAclRuleIdAndRetrieveItTestNetworkAclNotFound() {
        Mockito.doReturn(null).when(networkAclItemDaoMock).findById(Mockito.anyLong());

        networkAclServiceImpl.validateNetworkAclRuleIdAndRetrieveIt(updateNetworkACLItemCmdMock);
    }

    @Test
    public void validateNetworkAclRuleIdAndRetrieveItTestNetworkAclFound() {
        Mockito.doReturn(networkAclItemVoMock).when(networkAclItemDaoMock).findById(Mockito.anyLong());

        NetworkACLItemVO returnedNetworkAclItemVo = networkAclServiceImpl.validateNetworkAclRuleIdAndRetrieveIt(updateNetworkACLItemCmdMock);

        Assert.assertNotEquals(networkAclItemVoMock, returnedNetworkAclItemVo);
        Mockito.verify(networkAclItemVoMock).clone();
    }

    @Test(expected = InvalidParameterValueException.class)
    public void transferDataToNetworkAclRulePojoTestNumberOfAcltoBeUpdatedAlreadyInUse() {
        int aclNumberToUpdate = 1;
        Mockito.when(updateNetworkACLItemCmdMock.getNumber()).thenReturn(aclNumberToUpdate);
        Mockito.when(networkAclMock.getId()).thenReturn(networkAclMockId);
        Mockito.when(networkAclItemVoMock.getId()).thenReturn(100L);
        NetworkACLItemVO otherNetworkAclItemVoMock = Mockito.mock(NetworkACLItemVO.class);
        Mockito.when(otherNetworkAclItemVoMock.getId()).thenReturn(101L);
        Mockito.doReturn(otherNetworkAclItemVoMock).when(networkAclItemDaoMock).findByAclAndNumber(networkAclMockId, aclNumberToUpdate);

        networkAclServiceImpl.transferDataToNetworkAclRulePojo(updateNetworkACLItemCmdMock, networkAclItemVoMock, networkAclMock);
    }

    @Test
    public void transferDataToNetworkAclRulePojoTestPartialUpgradeAllValuesNull() {
        Mockito.when(updateNetworkACLItemCmdMock.isPartialUpgrade()).thenReturn(true);
        Mockito.when(updateNetworkACLItemCmdMock.getNumber()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getSourcePortStart()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getSourcePortEnd()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getSourceCidrList()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getProtocol()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getIcmpCode()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getIcmpType()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getAction()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getTrafficType()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getCustomId()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getReason()).thenReturn(null);

        Mockito.when(updateNetworkACLItemCmdMock.isDisplay()).thenReturn(false);
        Mockito.when(networkAclItemVoMock.isDisplay()).thenReturn(false);

        networkAclServiceImpl.transferDataToNetworkAclRulePojo(updateNetworkACLItemCmdMock, networkAclItemVoMock, networkAclMock);

        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setNumber(Mockito.anyInt());
        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setSourcePortStart(Mockito.anyInt());
        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setSourcePortEnd(Mockito.anyInt());
        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setSourceCidrList(Mockito.anyListOf(String.class));
        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setProtocol(Mockito.anyString());
        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setIcmpCode(Mockito.anyInt());
        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setIcmpType(Mockito.anyInt());
        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setAction(Mockito.any(Action.class));
        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setTrafficType(Mockito.any(TrafficType.class));
        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setUuid(Mockito.anyString());
        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setReason(Mockito.anyString());
        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setDisplay(Mockito.anyBoolean());
        Mockito.verify(networkAclServiceImpl, Mockito.times(0)).validateAndCreateNetworkAclRuleAction(Mockito.anyString());
    }

    @Test
    public void transferDataToNetworkAclRulePojoTestNotPartialUpgradeAllValuesNull() {
        Mockito.when(updateNetworkACLItemCmdMock.isPartialUpgrade()).thenReturn(false);

        Mockito.when(updateNetworkACLItemCmdMock.getNumber()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getSourcePortStart()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getSourcePortEnd()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getSourceCidrList()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getProtocol()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getIcmpCode()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getIcmpType()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getAction()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getTrafficType()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getCustomId()).thenReturn(null);
        Mockito.when(updateNetworkACLItemCmdMock.getReason()).thenReturn(null);

        Mockito.when(updateNetworkACLItemCmdMock.isDisplay()).thenReturn(false);
        Mockito.when(networkAclItemVoMock.isDisplay()).thenReturn(false);

        networkAclServiceImpl.transferDataToNetworkAclRulePojo(updateNetworkACLItemCmdMock, networkAclItemVoMock, networkAclMock);

        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setNumber(Mockito.anyInt());
        Mockito.verify(networkAclItemVoMock, Mockito.times(1)).setSourcePortStart(Mockito.anyInt());
        Mockito.verify(networkAclItemVoMock, Mockito.times(1)).setSourcePortEnd(Mockito.anyInt());
        Mockito.verify(networkAclItemVoMock, Mockito.times(1)).setSourceCidrList(Mockito.anyListOf(String.class));
        Mockito.verify(networkAclItemVoMock, Mockito.times(1)).setProtocol(Mockito.anyString());
        Mockito.verify(networkAclItemVoMock, Mockito.times(1)).setIcmpCode(Mockito.anyInt());
        Mockito.verify(networkAclItemVoMock, Mockito.times(1)).setIcmpType(Mockito.anyInt());
        Mockito.verify(networkAclItemVoMock, Mockito.times(1)).setAction(Mockito.any(Action.class));
        Mockito.verify(networkAclItemVoMock, Mockito.times(1)).setTrafficType(Mockito.any(TrafficType.class));
        Mockito.verify(networkAclItemVoMock, Mockito.times(0)).setUuid(Mockito.anyString());
        Mockito.verify(networkAclItemVoMock, Mockito.times(1)).setReason(Mockito.anyString());
        Mockito.verify(networkAclItemVoMock, Mockito.times(1)).setDisplay(Mockito.anyBoolean());
        Mockito.verify(networkAclServiceImpl, Mockito.times(1)).validateAndCreateNetworkAclRuleAction(Mockito.anyString());
    }

    @Test
    public void transferDataToNetworkAclRulePojoTestAllValuesWithUpdateData() {
        Mockito.when(updateNetworkACLItemCmdMock.getNumber()).thenReturn(1);
        Mockito.when(updateNetworkACLItemCmdMock.getSourcePortStart()).thenReturn(23);
        Mockito.when(updateNetworkACLItemCmdMock.getSourcePortEnd()).thenReturn(24);

        ArrayList<String> cidrsList = new ArrayList<>();
        cidrsList.add("192.168.6.0/24");
        Mockito.when(updateNetworkACLItemCmdMock.getSourceCidrList()).thenReturn(cidrsList);

        Mockito.when(updateNetworkACLItemCmdMock.getProtocol()).thenReturn("all");
        Mockito.when(updateNetworkACLItemCmdMock.getIcmpCode()).thenReturn(5);
        Mockito.when(updateNetworkACLItemCmdMock.getIcmpType()).thenReturn(6);
        Mockito.when(updateNetworkACLItemCmdMock.getAction()).thenReturn("deny");
        Mockito.when(updateNetworkACLItemCmdMock.getTrafficType()).thenReturn(TrafficType.Egress);
        Mockito.when(updateNetworkACLItemCmdMock.getCustomId()).thenReturn("customUuid");
        Mockito.when(updateNetworkACLItemCmdMock.getReason()).thenReturn("reason");

        Mockito.when(updateNetworkACLItemCmdMock.isDisplay()).thenReturn(true);
        Mockito.when(networkAclItemVoMock.isDisplay()).thenReturn(false);

        networkAclServiceImpl.transferDataToNetworkAclRulePojo(updateNetworkACLItemCmdMock, networkAclItemVoMock, networkAclMock);

        Mockito.verify(networkAclItemVoMock).setNumber(1);
        Mockito.verify(networkAclItemVoMock).setSourcePortStart(23);
        Mockito.verify(networkAclItemVoMock).setSourcePortEnd(24);
        Mockito.verify(networkAclItemVoMock).setSourceCidrList(cidrsList);
        Mockito.verify(networkAclItemVoMock).setProtocol("all");
        Mockito.verify(networkAclItemVoMock).setIcmpCode(5);
        Mockito.verify(networkAclItemVoMock).setIcmpType(6);
        Mockito.verify(networkAclItemVoMock).setAction(Action.Deny);
        Mockito.verify(networkAclItemVoMock).setTrafficType(TrafficType.Egress);
        Mockito.verify(networkAclItemVoMock).setUuid("customUuid");
        Mockito.verify(networkAclItemVoMock).setReason("reason");
        Mockito.verify(networkAclItemVoMock).setDisplay(true);
        Mockito.verify(networkAclServiceImpl).validateAndCreateNetworkAclRuleAction("deny");
    }

    @Test
    @PrepareForTest(CallContext.class)
    public void updateNetworkACLTestParametersNotNull() {
        String name = "name";
        String description = "desc";
        String customId = "customId";

        Mockito.when(updateNetworkACLListCmdMock.getName()).thenReturn(name);
        Mockito.when(updateNetworkACLListCmdMock.getDescription()).thenReturn(description);
        Mockito.when(updateNetworkACLListCmdMock.getCustomId()).thenReturn(customId);
        Mockito.when(updateNetworkACLListCmdMock.getId()).thenReturn(networkAclListId);
        Mockito.when(updateNetworkACLListCmdMock.getDisplay()).thenReturn(false);

        networkAclServiceImpl.updateNetworkACL(updateNetworkACLListCmdMock);

        InOrder inOrder = Mockito.inOrder(networkAclDaoMock, entityManagerMock, entityManagerMock, accountManagerMock, networkACLVOMock);

        inOrder.verify(networkAclDaoMock).findById(networkAclListId);
        inOrder.verify(entityManagerMock).findById(Mockito.eq(Vpc.class), Mockito.anyLong());
        inOrder.verify(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.isNull(AccessType.class), Mockito.eq(true), Mockito.any(Vpc.class));

        inOrder.verify(networkACLVOMock).setName(name);
        inOrder.verify(networkACLVOMock).setDescription(description);
        inOrder.verify(networkACLVOMock).setUuid(customId);
        inOrder.verify(networkACLVOMock).setDisplay(false);

        inOrder.verify(networkAclDaoMock).update(networkAclListId, networkACLVOMock);
        inOrder.verify(networkAclDaoMock).findById(networkAclListId);
    }

    @Test
    @PrepareForTest(CallContext.class)
    public void updateNetworkACLTestParametersWithNullValues() {
        Mockito.when(updateNetworkACLListCmdMock.getName()).thenReturn(null);
        Mockito.when(updateNetworkACLListCmdMock.getDescription()).thenReturn(null);
        Mockito.when(updateNetworkACLListCmdMock.getCustomId()).thenReturn(null);
        Mockito.when(updateNetworkACLListCmdMock.getId()).thenReturn(networkAclListId);
        Mockito.when(updateNetworkACLListCmdMock.getDisplay()).thenReturn(null);

        networkAclServiceImpl.updateNetworkACL(updateNetworkACLListCmdMock);

        InOrder inOrder = Mockito.inOrder(networkAclDaoMock, entityManagerMock, entityManagerMock, accountManagerMock, networkACLVOMock);

        inOrder.verify(networkAclDaoMock).findById(networkAclListId);
        inOrder.verify(entityManagerMock).findById(Mockito.eq(Vpc.class), Mockito.anyLong());
        inOrder.verify(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.isNull(AccessType.class), Mockito.eq(true), Mockito.any(Vpc.class));

        Mockito.verify(networkACLVOMock, Mockito.times(0)).setName(null);
        inOrder.verify(networkACLVOMock, Mockito.times(0)).setDescription(null);
        inOrder.verify(networkACLVOMock, Mockito.times(0)).setUuid(null);
        inOrder.verify(networkACLVOMock, Mockito.times(0)).setDisplay(false);

        inOrder.verify(networkAclDaoMock).update(networkAclListId, networkACLVOMock);
        inOrder.verify(networkAclDaoMock).findById(networkAclListId);
    }

}
