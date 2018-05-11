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

package com.cloud.agent.direct.download;

public interface DirectTemplateDownloader {

    class DirectTemplateInformation {
        private String installPath;
        private Long size;
        private String checksum;

        public DirectTemplateInformation(String installPath, Long size, String checksum) {
            this.installPath = installPath;
            this.size = size;
            this.checksum = checksum;
        }

        public String getInstallPath() {
            return installPath;
        }

        public Long getSize() {
            return size;
        }

        public String getChecksum() {
            return checksum;
        }
    }

    /**
     * Perform template download to pool specified on downloader creation
     * @return true if successful, false if not
     */
    boolean downloadTemplate();

    /**
     * Perform extraction (if necessary) and installation of previously downloaded template
     * @return true if successful, false if not
     */
    boolean extractAndInstallDownloadedTemplate();

    /**
     * Get template information after it is properly installed on pool
     * @return template information
     */
    DirectTemplateInformation getTemplateInformation();

    /**
     * Perform checksum validation of previously downloadeed template
     * @return true if successful, false if not
     */
    boolean validateChecksum();
}
