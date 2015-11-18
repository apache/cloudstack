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

package com.cloud.network.sync;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.sync.SyncVspCommand;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.dao.NuageVspDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class NuageVspSyncImpl implements NuageVspSync {

    private static final Logger s_logger = Logger.getLogger(NuageVspSyncImpl.class);

    @Inject
    NuageVspDao _nuageVspDao;
    @Inject
    HostDao _hostDao;
    @Inject
    AgentManager _agentMgr;

    public void syncWithNuageVsp(String nuageVspEntity) {
        //Get the NuageVspDevice and get the host information.
        //This information is used to query VSP and synch the corresponding
        //entities
        List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listAll();
        for (NuageVspDeviceVO nuageVspDevice : nuageVspDevices) {
            HostVO nuageVspHost = _hostDao.findById(nuageVspDevice.getHostId());
            _hostDao.loadDetails(nuageVspHost);
            SyncVspCommand cmd = new SyncVspCommand(nuageVspEntity);
            Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                s_logger.error("SyncNuageVspCommand for Nuage VSP Host " + nuageVspHost.getUuid() + " failed");
                if ((null != answer) && (null != answer.getDetails())) {
                    s_logger.error(answer.getDetails());
                }
            }
        }
    }
}
