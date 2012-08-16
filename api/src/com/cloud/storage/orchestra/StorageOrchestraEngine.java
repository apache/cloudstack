package com.cloud.storage.orchestra;

import com.cloud.storage.pool.StoragePoolService;
import com.cloud.storage.snapshot.SnapshotService;
import com.cloud.storage.volume.VolumeService;
import com.cloud.template.TemplateService;

public interface StorageOrchestraEngine extends StoragePoolService, VolumeService, SnapshotService, TemplateService {

}
