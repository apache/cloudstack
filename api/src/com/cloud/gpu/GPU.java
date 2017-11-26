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
package com.cloud.gpu;


public class GPU {

    public enum Keys {
        pciDevice,
        vgpuType
    }

    public enum GPUType {
        passthrough("passthrough"),
        GRID_K100("GRID K100"),
        GRID_K120Q("GRID K120Q"),
        GRID_K140Q("GRID K140Q"),
        GRID_K160Q("GRID K160Q"),
        GRID_K180Q("GRID K180Q"),
        GRID_K200("GRID K200"),
        GRID_K220Q("GRID K220Q"),
        GRID_K240Q("GRID K240Q"),
        GRID_K260("GRID K260Q"),
        GRID_K280Q("GRID K280Q"),
        TESLA_M60_0Q("Tesla M60-0Q"),
        TESLA_M60_1Q("Tesla M60-1Q"),
        TESLA_M60_2Q("Tesla M60-2Q"),
        TESLA_M60_4Q("Tesla M60-4Q"),
        TESLA_M60_8Q("Tesla M60-8Q"),
        TESLA_M60_0B("Tesla M60-0B"),
        TESLA_M60_1B("Tesla M60-1B"),
        TESLA_M60_1A("Tesla M60-1A"),
        TESLA_M60_2A("Tesla M60-2A"),
        TESLA_M60_4A("Tesla M60-4A"),
        TESLA_M60_8A("Tesla M60-8A");

        private String type;

        GPUType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
}
