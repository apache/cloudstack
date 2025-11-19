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

package org.apache.cloudstack.framework.extensions.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.framework.extensions.api.ImportExtensionCmd;
import org.apache.cloudstack.framework.extensions.dao.ExtensionDao;
import org.apache.cloudstack.framework.extensions.util.ExtensionConfig;
import org.apache.cloudstack.framework.extensions.util.YamlParser;
import org.apache.cloudstack.framework.extensions.util.ZipExtractor;
import org.apache.cloudstack.framework.extensions.vo.ExtensionVO;
import org.apache.commons.lang3.StringUtils;

import com.cloud.hypervisor.ExternalProvisioner;
import com.cloud.utils.HttpUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.exception.CloudRuntimeException;

public class ExtensionsImportManagerImpl extends ManagerBase implements ExtensionsImportManager {

    @Inject
    ExtensionsManager extensionsManager;

    @Inject
    ExternalProvisioner externalProvisioner;

    @Inject
    ExtensionDao extensionDao;

    protected Extension importExtensionInternal(String manifestUrl, Path tempDir) {
        Path manifestPath = tempDir.resolve("manifest.yaml");
        HttpUtils.downloadFileWithProgress(manifestUrl, manifestPath.toString(), logger);
        if (!Files.exists(manifestPath)) {
            throw new CloudRuntimeException("Failed to download extension manifest from URL: " + manifestUrl);
        }
        final ExtensionConfig extensionConfig = YamlParser.parseYamlFile(manifestPath.toString());
        //Parse the manifest and create the extension
        final String name = extensionConfig.metadata.name;
        final String extensionArchiveURL = extensionConfig.getArchiveUrl();
        ExtensionVO extensionByName = extensionDao.findByName(name);
        if (extensionByName != null) {
            throw new CloudRuntimeException("Extension by name already exists");
        }
        if (StringUtils.isBlank(extensionArchiveURL)) {
            throw new CloudRuntimeException("Unable to retrieve archive URL for extension source during import");
        }
        Path extensionArchivePath = tempDir.resolve(UUID.randomUUID() + ".zip");
        HttpUtils.downloadFileWithProgress(extensionArchiveURL, extensionArchivePath.toString(), logger);
        if (!Files.exists(extensionArchivePath)) {
            throw new CloudRuntimeException("Failed to download extension archive from URL: " + extensionArchiveURL);
        }
        final String extensionRootPath = externalProvisioner.getExtensionsPath() + File.separator + name;
        try {
            ZipExtractor.extractZipContents(extensionArchivePath.toString(), extensionRootPath);
        } catch (IOException e) {
            throw new CloudRuntimeException("Failed to extract extension archive during import at: " + extensionRootPath, e);
        }
        return Transaction.execute((TransactionCallbackWithException<Extension, CloudRuntimeException>) status -> {
            Extension extension = extensionsManager.createExtension(name, extensionConfig.metadata.description,
                    extensionConfig.spec.type, extensionConfig.spec.entrypoint.path, Extension.State.Enabled.name(),
                    false, Collections.emptyMap());

            for (ExtensionConfig.CustomAction action : extensionConfig.spec.customActions) {
                List<Map<String, String>> parameters = action.getParametersMapList();
                Map<Integer, Collection<Map<String, String>>> parametersMap = new HashMap<>();
                parametersMap.put(1, parameters);
                extensionsManager.addCustomAction(action.name, action.description, extension.getId(),
                        action.resourcetype, action.allowedroletypes, action.timeout, true, parametersMap,
                        null, null, Collections.emptyMap());
            }
            return null;
        });
    }

    @Override
    public Extension importExtension(ImportExtensionCmd cmd) {
        final String manifestUrl = cmd.getManifestUrl();
        final String extensionsRootPath = externalProvisioner.getExtensionsPath();

        Path tempDir;
        try {
            Path extensionsRootDir = Paths.get(extensionsRootPath);
            Files.createDirectories(extensionsRootDir);
            tempDir = Files.createTempDirectory(extensionsRootDir, "import-ext-");

        } catch (IOException e) {
            logger.error("Failed to create working directory for import extension, {}", extensionsRootPath, e);
            throw new CloudRuntimeException("Failed to create working directory for import extension", e);
        }
        try {
            return importExtensionInternal(manifestUrl, tempDir);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }/* finally {
            FileUtil.deletePath(tempDir.toString());
        }*/
    }
}
