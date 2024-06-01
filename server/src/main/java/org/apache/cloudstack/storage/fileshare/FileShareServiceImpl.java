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
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.user.storage.fileshare.CreateFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ListFileShareProvidersCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ListFileSharesCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.RemoveFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.UpdateFileShareCmd;

import com.cloud.utils.component.ManagerBase;

public class FileShareServiceImpl extends ManagerBase implements FileShareService {

    protected List<FileShareProvider> fileShareProviders;

    @Override
    public List<FileShareProvider> getFileShareProviders() {
        return fileShareProviders;
    }

    @Override
    public void setFileShareProviders(List<FileShareProvider> fileShareProviders) {
        this.fileShareProviders = fileShareProviders;
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
    public FileShare createFileShare(CreateFileShareCmd cmd) {
        return null;
    }
}
