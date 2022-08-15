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
        GRID_K100("GRID K100"),
        GRID_K120Q("GRID K120Q"),
        GRID_K140Q("GRID K140Q"),
        GRID_K200("GRID K200"),
        GRID_K220Q("GRID K220Q"),
        GRID_K240Q("GRID K240Q"),
        GRID_K260("GRID K260Q"),
        GRID_V100D_32A("GRID V100D-32A"),
        GRID_V100D_8Q("GRID V100D-8Q"),
        GRID_V100D_4A("GRID V100D-4A"),
        GRID_V100D_1B("GRID V100D-1B"),
        GRID_V100D_2Q("GRID V100D-2Q"),
        GRID_V100D_4Q("GRID V100D-4Q"),
        GRID_V100D_2A("GRID V100D-2A"),
        GRID_V100D_2B("GRID V100D-2B"),
        GRID_V100D_32Q("GRID V100D-32Q"),
        GRID_V100D_16A("GRID V100D-16A"),
        GRID_V100D_1Q("GRID V100D-1Q"),
        GRID_V100D_2B4("GRID V100D-2B4"),
        GRID_V100D_16Q("GRID V100D-16Q"),
        GRID_V100D_8A("GRID V100D-8A"),
        GRID_V100D_1A("GRID V100D-1A"),
        GRID_T4_16A("GRID T4-16A"),
        GRID_T4_2B4("GRID T4-2B4"),
        GRID_T4_4Q("GRID T4-4Q"),
        GRID_T4_16Q("GRID T4-16Q"),
        GRID_T4_4A("GRID T4-4A"),
        GRID_T4_1A("GRID T4-1A"),
        GRID_T4_2Q("GRID T4-2Q"),
        GRID_T4_2B("GRID T4-2B"),
        GRID_T4_8Q("GRID T4-8Q"),
        GRID_T4_2A("GRID T4-2A"),
        GRID_T4_1B("GRID T4-1B"),
        GRID_T4_1Q("GRID T4-1Q"),
        GRID_T4_8A("GRID T4-8A"),
        NVIDIA_RTX5500_1A("NVIDIA RTXA5500-1A"),
        NVIDIA_RTX5500_1B("NVIDIA RTXA5500-1B"),
        NVIDIA_RTX5500_1Q("NVIDIA RTXA5500-1Q"),
        NVIDIA_RTX5500_2A("NVIDIA RTXA5500-2A"),
        NVIDIA_RTX5500_2B("NVIDIA RTXA5500-2B"),
        NVIDIA_RTX5500_2Q("NVIDIA RTXA5500-2Q"),
        NVIDIA_RTX5500_3A("NVIDIA RTXA5500-3A"),
        NVIDIA_RTX5500_3Q("NVIDIA RTXA5500-3Q"),
        NVIDIA_RTX5500_4A("NVIDIA RTXA5500-4A"),
        NVIDIA_RTX5500_4Q("NVIDIA RTXA5500-4Q"),
        NVIDIA_RTX5500_6A("NVIDIA RTXA5500-6A"),
        NVIDIA_RTX5500_6Q("NVIDIA RTXA5500-6Q"),
        NVIDIA_RTX5500_8A("NVIDIA RTXA5500-8A"),
        NVIDIA_RTX5500_8Q("NVIDIA RTXA5500-8Q"),
        NVIDIA_RTX5500_12A("NVIDIA RTXA5500-12A"),
        NVIDIA_RTX5500_12Q("NVIDIA RTXA5500-12Q"),
        NVIDIA_RTX5500_24A("NVIDIA RTXA5500-24A"),
        NVIDIA_RTX5500_24Q("NVIDIA RTXA5500-24Q"),
        passthrough("passthrough");

        private String type;

        GPUType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
}
