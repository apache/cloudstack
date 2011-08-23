/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.mo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfSummaryType;

public class PerfCounterInfoMapper {
    // map <group name, counter name, list of PerfCounterInfo based on rollupType>
    Map<String, Map<String, List<PerfCounterInfo>>> _mapCounterInfos = 
        new HashMap<String, Map<String, List<PerfCounterInfo>>>();
    
    public PerfCounterInfoMapper(PerfCounterInfo[] counterInfos) {
        if(counterInfos != null) {
            for(PerfCounterInfo counterInfo : counterInfos) {
                List<PerfCounterInfo> counterInfoList = getSafeCounterInfoList(
                    counterInfo.getGroupInfo().getKey(), counterInfo.getNameInfo().getKey());
                counterInfoList.add(counterInfo);
            }
        }
    }
    
    public PerfCounterInfo[] lookup(String groupName, String counterName, PerfSummaryType rollupType) {
        assert(groupName != null);
        assert(counterName != null);
        
        Map<String, List<PerfCounterInfo>> groupMap = _mapCounterInfos.get(groupName);
        if(groupMap == null)
            return null;
        
        List<PerfCounterInfo> counterInfoList = groupMap.get(counterName);
        if(counterInfoList == null)
            return null;
        
        if(rollupType == null) {
            return counterInfoList.toArray(new PerfCounterInfo[0]);
        }
        
        for(PerfCounterInfo info : counterInfoList) {
            if(info.getRollupType() == rollupType)
                return new PerfCounterInfo[] { info };
        }
        
        return null; 
    }
    
    public PerfCounterInfo lookupOne(String groupName, String counterName, PerfSummaryType rollupType) {
        PerfCounterInfo[] infos = lookup(groupName, counterName, rollupType);
        if(infos != null && infos.length > 0)
            return infos[0];
        
        return null;
    }
    
    private Map<String, List<PerfCounterInfo>> getSafeGroupMap(String groupName) {
        Map<String, List<PerfCounterInfo>> groupMap = _mapCounterInfos.get(groupName);
        if(groupMap == null) {
            groupMap = new HashMap<String, List<PerfCounterInfo>>();
            _mapCounterInfos.put(groupName, groupMap);
        }
        return groupMap;
    }
    
    private List<PerfCounterInfo> getSafeCounterInfoList(String groupName, String counterName) {
        Map<String, List<PerfCounterInfo>> groupMap = getSafeGroupMap(groupName);
        assert(groupMap != null);
        
        List<PerfCounterInfo> counterInfoList = groupMap.get(counterName);
        if(counterInfoList == null) {
            counterInfoList = new ArrayList<PerfCounterInfo>();
            groupMap.put(counterName, counterInfoList);
        }
        return counterInfoList;
    }
}
