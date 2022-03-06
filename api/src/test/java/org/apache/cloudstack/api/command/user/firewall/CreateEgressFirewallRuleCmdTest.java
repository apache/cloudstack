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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;

@PrepareForTest(NetUtils.class)
@RunWith(PowerMockRunner.class)
public class CreateEgressFirewallRuleCmdTest {

    @Mock
    NetworkService networkServiceMock;

    @Spy
    @InjectMocks
    CreateEgressFirewallRuleCmd cmdMock = new CreateEgressFirewallRuleCmd();

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

        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.isValidIp4Cidr(cidrMock)).thenReturn(true);
        PowerMockito.when(NetUtils.isValidIp6Cidr(cidrMock)).thenReturn(false);
        PowerMockito.when(NetUtils.isNetworkAWithinNetworkB(cidrMock,cidrMock)).thenReturn(true);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        PowerMockito.verifyStatic(NetUtils.class, Mockito.times(2));
        NetUtils.isValidIp4Cidr(cidrMock);
        NetUtils.isValidIp6Cidr(cidrMock);

        PowerMockito.verifyStatic(NetUtils.class);
        NetUtils.isNetworkAWithinNetworkB(cidrMock,cidrMock);
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

        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.isValidIp4Cidr(Mockito.eq("10.1.1.0/24"))).thenReturn(true);
        PowerMockito.when(NetUtils.isValidIp6Cidr(Mockito.eq("10.1.1.0/24"))).thenReturn(false);
        PowerMockito.when(NetUtils.isNetworkAWithinNetworkB(Mockito.eq("10.1.1.0/24"), Mockito.eq("10.1.1.0/24"))).thenReturn(true);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        PowerMockito.verifyStatic(NetUtils.class, Mockito.times(2));
        NetUtils.isValidIp4Cidr(Mockito.eq("10.1.1.0/24"));
        NetUtils.isValidIp6Cidr(Mockito.eq("10.1.1.0/24"));

        PowerMockito.verifyStatic(NetUtils.class);
        NetUtils.isNetworkAWithinNetworkB(Mockito.eq("10.1.1.0/24"), Mockito.eq("10.1.1.0/24"));
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

        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.isValidIp4Cidr(sourceCidrMock)).thenReturn(false);
        PowerMockito.when(NetUtils.isValidIp6Cidr(sourceCidrMock)).thenReturn(false);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        PowerMockito.verifyStatic(NetUtils.class, Mockito.times(1));
        NetUtils.isValidIp4Cidr(sourceCidrMock);
        NetUtils.isValidIp6Cidr(sourceCidrMock);
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

        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.isValidIp4Cidr(Mockito.eq(sourceCidrMock))).thenReturn(true);
        PowerMockito.when(NetUtils.isValidIp6Cidr(Mockito.eq(sourceCidrMock))).thenReturn(false);
        PowerMockito.when(NetUtils.isNetworkAWithinNetworkB(sourceCidrMock, sourceCidrMock)).thenReturn(true);

        PowerMockito.when(NetUtils.isValidIp4Cidr(Mockito.eq(destCidrMock))).thenReturn(false);
        PowerMockito.when(NetUtils.isValidIp6Cidr(Mockito.eq(destCidrMock))).thenReturn(false);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        PowerMockito.verifyStatic(NetUtils.class);
        NetUtils.isValidIp4Cidr(sourceCidrMock);
        NetUtils.isValidIp6Cidr(sourceCidrMock);

        PowerMockito.verifyStatic(NetUtils.class);
        NetUtils.isNetworkAWithinNetworkB(sourceCidrMock,sourceCidrMock);

        PowerMockito.verifyStatic(NetUtils.class);
        NetUtils.isValidIp4Cidr(destCidrMock);
        NetUtils.isValidIp6Cidr(destCidrMock);
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

        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.isValidIp4Cidr(Mockito.eq(sourceCidrMock))).thenReturn(true);
        PowerMockito.when(NetUtils.isValidIp6Cidr(Mockito.eq(sourceCidrMock))).thenReturn(false);

        PowerMockito.when(NetUtils.isValidIp4Cidr(Mockito.eq(destCidrMock))).thenReturn(true);
        PowerMockito.when(NetUtils.isValidIp6Cidr(Mockito.eq(destCidrMock))).thenReturn(false);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        PowerMockito.verifyStatic(NetUtils.class);
        NetUtils.isValidIp4Cidr(sourceCidrMock);
        NetUtils.isValidIp6Cidr(sourceCidrMock);

        PowerMockito.verifyStatic(NetUtils.class, Mockito.never());
        NetUtils.isNetworkAWithinNetworkB(sourceCidrMock,sourceCidrMock);

        PowerMockito.verifyStatic(NetUtils.class);
        NetUtils.isValidIp4Cidr(destCidrMock);
        NetUtils.isValidIp6Cidr(destCidrMock);
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

        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.isValidIp4Cidr(cidrMock)).thenReturn(true);
        PowerMockito.when(NetUtils.isValidIp6Cidr(cidrMock)).thenReturn(false);
        PowerMockito.when(NetUtils.isNetworkAWithinNetworkB(cidrMock, cidrMock)).thenReturn(false);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        PowerMockito.verifyStatic(NetUtils.class, Mockito.times(1));
        NetUtils.isValidIp4Cidr(cidrMock);
        NetUtils.isValidIp6Cidr(cidrMock);

        PowerMockito.verifyStatic(NetUtils.class);
        NetUtils.isNetworkAWithinNetworkB(cidrMock, cidrMock);
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

        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.isValidIp4Cidr(Mockito.eq(sourceCidrMock))).thenReturn(true);
        PowerMockito.when(NetUtils.isValidIp6Cidr(Mockito.eq(sourceCidrMock))).thenReturn(false);
        PowerMockito.when(NetUtils.isNetworkAWithinNetworkB(sourceCidrMock, sourceCidrMock)).thenReturn(true);

        cmdMock.validateCidrs();

        Mockito.verify(cmdMock, Mockito.atLeast(2)).getSourceCidrList();
        Mockito.verify(cmdMock).getNetworkId();
        Mockito.verify(networkServiceMock).getNetwork(networkIdMock);
        Mockito.verify(networkMock).getCidr();

        PowerMockito.verifyStatic(NetUtils.class);
        NetUtils.isValidIp4Cidr(sourceCidrMock);
        NetUtils.isValidIp6Cidr(sourceCidrMock);

        PowerMockito.verifyStatic(NetUtils.class);
        NetUtils.isNetworkAWithinNetworkB(sourceCidrMock,sourceCidrMock);

        PowerMockito.verifyStatic(NetUtils.class, Mockito.never());
        NetUtils.isValidIp4Cidr(null);
        NetUtils.isValidIp6Cidr(null);
    }
}
