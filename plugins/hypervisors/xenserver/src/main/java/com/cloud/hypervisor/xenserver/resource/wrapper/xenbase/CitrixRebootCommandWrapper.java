//
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
//

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  RebootCommand.class)
public final class CitrixRebootCommandWrapper extends CommandWrapper<RebootCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixRebootCommandWrapper.class);

    @Override
    public Answer execute(final RebootCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        s_logger.debug("7. The VM " + command.getVmName() + " is in Starting state");
        try {
            Set<VM> vms = null;
            try {
                vms = VM.getByNameLabel(conn, command.getVmName());
            } catch (final XenAPIException e0) {
                s_logger.debug("getByNameLabel failed " + e0.toString());
                return new RebootAnswer(command, "getByNameLabel failed " + e0.toString(), false);
            } catch (final Exception e0) {
                s_logger.debug("getByNameLabel failed " + e0.getMessage());
                return new RebootAnswer(command, "getByNameLabel failed", false);
            }
            for (final VM vm : vms) {
                try {
                    citrixResourceBase.rebootVM(conn, vm, vm.getNameLabel(conn));
                } catch (final Exception e) {
                    final String msg = e.toString();
                    s_logger.warn(msg, e);
                    return new RebootAnswer(command, msg, false);
                }
            }
            return new RebootAnswer(command, "reboot succeeded", true);
        } finally {
            s_logger.debug("8. The VM " + command.getVmName() + " is in Running state");
        }
    }
}
