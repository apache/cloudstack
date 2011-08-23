/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.util;
import java.io.File;

import junit.framework.Assert;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.resource.SshHelper;
import com.cloud.hypervisor.vmware.resource.VmwareContextFactory;
import com.cloud.utils.Pair;
import com.cloud.utils.testcase.Log4jEnabledTestCase;
import com.vmware.vim25.ManagedObjectReference;

// This test case needs a particular setup, only used for my own test
public class TestVmwareUtil extends Log4jEnabledTestCase {
    private static final Logger s_logger = Logger.getLogger(TestVmwareUtil.class);

	public void testContextCreation() {
		try {
			VmwareContext context = VmwareContextFactory.create("vsphere-1.lab.vmops.com", "Administrator", "Suite219");
			Assert.assertTrue(true);
			context.close();
		} catch(Exception e) {
			s_logger.error("Unexpected exception : ", e);
		}
	}
	
	public void testSearchIndex() {
		try {
			VmwareContext context = VmwareContextFactory.create("vsphere-1.lab.vmops.com", "Administrator", "Suite219");
			Assert.assertTrue(true);
			
			ManagedObjectReference morHost = context.getService().findByDnsName(context.getServiceContent().getSearchIndex(), 
				null, "esxhost-1.lab.vmops.com", false);
			Assert.assertTrue(morHost.getType().equalsIgnoreCase("HostSystem"));
			
			morHost = context.getService().findByIp(context.getServiceContent().getSearchIndex(), 
				null, "192.168.1.168", false);
			Assert.assertTrue(morHost == null);
			context.close();
		} catch(Exception e) {
			s_logger.error("Unexpected exception : ", e);
		}
	}
	
	public void testVmxFileDownload() {
		try {
			VmwareContext context = VmwareContextFactory.create("vsphere-1.lab.vmops.com", "Administrator", "Suite219");
			byte[] vmxContent = context.getResourceContent("https://vsphere-1.lab.vmops.com/folder/ServerRoom-Fedora32/ServerRoom-Fedora32.vmx?dcPath=cupertino&dsName=NFS%20datastore");
			System.out.print(new String(vmxContent));
			context.close();
		} catch(Exception e) {
			s_logger.error("Unexpected exception : ", e);
		}
	}
	
	public void testVmxFileParser() {
		String[] tokens = "[NFS datastore] Fedora-clone-test/Fedora-clone-test.vmx".split("\\[|\\]|/");
		
		for(String str : tokens) {
			System.out.println("Token " + str);
		}
	}
	
	public void testSsh() {
		try {
			File file = new File("c:\\temp\\id_rsa.kelven");
			if(!file.exists()) {
				System.out.println("key file does not exist!");
			}
			
			Pair<Boolean, String> result = SshHelper.sshExecute("192.168.1.107", 22, "kelven", file, null, "ls -al");
			System.out.println("Result: " + result.second());
		} catch(Exception e) {
			s_logger.error("Unexpected exception : ", e);
		}
	}
	
	public void testScp() {
		try {
			File file = new File("c:\\temp\\id_rsa.kelven");
			if(!file.exists()) {
				System.out.println("key file does not exist!");
			}
			
			SshHelper.scpTo("192.168.1.107", 22, "kelven", file, null, "~", "Hello, world".getBytes(), 
				"hello.txt", null);
		} catch(Exception e) {
			s_logger.error("Unexpected exception : ", e);
		}
	}
}
