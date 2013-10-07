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
package org.apache.cloudstack.storage.helper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataTO;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.storage.command.ForgetObjectCmd;
import org.apache.cloudstack.storage.command.IntroduceObjectAnswer;
import org.apache.cloudstack.storage.command.IntroduceObjectCmd;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.log4j.Logger;

import javax.inject.Inject;

public class HypervisorHelperImpl implements HypervisorHelper {
    private static final Logger s_logger = Logger.getLogger(HypervisorHelperImpl.class);
    @Inject
    EndPointSelector selector;

    @Override
    public DataTO introduceObject(DataTO object, Scope scope, Long storeId) {
        EndPoint ep = selector.select(scope, storeId);
        IntroduceObjectCmd cmd = new IntroduceObjectCmd(object);
        Answer answer = ep.sendMessage(cmd);
        if (answer == null || !answer.getResult()) {
            String errMsg = answer == null ? null : answer.getDetails();
            throw new CloudRuntimeException("Failed to introduce object, due to " + errMsg);
        }
        IntroduceObjectAnswer introduceObjectAnswer = (IntroduceObjectAnswer)answer;
        return introduceObjectAnswer.getDataTO();
    }

    @Override
    public boolean forgetObject(DataTO object, Scope scope, Long storeId) {
        EndPoint ep = selector.select(scope, storeId);
        ForgetObjectCmd cmd = new ForgetObjectCmd(object);
        Answer answer = ep.sendMessage(cmd);
        if (answer == null || !answer.getResult()) {
            String errMsg = answer == null ? null : answer.getDetails();
            if (errMsg != null) {
                s_logger.debug("Failed to forget object: " + errMsg);
            }
            return false;
        }
        return true;
    }

    @Override
    public SnapshotObjectTO takeSnapshot(SnapshotObjectTO snapshotObjectTO, Scope scope) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean revertSnapshot(SnapshotObjectTO snapshotObjectTO, Scope scope) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
