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
package org.apache.cloudstack.engine.subsystem.api.storage;

import com.cloud.agent.api.to.DatadiskTO;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePool;

import java.util.List;

public interface TemplateService {

    class TemplateApiResult extends CommandResult {
        private final TemplateInfo template;

        public TemplateApiResult(TemplateInfo template) {
            super();

            this.template = template;
        }

        public TemplateInfo getTemplate() {
            return template;
        }
    }

    void createTemplateAsync(TemplateInfo template, DataStore store, AsyncCompletionCallback<TemplateApiResult> callback);

    AsyncCallFuture<TemplateApiResult> createTemplateFromSnapshotAsync(SnapshotInfo snapshot, TemplateInfo template, DataStore store);

    AsyncCallFuture<TemplateApiResult> createTemplateFromVolumeAsync(VolumeInfo volume, TemplateInfo template, DataStore store);

    boolean createOvaDataDiskTemplates(TemplateInfo parentTemplate, boolean deployAsIs);

    AsyncCallFuture<TemplateApiResult> deleteTemplateAsync(TemplateInfo template);

    AsyncCallFuture<TemplateApiResult> copyTemplate(TemplateInfo srcTemplate, DataStore destStore);

    AsyncCallFuture<TemplateApiResult> prepareTemplateOnPrimary(TemplateInfo srcTemplate, StoragePool pool);

    AsyncCallFuture<TemplateApiResult> deleteTemplateOnPrimary(TemplateInfo template, StoragePool pool);

    void syncTemplateToRegionStore(long templateId, DataStore store);

    void handleSysTemplateDownload(HypervisorType hostHyper, Long dcId);

    void handleTemplateSync(DataStore store);

    void downloadBootstrapSysTemplate(DataStore store);

    void addSystemVMTemplatesToSecondary(DataStore store);

    void associateTemplateToZone(long templateId, Long zoneId);

    void associateCrosszoneTemplatesToZone(long dcId);

    AsyncCallFuture<TemplateApiResult> createDatadiskTemplateAsync(TemplateInfo parentTemplate, TemplateInfo dataDiskTemplate, String path, String diskId, long fileSize, boolean bootable);

    List<DatadiskTO> getTemplateDatadisksOnImageStore(TemplateInfo templateInfo, String configurationId);
}
