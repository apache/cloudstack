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
package org.apache.cloudstack.backuprecovery.resource;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import org.apache.cloudstack.framework.backuprecovery.agent.api.AssignVMToBackupPolicyAnswer;
import org.apache.cloudstack.framework.backuprecovery.agent.api.AssignVMToBackupPolicyCommand;
import org.apache.cloudstack.framework.backuprecovery.agent.api.CheckBackupPolicyAnswer;
import org.apache.cloudstack.framework.backuprecovery.agent.api.CheckBackupPolicyCommand;
import org.apache.cloudstack.framework.backuprecovery.agent.api.ListBackupPoliciesAnswer;
import org.apache.cloudstack.framework.backuprecovery.agent.api.ListBackupPoliciesCommand;
import org.apache.cloudstack.framework.backuprecovery.agent.api.to.BackupPolicyTO;
import org.apache.cloudstack.framework.backuprecovery.resource.BackupRecoveryResource;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DummyBackupRecoveryResource extends BackupRecoveryResource {

    private static final Logger s_logger = Logger.getLogger(DummyBackupRecoveryResource.class);

    private BackupPolicyTO policy1 = new BackupPolicyTO("aaaa-aaaa", "Policy A", "Gold policy");
    private BackupPolicyTO policy2 = new BackupPolicyTO("bbbb-bbbb", "Policy B", "Silver policy");

    private List<BackupPolicyTO> policies = Arrays.asList(policy1, policy2);

    @Override
    public Answer executeRequest(Command cmd) {

        s_logger.debug("Received Command: " + cmd.toString());
        Answer answer = new Answer(cmd, true, "response");
        if (cmd instanceof ListBackupPoliciesCommand) {
            answer = execute((ListBackupPoliciesCommand) cmd);
        } else if (cmd instanceof CheckBackupPolicyCommand) {
            answer = execute((CheckBackupPolicyCommand) cmd);
        } else if (cmd instanceof AssignVMToBackupPolicyCommand) {
            answer = execute((AssignVMToBackupPolicyCommand) cmd);
        }
        s_logger.debug("Replying with: " + answer.toString());
        return answer;
    }

    private Answer execute(AssignVMToBackupPolicyCommand cmd) {
        s_logger.debug("Assigning VM " + cmd.getVmUuid() + " to Backup policy: " + cmd.getBackupPolicyUuid());
        return new AssignVMToBackupPolicyAnswer(true);
    }

    private Answer execute(CheckBackupPolicyCommand cmd) {
        s_logger.debug("Checking if backup policy " + cmd.getPolicyUuid() + " exists");
        boolean result = false;
        String policyUuid = cmd.getPolicyUuid();
        for (BackupPolicyTO policy: policies) {
            if (policy.getId().equals(policyUuid)) {
                result = true;
                break;
            }
        }
        return new CheckBackupPolicyAnswer(result);
    }

    private Answer execute(ListBackupPoliciesCommand cmd) {
        s_logger.debug("Listing existing backup policies");
        return new ListBackupPoliciesAnswer(true, policies);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        return true;
    }

    @Override
    public StartupCommand[] initialize() {
        return super.initialize();
    }

    @Override
    public String getName() {
        return "DummyBackupRecoveryProvider";
    }
}
