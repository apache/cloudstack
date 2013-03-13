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
package com.cloud.ha;

import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.host.HostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

@Component
@Local(value=FenceBuilder.class)
public class RecreatableFencer extends AdapterBase implements FenceBuilder {
    private static final Logger s_logger = Logger.getLogger(RecreatableFencer.class);
    @Inject VolumeDao _volsDao;
    @Inject PrimaryDataStoreDao _poolDao;
    
    public RecreatableFencer() {
        super();
    }

    @Override
    public Boolean fenceOff(VMInstanceVO vm, HostVO host) {
        VirtualMachine.Type type = vm.getType();
        if (type != VirtualMachine.Type.ConsoleProxy && type != VirtualMachine.Type.DomainRouter && type != VirtualMachine.Type.SecondaryStorageVm) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Don't know how to fence off " + type);
            }
            return null;
        }
        List<VolumeVO> vols = _volsDao.findByInstance(vm.getId());
        for (VolumeVO vol : vols) {
            if (!vol.isRecreatable()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to f	ence off volumes that are not recreatable: " + vol);
                }
                return null;
            }
            if (vol.getPoolType().isShared()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to fence off volumes that are shared: " + vol);
                }
                return null;
            }
        }
        return true;
    }
}
