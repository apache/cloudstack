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
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;

import com.cloud.agent.api.storage.MergeDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.MergeDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.SnapshotMergeTreeTO;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.cloudstack.utils.security.ParserUtils;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockJobInfo;
import org.libvirt.LibvirtException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Merges the deltas of disk-only VM snapshots when one of them is deleted.<br/><br/>
 * The merge is done so that every volume either has its merge fully applied, or is left with its backing chain exactly as it was, and the answer reports
 * which volumes are in which case. It is also bounded by the command's wait, so that the host never keeps merging after the management server has given
 * up on the answer, which would leave the management server's metadata pointing to files that no longer exist.<br/><br/>
 * For a running VM, all block commits are started together. Active commits (the current delta being merged into the snapshot) are held in the ready
 * state and are only pivoted once every disk is ready; if any disk fails or the deadline is reached, every job is cancelled, which leaves every disk on
 * its original active image. Non-active commits have no ready phase and finish on their own, thus each of them is reported individually.<br/><br/>
 * For a stopped VM, every delta is first committed into its parent without changing any backing chain; the chains are only changed once all the commits
 * succeeded.<br/><br/>
 * In every case, once the copy of a delta into its parent has started, the parent no longer holds the point-in-time state of the snapshot being deleted.
 * Retrying the merge reproduces the same result.
 */
@ResourceWrapper(handles = MergeDiskOnlyVmSnapshotCommand.class)
public class LibvirtMergeDiskOnlyVMSnapshotCommandWrapper extends CommandWrapper<MergeDiskOnlyVmSnapshotCommand, Answer, LibvirtComputingResource> {

    protected static final int MIN_SAFETY_MARGIN_IN_SECONDS = 5;
    protected static final int MAX_SAFETY_MARGIN_IN_SECONDS = 60;
    protected static final long POLL_INTERVAL_IN_MILLIS = 1000;

    @Override
    public Answer execute(MergeDiskOnlyVmSnapshotCommand command, LibvirtComputingResource serverResource) {
        VirtualMachine.State vmState = command.getVmState();

        try {
            if (VirtualMachine.State.Running.equals(vmState)) {
                return mergeDiskOnlySnapshotsForRunningVM(command, serverResource);
            }
            return mergeDiskOnlySnapshotsForStoppedVM(command, serverResource);
        } catch (LibvirtException | CloudRuntimeException ex) {
            logger.error("Failed to merge disk-only snapshots of VM [{}] before any volume was merged.", command.getVmName(), ex);
            return new MergeDiskOnlyVmSnapshotAnswer(command, false, ex.getMessage(), new ArrayList<>());
        }
    }

    /**
     * Returns how long, in seconds, the host may spend merging. When the management server informs the command's wait, a safety margin is subtracted
     * from it so that the jobs can be cancelled and the answer delivered before the management server stops waiting. Otherwise, the given fallback is used.
     */
    protected int getMergeTimeoutInSeconds(MergeDiskOnlyVmSnapshotCommand cmd, int fallbackInSeconds) {
        int wait = cmd.getWait();
        if (wait <= 0) {
            return fallbackInSeconds;
        }

        int timeout = wait - getSafetyMarginInSeconds(wait);
        if (timeout <= 0) {
            timeout = Math.max(1, wait / 2);
        }
        return fallbackInSeconds > 0 ? Math.min(timeout, fallbackInSeconds) : timeout;
    }

    protected int getSafetyMarginInSeconds(int wait) {
        return Math.min(MAX_SAFETY_MARGIN_IN_SECONDS, Math.max(MIN_SAFETY_MARGIN_IN_SECONDS, wait / 10));
    }

    protected Answer mergeDiskOnlySnapshotsForStoppedVM(MergeDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) {
        KVMStoragePoolManager storageManager = resource.getStoragePoolMgr();
        List<SnapshotMergeTreeTO> snapshotMergeTreeTOList = cmd.getSnapshotMergeTreeToList();
        long deadline = System.currentTimeMillis() + getMergeTimeoutInSeconds(cmd, resource.getCmdsTimeout() / 1000) * 1000L;

        logger.debug("Merging disk-only snapshots for stopped VM [{}] using the following Snapshot Merge Trees [{}].", cmd.getVmName(), snapshotMergeTreeTOList);

        List<StoppedVmDiskMerge> diskMerges = new ArrayList<>();
        for (SnapshotMergeTreeTO snapshotMergeTreeTO : snapshotMergeTreeTOList) {
            DataTO parentTo = snapshotMergeTreeTO.getParent();
            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) parentTo.getDataStore();
            KVMStoragePool storagePool = storageManager.getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());
            diskMerges.add(new StoppedVmDiskMerge(getVolumeUuid(snapshotMergeTreeTO), storagePool.getLocalPathFor(parentTo.getPath()),
                    storagePool.getLocalPathFor(snapshotMergeTreeTO.getChild().getPath()),
                    snapshotMergeTreeTO.getGrandChildren().stream().map(grandChild -> storagePool.getLocalPathFor(grandChild.getPath())).collect(Collectors.toList())));
        }

        for (StoppedVmDiskMerge diskMerge : diskMerges) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return abortStoppedVmMerge(cmd, String.format("the merge did not finish within the time allowed by the command's wait of [%s] seconds", cmd.getWait()));
            }

            QemuImgFile parent = new QemuImgFile(diskMerge.parentPath, QemuImg.PhysicalDiskFormat.QCOW2);
            QemuImgFile child = new QemuImgFile(diskMerge.childPath, QemuImg.PhysicalDiskFormat.QCOW2);
            logger.debug("Committing child delta [{}] into parent snapshot [{}] of volume [{}].", diskMerge.childPath, diskMerge.parentPath, diskMerge.volumeUuid);
            try {
                getQemuImg(remaining).commit(child, parent, true);
            } catch (QemuImgException | LibvirtException ex) {
                return abortStoppedVmMerge(cmd, String.format("committing [%s] into [%s] failed due to [%s]", diskMerge.childPath, diskMerge.parentPath, ex.getMessage()));
            }
        }

        List<String> mergedVolumeUuids = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (StoppedVmDiskMerge diskMerge : diskMerges) {
            try {
                if (!diskMerge.grandChildPaths.isEmpty()) {
                    QemuImg qemuImg = getQemuImg(resource.getCmdsTimeout());
                    QemuImgFile parent = new QemuImgFile(diskMerge.parentPath, QemuImg.PhysicalDiskFormat.QCOW2);
                    logger.debug("Rebasing grandChildren [{}] into parent at [{}].", diskMerge.grandChildPaths, diskMerge.parentPath);
                    for (String grandChildPath : diskMerge.grandChildPaths) {
                        qemuImg.rebase(new QemuImgFile(grandChildPath, QemuImg.PhysicalDiskFormat.QCOW2), parent, parent.getFormat().toString(), false);
                    }
                }

                logger.debug("Deleting child at [{}] as it is useless.", diskMerge.childPath);
                Files.deleteIfExists(Path.of(diskMerge.childPath));
                mergedVolumeUuids.add(diskMerge.volumeUuid);
            } catch (QemuImgException | LibvirtException | IOException ex) {
                logger.error("Failed to finish the merge of volume [{}]; its child [{}] was kept, thus its chain is still consistent.", diskMerge.volumeUuid,
                        diskMerge.childPath, ex);
                failures.add(String.format("volume [%s]: %s", diskMerge.volumeUuid, ex.getMessage()));
            }
        }

        if (failures.isEmpty()) {
            return new MergeDiskOnlyVmSnapshotAnswer(cmd, true, null, mergedVolumeUuids);
        }
        return new MergeDiskOnlyVmSnapshotAnswer(cmd, false, String.format("Failed to finish the merge of some volumes of VM [%s]: %s.", cmd.getVmName(),
                StringUtils.join(failures, "; ")), mergedVolumeUuids);
    }

    private Answer abortStoppedVmMerge(MergeDiskOnlyVmSnapshotCommand cmd, String reason) {
        String details = String.format("Aborted the merge of disk-only snapshots of stopped VM [%s] because %s. No backing chain was changed.", cmd.getVmName(), reason);
        logger.error(details);
        return new MergeDiskOnlyVmSnapshotAnswer(cmd, false, details, new ArrayList<>());
    }

    protected Answer mergeDiskOnlySnapshotsForRunningVM(MergeDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) throws LibvirtException {
        String vmName = cmd.getVmName();
        List<SnapshotMergeTreeTO> snapshotMergeTreeTOList = cmd.getSnapshotMergeTreeToList();

        LibvirtUtilitiesHelper libvirtUtilitiesHelper = resource.getLibvirtUtilitiesHelper();
        Connect conn = libvirtUtilitiesHelper.getConnection();
        Domain domain = resource.getDomain(conn, vmName);
        List<LibvirtVMDef.DiskDef> disks = resource.getDisks(conn, vmName);
        KVMStoragePoolManager storageManager = resource.getStoragePoolMgr();
        int timeout = getMergeTimeoutInSeconds(cmd, resource.getQcow2DeltaMergeTimeout());
        long deadline = System.currentTimeMillis() + timeout * 1000L;

        logger.debug("Merging disk-only snapshots for running VM [{}] within [{}] seconds using the following Snapshot Merge Trees [{}].", vmName, timeout,
                snapshotMergeTreeTOList);

        List<RunningVmDiskMerge> diskMerges = new ArrayList<>();
        for (SnapshotMergeTreeTO mergeTreeTO : snapshotMergeTreeTOList) {
            DataTO childTO = mergeTreeTO.getChild();
            SnapshotObjectTO parentSnapshotTO = (SnapshotObjectTO) mergeTreeTO.getParent();
            VolumeObjectTO volumeObjectTO = parentSnapshotTO.getVolume();
            KVMStoragePool storagePool = libvirtUtilitiesHelper.getPrimaryPoolFromDataTo(volumeObjectTO, storageManager);

            String label = resource.getDiskWithPathOfVolumeObjectTO(disks, volumeObjectTO).getDiskLabel();
            diskMerges.add(new RunningVmDiskMerge(volumeObjectTO.getUuid(), label, storagePool.getLocalPathFor(parentSnapshotTO.getPath()),
                    storagePool.getLocalPathFor(childTO.getPath()), DataObjectType.VOLUME.equals(childTO.getObjectType()),
                    mergeTreeTO.getGrandChildren().stream().map(grandChild -> storagePool.getLocalPathFor(grandChild.getPath())).collect(Collectors.toList())));
        }

        String failure;
        try {
            failure = startBlockCommits(domain, vmName, diskMerges);
            if (failure == null) {
                failure = waitForBlockCommits(domain, vmName, diskMerges, deadline, timeout);
            }
            if (failure == null) {
                failure = pivotActiveBlockCommits(domain, vmName, diskMerges);
            }
        } catch (RuntimeException ex) {
            logger.error("Unexpected error while merging the disk-only snapshots of VM [{}].", vmName, ex);
            failure = String.format("an unexpected error occurred [%s]", ex.getMessage());
        }

        cancelUnfinishedBlockCommits(domain, vmName, diskMerges, System.currentTimeMillis() + getSafetyMarginInSeconds(Math.max(cmd.getWait(), 0)) * 1000L);

        return finishRunningVmMerge(cmd, domain, diskMerges, failure, resource.getCmdsTimeout());
    }

    /**
     * Starts the block commit of every disk. Active commits are started without pivoting, so that the pivot of every disk can be done only once all of them
     * are ready. The merged-away files are not deleted by Libvirt, as they are only deleted once the merge of their disk is known to be applied.
     * @return the reason of the failure, or null if every block commit was started.
     */
    protected String startBlockCommits(Domain domain, String vmName, List<RunningVmDiskMerge> diskMerges) {
        for (RunningVmDiskMerge diskMerge : diskMerges) {
            int flags = diskMerge.active ? Domain.BlockCommitFlags.ACTIVE : 0;
            logger.info("Starting block commit of top file [{}] into base file [{}] on disk [{}] of VM [{}] (active: [{}]).", diskMerge.topPath, diskMerge.basePath,
                    diskMerge.label, vmName, diskMerge.active);
            try {
                domain.blockCommit(diskMerge.label, diskMerge.basePath, diskMerge.topPath, 0, flags);
            } catch (LibvirtException ex) {
                return String.format("starting the block commit of disk [%s] failed due to [%s]", diskMerge.label, ex.getMessage());
            }
        }
        return null;
    }

    /**
     * Waits until every active commit is ready to be pivoted and every non-active commit has finished.
     * @return the reason of the failure, or null if every disk got there before the deadline.
     */
    protected String waitForBlockCommits(Domain domain, String vmName, List<RunningVmDiskMerge> diskMerges, long deadline, int timeout) {
        while (true) {
            Map<String, DiskChain> diskChains;
            try {
                diskChains = getDiskChains(domain);
            } catch (LibvirtException | CloudRuntimeException ex) {
                return String.format("reading the disks of the VM failed due to [%s]", ex.getMessage());
            }

            boolean allDone = true;
            for (RunningVmDiskMerge diskMerge : diskMerges) {
                DiskChain diskChain = diskChains.get(diskMerge.label);
                if (diskMerge.active && diskChain != null && diskChain.isActiveCommitReady()) {
                    continue;
                }

                boolean jobRunning;
                try {
                    jobRunning = isBlockJobRunning(domain, diskMerge.label);
                } catch (LibvirtException ex) {
                    return String.format("reading the block job of disk [%s] failed due to [%s]", diskMerge.label, ex.getMessage());
                }

                if (jobRunning) {
                    allDone = false;
                    continue;
                }
                if (diskMerge.active || diskChain == null || diskChain.contains(diskMerge.topPath)) {
                    return String.format("the block commit of disk [%s] ended before being %s", diskMerge.label, diskMerge.active ? "ready to pivot" : "completed");
                }
                diskMerge.observedApplied = true;
            }

            if (allDone) {
                logger.debug("Every block commit of VM [{}] is ready to be finished.", vmName);
                return null;
            }
            if (System.currentTimeMillis() >= deadline) {
                return String.format("the block commits did not finish within [%s] seconds", timeout);
            }

            try {
                Thread.sleep(getPollIntervalInMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return "the thread waiting for the block commits was interrupted";
            }
        }
    }

    /**
     * Pivots every active commit. It is only called once all of them are ready, thus the pivots are quick; it stops at the first failure so that the
     * remaining disks keep their original active image.
     * @return the reason of the failure, or null if every active commit was pivoted.
     */
    protected String pivotActiveBlockCommits(Domain domain, String vmName, List<RunningVmDiskMerge> diskMerges) {
        for (RunningVmDiskMerge diskMerge : diskMerges) {
            if (!diskMerge.active) {
                continue;
            }
            logger.debug("Pivoting disk [{}] of VM [{}] to [{}].", diskMerge.label, vmName, diskMerge.basePath);
            try {
                domain.blockJobAbort(diskMerge.label, Domain.BlockJobAbortFlags.PIVOT);
                diskMerge.observedApplied = true;
            } catch (LibvirtException ex) {
                return String.format("pivoting disk [%s] failed due to [%s]", diskMerge.label, ex.getMessage());
            }
        }
        return null;
    }

    /**
     * Cancels every block job that is still running, without pivoting. Cancelling a block commit leaves the disk's backing chain unchanged. Then, waits
     * until the given deadline for the jobs to be gone.
     */
    protected void cancelUnfinishedBlockCommits(Domain domain, String vmName, List<RunningVmDiskMerge> diskMerges, long deadline) {
        List<RunningVmDiskMerge> unfinished = new ArrayList<>(diskMerges);
        while (!unfinished.isEmpty()) {
            for (RunningVmDiskMerge diskMerge : new ArrayList<>(unfinished)) {
                try {
                    if (!isBlockJobRunning(domain, diskMerge.label)) {
                        unfinished.remove(diskMerge);
                        continue;
                    }
                    logger.warn("Cancelling the block commit of disk [{}] of VM [{}]; its backing chain will be kept as it was.", diskMerge.label, vmName);
                    domain.blockJobAbort(diskMerge.label, 0);
                } catch (LibvirtException ex) {
                    logger.warn("Failed to cancel the block commit of disk [{}] of VM [{}] due to [{}].", diskMerge.label, vmName, ex.getMessage());
                }
            }

            if (unfinished.isEmpty() || System.currentTimeMillis() >= deadline) {
                break;
            }
            try {
                Thread.sleep(getPollIntervalInMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!unfinished.isEmpty()) {
            logger.error("The block jobs of disks [{}] of VM [{}] could not be cancelled; they will be reported as not merged.",
                    unfinished.stream().map(diskMerge -> diskMerge.label).collect(Collectors.toList()), vmName);
        }
    }

    /**
     * Reads the backing chain of every disk from Libvirt to know which merges were applied, then finishes those: off-chain grandchildren are rebased into
     * the base file and the merged-away file is deleted.
     */
    protected Answer finishRunningVmMerge(MergeDiskOnlyVmSnapshotCommand cmd, Domain domain, List<RunningVmDiskMerge> diskMerges, String failure, int cmdsTimeout) {
        String vmName = cmd.getVmName();
        Map<String, DiskChain> diskChains = null;
        try {
            diskChains = getDiskChains(domain);
        } catch (LibvirtException | CloudRuntimeException ex) {
            logger.error("Failed to read the disks of VM [{}] after merging; only the pivots and completions observed while merging will be reported as merged.",
                    vmName, ex);
            failure = StringUtils.defaultIfBlank(failure, String.format("reading the disks of the VM failed due to [%s]", ex.getMessage()));
        }

        List<String> mergedVolumeUuids = new ArrayList<>();
        List<String> notMergedVolumeUuids = new ArrayList<>();
        for (RunningVmDiskMerge diskMerge : diskMerges) {
            DiskChain diskChain = diskChains == null ? null : diskChains.get(diskMerge.label);
            boolean applied = diskChains == null ? diskMerge.observedApplied : isMergeApplied(diskMerge, diskChain);
            if (diskChains != null && applied != diskMerge.observedApplied) {
                logger.warn("Libvirt reports the merge of volume [{}] of VM [{}] as {}applied, which differs from what was observed while merging; using Libvirt's view.",
                        diskMerge.volumeUuid, vmName, applied ? "" : "not ");
            }
            if (!applied) {
                logger.info("The merge of volume [{}] of VM [{}] was not applied; its backing chain is [{}].", diskMerge.volumeUuid, vmName, diskChain);
                notMergedVolumeUuids.add(diskMerge.volumeUuid);
                continue;
            }

            mergedVolumeUuids.add(diskMerge.volumeUuid);
            finishAppliedMerge(vmName, diskMerge, diskChain, cmdsTimeout);
        }

        if (failure == null && notMergedVolumeUuids.isEmpty()) {
            logger.info("Merged the disk-only snapshots of every volume of VM [{}].", vmName);
            return new MergeDiskOnlyVmSnapshotAnswer(cmd, true, null, mergedVolumeUuids);
        }

        String details = String.format("Aborted the merge of disk-only snapshots of VM [%s] because %s. Volumes [%s] were left with their backing chain unchanged;" +
                " volumes [%s] were merged.", vmName, StringUtils.defaultIfBlank(failure, "not every merge was applied"), StringUtils.join(notMergedVolumeUuids, ", "),
                StringUtils.join(mergedVolumeUuids, ", "));
        logger.error(details);
        return new MergeDiskOnlyVmSnapshotAnswer(cmd, false, details, mergedVolumeUuids);
    }

    protected boolean isMergeApplied(RunningVmDiskMerge diskMerge, DiskChain diskChain) {
        if (diskChain == null || diskChain.mirrorJob != null) {
            return false;
        }
        if (diskMerge.active) {
            return samePath(diskChain.activeSource, diskMerge.basePath);
        }
        return !diskChain.contains(diskMerge.topPath) && diskChain.contains(diskMerge.basePath);
    }

    private void finishAppliedMerge(String vmName, RunningVmDiskMerge diskMerge, DiskChain diskChain, int cmdsTimeout) {
        QemuImgFile base = new QemuImgFile(diskMerge.basePath, QemuImg.PhysicalDiskFormat.QCOW2);
        boolean allGrandChildrenRebased = true;
        for (String grandChildPath : diskMerge.grandChildPaths) {
            if (diskChain != null && diskChain.contains(grandChildPath)) {
                logger.debug("Grandchild [{}] is in the backing chain of VM [{}], thus Libvirt has already rebased it.", grandChildPath, vmName);
                continue;
            }
            try {
                getQemuImg(cmdsTimeout).rebase(new QemuImgFile(grandChildPath, QemuImg.PhysicalDiskFormat.QCOW2), base, base.getFormat().toString(), false);
            } catch (QemuImgException | LibvirtException ex) {
                allGrandChildrenRebased = false;
                logger.error("Failed to rebase grandchild [{}] into [{}]; [{}] will be kept as the grandchild still depends on it.", grandChildPath,
                        diskMerge.basePath, diskMerge.topPath, ex);
            }
        }

        if (!allGrandChildrenRebased) {
            return;
        }
        try {
            logger.debug("Deleting [{}] as it was merged into [{}].", diskMerge.topPath, diskMerge.basePath);
            Files.deleteIfExists(Path.of(diskMerge.topPath));
        } catch (IOException ex) {
            logger.warn("Failed to delete [{}], which is no longer used by volume [{}].", diskMerge.topPath, diskMerge.volumeUuid, ex);
        }
    }

    protected boolean isBlockJobRunning(Domain domain, String label) throws LibvirtException {
        DomainBlockJobInfo blockJobInfo = domain.getBlockJobInfo(label, 0);
        return blockJobInfo != null && !(blockJobInfo.type == 0 && blockJobInfo.end == 0 && blockJobInfo.cur == 0);
    }

    /**
     * Reads, from the domain's live XML, the active image, the backing files and the mirror job (if any) of every disk, indexed by the disk's label.
     */
    protected Map<String, DiskChain> getDiskChains(Domain domain) throws LibvirtException {
        String xml = domain.getXMLDesc(0);
        Map<String, DiskChain> diskChains = new HashMap<>();
        try {
            Document document = ParserUtils.getSaferDocumentBuilderFactory().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList diskNodes = document.getElementsByTagName("disk");
            for (int i = 0; i < diskNodes.getLength(); i++) {
                Element disk = (Element) diskNodes.item(i);
                Element target = getChildElement(disk, "target");
                if (target == null) {
                    continue;
                }

                List<String> backingFiles = new ArrayList<>();
                Element backingStore = getChildElement(disk, "backingStore");
                while (backingStore != null) {
                    String backingFile = getSourcePath(getChildElement(backingStore, "source"));
                    if (backingFile == null) {
                        break;
                    }
                    backingFiles.add(backingFile);
                    backingStore = getChildElement(backingStore, "backingStore");
                }

                Element mirror = getChildElement(disk, "mirror");
                diskChains.put(target.getAttribute("dev"), new DiskChain(getSourcePath(getChildElement(disk, "source")), backingFiles,
                        mirror == null ? null : mirror.getAttribute("job"), mirror != null && "yes".equals(mirror.getAttribute("ready"))));
            }
        } catch (Exception ex) {
            throw new CloudRuntimeException(String.format("Failed to parse the XML of VM [%s] due to [%s].", domain.getName(), ex.getMessage()), ex);
        }
        return diskChains;
    }

    private Element getChildElement(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private String getSourcePath(Element source) {
        if (source == null) {
            return null;
        }
        for (String attribute : List.of("file", "dev", "name")) {
            if (StringUtils.isNotBlank(source.getAttribute(attribute))) {
                return source.getAttribute(attribute);
            }
        }
        return null;
    }

    private static boolean samePath(String path, String otherPath) {
        return path != null && otherPath != null && Path.of(path).normalize().equals(Path.of(otherPath).normalize());
    }

    private String getVolumeUuid(SnapshotMergeTreeTO snapshotMergeTreeTO) {
        return ((SnapshotObjectTO) snapshotMergeTreeTO.getParent()).getVolume().getUuid();
    }

    protected QemuImg getQemuImg(long timeoutInMillis) throws LibvirtException, QemuImgException {
        return new QemuImg(timeoutInMillis);
    }

    protected long getPollIntervalInMillis() {
        return POLL_INTERVAL_IN_MILLIS;
    }

    protected static class RunningVmDiskMerge {
        protected final String volumeUuid;
        protected final String label;
        protected final String basePath;
        protected final String topPath;
        protected final boolean active;
        protected final List<String> grandChildPaths;
        protected boolean observedApplied;

        protected RunningVmDiskMerge(String volumeUuid, String label, String basePath, String topPath, boolean active, List<String> grandChildPaths) {
            this.volumeUuid = volumeUuid;
            this.label = label;
            this.basePath = basePath;
            this.topPath = topPath;
            this.active = active;
            this.grandChildPaths = grandChildPaths;
        }
    }

    protected static class StoppedVmDiskMerge {
        protected final String volumeUuid;
        protected final String parentPath;
        protected final String childPath;
        protected final List<String> grandChildPaths;

        protected StoppedVmDiskMerge(String volumeUuid, String parentPath, String childPath, List<String> grandChildPaths) {
            this.volumeUuid = volumeUuid;
            this.parentPath = parentPath;
            this.childPath = childPath;
            this.grandChildPaths = grandChildPaths;
        }
    }

    protected static class DiskChain {
        protected final String activeSource;
        protected final List<String> backingFiles;
        protected final String mirrorJob;
        protected final boolean mirrorReady;

        protected DiskChain(String activeSource, List<String> backingFiles, String mirrorJob, boolean mirrorReady) {
            this.activeSource = activeSource;
            this.backingFiles = backingFiles;
            this.mirrorJob = StringUtils.isBlank(mirrorJob) ? null : mirrorJob;
            this.mirrorReady = mirrorReady;
        }

        protected boolean isActiveCommitReady() {
            return Objects.equals(mirrorJob, "active-commit") && mirrorReady;
        }

        protected boolean contains(String path) {
            return samePath(activeSource, path) || backingFiles.stream().anyMatch(backingFile -> samePath(backingFile, path));
        }

        @Override
        public String toString() {
            List<String> chain = new ArrayList<>();
            chain.add(activeSource);
            chain.addAll(backingFiles);
            return StringUtils.join(chain, " -> ") + (mirrorJob == null ? "" : String.format(" (job [%s], ready [%s])", mirrorJob, mirrorReady));
        }
    }
}
