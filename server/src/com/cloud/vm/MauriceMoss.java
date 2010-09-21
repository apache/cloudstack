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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.network.NetworkManager;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.user.AccountVO;
import com.cloud.utils.Journal;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value=VmManager.class)
public class MauriceMoss implements VmManager {
    private static final Logger s_logger = Logger.getLogger(MauriceMoss.class);
    
    String _name;
    @Inject private StorageManager _storageMgr;
    @Inject private NetworkManager _networkMgr;
    @Inject private AgentManager _agentMgr;
    @Inject private VMInstanceDao _vmDao;
    @Inject private ServiceOfferingDao _offeringDao;
    
    @Inject(adapter=DeploymentPlanner.class)
    private Adapters<DeploymentPlanner> _planners;
    
    private int _retry;

    @Override @DB
    public <T extends VMInstanceVO> VirtualMachineProfile allocate(T vm,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
            List<Pair<DiskOfferingVO, Long>> dataDiskOfferings,
            List<Pair<NetworkConfigurationVO, NicProfile>> networks, 
            DeploymentPlan plan,
            AccountVO owner) throws InsufficientCapacityException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocating entries for VM: " + vm);
        }
        //VMInstanceVO vm = _vmDao.findById(vm.getId());
        VirtualMachineProfile vmProfile = new VirtualMachineProfile(vm, serviceOffering);
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        vm.setDataCenterId(plan.getDataCenterId());
        _vmDao.update(vm.getId(), vm);
        
        List<NicProfile> nics = _networkMgr.allocate(vmProfile, networks);
        vmProfile.setNics(nics);

        if (dataDiskOfferings == null) {
            dataDiskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>(0);
        }
        
        List<DiskProfile> disks = new ArrayList<DiskProfile>(dataDiskOfferings.size() + 1);
        if (template.getFormat() == ImageFormat.ISO) {
            disks.add(_storageMgr.allocateRawVolume(VolumeType.ROOT, "ROOT-" + vm.getId(), rootDiskOffering.first(), rootDiskOffering.second(), vm, owner));
        } else {
            disks.add(_storageMgr.allocateTemplatedVolume(VolumeType.ROOT, "ROOT-" + vm.getId(), rootDiskOffering.first(), template, vm, owner));
        }
        for (Pair<DiskOfferingVO, Long> offering : dataDiskOfferings) {
            disks.add(_storageMgr.allocateRawVolume(VolumeType.DATADISK, "DATA-" + vm.getId(), offering.first(), offering.second(), vm, owner));
        }
        vmProfile.setDisks(disks);

        _vmDao.updateIf(vm, Event.OperationSucceeded, null);
        txn.commit();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocation completed for VM: " + vm);
        }
        
        return vmProfile;
    }
    
    @Override
    public <T extends VMInstanceVO> VirtualMachineProfile allocate(T vm,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            Long rootSize,
            Pair<DiskOfferingVO, Long> dataDiskOffering,
            List<Pair<NetworkConfigurationVO, NicProfile>> networks,
            DeploymentPlan plan,
            AccountVO owner) throws InsufficientCapacityException {
        List<Pair<DiskOfferingVO, Long>> diskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>(1);
        if (dataDiskOffering != null) {
            diskOfferings.add(dataDiskOffering);
        }
        return allocate(vm, template, serviceOffering, new Pair<DiskOfferingVO, Long>(serviceOffering, rootSize), diskOfferings, networks, plan, owner);
    }
    
    @Override
    public <T extends VMInstanceVO> VirtualMachineProfile allocate(T vm,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            List<NetworkConfigurationVO> networkProfiles,
            DeploymentPlan plan, 
            AccountVO owner) throws InsufficientCapacityException {
        List<Pair<NetworkConfigurationVO, NicProfile>> networks = new ArrayList<Pair<NetworkConfigurationVO, NicProfile>>(networkProfiles.size());
        for (NetworkConfigurationVO profile : networkProfiles) {
            networks.add(new Pair<NetworkConfigurationVO, NicProfile>(profile, null));
        }
        return allocate(vm, template, serviceOffering, new Pair<DiskOfferingVO, Long>(serviceOffering, null), null, networks, plan, owner);
    }
    
    protected VirtualMachineProfile create(VirtualMachineProfile vmProfile, DeploymentPlan plan) throws InsufficientCapacityException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating actual resources for VM " + vmProfile);
        }
        
        Journal journal = new Journal.LogJournal("Creating " + vmProfile, s_logger);

        Set<DeployDestination> avoids = new HashSet<DeployDestination>();
        int retry = _retry;
        while (_retry-- > 0) {
            DeployDestination context = null;
            for (DeploymentPlanner dispatcher : _planners) {
                context = dispatcher.plan(vmProfile, plan, avoids);
                if (context != null) {
                    journal.record("Deployment found ", vmProfile, context);
                    break;
                }
            }
            
            if (context == null) {
                throw new CloudRuntimeException("Unable to create a deployment for " + vmProfile);
            }
            
            VMInstanceVO vm = _vmDao.findById(vmProfile.getId());

            vm.setDataCenterId(context.getDataCenter().getId());
            vm.setPodId(context.getPod().getId());
            vm.setHostId(context.getHost().getId());
            _vmDao.update(vm.getId(), vm);
            
            _networkMgr.create(vm);
            _storageMgr.create(vm);
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creation complete for VM " + vmProfile);
        }
        
        return vmProfile;
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
        
        _planners = locator.getAdapters(DeploymentPlanner.class);
        
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
    public <T extends VMInstanceVO> T start(T vm, DeploymentPlan plan) throws InsufficientCapacityException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating actual resources for VM " + vm);
        }
        
        Journal journal = new Journal.LogJournal("Creating " + vm, s_logger);
        
        ServiceOffering offering = _offeringDao.findById(vm.getServiceOfferingId());
        
        VirtualMachineProfile vmProfile = new VirtualMachineProfile(vm, offering);

        Set<DeployDestination> avoids = new HashSet<DeployDestination>();
        int retry = _retry;
        while (retry-- > 0) {
            DeployDestination dest = null;
            for (DeploymentPlanner dispatcher : _planners) {
                dest = dispatcher.plan(vmProfile, plan, avoids);
                if (dest != null) {
                    avoids.add(dest);
                    journal.record("Deployment found ", vmProfile, dest);
                    break;
                }
            }
            
            if (dest == null) {
                throw new CloudRuntimeException("Unable to create a deployment for " + vmProfile);
            }
            
            vm.setDataCenterId(dest.getDataCenter().getId());
            vm.setPodId(dest.getPod().getId());
            _vmDao.updateIf(vm, Event.StartRequested, dest.getHost().getId());
            
            VirtualMachineTO vmTO = new VirtualMachineTO();
            _networkMgr.prepare(vmProfile, dest);
//            _storageMgr.prepare(vm, dest);
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creation complete for VM " + vmProfile);
        }
        
        return null;
    }

    @Override
    public <T extends VMInstanceVO> T stop(T vm) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }
    
}
