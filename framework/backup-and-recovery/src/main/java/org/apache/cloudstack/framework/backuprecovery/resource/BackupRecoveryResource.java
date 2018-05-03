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

package org.apache.cloudstack.framework.backuprecovery.resource;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import org.apache.cloudstack.framework.backuprecovery.agent.api.StartupBackupRecoveryProviderCommand;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.Map;

public class BackupRecoveryResource implements ServerResource {

    private static final Logger s_logger = Logger.getLogger(BackupRecoveryResource.class);
    private String name;
    private String zoneId;
    private String guid;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (MapUtils.isNotEmpty(params)) {
            this.name = (String) params.get("name");
            if (this.name == null) {
                throw new ConfigurationException("Unable to find name");
            }

            zoneId = (String) params.get("zoneId");
            if (zoneId == null) {
                throw new ConfigurationException("Unable to find zone");
            }

            guid = (String) params.get("guid");
            if (guid == null) {
                throw new ConfigurationException("Unable to find guid");
            }

            /*final String ip = (String) params.get("ip");
            if (ip == null) {
                throw new ConfigurationException("Unable to find IP");
            }*/

            final String adminuser = (String) params.get("adminuser");
            if (adminuser == null) {
                throw new ConfigurationException("Unable to find admin username");
            }

            final String adminpass = (String) params.get("adminpass");
            if (adminpass == null) {
                throw new ConfigurationException("Unable to find admin password");
            }
        }
        return true;
    }

    @Override
    public Host.Type getType() {
        return Host.Type.BackupRecovery;
    }

    @Override
    public StartupCommand[] initialize() {
        final StartupBackupRecoveryProviderCommand sc = new StartupBackupRecoveryProviderCommand();
        sc.setGuid(guid);
        sc.setName(name);
        sc.setDataCenter(zoneId);
        sc.setPod(null);
        sc.setPrivateIpAddress("");
        sc.setStorageIpAddress("");
        sc.setVersion(BackupRecoveryResource.class.getPackage().getImplementationVersion());
        return new StartupCommand[] { sc };
    }

    public PingCommand getCurrentStatus(long id) {
        return new PingCommand(getType(), id);
    }

    public Answer executeRequest(Command cmd) {
        s_logger.debug("Received Command: " + cmd.toString());
        Answer answer = new Answer(cmd, true, "response");
        s_logger.debug("Replying with: " + answer.toString());
        return answer;
    }

    @Override
    public void disconnected() {
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return null;
    }

    @Override
    public int getRunLevel() {
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
