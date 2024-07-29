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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.projects.Project;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.EntityManager;
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
    private EntityManager entityMgr;

    @Inject
    private ConfigurationManager configMgr;

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

    private final StateMachine2<FileShare.State, Event, FileShare> fileShareStateMachine;

    static final String DEFAULT_FILE_SHARE_DISK_OFFERING_NAME = "Default Offering for File Share";

   ScheduledExecutorService _executor = null;

    public FileShareServiceImpl() {
        this.fileShareStateMachine = FileShare.State.getStateMachine();
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

    public boolean stateTransitTo(FileShare fileShare, Event event) {
        try {
            return fileShareStateMachine.transitTo(fileShare, event, null, fileShareDao);
        } catch (NoTransitionException e) {
            logger.debug(String.format("Failed during event % for File Share %s [%s] due to exception %",
                    Event.DeployRequested.toString(), fileShare.getName(), fileShare.getId(), e));
            return false;
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

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_CREATE, eventDescription = "allocating fileshare", create = true)
    public FileShare allocFileShare(CreateFileShareCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();

        long ownerId = cmd.getEntityOwnerId();
        Account owner = accountMgr.getActiveAccountById(ownerId);
        accountMgr.checkAccess(caller, null, true, accountMgr.getActiveAccountById(ownerId));

        Long zoneId = cmd.getZoneId();
        DataCenter zone = entityMgr.findById(DataCenter.class, zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id=" + zoneId);
        }

        DiskOfferingVO diskOffering = diskOfferingDao.findById(cmd.getDiskOfferingId());
        configMgr.checkDiskOfferingAccess(null, diskOffering, zone);

        FileShareProvider provider = getFileShareProvider(cmd.getFileShareProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        lifeCycle.checkPrerequisites(zoneId, cmd.getServiceOfferingId());

        FileShareVO fileShare = new FileShareVO(cmd.getName(), cmd.getDescription(),owner.getDomainId(),
                ownerId, cmd.getZoneId(), cmd.getFileShareProviderName(), FileShare.Protocol.NFS,
                cmd.getMountOptions(), FileShare.FileSystemType.XFS, cmd.getServiceOfferingId());
        fileShareDao.persist(fileShare);
        return fileShare;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_DEPLOY, eventDescription = "Deploying fileshare", async = true)
    public FileShare deployFileShare(Long fileShareId, Long networkId, Long diskOfferingId, Long size) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();

        stateTransitTo(fileShare, Event.DeployRequested);
        Pair<Long, Long> result = null;
        try {
            result = lifeCycle.deployFileShare(fileShare, networkId, diskOfferingId, size);
        } catch (CloudRuntimeException ex) {
            stateTransitTo(fileShare, Event.OperationFailed);
            deleteFileShare(fileShare.getId());
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
    public FileShare startFileShare(Long fileShareId) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);

        if (fileShare.getState() == FileShare.State.Ready) {
            throw new InvalidParameterValueException("File Share is already in " + FileShare.State.Ready + " state");
        }
        if (fileShare.getState() == FileShare.State.Destroyed) {
            throw new InvalidParameterValueException("File Share is in " + FileShare.State.Destroyed + " state. Recover it first before starting.");
        }

        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        stateTransitTo(fileShare, Event.StartRequested);
        try {
            lifeCycle.startFileShare(fileShare);
        } catch (CloudRuntimeException ex) {
            stateTransitTo(fileShare, Event.OperationFailed);
            if (fileShare.getState() == FileShare.State.Deployed) {
                deleteFileShare(fileShare.getId());
            }
            throw ex;
        }
        stateTransitTo(fileShare, Event.OperationSucceeded);
        return fileShare;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_STOP, eventDescription = "Stopping fileshare", async = true)
    public FileShare stopFileShare(Long fileShareId) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);
        if (fileShare.getState() != FileShare.State.Ready) {
            throw new InvalidParameterValueException("File Share should be in " + FileShare.State.Ready + " state");
        }

        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        stateTransitTo(fileShare, Event.StopRequested);
        try {
            lifeCycle.stopFileShare(fileShare);
        } catch (CloudRuntimeException e) {
            stateTransitTo(fileShare, Event.OperationFailed);
            throw e;
        }
        stateTransitTo(fileShare, Event.OperationSucceeded);
        return fileShare;
    }

    private void restartWithoutCleanup(FileShareVO fileShare) {
        stopFileShare(fileShare.getId());
        startFileShare(fileShare.getId());
    }

    private void restartWithCleanup(FileShareVO fileShare) {
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        lifeCycle.checkPrerequisites(fileShare.getDataCenterId(), fileShare.getServiceOfferingId());
        stopFileShare(fileShare.getId());
        Long vmId = lifeCycle.reDeployFileShare(fileShare);
        fileShare.setVmId(vmId);
        fileShareDao.update(fileShare.getId(), fileShare);
        startFileShare(fileShare.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_RESTART, eventDescription = "Restarting fileshare", async = true)
    public FileShare restartFileShare(Long fileShareId, boolean cleanup) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);
        if (cleanup == false) {
            restartWithoutCleanup(fileShare);
        } else {
            restartWithCleanup(fileShare);
        }
        return fileShare;
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
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_CREATE, eventDescription = "Updating fileshare")
    public FileShare updateFileShare(UpdateFileShareCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getName();
        String description = cmd.getDescription();

        FileShareVO fileShare = fileShareDao.findById(id);

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
    public FileShare changeFileShareDiskOffering(ChangeFileShareDiskOfferingCmd cmd) {
        FileShareVO fileShare = fileShareDao.findById(cmd.getId());
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        lifeCycle.changeFileShareDiskOffering(fileShare, cmd.getDiskOfferingId(), cmd.getSize(), cmd.getMinIops(), cmd.getMaxIops());
        return fileShare;
    }

    @Override
    public FileShare changeFileShareServiceOffering(ChangeFileShareServiceOfferingCmd cmd) {
        FileShareVO fileShare = fileShareDao.findById(cmd.getId());
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        lifeCycle.checkPrerequisites(fileShare.getDataCenterId(), cmd.getServiceOfferingId());
        fileShare.setServiceOfferingId(cmd.getServiceOfferingId());
        stopFileShare(fileShare.getId());
        Long vmId = lifeCycle.reDeployFileShare(fileShare);
        fileShare.setVmId(vmId);
        fileShareDao.update(fileShare.getId(), fileShare);
        startFileShare(fileShare.getId());
        return fileShare;
    }

    @Override
    public FileShare destroyFileShare(Long fileShareId) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);

        if (!FileShare.State.Stopped.equals(fileShare.getState())) {
            throw new InvalidParameterValueException("File Share should be in the Stopped state before it is destroyed");

        }
        stateTransitTo(fileShare, Event.DestroyRequested);
        return fileShare;
    }

    @Override
    public FileShare recoverFileShare(Long fileShareId) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);
        if (!FileShare.State.Destroyed.equals(fileShare.getState())) {
            throw new InvalidParameterValueException("File Share should be in the Destroyed state to be recovered");

        }
        stateTransitTo(fileShare, Event.RecoveryRequested);
        return fileShare;

    }

    @Override
    public void deleteFileShare(Long fileShareId) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        boolean result = lifeCycle.deleteFileShare(fileShare);
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
            if (scanLock.lock(3)) {
                try {

                    List<FileShareVO> fileShares = fileShareDao.listFileSharesToBeDestroyed(new Date(System.currentTimeMillis() - ((long)FileShareCleanupDelay.value() << 10)));
                    for (FileShareVO fileShare : fileShares) {
                        try {
                            stateTransitTo(fileShare, Event.ExpungeOperation);
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
