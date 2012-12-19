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

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
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
        }
        return new Answer((Command)command, false, "not implemented yet"); 
    }
    
    private long getTemplateSize(String url) {
        /*
        HttpGet method = new HttpGet(url);
        HttpClient client = new HttpClient();
        try {
            int responseCode = client.executeMethod(method);
            if (responseCode != HttpStatus.SC_OK) {
                throw new CloudRuntimeException("http get returns error code:" + responseCode);
            }
            method.get
        } catch (HttpException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */
        return 0;
    }
    
    protected Answer directDownloadHttpTemplate(TemplateTO template, PrimaryDataStoreTO primarDataStore) {
        String primaryStoreUuid = primarDataStore.getUuid();
        Connection conn = hypervisorResource.getConnection();
        SR poolsr = null;
        VDI vdi = null;
        try {
            
            Set<SR> srs = SR.getByNameLabel(conn, primaryStoreUuid);
            if (srs.size() != 1) {
                throw new CloudRuntimeException("storage uuid: " + primaryStoreUuid + " is not unique");
            }
            poolsr = srs.iterator().next();
            VDI.Record vdir = new VDI.Record();
            vdir.nameLabel = "Base-Image-" + UUID.randomUUID().toString();
            vdir.SR = poolsr;
            vdir.type = Types.VdiType.USER;

            vdir.virtualSize = template.getSize();
            vdi = VDI.create(conn, vdir);
            
            vdir = vdi.getRecord(conn);
            String vdiLocation = vdir.location;
            Set<PBD> pbds = poolsr.getPBDs(conn);
            if (pbds.size() != 1) {
                throw new CloudRuntimeException("Don't how to handle multiple pbds:" + pbds.size() + " for sr: " + poolsr.getUuid(conn));
            }
            PBD pbd = pbds.iterator().next();
            PBD.Record pbdRec = pbd.getRecord(conn);
            Map<String, String> deviceCfg = pbd.getDeviceConfig(conn);
            String pbdLocation = deviceCfg.get("location");
            if (pbdLocation == null) {
                throw new CloudRuntimeException("Can't get pbd: " + pbd.getUuid(conn) + " location");
            }
            
            String vdiPath = pbdLocation + "/" + vdiLocation;
            //download a url into vdipath
            
        } catch (BadServerResponse e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (XenAPIException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (XmlRpcException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    protected Answer execute(CopyTemplateToPrimaryStorageCmd cmd) {
        ImageOnPrimayDataStoreTO imageTO = cmd.getImage();
        TemplateTO template = imageTO.getTemplate();
        ImageDataStoreTO imageStore = template.getImageDataStore();
        if (imageStore.getType().equalsIgnoreCase("http")) {
            return directDownloadHttpTemplate(template, imageTO.getPrimaryDataStore());
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
