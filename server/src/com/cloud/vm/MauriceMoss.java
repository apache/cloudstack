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
package com.cloud.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.VolumeVO;
import com.cloud.user.AccountVO;
import com.cloud.utils.Journal;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value=VmManager.class)
public class MauriceMoss implements VmManager {
    private static final Logger s_logger = Logger.getLogger(MauriceMoss.class);
    
    String _name;
    @Inject private StorageManager _storageMgr;
    @Inject private NetworkManager _networkMgr;
    @Inject private AgentManager _agentMgr;
    @Inject private VMInstanceDao _vmDao;
    private int _retry;

    @Override @DB
    public <T extends VMInstanceVO> T allocate(T vm,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
            List<Pair<DiskOfferingVO, Long>> dataDiskOfferings,
            List<Pair<NetworkConfigurationVO, NicVO>> networks, 
            DataCenterVO dc,
            AccountVO owner) throws InsufficientCapacityException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocating entries for VM: " + vm);
        }
        Transaction txn = Transaction.currentTxn();
        txn.start();
        List<NicVO> nics = _networkMgr.allocate(vm, networks);
        
        VolumeVO volume = _storageMgr.allocate(VolumeType.ROOT, rootDiskOffering.first(), "ROOT-" + vm.getId(), rootDiskOffering.second(), template.getFormat() != ImageFormat.ISO ? template : null, vm, owner);
        for (Pair<DiskOfferingVO, Long> offering : dataDiskOfferings) {
            volume = _storageMgr.allocate(VolumeType.DATADISK, offering.first(), "DATA-" + vm.getId(), offering.second(), null, vm, owner);
        }
        
        txn.commit();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocation completed for VM: " + vm);
        }
        return vm;
    }

    @Override
    public <T extends VMInstanceVO> T allocate(T vm,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            Long rootSize,
            Pair<DiskOfferingVO, Long> dataDiskOffering,
            List<Pair<NetworkConfigurationVO, NicVO>> networks,
            DataCenterVO dc,
            AccountVO owner) throws InsufficientCapacityException {
        List<Pair<DiskOfferingVO, Long>> diskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>(1);
        if (dataDiskOffering != null) {
            diskOfferings.add(dataDiskOffering);
        }
        return allocate(vm, template, serviceOffering, new Pair<DiskOfferingVO, Long>(serviceOffering, rootSize), diskOfferings, networks, dc, owner);
    }
    
    @Override
    public <T extends VMInstanceVO> T allocate(T vm,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            List<NetworkConfigurationVO> networkProfiles,
            DataCenterVO dc, AccountVO owner) throws InsufficientCapacityException {
        List<Pair<NetworkConfigurationVO, NicVO>> networks = new ArrayList<Pair<NetworkConfigurationVO, NicVO>>(networkProfiles.size());
        for (NetworkConfigurationVO profile : networkProfiles) {
            networks.add(new Pair<NetworkConfigurationVO, NicVO>(profile, null));
        }
        return allocate(vm, template, serviceOffering, new Pair<DiskOfferingVO, Long>(serviceOffering, null), null, networks, dc, owner);
    }
    
    
    @Override
    public void create(VmCharacteristics vm, List<DiskCharacteristics> disks, List<NetworkCharacteristics> networks) {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> xmlParams) throws ConfigurationException {
        _name = name;
        
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        Map<String, String> params = configDao.getConfiguration(xmlParams);
        
        _retry = NumbersUtil.parseInt(params.get(Config.StartRetry.key()), 2);
        return true;
    }
    
    @Override
    public String getName() {
        return _name;
    }
    
    protected MauriceMoss() {
    }

    @Override
    public <T extends VMInstanceVO> T create(T v) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating actual resources for VM " + v);
        }
        Journal journal = new Journal.LogJournal("Creating " + v, s_logger);
        
        VMInstanceVO vm = _vmDao.findById(v.getId());

        int retry = _retry;
        while (_retry-- > 0) {
//            Pod pod = _agentMgr.findPod(f);
        }
        _networkMgr.create(vm);
        _storageMgr.create(vm);
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creation complete for VM " + vm);
        }
        return null;
    }

    @Override
    public <T extends VMInstanceVO> T start(T vm) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends VMInstanceVO> T stop(T vm) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }
    
}
