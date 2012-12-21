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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreAnswer;
import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreCmd;
import org.apache.cloudstack.storage.command.CopyTemplateToPrimaryStorageCmd;
import org.apache.cloudstack.storage.command.CopyTemplateToPrimaryStorageAnswer;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.to.ImageDataStoreTO;
import org.apache.cloudstack.storage.to.ImageOnPrimayDataStoreTO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.TemplateTO;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Connection;
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
        if (command instanceof CopyTemplateToPrimaryStorageCmd) {
            return this.execute((CopyTemplateToPrimaryStorageCmd)command);
        } else if (command instanceof AttachPrimaryDataStoreCmd) {
            return this.execute((AttachPrimaryDataStoreCmd)command);
        }
        return new Answer((Command)command, false, "not implemented yet"); 
    }
    
    private long getTemplateSize(Connection conn, String url) {
        String size = hypervisorResource.callHostPlugin(conn, "storagePlugin", "getTemplateSize", "srcUrl", url);
        if (size == "" || size == null) {
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
    
    protected Answer directDownloadHttpTemplate(CopyTemplateToPrimaryStorageCmd cmd, TemplateTO template, PrimaryDataStoreTO primarDataStore) {
        String primaryStoreUuid = primarDataStore.getUuid();
        Connection conn = hypervisorResource.getConnection();
        SR poolsr = null;
        VDI vdi = null;
        boolean result = false;
        try {
            
            SR sr = SR.getByUuid(conn, primaryStoreUuid);
            if (sr == null) {
                throw new CloudRuntimeException("storage uuid: " + primaryStoreUuid + " is not unique");
            }
            poolsr = sr;
            VDI.Record vdir = new VDI.Record();
            vdir.nameLabel = "Base-Image-" + UUID.randomUUID().toString();
            vdir.SR = poolsr;
            vdir.type = Types.VdiType.USER;
            
            vdir.virtualSize = getTemplateSize(conn, template.getPath());
            vdi = VDI.create(conn, vdir);
            
            vdir = vdi.getRecord(conn);
            String vdiLocation = vdir.location;
            Set<PBD> pbds = poolsr.getPBDs(conn);
            if (pbds.size() != 1) {
                throw new CloudRuntimeException("Don't how to handle multiple pbds:" + pbds.size() + " for sr: " + poolsr.getUuid(conn));
            }
            PBD pbd = pbds.iterator().next();
            Map<String, String> deviceCfg = pbd.getDeviceConfig(conn);
            String pbdLocation = deviceCfg.get("location");
            if (pbdLocation == null) {
                throw new CloudRuntimeException("Can't get pbd: " + pbd.getUuid(conn) + " location");
            }
            
            String vdiPath = pbdLocation + "/" + vdiLocation + ".vhd";
            //download a url into vdipath
            //downloadHttpToLocalFile(vdiPath, template.getPath());
            hypervisorResource.callHostPlugin(conn, "storagePlugin", "downloadTemplateFromUrl", "destPath", vdiPath, "srcUrl", template.getPath());
            result = true;
            return new CopyTemplateToPrimaryStorageAnswer(cmd, vdi.getUuid(conn));
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
        PrimaryDataStoreTO dataStore = cmd.getDataStore();
        Connection conn = hypervisorResource.getConnection();
        try {
            SR sr = hypervisorResource.getStorageRepository(conn, dataStore.getUuid());
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
    
    protected Answer execute(CopyTemplateToPrimaryStorageCmd cmd) {
        ImageOnPrimayDataStoreTO imageTO = cmd.getImage();
        TemplateTO template = imageTO.getTemplate();
        ImageDataStoreTO imageStore = template.getImageDataStore();
        if (imageStore.getType().equalsIgnoreCase("http")) {
            return directDownloadHttpTemplate(cmd, template, imageTO.getPrimaryDataStore());
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
