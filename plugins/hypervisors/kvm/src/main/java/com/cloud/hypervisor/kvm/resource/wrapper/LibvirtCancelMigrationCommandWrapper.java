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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CancelMigrationAnswer;
import com.cloud.agent.api.CancelMigrationCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

@ResourceWrapper(handles = CancelMigrationCommand.class)
public class LibvirtCancelMigrationCommandWrapper extends CommandWrapper<CancelMigrationCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtCancelMigrationCommandWrapper.class);

    @Override
    public Answer execute(final CancelMigrationCommand cmd, final LibvirtComputingResource libvirtComputingResource) {
        Connect conn = null;
        Domain domain = null;
        final String vmName = cmd.getVmName();
        int result= 0;

        try {
            conn = LibvirtConnection.getConnectionByVmName(vmName);
            domain = conn.domainLookupByName(vmName);

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Cancel migration for vm " + vmName);
            }
            result = domain.abortJob();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Canceled migration for vm " + vmName + " with result=" + result);
            }


        } catch (LibvirtException e) {
            s_logger.error("Exception while cancelling the migration job, maybe it was finished just before trying to abort it. You must check the logs!");
            return new CancelMigrationAnswer(cmd, e);
        }

        if (result == 0) {
            return new CancelMigrationAnswer(cmd, true, null);
        } else {
            return new CancelMigrationAnswer(cmd, false, "Failed to abort vm migration");
        }
    }

}
