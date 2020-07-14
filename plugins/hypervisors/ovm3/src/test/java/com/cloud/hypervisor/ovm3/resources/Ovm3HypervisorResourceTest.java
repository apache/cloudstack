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

package com.cloud.hypervisor.ovm3.resources;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.Host;
import com.cloud.hypervisor.ovm3.objects.CloudStackPluginTest;
import com.cloud.hypervisor.ovm3.objects.ConnectionTest;
import com.cloud.hypervisor.ovm3.objects.NetworkTest;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.OvmObject;
import com.cloud.hypervisor.ovm3.objects.Xen;
import com.cloud.hypervisor.ovm3.objects.XenTest;
import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3Configuration;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3ConfigurationTest;
import com.cloud.hypervisor.ovm3.support.Ovm3SupportTest;
import com.cloud.network.Networks;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;

public class Ovm3HypervisorResourceTest {
    ConnectionTest con;
    OvmObject ovmObject = new OvmObject();
    XmlTestResultTest results = new XmlTestResultTest();
    Ovm3ConfigurationTest configTest = new Ovm3ConfigurationTest();
    Ovm3HypervisorResource hypervisor = new Ovm3HypervisorResource();
    Ovm3SupportTest support = new Ovm3SupportTest();
    NetworkTest net = new NetworkTest();
    // LinuxTest linux = new LinuxTest();
    CloudStackPluginTest csp = new CloudStackPluginTest();
    XenTest xen = new XenTest();
    String currentStatus = "put";
    String vmName = "i-2-3-VM";

    @Test
    public void configureTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con = support.prepConnectionResults();
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
    }

    /* fails */
    /*
     @Test(expected = CloudRuntimeException.class)
     public void configureFailBaseConnectionTest() throws
         ConfigurationException {
         hypervisor = support.prepare(configTest.getParams());
         results.basicBooleanTest(hypervisor.configure(con.getHostName(),
                 configTest.getParams()));
     }
    */

    @Test
    public void configureControlInterfaceTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        String netdef = net.getDiscoverNetwork();
        netdef = netdef.replaceAll(config.getAgentControlNetworkName(),
                "thisisnotit0");
        con = support.prepConnectionResults();
        con.removeMethodResponse("discover_network");
        con.setResult(results.simpleResponseWrapWrapper(netdef));
        con.addResult(results.simpleResponseWrapWrapper(net
                .getDiscoverNetwork()));
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
    }

    @Test
    public void startCommandTest() {
    }

    @Test
    public void getCurrentStatusAndConfigureTest()
            throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con = support.prepConnectionResults();
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
        assertNotNull(hypervisor.getCurrentStatus(1L));
        assertNotNull(hypervisor.getCurrentStatus(1L));
    }

    @Test
    public void getCurrentStatusFailTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con = support.prepConnectionResults();
        con.setResult(results.simpleResponseWrapWrapper("fail"));
        con.removeMethodResponse("echo");
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
        assertNull(hypervisor.getCurrentStatus(1L));
    }

    @Test
    public void getCurrentStatusExceptionTest() throws ConfigurationException {
        con = new ConnectionTest();
        hypervisor.setConnection(con);
        assertNull(hypervisor.getCurrentStatus(1L));
    }

    /* gives an IOException on ssh */
    @Test
    public void initializeTest() throws Exception {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con = support.prepConnectionResults();
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
        con.setIp(config.getAgentIp());
        for (StartupCommand start : hypervisor.initialize()) {
            assertNotNull(start);
        }
    }

    private Ovm3HypervisorResource vmActionPreparation()
            throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con = support.prepConnectionResults();
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
        return hypervisor;
    }

    private Boolean rebootVm(String name) throws ConfigurationException {
        hypervisor = vmActionPreparation();
        con.removeMethodResponse("list_vms");
        con.addResult(xen.getMultipleVmsListXML());
        con.addResult(xen.getMultipleVmsListXML());
        RebootCommand cmd = new RebootCommand(name, true);
        Answer ra = hypervisor.executeRequest(cmd);
        return ra.getResult();
    }

    @Test
    public void rebootCommandTest() throws ConfigurationException {
        results.basicBooleanTest(rebootVm(vmName));
    }

    @Test
    public void rebootCommandFailTest() throws ConfigurationException {
        results.basicBooleanTest(rebootVm("bogus"), false);
    }

    @Test
    public void stopVmTest() throws ConfigurationException {
        hypervisor = vmActionPreparation();
        con.removeMethodResponse("list_vms");
        con.setResult(xen.getMultipleVmsListXML());
        con.addResult(xen.getMultipleVmsListXML().replace(vmName,
                vmName + "-hide"));
        StopCommand cmd = new StopCommand(vmName, true, true);
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult());
    }

    /* takes too long */
    /*
     * @Test
     * public void stopVmTestFail() throws ConfigurationException {
     * stopVm();
     * con.addResult(xen.getVmListXML().replace(vmName, vmName));
     * StopCommand cmd = new StopCommand(vmName, true, true);
     * StopAnswer ra = hypervisor.execute(cmd);
     * results.basicBooleanTest(ra.getResult(), false);
     * }
     */

    @Test
    public void stopVmTreatAsStoppedTest() throws ConfigurationException {
        hypervisor = vmActionPreparation();
        con.setMethodResponse("list_vms",
                xen.getMultipleVmsListXML().replace(vmName, vmName + "-hide"));
        StopCommand cmd = new StopCommand(vmName, true, true);
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult());
    }

    @Test
    public void stopVmExceptionTest() throws ConfigurationException {
        hypervisor = vmActionPreparation();
        con.removeMethodResponse("list_vms");
        StopCommand cmd = new StopCommand(vmName, true, true);
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult(), false);
    }

    /* not relevant atm */
    public void addNicsToSpec(VirtualMachineTO vmspec, List<String> list) {
        for (String vif : list) {
            String parts[] = vif.split("[,=.]+");
            addNicToSpec(vmspec, parts[1], parts[3], parts[4]);
        }
    }

    public void addNicToSpec(VirtualMachineTO vmspec, String mac,
            String bridge, String vlan) {
        ArrayList<NicTO> nics;
        if (vmspec.getNics() != null) {
            nics = new ArrayList<NicTO>(Arrays.asList(vmspec.getNics()));
        } else {
            nics = new ArrayList<NicTO>();
        }
        NicTO nic = new NicTO();

        nic.setType(Networks.TrafficType.Guest);
        nic.setMac(mac);
        nic.setDeviceId(nics.size());
        nics.add(nic);
        vmspec.setNics(nics.toArray(new NicTO[nics.size()]));
    }

    /* hardcoded, dirty */
    public void addDisksToSpec(VirtualMachineTO vmspec, List<String> list) {
        for (String disk : list) {
            String parts[] = disk.split("[:,.]+");
            String partdeux[] = parts[1].split("/");
            String diskuuid = partdeux[partdeux.length - 1];
            String dsuuid = partdeux[3];
            String path = parts[1].replace("/" + diskuuid, "");
            addDiskToSpec(vmspec, diskuuid, dsuuid, path);
        }
    }

    public void addDiskToSpec(VirtualMachineTO vmspec, String uuid,
            String dsuuid, String path) {
        ArrayList<DiskTO> disks;
        if (vmspec.getDisks() != null) {
            disks = new ArrayList<DiskTO>(Arrays.asList(vmspec.getDisks()));
        } else {
            disks = new ArrayList<DiskTO>();
        }
        DiskTO disk = new DiskTO();
        VolumeObjectTO volume = new VolumeObjectTO();
        NfsTO nfsDataStore = new NfsTO();
        nfsDataStore.setUuid(dsuuid);
        volume.setDataStore(nfsDataStore);
        volume.setPath(path);
        volume.setUuid(uuid);
        disk.setData(volume);
        disk.setType(Volume.Type.ROOT);
        disks.add(disk);
        vmspec.setDisks(disks.toArray(new DiskTO[disks.size()]));
    }

    public Host getHost(String ip) {
        Host host = Mockito.mock(Host.class);
        Mockito.when(host.getPrivateIpAddress()).thenReturn(ip);
        return host;
    }

    public VirtualMachineTO createVm(String vmName) throws Ovm3ResourceException {
        con = support.prepConnectionResults();
        Xen vdata = new Xen(con);
        Xen.Vm vm = vdata.getVmConfig(vmName);
        vdata.listVm(xen.getRepoId(), xen.getVmId());

        // Ovm3VmGuestTypes types = new Ovm3VmGuestTypes();
        Long id = 1L;
        String instanceName = vm.getVmName();
        VirtualMachine.Type type = Type.User;
        int cpus = 1; // vm.getVmCpus();
        Integer speed = 0;
        long minRam = vm.getVmMemory();
        long maxRam = vm.getVmMemory();
        BootloaderType bootloader = BootloaderType.PyGrub;
        String os = "Oracle Enterprise Linux 6.0 (64-bit)";
        boolean enableHA = true;
        boolean limitCpuUse = false;
        String vncPassword = "gobbeldygoo";
        VirtualMachineTO vmspec = new VirtualMachineTO(id, instanceName, type,
                cpus, speed, minRam, maxRam, bootloader, os, enableHA,
                limitCpuUse, vncPassword);
        vmspec.setBootArgs("");
        addDisksToSpec(vmspec, vm.getVmDisks());
        addNicsToSpec(vmspec, vm.getVmVifs());
        return vmspec;
    }

    @Test
    public void createVmTest() throws ConfigurationException,
            Ovm3ResourceException {
        VirtualMachineTO vmspec = createVm(vmName);
        hypervisor = vmActionPreparation();
        StartCommand cmd = new StartCommand(vmspec,
                getHost(hypervisor.getName()), true);
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult());
    }

    @Test
    public void createOtherVmTest() throws ConfigurationException,
            Ovm3ResourceException {
        VirtualMachineTO vmspec = createVm(vmName);
        vmspec.setOs("bogus");
        hypervisor = vmActionPreparation();
        StartCommand cmd = new StartCommand(vmspec,
                getHost(hypervisor.getName()), true);
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult());
    }

    @Test
    public void startResourceTest() {
        results.basicBooleanTest(hypervisor.start());
    }

    @Test
    public void stopResourceTest() {
        results.basicBooleanTest(hypervisor.stop());
    }
    @Test
    public void readyCommandTest() throws ConfigurationException {
        hypervisor = support.prepare(configTest.getParams());
        ReadyCommand ready = new ReadyCommand();
        Answer ra = hypervisor.executeRequest(ready);
        results.basicBooleanTest(ra.getResult());
    }
}
