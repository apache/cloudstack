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
package com.cloud.cpu;

import org.apache.commons.lang3.StringUtils;

public class CPU {
    public enum CPUArch {
        x86("i686", 32),
        amd64("x86_64", 64),
        arm64("aarch64", 64);

        private final String type;
        private final int bits;

        CPUArch(String type, int bits) {
            this.type = type;
            this.bits = bits;
        }

        public static CPUArch getDefault() {
            return amd64;
        }

        public String getType() {
            return type;
        }

        public int getBits() {
            return bits;
        }

        public static CPUArch fromType(String type) {
            if (StringUtils.isBlank(type)) {
                return getDefault();
            }
            for (CPUArch arch : values()) {
                if (arch.type.equals(type)) {
                    return arch;
                }
            }
            throw new IllegalArgumentException("Unsupported arch type: " + type);
        }

        public static String getTypesAsCSV() {
            StringBuilder sb = new StringBuilder();
            for (CPUArch arch : values()) {
                sb.append(arch.getType()).append(",");
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            return sb.toString();
        }
    }
}
