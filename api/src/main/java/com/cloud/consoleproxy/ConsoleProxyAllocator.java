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
package com.cloud.consoleproxy;

import java.util.List;
import java.util.Map;

import com.cloud.utils.component.Adapter;
import com.cloud.vm.ConsoleProxy;

public interface ConsoleProxyAllocator extends Adapter {
    /**
     * Finds the least loaded console proxy.
     * @param candidates
     * @param loadInfo
     * @param dataCenterId
     * @return id of the console proxy to use or null if none.
     */
    public Long allocProxy(List<? extends ConsoleProxy> candidates, Map<Long, Integer> loadInfo, long dataCenterId);
}
