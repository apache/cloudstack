//
//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
//

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVolumeStatsAnswer;
import com.cloud.agent.api.GetVolumeStatsCommand;
import com.cloud.agent.api.VolumeStatsEntry;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.VDI;

@ResourceWrapper(handles = GetVolumeStatsCommand.class)
public final class CitrixGetVolumeStatsCommandWrapper extends CommandWrapper<GetVolumeStatsCommand, Answer, CitrixResourceBase> {
    private static final Logger s_logger = Logger.getLogger(CitrixGetVolumeStatsCommandWrapper.class);

    @Override
    public Answer execute(final GetVolumeStatsCommand cmd, final CitrixResourceBase citrixResourceBase) {
        Connection conn = citrixResourceBase.getConnection();
        HashMap<String, VolumeStatsEntry> statEntry = new HashMap<String, VolumeStatsEntry>();
        for (String volumeUuid : cmd.getVolumeUuids()) {
            VDI vdi = citrixResourceBase.getVDIbyUuid(conn, volumeUuid, false);
            if (vdi != null) {
                try {
                    VolumeStatsEntry vse = new VolumeStatsEntry(volumeUuid, vdi.getPhysicalUtilisation(conn), vdi.getVirtualSize(conn));
                    statEntry.put(volumeUuid, vse);
                } catch (Exception e) {
                    s_logger.warn("Unable to get volume stats", e);
                    statEntry.put(volumeUuid, new VolumeStatsEntry(volumeUuid, -1, -1));
                }
            } else {
                s_logger.warn("VDI not found for path " + volumeUuid);
                statEntry.put(volumeUuid, new VolumeStatsEntry(volumeUuid, -1L, -1L));
            }
        }
        return new GetVolumeStatsAnswer(cmd, "", statEntry);
    }

}
