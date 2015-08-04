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

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CopyFileInVmAnswer;
import com.cloud.agent.api.CopyFileInVmCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.ExecutionResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

import java.io.File;

@ResourceWrapper(handles = CopyFileInVmCommand.class)
public class CitrixCopyFileInVmCommandWrapper  extends CommandWrapper<CopyFileInVmCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixCopyFileInVmCommandWrapper.class);

    @Override
    public Answer execute(final CopyFileInVmCommand cmd, final CitrixResourceBase citrixResourceBase) {
        try {
            File file = new File(cmd.getSrc());
            if(file.exists()) {
                if(file.isDirectory()) {
                    for(File f : FileUtils.listFiles(file, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
                        ExecutionResult result = citrixResourceBase.copyFileInVm(cmd.getVmIp(), f, cmd.getDest());
                        if(!result.isSuccess()) {
                            return new CopyFileInVmAnswer(cmd, result.getDetails());
                        }
                    }
                } else {
                    ExecutionResult result = citrixResourceBase.copyFileInVm(cmd.getVmIp(), file, cmd.getDest());
                    if(!result.isSuccess()) {
                        return new CopyFileInVmAnswer(cmd, result.getDetails());
                    }
                }
            }

        } catch (Exception e) {
            s_logger.error("Unable to retrieve files " + cmd.getSrc() + " to copy to VM " + cmd.getVmIp());
            return new CopyFileInVmAnswer(cmd, e);
        }

        return new CopyFileInVmAnswer(cmd);
    }
}
