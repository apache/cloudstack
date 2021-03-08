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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Command;
import com.cloud.configuration.Config;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceManager;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.secstorage.CommandExecLogVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.SecondaryStorageVm;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.SystemVmLoadScanner.AfterScanAction;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.SecondaryStorageVmDao;

public class PremiumSecondaryStorageManagerImpl extends SecondaryStorageManagerImpl {
    private static final Logger s_logger = Logger.getLogger(PremiumSecondaryStorageManagerImpl.class);

    private int _capacityPerSSVM = SecondaryStorageVmManager.DEFAULT_SS_VM_CAPACITY;
    private int migrateCapPerSSVM = DEFAULT_MIGRATE_SS_VM_CAPACITY;
    private int _standbyCapacity = SecondaryStorageVmManager.DEFAULT_STANDBY_CAPACITY;
    private int _maxExecutionTimeMs = 1800000;
    private int maxDataMigrationWaitTime = 900000;
    long currentTime = DateUtil.currentGMTTime().getTime();
    long nextSpawnTime = currentTime + maxDataMigrationWaitTime;
    private List<SecondaryStorageVmVO> migrationSSVMS = new ArrayList<>();

    @Inject
    SecondaryStorageVmDao _secStorageVmDao;
    @Inject
    CommandExecLogDao _cmdExecLogDao;
    @Inject
    HostDao _hostDao;
    @Inject
    ResourceManager _resourceMgr;
    protected SearchBuilder<CommandExecLogVO> activeCommandSearch;
    protected SearchBuilder<CommandExecLogVO> activeCopyCommandSearch;
    protected SearchBuilder<HostVO> hostSearch;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        _capacityPerSSVM = NumbersUtil.parseInt(_configDao.getValue(Config.SecStorageSessionMax.key()), DEFAULT_SS_VM_CAPACITY);
        _standbyCapacity = NumbersUtil.parseInt(_configDao.getValue(Config.SecStorageCapacityStandby.key()), DEFAULT_STANDBY_CAPACITY);

        int nMaxExecutionMinutes = NumbersUtil.parseInt(_configDao.getValue(Config.SecStorageCmdExecutionTimeMax.key()), 30);
        _maxExecutionTimeMs = nMaxExecutionMinutes * 60 * 1000;

        migrateCapPerSSVM = StorageManager.SecStorageMaxMigrateSessions.value();
        int nMaxDataMigrationWaitTime = StorageManager.MaxDataMigrationWaitTime.value();
        maxDataMigrationWaitTime = nMaxDataMigrationWaitTime * 60 * 1000;
        nextSpawnTime = currentTime + maxDataMigrationWaitTime;

        hostSearch = _hostDao.createSearchBuilder();
        hostSearch.and("dc", hostSearch.entity().getDataCenterId(), Op.EQ);
        hostSearch.and("status", hostSearch.entity().getStatus(), Op.EQ);

        activeCommandSearch = _cmdExecLogDao.createSearchBuilder();
        activeCommandSearch.and("created", activeCommandSearch.entity().getCreated(), Op.GTEQ);
        activeCommandSearch.join("hostSearch", hostSearch, activeCommandSearch.entity().getHostId(), hostSearch.entity().getId(), JoinType.INNER);

        activeCopyCommandSearch = _cmdExecLogDao.createSearchBuilder();
        activeCopyCommandSearch.and("created", activeCopyCommandSearch.entity().getCreated(), Op.GTEQ);
        activeCopyCommandSearch.and("command_name", activeCopyCommandSearch.entity().getCommandName(), Op.EQ);
        activeCopyCommandSearch.join("hostSearch", hostSearch, activeCopyCommandSearch.entity().getHostId(), hostSearch.entity().getId(), JoinType.INNER);

        hostSearch.done();
        activeCommandSearch.done();
        activeCopyCommandSearch.done();
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
            List<CommandExecLogVO> activeCmds = findActiveCommands(dataCenterId, cutTime);
            List<CommandExecLogVO> copyCmdsInPipeline = findAllActiveCopyCommands(dataCenterId, cutTime);
            return scaleSSVMOnLoad(alreadyRunning, activeCmds, copyCmdsInPipeline, dataCenterId);

        }
        return new Pair<AfterScanAction, Object>(AfterScanAction.nop, null);
    }

    private Pair<AfterScanAction, Object> scaleSSVMOnLoad(List<SecondaryStorageVmVO> alreadyRunning, List<CommandExecLogVO> activeCmds,
                                                    List<CommandExecLogVO> copyCmdsInPipeline, long dataCenterId) {
        Integer hostsCount = _hostDao.countAllByTypeInZone(dataCenterId, Host.Type.Routing);
        Integer maxSsvms = (hostsCount < MaxNumberOfSsvmsForMigration.value()) ? hostsCount : MaxNumberOfSsvmsForMigration.value();
        int halfLimit = Math.round((float) (alreadyRunning.size() * migrateCapPerSSVM) / 2);
        currentTime = DateUtil.currentGMTTime().getTime();
        if (alreadyRunning.size() * _capacityPerSSVM - activeCmds.size() < _standbyCapacity) {
            s_logger.info("secondary storage command execution standby capactiy low (running VMs: " + alreadyRunning.size() + ", active cmds: " + activeCmds.size() +
                    "), starting a new one");
            return new Pair<AfterScanAction, Object>(AfterScanAction.expand, SecondaryStorageVm.Role.commandExecutor);
        }
        else if (!copyCmdsInPipeline.isEmpty()  && copyCmdsInPipeline.size() >= halfLimit &&
                ((Math.abs(currentTime - copyCmdsInPipeline.get(halfLimit - 1).getCreated().getTime()) > maxDataMigrationWaitTime )) &&
                (currentTime > nextSpawnTime) &&  alreadyRunning.size() <=  maxSsvms) {
            nextSpawnTime = currentTime + maxDataMigrationWaitTime;
            s_logger.debug("scaling SSVM to handle migration tasks");
            return new Pair<AfterScanAction, Object>(AfterScanAction.expand, SecondaryStorageVm.Role.commandExecutor);

        }
        scaleDownSSVMOnLoad(alreadyRunning, activeCmds, copyCmdsInPipeline);
        return new Pair<AfterScanAction, Object>(AfterScanAction.nop, null);
    }

    private void scaleDownSSVMOnLoad(List<SecondaryStorageVmVO> alreadyRunning, List<CommandExecLogVO> activeCmds,
                               List<CommandExecLogVO> copyCmdsInPipeline)  {
        int halfLimit = Math.round((float) (alreadyRunning.size() * migrateCapPerSSVM) / 2);
        if (alreadyRunning.size() > 1 && ( copyCmdsInPipeline.size() < halfLimit && (activeCmds.size() < (((alreadyRunning.size() -1) * _capacityPerSSVM)/2)) )) {
            Collections.reverse(alreadyRunning);
            for(SecondaryStorageVmVO vm : alreadyRunning) {
                long count = activeCmds.stream().filter(cmd -> cmd.getInstanceId() == vm.getId()).count();
                if (count == 0 && copyCmdsInPipeline.size() == 0 && vm.getRole() != SecondaryStorageVm.Role.templateProcessor) {
                    destroySecStorageVm(vm.getId());
                    break;
                }
            }
        }
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

    private List<CommandExecLogVO> findActiveCommands(long dcId, Date cutTime) {
        SearchCriteria<CommandExecLogVO> sc = activeCommandSearch.create();
        sc.setParameters("created", cutTime);
        sc.setJoinParameters("hostSearch", "dc", dcId);
        sc.setJoinParameters("hostSearch", "status", Status.Up);
        List<CommandExecLogVO> result = _cmdExecLogDao.search(sc, null);
        return _cmdExecLogDao.search(sc, null);
    }

    private List<CommandExecLogVO> findAllActiveCopyCommands(long dcId, Date cutTime) {
        SearchCriteria<CommandExecLogVO> sc = activeCopyCommandSearch.create();
        sc.setParameters("created", cutTime);
        sc.setParameters("command_name", "DataMigrationCommand");
        sc.setJoinParameters("hostSearch", "dc", dcId);
        sc.setJoinParameters("hostSearch", "status", Status.Up);
        Filter filter = new Filter(CommandExecLogVO.class, "created", true, null, null);
        return _cmdExecLogDao.search(sc, filter);
    }

    private boolean reserveStandbyCapacity() {
        String value = _configDao.getValue(Config.SystemVMAutoReserveCapacity.key());
        if (value != null && value.equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }
}
