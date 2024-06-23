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

import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
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
import com.cloud.utils.exception.CloudRuntimeException;

public class FileShareServiceImpl extends ManagerBase implements FileShareService {

    @Inject
    private AccountManager accountMgr;

    @Inject
    private FileShareDao fileShareDao;

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
    public FileShare deployFileShare(Long fileShareId, Long networkId) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);
        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        Long vmId = lifeCycle.deployFileShare(fileShare, networkId);
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
        Pair<List<FileShareVO>, Integer> result = fileShareDao.searchAndCount(cmd.getId(), cmd.getAccountId(), cmd.getNetworkId(), cmd.getStartIndex(), cmd.getPageSizeVal());
        return new Pair<List<? extends FileShare>, Integer>(result.first(), result.second());
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
        if (name != null) {
            fileShare.setDescription(description);
        }
        fileShareDao.update(fileShare.getId(), fileShare);
        return fileShare;
    }

    @Override
    public FileShare deleteFileShare(Long fileShareId, Account owner) {
        FileShareVO fileShare = fileShareDao.findById(fileShareId);

        FileShareProvider provider = getFileShareProvider(fileShare.getFsProviderName());
        FileShareLifeCycle lifeCycle = provider.getFileShareLifeCycle();
        boolean result = lifeCycle.deleteFileShare(fileShare);
        if (!result) {
            return null;
        }
        fileShareDao.remove(fileShareId);
        return fileShare;
    }
}
