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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.utils.Pair;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.VirtualMachineBootOptions;
import com.vmware.vim25.VirtualMachinePowerState;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.apache.commons.lang3.StringUtils;

import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.ManagedObjectReference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseMO {
    protected static Logger logger = LogManager.getLogger(BaseMO.class);

    protected VmwareContext _context;
    protected ManagedObjectReference _mor;

    protected static String[] propertyPathsForUnmanagedVmsThinListing = new String[] {"name", "config.template",
            "runtime.powerState", "config.guestId", "config.guestFullName", "runtime.host",
            "config.bootOptions", "config.firmware"};

    private String _name;

    public BaseMO(VmwareContext context, ManagedObjectReference mor) {
        assert (context != null);

        _context = context;
        _mor = mor;
    }

    public BaseMO(VmwareContext context, String morType, String morValue) {
        assert (context != null);
        assert (morType != null);
        assert (morValue != null);

        _context = context;
        _mor = new ManagedObjectReference();
        _mor.setType(morType);
        _mor.setValue(morValue);
    }

    public VmwareContext getContext() {
        return _context;
    }

    public ManagedObjectReference getMor() {
        assert (_mor != null);
        return _mor;
    }

    public ManagedObjectReference getParentMor() throws Exception {
        return _context.getVimClient().getDynamicProperty(_mor, "parent");
    }

    public String getName() throws Exception {
        if (_name == null)
            _name = _context.getVimClient().getDynamicProperty(_mor, "name");

        return _name;
    }

    public void unregisterVm() throws Exception {
        _context.getService().unregisterVM(_mor);
    }

    public boolean destroy() throws Exception {
        ManagedObjectReference morTask = _context.getService().destroyTask(_mor);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            logger.error("VMware destroy_Task failed due to {}", TaskMO.getTaskFailureInfo(_context, morTask));
        }
        return false;
    }

    public void reload() throws Exception {
        _context.getService().reload(_mor);
    }

    public boolean rename(String newName) throws Exception {
        ManagedObjectReference morTask = _context.getService().renameTask(_mor, newName);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            logger.error("VMware rename_Task failed due to {}", TaskMO.getTaskFailureInfo(_context, morTask));
        }
        return false;
    }

    public void setCustomFieldValue(String fieldName, String value) throws Exception {
        CustomFieldsManagerMO cfmMo = new CustomFieldsManagerMO(_context, _context.getServiceContent().getCustomFieldsManager());

        int key = getCustomFieldKey(fieldName);
        if (key == 0) {
            try {
                CustomFieldDef field = cfmMo.addCustomerFieldDef(fieldName, getMor().getType(), null, null);
                key = field.getKey();
            } catch (Exception e) {
                // assuming the exception is caused by concurrent operation from other places
                // so we retrieve the key again
                key = getCustomFieldKey(fieldName);
            }
        }

        if (key == 0)
            throw new Exception("Unable to setup custom field facility");

        cfmMo.setField(getMor(), key, value);
    }

    public String getCustomFieldValue(String fieldName) throws Exception {
        int key = getCustomFieldKey(fieldName);
        if (key == 0)
            return null;

        CustomFieldStringValue cfValue = _context.getVimClient().getDynamicProperty(getMor(), String.format("value[%d]", key));
        if (cfValue != null)
            return cfValue.getValue();

        return null;
    }

    public int getCustomFieldKey(String fieldName) throws Exception {
        return getCustomFieldKey(getMor().getType(), fieldName);
    }

    public int getCustomFieldKey(String morType, String fieldName) throws Exception {
        assert (morType != null);

        ManagedObjectReference cfmMor = _context.getServiceContent().getCustomFieldsManager();
        if (cfmMor == null) {
            return 0;
        }

        CustomFieldsManagerMO cfmMo = new CustomFieldsManagerMO(_context, cfmMor);

        return cfmMo.getCustomFieldKey(morType, fieldName);
    }

    private static UnmanagedInstanceTO.PowerState convertPowerState(VirtualMachinePowerState powerState) {
        return powerState == VirtualMachinePowerState.POWERED_ON ? UnmanagedInstanceTO.PowerState.PowerOn :
                powerState == VirtualMachinePowerState.POWERED_OFF ? UnmanagedInstanceTO.PowerState.PowerOff : UnmanagedInstanceTO.PowerState.PowerUnknown;
    }

    protected List<UnmanagedInstanceTO> convertVmsObjectContentsToUnmanagedInstances(List<ObjectContent> ocs, String keyword) throws Exception {
        Map<String, Pair<String, String>> hostClusterNamesMap = new HashMap<>();
        List<UnmanagedInstanceTO> vms = new ArrayList<>();
        if (ocs != null) {
            for (ObjectContent oc : ocs) {
                List<DynamicProperty> objProps = oc.getPropSet();
                if (objProps != null) {
                    UnmanagedInstanceTO vm = createUnmanagedInstanceTOFromThinListingDynamicProperties(
                            objProps, keyword, hostClusterNamesMap);
                    if (vm != null) {
                        vms.add(vm);
                    }
                }
            }
        }
        if (vms.size() > 0) {
            vms.sort(Comparator.comparing(UnmanagedInstanceTO::getName));
        }
        return vms;
    }

    private UnmanagedInstanceTO createUnmanagedInstanceTOFromThinListingDynamicProperties(List<DynamicProperty> objProps,
                                                                                            String keyword,
                                                                                            Map<String, Pair<String, String>> hostClusterNamesMap) throws Exception {
        UnmanagedInstanceTO vm = new UnmanagedInstanceTO();
        String vmName;
        boolean isTemplate = false;
        boolean excludeByKeyword = false;

        for (DynamicProperty objProp : objProps) {
            if (objProp.getName().equals("name")) {
                vmName = (String) objProp.getVal();
                if (StringUtils.isNotBlank(keyword) && !vmName.contains(keyword)) {
                    excludeByKeyword = true;
                }
                vm.setName(vmName);
            } else if (objProp.getName().equals("config.template")) {
                isTemplate = (Boolean) objProp.getVal();
            } else if (objProp.getName().equals("runtime.powerState")) {
                VirtualMachinePowerState powerState = (VirtualMachinePowerState) objProp.getVal();
                vm.setPowerState(convertPowerState(powerState));
            } else if (objProp.getName().equals("config.guestFullName")) {
                vm.setOperatingSystem((String) objProp.getVal());
            } else if (objProp.getName().equals("config.guestId")) {
                vm.setOperatingSystemId((String) objProp.getVal());
            } else if (objProp.getName().equals("config.bootOptions")) {
                VirtualMachineBootOptions bootOptions = (VirtualMachineBootOptions) objProp.getVal();
                String bootMode = "LEGACY";
                if (bootOptions != null && bootOptions.isEfiSecureBootEnabled()) {
                    bootMode = "SECURE";
                }
                vm.setBootMode(bootMode);
            } else if (objProp.getName().equals("config.firmware")) {
                String firmware = (String) objProp.getVal();
                vm.setBootType(firmware.equalsIgnoreCase("efi") ? "UEFI" : "BIOS");
            } else if (objProp.getName().equals("runtime.host")) {
                ManagedObjectReference hostMor = (ManagedObjectReference) objProp.getVal();
                setUnmanagedInstanceTOHostAndCluster(vm, hostMor, hostClusterNamesMap);
            }
        }
        if (isTemplate || excludeByKeyword) {
            return null;
        }
        return vm;
    }

    private void setUnmanagedInstanceTOHostAndCluster(UnmanagedInstanceTO vm, ManagedObjectReference hostMor,
                                                      Map<String, Pair<String, String>> hostClusterNamesMap) throws Exception {
        if (hostMor != null && StringUtils.isNotBlank(hostMor.getValue())) {
            String hostMorValue = hostMor.getValue();
            Pair<String, String> hostClusterPair;
            if (hostClusterNamesMap.containsKey(hostMorValue)) {
                hostClusterPair = hostClusterNamesMap.get(hostMorValue);
            } else {
                HostMO hostMO = new HostMO(_context, hostMor);
                ClusterMO clusterMO = new ClusterMO(_context, hostMO.getHyperHostCluster());
                hostClusterPair = new Pair<>(hostMO.getHostName(), clusterMO.getName());
                hostClusterNamesMap.put(hostMorValue, hostClusterPair);
            }
            vm.setHostName(hostClusterPair.first());
            vm.setClusterName(hostClusterPair.second());
        }
    }
}
