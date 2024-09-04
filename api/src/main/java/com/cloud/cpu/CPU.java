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

public class CPU {

    public static final String archX86Identifier = "i686";
    public static final String archX86_64Identifier = "x86_64";
    public static final String archARM64Identifier = "aarch64";

    public enum CPUArch {
        X86(archX86Identifier, 32),
        X86_64(archX86_64Identifier, 64),
        ARM64(archARM64Identifier, 64);

        private String type;
        private int bits;

        CPUArch(String type, int bits) {
            this.type = type;
            this.bits = bits;
        }

        public String getType() {
            return this.type;
        }

        public int getBits() {
            return this.bits;
        }

        public static CPUArch fromType(String type) {
            if (StringUtils.isBlank(type)) {
                return X86_64;
            }
            switch (type) {
                case archX86Identifier: return X86;
                case archX86_64Identifier: return X86_64;
                case archARM64Identifier: return ARM64;
                default: throw new CloudRuntimeException(String.format("Unsupported arch type: %s", type));
            }
        }
    }
}
