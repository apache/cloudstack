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
package com.cloud.hypervisor.kvm.resource;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.network.Networks;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class BridgeVifDriverTest {

    private static final String BRIDGE_NAME = "cloudbr1";
    private static final String MODIFY_BRDR = "/scripts/modifybrdr.sh";
    private static final String MODIFY_MACIP = "/scripts/modifymacip.sh";

    @Spy
    @InjectMocks
    private BridgeVifDriver driver = new BridgeVifDriver();

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = BridgeVifDriver.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Feeds the given text to the OutputInterpreter a Script.execute(parser) call passes, the
     * way Script does with the merged stdout/stderr of the real process, and reports success.
     */
    private static Answer<String> scriptOutput(String output) {
        return invocation -> {
            OutputInterpreter parser = invocation.getArgument(0);
            parser.interpret(new BufferedReader(new StringReader(output)));
            return null;
        };
    }

    private static MockedConstruction<Script> mockScriptOutput(String output) {
        return Mockito.mockConstruction(Script.class, (mock, context) ->
                Mockito.lenient().when(mock.execute(Mockito.any(OutputInterpreter.class))).thenAnswer(scriptOutput(output)));
    }

    private static MockedConstruction<Script> mockScriptFailure(String error) {
        return Mockito.mockConstruction(Script.class, (mock, context) ->
                Mockito.lenient().when(mock.execute(Mockito.any(OutputInterpreter.class))).thenReturn(error));
    }

    private NicTO directRoutedNic() throws URISyntaxException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Routed);
        nic.setBroadcastUri(new URI("routed://600"));
        nic.setMac("02:00:00:00:00:01");
        nic.setGateway("169.254.0.1");
        nic.setIp6Gateway("fe80::1");
        return nic;
    }

    private void configureDirectRoutedScripts() throws Exception {
        setField(driver, "_modifyBrdrPath", MODIFY_BRDR);
        setField(driver, "_macIpScriptPath", MODIFY_MACIP);
    }

    @Test
    public void isBroadcastTypeVlanOrVxlan() {
        final NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Native);
        Assert.assertFalse(driver.isBroadcastTypeVlanOrVxlan(null));
        Assert.assertFalse(driver.isBroadcastTypeVlanOrVxlan(nic));
        // Test VLAN
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        Assert.assertTrue(driver.isBroadcastTypeVlanOrVxlan(nic));
        // Test VXLAN
        nic.setBroadcastType(Networks.BroadcastDomainType.Vxlan);
        Assert.assertTrue(driver.isBroadcastTypeVlanOrVxlan(nic));
    }

    @Test
    public void isValidProtocolAndVnetId() {
        Assert.assertFalse(driver.isValidProtocolAndVnetId(null, null));
        Assert.assertFalse(driver.isValidProtocolAndVnetId("123", null));
        Assert.assertFalse(driver.isValidProtocolAndVnetId(null, "vlan"));
        Assert.assertFalse(driver.isValidProtocolAndVnetId("untagged", "vxlan"));
        Assert.assertTrue(driver.isValidProtocolAndVnetId("123", "vlan"));
        Assert.assertTrue(driver.isValidProtocolAndVnetId("456", "vxlan"));
    }

    @Test
    public void createStorageVnetBridgeIfNeededReturnsStorageBrNameWhenBroadcastTypeIsNotStorageButValidValues() throws InternalErrorException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Storage);
        int vlan = 123;
        String newBridge = "br-" + vlan;
        nic.setBroadcastUri(Networks.BroadcastDomainType.Storage.toUri(vlan));
        Mockito.doReturn(newBridge).when(driver).createVnetBr(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        String result = driver.createStorageVnetBridgeIfNeeded(nic, "trafficLabel", BRIDGE_NAME);
        Assert.assertEquals(newBridge, result);
    }

    @Test
    public void createStorageVnetBridgeIfNeededReturnsStorageBrNameWhenBroadcastTypeIsNotStorage() throws InternalErrorException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        String result = driver.createStorageVnetBridgeIfNeeded(nic, "trafficLabel", BRIDGE_NAME);
        Assert.assertEquals(BRIDGE_NAME, result);
    }

    @Test
    public void createStorageVnetBridgeIfNeededReturnsStorageBrNameWhenBroadcastUriIsNull() throws InternalErrorException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Storage);
        String result = driver.createStorageVnetBridgeIfNeeded(nic,  "trafficLabel", BRIDGE_NAME);
        Assert.assertEquals(BRIDGE_NAME, result);
    }

    @Test
    public void createStorageVnetBridgeIfNeededCreatesVnetBridgeWhenUntaggedVlan() throws InternalErrorException, URISyntaxException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Storage);
        nic.setBroadcastUri(new URI(Networks.BroadcastDomainType.Storage.scheme() + "://untagged"));
        String result = driver.createStorageVnetBridgeIfNeeded(nic, "trafficLabel", BRIDGE_NAME);
        Assert.assertEquals(BRIDGE_NAME, result);
    }

    @Test
    public void isDirectRoutedNicRecognisesBroadcastType() throws URISyntaxException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Routed);
        nic.setBroadcastUri(new URI("routed://600"));
        Assert.assertTrue(BridgeVifDriver.isDirectRoutedNic(nic));
    }

    /**
     * Regression: a SystemVM's public NIC is rebuilt at start with the shared Public network's
     * broadcast domain type (Vlan) — only its own broadcast URI carries routed://. It must still
     * be recognised, or the agent runs modifyvlan.sh on the routed id and skips modifymacip.sh.
     */
    @Test
    public void isDirectRoutedNicRecognisesRoutedUriWithForeignBroadcastType() throws URISyntaxException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        nic.setBroadcastUri(new URI("routed://600"));
        Assert.assertTrue(BridgeVifDriver.isDirectRoutedNic(nic));
    }

    @Test
    public void isDirectRoutedNicRejectsOrdinaryNics() throws URISyntaxException {
        Assert.assertFalse(BridgeVifDriver.isDirectRoutedNic(null));
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        Assert.assertFalse(BridgeVifDriver.isDirectRoutedNic(nic));
        nic.setBroadcastUri(new URI("vlan://600"));
        Assert.assertFalse(BridgeVifDriver.isDirectRoutedNic(nic));
    }

    @Test
    public void lastNonBlankLineTakesTheLastLineWithContent() {
        Assert.assertNull(BridgeVifDriver.lastNonBlankLine(null));
        Assert.assertNull(BridgeVifDriver.lastNonBlankLine(""));
        Assert.assertNull(BridgeVifDriver.lastNonBlankLine(" \n\n  \n"));
        Assert.assertEquals("brdr-600", BridgeVifDriver.lastNonBlankLine("brdr-600\n"));
        Assert.assertEquals("brdr-600", BridgeVifDriver.lastNonBlankLine("sysctl: cannot stat /proc/sys/net/ipv6/conf/brdr-600/disable_ipv6\n  brdr-600  \n\n"));
    }

    @Test
    public void createDirectRoutedBridgeUsesLastLineOfScriptOutput() throws Exception {
        configureDirectRoutedScripts();
        try (MockedConstruction<Script> scripts = mockScriptOutput("sysctl: cannot stat /proc/sys/net/ipv6/conf/brdr-600/disable_ipv6\nbrdr-600\n")) {
            Assert.assertEquals("brdr-600", driver.createDirectRoutedBridge(directRoutedNic()));
            Assert.assertEquals(1, scripts.constructed().size());
            Script script = scripts.constructed().get(0);
            Mockito.verify(script).add("-o", "add");
            Mockito.verify(script).add("-n", "600");
            Mockito.verify(script).add("-4", "169.254.0.1");
            Mockito.verify(script).add("-6", "fe80::1");
        }
    }

    @Test
    public void createDirectRoutedBridgeRejectsOutputThatIsNotAnInterfaceName() throws Exception {
        configureDirectRoutedScripts();
        try (MockedConstruction<Script> ignored = mockScriptOutput("sysctl: cannot stat /proc/sys/net/ipv6/conf/brdr-600/disable_ipv6\n")) {
            driver.createDirectRoutedBridge(directRoutedNic());
            Assert.fail("a diagnostic must not be accepted as the bridge name");
        } catch (InternalErrorException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("cannot stat"));
        }
        try (MockedConstruction<Script> ignored = mockScriptOutput("a-name-that-is-far-too-long-for-an-interface\n")) {
            driver.createDirectRoutedBridge(directRoutedNic());
            Assert.fail("an over-long name must not be accepted as the bridge name");
        } catch (InternalErrorException expected) {
        }
        try (MockedConstruction<Script> ignored = mockScriptOutput("")) {
            driver.createDirectRoutedBridge(directRoutedNic());
            Assert.fail("empty output must not be accepted");
        } catch (InternalErrorException expected) {
        }
    }

    @Test(expected = InternalErrorException.class)
    public void createDirectRoutedBridgeFailsWhenScriptFails() throws Exception {
        configureDirectRoutedScripts();
        try (MockedConstruction<Script> ignored = mockScriptFailure("modifybrdr.sh: command failed: ip link add name brdr-600 type bridge stp_state 0 forward_delay 0")) {
            driver.createDirectRoutedBridge(directRoutedNic());
        }
    }

    @Test
    public void createDirectRoutedBridgeRequiresBothScripts() throws Exception {
        setField(driver, "_modifyBrdrPath", MODIFY_BRDR);
        setField(driver, "_macIpScriptPath", null);
        try {
            driver.createDirectRoutedBridge(directRoutedNic());
            Assert.fail("a missing modifymacip.sh must be fatal for a Direct Routed NIC");
        } catch (InternalErrorException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("modifymacip.sh"));
        }
        setField(driver, "_modifyBrdrPath", null);
        setField(driver, "_macIpScriptPath", MODIFY_MACIP);
        try {
            driver.createDirectRoutedBridge(directRoutedNic());
            Assert.fail("a missing modifybrdr.sh must be fatal for a Direct Routed NIC");
        } catch (InternalErrorException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("modifybrdr.sh"));
        }
    }

    @Test
    public void deleteDirectRoutedBridgeAcceptsOnlyTheThreeVerdicts() throws Exception {
        configureDirectRoutedScripts();
        for (String verdict : new String[] {"notmine", "kept", "deleted"}) {
            try (MockedConstruction<Script> ignored = mockScriptOutput("ip: some diagnostic\n" + verdict + "\n")) {
                Assert.assertTrue(verdict, driver.deleteDirectRoutedBridge("brdr-600"));
            }
        }
        try (MockedConstruction<Script> ignored = mockScriptOutput("RTNETLINK answers: Operation not permitted\n")) {
            Assert.assertFalse(driver.deleteDirectRoutedBridge("brdr-600"));
        }
        try (MockedConstruction<Script> ignored = mockScriptOutput("")) {
            Assert.assertFalse(driver.deleteDirectRoutedBridge("brdr-600"));
        }
        try (MockedConstruction<Script> ignored = mockScriptFailure("modifybrdr.sh: command failed: ip link delete brdr-600 type bridge")) {
            Assert.assertFalse(driver.deleteDirectRoutedBridge("brdr-600"));
        }
    }

    @Test
    public void isDirectRoutedBridgeAcceptsOnlyMine() throws Exception {
        configureDirectRoutedScripts();
        try (MockedConstruction<Script> scripts = mockScriptOutput("mine\n")) {
            Assert.assertTrue(driver.isDirectRoutedBridge("brdr-600"));
            Mockito.verify(scripts.constructed().get(0)).add("-o", "query");
            Mockito.verify(scripts.constructed().get(0)).add("-b", "brdr-600");
        }
        try (MockedConstruction<Script> ignored = mockScriptOutput("notmine\n")) {
            Assert.assertFalse(driver.isDirectRoutedBridge("cloudbr0"));
        }
        try (MockedConstruction<Script> ignored = mockScriptOutput("something else\n")) {
            Assert.assertFalse(driver.isDirectRoutedBridge("brdr-600"));
        }
        try (MockedConstruction<Script> ignored = mockScriptFailure(Script.ERR_TIMEOUT)) {
            Assert.assertFalse(driver.isDirectRoutedBridge("brdr-600"));
        }
        setField(driver, "_modifyBrdrPath", null);
        Assert.assertFalse(driver.isDirectRoutedBridge("brdr-600"));
    }

    /**
     * On unplug of a Direct Routed interface the host route and neighbour entry must go before
     * the bridge they reference may be deleted: query, then modifymacip.sh delete, then
     * modifybrdr.sh delete, and no regular vnet bridge handling.
     */
    @Test
    public void unplugRemovesMacIpEntriesBeforeDeletingDirectRoutedBridge() throws Exception {
        configureDirectRoutedScripts();
        LibvirtVMDef.InterfaceDef iface = new LibvirtVMDef.InterfaceDef();
        iface.defBridgeNet("brdr-600", null, "02:00:00:00:00:01", LibvirtVMDef.InterfaceDef.NicModel.VIRTIO);
        try (MockedConstruction<Script> scripts = Mockito.mockConstruction(Script.class, (mock, context) -> {
            String output = context.getCount() == 1 ? "mine\n" : "deleted\n";
            Mockito.lenient().when(mock.execute(Mockito.any(OutputInterpreter.class))).thenAnswer(scriptOutput(output));
        })) {
            driver.unplug(iface, true);
            List<Script> constructed = scripts.constructed();
            Assert.assertEquals(3, constructed.size());
            Mockito.verify(constructed.get(0)).add("-o", "query");
            Mockito.verify(constructed.get(0)).add("-b", "brdr-600");
            Mockito.verify(constructed.get(1)).add("-o", "delete");
            Mockito.verify(constructed.get(1)).add("-m", "02:00:00:00:00:01");
            Mockito.verify(constructed.get(1)).execute();
            Mockito.verify(constructed.get(2)).add("-o", "delete");
            Mockito.verify(constructed.get(2)).add("-b", "brdr-600");
        }
    }
}
