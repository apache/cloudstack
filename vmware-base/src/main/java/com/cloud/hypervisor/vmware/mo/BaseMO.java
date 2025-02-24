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

import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.utils.Pair;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;

import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.RetrieveResult;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class BaseMO {
    protected static Logger logger = LogManager.getLogger(BaseMO.class);

    protected VmwareContext _context;
    protected ManagedObjectReference _mor;

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

    protected static Pair<String, List<ObjectContent>> createReturnObjectPair(RetrieveResult result) {
        if (logger.isDebugEnabled()) {
            logger.debug("vmware result : {} ", ReflectionToStringBuilderUtils.reflectCollection(result));
        }
        if (result == null) {
            return new Pair<>(null, new ArrayList<>());
        }
        String tokenForRetrievingNewResults = result.getToken();
        List<ObjectContent> listOfObjects = result.getObjects();
        return new Pair<>(tokenForRetrievingNewResults, listOfObjects);
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

    public int getCustomFieldKey(String fieldName) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return getCustomFieldKey(getMor().getType(), fieldName);
    }

    public int getCustomFieldKey(String morType, String fieldName) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        assert (morType != null);

        ManagedObjectReference cfmMor = _context.getServiceContent().getCustomFieldsManager();
        if (cfmMor == null) {
            return 0;
        }

        CustomFieldsManagerMO cfmMo = new CustomFieldsManagerMO(_context, cfmMor);

        return cfmMo.getCustomFieldKey(morType, fieldName);
    }

    protected Pair<String, List<ObjectContent>> retrieveNextSetOfProperties(String tokenForPriorQuery) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        RetrieveResult result = _context.getService().continueRetrievePropertiesEx(_context.getPropertyCollector(), tokenForPriorQuery);
        return BaseMO.createReturnObjectPair(result);
    }

    protected void objectContentToUnmanagedInstanceTO(Pair<String, List<ObjectContent>> objectContents, List<UnmanagedInstanceTO> vms) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        List<ObjectContent> ocs = objectContents.second();
        if (ocs != null) {
            for (ObjectContent oc : ocs) {
                ManagedObjectReference vmMor = oc.getObj();
                if (vmMor != null) {
                    VirtualMachineMO vmMo = new VirtualMachineMO(_context, vmMor);
                    try {
                        if (!vmMo.isTemplate()) {
                            HostMO hostMO = vmMo.getRunningHost();
                            UnmanagedInstanceTO unmanagedInstance = VmwareHelper.getUnmanagedInstance(hostMO, vmMo);
                            vms.add(unmanagedInstance);
                        }
                    } catch (Exception e) {
                        logger.debug("Unexpected error checking unmanaged instance {}, excluding it: {}", vmMo.getVmName(), e.getMessage(), e);
                    }
                }
            }
        }
    }
}
