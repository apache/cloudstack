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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.ConsoleProxy;

public class ConsoleProxyBalanceAllocator extends AdapterBase implements ConsoleProxyAllocator {

    @Override
    public Long allocProxy(List<? extends ConsoleProxy> candidates, final Map<Long, Integer> loadInfo, long dataCenterId) {
        List<ConsoleProxy> allocationList = new ArrayList<ConsoleProxy>(candidates);

        Collections.sort(candidates, new Comparator<ConsoleProxy>() {
            @Override
            public int compare(ConsoleProxy x, ConsoleProxy y) {
                Integer loadOfX = loadInfo.get(x.getId());
                Integer loadOfY = loadInfo.get(y.getId());

                if (loadOfX != null && loadOfY != null) {
                    if (loadOfX < loadOfY)
                        return -1;
                    else if (loadOfX > loadOfY)
                        return 1;
                    return 0;
                } else if (loadOfX == null && loadOfY == null) {
                    return 0;
                } else {
                    if (loadOfX == null)
                        return -1;
                    return 1;
                }
            }
        });

        return (allocationList.size() > 0) ? allocationList.get(0).getId() : null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
