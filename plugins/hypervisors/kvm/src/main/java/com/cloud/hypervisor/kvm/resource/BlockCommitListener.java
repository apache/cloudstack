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

package com.cloud.hypervisor.kvm.resource;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.libvirt.event.BlockJobListener;
import org.libvirt.event.BlockJobStatus;
import org.libvirt.event.BlockJobType;

import java.util.concurrent.Semaphore;

public class BlockCommitListener implements BlockJobListener {
    private Semaphore semaphore;
    private String result;
    private String vmName;

    private Logger logger;
    private String logid;

    protected BlockCommitListener(Semaphore semaphore, String vmName, String logid) {
        this.semaphore = semaphore;
        this.vmName = vmName;
        this.logid = logid;
        logger = LogManager.getLogger(getClass());
    }

    protected String getResult() {
        return result;
    }

    @Override
    public void onEvent(Domain domain, String diskPath, BlockJobType type, BlockJobStatus status) {
        if (!BlockJobType.COMMIT.equals(type) && !BlockJobType.ACTIVE_COMMIT.equals(type)) {
            return;
        }

        switch (status) {
            case COMPLETED:
                result = null;
                semaphore.release();
                return;
            case READY:
                try {
                    ThreadContext.put("logcontextid", logid);
                    logger.debug("Pivoting disk [{}] of VM [{}].", diskPath, vmName);
                    domain.blockJobAbort(diskPath, Domain.BlockJobAbortFlags.PIVOT);
                } catch (LibvirtException ex) {
                    result = String.format("Failed to pivot disk due to [%s].", ex.getMessage());
                    semaphore.release();
                }
                return;
            default:
                result = String.format("Failed to block commit disk with status [%s].", status);
                semaphore.release();
        }
    }
}
