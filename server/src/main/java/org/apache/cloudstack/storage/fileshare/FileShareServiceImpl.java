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

package org.apache.cloudstack.storage.fileshare;

import static org.apache.cloudstack.storage.fileshare.FileShare.FileShareCleanupDelay;
import static org.apache.cloudstack.storage.fileshare.FileShare.FileShareCleanupInterval;
import static org.apache.cloudstack.storage.fileshare.FileShare.FileShareFeatureEnabled;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.storage.fileshare.ChangeFileShareDiskOfferingCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ChangeFileShareServiceOfferingCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.CreateFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ExpungeFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ListFileShareProvidersCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ListFileSharesCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.DestroyFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.RecoverFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.RestartFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.StartFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.StopFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.UpdateFileShareCmd;
import org.apache.cloudstack.api.response.FileShareResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.fileshare.dao.FileShareDao;
import org.apache.cloudstack.storage.fileshare.FileShare.Event;
import org.apache.cloudstack.storage.fileshare.FileShare.State;
import org.apache.cloudstack.storage.fileshare.query.dao.FileShareJoinDao;
import org.apache.cloudstack.storage.fileshare.query.vo.FileShareJoinVO;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;

public class FileShareServiceImpl extends ManagerBase implements FileShareService, Configurable {

    @Inject
    private AccountManager accountMgr;

    @Inject
    private DataCenterDao dataCenterDao;

    @Inject
    private ConfigurationManager configMgr;

    @Inject
    private VolumeApiService volumeApiService;

    @Inject
    private FileShareDao fileShareDao;

    @Inject
    private FileShareJoinDao fileShareJoinDao;

    @Inject
    private DiskOfferingDao diskOfferingDao;

    @Inject
    ConfigurationDao configDao;

    @Inject
    VolumeDao volumeDao;

    protected List<FileShareProvider> fileShareProviders;

    private Map<String, FileShareProvider> fileShareProviderMap = new HashMap<>();

    protected final StateMachine2<State, Event, FileShare> fileShareStateMachine;

    ScheduledExecutorService _executor = null;

    public FileShareServiceImpl() {
        this.fileShareStateMachine = State.getStateMachine();
    }

    @Override
    public boolean start() {
        fileShareProviderMap.clear();
        for (final FileShareProvider provider : fileShareProviders) {
            fileShareProviderMap.put(provider.getName(), provider);
            provider.configure();
        }
        _executor.scheduleWithFixedDelay(new FileShareGarbageCollector(), FileShareCleanupInterval.value(), FileShareCleanupInterval.value(), TimeUnit.SECONDS);
        return true;
    }

    public boolean stop() {
        _executor.shutdown();
        return true;
    }

    @Override
    public List<FileShareProvider> getFileShareProviders() {
        return fileShareProviders;
    }

    @Override
    public boolean stateTransitTo(FileShare fileShare, Event event) {
        try {
            return fileShareStateMachine.transitTo(fileShare, event, null, fileShareDao);
        } catch (NoTransitionException e) {
            String message = String.format("State transit error for File share %s [%s] due to exception: %s.",
                    fileShare.getName(), fileShare.getId(), e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    @Override
    public void setFileShareProviders(List<FileShareProvider> fileShareProviders) {
        this.fileShareProviders = fileShareProviders;
    }

    @Override
    public FileShareProvider getFileShareProvider(String fileShareProviderName) {
        if (fileShareProviderMap.containsKey(fileShareProviderName)) {
            return fileShareProviderMap.get(fileShareProviderName);
        }
        throw new CloudRuntimeException("Invalid file share provider name!");
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        Map<String, String> configs = configDao.getConfiguration("management-server", params);
        String workers = configs.get("expunge.workers");
        int wrks = NumbersUtil.parseInt(workers, 10);
        _executor = Executors.newScheduledThreadPool(wrks, new NamedThreadFactory("FileShare-Scavenger"));
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        if (FileShareFeatureEnabled.value() == true) {
            cmdList.add(ListFileShareProvidersCmd.class);
            cmdList.add(CreateFileShareCmd.class);
            cmdList.add(ListFileSharesCmd.class);
            cmdList.add(UpdateFileShareCmd.class);
            cmdList.add(DestroyFileShareCmd.class);
            cmdList.add(RestartFileShareCmd.class);
            cmdList.add(StartFileShareCmd.class);
            cmdList.add(StopFileShareCmd.class);
            cmdList.add(ChangeFileShareDiskOfferingCmd.class);
            cmdList.add(ChangeFileShareServiceOfferingCmd.class);
            cmdList.add(RecoverFileShareCmd.class);
            cmdList.add(ExpungeFileShareCmd.class);
        }
        return cmdList;
    }

    private DataCenter validateAndGetZone(Long zoneId) {
        DataCenter zone = dataCenterDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by ID: " + zoneId);
        }
        if (zone.getAllocationState() == Grouping.AllocationState.Disabled) {
            throw new PermissionDeniedException(String.format("Cannot perform this operation, zone ID: %s is currently disabled", zone.getUuid()));
        }
        return zone;
    }

    private void validateDiskOffering(Long diskOfferingId, Long size, Long minIops, Long maxIops, DataCenter zone) {
        Account caller = CallContext.current().getCallingAccount();
        DiskOfferingVO diskOffering = diskOfferingDao.findById(diskOfferingId);
        configMgr.checkDiskOfferingAccess(caller, diskOffering, zone);

        if (!diskOffering.isCustomized() && size != null) {
            throw new InvalidParameterValueException("Size provided with a non-custom disk offering");
        }
        if ((diskOffering.isCustomizedIops() == null || diskOffering.isCustomizedIops() == false) && (minIops != null || maxIops != null)) {
            throw new InvalidParameterValueException("Iops provided with a non-custom-iops disk offering");
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_CREATE, eventDescription = "creating fileshare", create = true)
    public FileShare createFileShare(CreateFileShareCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        Account caller = CallContext.current().getCallingAccount();

        long ownerId = cmd.getEntityOwnerId();
        Account owner = accountMgr.getActiveAccountById(ownerId);
        accountMgr.checkAccess(caller, null, true, owner);
        DataCenter zone = validateAndGetZone(cmd.getZoneId());

        Long diskOfferingId = cmd.getDiskOfferingId();
        Long size = cmd.getSize();
        Long minIops = cmd.getMinIops();
        Long maxIops = cmd.getMaxIops();
        validateDiskOffering(diskOfferingId, size, minIops, maxIops, zone);

        FileShareProvider provider = getFileShareProvider(cmd.getFileShareProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        lifeCycle.checkPrerequisites(zone, cmd.getServiceOfferingId());

        FileShare.FileSystemType fsType;
        try {
            fsType = FileShare.FileSystemType.valueOf(cmd.getFsFormat().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidParameterValueException("Invalid File system format specified. Supported formats are EXT4 and XFS");
        }

        FileShareVO fileShare = new FileShareVO(cmd.getName(), cmd.getDescription(),owner.getDomainId(),
                ownerId, cmd.getZoneId(), cmd.getFileShareProviderName(), FileShare.Protocol.NFS,
                fsType, cmd.getServiceOfferingId());
        fileShareDao.persist(fileShare);

        fileShare = fileShareDao.findById(fileShare.getId());
        Pair<Long, Long> result = null;
        try {
            result = lifeCycle.commitFileShare(fileShare, cmd.getNetworkId(), diskOfferingId, size, minIops, maxIops);
        } catch (Exception ex) {
            stateTransitTo(fileShare, Event.OperationFailed);
            throw ex;
        }
        fileShare.setVolumeId(result.first());
        fileShare.setVmId(result.second());
        fileShareDao.update(fileShare.getId(), fileShare);
        stateTransitTo(fileShare, Event.OperationSucceeded);
        return fileShare;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_START, eventDescription = "Starting fileshare", async = true)
    public FileShare startFileShare(Long fileShareId) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);

        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, false, fileShare);
        Set<State> validStates = new HashSet<>(List.of(State.Stopped, State.Detached));
        if (!validStates.contains(fileShare.getState())) {
            throw new InvalidParameterValueException("File share can be started only if it is in the " + validStates.toString() + " states");
        }

        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();

        if (fileShare.getState().equals(State.Detached)) {
            Pair<Boolean, Long> result = lifeCycle.reDeployFileShare(fileShare);
            if (result.first() == true) {
                fileShare.setVmId(result.second());
                fileShareDao.update(fileShare.getId(), fileShare);
                stateTransitTo(fileShare, Event.OperationSucceeded);
            } else {
                stateTransitTo(fileShare, Event.OperationFailed);
                return null;
            }
        }

        stateTransitTo(fileShare, Event.StartRequested);
        try {
            lifeCycle.startFileShare(fileShare);
        } catch (Exception ex) {
            stateTransitTo(fileShare, Event.OperationFailed);
            throw ex;
        }
        stateTransitTo(fileShare, Event.OperationSucceeded);
        fileShare = fileShareDao.findById(fileShareId);
        return fileShare;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_STOP, eventDescription = "Stopping fileshare", async = true)
    public FileShare stopFileShare(Long fileShareId, Boolean forced) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, false, fileShare);
        Set<State> validStates = new HashSet<>(List.of(State.Ready));
        if (!validStates.contains(fileShare.getState())) {
            throw new InvalidParameterValueException("File share can be stopped only if it is in the " + State.Ready + " state");
        }

        stateTransitTo(fileShare, Event.StopRequested);
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        try {
            lifeCycle.stopFileShare(fileShare, forced);
        } catch (Exception e) {
            stateTransitTo(fileShare, Event.OperationFailed);
            throw e;
        }
        stateTransitTo(fileShare, Event.OperationSucceeded);
        return fileShare;
    }

    private FileShareVO reDeployFileShare(FileShareVO fileShare, Boolean startVm) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        DataCenter zone = validateAndGetZone(fileShare.getDataCenterId());
        lifeCycle.checkPrerequisites(zone, fileShare.getServiceOfferingId());

        if (!fileShare.getState().equals(State.Stopped)) {
            stopFileShare(fileShare.getId(), false);
        }

        Pair<Boolean, Long> result = lifeCycle.reDeployFileShare(fileShare);
        if (result.first() == true) {
            fileShare.setVmId(result.second());
            fileShareDao.update(fileShare.getId(), fileShare);
            if (startVm) {
                startFileShare(fileShare.getId());
            }
            return fileShare;
        } else {
            stateTransitTo(fileShare, Event.Detach);
            logger.error("Redeploy failed for fileshare " +  fileShare.toString() + ". File share is left in detached state.");
            return null;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_RESTART, eventDescription = "Restarting fileshare", async = true)
    public FileShare restartFileShare(Long fileShareId, boolean cleanup) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, false, fileShare);

        Set<State> validStates = new HashSet<>(List.of(State.Ready, State.Stopped));
        if (!validStates.contains(fileShare.getState())) {
            throw new InvalidParameterValueException("Stop file share can be done only if the file share is in " + validStates.toString() + " states");
        }

        if (cleanup == false) {
            if (!fileShare.getState().equals(State.Stopped)) {
                stopFileShare(fileShare.getId(), false);
            }
            return startFileShare(fileShare.getId());
        } else {
            return reDeployFileShare(fileShare, true);
        }
    }

    private Pair<List<Long>, Integer> searchForFileSharesIdsAndCount(ListFileSharesCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<>();

        Long id = cmd.getId();
        String name = cmd.getName();
        Long networkId = cmd.getNetworkId();
        Long diskOfferingId = cmd.getDiskOfferingId();
        Long serviceOfferingId = cmd.getServiceOfferingId();
        String keyword = cmd.getKeyword();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();
        Long zoneId = cmd.getZoneId();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long projectId = cmd.getProjectId();

        Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(domainId, cmd.isRecursive(), null);
        accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        Project.ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(FileShareVO.class, "created", false, startIndex, pageSize);

        SearchBuilder<FileShareVO> fileShareSearchBuilder = fileShareDao.createSearchBuilder();
        fileShareSearchBuilder.select(null, SearchCriteria.Func.DISTINCT, fileShareSearchBuilder.entity().getId()); // select distinct
        accountMgr.buildACLSearchBuilder(fileShareSearchBuilder, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        fileShareSearchBuilder.and("id", fileShareSearchBuilder.entity().getId(), SearchCriteria.Op.EQ);
        fileShareSearchBuilder.and("name", fileShareSearchBuilder.entity().getName(), SearchCriteria.Op.EQ);
        fileShareSearchBuilder.and("dataCenterId", fileShareSearchBuilder.entity().getDataCenterId(), SearchCriteria.Op.EQ);

        if (keyword != null) {
            fileShareSearchBuilder.and().op("keywordName", fileShareSearchBuilder.entity().getName(), SearchCriteria.Op.LIKE);
        }

        fileShareSearchBuilder.and("serviceOfferingId", fileShareSearchBuilder.entity().getServiceOfferingId(), SearchCriteria.Op.EQ);

        if (diskOfferingId != null) {
            SearchBuilder<VolumeVO> volSearch = volumeDao.createSearchBuilder();
            volSearch.and("diskOfferingId", volSearch.entity().getDiskOfferingId(), SearchCriteria.Op.EQ);
            fileShareSearchBuilder.join("volSearch", volSearch, volSearch.entity().getId(), fileShareSearchBuilder.entity().getVolumeId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<FileShareVO> sc = fileShareSearchBuilder.create();
        accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            sc.setParameters("keywordName", "%" + keyword + "%");
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (diskOfferingId != null) {
            sc.setJoinParameters("volSearch", "diskOfferingId", diskOfferingId);
        }

        if (serviceOfferingId != null) {
            sc.setParameters("serviceOfferingId", serviceOfferingId);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }

        Pair<List<FileShareVO>, Integer> result = fileShareDao.searchAndCount(sc, searchFilter);
        List<Long> idsArray = result.first().stream().map(FileShareVO::getId).collect(Collectors.toList());
        return new Pair<List<Long>, Integer>(idsArray, result.second());
    }

    private Pair<List<FileShareJoinVO>, Integer> searchForFileSharesInternal(ListFileSharesCmd cmd) {
        Pair<List<Long>, Integer> fileShareIds = searchForFileSharesIdsAndCount(cmd);
        if (fileShareIds.second() == 0) {
            return new Pair<List<FileShareJoinVO>, Integer>(null, 0);
        }

        List<FileShareJoinVO> fileShares = fileShareJoinDao.searchByIds(fileShareIds.first().toArray(new Long[0]));
        return new Pair<List<FileShareJoinVO>, Integer>(fileShares, fileShareIds.second());
    }

    @Override
    public ListResponse<FileShareResponse> searchForFileShares(ResponseObject.ResponseView respView, ListFileSharesCmd cmd) {
        Pair<List<FileShareJoinVO>, Integer> result = searchForFileSharesInternal(cmd);
        ListResponse<FileShareResponse> response = new ListResponse<>();

        if (cmd.getRetrieveOnlyResourceCount()) {
            response.setResponses(new ArrayList<>(), result.second());
            return response;
        }

        Account caller = CallContext.current().getCallingAccount();
        if (accountMgr.isRootAdmin(caller.getId())) {
            respView = ResponseObject.ResponseView.Full;
        }

        List<FileShareResponse> fileShareResponses = null;
        if (result.second() > 0) {
            fileShareResponses = fileShareJoinDao.createFileShareResponses(respView, result.first().toArray(new FileShareJoinVO[result.first().size()]));
        }

        response.setResponses(fileShareResponses, result.second());
        return response;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_UPDATE, eventDescription = "Updating fileshare")
    public FileShare updateFileShare(UpdateFileShareCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getName();
        String description = cmd.getDescription();

        FileShareVO fileShare = fileShareDao.findById(id);
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, false, fileShare);

        if (name != null) {
            fileShare.setName(name);
        }
        if (description != null) {
            fileShare.setDescription(description);
        }

        fileShareDao.update(fileShare.getId(), fileShare);
        return fileShare;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_UPDATE, eventDescription = "Change file share disk offering")
    public FileShare changeFileShareDiskOffering(ChangeFileShareDiskOfferingCmd cmd) throws ResourceAllocationException {
        FileShareVO fileShare = fileShareDao.findById(cmd.getId());
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, false, fileShare);
        Set<State> validStates = new HashSet<>(List.of(State.Ready, State.Stopped));

        if (!validStates.contains(fileShare.getState())) {
            throw new InvalidParameterValueException("Disk offering of the file share can be changed only if the file share is in " + validStates.toString() + " states");
        }

        Long diskOfferingId = cmd.getDiskOfferingId();
        Long newSize = cmd.getSize();
        Long newMinIops = cmd.getMinIops();
        Long newMaxIops = cmd.getMaxIops();
        DataCenter zone = validateAndGetZone(fileShare.getDataCenterId());
        validateDiskOffering(diskOfferingId, newSize, newMinIops, newMaxIops, zone);
        volumeApiService.changeDiskOfferingForVolumeInternal(fileShare.getVolumeId(), diskOfferingId, newSize, newMinIops, newMaxIops, true, false);
        return fileShare;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_UPDATE, eventDescription = "Change file share service offering")
    public FileShare changeFileShareServiceOffering(ChangeFileShareServiceOfferingCmd cmd) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        FileShareVO fileShare = fileShareDao.findById(cmd.getId());
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, false, fileShare);
        Set<State> validStates = new HashSet<>(List.of(State.Ready, State.Stopped));
        if (!validStates.contains(fileShare.getState())) {
            throw new InvalidParameterValueException("Service offering of the file share can be changed only if the file share is in " + validStates.toString() + " states");
        }

        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        DataCenter zone = validateAndGetZone(fileShare.getDataCenterId());
        lifeCycle.checkPrerequisites(zone, cmd.getServiceOfferingId());
        fileShare.setServiceOfferingId(cmd.getServiceOfferingId());

        Boolean startVm = false;
        if (!fileShare.getState().equals(State.Stopped)) {
            stopFileShare(fileShare.getId(), false);
            startVm = true;
        }

        fileShare = fileShareDao.findById(cmd.getId());
        return reDeployFileShare(fileShare, startVm);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_DESTROY, eventDescription = "Destroy file share")
    public Boolean destroyFileShare(DestroyFileShareCmd cmd) {
        Long fileShareId = cmd.getId();
        Boolean expunge = cmd.isExpunge();
        FileShareVO fileShare = fileShareDao.findById(fileShareId);

        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, false, fileShare);
        Set<State> validStates = new HashSet<>(List.of(State.Stopped, State.Error));
        if (!validStates.contains(fileShare.getState())) {
            throw new InvalidParameterValueException("File share can be destroyed only if it is in the " + validStates.toString() + " states");
        }

        stateTransitTo(fileShare, Event.DestroyRequested);
        if (expunge || fileShare.getState().equals(State.Error)) {
            deleteFileShare(fileShareId);
        }
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_RECOVER, eventDescription = "Recover file share")
    public FileShare recoverFileShare(Long fileShareId) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, false, fileShare);
        if (!State.Destroyed.equals(fileShare.getState())) {
            throw new InvalidParameterValueException("File Share should be in the Destroyed state to be recovered");
        }
        stateTransitTo(fileShare, Event.RecoveryRequested);
        fileShare = fileShareDao.findById(fileShareId);
        return fileShare;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_EXPUNGE, eventDescription = "Expunge file share")
    public void deleteFileShare(Long fileShareId) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);
        Account caller = CallContext.current().getCallingAccount();
        accountMgr.checkAccess(caller, null, false, fileShare);

        Set<State> validStates = new HashSet<>(List.of(State.Destroyed, State.Expunging));
        if (!validStates.contains(fileShare.getState())) {
            throw new InvalidParameterValueException("File share can be expunged only if it is in the " + validStates.toString() + " states");
        }
        stateTransitTo(fileShare, Event.ExpungeOperation);
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        lifeCycle.deleteFileShare(fileShare);
        stateTransitTo(fileShare, Event.OperationSucceeded);
        fileShareDao.remove(fileShare.getId());
    }

    @Override
    public String getConfigComponentName() {
        return FileShareService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{
                FileShareCleanupInterval,
                FileShareCleanupDelay,
                FileShareFeatureEnabled
        };
    }
    protected class FileShareGarbageCollector extends ManagedContextRunnable {

        public FileShareGarbageCollector() {
        }

        @Override
        protected void runInContext() {
            try {
                logger.trace("File Share Garbage Collection Thread is running.");

                cleanupFileShare(true);

            } catch (Exception e) {
                logger.error("Caught the following Exception", e);
            }
        }
    }

    public void cleanupFileShare(boolean recurring) {
        GlobalLock scanLock = GlobalLock.getInternLock("fileshareservice.cleanup");

        try {
            if (scanLock.lock(30)) {
                try {

                    List<FileShareVO> fileShares = fileShareDao.listFileSharesToBeDestroyed(new Date(System.currentTimeMillis() - ((long)FileShareCleanupDelay.value() << 10)));
                    for (FileShareVO fileShare : fileShares) {
                        try {
                            deleteFileShare(fileShare.getId());
                        } catch (Exception e) {
                            stateTransitTo(fileShare, Event.OperationFailed);
                            logger.error(String.format("Unable to expunge file share [%s] due to: [%s].", fileShare.getUuid(), e.getMessage()));
                        }
                    }
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }
    }
}
