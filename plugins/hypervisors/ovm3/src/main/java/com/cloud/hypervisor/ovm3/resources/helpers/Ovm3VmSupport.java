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

package com.cloud.hypervisor.ovm3.resources.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.ovm3.objects.CloudstackPlugin;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.OvmObject;
import com.cloud.hypervisor.ovm3.objects.Xen;
import com.cloud.hypervisor.ovm3.resources.Ovm3StorageProcessor;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Volume;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine.State;

public class Ovm3VmSupport {
    private final Logger LOGGER = Logger.getLogger(Ovm3VmSupport.class);
    private OvmObject ovmObject = new OvmObject();
    private ResourceManager resourceMgr;
    private Connection c;
    private Ovm3HypervisorNetwork network;
    private Ovm3Configuration config;
    private Ovm3HypervisorSupport hypervisor;
    private Ovm3StorageProcessor processor;
    private Ovm3StoragePool pool;
    private final Map<String, Map<String, String>> vmStats = new ConcurrentHashMap<String, Map<String, String>>();
    public Ovm3VmSupport(Connection conn,
            Ovm3Configuration ovm3config,
            Ovm3HypervisorSupport ovm3hyper,
            Ovm3StorageProcessor ovm3stp,
            Ovm3StoragePool ovm3sp,
            Ovm3HypervisorNetwork ovm3hvn) {
        c = conn;
        config = ovm3config;
        hypervisor = ovm3hyper;
        pool = ovm3sp;
        processor = ovm3stp;
        network = ovm3hvn;
    }
    public Boolean createVifs(Xen.Vm vm, VirtualMachineTO spec)
            throws Ovm3ResourceException {
        if (spec.getNics() != null) {
            NicTO[] nics = spec.getNics();
            return createVifs(vm, nics);
        } else {
            LOGGER.info("No nics for vm " + spec.getName());
            return false;
        }
    }

    private Boolean createVifs(Xen.Vm vm, NicTO[] nics)
            throws Ovm3ResourceException {
        for (NicTO nic : nics) {
            if (!createVif(vm, nic)) {
                return false;
            }
        }
        return true;
    }

    /* should add bitrates and latency... */
    private Boolean createVif(Xen.Vm vm, NicTO nic)
            throws Ovm3ResourceException {
        try {
            String net = network.getNetwork(nic);
            if (net != null) {
                LOGGER.debug("Adding vif " + nic.getDeviceId() + " "
                        + nic.getMac() + " " + net + " to " + vm.getVmName());
                vm.addVif(nic.getDeviceId(), net, nic.getMac());
            } else {
                LOGGER.debug("Unable to add vif " + nic.getDeviceId()
                        + " no network for " + vm.getVmName());
                return false;
            }
        } catch (Exception e) {
            String msg = "Unable to add vif " + nic.getType() + " for "
                    + vm.getVmName() + " " + e.getMessage();
            LOGGER.debug(msg);
            throw new Ovm3ResourceException(msg);
        }
        return true;
    }
    private Boolean deleteVif(Xen.Vm vm, NicTO nic)
            throws Ovm3ResourceException {
        /* here we should use the housekeeping of VLANs/Networks etc..
         * so we can clean after the last VM is gone
         */
        try {
            String net = network.getNetwork(nic);
            if (net != null) {
                LOGGER.debug("Removing vif " + nic.getDeviceId() + " " + " "
                        + nic.getMac() + " " + net + " from " + vm.getVmName());
                vm.removeVif(net, nic.getMac());
            } else {
                LOGGER.debug("Unable to remove vif " + nic.getDeviceId()
                        + " no network for " + vm.getVmName());
                return false;
            }
        } catch (Exception e) {
            String msg = "Unable to remove vif " + nic.getType() + " for "
                    + vm.getVmName() + " " + e.getMessage();
            LOGGER.debug(msg);
            throw new Ovm3ResourceException(msg);
        }
        return true;
    }

    /* Migration should make sure both HVs are the same ? */
    public PrepareForMigrationAnswer execute(PrepareForMigrationCommand cmd) {
        VirtualMachineTO vm = cmd.getVirtualMachine();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Preparing host for migrating " + vm.getName());
        }
        NicTO[] nics = vm.getNics();
        try {
            for (NicTO nic : nics) {
                network.getNetwork(nic);
            }
            hypervisor.setVmState(vm.getName(), State.Migrating);
            LOGGER.debug("VM " + vm.getName() + " is in Migrating state");
            return new PrepareForMigrationAnswer(cmd);
        } catch (Ovm3ResourceException e) {
            LOGGER.error("Catch Exception " + e.getClass().getName()
                    + " prepare for migration failed due to: " + e.getMessage());
            return new PrepareForMigrationAnswer(cmd, e);
        }
    }

    /* do migrations of VMs in a simple way just inside a cluster for now */
    public MigrateAnswer execute(final MigrateCommand cmd) {
        final String vmName = cmd.getVmName();
        String destUuid = cmd.getHostGuid();
        String destIp = cmd.getDestinationIp();
        State state = State.Error;
        /*
         * TODO: figure out non pooled migration, works from CLI but not from
         * the agent... perhaps pause the VM and then migrate it ? for now just
         * stop the VM.
         */
        String msg = "Migrating " + vmName + " to " + destIp;
        LOGGER.info(msg);
        if (!config.getAgentInOvm3Cluster() && !config.getAgentInOvm3Pool()) {
            try {
                Xen xen = new Xen(c);
                Xen.Vm vm = xen.getRunningVmConfig(vmName);
                HostVO destHost = resourceMgr.findHostByGuid(destUuid);
                if (destHost == null) {
                    msg = "Unable to find migration target host in DB "
                            + destUuid + " with ip " + destIp;
                    LOGGER.info(msg);
                    return new MigrateAnswer(cmd, false, msg, null);
                }
                xen.stopVm(ovmObject.deDash(vm.getVmRootDiskPoolId()),
                        vm.getVmUuid());
                msg = destHost.toString();
                state = State.Stopping;
                return new MigrateAnswer(cmd, false, msg, null);
            } catch (Ovm3ResourceException e) {
                msg = "Unpooled VM Migrate of " + vmName + " to " + destUuid
                        + " failed due to: " + e.getMessage();
                LOGGER.debug(msg, e);
                return new MigrateAnswer(cmd, false, msg, null);
            } finally {
                /* shouldn't we just reinitialize completely as a last resort ? */
                hypervisor.setVmState(vmName, state);
            }
        } else {
            try {
                Xen xen = new Xen(c);
                Xen.Vm vm = xen.getRunningVmConfig(vmName);
                if (vm == null) {
                    state = State.Stopped;
                    msg = vmName + " is no running on " + config.getAgentHostname();
                    return new MigrateAnswer(cmd, false, msg, null);
                }
                /* not a storage migration!!! */
                xen.migrateVm(ovmObject.deDash(vm.getVmRootDiskPoolId()),
                        vm.getVmUuid(), destIp);
                state = State.Stopping;
                msg = "Migration of " + vmName + " successfull";
                return new MigrateAnswer(cmd, true, msg, null);
            } catch (Ovm3ResourceException e) {
                msg = "Pooled VM Migrate" + ": Migration of " + vmName + " to "
                        + destIp + " failed due to " + e.getMessage();
                LOGGER.debug(msg, e);
                return new MigrateAnswer(cmd, false, msg, null);
            } finally {
                hypervisor.setVmState(vmName, state);
            }
        }
    }

    /*
     */
    public GetVncPortAnswer execute(GetVncPortCommand cmd) {
        try {
            Xen host = new Xen(c);
            Xen.Vm vm = host.getRunningVmConfig(cmd.getName());
            Integer vncPort = vm.getVncPort();
            LOGGER.debug("get vnc port for " + cmd.getName() + ": " + vncPort);
            return new GetVncPortAnswer(cmd, c.getIp(), vncPort);
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("get vnc port for " + cmd.getName() + " failed", e);
            return new GetVncPortAnswer(cmd, e.getMessage());
        }
    }

    private VmStatsEntry getVmStat(String vmName) {
        CloudstackPlugin cSp = new CloudstackPlugin(c);
        Map<String, String> oldVmStats = null;
        Map<String, String> newVmStats = null;
        VmStatsEntry stats = new VmStatsEntry();
        try {
            if (vmStats.containsKey(vmName)) {
                oldVmStats = new HashMap<String, String>();
                oldVmStats.putAll(vmStats.get(vmName));
            }
            newVmStats = cSp.ovsDomUStats(vmName);
        } catch (Ovm3ResourceException e) {
            LOGGER.info("Unable to retrieve stats from " + vmName, e);
            return stats;
        }
        if (oldVmStats == null) {
            LOGGER.debug("No old stats retrieved stats from " + vmName);
            stats.setNumCPUs(1);
            stats.setNetworkReadKBs(0);
            stats.setNetworkWriteKBs(0);
            stats.setDiskReadKBs(0);
            stats.setDiskWriteKBs(0);
            stats.setDiskReadIOs(0);
            stats.setDiskWriteIOs(0);
            stats.setCPUUtilization(0);
            stats.setEntityType("vm");
        } else {
            LOGGER.debug("Retrieved new stats from " + vmName);
            int cpus = Integer.parseInt(newVmStats.get("vcpus"));
            stats.setNumCPUs(cpus);
            stats.setNetworkReadKBs(doubleMin(newVmStats.get("rx_bytes"), oldVmStats.get("rx_bytes")));
            stats.setNetworkWriteKBs(doubleMin(newVmStats.get("tx_bytes"), oldVmStats.get("tx_bytes")));
            stats.setDiskReadKBs(doubleMin(newVmStats.get("rd_bytes"), oldVmStats.get("rd_bytes")));
            stats.setDiskWriteKBs(doubleMin(newVmStats.get("rw_bytes"), oldVmStats.get("rw_bytes")));
            stats.setDiskReadIOs(doubleMin(newVmStats.get("rd_ops"), oldVmStats.get("rd_ops")));
            stats.setDiskWriteIOs(doubleMin(newVmStats.get("rw_ops"), oldVmStats.get("rw_ops")));
            Double dCpu = doubleMin(newVmStats.get("cputime"), oldVmStats.get("cputime"));
            Double dTime = doubleMin(newVmStats.get("uptime"), oldVmStats.get("uptime"));
            Double cpupct = dCpu / dTime * 100 * cpus;
            stats.setCPUUtilization(cpupct);
            stats.setEntityType("vm");
        }
        ((ConcurrentHashMap<String, Map<String, String>>) vmStats).put(
                vmName, newVmStats);
        return stats;
    }
    private Double doubleMin(String x, String y) {
        try {
            return (Double.parseDouble(x) - Double.parseDouble(y));
        } catch (NullPointerException e) {
            return 0D;
        }
    }

    public GetVmStatsAnswer execute(GetVmStatsCommand cmd) {
        List<String> vmNames = cmd.getVmNames();
        Map<String, VmStatsEntry> vmStatsNameMap = new HashMap<String, VmStatsEntry>();
        for (String vmName : vmNames) {
            VmStatsEntry e = getVmStat(vmName);
            vmStatsNameMap.put(vmName, e);
        }
        return new GetVmStatsAnswer(cmd,
                (HashMap<String, VmStatsEntry>) vmStatsNameMap);
    }

    /* This is not create for us, but really start */
/*
    public boolean startVm(String repoId, String vmId) throws XmlRpcException {
        Xen host = new Xen(c);
        try {
            if (host.getRunningVmConfig(vmId) == null) {
                LOGGER.error("Create VM " + vmId + " first on " + c.getIp());
                return false;
            } else {
                LOGGER.info("VM " + vmId + " exists on " + c.getIp());
            }
            host.startVm(repoId, vmId);
        } catch (Exception e) {
            LOGGER.error("Failed to start VM " + vmId + " on " + c.getIp()
                    + " " + e.getMessage());
            return false;
        }
        return true;
    }
*/
    /*
     * TODO: OVM already cleans stuff up, just not the extra bridges which we
     * don't want right now, as we'd have to keep a state table of which vlans
     * need to stay on the host!? A map with vlanid -> list-o-hosts
     */
    private void cleanupNetwork(List<String> vifs) throws XmlRpcException {
        /* peel out vif info for vlan stuff */
    }

    public void cleanup(Xen.Vm vm) {
        try {
            cleanupNetwork(vm.getVmVifs());
        } catch (XmlRpcException e) {
            LOGGER.info("Clean up network for " + vm.getVmName() + " failed", e);
        }
        String vmName = vm.getVmName();
        /* should become a single entity */
        vmStats.remove(vmName);
    }

    /*
     * Add rootdisk, datadisk and iso's
     */
    public Boolean createVbds(Xen.Vm vm, VirtualMachineTO spec) {
        if (spec.getDisks() == null) {
            LOGGER.info("No disks defined for " + vm.getVmName());
            return false;
        }
        for (DiskTO disk : spec.getDisks()) {
            try {
                if (disk.getType() == Volume.Type.ROOT) {
                    VolumeObjectTO vol = (VolumeObjectTO) disk.getData();
                    String diskFile = processor.getVirtualDiskPath(vol.getUuid(),  vol.getDataStore().getUuid());
                    vm.addRootDisk(diskFile);
                    vm.setPrimaryPoolUuid(vol.getDataStore().getUuid());
                    LOGGER.debug("Adding root disk: " + diskFile);
                } else if (disk.getType() == Volume.Type.ISO) {
                    DataTO isoTO = disk.getData();
                    if (isoTO.getPath() != null) {
                        TemplateObjectTO template = (TemplateObjectTO) isoTO;
                        DataStoreTO store = template.getDataStore();
                        if (!(store instanceof NfsTO)) {
                            throw new CloudRuntimeException(
                                    "unsupported protocol");
                        }
                        NfsTO nfsStore = (NfsTO) store;
                        String secPoolUuid = pool.setupSecondaryStorage(nfsStore
                                .getUrl());
                        String isoPath = config.getAgentSecStoragePath() + "/"
                                + secPoolUuid + "/"
                                + template.getPath();
                        vm.addIso(isoPath);
                        /* check if secondary storage is mounted */
                        LOGGER.debug("Adding ISO: " + isoPath);
                    }
                } else if (disk.getType() == Volume.Type.DATADISK) {
                    VolumeObjectTO vol = (VolumeObjectTO) disk.getData();
                    String diskFile = processor.getVirtualDiskPath(vol.getUuid(),  vol.getDataStore().getUuid());
                    vm.addDataDisk(diskFile);
                    LOGGER.debug("Adding data disk: "
                            + diskFile);
                } else {
                    throw new CloudRuntimeException("Unknown disk type: "
                            + disk.getType());
                }
            } catch (Exception e) {
                LOGGER.debug("CreateVbds failed", e);
                throw new CloudRuntimeException("Exception" + e.getMessage(), e);
            }
        }
        return true;
    }

    /**
     * Implements the unplug and plug feature for Nics, the boolan decides
     * to either plug (true) or unplug (false)
     *
     * @param nic
     * @param vmName
     * @param plug
     * @return
     */
    private Answer plugNunplugNic(NicTO nic, String vmName, Boolean plug) {
        try {
            Xen xen = new Xen(c);
            Xen.Vm vm = xen.getVmConfig(vmName);
            /* check running */
            if (vm == null) {
                return new Answer(null, false,
                        "Unable to execute command due to missing VM");
            }
            // setup the NIC in the VM config.
            if (plug) {
                createVif(vm, nic);
                vm.setupVifs();
            } else {
                deleteVif(vm, nic);
            }
            // execute the change
            xen.configureVm(ovmObject.deDash(vm.getPrimaryPoolUuid()),
                    vm.getVmUuid());
        } catch (Ovm3ResourceException e) {
            String msg = "Unable to execute command due to " + e.toString();
            LOGGER.debug(msg);
            return new Answer(null, false, msg);
        }
        return new Answer(null, true, "success");
    }
    /**
     * PlugNicAnswer: plug a network interface into a VM
     * @param cmd
     * @return
     */
    public PlugNicAnswer execute(PlugNicCommand cmd) {
        Answer ans = plugNunplugNic(cmd.getNic(), cmd.getVmName(), true);
        return new PlugNicAnswer(cmd, ans.getResult(), ans.getDetails());
    }

    /**
     * UnPlugNicAnswer: remove a nic from a VM
     * @param cmd
     * @return
     */
    public UnPlugNicAnswer execute(UnPlugNicCommand cmd) {
        Answer ans = plugNunplugNic(cmd.getNic(), cmd.getVmName(), false);
        return new UnPlugNicAnswer(cmd, ans.getResult(), ans.getDetails());
    }
}
