/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.hypervisor.xen.resource;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckOnHostAnswer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.FenceAnswer;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PoolEjectCommand;
import com.cloud.agent.api.SetupAnswer;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.resource.ServerResource;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.xensource.xenapi.Bond;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.PIF;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.IpConfigurationMode;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VLAN;
import com.xensource.xenapi.VM;

@Local(value = ServerResource.class)
public class XenServer56Resource extends CitrixResourceBase {
    private final static Logger s_logger = Logger.getLogger(XenServer56Resource.class);
    protected int _heartbeatInterval = 60;

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof FenceCommand) {
            return execute((FenceCommand) cmd);
        } else if (cmd instanceof PoolEjectCommand) {
            return execute((PoolEjectCommand) cmd);
        } else if (cmd instanceof NetworkUsageCommand) {
            return execute((NetworkUsageCommand) cmd);
        } else {
            return super.executeRequest(cmd);
        }
    }
    
    @Override
    protected void setMemory(Connection conn, VM vm, long memsize) throws XmlRpcException, XenAPIException {
        vm.setMemoryLimits(conn, memsize, memsize, memsize, memsize);
    }   
    

    @Override
    protected String getGuestOsType(String stdType, boolean bootFromCD) {
        return CitrixHelper.getXenServerGuestOsType(stdType);
    }


    @Override
    protected List<File> getPatchFiles() {
        List<File> files = new ArrayList<File>();
        String patch = "scripts/vm/hypervisor/xenserver/xenserver56/patch";
        String patchfilePath = Script.findScript("", patch);
        if (patchfilePath == null) {
            throw new CloudRuntimeException("Unable to find patch file " + patch);
        }
        File file = new File(patchfilePath);
        files.add(file);
        patch = "premium-scripts/vm/hypervisor/xenserver/xenserver56/patch";
        patchfilePath = Script.findScript("", patch);
        if (patchfilePath == null) {
            throw new CloudRuntimeException("Unable to find patch file " + patch);
        }
        file = new File(patchfilePath);
        files.add(file);
        return files;
    }

    protected String callHostPluginPremium(Connection conn, String cmd, String... params) {
        return callHostPlugin(conn, "vmopspremium", cmd, params);
    }

    @Override
    protected void disableVlanNetwork(Connection conn, Network network) {
        try {
            if (!network.getNameLabel(conn).startsWith("VLAN")) {
                return;
            }
            String bridge = network.getBridge(conn).trim();
            for (PIF pif : network.getPIFs(conn)) {
                if (pif.getHost(conn).getUuid(conn).equalsIgnoreCase(_host.uuid)) {
                    VLAN vlan = pif.getVLANMasterOf(conn);
                    if (vlan != null) {
                        String vlannum = pif.getVLAN(conn).toString();
                        String device = pif.getDevice(conn).trim();
                        if (vlannum.equals("-1")) {
                            return;
                        }
                        try {
                            vlan.destroy(conn);
                            Host host = Host.getByUuid(conn, _host.uuid);
                            host.forgetDataSourceArchives(conn, "pif_" + bridge + "_tx");
                            host.forgetDataSourceArchives(conn, "pif_" + bridge + "_rx");
                            host.forgetDataSourceArchives(conn, "pif_" + device + "." + vlannum + "_tx");
                            host.forgetDataSourceArchives(conn, "pif_" + device + "." + vlannum + "_rx");
                        } catch (XenAPIException e) {
                            s_logger.debug("Catch Exception: " + e.getClass().getName() + ": failed to destory VLAN " + device + " on host " + _host.uuid
                                    + " due to "  + e.toString());
                        }
                    }
                    break;
                }
            }
        } catch (XenAPIException e) {
            String msg = "Unable to disable VLAN network due to " + e.toString();
            s_logger.warn(msg, e);
        } catch (Exception e) {
            String msg = "Unable to disable VLAN network due to " + e.getMessage();
            s_logger.warn(msg, e);
        }
    }

    @Override
    protected SR getStorageRepository(Connection conn, StorageFilerTO pool) {
        try {
            SR sr = super.getStorageRepository(conn, pool);

            if (pool.getType() != StoragePoolType.IscsiLUN && pool.getType() != StoragePoolType.NetworkFilesystem) {
                return sr;
            }
            setupHeartbeatSr(conn, sr, false);
            return sr;
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + ", Unable to setup heartbeat sr on " + pool.getUuid() +  " due to " + e.toString();
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    protected String callHostPluginThroughMaster(Connection conn, String plugin, String cmd, String... params) {
        Map<String, String> args = new HashMap<String, String>();

        try {
            Map<Pool, Pool.Record> poolRecs = Pool.getAllRecords(conn);
            if (poolRecs.size() != 1) {
                throw new CloudRuntimeException("There are " + poolRecs.size() + " pool for host :" + _host.uuid);
            }
            Host master = poolRecs.values().iterator().next().master;
            for (int i = 0; i < params.length; i += 2) {
                args.put(params[i], params[i + 1]);
            }

            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin executing for command " + cmd + " with " + getArgsString(args));
            }
            String result = master.callPlugin(conn, plugin, cmd, args);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("callHostPlugin Result: " + result);
            }
            return result.replace("\n", "");
        } catch (Types.HandleInvalid e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args)
                    + " due to HandleInvalid clazz:" + e.clazz + ", handle:" + e.handle);
        } catch (XenAPIException e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to "
                    + e.toString(), e);
        } catch (XmlRpcException e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd + " with args " + getArgsString(args) + " due to "
                    + e.getMessage(), e);
        }
        return null;
    }

    protected String setupHeartbeatSr(Connection conn, SR sr, boolean force) throws XenAPIException, XmlRpcException {
        Host host = Host.getByUuid(conn, _host.uuid);
        SR.Record srRec = sr.getRecord(conn);
        String srUuid = srRec.uuid;
        if (!srRec.shared || (!SRType.LVMOISCSI.equals(srRec.type) && !SRType.NFS.equals(srRec.type))) {
            return srUuid;
        }
        Set<String> tags = host.getTags(conn);
        if (force || !tags.contains("cloud-heartbeat-" + srUuid)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Setting up the heartbeat sr for host " + _host.ip + " and sr " + srUuid);
            }
            Set<PBD> pbds = sr.getPBDs(conn);
            for (PBD pbd : pbds) {
                PBD.Record pbdr = pbd.getRecord(conn);
                if (!pbdr.currentlyAttached && pbdr.host.getUuid(conn).equals(_host.uuid)) {
                    pbd.plug(conn);
                    break;
                }
            }
            String result = callHostPluginThroughMaster(conn, "vmopspremium", "setup_heartbeat_sr", "host", _host.uuid,
                    "sr", srUuid);
            if (result == null || !result.split("#")[0].equals("0")) {
                throw new CloudRuntimeException("Unable to setup heartbeat sr on SR " + srUuid + " due to " + result);
            }
            result = callHostPluginPremium(conn, "setup_heartbeat_file", "host", _host.uuid, "sr", srUuid);
            if (result == null || !result.split("#")[0].equals("0")) {
                throw new CloudRuntimeException("Unable to setup heartbeat file entry on SR " + srUuid + " due to "
                        + result);
            }
            if (!tags.contains("cloud-heartbeat-" + srUuid)) {
                tags.add("cloud-heartbeat-" + srUuid);
                host.setTags(conn, tags);
            }
        }
        return srUuid;
    }

    @Override
    protected String networkUsage(Connection conn, final String privateIpAddress, final String option, final String vif) {
        String args = null;
        if (option.equals("get")) {
            args = "-g";
        } else if (option.equals("create")) {
            args = "-c";
        } else if (option.equals("reset")) {
            args = "-r";
        } else if (option.equals("addVif")) {
            args = "-a";
            args += vif;
        } else if (option.equals("deleteVif")) {
            args = "-d";
            args += vif;
        }

        args += " -i ";
        args += privateIpAddress;
        return callHostPlugin(conn, "vmops", "networkUsage", "args", args);
    }
    String copy_vhd_to_secondarystorage(Connection conn, String mountpoint, String vdiuuid, String sruuid) {
        String results = callHostPluginAsync(conn, "vmopspremium", "copy_vhd_to_secondarystorage",
                2 * 60 * 60 * 1000, "mountpoint", mountpoint, "vdiuuid", vdiuuid, "sruuid", sruuid);

        if (results == null || results.isEmpty()) {
            String msg = "copy_vhd_to_secondarystorage return null";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        String[] tmp = results.split("#");
        String status = tmp[0];
        if (status.equals("0")) {
            return tmp[1];
        } else {
            s_logger.warn(tmp[1]);
            throw new CloudRuntimeException(tmp[1]);
        }
    }
    
    String copy_vhd_from_secondarystorage(Connection conn, String mountpoint, String sruuid) {
        String results = callHostPluginAsync(conn, "vmopspremium", "copy_vhd_from_secondarystorage",
                2 * 60 * 60 * 1000, "mountpoint", mountpoint, "sruuid", sruuid);

        if (results == null || results.isEmpty()) {
            String msg = "copy_vhd_from_secondarystorage return null";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
        String[] tmp = results.split("#");
        String status = tmp[0];
        if (status.equals("0")) {
            return tmp[1];
        } else {
            s_logger.warn(tmp[1]);
            throw new CloudRuntimeException(tmp[1]);
        }
    }

    @Override
    public CopyVolumeAnswer execute(final CopyVolumeCommand cmd) {
        Connection conn = getConnection();
        String volumeUUID = cmd.getVolumePath();
        StorageFilerTO poolTO = cmd.getPool();
        String secondaryStorageURL = cmd.getSecondaryStorageURL();
        boolean toSecondaryStorage = cmd.toSecondaryStorage();
        try {
            URI uri = new URI(secondaryStorageURL);
            String remoteVolumesMountPath = uri.getHost() + ":" + uri.getPath() + "/volumes/";
            String volumeFolder = String.valueOf(cmd.getVolumeId()) + "/";
            String mountpoint = remoteVolumesMountPath + volumeFolder;
            SR primaryStoragePool = getStorageRepository(conn, poolTO);
            String srUuid = primaryStoragePool.getUuid(conn);         
            if (toSecondaryStorage) {
                VDI vdi = VDI.getByUuid(conn, volumeUUID);
                String vdiParent = vdi.getSmConfig(conn).get("vhd-parent");
                if( vdiParent != null && !vdiParent.isEmpty() ) {
                    return super.execute(cmd);
                }
                // Create the volume folder
                if (!createSecondaryStorageFolder(conn, remoteVolumesMountPath, volumeFolder)) {
                    throw new InternalErrorException("Failed to create the volume folder.");
                }
                String uuid = copy_vhd_to_secondarystorage(conn, mountpoint, volumeUUID, srUuid);              
                return new CopyVolumeAnswer(cmd, true, null, null, uuid);
            } else {
                String uuid = copy_vhd_from_secondarystorage(conn, mountpoint, srUuid);
                deleteSecondaryStorageFolder(conn, remoteVolumesMountPath, volumeFolder);
                return new CopyVolumeAnswer(cmd, true, null, srUuid, uuid);
            }
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + " due to " + e.toString();
            s_logger.warn(msg, e);
            return new CopyVolumeAnswer(cmd, false, msg, null, null);
        } 
    }

    @Override
    public PrimaryStorageDownloadAnswer execute(final PrimaryStorageDownloadCommand cmd) {
        String tmplturl = cmd.getUrl();
        int index = tmplturl.lastIndexOf("/");
        String tmplpath = tmplturl.substring(0, index);
        String poolName = cmd.getPoolUuid();
        try {
            URI uri = new URI(tmplpath);
            String mountpoint = uri.getHost() + ":" + uri.getPath();
            Connection conn = getConnection();
            SR poolsr = null;
            Set<SR> srs = SR.getByNameLabel(conn, poolName);
            if (srs.size() != 1) {
                String msg = "There are " + srs.size() + " SRs with same name: " + poolName;
                s_logger.warn(msg);
                return new PrimaryStorageDownloadAnswer(msg);
            } else {
                poolsr = srs.iterator().next();
            }
            String pUuid = poolsr.getUuid(conn);
            String uuid = copy_vhd_from_secondarystorage(conn, mountpoint, pUuid);
            VDI tmpl = getVDIbyUuid(conn, uuid);
            VDI snapshotvdi = tmpl.snapshot(conn, new HashMap<String, String>());
            snapshotvdi.setNameLabel(conn, "Template " + cmd.getName());
            tmpl.destroy(conn);
            String parentuuid = snapshotvdi.getSmConfig(conn).get("vhd-parent");
            VDI parent = getVDIbyUuid(conn, parentuuid);
            Long phySize = parent.getPhysicalUtilisation(conn);
            return new PrimaryStorageDownloadAnswer(snapshotvdi.getUuid(conn), phySize);
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + " on host:" + _host.uuid + " for template: "
                    + tmplturl + " due to " + e.toString();
            s_logger.warn(msg, e);
            return new PrimaryStorageDownloadAnswer(msg);
        }
    }

    protected NetworkUsageAnswer execute(NetworkUsageCommand cmd) {
        Connection conn = getConnection();
        if(cmd.getOption()!=null && cmd.getOption().equals("create") ){
            String result = networkUsage(conn, cmd.getPrivateIP(), "create", null);
            NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, result, 0L, 0L);
            return answer;
        }
        long[] stats = getNetworkStats(conn, cmd.getPrivateIP());
        NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, "", stats[0], stats[1]);
        return answer;
    }

    @Override
    protected Answer execute(PoolEjectCommand cmd) {
        Connection conn = getConnection();
        String hostuuid = cmd.getHostuuid();
        try {
            Host host = Host.getByUuid(conn, hostuuid);
            // remove all tags cloud stack add before eject
            Host.Record hr = host.getRecord(conn);
            Iterator<String> it = hr.tags.iterator();
            while (it.hasNext()) {
                String tag = it.next();
                if (tag.contains("cloud-heartbeat-")) {
                    it.remove();
                }
            }
            return super.execute(cmd);

        } catch (XenAPIException e) {
            String msg = "Unable to eject host " + _host.uuid + " due to " + e.toString();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        } catch (Exception e) {
            s_logger.warn("Unable to eject host " + _host.uuid, e);
            String msg = "Unable to eject host " + _host.uuid + " due to " + e.getMessage();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }

    }

    protected FenceAnswer execute(FenceCommand cmd) {
        Connection conn = getConnection();
        try {
            String result = callHostPluginPremium(conn, "check_heartbeat", "host", cmd.getHostGuid(), "interval",
                    Integer.toString(_heartbeatInterval * 2));
            if (!result.contains("> DEAD <")) {
                s_logger.debug("Heart beat is still going so unable to fence");
                return new FenceAnswer(cmd, false, "Heartbeat is still going on unable to fence");
            }

            Set<VM> vms = VM.getByNameLabel(conn, cmd.getVmName());
            for (VM vm : vms) {
                vm.powerStateReset(conn);
                vm.destroy(conn);
            }
            XenServerConnectionPool.PoolSyncDB(conn);
            return new FenceAnswer(cmd);
        } catch (XmlRpcException e) {
            s_logger.warn("Unable to fence", e);
            return new FenceAnswer(cmd, false, e.getMessage());
        } catch (XenAPIException e) {
            s_logger.warn("Unable to fence", e);
            return new FenceAnswer(cmd, false, e.getMessage());
        }
    }

    @Override
    protected boolean transferManagementNetwork(Connection conn, Host host, PIF src, PIF.Record spr, PIF dest)
            throws XmlRpcException, XenAPIException {
        dest.reconfigureIp(conn, spr.ipConfigurationMode, spr.IP, spr.netmask, spr.gateway, spr.DNS);
        Host.managementReconfigure(conn, dest);
        String hostUuid = null;
        int count = 0;
        while (count < 10) {
            try {
                Thread.sleep(10000);
                hostUuid = host.getUuid(conn);
                if (hostUuid != null) {
                    break;
                }
            } catch (XmlRpcException e) {
                s_logger.debug("Waiting for host to come back: " + e.getMessage());
            } catch (XenAPIException e) {
                s_logger.debug("Waiting for host to come back: " + e.getMessage());
            } catch (InterruptedException e) {
                s_logger.debug("Gotta run");
                return false;
            }
        }
        if (hostUuid == null) {
            s_logger.warn("Unable to transfer the management network from " + spr.uuid);
            return false;
        }

        src.reconfigureIp(conn, IpConfigurationMode.NONE, null, null, null, null);
        return true;
    }

    @Override
    protected SetupAnswer execute(SetupCommand cmd) {
        Connection conn = getConnection();
        try {
            callHostPluginPremium(conn, "setupXenServer");
            cleanupTemplateSR(conn);
            Host host = Host.getByUuid(conn, _host.uuid);
            try {
                if (cmd.useMultipath()) {
                    // the config value is set to true
                    host.addToOtherConfig(conn, "multipathing", "true");
                    host.addToOtherConfig(conn, "multipathhandle", "dmp");
                }

            } catch (Types.MapDuplicateKey e) {
                s_logger.debug("multipath is already set");
            }

            String result = callHostPlugin(conn, "vmops", "setup_iscsi", "uuid", _host.uuid);
            if (!result.contains("> DONE <")) {
                s_logger.warn("Unable to setup iscsi: " + result);
                return new SetupAnswer(cmd, result);
            }

            Pair<PIF, PIF.Record> mgmtPif = null;
            Set<PIF> hostPifs = host.getPIFs(conn);
            for (PIF pif : hostPifs) {
                PIF.Record rec = pif.getRecord(conn);
                if (rec.management) {
                    if (rec.VLAN != null && rec.VLAN != -1) {
                        String msg = new StringBuilder(
                                "Unsupported configuration.  Management network is on a VLAN.  host=").append(
                                _host.uuid).append("; pif=").append(rec.uuid).append("; vlan=").append(rec.VLAN)
                                .toString();
                        s_logger.warn(msg);
                        return new SetupAnswer(cmd, msg);
                    }
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Management network is on pif=" + rec.uuid);
                    }
                    mgmtPif = new Pair<PIF, PIF.Record>(pif, rec);
                    break;
                }
            }

            if (mgmtPif == null) {
                String msg = "Unable to find management network for " + _host.uuid;
                s_logger.warn(msg);
                return new SetupAnswer(cmd, msg);
            }

            Map<Network, Network.Record> networks = Network.getAllRecords(conn);
            for (Network.Record network : networks.values()) {
                if (network.nameLabel.equals("cloud-private")) {
                    for (PIF pif : network.PIFs) {
                        PIF.Record pr = pif.getRecord(conn);
                        if (_host.uuid.equals(pr.host.getUuid(conn))) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Found a network called cloud-private. host=" + _host.uuid
                                        + ";  Network=" + network.uuid + "; pif=" + pr.uuid);
                            }
                            if (pr.VLAN != null && pr.VLAN != -1) {
                                String msg = new StringBuilder(
                                        "Unsupported configuration.  Network cloud-private is on a VLAN.  Network=")
                                        .append(network.uuid).append(" ; pif=").append(pr.uuid).toString();
                                s_logger.warn(msg);
                                return new SetupAnswer(cmd, msg);
                            }
                            if (!pr.management && pr.bondMasterOf != null && pr.bondMasterOf.size() > 0) {
                                if (pr.bondMasterOf.size() > 1) {
                                    String msg = new StringBuilder(
                                            "Unsupported configuration.  Network cloud-private has more than one bond.  Network=")
                                            .append(network.uuid).append("; pif=").append(pr.uuid).toString();
                                    s_logger.warn(msg);
                                    return new SetupAnswer(cmd, msg);
                                }
                                Bond bond = pr.bondMasterOf.iterator().next();
                                Set<PIF> slaves = bond.getSlaves(conn);
                                for (PIF slave : slaves) {
                                    PIF.Record spr = slave.getRecord(conn);
                                    if (spr.management) {
                                        if (!transferManagementNetwork(conn, host, slave, spr, pif)) {
                                            String msg = new StringBuilder(
                                                    "Unable to transfer management network.  slave=" + spr.uuid
                                                            + "; master=" + pr.uuid + "; host=" + _host.uuid)
                                                    .toString();
                                            s_logger.warn(msg);
                                            return new SetupAnswer(cmd, msg);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return new SetupAnswer(cmd, false);

        } catch (XmlRpcException e) {
            s_logger.warn("Unable to setup", e);
            return new SetupAnswer(cmd, e.getMessage());
        } catch (XenAPIException e) {
            s_logger.warn("Unable to setup", e);
            return new SetupAnswer(cmd, e.getMessage());
        } catch (Exception e) {
            s_logger.warn("Unable to setup", e);
            return new SetupAnswer(cmd, e.getMessage());
        }
    }

    private void cleanupTemplateSR(Connection conn) {
        Set<PBD> pbds = null;
        try {
            Host host = Host.getByUuid(conn, _host.uuid);
            pbds = host.getPBDs(conn);
        } catch (XenAPIException e) {
            s_logger.warn("Unable to get the SRs " + e.toString(), e);
            throw new CloudRuntimeException("Unable to get SRs " + e.toString(), e);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to get SRs " + e.getMessage(), e);
        }
        for (PBD pbd : pbds) {
            SR sr = null;
            SR.Record srRec = null;
            try {
                sr = pbd.getSR(conn);
                srRec = sr.getRecord(conn);
            } catch (Exception e) {
                s_logger.warn("pbd.getSR get Exception due to " + e.toString());
                continue;
            }
            String type = srRec.type;
            if (srRec.shared) {
                continue;
            }
            if (SRType.NFS.equals(type) || (SRType.ISO.equals(type) && srRec.nameDescription.contains("template"))) {
                try {
                    pbd.unplug(conn);
                    pbd.destroy(conn);
                    sr.forget(conn);
                } catch (Exception e) {
                    s_logger.warn("forget SR catch Exception due to " + e.toString());
                }
            }
        }
    }

    @Override
    public StartupCommand[] initialize() {
        pingxenserver();
        StartupCommand[] cmds = super.initialize();
        Connection conn = getConnection();
        try {
            Host host = Host.getByUuid(conn, _host.uuid);
            Set<String> tags = host.getTags(conn);
            Iterator<String> it = tags.iterator();
            while (it.hasNext()) {
                String heartbeatTag = it.next();
                if (heartbeatTag.contains("cloud-heartbeat-")) {
                    s_logger.debug("Removing heatbeat tag: " + heartbeatTag);
                    it.remove();
                }
            }
            host.setTags(conn, tags);
        } catch (XenAPIException e) {
            throw new CloudRuntimeException("Unable to setup heartbeat", e);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to setup heartbeat", e);
        }

        String result = callHostPluginPremium(conn, "heartbeat", "host", _host.uuid, "interval", Integer
                .toString(_heartbeatInterval));
        if (result == null || !result.contains("> DONE <")) {
            s_logger.warn("Unable to launch the heartbeat process on " + _host.ip);
            return null;
        }

        return cmds;
    }

    @Override
    protected CheckOnHostAnswer execute(CheckOnHostCommand cmd) {
        Connection conn = getConnection();
        try {
            String result = callHostPluginPremium(conn, "check_heartbeat", "host", cmd.getHost().getGuid(), "interval",
                    Integer.toString(_heartbeatInterval * 2));
            if (result == null) {
                return new CheckOnHostAnswer(cmd, "Unable to call plugin");
            }
            if (result.contains("> DEAD <")) {
                s_logger.debug("Heart beat is gone so dead.");
                return new CheckOnHostAnswer(cmd, false, "Heart Beat is done");
            } else if (result.contains("> ALIVE <")) {
                s_logger.debug("Heart beat is still going");
                return new CheckOnHostAnswer(cmd, true, "Heartbeat is still going");
            }
            return new CheckOnHostAnswer(cmd, null, "Unable to determine");
        } catch (Exception e) {
            s_logger.warn("Unable to fence", e);
            return new CheckOnHostAnswer(cmd, e.getMessage());
        }
    }

    public XenServer56Resource() {
        super();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        _heartbeatInterval = NumbersUtil.parseInt((String) params.get("xen.heartbeat.interval"), 60);
        // xapi connection timeout 600 seconds
        _wait = 600;

        return true;
    }
}
