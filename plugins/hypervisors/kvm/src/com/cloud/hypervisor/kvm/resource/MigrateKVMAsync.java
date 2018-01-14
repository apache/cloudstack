/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.hypervisor.kvm.resource;

import java.util.concurrent.Callable;

import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

public class MigrateKVMAsync implements Callable<Domain> {

    private final LibvirtComputingResource libvirtComputingResource;

    private Domain dm = null;
    private Connect dconn = null;
    private String dxml = "";
    private String vmName = "";
    private String destIp = "";
    private boolean migrateStorage;
    private boolean autoConvergence;

    public MigrateKVMAsync(final LibvirtComputingResource libvirtComputingResource, final Domain dm, final Connect dconn, final String dxml,
                           final boolean migrateStorage, final boolean autoConvergence, final String vmName, final String destIp) {
        this.libvirtComputingResource = libvirtComputingResource;

        this.dm = dm;
        this.dconn = dconn;
        this.dxml = dxml;
        this.migrateStorage = migrateStorage;
        this.autoConvergence = autoConvergence;
        this.vmName = vmName;
        this.destIp = destIp;
    }

    @Override
    public Domain call() throws LibvirtException {
        long flags = 1 << 0;

        // set compression flag for migration, if libvirt version supports it
        if (dconn.getLibVirVersion() >= 1000003) {
            flags |= 1 << 11;
        }

        if (migrateStorage) {
            flags |= 1 << 6;
        }

        if (autoConvergence && dconn.getLibVirVersion() >= 1002003) {
            flags |= 1 << 13;
        }

        return dm.migrate(dconn, flags, dxml, vmName, "tcp:" + destIp, libvirtComputingResource.getMigrateSpeed());
    }
}
