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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreAnswer;
import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreCmd;
import org.apache.cloudstack.storage.command.CopyCmd;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.CreatePrimaryDataStoreCmd;
import org.apache.cloudstack.storage.command.CreateVolumeFromBaseImageCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.datastore.protocol.DataStoreProtocol;
import org.apache.cloudstack.storage.to.ImageOnPrimayDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeTO;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.DeleteVolumeCommand;
import com.cloud.hypervisor.xen.resource.CitrixResourceBase.SRType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.storage.encoding.DecodedDataObject;
import com.cloud.utils.storage.encoding.DecodedDataStore;
import com.cloud.utils.storage.encoding.Decoder;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VDI;

import edu.emory.mathcs.backport.java.util.Arrays;

public class XenServerStorageResource {
    private static final Logger s_logger = Logger.getLogger(XenServerStorageResource.class);
    protected CitrixResourceBase hypervisorResource;
    
    public XenServerStorageResource(CitrixResourceBase resource) {
        this.hypervisorResource = resource;
    }
    
    public Answer handleStorageCommands(StorageSubSystemCommand command) {
        if (command instanceof CopyCmd) {
            return this.execute((CopyCmd)command);
        } else if (command instanceof AttachPrimaryDataStoreCmd) {
            return this.execute((AttachPrimaryDataStoreCmd)command);
        } else if (command instanceof CreatePrimaryDataStoreCmd) {
            return execute((CreatePrimaryDataStoreCmd) command);
        } else if (command instanceof CreateVolumeFromBaseImageCommand) {
            return execute((CreateVolumeFromBaseImageCommand)command);
        } else if (command instanceof CreateObjectCommand) {
            return execute((CreateObjectCommand) command);
        } else if (command instanceof DeleteVolumeCommand) {
            return execute((DeleteVolumeCommand)command);
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
        Connection conn = hypervisorResource.getConnection();
        long size = this.getTemplateSize(conn, templateUrl);
        return new CreateObjectAnswer(cmd, templateUrl, size);
    }
    protected CreateObjectAnswer execute(CreateObjectCommand cmd) {
        String uriString = cmd.getObjectUri();
        DecodedDataObject obj = null;

        Connection conn = hypervisorResource.getConnection();
        VDI vdi = null;
        boolean result = false;
        String errorMsg = null;
        
        try {
            obj = Decoder.decode(uriString);

            DecodedDataStore store = obj.getStore(); 
            if (obj.getObjType().equalsIgnoreCase("template") && store.getRole().equalsIgnoreCase("image")) {
                return getTemplateSize(cmd, obj.getPath());
            }
            
            long size = obj.getSize();
            String name = obj.getName();
            String storeUuid = store.getUuid();
            SR primaryDataStoreSR = getSRByNameLabel(conn, storeUuid);
            vdi = createVdi(conn, name, primaryDataStoreSR, size);
            VDI.Record record = vdi.getRecord(conn);
            result = true;
            return new CreateObjectAnswer(cmd, record.uuid, record.virtualSize);
        } catch (BadServerResponse e) {
            s_logger.debug("Failed to create volume", e);
            errorMsg = e.toString();
        } catch (XenAPIException e) {
            s_logger.debug("Failed to create volume", e);
            errorMsg = e.toString();
        } catch (XmlRpcException e) {
            s_logger.debug("Failed to create volume", e);
            errorMsg = e.toString();
        } catch (URISyntaxException e) {
            s_logger.debug("Failed to create volume", e);
            errorMsg = e.toString();
        } finally {
            if (!result && vdi != null) {
                try {
                    deleteVDI(conn, vdi);
                } catch (Exception e) {
                    s_logger.debug("Faled to delete vdi: " + vdi.toString());
                }
            }
        }
        
        return new CreateObjectAnswer(cmd, false, errorMsg);
    }
    
    protected Answer execute(DeleteVolumeCommand cmd) {
        VolumeTO volume = null;
        Connection conn = hypervisorResource.getConnection();
        String errorMsg = null;
        try {
            VDI vdi = VDI.getByUuid(conn, volume.getUuid());
            deleteVDI(conn, vdi);
            return new Answer(cmd);
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
        
        return new Answer(cmd, false, errorMsg);
    }
    
    protected Answer execute(CreateVolumeFromBaseImageCommand cmd) {
        VolumeTO volume = cmd.getVolume();
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
    }
    
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
    
    protected Answer directDownloadHttpTemplate(CopyCmd cmd, DecodedDataObject srcObj, DecodedDataObject destObj) {
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
            return new CopyCmdAnswer(cmd, vdi.getUuid(conn));
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
    
    protected Answer execute(CopyCmd cmd) {
        DecodedDataObject srcObj = null;
        DecodedDataObject destObj = null;
        try {
            srcObj = Decoder.decode(cmd.getSrcUri());
            destObj = Decoder.decode(cmd.getDestUri());
        } catch (URISyntaxException e) {
            return new Answer(cmd, false, e.toString());
        }
       
        
        if (srcObj.getPath().startsWith("http")) {
            return directDownloadHttpTemplate(cmd, srcObj, destObj);
        } else {
            return new Answer(cmd, false, "not implemented yet");
            /*
        String tmplturl = cmd.getUrl();
        String poolName = cmd.getPoolUuid();
        int wait = cmd.getWait();
        try {
            URI uri = new URI(tmplturl);
            String tmplpath = uri.getHost() + ":" + uri.getPath();
            Connection conn = hypervisorResource.getConnection();
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
            boolean isISCSI = IsISCSI(poolsr.getType(conn));
            String uuid = copy_vhd_from_secondarystorage(conn, tmplpath, pUuid, wait);
            VDI tmpl = getVDIbyUuid(conn, uuid);
            VDI snapshotvdi = tmpl.snapshot(conn, new HashMap<String, String>());
            String snapshotUuid = snapshotvdi.getUuid(conn);
            snapshotvdi.setNameLabel(conn, "Template " + cmd.getName());
            String parentuuid = getVhdParent(conn, pUuid, snapshotUuid, isISCSI);
            VDI parent = getVDIbyUuid(conn, parentuuid);
            Long phySize = parent.getPhysicalUtilisation(conn);
            tmpl.destroy(conn);
            poolsr.scan(conn);
            try{
                Thread.sleep(5000);
            } catch (Exception e) {
            }
            return new PrimaryStorageDownloadAnswer(snapshotvdi.getUuid(conn), phySize);
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + " on host:" + _host.uuid + " for template: "
                    + tmplturl + " due to " + e.toString();
            s_logger.warn(msg, e);
            return new PrimaryStorageDownloadAnswer(msg);
        }*/
        }
        
    }
}
