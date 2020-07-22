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
package com.cloud.hypervisor.kvm.storage;

import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IscsiStorageCleanupMonitor implements Runnable{
    private static final Logger s_logger = Logger.getLogger(IscsiStorageCleanupMonitor.class);
    private static final int CLEANUP_INTERVAL_SEC = 60; // check every X seconds
    private static final String ISCSI_PATH_PREFIX = "/dev/disk/by-path";
    private static final String KEYWORD_ISCSI = "iscsi";
    private static final String KEYWORD_IQN = "iqn";
    private static final String REGEX_PART = "\\S+part\\d+$";

    private IscsiAdmStorageAdaptor iscsiStorageAdaptor;

    private Map<String, Boolean> diskStatusMap;

    public IscsiStorageCleanupMonitor() {
        diskStatusMap = new HashMap<>();
        s_logger.debug("Initialize cleanup thread");
        iscsiStorageAdaptor = new IscsiAdmStorageAdaptor();
    }


    private class Monitor extends ManagedContextRunnable {

        @Override
        protected void runInContext() {
            Connect conn = null;
            try {
                conn = LibvirtConnection.getConnection();

                //populate all the iscsi disks currently attached to this host
                File[] iscsiVolumes = new File(ISCSI_PATH_PREFIX).listFiles();
                if (iscsiVolumes == null || iscsiVolumes.length == 0) {
                    s_logger.debug("No iscsi sessions found for cleanup");
                    return;
                }

                // set all status values to false
                initializeDiskStatusMap(iscsiVolumes);

                // check if iscsi sessions belong to any VM
                updateDiskStatusMapWithInactiveIscsiSessions(conn);

                // disconnect stale iscsi sessions
                disconnectInactiveSessions();

            } catch (LibvirtException e) {
                s_logger.warn("[ignored] Error trying to cleanup ", e);
            }
        }

        private boolean isIscsiDisk(String path) {
            return path.startsWith(ISCSI_PATH_PREFIX) && path.contains(KEYWORD_ISCSI) && path.contains(KEYWORD_IQN);
        }

        /**
         * for each volume if the volume is path is of type iscsi, add to diskstatusmap and set status to false.
         * @param iscsiVolumes
         */
        private void initializeDiskStatusMap(File[] iscsiVolumes){
            diskStatusMap.clear();
            for( File v : iscsiVolumes) {
                if (isIscsiDisk(v.getAbsolutePath())) {
                    s_logger.debug("found iscsi disk by cleanup thread, marking inactive: " + v.getAbsolutePath());
                    diskStatusMap.put(v.getAbsolutePath(), false);
                }
            }
        }

        /** Loop over list of VMs or domains, get disks, if disk is in diskStatusMap, set status value to true.
         *
         * @param conn
         */
        private void updateDiskStatusMapWithInactiveIscsiSessions(Connect conn){
            try {
                int[] domains = conn.listDomains();
                s_logger.debug(String.format("found %d domains", domains.length));
                for (int domId : domains) {
                    Domain dm = conn.domainLookupByID(domId);
                    final String domXml = dm.getXMLDesc(0);
                    final LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
                    parser.parseDomainXML(domXml);
                    List<LibvirtVMDef.DiskDef> disks = parser.getDisks();

                    //check the volume map. If an entry exists change the status to True
                    for (final LibvirtVMDef.DiskDef disk : disks) {
                        if (diskStatusMap.containsKey(disk.getDiskPath())&&!disk.getDiskPath().matches(REGEX_PART)) {
                            diskStatusMap.put(disk.getDiskPath(), true);
                            s_logger.debug("active disk found by cleanup thread" + disk.getDiskPath());
                        }
                    }
                }
            } catch (LibvirtException e) {
                s_logger.warn("[ignored] Error trying to cleanup ", e);
            }

        }

        /**
         * When the state is false, the iscsi sessions are stale. They may be
         * removed. We go through each volume which is false, check iscsiadm,
         * if the volume still exisits, logout of that volume and remove it from the map

         * XXX: It is possible that someone had manually added an iSCSI volume.
         * we would not be able to detect that
         */
        private void disconnectInactiveSessions(){

            for (String diskPath : diskStatusMap.keySet()) {
                if (!diskStatusMap.get(diskPath)) {
                    if (Files.exists(Paths.get(diskPath))) {
                        try {
                            s_logger.info("Cleaning up disk " + diskPath);
                            iscsiStorageAdaptor.disconnectPhysicalDiskByPath(diskPath);
                        } catch (Exception e) {
                            s_logger.warn("[ignored] Error cleaning up " + diskPath, e);
                        }
                    }
                }
            }

        }
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(CLEANUP_INTERVAL_SEC * 1000);
            } catch (InterruptedException e) {
                s_logger.debug("[ignored] interupted between heartbeats.");
            }

            Thread monitorThread = new Thread(new Monitor());
            monitorThread.start();
            try {
                monitorThread.join();
            } catch (InterruptedException e) {
                s_logger.debug("[ignored] interupted joining monitor.");
            }
        }
    }
}
