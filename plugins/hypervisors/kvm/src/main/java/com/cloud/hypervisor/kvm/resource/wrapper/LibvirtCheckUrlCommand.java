//
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
//
package com.cloud.hypervisor.kvm.resource.wrapper;

import org.apache.cloudstack.direct.download.DirectDownloadHelper;
import org.apache.cloudstack.agent.directdownload.CheckUrlAnswer;
import org.apache.cloudstack.agent.directdownload.CheckUrlCommand;

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  CheckUrlCommand.class)
public class LibvirtCheckUrlCommand extends CommandWrapper<CheckUrlCommand, CheckUrlAnswer, LibvirtComputingResource> {


    @Override
    public CheckUrlAnswer execute(CheckUrlCommand cmd, LibvirtComputingResource serverResource) {
        final String url = cmd.getUrl();
        final Integer connectTimeout = cmd.getConnectTimeout();
        final Integer connectionRequestTimeout = cmd.getConnectionRequestTimeout();
        final Integer socketTimeout = cmd.getSocketTimeout();

        logger.info(String.format("Checking URL: %s, with connect timeout: %d, connect request timeout: %d, socket timeout: %d", url, connectTimeout, connectionRequestTimeout, socketTimeout));
        Long remoteSize = null;

        boolean checkResult = DirectDownloadHelper.checkUrlExistence(url, connectTimeout, connectionRequestTimeout, socketTimeout, cmd.isFollowRedirects());
        if (checkResult) {
            remoteSize = DirectDownloadHelper.getFileSize(url, cmd.getFormat(), connectTimeout, connectionRequestTimeout, socketTimeout, cmd.isFollowRedirects());
            if (remoteSize == null || remoteSize < 0) {
                logger.error(String.format("Couldn't properly retrieve the remote size of the template on " +
                        "url %s, obtained size = %s", url, remoteSize));
                return new CheckUrlAnswer(false, remoteSize);
            }
        }
        return new CheckUrlAnswer(checkResult, remoteSize);
    }
}
