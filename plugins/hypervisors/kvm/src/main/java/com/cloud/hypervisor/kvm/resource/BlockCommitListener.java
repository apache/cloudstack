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
import java.util.concurrent.TimeUnit;

public class BlockCommitListener implements BlockJobListener {
    private String result;
    private String vmName;

    private Logger logger;
    private String logid;
    private Semaphore semaphore;

    protected BlockCommitListener(String vmName, String logid) {
        this.vmName = vmName;
        this.logid = logid;
        this.logger = LogManager.getLogger(getClass());
        this.semaphore = new Semaphore(0);
        this.result = String.format("Failed to block commit disk of VM [%s]. Libvirt did not launch an event for it.", vmName);
    }

    protected String getResult(int timeout) {
        this.waitBlockCommit(timeout);
        return result;
    }

    protected void waitBlockCommit(int timeout) {
        try {
            logger.debug("Trying to acquire result semaphore. If the correct event was not launched, will wait for [{}] seconds before giving up.", timeout);
            this.semaphore.tryAcquire(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            logger.error("Thread that was tracking the progress for the block commit job of vm {} was interrupted.", vmName, ex);
        }
    }

    @Override
    public void onEvent(Domain domain, String diskPath, BlockJobType type, BlockJobStatus status) {
        if (!BlockJobType.COMMIT.equals(type) && !BlockJobType.ACTIVE_COMMIT.equals(type)) {
            return;
        }

        ThreadContext.put("logcontextid", logid);
        logger.debug("Received status [{}] on disk [{}] while listening for block commit of VM [{}].", status, diskPath, vmName);
        switch (status) {
            case COMPLETED:
                result = null;
                semaphore.release();
                return;
            case READY:
                try {
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
