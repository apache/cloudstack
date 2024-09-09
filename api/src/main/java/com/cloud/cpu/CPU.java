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

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class CPU {

    public static final String archX86Identifier = "i686";
    public static final String archX86_64Identifier = "x86_64";
    public static final String archARM64Identifier = "aarch64";

    public static class CPUArch {
        private static final Map<String, CPUArch> cpuArchMap = new LinkedHashMap<>();

        public static final CPUArch archX86 = new CPUArch(archX86Identifier, 32);
        public static final CPUArch amd64 = new CPUArch(archX86_64Identifier, 64);
        public static final CPUArch arm64 = new CPUArch(archARM64Identifier, 64);

        private String type;
        private int bits;

        public CPUArch(String type, int bits) {
            this.type = type;
            this.bits = bits;
            cpuArchMap.put(type, this);
        }

        public String getType() {
            return this.type;
        }

        public int getBits() {
            return this.bits;
        }

        public static CPUArch fromType(String type) {
            if (StringUtils.isBlank(type)) {
                return amd64;
            }
            switch (type) {
                case archX86Identifier: return archX86;
                case archX86_64Identifier: return amd64;
                case archARM64Identifier: return arm64;
                default: throw new CloudRuntimeException(String.format("Unsupported arch type: %s", type));
            }
        }
    }
}
