/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.ha;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.host.HostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

@Local(value=FenceBuilder.class)
public class RecreatableFencer extends AdapterBase implements FenceBuilder {
    private static final Logger s_logger = Logger.getLogger(RecreatableFencer.class);
    @Inject VolumeDao _volsDao;
    @Inject StoragePoolDao _poolDao;
    
    protected RecreatableFencer() {
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
                    s_logger.debug("Unable to fence off volumes that are not recreatable: " + vol);
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
