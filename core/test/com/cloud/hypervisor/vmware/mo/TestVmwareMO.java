/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.mo;

import java.util.GregorianCalendar;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.testcase.Log4jEnabledTestCase;
import com.google.gson.Gson;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.VirtualMachineConfigSpec;

// This test case needs a particular setup, only used for my own test
public class TestVmwareMO extends Log4jEnabledTestCase {
    private static final Logger s_logger = Logger.getLogger(TestVmwareMO.class);

    public void test() {
		try {
			VmwareContext context = TestVmwareContextFactory.create(
					"10.223.80.29", "Administrator", "Suite219");
			
			HostMO hostMo = new HostMO(context, "HostSystem", "host-10");
			ObjectContent[] ocs = hostMo.getVmPropertiesOnHyperHost(new String[] {"name", "config.template", "runtime.bootTime"});
			if(ocs != null) {
			    for(ObjectContent oc : ocs) {
			        DynamicProperty[] props = oc.getPropSet();
			        if(props != null) {
			            String name = null;
			            boolean template = false;
			            GregorianCalendar bootTime = null;
			            
    			        for(DynamicProperty prop : props) {
    			            if(prop.getName().equals("name"))
    			                name = prop.getVal().toString();
    			            else if(prop.getName().equals("config.template"))
    			                template = (Boolean)prop.getVal();
    			            else if(prop.getName().equals("runtime.bootTime")) 
    			                bootTime = (GregorianCalendar)prop.getVal();
    			        }
    			        
                        System.out.println("name: " + name + ", template: " + template + ", bootTime: " + bootTime);
    			        
			        }
			        System.out.println("");
			    }
			}
			
			context.close();
		} catch(Exception e) {
			s_logger.error("Unexpected exception : ", e);
		}
    }
}

