package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

import com.cloud.utils.exception.CloudRuntimeException;
import com.dynatrace.hash4j.hashing.HashStream128;
import com.dynatrace.hash4j.hashing.HashValue128;
import com.dynatrace.hash4j.hashing.Hashing;
import org.apache.cloudstack.backup.TakeBackupHashCommand;
import org.apache.cloudstack.storage.to.BackupDeltaTO;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@ResourceWrapper(handles = TakeBackupHashCommand.class)
public class LibvirtTakeBackupHashCommandWrapper extends CommandWrapper<TakeBackupHashCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(TakeBackupHashCommand command, LibvirtComputingResource resource) {
        String backupUuid = command.getBackupUuid();
        logger.info("Taking hash of backup [{}].", backupUuid);

        KVMStoragePoolManager storagePoolManager = resource.getStoragePoolMgr();
        KVMStoragePool imagePool = null;
        try {
            imagePool = storagePoolManager.getStoragePoolByURI(command.getBackupDeltaTOList().get(0).getDataStore().getUrl());
            HashStream128 hashStream128 = Hashing.xxh3_128().hashStream();
            for (BackupDeltaTO backupDelta : command.getBackupDeltaTOList()) {
                try (InputStream is = new BufferedInputStream(new FileInputStream(imagePool.getLocalPathFor(backupDelta.getPath())))) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        hashStream128.putBytes(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    throw new CloudRuntimeException(e);
                }
            }
            HashValue128 hash = hashStream128.get();
            String hashString = hash.toString();
            logger.info("The xxHash128 of backup [{}] is [{}].", backupUuid, hashString);
            return new Answer(command, true, hashString);
        } finally {
            if (imagePool != null) {
                storagePoolManager.deleteStoragePool(imagePool.getType(), imagePool.getUuid());
            }
        }
    }
}
