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

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.command.user.storage.fileshare.CreateFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ListFileShareProvidersCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ListFileSharesCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.RemoveFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.UpdateFileShareCmd;
import org.apache.cloudstack.storage.fileshare.dao.FileShareDao;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.fileshare.dao.StorageFsVmDao;

public class FileShareServiceImpl extends ManagerBase implements FileShareService {

    @Inject
    private AccountManager accountMgr;

    @Inject
    VirtualMachineManager virtualMachineManager;

    @Inject
    private VolumeApiService volumeApiService;

    @Inject
    private FileShareDao fileShareDao;

    @Inject
    StorageFsVmDao storageFsVmDao;

    @Inject
    VMInstanceDao vmDao;

    @Inject
    private VolumeDao volumeDao;

    protected List<FileShareProvider> fileShareProviders;

    private Map<String, FileShareProvider> fileShareProviderMap = new HashMap<>();

    @Override
    public boolean start() {
        fileShareProviderMap.clear();
        for (final FileShareProvider provider : fileShareProviders) {
            fileShareProviderMap.put(provider.getName(), provider);
            provider.configure();
        }
        return true;
    }

    public boolean stop() {
        return true;
    }

    @Override
    public List<FileShareProvider> getFileShareProviders() {
        return fileShareProviders;
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
        return cmdList;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_CREATE, eventDescription = "allocating fileshare", create = true)
    public FileShare allocFileShare(CreateFileShareCmd cmd) {
        long ownerId = cmd.getEntityOwnerId();
        Account owner = accountMgr.getActiveAccountById(ownerId);
        FileShareVO fileShare = new FileShareVO(cmd.getName(), cmd.getDescription(),owner.getDomainId(), ownerId, 0,
                                                cmd.getZoneId(), cmd.getFileShareProviderName(), cmd.getSize(), null,
                                                cmd.getMountOptions(), FileShare.FileSystemType.EXT4, cmd.getDiskOfferingId());
        fileShareDao.persist(fileShare);
        return fileShare;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_CREATE, eventDescription = "Deploying fileshare", create = true)
    public FileShare deployFileShare(Long fileShareId, List<Long> networkIds) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        Long vmId = lifeCycle.deployFileShare(fileShare, networkIds);
        fileShare.setVmId(vmId);
        fileShareDao.update(fileShare.getId(), fileShare);
        return fileShare;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FILESHARE_CREATE, eventDescription = "Deploying fileshare", create = true)
    public FileShare initializeFileShare(Long fileShareId) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        lifeCycle.initializeFileShare(fileShare);
        return fileShare;
    }

    @Override
    public Pair<List<? extends FileShare>, Integer> searchForFileShares(ListFileSharesCmd cmd) {
        final SearchBuilder<FileShareVO> sb = fileShareDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("account_id", sb.entity().getAccountId(), SearchCriteria.Op.EQ);

        Filter searchFilter = new Filter(FileShareVO.class, "id", Boolean.TRUE, cmd.getStartIndex(), cmd.getPageSizeVal());
        final SearchCriteria<FileShareVO> sc = sb.create();

        Long id = cmd.getId();
        Long accountId = cmd.getAccountId();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (accountId != null) {
            sc.setParameters("account_id", accountId);
        }

        Pair<List<FileShareVO>, Integer> result = fileShareDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends FileShare>, Integer>(result.first(), result.second());
    }

    @Override
    public FileShare deleteFileShare(Long fileShareId, Account owner) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);

        Long vmId = fileShare.getVmId();
        if (vmId != null) {
            VirtualMachine vm = vmDao.findById(vmId);
            try {
                virtualMachineManager.expunge(vm.getUuid());
            } catch (ResourceUnavailableException e) {
                logger.warn(String.format("Unable to destroy storagefsvm [%s] due to [%s].", vm, e.getMessage()), e);
                return null;
            }
            storageFsVmDao.remove(vmId);
        }
        fileShareDao.remove(fileShareId);
        return fileShare;
    }
}
