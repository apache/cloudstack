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
package org.apache.cloudstack.secondarystorage;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Command;
import com.cloud.configuration.Config;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceManager;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.secstorage.CommandExecLogVO;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.SecondaryStorageVm;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.SystemVmLoadScanner.AfterScanAction;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.SecondaryStorageVmDao;

@Local(value = {SecondaryStorageVmManager.class})
public class PremiumSecondaryStorageManagerImpl extends SecondaryStorageManagerImpl {
    private static final Logger s_logger = Logger.getLogger(PremiumSecondaryStorageManagerImpl.class);

    private int _capacityPerSSVM = SecondaryStorageVmManager.DEFAULT_SS_VM_CAPACITY;
    private int _standbyCapacity = SecondaryStorageVmManager.DEFAULT_STANDBY_CAPACITY;
    private int _maxExecutionTimeMs = 1800000;

    @Inject
    SecondaryStorageVmDao _secStorageVmDao;
    @Inject
    CommandExecLogDao _cmdExecLogDao;
    @Inject
    HostDao _hostDao;
    @Inject
    ResourceManager _resourceMgr;
    protected SearchBuilder<CommandExecLogVO> activeCommandSearch;
    protected SearchBuilder<HostVO> hostSearch;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        _capacityPerSSVM = NumbersUtil.parseInt(_configDao.getValue(Config.SecStorageSessionMax.key()), DEFAULT_SS_VM_CAPACITY);
        _standbyCapacity = NumbersUtil.parseInt(_configDao.getValue(Config.SecStorageCapacityStandby.key()), DEFAULT_STANDBY_CAPACITY);

        int nMaxExecutionMinutes = NumbersUtil.parseInt(_configDao.getValue(Config.SecStorageCmdExecutionTimeMax.key()), 30);
        _maxExecutionTimeMs = nMaxExecutionMinutes * 60 * 1000;

        hostSearch = _hostDao.createSearchBuilder();
        hostSearch.and("dc", hostSearch.entity().getDataCenterId(), Op.EQ);
        hostSearch.and("status", hostSearch.entity().getStatus(), Op.EQ);

        activeCommandSearch = _cmdExecLogDao.createSearchBuilder();
        activeCommandSearch.and("created", activeCommandSearch.entity().getCreated(), Op.GTEQ);
        activeCommandSearch.join("hostSearch", hostSearch, activeCommandSearch.entity().getInstanceId(), hostSearch.entity().getId(), JoinType.INNER);

        hostSearch.done();
        activeCommandSearch.done();
        return true;
    }

    @Override
    public Pair<AfterScanAction, Object> scanPool(Long pool) {
        long dataCenterId = pool.longValue();
        if (!isSecondaryStorageVmRequired(dataCenterId)) {
            return new Pair<AfterScanAction, Object>(AfterScanAction.nop, null);
        }

        Date cutTime = new Date(DateUtil.currentGMTTime().getTime() - _maxExecutionTimeMs);

        _cmdExecLogDao.expungeExpiredRecords(cutTime);

        boolean suspendAutoLoading = !reserveStandbyCapacity();
        if (!suspendAutoLoading) {
            // this is a hacking, has nothing to do with console proxy, it is just a flag that primary storage is being under maintenance mode
            String restart = _configDao.getValue("consoleproxy.restart");
            if (restart != null && restart.equalsIgnoreCase("false")) {
                s_logger.debug("Capacity scan disabled purposefully, consoleproxy.restart = false. This happens when the primarystorage is in maintenance mode");
                suspendAutoLoading = true;
            }
        }

        List<SecondaryStorageVmVO> alreadyRunning =
                _secStorageVmDao.getSecStorageVmListInStates(SecondaryStorageVm.Role.templateProcessor, dataCenterId, State.Running, State.Migrating, State.Starting);
        if (alreadyRunning.size() == 0) {
            s_logger.info("No running secondary storage vms found in datacenter id=" + dataCenterId + ", starting one");

            List<SecondaryStorageVmVO> stopped =
                    _secStorageVmDao.getSecStorageVmListInStates(SecondaryStorageVm.Role.templateProcessor, dataCenterId, State.Stopped, State.Stopping);
            if (stopped.size() == 0 || !suspendAutoLoading) {
                List<SecondaryStorageVmVO> stopping = _secStorageVmDao.getSecStorageVmListInStates(SecondaryStorageVm.Role.templateProcessor, State.Stopping);
                if (stopping.size() > 0) {
                    s_logger.info("Found SSVMs that are currently at stopping state, wait until they are settled");
                    return new Pair<AfterScanAction, Object>(AfterScanAction.nop, null);
                }

                expandPool(pool, SecondaryStorageVm.Role.templateProcessor);
            }
        }

        if (!suspendAutoLoading) {
            // this is to avoid surprises that people may accidently see two SSVMs being launched, capacity expanding only happens when we have at least the primary SSVM is up
            if (alreadyRunning.size() == 0) {
                s_logger.info("Primary secondary storage is not even started, wait until next turn");
                return new Pair<AfterScanAction, Object>(AfterScanAction.nop, null);
            }

            alreadyRunning = _secStorageVmDao.getSecStorageVmListInStates(null, dataCenterId, State.Running, State.Migrating, State.Starting);

            List<CommandExecLogVO> activeCmds = listActiveCommands(dataCenterId, cutTime);
            if (alreadyRunning.size() * _capacityPerSSVM - activeCmds.size() < _standbyCapacity) {
                s_logger.info("secondary storage command execution standby capactiy low (running VMs: " + alreadyRunning.size() + ", active cmds: " + activeCmds.size() +
                        "), starting a new one");
                return new Pair<AfterScanAction, Object>(AfterScanAction.expand, SecondaryStorageVm.Role.commandExecutor);
            }
        }

        return new Pair<AfterScanAction, Object>(AfterScanAction.nop, null);
    }

    @Override
    public Pair<HostVO, SecondaryStorageVmVO> assignSecStorageVm(long zoneId, Command cmd) {

        // TODO, need performance optimization
        List<Long> vms = _secStorageVmDao.listRunningSecStorageOrderByLoad(null, zoneId);
        for (Long vmId : vms) {
            SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(vmId);
            HostVO host;
            host = _resourceMgr.findHostByName(secStorageVm.getHostName());
            if (host != null && host.getStatus() == Status.Up)
                return new Pair<HostVO, SecondaryStorageVmVO>(host, secStorageVm);
        }

        return null;
    }

    private List<CommandExecLogVO> listActiveCommands(long dcId, Date cutTime) {
        SearchCriteria<CommandExecLogVO> sc = activeCommandSearch.create();

        sc.setParameters("created", cutTime);
        sc.setJoinParameters("hostSearch", "dc", dcId);
        sc.setJoinParameters("hostSearch", "status", Status.Up);

        return _cmdExecLogDao.search(sc, null);
    }

    private boolean reserveStandbyCapacity() {
        String value = _configDao.getValue(Config.SystemVMAutoReserveCapacity.key());
        if (value != null && value.equalsIgnoreCase("true")) {
            return true;
        }

        return false;
    }
}
