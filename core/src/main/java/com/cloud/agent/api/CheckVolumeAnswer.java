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

package com.cloud.agent.api;

public class CheckVolumeAnswer extends Answer {

    private long size;

    CheckVolumeAnswer() {
    }

    public CheckVolumeAnswer(CheckVolumeCommand cmd, String details, long size) {
        super(cmd, true, details);
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public String getString() {
        return "CheckVolumeAnswer [size=" + size + "]";
    }
}
