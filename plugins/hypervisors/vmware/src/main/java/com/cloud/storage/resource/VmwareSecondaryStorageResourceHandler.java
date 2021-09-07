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
import org.apache.log4j.NDC;

import com.google.gson.Gson;
import com.vmware.vim25.ManagedObjectReference;

import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.resource.SecondaryStorageResourceHandler;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateEntityDownloadURLCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.hypervisor.vmware.manager.VmwareHostService;
import com.cloud.hypervisor.vmware.manager.VmwareStorageManager;
import com.cloud.hypervisor.vmware.manager.VmwareStorageManagerImpl;
import com.cloud.hypervisor.vmware.manager.VmwareStorageMount;
import com.cloud.hypervisor.vmware.mo.ClusterMO;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.VmwareHostType;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHostNetworkSummary;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;

public class VmwareSecondaryStorageResourceHandler implements SecondaryStorageResourceHandler, VmwareHostService, VmwareStorageMount {
    private static final Logger s_logger = Logger.getLogger(VmwareSecondaryStorageResourceHandler.class);

    private final PremiumSecondaryStorageResource _resource;
    private final VmwareStorageManager _storageMgr;

    private final Gson _gson;
    private final StorageSubsystemCommandHandler storageSubsystemHandler;

    private static ThreadLocal<VmwareContext> currentContext = new ThreadLocal<VmwareContext>();

    /*
     * private Map<String, HostMO> _activeHosts = new HashMap<String, HostMO>();
     */

    public VmwareSecondaryStorageResourceHandler(PremiumSecondaryStorageResource resource, String nfsVersion) {
        _resource = resource;
        _storageMgr = new VmwareStorageManagerImpl(this, nfsVersion);
        _gson = GsonHelper.getGsonLogger();

        VmwareStorageProcessor storageProcessor = new VmwareStorageProcessor(this, true, this, resource.getTimeout(), null, null, _resource, nfsVersion);
        VmwareStorageSubsystemCommandHandler vmwareStorageSubsystemCommandHandler = new VmwareStorageSubsystemCommandHandler(storageProcessor, nfsVersion);
        vmwareStorageSubsystemCommandHandler.setStorageResource(_resource);
        vmwareStorageSubsystemCommandHandler.setStorageManager(_storageMgr);
        storageSubsystemHandler = vmwareStorageSubsystemCommandHandler;
    }

    private static String getCommandLogTitle(Command cmd) {
        StringBuffer sb = new StringBuffer();
        if (cmd.getContextParam("job") != null) {
            sb.append(cmd.getContextParam("job"));
        }

        sb.append(", cmd: ").append(cmd.getClass().getSimpleName());

        return sb.toString();
    }

    @Override
    public Answer executeRequest(Command cmd) {

        try {
            Answer answer;
            NDC.push(getCommandLogTitle(cmd));

            if (s_logger.isDebugEnabled())
                s_logger.debug("Executing " + _gson.toJson(cmd));

            if (cmd instanceof PrimaryStorageDownloadCommand) {
                answer = execute((PrimaryStorageDownloadCommand)cmd);
            } else if (cmd instanceof BackupSnapshotCommand) {
                answer = execute((BackupSnapshotCommand)cmd);
            } else if (cmd instanceof CreatePrivateTemplateFromVolumeCommand) {
                answer = execute((CreatePrivateTemplateFromVolumeCommand)cmd);
            } else if (cmd instanceof CreatePrivateTemplateFromSnapshotCommand) {
                answer = execute((CreatePrivateTemplateFromSnapshotCommand)cmd);
            } else if (cmd instanceof CopyVolumeCommand) {
                answer = execute((CopyVolumeCommand)cmd);
            } else if (cmd instanceof CreateVolumeFromSnapshotCommand) {
                answer = execute((CreateVolumeFromSnapshotCommand)cmd);
            } else if (cmd instanceof StorageSubSystemCommand) {
                answer = storageSubsystemHandler.handleStorageCommands((StorageSubSystemCommand)cmd);
            } else if (cmd instanceof CreateEntityDownloadURLCommand) {
                answer = execute((CreateEntityDownloadURLCommand)cmd);
            } else {
                answer = _resource.defaultAction(cmd);
            }

            // special handling to pass-back context info for cleanups
            if (cmd.getContextParam("execid") != null) {
                answer.setContextParam("execid", cmd.getContextParam("execid"));
            }

            if (cmd.getContextParam("checkpoint") != null) {
                answer.setContextParam("checkpoint", cmd.getContextParam("checkpoint"));
            }

            if (cmd.getContextParam("checkpoint2") != null) {
                answer.setContextParam("checkpoint2", cmd.getContextParam("checkpoint2"));
            }

            if (s_logger.isDebugEnabled())
                s_logger.debug("Command execution answer: " + _gson.toJson(answer));

            return answer;
        } finally {
            if (s_logger.isDebugEnabled())
                s_logger.debug("Done executing " + _gson.toJson(cmd));
            recycleServiceContext();
            NDC.pop();
        }
    }

    protected Answer execute(CreateEntityDownloadURLCommand cmd) {
        boolean success = _storageMgr.execute(this, cmd);
        if (success) {
            return _resource.defaultAction(cmd);
        }
        return new Answer(cmd, false, "Failed to download");
    }

    private Answer execute(PrimaryStorageDownloadCommand cmd) {
        return _storageMgr.execute(this, cmd);
    }

    private Answer execute(BackupSnapshotCommand cmd) {
        return _storageMgr.execute(this, cmd);
    }

    private Answer execute(CreatePrivateTemplateFromVolumeCommand cmd) {
        return _storageMgr.execute(this, cmd);
    }

    private Answer execute(CreatePrivateTemplateFromSnapshotCommand cmd) {
        return _storageMgr.execute(this, cmd);
    }

    private Answer execute(CopyVolumeCommand cmd) {
        return _storageMgr.execute(this, cmd);
    }

    private Answer execute(CreateVolumeFromSnapshotCommand cmd) {
        return _storageMgr.execute(this, cmd);
    }

    @Override
    public VmwareContext getServiceContext(Command cmd) {
        String guid = cmd.getContextParam("guid");
        if (guid == null || guid.isEmpty()) {
            s_logger.error("Invalid command context parameter guid");
            return null;
        }

        String username = cmd.getContextParam("username");
        if (username == null || username.isEmpty()) {
            s_logger.error("Invalid command context parameter username");
            return null;
        }

        String password = cmd.getContextParam("password");

        // validate command guid parameter
        String[] tokens = guid.split("@");
        if (tokens == null || tokens.length != 2) {
            s_logger.error("Invalid content in command context parameter guid");
            return null;
        }

        String vCenterAddress = tokens[1];
        String[] hostTokens = tokens[0].split(":");
        if (hostTokens == null || hostTokens.length != 2) {
            s_logger.error("Invalid content in command context parameter guid");
            return null;
        }

        int vCenterSessionTimeout = NumbersUtil.parseInt(cmd.getContextParam("vCenterSessionTimeout"), 1200000);

        try {
            _resource.ensureOutgoingRuleForAddress(vCenterAddress);

            VmwareContext context = currentContext.get();
            if (context != null && !context.validate()) {
                invalidateServiceContext(context);
                context = null;
            }
            if (context == null) {
                s_logger.info("Open new VmwareContext. vCenter: " + vCenterAddress + ", user: " + username + ", password: " + StringUtils.getMaskedPasswordForDisplay(password));
                VmwareSecondaryStorageContextFactory.setVcenterSessionTimeout(vCenterSessionTimeout);
                context = VmwareSecondaryStorageContextFactory.getContext(vCenterAddress, username, password);
            }
            if (context != null) {
                context.registerStockObject("serviceconsole", cmd.getContextParam("serviceconsole"));
                context.registerStockObject("manageportgroup", cmd.getContextParam("manageportgroup"));
                context.registerStockObject("noderuninfo", cmd.getContextParam("noderuninfo"));
            }
            currentContext.set(context);
            return context;
        } catch (Exception e) {
            s_logger.error("Unexpected exception " + e.toString(), e);
            return null;
        }
    }

    public void recycleServiceContext() {
        if (currentContext.get() != null) {
            VmwareContext context = currentContext.get();
            currentContext.set(null);
            assert (context.getPool() != null);
            context.getPool().registerContext(context);
        }
    }

    @Override
    public void invalidateServiceContext(VmwareContext context) {
        currentContext.set(null);
        VmwareSecondaryStorageContextFactory.invalidate(context);
    }

    @Override
    public VmwareHypervisorHost getHyperHost(VmwareContext context, Command cmd) {
        String guid = cmd.getContextParam("guid");
        assert (guid != null);

        String[] tokens = guid.split("@");
        assert (tokens != null && tokens.length == 2);

        ManagedObjectReference morHyperHost = new ManagedObjectReference();
        String[] hostTokens = tokens[0].split(":");
        if (hostTokens == null || hostTokens.length != 2) {
            s_logger.error("Invalid content in command context parameter guid");
            return null;
        }

        morHyperHost.setType(hostTokens[0]);
        morHyperHost.setValue(hostTokens[1]);

        if (morHyperHost.getType().equalsIgnoreCase("HostSystem")) {
            HostMO hostMo = new HostMO(context, morHyperHost);

            try {

                ManagedObjectReference mor = hostMo.getHyperHostCluster();
                ClusterMO clusterMo = new ClusterMO(hostMo.getContext(), mor);
                List<Pair<ManagedObjectReference, String>> hostsInCluster = clusterMo.getClusterHosts();
                for (Pair<ManagedObjectReference, String> hostPair : hostsInCluster) {
                    HostMO hostIteratorMo = new HostMO(hostMo.getContext(), hostPair.first());

                    VmwareHypervisorHostNetworkSummary netSummary =
                        hostIteratorMo.getHyperHostNetworkSummary(hostIteratorMo.getHostType() == VmwareHostType.ESXi ? cmd.getContextParam("manageportgroup")
                            : cmd.getContextParam("serviceconsole"));
                    _resource.ensureOutgoingRuleForAddress(netSummary.getHostIp());

                    s_logger.info("Setup firewall rule for host: " + netSummary.getHostIp());
                }
            } catch (Throwable e) {
                s_logger.warn("Unable to retrive host network information due to exception " + e.toString() + ", host: " + hostTokens[0] + "-" + hostTokens[1]);
            }

            return hostMo;
        }

        assert (false);
        return new ClusterMO(context, morHyperHost);
    }

    @Override
    public String getWorkerName(VmwareContext context, Command cmd, int workerSequence, DatastoreMO dsMo) {
        assert (cmd.getContextParam("worker") != null);
        assert (workerSequence < 2);

        if (workerSequence == 0)
            return cmd.getContextParam("worker");
        return cmd.getContextParam("worker2");
    }

    @Override
    public String getMountPoint(String storageUrl, String nfsVersion) {
        return _resource.getRootDir(storageUrl, nfsVersion);
    }
}
