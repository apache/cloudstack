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

package com.cloud.utils.storage;

import org.apache.commons.io.FilenameUtils;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class TemplateDownloaderUtil {

    private TemplateDownloaderUtil() {}

    /**
     * Checks if downloaded template is extractable
     * @return true if it should be extracted, false if not
     */
    public static boolean isTemplateExtractable(String templatePath) {
        String type = Script.runSimpleBashScript("file " + templatePath + " | awk -F' ' '{print $2}'");
        return type.equalsIgnoreCase("bzip2") || type.equalsIgnoreCase("gzip")
                || type.equalsIgnoreCase("zip") || type.equalsIgnoreCase("xz");
    }

    /**
     * Return extract command to execute given downloaded file
     * @param downloadedTemplateFile
     * @param templateFile
     */
    public static String getExtractCommandForDownloadedFile(String downloadedTemplateFile, String templateFile) {
        String extension = FilenameUtils.getExtension(downloadedTemplateFile).toLowerCase();
        switch (extension) {
            case "zip":
                return String.format("unzip -p '%s' | cat > '%s'", downloadedTemplateFile, templateFile);
            case "bz2":
                return String.format("bunzip2 -c '%s' > '%s'", downloadedTemplateFile, templateFile);
            case "gz":
                return String.format("gunzip -c '%s' > '%s'", downloadedTemplateFile, templateFile);
            case "xz":
                return String.format("xz -d -c '%s' > '%s'", downloadedTemplateFile, templateFile);
            default:
                throw new CloudRuntimeException("Unable to extract template: " + downloadedTemplateFile + " (unsupported format: ." + extension + ")");
        }
    }
}
