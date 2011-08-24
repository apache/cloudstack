/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.test.utils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Formatter;

public class SqlDataGenerator {
	public static void main (String[] args) {
		try {
			FileOutputStream fs = new FileOutputStream(new File("out.txt"));
			
			DataOutputStream out = new DataOutputStream(fs);
			
			for (int i = 20; i < 171; i++) {
				out.writeBytes("INSERT INTO `vmops`.`dc_ip_address_alloc` (ip_address, data_center_id, pod_id) VALUES ('192.168.2."+i+"',1,1);\r\n");
			}
			out.writeBytes("\r\n");
			for (int i = 1; i < 10000; i++) {
				StringBuilder imagePath = new StringBuilder();
    	        Formatter formatter = new Formatter(imagePath);
    	        formatter.format("%04x", i);
				out.writeBytes("INSERT INTO `vmops`.`dc_vnet_alloc` (vnet, data_center_id) VALUES ('"+imagePath.toString()+"',1);\r\n");
			}
			
			out.flush();
			out.close();
		} catch (Exception e) {
			
		}
	}
}
