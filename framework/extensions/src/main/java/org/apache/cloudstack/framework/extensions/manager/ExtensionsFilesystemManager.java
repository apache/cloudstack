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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.extension.Extension;

public interface ExtensionsFilesystemManager {

    String getExtensionsPath();

    Path getExtensionRootPath(Extension extension);

    String getExtensionPath(String relativePath);

    String getExtensionCheckedPath(String extensionName, String extensionRelativePath);

    Map<String, String> getChecksumMapForExtension(String extensionName, String relativePath);

    void prepareExtensionPath(String extensionName, boolean userDefined, Extension.Type type, String extensionRelativePath);

    void cleanupExtensionPath(String extensionName, String extensionRelativePath);

    void cleanupExtensionData(String extensionName, int olderThanDays, boolean cleanupDirectory);

    Path getExtensionsStagingPath() throws IOException;

    String prepareExternalPayload(String extensionName, Map<String, Object> details) throws IOException;

    void deleteExtensionPayload(String extensionName, String payloadFileName);

    void validateExtensionFiles(Extension extension, List<String> files);

    boolean packExtensionFilesAsTgz(Extension extension, Path extensionFilesPath, Path archivePath);
}
