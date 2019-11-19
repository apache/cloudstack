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
                diskStatusMap.clear();
                File[] iscsiVolumes = new File(ISCSI_PATH_PREFIX).listFiles();

                if (iscsiVolumes == null || iscsiVolumes.length == 0) {
                    s_logger.debug("No iscsi sessions found for cleanup");
                    return;
                }

                for( File v : iscsiVolumes) {
                    if (isIscsiDisk(v.getAbsolutePath())) {
                        s_logger.debug("found iscsi disk by cleanup thread, marking inactive:" + v.getAbsolutePath());
                        diskStatusMap.put(v.getAbsolutePath(), false);
                    }
                }

                // check if they belong to any VM
                int[] domains = conn.listDomains();
                s_logger.debug(String.format(" ********* FOUND %d DOMAINS ************", domains.length));
                for (int domId : domains) {
                    Domain dm = conn.domainLookupByID(domId);
                    final String domXml = dm.getXMLDesc(0);
                    final LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
                    parser.parseDomainXML(domXml);
                    List<LibvirtVMDef.DiskDef> disks = parser.getDisks();

                    //check the volume map. If an entry exists change the status to True
                    for (final LibvirtVMDef.DiskDef disk : disks) {
                        if (diskStatusMap.containsKey(disk.getDiskPath())) {
                            diskStatusMap.put(disk.getDiskPath(), true);
                            s_logger.debug("active disk found by cleanup thread" + disk.getDiskPath());
                        }
                    }
                }

                // the ones where the state is false, they are stale. They may be
                // removed we go through each volume which is false, check iscsiadm,
                // if the volume still exisits, logout of that volume and remove it from the map

                // XXX: It is possible that someone had manually added an iSCSI volume.
                // we would not be able to detect that
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
                        diskStatusMap.remove(diskPath);
                    }
                }

            } catch (LibvirtException e) {
                s_logger.warn("[ignored] Error tryong to cleanup ", e);
            }
        }

        private boolean isIscsiDisk(String path) {
            return path.startsWith(ISCSI_PATH_PREFIX) && path.contains(KEYWORD_ISCSI) && path.contains(KEYWORD_IQN);
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
