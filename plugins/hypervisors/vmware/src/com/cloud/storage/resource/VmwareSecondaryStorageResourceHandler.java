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
package com.cloud.storage.resource;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.hypervisor.vmware.manager.VmwareHostService;
import com.cloud.hypervisor.vmware.manager.VmwareStorageManager;
import com.cloud.hypervisor.vmware.manager.VmwareStorageManagerImpl;
import com.cloud.hypervisor.vmware.manager.VmwareStorageMount;
import com.cloud.hypervisor.vmware.mo.ClusterMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.VmwareHostType;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHostNetworkSummary;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.Pair;
import com.google.gson.Gson;
import com.vmware.vim25.ManagedObjectReference;

public class VmwareSecondaryStorageResourceHandler implements SecondaryStorageResourceHandler, VmwareHostService, VmwareStorageMount {
    private static final Logger s_logger = Logger.getLogger(VmwareSecondaryStorageResourceHandler.class);

    private final PremiumSecondaryStorageResource _resource;
    private final VmwareStorageManager _storageMgr;

    private final Gson _gson;

    /*
	private Map<String, HostMO> _activeHosts = new HashMap<String, HostMO>();
     */

    public VmwareSecondaryStorageResourceHandler(PremiumSecondaryStorageResource resource) {
        _resource = resource;
        _storageMgr = new VmwareStorageManagerImpl(this);
        _gson = GsonHelper.getGsonLogger();
    }

    @Override
    public Answer executeRequest(Command cmd) {
        Answer answer;
        if (cmd instanceof PrimaryStorageDownloadCommand) {
            answer = execute((PrimaryStorageDownloadCommand)cmd);
        } else if(cmd instanceof BackupSnapshotCommand) {
            answer = execute((BackupSnapshotCommand)cmd);
        } else if(cmd instanceof CreatePrivateTemplateFromVolumeCommand) {
            answer = execute((CreatePrivateTemplateFromVolumeCommand)cmd);
        } else if(cmd instanceof CreatePrivateTemplateFromSnapshotCommand) {
            answer = execute((CreatePrivateTemplateFromSnapshotCommand)cmd);
        } else if(cmd instanceof CopyVolumeCommand) {
            answer = execute((CopyVolumeCommand)cmd);
        } else if(cmd instanceof CreateVolumeFromSnapshotCommand) {
            answer = execute((CreateVolumeFromSnapshotCommand)cmd);
        } else {
            answer =  _resource.defaultAction(cmd);
        }

        // special handling to pass-back context info for cleanups
        if(cmd.getContextParam("execid") != null) {
            answer.setContextParam("execid", cmd.getContextParam("execid"));
        }

        if(cmd.getContextParam("checkpoint") != null) {
            answer.setContextParam("checkpoint", cmd.getContextParam("checkpoint"));
        }

        if(cmd.getContextParam("checkpoint2") != null) {
            answer.setContextParam("checkpoint2", cmd.getContextParam("checkpoint2"));
        }

        return answer;
    }

    private Answer execute(PrimaryStorageDownloadCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Executing resource PrimaryStorageDownloadCommand: " + _gson.toJson(cmd));
        }

        return _storageMgr.execute(this, cmd);
    }

    private Answer execute(BackupSnapshotCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Executing resource BackupSnapshotCommand: " + _gson.toJson(cmd));
        }

        return _storageMgr.execute(this, cmd);
    }

    private Answer execute(CreatePrivateTemplateFromVolumeCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Executing resource CreatePrivateTemplateFromVolumeCommand: " + _gson.toJson(cmd));
        }

        return _storageMgr.execute(this, cmd);
    }

    private Answer execute(CreatePrivateTemplateFromSnapshotCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Executing resource CreatePrivateTemplateFromVolumeCommand: " + _gson.toJson(cmd));
        }

        return _storageMgr.execute(this, cmd);
    }

    private Answer execute(CopyVolumeCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Executing resource CopyVolumeCommand: " + _gson.toJson(cmd));
        }

        return _storageMgr.execute(this, cmd);
    }

    private Answer execute(CreateVolumeFromSnapshotCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Executing resource CreateVolumeFromSnapshotCommand: " + _gson.toJson(cmd));
        }

        return _storageMgr.execute(this, cmd);
    }

    @Override
    public VmwareContext getServiceContext(Command cmd) {
        String guid = cmd.getContextParam("guid");
        if(guid == null || guid.isEmpty()) {
            s_logger.error("Invalid command context parameter guid");
            return null;
        }

        String username = cmd.getContextParam("username");
        if(username == null || username.isEmpty()) {
            s_logger.error("Invalid command context parameter username");
            return null;
        }

        String password = cmd.getContextParam("password");

        // validate command guid parameter
        String[] tokens = guid.split("@");
        if(tokens == null || tokens.length != 2) {
            s_logger.error("Invalid content in command context parameter guid");
            return null;
        }

        String vCenterAddress = tokens[1];
        String[] hostTokens = tokens[0].split(":");
        if(hostTokens == null || hostTokens.length != 2) {
            s_logger.error("Invalid content in command context parameter guid");
            return null;
        }

        try {
            _resource.ensureOutgoingRuleForAddress(vCenterAddress);
            VmwareContext context = null;

            // cached VmwareContext may be timed out in vCenter, give it a chance to reclaim a new context from factory
            for(int i = 0; i < 2; i++) {
                context = VmwareSecondaryStorageContextFactory.create(vCenterAddress, username, password);
                if(!validateContext(context, cmd)) {
                    invalidateServiceContext(context);
                }
            }

            if(context != null) {
                context.registerStockObject("serviceconsole", cmd.getContextParam("serviceconsole"));
                context.registerStockObject("manageportgroup", cmd.getContextParam("manageportgroup"));
            }
            return context;
        } catch(Exception e) {
            s_logger.error("Unexpected exception " + e.toString(), e);
            return null;
        }
    }

    @Override
    public void invalidateServiceContext(VmwareContext context) {
        VmwareSecondaryStorageContextFactory.invalidate(context);
    }

    @Override
    public VmwareHypervisorHost getHyperHost(VmwareContext context, Command cmd) {
        String guid = cmd.getContextParam("guid");
        assert(guid != null);

        String[] tokens = guid.split("@");
        assert(tokens != null && tokens.length == 2);

        ManagedObjectReference morHyperHost = new ManagedObjectReference();
        String[] hostTokens = tokens[0].split(":");
        if(hostTokens == null || hostTokens.length != 2) {
            s_logger.error("Invalid content in command context parameter guid");
            return null;
        }

        morHyperHost.setType(hostTokens[0]);
        morHyperHost.setValue(hostTokens[1]);

        if(morHyperHost.getType().equalsIgnoreCase("HostSystem")) {
            HostMO hostMo =  new HostMO(context, morHyperHost);

            try {

                ManagedObjectReference mor = hostMo.getHyperHostCluster();
                ClusterMO clusterMo = new ClusterMO(hostMo.getContext(), mor);
                List<Pair<ManagedObjectReference, String>> hostsInCluster = clusterMo.getClusterHosts();
                for(Pair<ManagedObjectReference, String> hostPair : hostsInCluster) {
                    HostMO hostIteratorMo = new HostMO(hostMo.getContext(), hostPair.first());

                    VmwareHypervisorHostNetworkSummary netSummary = hostIteratorMo.getHyperHostNetworkSummary(
                            hostIteratorMo.getHostType() == VmwareHostType.ESXi ? cmd.getContextParam("manageportgroup") : cmd.getContextParam("serviceconsole"));
                    _resource.ensureOutgoingRuleForAddress(netSummary.getHostIp());

                    s_logger.info("Setup firewall rule for host: " + netSummary.getHostIp());
                }
            } catch(Throwable e) {
                s_logger.warn("Unable to retrive host network information due to exception " + e.toString() + ", host: " + hostTokens[0] + "-" + hostTokens[1]);
            }

            return hostMo;
        }

        assert(false);
        return new ClusterMO(context, morHyperHost);
    }

    @Override
    public String getWorkerName(VmwareContext context, Command cmd, int workerSequence) {
        assert(cmd.getContextParam("worker") != null);
        assert(workerSequence < 2);

        if(workerSequence == 0)
            return cmd.getContextParam("worker");
        return cmd.getContextParam("worker2");
    }

    @Override
    public String getMountPoint(String storageUrl) {
        return _resource.getRootDir(storageUrl);
    }

    private boolean validateContext(VmwareContext context, Command cmd) {
        String guid = cmd.getContextParam("guid");
        assert(guid != null);

        String[] tokens = guid.split("@");
        assert(tokens != null && tokens.length == 2);

        ManagedObjectReference morHyperHost = new ManagedObjectReference();
        String[] hostTokens = tokens[0].split(":");
        assert(hostTokens.length == 2);

        morHyperHost.setType(hostTokens[0]);
        morHyperHost.setValue(hostTokens[1]);

        if(morHyperHost.getType().equalsIgnoreCase("HostSystem")) {
            HostMO hostMo =  new HostMO(context, morHyperHost);
            try {
                VmwareHypervisorHostNetworkSummary netSummary = hostMo.getHyperHostNetworkSummary(
                	hostMo.getHostType() == VmwareHostType.ESXi ? cmd.getContextParam("manageportgroup") : cmd.getContextParam("serviceconsole"));
                assert(netSummary != null);
                if(netSummary.getHostIp() != null && !netSummary.getHostIp().isEmpty()) {
                    if(s_logger.isDebugEnabled()) {
                        s_logger.debug("Context validation succeeded. Validated via host: " + netSummary.getHostIp() + ", guid: " + guid);
                    }
                    return true;
                }

                s_logger.warn("Context validation failed due to invalid host network summary");
                return false;
            } catch(Throwable e) {
                s_logger.warn("Context validation failed due to " + VmwareHelper.getExceptionMessage(e));
                return false;
            }
        }

        assert(false);
        return true;
    }
}
