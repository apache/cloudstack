/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.hypervisor.xen.resource;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataTO;
import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreAnswer;
import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreCmd;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.CreatePrimaryDataStoreCmd;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.datastore.protocol.DataStoreProtocol;
import org.apache.cloudstack.storage.to.ImageStoreTO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ManageSnapshotAnswer;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.DeleteVolumeCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.xen.resource.CitrixResourceBase.SRType;
import com.cloud.storage.DataStoreRole;
import com.cloud.utils.S3Utils;
import com.cloud.utils.StringUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.storage.encoding.DecodedDataObject;
import com.cloud.utils.storage.encoding.DecodedDataStore;
import com.cloud.utils.storage.encoding.Decoder;
import com.cloud.vm.DiskProfile;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VDI;

public class XenServerStorageResource {
    private static final Logger s_logger = Logger.getLogger(XenServerStorageResource.class);
    protected CitrixResourceBase hypervisorResource;

    public XenServerStorageResource(CitrixResourceBase resource) {
        this.hypervisorResource = resource;
    }

    public Answer handleStorageCommands(StorageSubSystemCommand command) {
        if (command instanceof CopyCommand) {
            return this.execute((CopyCommand)command);
        } else if (command instanceof AttachPrimaryDataStoreCmd) {
            return this.execute((AttachPrimaryDataStoreCmd)command);
        } else if (command instanceof CreatePrimaryDataStoreCmd) {
            return execute((CreatePrimaryDataStoreCmd) command);
        } else if (command instanceof CreateObjectCommand) {
            return execute((CreateObjectCommand) command);
        } else if (command instanceof DeleteCommand) {
            return execute((DeleteCommand)command);
        }
        return new Answer((Command)command, false, "not implemented yet");
    }

    protected SR getSRByNameLabel(Connection conn, String nameLabel) throws BadServerResponse, XenAPIException, XmlRpcException {
        Set<SR> srs = SR.getByNameLabel(conn, nameLabel);
        if (srs.size() != 1) {
            throw new CloudRuntimeException("storage uuid: " + nameLabel + " is not unique");
        }
        SR poolsr = srs.iterator().next();
        return poolsr;
    }

    protected VDI createVdi(Connection conn, String vdiName, SR sr, long size) throws BadServerResponse, XenAPIException, XmlRpcException {
        VDI.Record vdir = new VDI.Record();
        vdir.nameLabel = vdiName;
        vdir.SR = sr;
        vdir.type = Types.VdiType.USER;

        vdir.virtualSize = size;
        VDI vdi = VDI.create(conn, vdir);
        return vdi;
    }

    protected void deleteVDI(Connection conn, VDI vdi) throws BadServerResponse, XenAPIException, XmlRpcException {
        vdi.destroy(conn);
    }

    private Map<String, String> getParameters(URI uri) {
        String parameters = uri.getQuery();
        Map<String, String> params = new HashMap<String, String>();
        List<String> paraLists = Arrays.asList(parameters.split("&"));
        for (String para : paraLists) {
            String[] pair = para.split("=");
            params.put(pair[0], pair[1]);
        }
        return params;
    }

    protected CreateObjectAnswer getTemplateSize(CreateObjectCommand cmd, String templateUrl) {
        /*Connection conn = hypervisorResource.getConnection();
        long size = this.getTemplateSize(conn, templateUrl);
        return new CreateObjectAnswer(cmd, templateUrl, size);*/
        return null;
    }

    protected CreateObjectAnswer createSnapshot(SnapshotObjectTO snapshotTO) {
        Connection conn = hypervisorResource.getConnection();
        long snapshotId = snapshotTO.getId();
        String snapshotName = snapshotTO.getName();
        String details = "create snapshot operation Failed for snapshotId: " + snapshotId;
        String snapshotUUID = null;

        try {
            String volumeUUID = snapshotTO.getVolume().getPath();
            VDI volume = VDI.getByUuid(conn, volumeUUID);

            VDI snapshot = volume.snapshot(conn, new HashMap<String, String>());

            if (snapshotName != null) {
                snapshot.setNameLabel(conn, snapshotName);
            }

            snapshotUUID = snapshot.getUuid(conn);
            String preSnapshotUUID = snapshotTO.getParentSnapshotPath();
            //check if it is a empty snapshot
            if( preSnapshotUUID != null) {
                SR sr = volume.getSR(conn);
                String srUUID = sr.getUuid(conn);
                String type = sr.getType(conn);
                Boolean isISCSI = IsISCSI(type);
                String snapshotParentUUID = getVhdParent(conn, srUUID, snapshotUUID, isISCSI);

                String preSnapshotParentUUID = getVhdParent(conn, srUUID, preSnapshotUUID, isISCSI);
                if( snapshotParentUUID != null && snapshotParentUUID.equals(preSnapshotParentUUID)) {
                    // this is empty snapshot, remove it
                    snapshot.destroy(conn);
                    snapshotUUID = preSnapshotUUID;
                }
            }
            SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            newSnapshot.setPath(snapshotUUID);
            return new CreateObjectAnswer(newSnapshot);
        } catch (XenAPIException e) {
            details += ", reason: " + e.toString();
            s_logger.warn(details, e);
        } catch (Exception e) {
            details += ", reason: " + e.toString();
            s_logger.warn(details, e);
        }

        return new CreateObjectAnswer(details);
    }
    protected CreateObjectAnswer execute(CreateObjectCommand cmd) {
       DataTO data = cmd.getData();
       try {
           if (data.getObjectType() == DataObjectType.VOLUME) {
               return createVolume(data);
           } else if (data.getObjectType() == DataObjectType.SNAPSHOT) {
               return createSnapshot((SnapshotObjectTO)data);
           }
           return new CreateObjectAnswer("not supported type");
       } catch (Exception e) {
           s_logger.debug("Failed to create object: " + data.getObjectType() + ": " + e.toString());
           return new CreateObjectAnswer(e.toString());
       }
    }
    
    protected Answer deleteVolume(VolumeObjectTO volume) {
        Connection conn = hypervisorResource.getConnection();
        String errorMsg = null;
        try {
            VDI vdi = VDI.getByUuid(conn, volume.getPath());
            deleteVDI(conn, vdi);
            return new Answer(null);
        } catch (BadServerResponse e) {
            s_logger.debug("Failed to delete volume", e);
            errorMsg = e.toString();
        } catch (XenAPIException e) {
            s_logger.debug("Failed to delete volume", e);
            errorMsg = e.toString();
        } catch (XmlRpcException e) {
            s_logger.debug("Failed to delete volume", e);
            errorMsg = e.toString();
        }
        return new Answer(null, false, errorMsg);
    }

    protected Answer execute(DeleteCommand cmd) {
        DataTO data = cmd.getData();
        Answer answer = null;
        if (data.getObjectType() == DataObjectType.VOLUME) {
            answer = deleteVolume((VolumeObjectTO)data);
        } else {
            answer = new Answer(cmd, false, "unsupported type");
        }

        return answer;
    }

   /* protected Answer execute(CreateVolumeFromBaseImageCommand cmd) {
        VolumeObjectTO volume = cmd.getVolume();
        ImageOnPrimayDataStoreTO baseImage = cmd.getImage();
        Connection conn = hypervisorResource.getConnection();

        try {
            VDI baseVdi = VDI.getByUuid(conn, baseImage.getPathOnPrimaryDataStore());
            VDI newVol = baseVdi.createClone(conn, new HashMap<String, String>());
            newVol.setNameLabel(conn, volume.getName());
            return new CreateObjectAnswer(cmd, newVol.getUuid(conn), newVol.getVirtualSize(conn));
        } catch (BadServerResponse e) {
            return new Answer(cmd, false, e.toString());
        } catch (XenAPIException e) {
            return new Answer(cmd, false, e.toString());
        } catch (XmlRpcException e) {
            return new Answer(cmd, false, e.toString());
        }
    }*/

    protected SR getNfsSR(Connection conn, DecodedDataStore store) {

        Map<String, String> deviceConfig = new HashMap<String, String>();

        String uuid = store.getUuid();
        try {
            String server = store.getServer();
            String serverpath = store.getPath();

            serverpath = serverpath.replace("//", "/");
            Set<SR> srs = SR.getAll(conn);
            for (SR sr : srs) {
                if (!SRType.NFS.equals(sr.getType(conn))) {
                    continue;
                }

                Set<PBD> pbds = sr.getPBDs(conn);
                if (pbds.isEmpty()) {
                    continue;
                }

                PBD pbd = pbds.iterator().next();

                Map<String, String> dc = pbd.getDeviceConfig(conn);

                if (dc == null) {
                    continue;
                }

                if (dc.get("server") == null) {
                    continue;
                }

                if (dc.get("serverpath") == null) {
                    continue;
                }

                if (server.equals(dc.get("server")) && serverpath.equals(dc.get("serverpath"))) {
                    throw new CloudRuntimeException("There is a SR using the same configuration server:" + dc.get("server") + ", serverpath:"
                            + dc.get("serverpath") + " for pool " + uuid + "on host:" + hypervisorResource.getHost().uuid);
                }

            }
            deviceConfig.put("server", server);
            deviceConfig.put("serverpath", serverpath);
            Host host = Host.getByUuid(conn, hypervisorResource.getHost().uuid);
            SR sr = SR.create(conn, host, deviceConfig, new Long(0), uuid, uuid, SRType.NFS.toString(), "user", true,
                    new HashMap<String, String>());
            sr.scan(conn);
            return sr;
        } catch (XenAPIException e) {
            throw new CloudRuntimeException("Unable to create NFS SR " + uuid, e);
        } catch (XmlRpcException e) {
            throw new CloudRuntimeException("Unable to create NFS SR " + uuid, e);
        }
    }
    /*
    protected SR getIscsiSR(Connection conn, PrimaryDataStoreTO pool) {
        synchronized (pool.getUuid().intern()) {
            Map<String, String> deviceConfig = new HashMap<String, String>();
            try {
                String target = pool.getHost();
                String path = pool.getPath();
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }

                String tmp[] = path.split("/");
                if (tmp.length != 3) {
                    String msg = "Wrong iscsi path " + pool.getPath() + " it should be /targetIQN/LUN";
                    s_logger.warn(msg);
                    throw new CloudRuntimeException(msg);
                }
                String targetiqn = tmp[1].trim();
                String lunid = tmp[2].trim();
                String scsiid = "";

                Set<SR> srs = SR.getByNameLabel(conn, pool.getUuid());
                for (SR sr : srs) {
                    if (!SRType.LVMOISCSI.equals(sr.getType(conn))) {
                        continue;
                    }
                    Set<PBD> pbds = sr.getPBDs(conn);
                    if (pbds.isEmpty()) {
                        continue;
                    }
                    PBD pbd = pbds.iterator().next();
                    Map<String, String> dc = pbd.getDeviceConfig(conn);
                    if (dc == null) {
                        continue;
                    }
                    if (dc.get("target") == null) {
                        continue;
                    }
                    if (dc.get("targetIQN") == null) {
                        continue;
                    }
                    if (dc.get("lunid") == null) {
                        continue;
                    }
                    if (target.equals(dc.get("target")) && targetiqn.equals(dc.get("targetIQN")) && lunid.equals(dc.get("lunid"))) {
                        throw new CloudRuntimeException("There is a SR using the same configuration target:" + dc.get("target") +  ",  targetIQN:"
                                + dc.get("targetIQN")  + ", lunid:" + dc.get("lunid") + " for pool " + pool.getUuid() + "on host:" + _host.uuid);
                    }
                }
                deviceConfig.put("target", target);
                deviceConfig.put("targetIQN", targetiqn);

                Host host = Host.getByUuid(conn, _host.uuid);
                Map<String, String> smConfig = new HashMap<String, String>();
                String type = SRType.LVMOISCSI.toString();
                String poolId = Long.toString(pool.getId());
                SR sr = null;
                try {
                    sr = SR.create(conn, host, deviceConfig, new Long(0), pool.getUuid(), poolId, type, "user", true,
                            smConfig);
                } catch (XenAPIException e) {
                    String errmsg = e.toString();
                    if (errmsg.contains("SR_BACKEND_FAILURE_107")) {
                        String lun[] = errmsg.split("<LUN>");
                        boolean found = false;
                        for (int i = 1; i < lun.length; i++) {
                            int blunindex = lun[i].indexOf("<LUNid>") + 7;
                            int elunindex = lun[i].indexOf("</LUNid>");
                            String ilun = lun[i].substring(blunindex, elunindex);
                            ilun = ilun.trim();
                            if (ilun.equals(lunid)) {
                                int bscsiindex = lun[i].indexOf("<SCSIid>") + 8;
                                int escsiindex = lun[i].indexOf("</SCSIid>");
                                scsiid = lun[i].substring(bscsiindex, escsiindex);
                                scsiid = scsiid.trim();
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            String msg = "can not find LUN " + lunid + " in " + errmsg;
                            s_logger.warn(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    } else {
                        String msg = "Unable to create Iscsi SR  " + deviceConfig + " due to  " + e.toString();
                        s_logger.warn(msg, e);
                        throw new CloudRuntimeException(msg, e);
                    }
                }
                deviceConfig.put("SCSIid", scsiid);

                String result = SR.probe(conn, host, deviceConfig, type , smConfig);
                String pooluuid = null;
                if( result.indexOf("<UUID>") != -1) {
                    pooluuid = result.substring(result.indexOf("<UUID>") + 6, result.indexOf("</UUID>")).trim();
                }
                if( pooluuid == null || pooluuid.length() != 36) {
                    sr = SR.create(conn, host, deviceConfig, new Long(0), pool.getUuid(), poolId, type, "user", true,
                            smConfig);
                } else {
                    sr = SR.introduce(conn, pooluuid, pool.getUuid(), poolId,
                            type, "user", true, smConfig);
                    Pool.Record pRec = XenServerConnectionPool.getPoolRecord(conn);
                    PBD.Record rec = new PBD.Record();
                    rec.deviceConfig = deviceConfig;
                    rec.host = pRec.master;
                    rec.SR = sr;
                    PBD pbd = PBD.create(conn, rec);
                    pbd.plug(conn);
                }
                sr.scan(conn);
                return sr;
            } catch (XenAPIException e) {
                String msg = "Unable to create Iscsi SR  " + deviceConfig + " due to  " + e.toString();
                s_logger.warn(msg, e);
                throw new CloudRuntimeException(msg, e);
            } catch (Exception e) {
                String msg = "Unable to create Iscsi SR  " + deviceConfig + " due to  " + e.getMessage();
                s_logger.warn(msg, e);
                throw new CloudRuntimeException(msg, e);
            }
        }
    }*/

    protected Answer execute(CreatePrimaryDataStoreCmd cmd) {
        Connection conn = hypervisorResource.getConnection();
        String storeUrl = cmd.getDataStore();

        try {
            DecodedDataObject obj = Decoder.decode(storeUrl);
            DecodedDataStore store = obj.getStore();

            if (store.getScheme().equalsIgnoreCase("nfs")) {
                SR sr = getNfsSR(conn, store);
            } else if (store.getScheme().equalsIgnoreCase("iscsi")) {
                //getIscsiSR(conn, dataStore);
            } else if (store.getScheme().equalsIgnoreCase("presetup")) {
            } else {
                return new Answer(cmd, false, "The pool type: " + store.getScheme() + " is not supported.");
            }
            return new Answer(cmd, true, "success");
        } catch (Exception e) {
           // String msg = "Catch Exception " + e.getClass().getName() + ", create StoragePool failed due to " + e.toString() + " on host:" + _host.uuid + " pool: " + pool.getHost() + pool.getPath();
            //s_logger.warn(msg, e);
            return new Answer(cmd, false, null);
        }
    }

    private long getTemplateSize(Connection conn, String url) {
        String size = hypervisorResource.callHostPlugin(conn, "storagePlugin", "getTemplateSize", "srcUrl", url);
        if (size.equalsIgnoreCase("") || size == null) {
            throw new CloudRuntimeException("Can't get template size");
        }

        try {
            return Long.parseLong(size);
        } catch (NumberFormatException e) {
            throw new CloudRuntimeException("Failed to get template lenght", e);
        }

        /*
        HttpHead method = new HttpHead(url);
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            HttpResponse response = client.execute(method);
            Header header = response.getFirstHeader("Content-Length");
            if (header == null) {
                throw new CloudRuntimeException("Can't get content-lenght header from :" + url);
            }
            Long length = Long.parseLong(header.getValue());
            return length;
        } catch (HttpException e) {
            throw new CloudRuntimeException("Failed to get template lenght", e);
        } catch (IOException e) {
            throw new CloudRuntimeException("Failed to get template lenght", e);
        } catch (NumberFormatException e) {
            throw new CloudRuntimeException("Failed to get template lenght", e);
        }*/
    }

    private void downloadHttpToLocalFile(String destFilePath, String url) {
        File destFile = new File(destFilePath);
        if (!destFile.exists()) {
            throw new CloudRuntimeException("dest file doesn't exist: " + destFilePath);
        }

        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet getMethod = new HttpGet(url);
        HttpResponse response;
        BufferedOutputStream output = null;
        long length = 0;
        try {
            response = client.execute(getMethod);
            HttpEntity entity = response.getEntity();
            length = entity.getContentLength();
            output = new BufferedOutputStream(new FileOutputStream(destFile));
            entity.writeTo(output);
        } catch (ClientProtocolException e) {
           throw new CloudRuntimeException("Failed to download template", e);
        } catch (IOException e) {
            throw new CloudRuntimeException("Failed to download template", e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    throw new CloudRuntimeException("Failed to download template", e);
                }
            }
        }

        //double check the length
        destFile = new File(destFilePath);
        if (destFile.length() != length) {
            throw new CloudRuntimeException("Download file length doesn't match: expected: " + length + ", actual: " + destFile.length());
        }

    }

    protected Answer directDownloadHttpTemplate(CopyCommand cmd, DecodedDataObject srcObj, DecodedDataObject destObj) {
        Connection conn = hypervisorResource.getConnection();
        SR poolsr = null;
        VDI vdi = null;
        boolean result = false;
        try {
            if (destObj.getPath() == null) {
                //need to create volume at first

            }
            vdi = VDI.getByUuid(conn, destObj.getPath());
            if (vdi == null) {
                throw new CloudRuntimeException("can't find volume: " + destObj.getPath());
            }
            String destStoreUuid = destObj.getStore().getUuid();
            Set<SR> srs = SR.getByNameLabel(conn, destStoreUuid);
            if (srs.size() != 1) {
                throw new CloudRuntimeException("storage uuid: " + destStoreUuid + " is not unique");
            }
            poolsr = srs.iterator().next();
            VDI.Record vdir = vdi.getRecord(conn);
            String vdiLocation = vdir.location;
            String pbdLocation = null;
            if (destObj.getStore().getScheme().equalsIgnoreCase(DataStoreProtocol.NFS.toString())) {
                pbdLocation = "/run/sr-mount/" + poolsr.getUuid(conn);
            } else {
                Set<PBD> pbds = poolsr.getPBDs(conn);
                if (pbds.size() != 1) {
                    throw new CloudRuntimeException("Don't how to handle multiple pbds:" + pbds.size() + " for sr: " + poolsr.getUuid(conn));
                }
                PBD pbd = pbds.iterator().next();
                Map<String, String> deviceCfg = pbd.getDeviceConfig(conn);
                pbdLocation = deviceCfg.get("location");
            }
            if (pbdLocation == null) {
                throw new CloudRuntimeException("Can't get pbd location");
            }

            String vdiPath = pbdLocation + "/" + vdiLocation + ".vhd";
            //download a url into vdipath
            //downloadHttpToLocalFile(vdiPath, template.getPath());
            hypervisorResource.callHostPlugin(conn, "storagePlugin", "downloadTemplateFromUrl", "destPath", vdiPath, "srcUrl", srcObj.getPath());
            result = true;
            //return new CopyCmdAnswer(cmd, vdi.getUuid(conn));
        } catch (BadServerResponse e) {
            s_logger.debug("Failed to download template", e);
        } catch (XenAPIException e) {
            s_logger.debug("Failed to download template", e);
        } catch (XmlRpcException e) {
            s_logger.debug("Failed to download template", e);
        } catch (Exception e) {
            s_logger.debug("Failed to download template", e);
        } finally {
            if (!result && vdi != null) {
                try {
                    vdi.destroy(conn);
                } catch (BadServerResponse e) {
                   s_logger.debug("Failed to cleanup newly created vdi");
                } catch (XenAPIException e) {
                    s_logger.debug("Failed to cleanup newly created vdi");
                } catch (XmlRpcException e) {
                    s_logger.debug("Failed to cleanup newly created vdi");
                }
            }
        }
        return new Answer(cmd, false, "Failed to download template");
    }

    protected Answer execute(AttachPrimaryDataStoreCmd cmd) {
        String dataStoreUri = cmd.getDataStore();
        Connection conn = hypervisorResource.getConnection();
        try {
            DecodedDataObject obj = Decoder.decode(dataStoreUri);

            DecodedDataStore store = obj.getStore();

            SR sr = hypervisorResource.getStorageRepository(conn, store.getUuid());
            hypervisorResource.setupHeartbeatSr(conn, sr, false);
            long capacity = sr.getPhysicalSize(conn);
            long available = capacity - sr.getPhysicalUtilisation(conn);
            if (capacity == -1) {
                String msg = "Pool capacity is -1! pool: ";
                s_logger.warn(msg);
                return new Answer(cmd, false, msg);
            }
            AttachPrimaryDataStoreAnswer answer = new AttachPrimaryDataStoreAnswer(cmd);
            answer.setCapacity(capacity);
            answer.setUuid(sr.getUuid(conn));
            answer.setAvailable(available);
            return answer;
        } catch (XenAPIException e) {
            String msg = "AttachPrimaryDataStoreCmd add XenAPIException:" + e.toString();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        } catch (Exception e) {
            String msg = "AttachPrimaryDataStoreCmd failed:" + e.getMessage();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }
    }

    private boolean IsISCSI(String type) {
        return SRType.LVMOHBA.equals(type) || SRType.LVMOISCSI.equals(type) || SRType.LVM.equals(type) ;
    }

    private String copy_vhd_from_secondarystorage(Connection conn, String mountpoint, String sruuid, int wait) {
        String nameLabel = "cloud-" + UUID.randomUUID().toString();
        String results = hypervisorResource.callHostPluginAsync(conn, "vmopspremium", "copy_vhd_from_secondarystorage",
                wait, "mountpoint", mountpoint, "sruuid", sruuid, "namelabel", nameLabel);
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg = "copy_vhd_from_secondarystorage return null";
        } else {
            String[] tmp = results.split("#");
            String status = tmp[0];
            if (status.equals("0")) {
                return tmp[1];
            } else {
                errMsg = tmp[1];
            }
        }
        String source = mountpoint.substring(mountpoint.lastIndexOf('/') + 1);
        if( hypervisorResource.killCopyProcess(conn, source) ) {
            destroyVDIbyNameLabel(conn, nameLabel);
        }
        s_logger.warn(errMsg);
        throw new CloudRuntimeException(errMsg);
    }

    private void destroyVDIbyNameLabel(Connection conn, String nameLabel) {
        try {
            Set<VDI> vdis = VDI.getByNameLabel(conn, nameLabel);
            if ( vdis.size() != 1 ) {
                s_logger.warn("destoryVDIbyNameLabel failed due to there are " + vdis.size() + " VDIs with name " + nameLabel);
                return;
            }
            for (VDI vdi : vdis) {
                try {
                    vdi.destroy(conn);
                } catch (Exception e) {
                }
            }
        } catch (Exception e){
        }
    }

    protected VDI getVDIbyUuid(Connection conn, String uuid) {
        try {
            return VDI.getByUuid(conn, uuid);
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + " :VDI getByUuid for uuid: " + uuid + " failed due to " + e.toString();
            s_logger.debug(msg);
            throw new CloudRuntimeException(msg, e);
        }
    }

    protected String getVhdParent(Connection conn, String primaryStorageSRUuid, String snapshotUuid, Boolean isISCSI) {
        String parentUuid = hypervisorResource.callHostPlugin(conn, "vmopsSnapshot", "getVhdParent", "primaryStorageSRUuid", primaryStorageSRUuid,
                "snapshotUuid", snapshotUuid, "isISCSI", isISCSI.toString());

        if (parentUuid == null || parentUuid.isEmpty() || parentUuid.equalsIgnoreCase("None")) {
            s_logger.debug("Unable to get parent of VHD " + snapshotUuid + " in SR " + primaryStorageSRUuid);
            // errString is already logged.
            return null;
        }
        return parentUuid;
    }

    protected CopyCmdAnswer copyTemplateToPrimaryStorage(DataTO srcData, DataTO destData, int wait) {
        DataStoreTO srcStore = srcData.getDataStore();
        try {
            if ((srcStore instanceof NfsTO) && (srcData.getObjectType() == DataObjectType.TEMPLATE)) {
                NfsTO srcImageStore = (NfsTO)srcStore;
                TemplateObjectTO srcTemplate = (TemplateObjectTO)srcData;
                String storeUrl = srcImageStore.getUrl();
            
                URI uri = new URI(storeUrl);
                String tmplpath = uri.getHost() + ":" + uri.getPath() + "/" + srcData.getPath();
                PrimaryDataStoreTO destStore = (PrimaryDataStoreTO)destData.getDataStore();
                String poolName = destStore.getUuid();
                Connection conn = hypervisorResource.getConnection();

                SR poolsr = null;
                Set<SR> srs = SR.getByNameLabel(conn, poolName);
                if (srs.size() != 1) {
                    String msg = "There are " + srs.size() + " SRs with same name: " + poolName;
                    s_logger.warn(msg);
                    return new CopyCmdAnswer(msg);
                } else {
                    poolsr = srs.iterator().next();
                }
                String pUuid = poolsr.getUuid(conn);
                boolean isISCSI = IsISCSI(poolsr.getType(conn));
                String uuid = copy_vhd_from_secondarystorage(conn, tmplpath, pUuid, wait);
                VDI tmpl = getVDIbyUuid(conn, uuid);
                VDI snapshotvdi = tmpl.snapshot(conn, new HashMap<String, String>());
                String snapshotUuid = snapshotvdi.getUuid(conn);
                snapshotvdi.setNameLabel(conn, "Template " + srcTemplate.getName());
                String parentuuid = getVhdParent(conn, pUuid, snapshotUuid, isISCSI);
                VDI parent = getVDIbyUuid(conn, parentuuid);
                Long phySize = parent.getPhysicalUtilisation(conn);
                tmpl.destroy(conn);
                poolsr.scan(conn);
                try{
                    Thread.sleep(5000);
                } catch (Exception e) {
                }

                TemplateObjectTO newVol = new TemplateObjectTO();
                newVol.setUuid(snapshotvdi.getUuid(conn));
                newVol.setPath(newVol.getUuid());
                return new CopyCmdAnswer(newVol);
            }
        }catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + " for template + " + " due to " + e.toString();
            s_logger.warn(msg, e);
            return new CopyCmdAnswer(msg);
        }
        return new CopyCmdAnswer("not implemented yet");
    }

    protected CreateObjectAnswer createVolume(DataTO data) throws BadServerResponse, XenAPIException, XmlRpcException {
        VolumeObjectTO volume = (VolumeObjectTO)data;

        Connection conn = hypervisorResource.getConnection();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)data.getDataStore();
        SR poolSr = hypervisorResource.getStorageRepository(conn, primaryStore.getUuid());
        VDI.Record vdir = new VDI.Record();
        vdir.nameLabel = volume.getName();
        vdir.SR = poolSr;
        vdir.type = Types.VdiType.USER;

        vdir.virtualSize = volume.getSize();
        VDI vdi;

        vdi = VDI.create(conn, vdir);
        vdir = vdi.getRecord(conn);
        VolumeObjectTO newVol = new VolumeObjectTO();
        newVol.setName(vdir.nameLabel);
        newVol.setSize(vdir.virtualSize);
        newVol.setPath(vdir.uuid);

        return new CreateObjectAnswer(newVol);
    }

    protected CopyCmdAnswer cloneVolumeFromBaseTemplate(DataTO srcData, DataTO destData) {
        Connection conn = hypervisorResource.getConnection();
        PrimaryDataStoreTO pool = (PrimaryDataStoreTO)destData.getDataStore();
        VolumeObjectTO volume = (VolumeObjectTO)destData;
        VDI vdi = null;
        try {
            VDI tmpltvdi = null;

            tmpltvdi = getVDIbyUuid(conn, srcData.getPath());
            vdi = tmpltvdi.createClone(conn, new HashMap<String, String>());
            vdi.setNameLabel(conn, volume.getName());


            VDI.Record vdir;
            vdir = vdi.getRecord(conn);
            s_logger.debug("Succesfully created VDI: Uuid = " + vdir.uuid);

            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setName(vdir.nameLabel);
            newVol.setSize(vdir.virtualSize);
            newVol.setPath(vdir.uuid);

            return new CopyCmdAnswer(newVol);
        } catch (Exception e) {
            s_logger.warn("Unable to create volume; Pool=" + pool + "; Disk: ", e);
            return new CopyCmdAnswer(e.toString());
        }
    }


    protected Answer copyVolumeFromImageCacheToPrimary(DataTO srcData, DataTO destData, int wait) {
        Connection conn = hypervisorResource.getConnection();
        VolumeObjectTO srcVolume = (VolumeObjectTO)srcData;
        VolumeObjectTO destVolume = (VolumeObjectTO)destData;
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)destVolume.getDataStore();
        DataStoreTO srcStore = srcVolume.getDataStore();

        if (srcStore instanceof NfsTO) {
            NfsTO nfsStore = (NfsTO)srcStore;
            try {
                SR primaryStoragePool = hypervisorResource.getStorageRepository(conn, primaryStore.getUuid());
                String srUuid = primaryStoragePool.getUuid(conn);
                String volumePath = nfsStore.getUrl() + ":" + srcVolume.getPath();
                String uuid = copy_vhd_from_secondarystorage(conn, volumePath, srUuid, wait );
                VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setPath(uuid);
                newVol.setSize(srcVolume.getSize());

                return new CopyCmdAnswer(newVol);
            } catch (Exception e) {
                String msg = "Catch Exception " + e.getClass().getName() + " due to " + e.toString();
                s_logger.warn(msg, e);
                return new CopyCmdAnswer(e.toString());
            }
        }

        s_logger.debug("unsupported protocol");
        return new CopyCmdAnswer("unsupported protocol");
    }

    protected Answer copyVolumeFromPrimaryToSecondary(DataTO srcData, DataTO destData, int wait) {
        Connection conn = hypervisorResource.getConnection();
        VolumeObjectTO srcVolume = (VolumeObjectTO)srcData;
        VolumeObjectTO destVolume = (VolumeObjectTO)destData;
        DataStoreTO destStore = destVolume.getDataStore();

        if (destStore instanceof NfsTO) {
            SR secondaryStorage = null;
            try {
                NfsTO nfsStore = (NfsTO)destStore;
                URI uri = new URI(nfsStore.getUrl());
                // Create the volume folder
                if (!hypervisorResource.createSecondaryStorageFolder(conn, uri.getHost() + ":" + uri.getPath(), destVolume.getPath())) {
                    throw new InternalErrorException("Failed to create the volume folder.");
                }

                // Create a SR for the volume UUID folder
                secondaryStorage = hypervisorResource.createNfsSRbyURI(conn, new URI(nfsStore.getUrl() + destVolume.getPath()), false);
                // Look up the volume on the source primary storage pool
                VDI srcVdi = getVDIbyUuid(conn, srcVolume.getPath());
                // Copy the volume to secondary storage
                VDI destVdi = hypervisorResource.cloudVDIcopy(conn, srcVdi, secondaryStorage, wait);
                String destVolumeUUID = destVdi.getUuid(conn);

                VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setPath(destVolumeUUID);
                newVol.setSize(srcVolume.getSize());
                return new CopyCmdAnswer(newVol);
            } catch (Exception e) {
                s_logger.debug("Failed to copy volume to secondary: " + e.toString());
                return new CopyCmdAnswer("Failed to copy volume to secondary: " + e.toString());
            } finally {
                hypervisorResource.removeSR(conn, secondaryStorage);
            }
        }
        return new CopyCmdAnswer("unsupported protocol");
    }

    boolean swiftUpload(Connection conn, SwiftTO swift, String container, String ldir, String lfilename, Boolean isISCSI, int wait) {
        String result = null;
        try {
            result = hypervisorResource.callHostPluginAsync(conn, "swiftxen", "swift", wait,
                    "op", "upload", "url", swift.getUrl(), "account", swift.getAccount(),
                    "username", swift.getUserName(), "key", swift.getKey(), "container", container,
                    "ldir", ldir, "lfilename", lfilename, "isISCSI", isISCSI.toString());
            if( result != null && result.equals("true")) {
                return true;
            }
        } catch (Exception e) {
            s_logger.warn("swift upload failed due to " + e.toString(), e);
        }
        return false;
    }

    protected String deleteSnapshotBackup(Connection conn, String path, String secondaryStorageMountPath, String backupUUID) {

        // If anybody modifies the formatting below again, I'll skin them
        String result = hypervisorResource.callHostPlugin(conn, "vmopsSnapshot", "deleteSnapshotBackup", "backupUUID", backupUUID, "path", path, "secondaryStorageMountPath", secondaryStorageMountPath);

        return result;
    }

    public void swiftBackupSnapshot(Connection conn, SwiftTO swift, String srUuid, String snapshotUuid, String container, Boolean isISCSI, int wait)  {
        String lfilename;
        String ldir;
        if ( isISCSI ) {
            ldir = "/dev/VG_XenStorage-" + srUuid;
            lfilename = "VHD-" + snapshotUuid;
        } else {
            ldir = "/var/run/sr-mount/" + srUuid;
            lfilename = snapshotUuid + ".vhd";
        }
        swiftUpload(conn, swift, container, ldir, lfilename, isISCSI, wait);
    }

    private static List<String> serializeProperties(final Object object,
            final Class<?> propertySet) {

        assert object != null;
        assert propertySet != null;
        assert propertySet.isAssignableFrom(object.getClass());

        try {

            final BeanInfo beanInfo = Introspector.getBeanInfo(propertySet);
            final PropertyDescriptor[] descriptors = beanInfo
                    .getPropertyDescriptors();

            final List<String> serializedProperties = new ArrayList<String>();
            for (final PropertyDescriptor descriptor : descriptors) {

                serializedProperties.add(descriptor.getName());
                final Object value = descriptor.getReadMethod().invoke(object);
                serializedProperties.add(value != null ? value.toString()
                        : "null");

            }

            return Collections.unmodifiableList(serializedProperties);

        } catch (IntrospectionException e) {
            s_logger.warn(
                    "Ignored IntrospectionException when serializing class "
                            + object.getClass().getCanonicalName(), e);
        } catch (IllegalArgumentException e) {
            s_logger.warn(
                    "Ignored IllegalArgumentException when serializing class "
                            + object.getClass().getCanonicalName(), e);
        } catch (IllegalAccessException e) {
            s_logger.warn(
                    "Ignored IllegalAccessException when serializing class "
                            + object.getClass().getCanonicalName(), e);
        } catch (InvocationTargetException e) {
            s_logger.warn(
                    "Ignored InvocationTargetException when serializing class "
                            + object.getClass().getCanonicalName(), e);
        }

        return Collections.emptyList();

    }

    private boolean backupSnapshotToS3(final Connection connection,
            final S3TO s3, final String srUuid, final String snapshotUuid,
            final Boolean iSCSIFlag, final int wait) {

        final String filename = iSCSIFlag ? "VHD-" + snapshotUuid
                : snapshotUuid + ".vhd";
        final String dir = (iSCSIFlag ? "/dev/VG_XenStorage-"
                : "/var/run/sr-mount/") + srUuid;
        final String key = StringUtils.join("/", "snapshots", snapshotUuid);

        try {

            final List<String> parameters = new ArrayList<String>(
                    serializeProperties(s3, S3Utils.ClientOptions.class));
            parameters.addAll(Arrays.asList("operation", "put", "directory",
                    dir, "filename", filename, "iSCSIFlag",
                    iSCSIFlag.toString(), "key", key));
            final String result = hypervisorResource.callHostPluginAsync(connection, "s3xen",
                    "s3", wait,
                    parameters.toArray(new String[parameters.size()]));

            if (result != null && result.equals("true")) {
                return true;
            }

        } catch (Exception e) {
            s_logger.error(String.format(
                    "S3 upload failed of snapshot %1$s due to %2$s.",
                    snapshotUuid, e.toString()), e);
        }

        return false;

    }

    protected String backupSnapshot(Connection conn, String primaryStorageSRUuid, String path, String secondaryStorageMountPath, String snapshotUuid, String prevBackupUuid, Boolean isISCSI, int wait) {
        String backupSnapshotUuid = null;

        if (prevBackupUuid == null) {
            prevBackupUuid = "";
        }

        // Each argument is put in a separate line for readability.
        // Using more lines does not harm the environment.
        String backupUuid = UUID.randomUUID().toString();
        String results = hypervisorResource.callHostPluginAsync(conn, "vmopsSnapshot", "backupSnapshot", wait,
                "primaryStorageSRUuid", primaryStorageSRUuid, "path", path, "secondaryStorageMountPath", secondaryStorageMountPath,
                "snapshotUuid", snapshotUuid, "prevBackupUuid", prevBackupUuid, "backupUuid", backupUuid, "isISCSI", isISCSI.toString());
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg = "Could not copy backupUuid: " + backupSnapshotUuid
                    + " from primary storage " + primaryStorageSRUuid + " to secondary storage "
                    + secondaryStorageMountPath + " due to null";
        } else {

            String[] tmp = results.split("#");
            String status = tmp[0];
            backupSnapshotUuid = tmp[1];
            // status == "1" if and only if backupSnapshotUuid != null
            // So we don't rely on status value but return backupSnapshotUuid as an
            // indicator of success.
            if (status != null && status.equalsIgnoreCase("1") && backupSnapshotUuid != null) {
                s_logger.debug("Successfully copied backupUuid: " + backupSnapshotUuid
                        + " to secondary storage");
                return backupSnapshotUuid;
            } else {
                errMsg = "Could not copy backupUuid: " + backupSnapshotUuid
                        + " from primary storage " + primaryStorageSRUuid + " to secondary storage "
                        + secondaryStorageMountPath + " due to " + tmp[1];
            }
        }
        String source = backupUuid + ".vhd";
        hypervisorResource.killCopyProcess(conn, source);
        s_logger.warn(errMsg);
        return null;

    }

    private boolean destroySnapshotOnPrimaryStorageExceptThis(Connection conn, String volumeUuid, String avoidSnapshotUuid){
        try {
            VDI volume = getVDIbyUuid(conn, volumeUuid);
            if (volume == null) {
                throw new InternalErrorException("Could not destroy snapshot on volume " + volumeUuid + " due to can not find it");
            }
            Set<VDI> snapshots = volume.getSnapshots(conn);
            for( VDI snapshot : snapshots ) {
                try {
                    if(! snapshot.getUuid(conn).equals(avoidSnapshotUuid)) {
                        snapshot.destroy(conn);
                    }
                } catch (Exception e) {
                    String msg = "Destroying snapshot: " + snapshot+ " on primary storage failed due to " + e.toString();
                    s_logger.warn(msg, e);
                }
            }
            s_logger.debug("Successfully destroyed snapshot on volume: " + volumeUuid + " execept this current snapshot "+ avoidSnapshotUuid );
            return true;
        } catch (XenAPIException e) {
            String msg = "Destroying snapshot on volume: " + volumeUuid + " execept this current snapshot "+ avoidSnapshotUuid + " failed due to " + e.toString();
            s_logger.error(msg, e);
        } catch (Exception e) {
            String msg = "Destroying snapshot on volume: " + volumeUuid + " execept this current snapshot "+ avoidSnapshotUuid + " failed due to " + e.toString();
            s_logger.warn(msg, e);
        }

        return false;
    }

    protected Answer backupSnasphot(DataTO srcData, DataTO destData, DataTO cacheData, int wait) {
        Connection conn = hypervisorResource.getConnection();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)srcData.getDataStore();
        String primaryStorageNameLabel = primaryStore.getUuid();
        String secondaryStorageUrl = null;
        NfsTO cacheStore = null;
        String destPath = null;
        if (cacheData != null) {
            cacheStore = (NfsTO)cacheData.getDataStore();
            secondaryStorageUrl = cacheStore.getUrl();
            destPath = cacheData.getPath();
        } else {
            cacheStore = (NfsTO)destData.getDataStore();
            secondaryStorageUrl = cacheStore.getUrl();
            destPath = destData.getPath();
        }

        SnapshotObjectTO snapshotTO = (SnapshotObjectTO)srcData;
        SnapshotObjectTO snapshotOnImage = (SnapshotObjectTO)destData;
        String snapshotUuid = snapshotTO.getPath();

        String prevBackupUuid = snapshotOnImage.getParentSnapshotPath();
        String prevSnapshotUuid = snapshotTO.getParentSnapshotPath();

        // By default assume failure
        String details = null;
        String snapshotBackupUuid = null;
        boolean fullbackup = true;
        try {
            SR primaryStorageSR = hypervisorResource.getSRByNameLabelandHost(conn, primaryStorageNameLabel);
            if (primaryStorageSR == null) {
                throw new InternalErrorException("Could not backup snapshot because the primary Storage SR could not be created from the name label: " + primaryStorageNameLabel);
            }
            String psUuid = primaryStorageSR.getUuid(conn);
            Boolean isISCSI = IsISCSI(primaryStorageSR.getType(conn));

            VDI snapshotVdi = getVDIbyUuid(conn, snapshotUuid);
            String snapshotPaUuid = null;
            if ( prevBackupUuid != null ) {
                try {
                    snapshotPaUuid = getVhdParent(conn, psUuid, snapshotUuid, isISCSI);
                    if( snapshotPaUuid != null ) {
                        String snashotPaPaPaUuid = getVhdParent(conn, psUuid, snapshotPaUuid, isISCSI);
                        String prevSnashotPaUuid = getVhdParent(conn, psUuid, prevSnapshotUuid, isISCSI);
                        if (snashotPaPaPaUuid != null && prevSnashotPaUuid!= null && prevSnashotPaUuid.equals(snashotPaPaPaUuid)) {
                            fullbackup = false;
                        }
                    }
                } catch (Exception e) {
                }
            }

            URI uri = new URI(secondaryStorageUrl);
            String secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
            DataStoreTO destStore = destData.getDataStore();
            String folder = destPath;
            if (fullbackup) {
                // the first snapshot is always a full snapshot

                if( !hypervisorResource.createSecondaryStorageFolder(conn, secondaryStorageMountPath, folder)) {
                    details = " Filed to create folder " + folder + " in secondary storage";
                    s_logger.warn(details);
                    return new CopyCmdAnswer(details);
                }
                String snapshotMountpoint = secondaryStorageUrl + "/" + folder;
                SR snapshotSr = null;
                try {
                    snapshotSr = hypervisorResource.createNfsSRbyURI(conn, new URI(snapshotMountpoint), false);
                    VDI backedVdi = hypervisorResource.cloudVDIcopy(conn, snapshotVdi, snapshotSr, wait);
                    snapshotBackupUuid = backedVdi.getUuid(conn);

                    if( destStore instanceof SwiftTO) {
                        try {
                            hypervisorResource.swiftBackupSnapshot(conn, (SwiftTO)destStore, snapshotSr.getUuid(conn), snapshotBackupUuid, "S-" + snapshotTO.getVolume().getVolumeId().toString(), false, wait);
                            snapshotBackupUuid = snapshotBackupUuid + ".vhd";
                        } finally {
                            deleteSnapshotBackup(conn, folder, secondaryStorageMountPath, snapshotBackupUuid);
                        }
                    } else if (destStore instanceof S3TO) {
                        try {
                            backupSnapshotToS3(conn, (S3TO)destStore, snapshotSr.getUuid(conn), snapshotBackupUuid, isISCSI, wait);
                            snapshotBackupUuid = snapshotBackupUuid + ".vhd";
                        } finally {
                            deleteSnapshotBackup(conn, folder, secondaryStorageMountPath, snapshotBackupUuid);
                        }
                    }

                } finally {
                    if( snapshotSr != null) {
                        hypervisorResource.removeSR(conn, snapshotSr);
                    }
                }
            } else {
                String primaryStorageSRUuid = primaryStorageSR.getUuid(conn);
                if( destStore instanceof SwiftTO ) {
                    swiftBackupSnapshot(conn, (SwiftTO)destStore, primaryStorageSRUuid, snapshotPaUuid, "S-" + snapshotTO.getVolume().getVolumeId().toString(), isISCSI, wait);
                    if ( isISCSI ) {
                        snapshotBackupUuid = "VHD-" + snapshotPaUuid;
                    } else {
                        snapshotBackupUuid = snapshotPaUuid + ".vhd";
                    }

                } else if (destStore instanceof S3TO ) {
                    backupSnapshotToS3(conn, (S3TO)destStore, primaryStorageSRUuid, snapshotPaUuid, isISCSI, wait);
                } else {
                    snapshotBackupUuid = backupSnapshot(conn, primaryStorageSRUuid, folder + File.separator + UUID.nameUUIDFromBytes(secondaryStorageMountPath.getBytes())
                             , secondaryStorageMountPath, snapshotUuid, prevBackupUuid, isISCSI, wait);

                }
            }
            String volumeUuid = snapshotTO.getVolume().getPath();
            destroySnapshotOnPrimaryStorageExceptThis(conn, volumeUuid, snapshotUuid);

            SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            newSnapshot.setPath(snapshotBackupUuid);
            if (fullbackup) {
                newSnapshot.setParentSnapshotPath(null);
            } else {
                newSnapshot.setParentSnapshotPath(prevBackupUuid);
            }
            return new CopyCmdAnswer(newSnapshot);
        } catch (XenAPIException e) {
            details = "BackupSnapshot Failed due to " + e.toString();
            s_logger.warn(details, e);
        } catch (Exception e) {
            details = "BackupSnapshot Failed due to " + e.getMessage();
            s_logger.warn(details, e);
        }

        return new CopyCmdAnswer(details);
    }

    protected Answer execute(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        DataStoreTO srcDataStore = srcData.getDataStore();
        DataStoreTO destDataStore = destData.getDataStore();
        DataObjectType srcType = srcData.getObjectType();
        DataObjectType destType = destData.getObjectType();
        DataStoreRole destRole = destDataStore.getRole();
        
        boolean nfs = (srcDataStore instanceof NfsTO) ? true : false;

        if ((srcData.getObjectType() == DataObjectType.TEMPLATE) && (srcDataStore instanceof NfsTO)  && (destData.getDataStore().getRole() == DataStoreRole.Primary)) {
            //copy template to primary storage
            return copyTemplateToPrimaryStorage(srcData, destData, cmd.getWait());
        } else if (srcData.getObjectType() == DataObjectType.TEMPLATE && srcDataStore.getRole() == DataStoreRole.Primary && destDataStore.getRole() == DataStoreRole.Primary) {
            //clone template to a volume
            return cloneVolumeFromBaseTemplate(srcData, destData);
        } else if (srcData.getObjectType() == DataObjectType.VOLUME && srcData.getDataStore().getRole() == DataStoreRole.ImageCache) {
            //copy volume from image cache to primary
            return copyVolumeFromImageCacheToPrimary(srcData, destData, cmd.getWait());
        } else if (srcData.getObjectType() == DataObjectType.VOLUME && srcData.getDataStore().getRole() == DataStoreRole.Primary) {
            return copyVolumeFromPrimaryToSecondary(srcData, destData, cmd.getWait());
        } else if (srcData.getObjectType() == DataObjectType.SNAPSHOT && srcData.getDataStore().getRole() == DataStoreRole.Primary) {
            DataTO cacheData = cmd.getCacheTO();
            return backupSnasphot(srcData, destData, cacheData, cmd.getWait());
        }

        return new Answer(cmd, false, "not implemented yet");
    }
}
