// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information regarding copyright ownership.
// The ASF licenses this file to you under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and limitations under the License.

package org.apache.cloudstack.storage;

import java.util.HashMap;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.storage.resource.NfsSecondaryStorageResource;
import org.apache.cloudstack.storage.template.DownloadManagerImpl;
import org.springframework.stereotype.Component;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.script.Script;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MockLocalNfsSecondaryStorageResource extends NfsSecondaryStorageResource {

    private static final Logger logger = LoggerFactory.getLogger(MockLocalNfsSecondaryStorageResource.class);

    public MockLocalNfsSecondaryStorageResource() {
        _dlMgr = new DownloadManagerImpl();
        _storage = new JavaStorageLayer();
        HashMap<String, Object> params = new HashMap<>();

        params.put(StorageLayer.InstanceConfigKey, _storage);
        try {
            _dlMgr.configure("downloadMgr", params);
        } catch (ConfigurationException e) {
            logger.error("Failed to configure the download manager", e);
        }

        createTemplateFromSnapshotXenScript = Script.findScript(getDefaultScriptsDir(), "create_privatetemplate_from_snapshot_xen.sh");

        logger.info("MockLocalNfsSecondaryStorageResource initialized");
    }

    @Override
    public String getRootDir(String secUrl, Integer nfsVersion) {
        String mountPath = "/mnt";

        // Ensure NFS server is reachable
        if (!isNfsServerReachable(secUrl)) {
            logger.error("NFS server is not reachable for URL: {}", secUrl);
            throw new RuntimeException("No route to NFS server: " + secUrl);
        }

        logger.info("NFS server is reachable, returning mount path: {}", mountPath);
        return mountPath;
    }

    private boolean isNfsServerReachable(String secUrl) {
        try {
            // Extract the NFS server IP and add route
            String nfsIp = extractNfsIp(secUrl);
            String gatewayIp = getDefaultGateway();

            if (nfsIp == null || gatewayIp == null) {
                logger.error("Missing NFS or gateway IP. NFS IP: {}, Gateway IP: {}", nfsIp, gatewayIp);
                return false;
            }

            // Add route to the NFS server
            if (!addRouteToNfs(nfsIp, gatewayIp)) {
                logger.error("Failed to add route to NFS server at {}", nfsIp);
                return false;
            }

            // Simulate a basic check (e.g., ping the server)
            if (!pingNfsServer(nfsIp)) {
                logger.error("NFS server is unreachable at {}", nfsIp);
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("Error checking NFS server reachability", e);
            return false;
        }
    }

    private String extractNfsIp(String secUrl) {
        // Extract NFS server IP from the URL
        if (secUrl.startsWith("nfs://")) {
            int endIndex = secUrl.indexOf("/", 6);
            return endIndex > 0 ? secUrl.substring(6, endIndex) : null;
        }
        return null;
    }

    private String getDefaultGateway() {
        // Placeholder logic to fetch default gateway (can be enhanced to fetch actual system data)
        return "10.143.51.1";
    }

    private boolean addRouteToNfs(String nfsIp, String gatewayIp) {
        try {
            String command = String.format("ip route add %s via %s", nfsIp, gatewayIp);
            Script.runSimpleBashScript(command);
            logger.info("Successfully added route to NFS server: {} via gateway: {}", nfsIp, gatewayIp);
            return true;
        } catch (Exception e) {
            logger.error("Error adding route to NFS server", e);
            return false;
        }
    }

    private boolean pingNfsServer(String nfsIp) {
        try {
            String command = String.format("ping -c 1 %s", nfsIp);
            String result = Script.runSimpleBashScript(command);

            if (result == null || result.contains("100% packet loss")) {
                logger.error("Ping to NFS server {} failed", nfsIp);
                return false;
            }

            logger.info("Ping to NFS server {} succeeded", nfsIp);
            return true;
        } catch (Exception e) {
            logger.error("Error pinging NFS server {}", nfsIp, e);
            return false;
        }
    }

    @Override
    public Answer executeRequest(Command cmd) {
        logger.info("Executing request: {}", cmd.getClass().getSimpleName());

        try {
            return super.executeRequest(cmd);
        } catch (Exception e) {
            logger.error("Error executing request", e);
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }
}
