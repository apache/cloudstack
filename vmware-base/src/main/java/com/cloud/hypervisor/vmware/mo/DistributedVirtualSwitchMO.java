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
import java.util.concurrent.ConcurrentHashMap;


import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.utils.Pair;
import com.vmware.vim25.DVPortgroupConfigSpec;
import com.vmware.vim25.DVSConfigInfo;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VMwareDVSConfigInfo;
import com.vmware.vim25.VMwareDVSConfigSpec;
import com.vmware.vim25.VMwareDVSPvlanMapEntry;

public class DistributedVirtualSwitchMO extends BaseMO {
    @SuppressWarnings("unused")
    private static ConcurrentHashMap<String, List<String>> s_dvPortGroupCacheMap = null;

    public DistributedVirtualSwitchMO(VmwareContext context, ManagedObjectReference morDvs) {
        super(context, morDvs);
        s_dvPortGroupCacheMap = new ConcurrentHashMap<String, List<String>>();
    }

    public DistributedVirtualSwitchMO(VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
        s_dvPortGroupCacheMap = new ConcurrentHashMap<String, List<String>>();
    }

    public void createDVPortGroup(DVPortgroupConfigSpec dvPortGroupSpec) throws Exception {
        List<DVPortgroupConfigSpec> dvPortGroupSpecArray = new ArrayList<DVPortgroupConfigSpec>();
        dvPortGroupSpecArray.add(dvPortGroupSpec);
        boolean dvPortGroupExists = false;
        String dvSwitchInstance = _mor.getValue();
        String dvPortGroupName = dvPortGroupSpec.getName();
        String uniquedvPortGroupPerDvs = dvSwitchInstance + dvPortGroupName;
        List<String> dvPortGroupList = null;
        synchronized (uniquedvPortGroupPerDvs.intern()) {
            // Looking up local cache rather than firing another API call to see if dvPortGroup exists already.
            if (s_dvPortGroupCacheMap.containsKey(dvSwitchInstance)) {
                dvPortGroupList = s_dvPortGroupCacheMap.get(dvSwitchInstance);
                if (dvPortGroupList != null && dvPortGroupList.contains(dvPortGroupName)) {
                    dvPortGroupExists = true;
                }
            }
            if (!dvPortGroupExists) {
                ManagedObjectReference task = _context.getService().addDVPortgroupTask(_mor, dvPortGroupSpecArray);
                if (!_context.getVimClient().waitForTask(task)) {
                    throw new Exception("Failed to create dvPortGroup " + dvPortGroupSpec.getName());
                } else {
                    if (s_dvPortGroupCacheMap.containsKey(dvSwitchInstance)) {
                        dvPortGroupList = s_dvPortGroupCacheMap.get(dvSwitchInstance);
                        if (dvPortGroupList == null) {
                            dvPortGroupList = new ArrayList<String>();
                        }
                        dvPortGroupList.add(dvPortGroupName); //does this update map?
                    } else {
                        dvPortGroupList = new ArrayList<String>();
                        dvPortGroupList.add(dvPortGroupName);
                        s_dvPortGroupCacheMap.put(dvSwitchInstance, dvPortGroupList);
                    }
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Created dvPortGroup. dvPortGroup cache is :" + s_dvPortGroupCacheMap);
                }
            } else if (logger.isDebugEnabled()) {
                logger.debug("Detected dvPortGroup [" + dvPortGroupName + "] already present. Not attempting to create again.");
            }
        }
    }

    public void updateDvPortGroup(ManagedObjectReference dvPortGroupMor, DVPortgroupConfigSpec dvPortGroupSpec) throws Exception {
        synchronized (dvPortGroupMor.getValue().intern()) {
            ManagedObjectReference task = _context.getService().reconfigureDVPortgroupTask(dvPortGroupMor, dvPortGroupSpec);
            if (!_context.getVimClient().waitForTask(task)) {
                throw new Exception("Failed to update dvPortGroup " + dvPortGroupMor.getValue());
            }
        }
    }

    public void updateVMWareDVSwitch(ManagedObjectReference dvSwitchMor, VMwareDVSConfigSpec dvsSpec) throws Exception {
        _context.getService().reconfigureDvsTask(dvSwitchMor, dvsSpec);
    }

    public TaskInfo updateVMWareDVSwitchGetTask(ManagedObjectReference dvSwitchMor, VMwareDVSConfigSpec dvsSpec) throws Exception {
        ManagedObjectReference task = _context.getService().reconfigureDvsTask(dvSwitchMor, dvsSpec);
        TaskInfo info = (TaskInfo)(_context.getVimClient().getDynamicProperty(task, "info"));
        _context.getVimClient().waitForTask(task);
        return info;
    }

    public String getDVSConfigVersion(ManagedObjectReference dvSwitchMor) throws Exception {
        assert (dvSwitchMor != null);
        DVSConfigInfo dvsConfigInfo = (DVSConfigInfo)_context.getVimClient().getDynamicProperty(dvSwitchMor, "config");
        return dvsConfigInfo.getConfigVersion();
    }

    public String getDVSProductVersion(ManagedObjectReference dvSwitchMor) throws Exception {
        assert (dvSwitchMor != null);
        DVSConfigInfo dvsConfigInfo = (DVSConfigInfo)_context.getVimClient().getDynamicProperty(dvSwitchMor, "config");
        if (dvsConfigInfo != null && dvsConfigInfo.getProductInfo() != null) {
            return dvsConfigInfo.getProductInfo().getVersion();
        }
        return null;
    }

    public Map<Integer, HypervisorHostHelper.PvlanType> retrieveVlanPvlan(int vlanid, int secondaryvlanid, ManagedObjectReference dvSwitchMor) throws Exception {
        assert (dvSwitchMor != null);

        Map<Integer, HypervisorHostHelper.PvlanType> result = new HashMap<Integer, HypervisorHostHelper.PvlanType>();

        VMwareDVSConfigInfo configinfo = (VMwareDVSConfigInfo)_context.getVimClient().getDynamicProperty(dvSwitchMor, "config");
        List<VMwareDVSPvlanMapEntry> pvlanconfig = null;
        pvlanconfig = configinfo.getPvlanConfig();

        if (null == pvlanconfig || 0 == pvlanconfig.size()) {
            return result;
        }
        // Iterate through the pvlanMapList and check if the specified vlan id and pvlan id exist. If they do, set the fields in result accordingly.

        for (VMwareDVSPvlanMapEntry mapEntry : pvlanconfig) {
            int entryVlanid = mapEntry.getPrimaryVlanId();
            int entryPvlanid = mapEntry.getSecondaryVlanId();
            if (entryVlanid == entryPvlanid) {
                // promiscuous
                if (vlanid == entryVlanid) {
                    // pvlan type will always be promiscuous in this case.
                    result.put(vlanid, HypervisorHostHelper.PvlanType.valueOf(mapEntry.getPvlanType()));
                } else if ((vlanid != secondaryvlanid) && secondaryvlanid == entryVlanid) {
                    result.put(secondaryvlanid, HypervisorHostHelper.PvlanType.valueOf(mapEntry.getPvlanType()));
                }
            } else {
                if (vlanid == entryVlanid) {
                    // vlan id in entry is promiscuous
                    result.put(vlanid, HypervisorHostHelper.PvlanType.promiscuous);
                } else if (vlanid == entryPvlanid) {
                    result.put(vlanid, HypervisorHostHelper.PvlanType.valueOf(mapEntry.getPvlanType()));
                }
                if ((vlanid != secondaryvlanid) && secondaryvlanid == entryVlanid) {
                    //promiscuous
                    result.put(secondaryvlanid, HypervisorHostHelper.PvlanType.promiscuous);
                } else if (secondaryvlanid == entryPvlanid) {
                    result.put(secondaryvlanid, HypervisorHostHelper.PvlanType.valueOf(mapEntry.getPvlanType()));
                }

            }
            // If we already know that the vlanid is being used as a non primary vlan, it's futile to
            // go over the entire list. Return.
            if (result.containsKey(vlanid) && result.get(vlanid) != HypervisorHostHelper.PvlanType.promiscuous)
                return result;

            // If we've already found both vlanid and pvlanid, we have enough info to make a decision. Return.
            if (result.containsKey(vlanid) && result.containsKey(secondaryvlanid))
                return result;
        }
        return result;
    }

    public Pair<Integer, HypervisorHostHelper.PvlanType> retrieveVlanFromPvlan(int pvlanid, ManagedObjectReference dvSwitchMor) throws Exception {
        assert (dvSwitchMor != null);

        Pair<Integer, HypervisorHostHelper.PvlanType> result = null;

        VMwareDVSConfigInfo configinfo = (VMwareDVSConfigInfo)_context.getVimClient().getDynamicProperty(dvSwitchMor, "config");
        List<VMwareDVSPvlanMapEntry> pvlanConfig = null;
        pvlanConfig = configinfo.getPvlanConfig();

        if (null == pvlanConfig || 0 == pvlanConfig.size()) {
            return result;
        }

        // Iterate through the pvlanMapList and check if the specified pvlan id exist. If it does, set the fields in result accordingly.
        for (VMwareDVSPvlanMapEntry mapEntry : pvlanConfig) {
            int entryVlanid = mapEntry.getPrimaryVlanId();
            int entryPvlanid = mapEntry.getSecondaryVlanId();
            if (pvlanid == entryPvlanid) {
                result = new Pair<>(entryVlanid, HypervisorHostHelper.PvlanType.valueOf(mapEntry.getPvlanType()));
                break;
            }
        }
        return result;
    }
}
