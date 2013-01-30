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
package com.cloud.hypervisor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Command;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.ManagerBase;

@Component
@Local(value = { HypervisorGuruManager.class } )
public class HypervisorGuruManagerImpl extends ManagerBase implements HypervisorGuruManager {
    public static final Logger s_logger = Logger.getLogger(HypervisorGuruManagerImpl.class.getName());

    @Inject HostDao _hostDao;

    @Inject List<HypervisorGuru> _hvGuruList;
    Map<HypervisorType, HypervisorGuru> _hvGurus = new HashMap<HypervisorType, HypervisorGuru>();

    @PostConstruct
    public void init() {
        for(HypervisorGuru guru : _hvGuruList) {
            _hvGurus.put(guru.getHypervisorType(), guru);
        }
    }

    @Override
    public HypervisorGuru getGuru(HypervisorType hypervisorType) {
        return _hvGurus.get(hypervisorType);
    }

    @Override
    public long getGuruProcessedCommandTargetHost(long hostId, Command cmd) {
        HostVO hostVo = _hostDao.findById(hostId);
        HypervisorGuru hvGuru = null;
        if(hostVo.getType() == Host.Type.Routing) {
            hvGuru = _hvGurus.get(hostVo.getHypervisorType());
        }

        if(hvGuru != null)
            return hvGuru.getCommandHostDelegation(hostId, cmd);

        return hostId;
    }
}
