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
package org.apache.cloudstack.storage.image.datastore;

import java.util.List;
import java.util.Set;

import com.cloud.storage.Upload;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;

import com.cloud.storage.ImageStore;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.agent.api.to.DatadiskTO;

public interface ImageStoreEntity extends DataStore, ImageStore {
    TemplateInfo getTemplate(long templateId);

    VolumeInfo getVolume(long volumeId);

    SnapshotInfo getSnapshot(long snapshotId);

    boolean exists(DataObject object);

    Set<TemplateInfo> listTemplates();

    String getMountPoint(); // get the mount point on ssvm.

    String createEntityExtractUrl(String installPath, ImageFormat format, DataObject dataObject);  // get the entity download URL

    void deleteExtractUrl(String installPath, String url, Upload.Type volume);

    List<DatadiskTO> getDataDiskTemplates(DataObject obj, String configurationId);

    Void createDataDiskTemplateAsync(TemplateInfo dataDiskTemplate, String path, String diskId, long fileSize, boolean bootable, AsyncCompletionCallback<CreateCmdResult> callback);
}
