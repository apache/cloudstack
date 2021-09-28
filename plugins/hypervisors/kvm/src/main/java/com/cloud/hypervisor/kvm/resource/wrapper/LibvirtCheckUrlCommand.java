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

import org.apache.cloudstack.agent.directdownload.CheckUrlAnswer;
import org.apache.cloudstack.agent.directdownload.CheckUrlCommand;
import org.apache.log4j.Logger;

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.UriUtils;
import com.cloud.utils.storage.QCOW2Utils;

@ResourceWrapper(handles =  CheckUrlCommand.class)
public class LibvirtCheckUrlCommand extends CommandWrapper<CheckUrlCommand, CheckUrlAnswer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtCheckUrlCommand.class);

    @Override
    public CheckUrlAnswer execute(CheckUrlCommand cmd, LibvirtComputingResource serverResource) {
        final String url = cmd.getUrl();
        s_logger.info("Checking URL: " + url);
        boolean checkResult = true;
        Long remoteSize = null;
        try {
            UriUtils.checkUrlExistence(url);

            if ("qcow2".equalsIgnoreCase(cmd.getFormat())) {
                remoteSize = QCOW2Utils.getVirtualSize(url);
            } else {
                remoteSize = UriUtils.getRemoteSize(url);
            }
        }
        catch (IllegalArgumentException e) {
            s_logger.warn(e.getMessage());
            checkResult = false;
        }
        return new CheckUrlAnswer(checkResult, remoteSize);
    }
}
