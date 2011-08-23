/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.mo;

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
	
	public CustomFieldDef[] getFields() throws Exception {
		return (CustomFieldDef[])_context.getServiceUtil().getDynamicProperty(getMor(), "field");
	}
	
	public int getCustomFieldKey(String morType, String fieldName) throws Exception {
		CustomFieldDef[] fields = getFields();
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
