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

import java.util.List;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PrivilegePolicyDef;

public class CustomFieldsManagerMO extends BaseMO {

	public CustomFieldsManagerMO(VmwareContext context, ManagedObjectReference mor) {
		super(context, mor);
	}

	public CustomFieldsManagerMO(VmwareContext context, String morType, String morValue) {
		super(context, morType, morValue);
	}

	public CustomFieldDef addCustomerFieldDef(String fieldName, String morType,
		PrivilegePolicyDef fieldDefPolicy, PrivilegePolicyDef fieldPolicy) throws Exception {
		return _context.getService().addCustomFieldDef(getMor(), fieldName, morType, fieldDefPolicy, fieldPolicy);
	}

	public void removeCustomFieldDef(int key) throws Exception {
		_context.getService().removeCustomFieldDef(getMor(), key);
	}

	public void renameCustomFieldDef(int key, String name) throws Exception {
		_context.getService().renameCustomFieldDef(getMor(), key, name);
	}

	public void setField(ManagedObjectReference morEntity, int key, String value) throws Exception {
		_context.getService().setField(getMor(), morEntity, key, value);
	}

	public List<CustomFieldDef> getFields() throws Exception {
		return (List<CustomFieldDef>)_context.getVimClient().getDynamicProperty(getMor(), "field");
	}

	public int getCustomFieldKey(String morType, String fieldName) throws Exception {
		List<CustomFieldDef> fields = getFields();
		if(fields != null) {
			for(CustomFieldDef field : fields) {
				if(field.getName().equals(fieldName) && field.getManagedObjectType().equals(morType))
					return field.getKey();
			}
		}
		return 0;
	}

	public int ensureCustomFieldDef(String morType, String fieldName) throws Exception {
		int key = getCustomFieldKey(morType, fieldName);
		if(key > 0)
			return key;

		try {
			CustomFieldDef field = addCustomerFieldDef(fieldName, morType, null, null);
			return field.getKey();
		} catch(Exception e) {
			// assuming that someone is adding it
			key = getCustomFieldKey(morType, fieldName);
		}

		if(key == 0)
			throw new Exception("Unable to setup custom field facility for " + morType + ":" + fieldName);

		return key;
	}
}
