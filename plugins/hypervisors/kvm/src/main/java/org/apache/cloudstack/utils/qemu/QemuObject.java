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

import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Joiner;

public class QemuObject {
    private final ObjectType type;
    private final Map<ObjectParameter, String> params;

    public enum ObjectParameter {
        DATA("data"),
        FILE("file"),
        FORMAT("format"),
        ID("id"),
        IV("iv"),
        KEYID("keyid");

        private final String parameter;

        ObjectParameter(String param) {
            this.parameter = param;
        }

        @Override
        public String toString() {return parameter; }
    }

    /**
     * Supported qemu encryption formats.
     * NOTE: Only "luks" is currently supported with Libvirt, so while
     * this utility may be capable of creating various formats, care should
     * be taken to use types that work for the use case.
     */
    public enum EncryptFormat {
        LUKS("luks"),
        AES("aes");

        private final String format;

        EncryptFormat(String format) { this.format = format; }

        @Override
        public String toString() { return format;}

        public static EncryptFormat enumValue(String value) {
            if (StringUtils.isBlank(value)) {
                return LUKS; // default encryption format
            }
            return EncryptFormat.valueOf(value.toUpperCase());
        }
    }

    public enum ObjectType {
        SECRET("secret");

        private final String objectTypeValue;

        ObjectType(String objectTypeValue) {
            this.objectTypeValue = objectTypeValue;
        }

        @Override
        public String toString() {
            return objectTypeValue;
        }
    }

    public QemuObject(ObjectType type, Map<ObjectParameter, String> params) {
        this.type = type;
        this.params = params;
    }

    /**
     * Converts QemuObject into the command strings required by qemu-img flags
     * @return array of strings representing command flag and value (--object)
     */
    public String[] toCommandFlag() {
        Map<ObjectParameter, String> sorted = new TreeMap<>(params);
        String paramString = Joiner.on(",").withKeyValueSeparator("=").join(sorted);
        return new String[] {"--object", String.format("%s,%s", type, paramString) };
    }

    /**
     * Creates a QemuObject with the correct parameters for passing encryption secret details to qemu-img
     * @param format the image format to use
     * @param encryptFormat the encryption format to use (luks)
     * @param keyFilePath the path to the file containing encryption key
     * @param secretName the name to use for the secret
     * @param options the options map for qemu-img (-o flag)
     * @return the QemuObject containing encryption parameters
     */
    public static QemuObject prepareSecretForQemuImg(QemuImg.PhysicalDiskFormat format, EncryptFormat encryptFormat, String keyFilePath, String secretName, Map<String, String> options) {
        EnumMap<ObjectParameter, String> params = new EnumMap<>(ObjectParameter.class);
        params.put(ObjectParameter.ID, secretName);
        params.put(ObjectParameter.FILE, keyFilePath);

        if (options != null) {
            if (format == QemuImg.PhysicalDiskFormat.QCOW2) {
                options.put("encrypt.key-secret", secretName);
                options.put("encrypt.format", encryptFormat.toString());
            } else if (format == QemuImg.PhysicalDiskFormat.RAW || format == QemuImg.PhysicalDiskFormat.LUKS) {
                options.put("key-secret", secretName);
            }
        }
        return new QemuObject(ObjectType.SECRET, params);
    }
}
