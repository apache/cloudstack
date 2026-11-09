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
package com.cloud.hypervisor.kvm.resource.disconnecthook;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.libvirt.Domain;
import org.libvirt.LibvirtException;

public class MigrationCancelHook extends DisconnectHook {
    private static final Logger LOGGER = LogManager.getLogger(MigrationCancelHook.class);

    Domain _migratingDomain;
    String _vmName;

    public MigrationCancelHook(Domain migratingDomain) throws LibvirtException {
        super(migratingDomain.getName());
        _migratingDomain = migratingDomain;
        _vmName = migratingDomain.getName();
    }

    @Override
    public void run() {
        LOGGER.info("Interrupted migration of " + _vmName);
        try {
            final boolean aborted = _migratingDomain.abortJob() == 0;
            setStoppedTheWork(aborted);
            if (aborted) {
                LOGGER.info("Aborted migration job for " + _vmName);
            } else {
                LOGGER.info("Could not abort the migration job for " + _vmName + ", it has most likely finished");
            }
        } catch (LibvirtException ex) {
            // "Domain not found" once the migration completed and the source domain is gone
            setStoppedTheWork(false);
            LOGGER.info("Could not abort the migration job for " + _vmName + ", it has most likely finished: " + ex.getMessage());
        }
    }
}
