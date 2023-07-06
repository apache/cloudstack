//
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
//

package org.apache.cloudstack.direct.download;

import com.cloud.utils.Pair;

import java.util.List;

public interface DirectTemplateDownloader {

    /**
     * Perform template download to pool specified on downloader creation
     * @return (true if successful, false if not, download file path)
     */
    Pair<Boolean, String> downloadTemplate();

    /**
     * Perform checksum validation of previously downloaded template
     * @return true if successful, false if not
     */
    boolean validateChecksum();

    /**
     * Validate if the URL is reachable and returns HTTP.OK status code
     * @return true if the URL is reachable, false if not
     */
    boolean checkUrl(String url);

    /**
     * Obtain the remote file size (and virtual size in case format is qcow2)
     */
    Long getRemoteFileSize(String url, String format);

    /**
     * Get list of urls within metalink content ordered by ascending priority
     * (for those which priority tag is not defined, highest priority value is assumed)
     */
    List<String> getMetalinkUrls(String metalinkUrl);

    /**
     * Get the list of checksums within a metalink content
     */
    List<String> getMetalinkChecksums(String metalinkUrl);
}