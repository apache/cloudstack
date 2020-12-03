// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.usage.parser;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.usage.UsageTypes;

import com.cloud.usage.UsageVO;
import com.cloud.usage.UsageVmDiskVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageVmDiskDao;
import com.cloud.user.AccountVO;
import com.cloud.utils.db.SearchCriteria;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

@Component
public class VmDiskUsageParser {
    public static final Logger s_logger = Logger.getLogger(VmDiskUsageParser.class.getName());

    private static UsageDao s_usageDao;
    private static UsageVmDiskDao s_usageVmDiskDao;

    @Inject
    private UsageDao _usageDao;
    @Inject
    private UsageVmDiskDao _usageVmDiskDao;

    @PostConstruct
    void init() {
        s_usageDao = _usageDao;
        s_usageVmDiskDao = _usageVmDiskDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Parsing all Vm Disk usage events for account: " + account.getId());
        }

        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        // - query usage_disk table for all entries for userId with
        // event_date in the given range
        SearchCriteria<UsageVmDiskVO> sc = s_usageVmDiskDao.createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, account.getId());
        sc.addAnd("eventTimeMillis", SearchCriteria.Op.BETWEEN, startDate.getTime(), endDate.getTime());
        List<UsageVmDiskVO> usageVmDiskVOs = s_usageVmDiskDao.search(sc, null);

        Map<String, VmDiskInfo> vmDiskUsageByZone = new HashMap<String, VmDiskInfo>();

        // Calculate the bytes since last parsing
        for (UsageVmDiskVO usageVmDisk : usageVmDiskVOs) {
            long zoneId = usageVmDisk.getZoneId();
            String key = "" + zoneId;
            if (usageVmDisk.getVmId() != 0) {
                key += "-Vm-" + usageVmDisk.getVmId() + "-Disk-" + usageVmDisk.getVolumeId();
            }
            VmDiskInfo vmDiskInfo = vmDiskUsageByZone.get(key);

            long ioRead = usageVmDisk.getIORead();
            long ioWrite = usageVmDisk.getIOWrite();
            long bytesRead = usageVmDisk.getBytesRead();
            long bytesWrite = usageVmDisk.getBytesWrite();
            if (vmDiskInfo != null) {
                ioRead += vmDiskInfo.getIORead();
                ioWrite += vmDiskInfo.getIOWrite();
                bytesRead += vmDiskInfo.getBytesRead();
                bytesWrite += vmDiskInfo.getBytesWrite();
            }

            vmDiskUsageByZone.put(key, new VmDiskInfo(zoneId, usageVmDisk.getVmId(), usageVmDisk.getVolumeId(), ioRead, ioWrite, bytesRead, bytesWrite));
        }

        List<UsageVO> usageRecords = new ArrayList<UsageVO>();
        for (String key : vmDiskUsageByZone.keySet()) {
            VmDiskInfo vmDiskInfo = vmDiskUsageByZone.get(key);
            long ioRead = vmDiskInfo.getIORead();
            long ioWrite = vmDiskInfo.getIOWrite();
            long bytesRead = vmDiskInfo.getBytesRead();
            long bytesWrite = vmDiskInfo.getBytesWrite();

            if ((ioRead > 0L) || (ioWrite > 0L) || (bytesRead > 0L) || (bytesWrite > 0L)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating vm disk usage record, io read:" + toHumanReadableSize(ioRead) + ", io write: " + toHumanReadableSize(ioWrite) + ", bytes read:" + toHumanReadableSize(bytesRead) + ", bytes write: " +
                            toHumanReadableSize(bytesWrite) + " for account: " + account.getId() + " in availability zone " + vmDiskInfo.getZoneId() + ", start: " + startDate + ", end: " +
                        endDate);
                }

                Long vmId = null;
                Long volumeId = null;

                // Create the usage record for disk I/O read (io requests)
                String usageDesc = "disk I/O read (io requests)";
                if ((vmDiskInfo.getVmId() != 0) && (vmDiskInfo.getVolumeId() != 0)) {
                    vmId = vmDiskInfo.getVmId();
                    volumeId = vmDiskInfo.getVolumeId();
                    usageDesc += " for Vm: " + vmId + " and Volume: " + volumeId;
                }
                UsageVO usageRecord =
                    new UsageVO(vmDiskInfo.getZoneId(), account.getId(), account.getDomainId(), usageDesc, ioRead + " io read", UsageTypes.VM_DISK_IO_READ, new Double(
                        ioRead), vmId, null, null, null, vmDiskInfo.getVolumeId(), startDate, endDate, "VirtualMachine");
                usageRecords.add(usageRecord);

                // Create the usage record for disk I/O write (io requests)
                usageDesc = "disk I/O write (io requests)";
                if ((vmDiskInfo.getVmId() != 0) && (vmDiskInfo.getVolumeId() != 0)) {
                    usageDesc += " for Vm: " + vmId + " and Volume: " + volumeId;
                }
                usageRecord =
                    new UsageVO(vmDiskInfo.getZoneId(), account.getId(), account.getDomainId(), usageDesc, ioWrite + " io write", UsageTypes.VM_DISK_BYTES_WRITE,
                        new Double(ioWrite), vmId, null, null, null, vmDiskInfo.getVolumeId(), startDate, endDate, "VirtualMachine");
                usageRecords.add(usageRecord);

                // Create the usage record for disk I/O read (bytes)
                usageDesc = "disk I/O read (bytes)";
                if ((vmDiskInfo.getVmId() != 0) && (vmDiskInfo.getVolumeId() != 0)) {
                    usageDesc += " for Vm: " + vmId + " and Volume: " + volumeId;
                }
                usageRecord =
                    new UsageVO(vmDiskInfo.getZoneId(), account.getId(), account.getDomainId(), usageDesc, bytesRead + " bytes read", UsageTypes.VM_DISK_BYTES_READ,
                        new Double(bytesRead), vmId, null, null, null, vmDiskInfo.getVolumeId(), startDate, endDate, "VirtualMachine");
                usageRecords.add(usageRecord);

                // Create the usage record for disk I/O write (bytes)
                usageDesc = "disk I/O write (bytes)";
                if ((vmDiskInfo.getVmId() != 0) && (vmDiskInfo.getVolumeId() != 0)) {
                    usageDesc += " for Vm: " + vmId + " and Volume: " + volumeId;
                }
                usageRecord =
                    new UsageVO(vmDiskInfo.getZoneId(), account.getId(), account.getDomainId(), usageDesc, bytesWrite + " bytes write", UsageTypes.VM_DISK_BYTES_WRITE,
                        new Double(bytesWrite), vmId, null, null, null, vmDiskInfo.getVolumeId(), startDate, endDate, "VirtualMachine");
                usageRecords.add(usageRecord);

            } else {
                // Don't charge anything if there were zero bytes processed
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("No vm disk usage record (0 bytes used) generated for account: " + account.getId());
                }
            }
        }

        s_usageDao.saveUsageRecords(usageRecords);

        return true;
    }

    private static class VmDiskInfo {
        private long zoneId;
        private long vmId;
        private Long volumeId;
        private long ioRead;
        private long ioWrite;
        private long bytesRead;
        private long bytesWrite;

        public VmDiskInfo(long zoneId, long vmId, Long volumeId, long ioRead, long ioWrite, long bytesRead, long bytesWrite) {
            this.zoneId = zoneId;
            this.vmId = vmId;
            this.volumeId = volumeId;
            this.ioRead = ioRead;
            this.ioWrite = ioWrite;
            this.bytesRead = bytesRead;
            this.bytesWrite = bytesWrite;
        }

        public long getZoneId() {
            return zoneId;
        }

        public long getVmId() {
            return vmId;
        }

        public Long getVolumeId() {
            return volumeId;
        }

        public long getIORead() {
            return ioRead;
        }

        public long getIOWrite() {
            return ioWrite;
        }

        public long getBytesRead() {
            return bytesRead;
        }

        public long getBytesWrite() {
            return bytesWrite;
        }

    }
}
