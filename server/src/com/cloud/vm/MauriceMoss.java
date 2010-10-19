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
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Start2Command;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.network.NetworkManager;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.user.Account;
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
    @Inject private GuestOSDao _guestOsDao;
    @Inject private VMTemplateDao _templateDao;
    
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
            Account owner) throws InsufficientCapacityException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocating entries for VM: " + vm);
        }
        
        // Determine the VM's OS description
        GuestOSVO guestOS = _guestOsDao.findById(vm.getGuestOSId());
        if (guestOS == null) {
            throw new CloudRuntimeException("Guest OS is not set");
        }
        
        //VMInstanceVO vm = _vmDao.findById(vm.getId());
        VirtualMachineProfile vmProfile = new VirtualMachineProfile(vm, serviceOffering, guestOS.getDisplayName(), template.getHypervisorType());
        
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
            Account owner) throws InsufficientCapacityException {
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
            List<Pair<NetworkConfigurationVO, NicProfile>> networks,
            DeploymentPlan plan, 
            Account owner) throws InsufficientCapacityException {
        return allocate(vm, template, serviceOffering, new Pair<DiskOfferingVO, Long>(serviceOffering, null), null, networks, plan, owner);
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
    public <T extends VMInstanceVO> T start(T vm, DeploymentPlan plan, Account acct, VirtualMachineGuru<T> guru) throws InsufficientCapacityException, ConcurrentOperationException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating actual resources for VM " + vm);
        }
        
        Journal journal = new Journal.LogJournal("Creating " + vm, s_logger);
        
        ServiceOffering offering = _offeringDao.findById(vm.getServiceOfferingId());
        VMTemplateVO template = _templateDao.findById(vm.getTemplateId());
        
        BootloaderType bt = BootloaderType.PyGrub;
        if (template.getFormat() == Storage.ImageFormat.ISO || template.isRequiresHvm()) {
            bt = BootloaderType.HVM;
        }
        
        // Determine the VM's OS description
        GuestOSVO guestOS = _guestOsDao.findById(vm.getGuestOSId());
        if (guestOS == null) {
            throw new CloudRuntimeException("Guest OS is not set");
        }
        VirtualMachineProfile vmProfile = new VirtualMachineProfile(vm, offering, guestOS.getDisplayName(), template.getHypervisorType());
        if (!_vmDao.updateIf(vm, Event.StartRequested, null)) {
            throw new ConcurrentOperationException("Unable to start vm "  + vm + " due to concurrent operations");
        }

        ExcludeList avoids = new ExcludeList();
        int retry = _retry;
        while (retry-- != 0) { // It's != so that it can match -1.
            DeployDestination dest = null;
            for (DeploymentPlanner planner : _planners) {
                dest = planner.plan(vmProfile, plan, avoids);
                if (dest != null) {
                    avoids.addHost(dest.getHost().getId());
                    journal.record("Deployment found ", vmProfile, dest);
                    break;
                }
            }
            
            if (dest == null) {
                throw new InsufficientServerCapacityException("Unable to create a deployment for " + vmProfile);
            }
            
            vm.setDataCenterId(dest.getDataCenter().getId());
            vm.setPodId(dest.getPod().getId());
            _vmDao.updateIf(vm, Event.OperationRetry, dest.getHost().getId());

            VirtualMachineTO vmTO = new VirtualMachineTO(vmProfile, bt);
            VolumeTO[] volumes = null;
            try {
                volumes = _storageMgr.prepare(vmProfile, dest);
            } catch (ConcurrentOperationException e) {
                throw e;
            } catch (StorageUnavailableException e) {
                s_logger.warn("Unable to contact storage.", e);
                continue;
            }
            NicTO[] nics = _networkMgr.prepare(vmProfile, dest, acct);
            
            vmTO.setNics(nics);
            vmTO.setDisks(volumes);
            
            Commands cmds = new Commands(OnError.Revert);
            cmds.addCommand(new Start2Command(vmTO));
            if (guru != null) {
                guru.finalizeDeployment(cmds, vmProfile, dest);
            }
            try {
                Answer[] answers = _agentMgr.send(dest.getHost().getId(), cmds);
                if (answers[0].getResult()) {
                    if (!_vmDao.updateIf(vm, Event.OperationSucceeded, dest.getHost().getId())) {
                        throw new CloudRuntimeException("Unable to transition to a new state.");
                    }
                    return vm;
                }
                s_logger.info("Unable to start VM on " + dest.getHost() + " due to " + answers[0].getDetails());
            } catch (AgentUnavailableException e) {
                s_logger.debug("Unable to send the start command to host " + dest.getHost());
                continue;
            } catch (OperationTimedoutException e) {
                s_logger.debug("Unable to send the start command to host " + dest.getHost());
                continue;
            }
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
