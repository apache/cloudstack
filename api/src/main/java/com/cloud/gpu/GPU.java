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
        NVIDIA_A40_1A("NVIDIA A40-1A"),
        NVIDIA_A40_1B("NVIDIA A40-1B"),
        NVIDIA_A40_1Q("NVIDIA A40-1Q"),
        NVIDIA_A40_2A("NVIDIA A40-2A"),
        NVIDIA_A40_2B("NVIDIA A40-2B"),
        NVIDIA_A40_2Q("NVIDIA A40-2Q"),
        NVIDIA_A40_3A("NVIDIA A40-3A"),
        NVIDIA_A40_3Q("NVIDIA A40-3Q"),
        NVIDIA_A40_4A("NVIDIA A40-4A"),
        NVIDIA_A40_4Q("NVIDIA A40-4Q"),
        NVIDIA_A40_6A("NVIDIA A40-6A"),
        NVIDIA_A40_6Q("NVIDIA A40-6Q"),
        NVIDIA_A40_8A("NVIDIA A40-8A"),
        NVIDIA_A40_8Q("NVIDIA A40-8Q"),
        NVIDIA_A40_12A("NVIDIA A40-12A"),
        NVIDIA_A40_12Q("NVIDIA A40-12Q"),
        NVIDIA_A40_16A("NVIDIA A40-16A"),
        NVIDIA_A40_16Q("NVIDIA A40-16Q"),
        NVIDIA_A40_24A("NVIDIA A40-24A"),
        NVIDIA_A40_24Q("NVIDIA A40-24Q"),
        NVIDIA_A40_48A("NVIDIA A40-48A"),
        NVIDIA_A40_48Q("NVIDIA A40-48Q"),
        NVIDIA_A2_1A("NVIDIA A2-1A"),
        NVIDIA_A2_1B("NVIDIA A2-1B"),
        NVIDIA_A2_1Q("NVIDIA A2-1Q"),
        NVIDIA_A2_2A("NVIDIA A2-2A"),
        NVIDIA_A2_2B("NVIDIA A2-2B"),
        NVIDIA_A2_2Q("NVIDIA A2-2Q"),
        NVIDIA_A2_4A("NVIDIA A2-4A"),
        NVIDIA_A2_4Q("NVIDIA A2-4Q"),
        NVIDIA_A2_8A("NVIDIA A2-8A"),
        NVIDIA_A2_8Q("NVIDIA A2-8Q"),
        NVIDIA_A2_16A("NVIDIA A2-16A"),
        NVIDIA_A2_16Q("NVIDIA A2-16Q"),
        NVIDIA_A10_1A("NVIDIA A10-1A"),
        NVIDIA_A10_1B("NVIDIA A10-1B"),
        NVIDIA_A10_1Q("NVIDIA A10-1Q"),
        NVIDIA_A10_2A("NVIDIA A10-2A"),
        NVIDIA_A10_2B("NVIDIA A10-2B"),
        NVIDIA_A10_2Q("NVIDIA A10-2Q"),
        NVIDIA_A10_3A("NVIDIA A10-3A"),
        NVIDIA_A10_3Q("NVIDIA A10-3Q"),
        NVIDIA_A10_4A("NVIDIA A10-4A"),
        NVIDIA_A10_4Q("NVIDIA A10-4Q"),
        NVIDIA_A10_6A("NVIDIA A10-6A"),
        NVIDIA_A10_6Q("NVIDIA A10-6Q"),
        NVIDIA_A10_8A("NVIDIA A10-8A"),
        NVIDIA_A10_8Q("NVIDIA A10-8Q"),
        NVIDIA_A10_12A("NVIDIA A10-12A"),
        NVIDIA_A10_12Q("NVIDIA A10-12Q"),
        NVIDIA_A10_24A("NVIDIA A10-24A"),
        NVIDIA_A10_24Q("NVIDIA A10-24Q"),
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
