package com.cloud.gate.util;

import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;

import com.cloud.gate.testcase.BaseTestCase;
import com.cloud.stack.CloudStackClient;
import com.cloud.stack.CloudStackCommand;
import com.cloud.stack.models.CloudStackUserVm;
import com.google.gson.reflect.TypeToken;

public class CloudStackClientTestCase extends BaseTestCase {
    protected final static Logger logger = Logger.getLogger(CloudStackClientTestCase.class);

    // remember to replace with a valid key-pair in test
    private final static String API_KEY = "kVMfr1iE0KlKKOUPD-H4GburZHo4KLxIczbl5CM_ilcKFXkmsIfZjWIkCY5QpuKpDvu-DyFud44VfVvXmPKMkw";
    private final static String SECRET_KEY = "a5Y0ysvVHZ0cuffaV26wRm_vvsV5VQldRq9udC21AE8Kwsk0JG8-pz6YSp3bbc3rC0kK5q3_B9QBBzjHafVicw";

    public void testCall() {
    	CloudStackClient client = new CloudStackClient("192.168.130.22", 8080, false);

		CloudStackCommand command = new CloudStackCommand("startVirtualMachine");
		command.setParam("id", "246446");
		try {
			CloudStackUserVm vm = client.call(command, API_KEY, SECRET_KEY, true, "startvirtualmachineresponse", "virtualmachine", CloudStackUserVm.class);
			Assert.assertTrue(vm.getId() == 246446);
		} catch(Exception e) {
			logger.error("Unexpected exception ", e);
		}
    }

    public void testListCall() {
    	CloudStackClient client = new CloudStackClient("192.168.130.22", 8080, false);
    	
		CloudStackCommand command = new CloudStackCommand("listVirtualMachines");
		command.setParam("domainid", "1");
		command.setParam("account", "admin");
		command.setParam("page", "1");
		command.setParam("pagesize", "20");
		try {
			List<CloudStackUserVm> vms = client.listCall(command, API_KEY, SECRET_KEY, 
				"listvirtualmachinesresponse", "virtualmachine", new TypeToken<List<CloudStackUserVm>>() {}.getType());

			for(CloudStackUserVm vm : vms) {
				logger.info("id: " + vm.getId() + ", name: " + vm.getName());
			}
		} catch(Exception e) {
			logger.error("Unexpected exception ", e);
		}
    }
}
