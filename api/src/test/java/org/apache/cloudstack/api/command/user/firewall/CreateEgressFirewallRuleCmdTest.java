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
package org.apache.cloudstack.api.command.user.firewall;

import com.cloud.network.Network;
import com.cloud.network.NetworkService;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.api.ServerApiException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class CreateEgressFirewallRuleCmdTest {

    @Mock
    NetworkService networkServiceMock;

    @Spy
    CreateEgressFirewallRuleCmd cmdMock = new CreateEgressFirewallRuleCmd();

    MockedStatic<NetUtils> netUtilsMocked;

    @Before
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(cmdMock, "_networkService", networkServiceMock);;
        netUtilsMocked = Mockito.mockStatic(NetUtils.class);
    }

    @After
    public void tearDown() throws Exception {
        netUtilsMocked.close();
    }

    @Test
    public void validateCidrsTestValidCidrs(){
        ArrayList<String> listMock = new ArrayList<>();
        String cidrMock = "10.1.1.0/24";
        listMock.add(cidrMock);
        cmdMock.setSourceCidrList(listMock);
        cmdMock.setDestCidrList(listMock);

        long networkIdMock = 1234;
        Mockito.doReturn(networkIdMock).when(cmdMock).getNetworkId();

        Network networkMock = Mockito.mock(Network.class);
        Mockito.doReturn(networkMock).when(networkServiceMock).getNetwork(networkIdMock);

        Mockito.doReturn(cidrMock).when(networkMock).getCidr();

        netUtilsMocked.when(() -> NetUtils.isValidIp4Cidr(cidrMock)).thenReturn(true);
        netUtilsMocked.when(() -> NetUtils.isValidIp6Cidr(cidrMock)).thenReturn(false);
        netUtilsMocked.when(() -> NetUtils.isNetworkAWithinNetworkB(cidrMock,cidrMock)).thenReturn(true);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        netUtilsMocked.verify(() -> NetUtils.isValidIp4Cidr(cidrMock), Mockito.times(2));
        netUtilsMocked.verify(() -> NetUtils.isValidIp6Cidr(cidrMock), Mockito.never());
        netUtilsMocked.verify(() -> NetUtils.isNetworkAWithinNetworkB(cidrMock,cidrMock), Mockito.times(1));
    }

    @Test
    public void validateCidrsTestCidrsBeginningWithWhiteSpace(){
        ArrayList<String> listMock = new ArrayList<>();
        String cidrMock = "  10.1.1.0/24";
        listMock.add(cidrMock);
        cmdMock.setSourceCidrList(listMock);
        cmdMock.setDestCidrList(listMock);

        long networkIdMock = 1234;
        Mockito.doReturn(networkIdMock).when(cmdMock).getNetworkId();

        Network networkMock = Mockito.mock(Network.class);
        Mockito.doReturn(networkMock).when(networkServiceMock).getNetwork(networkIdMock);

        Mockito.doReturn("10.1.1.0/24").when(networkMock).getCidr();

        netUtilsMocked.when(() -> NetUtils.isValidIp4Cidr(Mockito.eq("10.1.1.0/24"))).thenReturn(true);
        netUtilsMocked.when(() -> NetUtils.isValidIp6Cidr(Mockito.eq("10.1.1.0/24"))).thenReturn(false);
        netUtilsMocked.when(() -> NetUtils.isNetworkAWithinNetworkB(Mockito.eq("10.1.1.0/24"), Mockito.eq("10.1.1.0/24"))).thenReturn(true);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        netUtilsMocked.verify(() -> NetUtils.isValidIp4Cidr(Mockito.eq("10.1.1.0/24")), Mockito.times(2));
        netUtilsMocked.verify(() -> NetUtils.isValidIp6Cidr(Mockito.eq("10.1.1.0/24")), Mockito.never());

        netUtilsMocked.verify(() -> NetUtils.isNetworkAWithinNetworkB(Mockito.eq("10.1.1.0/24"), Mockito.eq("10.1.1.0/24")));
    }

    @Test (expected = ServerApiException.class)
    public void validateCidrsTestInvalidSourceCidr(){
        ArrayList<String> listMock = new ArrayList<>();
        String sourceCidrMock = "aaaa";
        listMock.add(sourceCidrMock);
        cmdMock.setSourceCidrList(listMock);
        cmdMock.setDestCidrList(listMock);

        long networkIdMock = 1234;
        Mockito.doReturn(networkIdMock).when(cmdMock).getNetworkId();

        Network networkMock = Mockito.mock(Network.class);
        Mockito.doReturn(networkMock).when(networkServiceMock).getNetwork(networkIdMock);

        Mockito.doReturn("10.1.1.0/24").when(networkMock).getCidr();

        netUtilsMocked.when(() -> NetUtils.isValidIp4Cidr(sourceCidrMock)).thenReturn(false);
        netUtilsMocked.when(() -> NetUtils.isValidIp6Cidr(sourceCidrMock)).thenReturn(false);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        netUtilsMocked.verify(() -> NetUtils.isValidIp4Cidr(sourceCidrMock), Mockito.times(1));
        netUtilsMocked.verify(() -> NetUtils.isValidIp6Cidr(sourceCidrMock), Mockito.times(1));
    }

    @Test (expected = ServerApiException.class)
    public void validateCidrsTestInvalidDestinationCidr(){
        ArrayList<String> listSourceMock = new ArrayList<>();
        String sourceCidrMock = "10.1.1.0/24";
        listSourceMock.add(sourceCidrMock);
        cmdMock.setSourceCidrList(listSourceMock);

        ArrayList<String> listDestMock = new ArrayList<>();
        String destCidrMock = "aaaa";
        listDestMock.add(destCidrMock);
        cmdMock.setDestCidrList(listDestMock);

        long networkIdMock = 1234;
        Mockito.doReturn(networkIdMock).when(cmdMock).getNetworkId();

        Network networkMock = Mockito.mock(Network.class);
        Mockito.doReturn(networkMock).when(networkServiceMock).getNetwork(networkIdMock);

        Mockito.doReturn(sourceCidrMock).when(networkMock).getCidr();

        netUtilsMocked.when(() -> NetUtils.isValidIp4Cidr(Mockito.eq(sourceCidrMock))).thenReturn(true);
        netUtilsMocked.when(() -> NetUtils.isValidIp6Cidr(Mockito.eq(sourceCidrMock))).thenReturn(false);
        netUtilsMocked.when(() -> NetUtils.isNetworkAWithinNetworkB(sourceCidrMock, sourceCidrMock)).thenReturn(true);

        netUtilsMocked.when(() -> NetUtils.isValidIp4Cidr(Mockito.eq(destCidrMock))).thenReturn(false);
        netUtilsMocked.when(() -> NetUtils.isValidIp6Cidr(Mockito.eq(destCidrMock))).thenReturn(false);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        netUtilsMocked.verify(() -> NetUtils.isValidIp4Cidr(sourceCidrMock));
        netUtilsMocked.verify(() -> NetUtils.isValidIp6Cidr(sourceCidrMock));

        netUtilsMocked.verify(() -> NetUtils.isNetworkAWithinNetworkB(sourceCidrMock, sourceCidrMock));


        netUtilsMocked.verify(() -> NetUtils.isValidIp4Cidr(destCidrMock));
        netUtilsMocked.verify(() -> NetUtils.isValidIp6Cidr(destCidrMock));
    }

    @Test
    public void validateCidrsTestSourceCidrEqualsAllIp4(){
        ArrayList<String> listSourceMock = new ArrayList<>();
        String sourceCidrMock = "0.0.0.0/0";
        listSourceMock.add(sourceCidrMock);
        cmdMock.setSourceCidrList(listSourceMock);

        ArrayList<String> listDestMock = new ArrayList<>();
        String destCidrMock = "10.1.1.0/24";
        listDestMock.add(destCidrMock);
        cmdMock.setDestCidrList(listDestMock);

        long networkIdMock = 1234;
        Mockito.doReturn(networkIdMock).when(cmdMock).getNetworkId();

        Network networkMock = Mockito.mock(Network.class);
        Mockito.doReturn(networkMock).when(networkServiceMock).getNetwork(networkIdMock);

        Mockito.doReturn(sourceCidrMock).when(networkMock).getCidr();

        netUtilsMocked.when(() -> NetUtils.isValidIp4Cidr(Mockito.eq(sourceCidrMock))).thenReturn(true);
        netUtilsMocked.when(() -> NetUtils.isValidIp6Cidr(Mockito.eq(sourceCidrMock))).thenReturn(false);

        netUtilsMocked.when(() -> NetUtils.isValidIp4Cidr(Mockito.eq(destCidrMock))).thenReturn(true);
        netUtilsMocked.when(() -> NetUtils.isValidIp6Cidr(Mockito.eq(destCidrMock))).thenReturn(false);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        netUtilsMocked.verify(() -> NetUtils.isValidIp4Cidr(sourceCidrMock), Mockito.times(1));
        netUtilsMocked.verify(() -> NetUtils.isValidIp6Cidr(sourceCidrMock), Mockito.never());

        netUtilsMocked.verify(() -> NetUtils.isNetworkAWithinNetworkB(sourceCidrMock, sourceCidrMock), Mockito.never());

        netUtilsMocked.verify(() -> NetUtils.isValidIp4Cidr(destCidrMock));
        netUtilsMocked.verify(() -> NetUtils.isValidIp6Cidr(destCidrMock), Mockito.never());
    }

    @Test(expected = ServerApiException.class)
    public void validateCidrsTestSourceCidrNotWithinNetwork() {
        ArrayList<String> listMock = new ArrayList<>();
        String cidrMock = "10.1.1.0/24";
        listMock.add(cidrMock);
        cmdMock.setSourceCidrList(listMock);
        cmdMock.setDestCidrList(listMock);

        long networkIdMock = 1234;
        Mockito.doReturn(networkIdMock).when(cmdMock).getNetworkId();

        Network networkMock = Mockito.mock(Network.class);
        Mockito.doReturn(networkMock).when(networkServiceMock).getNetwork(networkIdMock);

        Mockito.doReturn(cidrMock).when(networkMock).getCidr();

        netUtilsMocked.when(() -> NetUtils.isValidIp4Cidr(cidrMock)).thenReturn(true);
        netUtilsMocked.when(() -> NetUtils.isValidIp6Cidr(cidrMock)).thenReturn(false);
        netUtilsMocked.when(() -> NetUtils.isNetworkAWithinNetworkB(cidrMock, cidrMock)).thenReturn(false);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        netUtilsMocked.verify(() -> NetUtils.isValidIp4Cidr(cidrMock));
        netUtilsMocked.verify(() -> NetUtils.isValidIp6Cidr(cidrMock));

        netUtilsMocked.verify(() -> NetUtils.isNetworkAWithinNetworkB(cidrMock, cidrMock));
    }

    @Test
    public void validateCidrsTestNullDestinationCidr(){
        ArrayList<String> listSourceMock = new ArrayList<>();
        String sourceCidrMock = "10.1.1.0/24";
        listSourceMock.add(sourceCidrMock);
        cmdMock.setSourceCidrList(listSourceMock);

        long networkIdMock = 1234;
        Mockito.doReturn(networkIdMock).when(cmdMock).getNetworkId();

        Network networkMock = Mockito.mock(Network.class);
        Mockito.doReturn(networkMock).when(networkServiceMock).getNetwork(networkIdMock);

        Mockito.doReturn(sourceCidrMock).when(networkMock).getCidr();

        netUtilsMocked.when(() -> NetUtils.isValidIp4Cidr(Mockito.eq(sourceCidrMock))).thenReturn(true);
        netUtilsMocked.when(() -> NetUtils.isValidIp6Cidr(Mockito.eq(sourceCidrMock))).thenReturn(false);
        netUtilsMocked.when(() -> NetUtils.isNetworkAWithinNetworkB(sourceCidrMock, sourceCidrMock)).thenReturn(true);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        netUtilsMocked.verify(() -> NetUtils.isValidIp4Cidr(sourceCidrMock));
        netUtilsMocked.verify(() -> NetUtils.isValidIp6Cidr(sourceCidrMock), Mockito.never());

        netUtilsMocked.verify(() -> NetUtils.isNetworkAWithinNetworkB(sourceCidrMock, sourceCidrMock));

        netUtilsMocked.verify(() -> NetUtils.isValidIp4Cidr(null), Mockito.never());
        netUtilsMocked.verify(() -> NetUtils.isValidIp6Cidr(null), Mockito.never());
    }
}
