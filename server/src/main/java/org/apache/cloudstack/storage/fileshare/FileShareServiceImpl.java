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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.dc.DataCenter;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.storage.fileshare.CreateFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ListFileShareProvidersCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ListFileSharesCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.RemoveFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.RestartFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.UpdateFileShareCmd;
import org.apache.cloudstack.api.response.FileShareResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
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

public class FileShareServiceImpl extends ManagerBase implements FileShareService {

    @Inject
    private AccountManager accountMgr;

    @Inject
    private EntityManager entityMgr;

    @Inject
    private FileShareDao fileShareDao;

    @Inject
    private FileShareJoinDao fileShareJoinDao;

    @Inject
    private DiskOfferingDao diskOfferingDao;

    protected List<FileShareProvider> fileShareProviders;

    private Map<String, FileShareProvider> fileShareProviderMap = new HashMap<>();

    private final StateMachine2<FileShare.State, Event, FileShare> fileShareStateMachine;

    static final String DEFAULT_FILE_SHARE_DISK_OFFERING_NAME = "Default Offering for File Share";

    public FileShareServiceImpl() {
        this.fileShareStateMachine = FileShare.State.getStateMachine();
    }

    private void createDefaultDiskOfferingForFileShare() {
        DiskOfferingVO newDiskOffering = new DiskOfferingVO(DEFAULT_FILE_SHARE_DISK_OFFERING_NAME, DEFAULT_FILE_SHARE_DISK_OFFERING_NAME, Storage.ProvisioningType.THIN, 0, null, true, null, null, null);
        newDiskOffering.setUniqueName(DEFAULT_FILE_SHARE_DISK_OFFERING_NAME);
        newDiskOffering.setFileShare(true);
        diskOfferingDao.persistDefaultDiskOffering(newDiskOffering);
    }

    @Override
    public boolean start() {
        fileShareProviderMap.clear();
        for (final FileShareProvider provider : fileShareProviders) {
            fileShareProviderMap.put(provider.getName(), provider);
            provider.configure();
        }
        createDefaultDiskOfferingForFileShare();
        return true;
    }

    public boolean stop() {
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
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(ListFileShareProvidersCmd.class);
        cmdList.add(CreateFileShareCmd.class);
        cmdList.add(ListFileSharesCmd.class);
        cmdList.add(UpdateFileShareCmd.class);
        cmdList.add(RemoveFileShareCmd.class);
        cmdList.add(RestartFileShareCmd.class);
        return cmdList;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_CREATE, eventDescription = "allocating fileshare", create = true)
    public FileShare allocFileShare(CreateFileShareCmd cmd) {
        long ownerId = cmd.getEntityOwnerId();
        Account owner = accountMgr.getActiveAccountById(ownerId);

        Long zoneId = cmd.getZoneId();
        DataCenter zone = entityMgr.findById(DataCenter.class, zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id=" + zoneId);
        }

        DiskOfferingVO diskOffering = diskOfferingDao.findById(cmd.getDiskOfferingId());
        if (!diskOffering.isFileShare()) {
            throw new InvalidParameterValueException("Disk offering is not file share enabled");
        }

        FileShareProvider provider = getFileShareProvider(cmd.getFileShareProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        lifeCycle.checkPrerequisites(zoneId, cmd.getServiceOfferingId());

        FileShareVO fileShare = new FileShareVO(cmd.getName(), cmd.getDescription(),owner.getDomainId(), ownerId, 0,
                                                cmd.getZoneId(), cmd.getFileShareProviderName(), FileShare.Protocol.NFS,
                                                cmd.getMountOptions(), FileShare.FileSystemType.XFS, cmd.getServiceOfferingId());
        fileShareDao.persist(fileShare);
        return fileShare;
    }

    @Override
    @DB
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

    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_RESTART_WITH_CLEANUP, eventDescription = "Restarting fileshare", async = true)
    private void restartWithCleanup(FileShareVO fileShare) {
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        lifeCycle.checkPrerequisites(fileShare.getDataCenterId(), fileShare.getServiceOfferingId());
        Long vmId = lifeCycle.restartFileShare(fileShare, true);
        fileShare.setVmId(vmId);
        fileShareDao.update(fileShare.getId(), fileShare);
    }

    @Override
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
        Filter searchFilter = new Filter(FileShareVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Pair<List<FileShareVO>, Integer> result = fileShareDao.searchAndCount(cmd.getId(), cmd.getAccountId(), cmd.getNetworkId(), cmd.getStartIndex(), cmd.getPageSizeVal());
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
        List<FileShareResponse> fileShareResponses = null;

        Account caller = CallContext.current().getCallingAccount();
        if (accountMgr.isRootAdmin(caller.getId())) {
            respView = ResponseObject.ResponseView.Full;
        }

        if (result.second() > 0) {
            fileShareResponses = fileShareJoinDao.createFileShareResponses(respView, result.first().toArray(new FileShareJoinVO[result.first().size()]));
        }

        ListResponse<FileShareResponse> response = new ListResponse<>();
        response.setResponses(fileShareResponses, result.second());
        return response;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_CREATE, eventDescription = "Updating fileshare", create = true)
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
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        boolean result = lifeCycle.resizeFileShare(fileShare, cmd.getSize());

        fileShareDao.update(fileShare.getId(), fileShare);
        return fileShare;
    }

    @Override
    public FileShare deleteFileShare(Long fileShareId) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);

        //stateTransitTo(fileShare, Event.DestroyRequested);
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        boolean result = lifeCycle.deleteFileShare(fileShare);
        if (!result) {
            //stateTransitTo(fileShare, Event.OperationFailed);
            return null;
        }
        //stateTransitTo(fileShare, Event.OperationSucceeded);
        fileShareDao.remove(fileShareId);
        return fileShare;
    }
}
