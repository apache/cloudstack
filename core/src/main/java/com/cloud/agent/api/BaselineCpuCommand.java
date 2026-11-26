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

package com.cloud.agent.api;

import java.util.List;

/**
 * Runs {@code virsh cpu-baseline} over the given per-host {@code <cpu>} elements and returns the
 * most feature-rich CPU compatible with all of them: the common-denominator cluster baseline.
 */
public class BaselineCpuCommand extends Command {

    private List<String> hostCpuXmls;

    protected BaselineCpuCommand() {
    }

    public BaselineCpuCommand(List<String> hostCpuXmls) {
        this.hostCpuXmls = hostCpuXmls;
    }

    public List<String> getHostCpuXmls() {
        return hostCpuXmls;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
