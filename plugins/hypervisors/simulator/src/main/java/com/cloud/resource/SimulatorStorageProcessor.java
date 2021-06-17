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

package com.cloud.resource;

import java.io.File;
import java.util.UUID;

import org.apache.cloudstack.agent.directdownload.DirectDownloadCommand;
import org.apache.log4j.Logger;

import org.apache.cloudstack.storage.command.AttachAnswer;
import org.apache.cloudstack.storage.command.AttachCommand;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.CheckDataStoreStoragePolicyComplainceCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DettachAnswer;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.command.ForgetObjectCmd;
import org.apache.cloudstack.storage.command.IntroduceObjectCmd;
import org.apache.cloudstack.storage.command.ResignatureAnswer;
import org.apache.cloudstack.storage.command.ResignatureCommand;
import org.apache.cloudstack.storage.command.SnapshotAndCopyAnswer;
import org.apache.cloudstack.storage.command.SnapshotAndCopyCommand;
import org.apache.cloudstack.storage.command.SyncVolumePathCommand;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.manager.SimulatorManager;
import com.cloud.storage.Storage;
import com.cloud.storage.resource.StorageProcessor;

public class SimulatorStorageProcessor implements StorageProcessor {

    private static final Logger s_logger = Logger.getLogger(SimulatorStorageProcessor.class);
    protected SimulatorManager hypervisorResource;

    public SimulatorStorageProcessor(SimulatorManager resource) {
        this.hypervisorResource = resource;
    }

    @Override
    public SnapshotAndCopyAnswer snapshotAndCopy(SnapshotAndCopyCommand cmd) {
        s_logger.info("'SnapshotAndCopyAnswer snapshotAndCopy(SnapshotAndCopyCommand)' not currently used for SimulatorStorageProcessor");

        return new SnapshotAndCopyAnswer();
    }

    @Override
    public ResignatureAnswer resignature(ResignatureCommand cmd) {
        s_logger.info("'ResignatureAnswer resignature(ResignatureCommand)' not currently used for SimulatorStorageProcessor");

        return new ResignatureAnswer();
    }

    @Override
    public Answer handleDownloadTemplateToPrimaryStorage(DirectDownloadCommand cmd) {
        return null;
    }

    @Override
    public Answer copyTemplateToPrimaryStorage(CopyCommand cmd) {
        TemplateObjectTO template = new TemplateObjectTO();
        template.setPath(UUID.randomUUID().toString());
        template.setSize(100L);
        template.setFormat(Storage.ImageFormat.RAW);
        return new CopyCmdAnswer(template);
    }

    @Override
    public Answer cloneVolumeFromBaseTemplate(CopyCommand cmd) {
        long size = 100;
        DataTO dataTO = cmd.getDestTO();
        if (dataTO instanceof VolumeObjectTO) {
            VolumeObjectTO destVolume = (VolumeObjectTO)dataTO;
            if (destVolume.getSize() != null) {
                size = destVolume.getSize();
            }
        }
        VolumeObjectTO volume = new VolumeObjectTO();
        volume.setPath(UUID.randomUUID().toString());
        volume.setSize(size);
        volume.setFormat(Storage.ImageFormat.RAW);
        return new CopyCmdAnswer(volume);
    }

    @Override
    public Answer copyVolumeFromImageCacheToPrimary(CopyCommand cmd) {
        VolumeObjectTO volume = new VolumeObjectTO();
        volume.setPath(UUID.randomUUID().toString());
        volume.setSize(100);
        volume.setFormat(Storage.ImageFormat.RAW);
        return new CopyCmdAnswer(volume);
    }

    @Override
    public Answer copyVolumeFromPrimaryToSecondary(CopyCommand cmd) {
        VolumeObjectTO volume = new VolumeObjectTO();
        volume.setPath(UUID.randomUUID().toString());
        volume.setSize(100);
        volume.setFormat(Storage.ImageFormat.RAW);
        return new CopyCmdAnswer(volume);
    }

    @Override
    public Answer createTemplateFromVolume(CopyCommand cmd) {
        DataTO destData = cmd.getDestTO();
        VolumeObjectTO srcData = (VolumeObjectTO)cmd.getSrcTO();
        TemplateObjectTO template = new TemplateObjectTO();
        template.setPath(template.getName());
        template.setFormat(Storage.ImageFormat.RAW);
        template.setSize(srcData.getSize());
        DataStoreTO imageStore = destData.getDataStore();
        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }

        return new CopyCmdAnswer(template);
    }

    @Override
    public Answer createTemplateFromSnapshot(CopyCommand cmd) {
        TemplateObjectTO template = (TemplateObjectTO)cmd.getDestTO();
        DataStoreTO imageStore = template.getDataStore();
        String details;

        try {
            if (!(imageStore instanceof NfsTO)) {
                return new CopyCmdAnswer("Only support create template from snapshot, when the dest store is nfs");
            }

            template.setPath(template.getName());
            template.setFormat(Storage.ImageFormat.RAW);

            return new CopyCmdAnswer(template);
        } catch (Throwable e) {
            details = "CreatePrivateTemplateFromSnapshotCommand exception: " + e.toString();
            return new CopyCmdAnswer(details);
        }
    }

    @Override
    public Answer backupSnapshot(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        SnapshotObjectTO snapshot = (SnapshotObjectTO)srcData;
        DataStoreTO imageStore = destData.getDataStore();
        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }

        int index = snapshot.getPath().lastIndexOf("/");

        String snapshotName = snapshot.getPath().substring(index + 1);
        String snapshotRelPath = "snapshots";
        SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
        newSnapshot.setPath(snapshotRelPath + File.separator + snapshotName);
        return new CopyCmdAnswer(newSnapshot);
    }

    @Override
    public Answer attachIso(AttachCommand cmd) {
        DiskTO disk = cmd.getDisk();
        TemplateObjectTO isoTO = (TemplateObjectTO)disk.getData();
        DataStoreTO store = isoTO.getDataStore();
        if (!(store instanceof NfsTO)) {
            return new AttachAnswer("unsupported protocol");
        }
        return new Answer(cmd);
    }

    @Override
    public Answer attachVolume(AttachCommand cmd) {
        DiskTO disk = cmd.getDisk();
        return new AttachAnswer(disk);
    }

    @Override
    public Answer dettachIso(DettachCommand cmd) {
        DiskTO disk = cmd.getDisk();
        TemplateObjectTO isoTO = (TemplateObjectTO)disk.getData();
        DataStoreTO store = isoTO.getDataStore();
        if (!(store instanceof NfsTO)) {
            return new AttachAnswer("unsupported protocol");
        }
        return new Answer(cmd);
    }

    @Override
    public Answer dettachVolume(DettachCommand cmd) {
        DiskTO disk = cmd.getDisk();
        return new DettachAnswer(disk);
    }

    @Override
    public Answer createVolume(CreateObjectCommand cmd) {
        VolumeObjectTO volume = (VolumeObjectTO)cmd.getData();
        VolumeObjectTO newVol = new VolumeObjectTO();
        newVol.setPath(volume.getName());
        return new CreateObjectAnswer(newVol);
    }

    @Override
    public Answer createSnapshot(CreateObjectCommand cmd) {
        String snapshotName = UUID.randomUUID().toString();
        SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
        newSnapshot.setPath(snapshotName);
        return new CreateObjectAnswer(newSnapshot);
    }

    @Override
    public Answer deleteVolume(DeleteCommand cmd) {
        return new Answer(null);
    }

    @Override
    public Answer createVolumeFromSnapshot(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        SnapshotObjectTO snapshot = (SnapshotObjectTO)srcData;
        String snapshotPath = snapshot.getPath();
        int index = snapshotPath.lastIndexOf("/");
        String snapshotName = snapshotPath.substring(index + 1);
        VolumeObjectTO newVol = new VolumeObjectTO();
        newVol.setPath(snapshotName);
        return new CopyCmdAnswer(newVol);
    }

    @Override
    public Answer deleteSnapshot(DeleteCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer introduceObject(IntroduceObjectCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Answer forgetObject(ForgetObjectCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Answer copyVolumeFromPrimaryToPrimary(CopyCommand cmd) {
        return null;
    }

    @Override
    public Answer checkDataStoreStoragePolicyCompliance(CheckDataStoreStoragePolicyComplainceCommand cmd) {
        return new Answer(cmd, true, null);
    }

    @Override
    public Answer syncVolumePath(SyncVolumePathCommand cmd) {
        return new Answer(cmd, true, null);
    }
}
