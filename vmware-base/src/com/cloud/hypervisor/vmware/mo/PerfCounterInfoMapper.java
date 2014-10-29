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
package com.cloud.hypervisor.vmware.mo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfSummaryType;

public class PerfCounterInfoMapper {
    // map <group name, counter name, list of PerfCounterInfo based on rollupType>
    Map<String, Map<String, List<PerfCounterInfo>>> _mapCounterInfos = new HashMap<String, Map<String, List<PerfCounterInfo>>>();

    public PerfCounterInfoMapper(PerfCounterInfo[] counterInfos) {
        if (counterInfos != null) {
            for (PerfCounterInfo counterInfo : counterInfos) {
                List<PerfCounterInfo> counterInfoList = getSafeCounterInfoList(counterInfo.getGroupInfo().getKey(), counterInfo.getNameInfo().getKey());
                counterInfoList.add(counterInfo);
            }
        }
    }

    public PerfCounterInfo[] lookup(String groupName, String counterName, PerfSummaryType rollupType) {
        assert (groupName != null);
        assert (counterName != null);

        Map<String, List<PerfCounterInfo>> groupMap = _mapCounterInfos.get(groupName);
        if (groupMap == null)
            return null;

        List<PerfCounterInfo> counterInfoList = groupMap.get(counterName);
        if (counterInfoList == null)
            return null;

        if (rollupType == null) {
            return counterInfoList.toArray(new PerfCounterInfo[0]);
        }

        for (PerfCounterInfo info : counterInfoList) {
            if (info.getRollupType() == rollupType)
                return new PerfCounterInfo[] {info};
        }

        return null;
    }

    public PerfCounterInfo lookupOne(String groupName, String counterName, PerfSummaryType rollupType) {
        PerfCounterInfo[] infos = lookup(groupName, counterName, rollupType);
        if (infos != null && infos.length > 0)
            return infos[0];

        return null;
    }

    private Map<String, List<PerfCounterInfo>> getSafeGroupMap(String groupName) {
        Map<String, List<PerfCounterInfo>> groupMap = _mapCounterInfos.get(groupName);
        if (groupMap == null) {
            groupMap = new HashMap<String, List<PerfCounterInfo>>();
            _mapCounterInfos.put(groupName, groupMap);
        }
        return groupMap;
    }

    private List<PerfCounterInfo> getSafeCounterInfoList(String groupName, String counterName) {
        Map<String, List<PerfCounterInfo>> groupMap = getSafeGroupMap(groupName);
        assert (groupMap != null);

        List<PerfCounterInfo> counterInfoList = groupMap.get(counterName);
        if (counterInfoList == null) {
            counterInfoList = new ArrayList<PerfCounterInfo>();
            groupMap.put(counterName, counterInfoList);
        }
        return counterInfoList;
    }
}
