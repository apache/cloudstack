/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CopyFileInVmAnswer;
import com.cloud.agent.api.CopyFileInVmCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.ssh.SshHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

import java.io.File;

@ResourceWrapper(handles = CopyFileInVmCommand.class)
public class LibvirtCopyFileInVmCommandWrapper   extends CommandWrapper<CopyFileInVmCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtCopyFileInVmCommandWrapper.class);

    @Override public Answer execute(CopyFileInVmCommand cmd, LibvirtComputingResource libvirtComputingResource) {
        final File keyFile = new File("/root/.ssh/id_rsa.cloud");
        try {
            File file = new File(cmd.getSrc());
            if(file.exists()) {
                if(file.isDirectory()) {
                    for (File f : FileUtils.listFiles(new File(cmd.getSrc()), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
                        SshHelper.scpTo(cmd.getVmIp(), 3922, "root", keyFile, null, cmd.getDest(), f.getCanonicalPath(), null);
                    }
                } else {
                    SshHelper.scpTo(cmd.getVmIp(), 3922, "root", keyFile, null, cmd.getDest(), file.getCanonicalPath(), null);
                }
            }
        } catch (Exception e) {
            s_logger.error("Fail to copy file " + cmd.getSrc() + " in VM " + cmd.getVmIp(), e);
            return new CopyFileInVmAnswer(cmd, e);
        }
        return new CopyFileInVmAnswer(cmd);
    }
}
