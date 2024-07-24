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
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types.XenAPIException;

@ResourceWrapper(handles =  GetStorageStatsCommand.class)
public final class CitrixGetStorageStatsCommandWrapper extends CommandWrapper<GetStorageStatsCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixGetStorageStatsCommandWrapper.class);

    @Override
    public Answer execute(final GetStorageStatsCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        try {
            final Set<SR> srs = SR.getByNameLabel(conn, command.getStorageId());
            if (srs.size() != 1) {
                final String msg = "There are " + srs.size() + " storageid: " + command.getStorageId();
                s_logger.warn(msg);
                return new GetStorageStatsAnswer(command, msg);
            }
            final SR sr = srs.iterator().next();
            sr.scan(conn);
            final long capacity = sr.getPhysicalSize(conn);
            final long used = sr.getPhysicalUtilisation(conn);
            return new GetStorageStatsAnswer(command, capacity, used);
        } catch (final XenAPIException e) {
            final String msg = "GetStorageStats Exception:" + e.toString() + "host:" + citrixResourceBase.getHost().getUuid() + "storageid: " + command.getStorageId();
            s_logger.warn(msg);
            return new GetStorageStatsAnswer(command, msg);
        } catch (final XmlRpcException e) {
            final String msg = "GetStorageStats Exception:" + e.getMessage() + "host:" + citrixResourceBase.getHost().getUuid() + "storageid: " + command.getStorageId();
            s_logger.warn(msg);
            return new GetStorageStatsAnswer(command, msg);
        }  catch (final Exception e) {
            final String msg = "GetStorageStats Exception:" + e.getMessage() + "host:" + citrixResourceBase.getHost().getUuid() + "storageid: " + command.getStorageId();
            s_logger.warn(msg);
            return new GetStorageStatsAnswer(command, msg);
        }
    }
}
