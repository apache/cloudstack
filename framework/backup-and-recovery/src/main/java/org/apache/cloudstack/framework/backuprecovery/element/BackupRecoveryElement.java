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
package org.apache.cloudstack.framework.backuprecovery.element;

import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.component.AdapterBase;
import org.apache.cloudstack.framework.backuprecovery.agent.api.StartupBackupRecoveryProviderCommand;
import org.apache.cloudstack.framework.backuprecovery.resource.BackupRecoveryResource;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

/**
 * Backup and Recovery Element class.
 * To register a Backup and Recovery element, just extend this class
 */
public class BackupRecoveryElement extends AdapterBase implements ResourceStateAdapter, BackupRecoveryElementService {

    @Inject
    ResourceManager resourceManager;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        resourceManager.registerResourceStateAdapter(name, this);
        return true;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupBackupRecoveryProviderCommand)) {
            return null;
        }
        host.setType(Host.Type.BackupRecovery);
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (!(host.getType() == Host.Type.BackupRecovery)) {
            return null;
        }
        return new DeleteHostAnswer(true);
    }

    @Override
    public BackupRecoveryResource createNewResource() {
        return new BackupRecoveryResource();
    }
}
