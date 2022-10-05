// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.utils.qemu;

import com.google.common.base.Joiner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class QemuImageOptions {
    private Map<String, String> params = new HashMap<>();
    private static final String FILENAME_PARAM_KEY = "file.filename";
    private static final String LUKS_KEY_SECRET_PARAM_KEY = "key-secret";
    private static final String QCOW2_KEY_SECRET_PARAM_KEY = "encrypt.key-secret";
    private static final String DRIVER = "driver";

    private QemuImg.PhysicalDiskFormat format;
    private static final List<QemuImg.PhysicalDiskFormat> DISK_FORMATS_THAT_SUPPORT_OPTION_IMAGE_OPTS = Arrays.asList(QemuImg.PhysicalDiskFormat.QCOW2, QemuImg.PhysicalDiskFormat.LUKS);

    public QemuImageOptions(String filePath) {
        params.put(FILENAME_PARAM_KEY, filePath);
    }

    /**
     * Constructor for self-crafting the full map of parameters
     * @param params the map of parameters
     */
    public QemuImageOptions(Map<String, String> params) {
        this.params = params;
    }

    /**
     * Constructor for crafting image options that may contain a secret or format
     * @param format optional format, renders as "driver" option
     * @param filePath required path of image
     * @param secretName optional secret name for image. Secret only applies for QCOW2 or LUKS format
     */
    public QemuImageOptions(QemuImg.PhysicalDiskFormat format, String filePath, String secretName) {
        params.put(FILENAME_PARAM_KEY, filePath);
        if (secretName != null && !secretName.isBlank()) {
            if (format.equals(QemuImg.PhysicalDiskFormat.QCOW2)) {
                params.put(QCOW2_KEY_SECRET_PARAM_KEY, secretName);
            } else if (format.equals(QemuImg.PhysicalDiskFormat.LUKS)) {
                params.put(LUKS_KEY_SECRET_PARAM_KEY, secretName);
            }
        }
        setFormat(format);
    }

    public void setFormat(QemuImg.PhysicalDiskFormat format) {
        if (format != null) {
            params.put(DRIVER, format.toString());
            this.format = format;
        }
    }

    /**
     * Converts QemuImageOptions into the command strings required by qemu-img flags
     * @return array of strings representing command flag and value (--image-opts)
     */
    public String[] toCommandFlag() {
        if (format == null || !DISK_FORMATS_THAT_SUPPORT_OPTION_IMAGE_OPTS.contains(format)) {
            return new String[] { params.get(FILENAME_PARAM_KEY) };
        }
        Map<String, String> sorted = new TreeMap<>(params);
        String paramString = Joiner.on(",").withKeyValueSeparator("=").join(sorted);
        return new String[] {"--image-opts", paramString};
    }
}
