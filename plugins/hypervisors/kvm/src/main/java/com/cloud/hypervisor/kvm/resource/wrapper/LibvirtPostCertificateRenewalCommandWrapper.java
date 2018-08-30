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

package com.cloud.hypervisor.kvm.resource.wrapper;

import org.apache.cloudstack.ca.PostCertificateRenewalCommand;
import org.apache.cloudstack.ca.SetupCertificateAnswer;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  PostCertificateRenewalCommand.class)
public final class LibvirtPostCertificateRenewalCommandWrapper extends CommandWrapper<PostCertificateRenewalCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtPostCertificateRenewalCommandWrapper.class);

    @Override
    public Answer execute(final PostCertificateRenewalCommand command, final LibvirtComputingResource serverResource) {
        s_logger.info("Restarting libvirt after certificate provisioning/renewal");
        if (command != null) {
            final int timeout = 30000;
            Script script = new Script(true, "service", timeout, s_logger);
            if ("Ubuntu".equals(serverResource.getHostDistro()) || "Debian".equals(serverResource.getHostDistro())) {
                script.add("libvirt-bin");
            } else {
               script.add("libvirtd");
            }
            script.add("restart");
            script.execute();
            return new SetupCertificateAnswer(true);
       }
        return new SetupCertificateAnswer(false);
    }
}
