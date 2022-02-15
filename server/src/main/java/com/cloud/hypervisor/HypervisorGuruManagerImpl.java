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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Command;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;

@Component
public class HypervisorGuruManagerImpl extends ManagerBase implements HypervisorGuruManager {
    public static final Logger s_logger = Logger.getLogger(HypervisorGuruManagerImpl.class.getName());

    @Inject
    HostDao _hostDao;

    List<HypervisorGuru> _hvGuruList;
    Map<HypervisorType, HypervisorGuru> _hvGurus = new ConcurrentHashMap<HypervisorType, HypervisorGuru>();

    @PostConstruct
    public void init() {
        for (HypervisorGuru guru : _hvGuruList) {
            _hvGurus.put(guru.getHypervisorType(), guru);
        }
    }

    @Override
    public HypervisorGuru getGuru(HypervisorType hypervisorType) {
        if (hypervisorType == null) {
            return null;
        }

        HypervisorGuru result = _hvGurus.get(hypervisorType);

        if (result == null) {
            for (HypervisorGuru guru : _hvGuruList) {
                if (guru.getHypervisorType() == hypervisorType) {
                    _hvGurus.put(hypervisorType, guru);
                    result = guru;
                    break;
                }
            }
        }

        return result;
    }

    @Override
    public long getGuruProcessedCommandTargetHost(long hostId, Command cmd) {
        for (HypervisorGuru guru : _hvGuruList) {
            Pair<Boolean, Long> result = guru.getCommandHostDelegation(hostId, cmd);
            if (result.first()) {
                return result.second();
            }
        }
        return hostId;
    }

    @Override
    public long getGuruProcessedCommandTargetHost(long hostId, Command cmd, HypervisorType hypervisorType) {
        HypervisorGuru guru = getGuru(hypervisorType);
        Pair<Boolean, Long> result = guru.getCommandHostDelegation(hostId, cmd);
        return result.first() ? result.second() : hostId;
    }

    public List<HypervisorGuru> getHvGuruList() {
        return _hvGuruList;
    }

    @Inject
    public void setHvGuruList(List<HypervisorGuru> hvGuruList) {
        this._hvGuruList = hvGuruList;
    }

}
